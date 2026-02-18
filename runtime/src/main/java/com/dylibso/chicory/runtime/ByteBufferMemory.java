package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static java.lang.Math.min;

import com.dylibso.chicory.runtime.alloc.MemAllocStrategy;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import java.lang.reflect.InvocationTargetException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Represents the linear memory in the Wasm program. Can be shared
 * reference b/w the host and the guest.
 *
 * This is the preferred memory implementation on Android systems.
 */
public final class ByteBufferMemory implements Memory {
    // Package private for usage as default impl. in Memory. Can become private in next major
    // release.
    static final Runnable ATOMIC_FENCE_IMPL = getAtomicFenceImpl();

    // Page addressing constants
    private static final int PAGE_SHIFT = 16; // PAGE_SIZE = 65536 = 2^16
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    private final MemoryLimits limits;
    private DataSegment[] dataSegments;

    // Page-based storage: fixed-size array of pages, slots filled lazily during grow
    // Individual pages are never reallocated once created, enabling lock-free reads
    private final ByteBuffer[] pages;

    // Number of currently allocated pages
    private volatile int nPages;

    // Lock for grow operation (only used when memory is shared)
    private final Object growLock = new Object();

    public ByteBufferMemory(MemoryLimits limits) {
        this.limits = limits;
        int maxPages = min(limits.maximumPages(), RUNTIME_MAX_PAGES);
        this.pages = new ByteBuffer[maxPages];

        // Allocate initial pages
        for (int i = 0; i < limits.initialPages(); i++) {
            pages[i] = ByteBuffer.allocate(PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        }
        this.nPages = limits.initialPages();

        if (limits.shared()) {
            waitStates = new ConcurrentHashMap<>();
        } else {
            waitStates = null;
        }
    }

    /**
     * @deprecated The MemAllocStrategy is no longer used since memory is allocated by page.
     *             Use {@link #ByteBufferMemory(MemoryLimits)} instead.
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public ByteBufferMemory(MemoryLimits limits, MemAllocStrategy allocStrategy) {
        this(limits);
    }

    // Tracks wait state per address: waiter count and pending wakeups
    // all field access should be guarded by synchronizing on the instance.
    // Invariants: 0 <= pendingWakeups <= waiterCount
    private static final class WaitState {
        // The number of threads currently waiting
        int waiterCount;
        // The number of waiting threads that have been scheduled to wake up
        int pendingWakeups;
    }

    private final Map<Integer, WaitState> waitStates;

    @Override
    @SuppressWarnings("removal")
    public Object lock(int address) {
        if (!shared()) {
            // disable locking
            return new Object();
        }
        return waitStates.computeIfAbsent(address, k -> new WaitState());
    }

    // Wait IF condition is true
    private int waitOn(int address, BooleanSupplier condition, long timeout) {
        if (!shared()) {
            throw new ChicoryException("Attempt to wait on a non-shared memory, not supported.");
        }

        long deadline = (timeout < 0) ? Long.MAX_VALUE : System.nanoTime() + timeout;

        WaitState state = waitStates.computeIfAbsent(address, k -> new WaitState());

        synchronized (state) {
            // Check the condition while holding the lock
            // This must be atomic with the decision to wait
            if (!condition.getAsBoolean()) {
                return 1; // not-equal
            }

            state.waiterCount++;
            try {

                while (state.pendingWakeups == 0) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        return 2; // timeout
                    }
                    long millis = Math.max(remaining / 1_000_000L, 0);
                    int nanos = Math.max((int) (remaining % 1_000_000L), 0);
                    try {
                        state.wait(millis, nanos);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ChicoryInterruptedException("Thread interrupted");
                    }
                }
                return 0; // woken
            } finally {

                if (state.pendingWakeups > 0) {
                    // any thread leaving the try block is correct to consume
                    // a pending wakeup IF available:
                    // ret 0 - woken thread correctly consumes a wakeup
                    // ret 2 - timeout can only occur with pendingWakeups == 0 so will not consume
                    // throw - isn't part of the wasm runtime but is semantically correct to consume
                    state.pendingWakeups--;
                }
                state.waiterCount--;
                assert (0 <= state.pendingWakeups);
                assert (state.pendingWakeups <= state.waiterCount);
            }
        }
    }

    @Override
    @SuppressWarnings("removal")
    public int waitOn(int address, int expected, long timeout) {
        return waitOn(address, () -> readInt(address) == expected, timeout);
    }

    @Override
    @SuppressWarnings("removal")
    public int waitOn(int address, long expected, long timeout) {
        return waitOn(address, () -> readLong(address) == expected, timeout);
    }

    // Notify waiters at this address
    @Override
    @SuppressWarnings("removal")
    public int notify(int address, int maxThreads) {
        if (!shared()) {
            return 0;
        }

        WaitState state = waitStates.get(address);
        if (state == null) {
            return 0;
        }

        synchronized (state) {
            int actualWaiters = state.waiterCount - state.pendingWakeups;

            if (actualWaiters == 0) {
                return 0;
            }

            // Calculate how many to wake: min(actualWaiters, maxThreads)
            int toWake;
            if (maxThreads < 0) {
                toWake = actualWaiters; // wake all
            } else {
                toWake = Math.min(actualWaiters, maxThreads);
            }

            // Add pending wakeups - waiters will consume these
            state.pendingWakeups += toWake;
            assert (state.pendingWakeups <= state.waiterCount);

            // It's always safe to notify all. In the wait routine we consume pendingWakeups
            // and go back to waiting if there are none. We could also choose to notify waiters
            // one by one in a loop. The optimal choice of whether to notify all should be
            // toWake > C * state.waiterCount, where C is an unknown constant weighing the cost
            // of looping through notify vs. having threads wake and go back to waiting. Since
            // the constant is unknown we opt for the simplest choice of just notifying all.
            state.notifyAll();

            return toWake;
        }
    }

    /**
     * Gets the size of the memory in number of pages
     */
    @Override
    public int pages() {
        return nPages;
    }

    @Override
    public int grow(int size) {
        if (!shared()) {
            return growImpl(size);
        }
        synchronized (growLock) {
            return growImpl(size);
        }
    }

    private int growImpl(int size) {
        int prevPages = nPages;
        int numPages = prevPages + size;

        if (numPages > maximumPages() || numPages < prevPages) {
            return -1;
        }

        // Allocate new pages
        for (int i = prevPages; i < numPages; i++) {
            pages[i] = ByteBuffer.allocate(PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        }

        // Publish new page count (volatile write ensures visibility of new pages)
        nPages = numPages;
        return prevPages;
    }

    @Override
    public int initialPages() {
        return this.limits.initialPages();
    }

    @Override
    public int maximumPages() {
        return min(this.limits.maximumPages(), RUNTIME_MAX_PAGES);
    }

    @Override
    public boolean shared() {
        return this.limits.shared();
    }

    @Override
    public void initialize(Instance instance, DataSegment[] dataSegments) {
        this.dataSegments = dataSegments;
        if (dataSegments == null) {
            return;
        }

        for (var s : dataSegments) {
            if (s instanceof ActiveDataSegment) {
                var segment = (ActiveDataSegment) s;
                var offsetExpr = segment.offsetInstructions();
                var data = segment.data();
                var offset = (int) computeConstantValue(instance, offsetExpr)[0];
                checkBounds(offset, data.length, sizeInBytes(), UninstantiableException::new);
                write(offset, data, 0, data.length);
            } else if (s instanceof PassiveDataSegment) {
                // Passive segment should be skipped
            } else {
                throw new ChicoryException("Data segment should be active or passive: " + s);
            }
        }
    }

    private static void checkBounds(
            int addr, int size, int limit, Function<String, ChicoryException> exceptionFactory) {
        if (addr < 0 || size < 0 || addr > limit || (size > 0 && ((addr + size) > limit))) {
            var errorMsg =
                    "out of bounds memory access: attempted to access address: "
                            + addr
                            + " but limit is: "
                            + limit
                            + " and size: "
                            + size;
            throw exceptionFactory.apply(errorMsg);
        }
    }

    private RuntimeException outOfBoundsException(RuntimeException e, int addr, int size) {
        if (e instanceof IndexOutOfBoundsException
                || e instanceof BufferOverflowException
                || e instanceof BufferUnderflowException
                || e instanceof IllegalArgumentException
                || e instanceof NullPointerException
                || e instanceof NegativeArraySizeException) {
            var limit = sizeInBytes();
            var errorMsg =
                    "out of bounds memory access: attempted to access address: "
                            + addr
                            + " but limit is: "
                            + limit
                            + " and size: "
                            + size;
            return new WasmRuntimeException(errorMsg);
        } else {
            return e;
        }
    }

    @Override
    public void initPassiveSegment(int segmentId, int dest, int offset, int size) {
        var segment = dataSegments[segmentId];
        write(dest, segment.data(), offset, size);
    }

    private int sizeInBytes() {
        return PAGE_SIZE * nPages;
    }

    @Override
    public void write(int addr, byte[] data, int offset, int size) {
        checkBounds(offset, size, data.length, WasmRuntimeException::new);
        checkBounds(addr, size, sizeInBytes(), WasmRuntimeException::new);
        while (size > 0) {
            int pageIdx = addr >>> PAGE_SHIFT;
            int pageOffset = addr & PAGE_MASK;
            int chunk = Math.min(size, PAGE_SIZE - pageOffset);
            pages[pageIdx].position(pageOffset);
            pages[pageIdx].put(data, offset, chunk);
            addr += chunk;
            offset += chunk;
            size -= chunk;
        }
    }

    @Override
    public byte read(int addr) {
        try {
            return pages[addr >>> PAGE_SHIFT].get(addr & PAGE_MASK);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public byte[] readBytes(int addr, int len) {
        checkBounds(addr, len, sizeInBytes(), WasmRuntimeException::new);
        byte[] result = new byte[len];
        int destOffset = 0;
        int remaining = len;
        int a = addr;
        while (remaining > 0) {
            int pageIdx = a >>> PAGE_SHIFT;
            int pageOffset = a & PAGE_MASK;
            int chunk = Math.min(remaining, PAGE_SIZE - pageOffset);
            pages[pageIdx].position(pageOffset);
            pages[pageIdx].get(result, destOffset, chunk);
            a += chunk;
            destOffset += chunk;
            remaining -= chunk;
        }
        return result;
    }

    @Override
    public void writeI32(int addr, int data) {
        int off = addr & PAGE_MASK;
        if (off + 4 <= PAGE_SIZE) {
            try {
                pages[addr >>> PAGE_SHIFT].putInt(off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 4);
            }
        } else {
            writeI32Slow(addr, data);
        }
    }

    private void writeI32Slow(int addr, int data) {
        checkBounds(addr, 4, sizeInBytes(), WasmRuntimeException::new);
        writeByte(addr, (byte) data);
        writeByte(addr + 1, (byte) (data >>> 8));
        writeByte(addr + 2, (byte) (data >>> 16));
        writeByte(addr + 3, (byte) (data >>> 24));
    }

    @Override
    public int readInt(int addr) {
        int off = addr & PAGE_MASK;
        if (off + 4 <= PAGE_SIZE) {
            try {
                return pages[addr >>> PAGE_SHIFT].getInt(off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 4);
            }
        } else {
            return readIntSlow(addr);
        }
    }

    private int readIntSlow(int addr) {
        return (read(addr) & 0xFF)
                | ((read(addr + 1) & 0xFF) << 8)
                | ((read(addr + 2) & 0xFF) << 16)
                | ((read(addr + 3) & 0xFF) << 24);
    }

    @Override
    public void writeLong(int addr, long data) {
        int off = addr & PAGE_MASK;
        if (off + 8 <= PAGE_SIZE) {
            try {
                pages[addr >>> PAGE_SHIFT].putLong(off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 8);
            }
        } else {
            writeLongSlow(addr, data);
        }
    }

    private void writeLongSlow(int addr, long data) {
        checkBounds(addr, 8, sizeInBytes(), WasmRuntimeException::new);
        writeByte(addr, (byte) data);
        writeByte(addr + 1, (byte) (data >>> 8));
        writeByte(addr + 2, (byte) (data >>> 16));
        writeByte(addr + 3, (byte) (data >>> 24));
        writeByte(addr + 4, (byte) (data >>> 32));
        writeByte(addr + 5, (byte) (data >>> 40));
        writeByte(addr + 6, (byte) (data >>> 48));
        writeByte(addr + 7, (byte) (data >>> 56));
    }

    @Override
    public long readLong(int addr) {
        int off = addr & PAGE_MASK;
        if (off + 8 <= PAGE_SIZE) {
            try {
                return pages[addr >>> PAGE_SHIFT].getLong(off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 8);
            }
        } else {
            return readLongSlow(addr);
        }
    }

    private long readLongSlow(int addr) {
        return (read(addr) & 0xFFL)
                | ((read(addr + 1) & 0xFFL) << 8)
                | ((read(addr + 2) & 0xFFL) << 16)
                | ((read(addr + 3) & 0xFFL) << 24)
                | ((read(addr + 4) & 0xFFL) << 32)
                | ((read(addr + 5) & 0xFFL) << 40)
                | ((read(addr + 6) & 0xFFL) << 48)
                | ((read(addr + 7) & 0xFFL) << 56);
    }

    @Override
    public void writeShort(int addr, short data) {
        int off = addr & PAGE_MASK;
        if (off + 2 <= PAGE_SIZE) {
            try {
                pages[addr >>> PAGE_SHIFT].putShort(off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 2);
            }
        } else {
            writeShortSlow(addr, data);
        }
    }

    private void writeShortSlow(int addr, short data) {
        checkBounds(addr, 2, sizeInBytes(), WasmRuntimeException::new);
        writeByte(addr, (byte) data);
        writeByte(addr + 1, (byte) (data >>> 8));
    }

    @Override
    public short readShort(int addr) {
        int off = addr & PAGE_MASK;
        if (off + 2 <= PAGE_SIZE) {
            try {
                return pages[addr >>> PAGE_SHIFT].getShort(off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 2);
            }
        } else {
            return readShortSlow(addr);
        }
    }

    private short readShortSlow(int addr) {
        return (short) ((read(addr) & 0xFF) | ((read(addr + 1) & 0xFF) << 8));
    }

    @Override
    public long readU16(int addr) {
        return readShort(addr) & 0xFFFFL;
    }

    @Override
    public void writeByte(int addr, byte data) {
        try {
            pages[addr >>> PAGE_SHIFT].put(addr & PAGE_MASK, data);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public void writeF32(int addr, float data) {
        int off = addr & PAGE_MASK;
        if (off + 4 <= PAGE_SIZE) {
            try {
                pages[addr >>> PAGE_SHIFT].putFloat(off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 4);
            }
        } else {
            writeI32Slow(addr, Float.floatToRawIntBits(data));
        }
    }

    @Override
    public long readF32(int addr) {
        return readInt(addr);
    }

    @Override
    public float readFloat(int addr) {
        int off = addr & PAGE_MASK;
        if (off + 4 <= PAGE_SIZE) {
            try {
                return pages[addr >>> PAGE_SHIFT].getFloat(off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 4);
            }
        } else {
            return Float.intBitsToFloat(readIntSlow(addr));
        }
    }

    @Override
    public void writeF64(int addr, double data) {
        int off = addr & PAGE_MASK;
        if (off + 8 <= PAGE_SIZE) {
            try {
                pages[addr >>> PAGE_SHIFT].putDouble(off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 8);
            }
        } else {
            writeLongSlow(addr, Double.doubleToRawLongBits(data));
        }
    }

    @Override
    public double readDouble(int addr) {
        int off = addr & PAGE_MASK;
        if (off + 8 <= PAGE_SIZE) {
            try {
                return pages[addr >>> PAGE_SHIFT].getDouble(off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 8);
            }
        } else {
            return Double.longBitsToDouble(readLongSlow(addr));
        }
    }

    @Override
    public long readF64(int addr) {
        return readLong(addr);
    }

    @Override
    public void zero() {
        fill((byte) 0, 0, sizeInBytes());
    }

    @Override
    @SuppressWarnings("ByteBufferBackingArray")
    public void fill(byte value, int fromIndex, int toIndex) {
        int addr = fromIndex;
        int remaining = toIndex - fromIndex;
        checkBounds(addr, remaining, sizeInBytes(), WasmRuntimeException::new);
        while (remaining > 0) {
            int pageIdx = addr >>> PAGE_SHIFT;
            int pageOffset = addr & PAGE_MASK;
            int chunk = Math.min(remaining, PAGE_SIZE - pageOffset);
            Arrays.fill(pages[pageIdx].array(), pageOffset, pageOffset + chunk, value);
            addr += chunk;
            remaining -= chunk;
        }
    }

    @Override
    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }

    private static Runnable getAtomicFenceImpl() {
        try {
            // to take into account older Android API level:
            // https://developer.android.com/reference/java/lang/invoke/VarHandle#fullFence()
            java.lang.invoke.VarHandle.fullFence();
            return java.lang.invoke.VarHandle::fullFence;
        } catch (NoSuchMethodError e) {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                var theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                var theUnsafe = theUnsafeField.get(null);
                var fullFence = unsafeClass.getMethod("fullFence");

                return () -> {
                    try {
                        fullFence.invoke(theUnsafe);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new RuntimeException(
                                "ATOMIC_FENCE implementation: Failed to invoke"
                                        + " sun.misc.Unsafe",
                                ex);
                    }
                };
            } catch (Throwable ex) {
                throw new RuntimeException(
                        "ATOMIC_FENCE implementation: Failed to lookup sun.misc.Unsafe", ex);
            }
        }
    }
}
