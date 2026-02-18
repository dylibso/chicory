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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;
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
 * try-catch is faster than explicit checks and can be optimized by the JVM.
 * Catching generic RuntimeException to keep the method bodies short and easily inlinable.
 */
public final class ByteArrayMemory implements Memory {
    // get access to the byte array elements viewed as if it were
    // a different primitive array type, such as int[], long[], etc.
    // This is actually the fastest way to access and reinterpret the underlying bytes.
    // see: https://stackoverflow.com/a/65276765/7898052
    private static final VarHandle BYTE_ARR_HANDLE =
            MethodHandles.arrayElementVarHandle(byte[].class);
    private static final VarHandle SHORT_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOAT_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle DOUBLE_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private static final boolean HAS_BYTE_ATOMICS = hasFullAtomicSupport(BYTE_ARR_HANDLE);
    private static final boolean HAS_SHORT_ATOMICS = hasFullAtomicSupport(SHORT_ARR_HANDLE);
    private static final boolean HAS_INT_ATOMICS = hasFullAtomicSupport(INT_ARR_HANDLE);
    private static final boolean HAS_LONG_ATOMICS = hasFullAtomicSupport(LONG_ARR_HANDLE);

    // Page addressing constants
    private static final int PAGE_SHIFT = 16; // PAGE_SIZE = 65536 = 2^16
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    private static boolean hasFullAtomicSupport(VarHandle varHandle) {
        return varHandle.isAccessModeSupported(AccessMode.GET_VOLATILE)
                && varHandle.isAccessModeSupported(AccessMode.SET_VOLATILE)
                && varHandle.isAccessModeSupported(AccessMode.COMPARE_AND_EXCHANGE)
                && varHandle.isAccessModeSupported(AccessMode.GET_AND_SET)
                && varHandle.isAccessModeSupported(AccessMode.GET_AND_ADD)
                && varHandle.isAccessModeSupported(AccessMode.GET_AND_BITWISE_AND)
                && varHandle.isAccessModeSupported(AccessMode.GET_AND_BITWISE_OR)
                && varHandle.isAccessModeSupported(AccessMode.GET_AND_BITWISE_XOR);
    }

    private final MemoryLimits limits;
    private DataSegment[] dataSegments;

    // Page-based storage: fixed-size array of pages, slots filled lazily during grow
    // Individual pages are never reallocated once created, enabling lock-free reads
    private final byte[][] pages;

    // Number of currently allocated pages
    private volatile int nPages;

    // Lock for grow operation (only used when memory is shared)
    private final Object growLock = new Object();

    public ByteArrayMemory(MemoryLimits limits) {
        this.limits = limits;
        int maxPages = min(limits.maximumPages(), RUNTIME_MAX_PAGES);
        this.pages = new byte[maxPages][];

        // Allocate initial pages
        for (int i = 0; i < limits.initialPages(); i++) {
            pages[i] = new byte[PAGE_SIZE];
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
     *             Use {@link #ByteArrayMemory(MemoryLimits)} instead.
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public ByteArrayMemory(MemoryLimits limits, MemAllocStrategy allocStrategy) {
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
        throw new UnsupportedOperationException();
    }

    private Object monitor(int address) {
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
        return waitOn(address, () -> atomicReadInt(address) == expected, timeout);
    }

    @Override
    @SuppressWarnings("removal")
    public int waitOn(int address, long expected, long timeout) {
        return waitOn(address, () -> atomicReadLong(address) == expected, timeout);
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
            pages[i] = new byte[PAGE_SIZE];
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
            System.arraycopy(data, offset, pages[pageIdx], pageOffset, chunk);
            addr += chunk;
            offset += chunk;
            size -= chunk;
        }
    }

    @Override
    public byte read(int addr) {
        try {
            return pages[addr >>> PAGE_SHIFT][addr & PAGE_MASK];
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
            System.arraycopy(pages[pageIdx], pageOffset, result, destOffset, chunk);
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
                INT_ARR_HANDLE.set(pages[addr >>> PAGE_SHIFT], off, data);
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
                return (int) INT_ARR_HANDLE.get(pages[addr >>> PAGE_SHIFT], off);
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
                LONG_ARR_HANDLE.set(pages[addr >>> PAGE_SHIFT], off, data);
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
                return (long) LONG_ARR_HANDLE.get(pages[addr >>> PAGE_SHIFT], off);
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
                SHORT_ARR_HANDLE.set(pages[addr >>> PAGE_SHIFT], off, data);
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
                return (short) SHORT_ARR_HANDLE.get(pages[addr >>> PAGE_SHIFT], off);
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
            pages[addr >>> PAGE_SHIFT][addr & PAGE_MASK] = data;
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public void writeF32(int addr, float data) {
        int off = addr & PAGE_MASK;
        if (off + 4 <= PAGE_SIZE) {
            try {
                FLOAT_ARR_HANDLE.set(pages[addr >>> PAGE_SHIFT], off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 4);
            }
        } else {
            writeI32(addr, Float.floatToRawIntBits(data));
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
                return (float) FLOAT_ARR_HANDLE.get(pages[addr >>> PAGE_SHIFT], off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 4);
            }
        } else {
            return Float.intBitsToFloat(readInt(addr));
        }
    }

    @Override
    public void writeF64(int addr, double data) {
        int off = addr & PAGE_MASK;
        if (off + 8 <= PAGE_SIZE) {
            try {
                DOUBLE_ARR_HANDLE.set(pages[addr >>> PAGE_SHIFT], off, data);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 8);
            }
        } else {
            writeLong(addr, Double.doubleToRawLongBits(data));
        }
    }

    @Override
    public double readDouble(int addr) {
        int off = addr & PAGE_MASK;
        if (off + 8 <= PAGE_SIZE) {
            try {
                return (double) DOUBLE_ARR_HANDLE.get(pages[addr >>> PAGE_SHIFT], off);
            } catch (RuntimeException e) {
                throw outOfBoundsException(e, addr, 8);
            }
        } else {
            return Double.longBitsToDouble(readLong(addr));
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
    public void fill(byte value, int fromIndex, int toIndex) {
        int addr = fromIndex;
        int remaining = toIndex - fromIndex;
        checkBounds(addr, remaining, sizeInBytes(), WasmRuntimeException::new);
        while (remaining > 0) {
            int pageIdx = addr >>> PAGE_SHIFT;
            int pageOffset = addr & PAGE_MASK;
            int chunk = Math.min(remaining, PAGE_SIZE - pageOffset);
            Arrays.fill(pages[pageIdx], pageOffset, pageOffset + chunk, value);
            addr += chunk;
            remaining -= chunk;
        }
    }

    @Override
    public void copy(int dest, int src, int size) {
        int limit = sizeInBytes();
        checkBounds(dest, size, limit, WasmRuntimeException::new);
        checkBounds(src, size, limit, WasmRuntimeException::new);
        while (size > 0) {
            int destOffset = dest & PAGE_MASK;
            int srcOffset = src & PAGE_MASK;
            int chunk = Math.min(size, PAGE_SIZE - Math.max(destOffset, srcOffset));
            System.arraycopy(
                    pages[src >>> PAGE_SHIFT],
                    srcOffset,
                    pages[dest >>> PAGE_SHIFT],
                    destOffset,
                    chunk);
            dest += chunk;
            src += chunk;
            size -= chunk;
        }
    }

    @Override
    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }

    // ===========================================
    // Atomic operations
    // ===========================================

    @Override
    public void atomicFence() {
        VarHandle.fullFence();
    }

    @Override
    public byte atomicAddByte(int addr, byte delta) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_BYTE_ATOMICS) {
                return (byte) BYTE_ARR_HANDLE.getAndAdd(page, off, delta);
            }
            synchronized (monitor(addr)) {
                byte value = (byte) BYTE_ARR_HANDLE.get(page, off);
                BYTE_ARR_HANDLE.set(page, off, (byte) (value + delta));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicAddInt(int addr, int delta) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_INT_ATOMICS) {
                return (int) INT_ARR_HANDLE.getAndAdd(page, off, delta);
            }
            synchronized (monitor(addr)) {
                int value = (int) INT_ARR_HANDLE.get(page, off);
                INT_ARR_HANDLE.set(page, off, value + delta);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicAddLong(int addr, long delta) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_LONG_ATOMICS) {
                return (long) LONG_ARR_HANDLE.getAndAdd(page, off, delta);
            }
            synchronized (monitor(addr)) {
                long value = (long) LONG_ARR_HANDLE.get(page, off);
                LONG_ARR_HANDLE.set(page, off, value + delta);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicAddShort(int addr, short delta) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_SHORT_ATOMICS) {
                return (short) SHORT_ARR_HANDLE.getAndAdd(page, off, delta);
            }
            if (HAS_INT_ATOMICS) {
                // cas loop using aligned int
                int alignedOff = off & ~3;
                int shift = (off & 2) * 8;
                int mask = 0xFFFF << shift;

                while (true) {
                    int oldInt = (int) INT_ARR_HANDLE.getVolatile(page, alignedOff);
                    short oldShort = (short) ((oldInt >>> shift) & 0xFFFF);
                    short newShort = (short) (oldShort + delta);
                    int newInt = (oldInt & ~mask) | ((newShort & 0xFFFF) << shift);
                    if (INT_ARR_HANDLE.compareAndSet(page, alignedOff, oldInt, newInt)) {
                        return oldShort;
                    }
                }
            }
            synchronized (monitor(addr)) {
                short value = (short) SHORT_ARR_HANDLE.get(page, off);
                SHORT_ARR_HANDLE.set(page, off, (short) (value + delta));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public byte atomicAndByte(int addr, byte mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_BYTE_ATOMICS) {
                return (byte) BYTE_ARR_HANDLE.getAndBitwiseAnd(page, off, mask);
            }
            synchronized (monitor(addr)) {
                byte value = (byte) BYTE_ARR_HANDLE.get(page, off);
                BYTE_ARR_HANDLE.set(page, off, (byte) (value & mask));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicAndInt(int addr, int mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_INT_ATOMICS) {
                return (int) INT_ARR_HANDLE.getAndBitwiseAnd(page, off, mask);
            }
            synchronized (monitor(addr)) {
                int value = (int) INT_ARR_HANDLE.get(page, off);
                INT_ARR_HANDLE.set(page, off, value & mask);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicAndLong(int addr, long mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_LONG_ATOMICS) {
                return (long) LONG_ARR_HANDLE.getAndBitwiseAnd(page, off, mask);
            }
            synchronized (monitor(addr)) {
                long value = (long) LONG_ARR_HANDLE.get(page, off);
                LONG_ARR_HANDLE.set(page, off, value & mask);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicAndShort(int addr, short mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_SHORT_ATOMICS) {
                return (short) SHORT_ARR_HANDLE.getAndBitwiseAnd(page, off, mask);
            }
            if (HAS_INT_ATOMICS) {
                // leverage that 0xFFFF is identity for and
                int alignedOff = off & ~3;
                int shift = (off & 2) * 8;
                int intMask = ((mask & 0xFFFF) << shift) | ~(0xFFFF << shift);
                int intValue = (int) INT_ARR_HANDLE.getAndBitwiseAnd(page, alignedOff, intMask);
                return (short) ((intValue >>> shift) & 0xFFFF);
            }
            synchronized (monitor(addr)) {
                short value = (short) SHORT_ARR_HANDLE.get(page, off);
                SHORT_ARR_HANDLE.set(page, off, (short) (value & mask));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public byte atomicCmpxchgByte(int addr, byte expected, byte replacement) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_BYTE_ATOMICS) {
                return (byte) BYTE_ARR_HANDLE.compareAndExchange(page, off, expected, replacement);
            }
            synchronized (monitor(addr)) {
                byte value = (byte) BYTE_ARR_HANDLE.get(page, off);
                if (value == expected) {
                    BYTE_ARR_HANDLE.set(page, off, replacement);
                }
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicCmpxchgInt(int addr, int expected, int replacement) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_INT_ATOMICS) {
                return (int) INT_ARR_HANDLE.compareAndExchange(page, off, expected, replacement);
            }
            synchronized (monitor(addr)) {
                int value = (int) INT_ARR_HANDLE.get(page, off);
                if (value == expected) {
                    INT_ARR_HANDLE.set(page, off, replacement);
                }
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicCmpxchgLong(int addr, long expected, long replacement) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_LONG_ATOMICS) {
                return (long) LONG_ARR_HANDLE.compareAndExchange(page, off, expected, replacement);
            }
            synchronized (monitor(addr)) {
                long value = (long) LONG_ARR_HANDLE.get(page, off);
                if (value == expected) {
                    LONG_ARR_HANDLE.set(page, off, replacement);
                }
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicCmpxchgShort(int addr, short expected, short replacement) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_SHORT_ATOMICS) {
                return (short)
                        SHORT_ARR_HANDLE.compareAndExchange(page, off, expected, replacement);
            }
            if (HAS_INT_ATOMICS) {
                int alignedOff = off & ~3;
                int shift = (off & 2) * 8;
                int mask = 0xFFFF << shift;

                while (true) {
                    int oldInt = (int) INT_ARR_HANDLE.getVolatile(page, alignedOff);
                    short oldShort = (short) ((oldInt >>> shift) & 0xFFFF);
                    if (oldShort != expected) {
                        return oldShort; // no match, return current value without swapping
                    }
                    int newInt = (oldInt & ~mask) | ((replacement & 0xFFFF) << shift);
                    if (INT_ARR_HANDLE.compareAndSet(page, alignedOff, oldInt, newInt)) {
                        return oldShort;
                    }
                }
            }
            synchronized (monitor(addr)) {
                short value = (short) SHORT_ARR_HANDLE.get(page, off);
                if (value == expected) {
                    SHORT_ARR_HANDLE.set(page, off, replacement);
                }
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public byte atomicOrByte(int addr, byte mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_BYTE_ATOMICS) {
                return (byte) BYTE_ARR_HANDLE.getAndBitwiseOr(page, off, mask);
            }
            synchronized (monitor(addr)) {
                byte value = (byte) BYTE_ARR_HANDLE.get(page, off);
                BYTE_ARR_HANDLE.set(page, off, (byte) (value | mask));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicOrInt(int addr, int mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_INT_ATOMICS) {
                return (int) INT_ARR_HANDLE.getAndBitwiseOr(page, off, mask);
            }
            synchronized (monitor(addr)) {
                int value = (int) INT_ARR_HANDLE.get(page, off);
                INT_ARR_HANDLE.set(page, off, value | mask);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicOrLong(int addr, long mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_LONG_ATOMICS) {
                return (long) LONG_ARR_HANDLE.getAndBitwiseOr(page, off, mask);
            }
            synchronized (monitor(addr)) {
                long value = (long) LONG_ARR_HANDLE.get(page, off);
                LONG_ARR_HANDLE.set(page, off, value | mask);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicOrShort(int addr, short mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_SHORT_ATOMICS) {
                return (short) SHORT_ARR_HANDLE.getAndBitwiseOr(page, off, mask);
            }
            if (HAS_INT_ATOMICS) {
                // leverage that 0x0000 is identity for or
                int alignedOff = off & ~3;
                int shift = (off & 2) * 8;
                int intMask = ((mask & 0xFFFF) << shift);
                int intValue = (int) INT_ARR_HANDLE.getAndBitwiseOr(page, alignedOff, intMask);
                return (short) ((intValue >>> shift) & 0xFFFF);
            }
            synchronized (monitor(addr)) {
                short value = (short) SHORT_ARR_HANDLE.get(page, off);
                SHORT_ARR_HANDLE.set(page, off, (short) (value | mask));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public byte atomicReadByte(int addr) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (BYTE_ARR_HANDLE.isAccessModeSupported(AccessMode.GET_VOLATILE)) {
                return (byte) BYTE_ARR_HANDLE.getVolatile(page, off);
            }
            synchronized (monitor(addr)) {
                return (byte) BYTE_ARR_HANDLE.get(page, off);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicReadInt(int addr) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (INT_ARR_HANDLE.isAccessModeSupported(AccessMode.GET_VOLATILE)) {
                return (int) INT_ARR_HANDLE.getVolatile(page, off);
            }
            synchronized (monitor(addr)) {
                return (int) INT_ARR_HANDLE.get(page, off);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicReadLong(int addr) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (LONG_ARR_HANDLE.isAccessModeSupported(AccessMode.GET_VOLATILE)) {
                return (long) LONG_ARR_HANDLE.getVolatile(page, off);
            }
            synchronized (monitor(addr)) {
                return (long) LONG_ARR_HANDLE.get(page, off);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicReadShort(int addr) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (SHORT_ARR_HANDLE.isAccessModeSupported(AccessMode.GET_VOLATILE)) {
                return (short) SHORT_ARR_HANDLE.getVolatile(page, off);
            }
            synchronized (monitor(addr)) {
                return (short) SHORT_ARR_HANDLE.get(page, off);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public void atomicWriteByte(int addr, byte value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (BYTE_ARR_HANDLE.isAccessModeSupported(AccessMode.SET_VOLATILE)) {
                BYTE_ARR_HANDLE.setVolatile(page, off, value);
                return;
            }
            synchronized (monitor(addr)) {
                BYTE_ARR_HANDLE.set(page, off, value);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public void atomicWriteInt(int addr, int value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (INT_ARR_HANDLE.isAccessModeSupported(AccessMode.SET_VOLATILE)) {
                INT_ARR_HANDLE.setVolatile(page, off, value);
                return;
            }
            synchronized (monitor(addr)) {
                INT_ARR_HANDLE.set(page, off, value);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public void atomicWriteLong(int addr, long value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (LONG_ARR_HANDLE.isAccessModeSupported(AccessMode.SET_VOLATILE)) {
                LONG_ARR_HANDLE.setVolatile(page, off, value);
                return;
            }
            synchronized (monitor(addr)) {
                LONG_ARR_HANDLE.set(page, off, value);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public void atomicWriteShort(int addr, short value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (SHORT_ARR_HANDLE.isAccessModeSupported(AccessMode.SET_VOLATILE)) {
                SHORT_ARR_HANDLE.setVolatile(page, off, value);
                return;
            }
            synchronized (monitor(addr)) {
                SHORT_ARR_HANDLE.set(page, off, value);
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public byte atomicXchgByte(int addr, byte value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_BYTE_ATOMICS) {
                return (byte) BYTE_ARR_HANDLE.getAndSet(page, off, value);
            }
            synchronized (monitor(addr)) {
                byte oldValue = (byte) BYTE_ARR_HANDLE.get(page, off);
                BYTE_ARR_HANDLE.set(page, off, value);
                return oldValue;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicXchgInt(int addr, int value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_INT_ATOMICS) {
                return (int) INT_ARR_HANDLE.getAndSet(page, off, value);
            }
            synchronized (monitor(addr)) {
                int oldValue = (int) INT_ARR_HANDLE.get(page, off);
                INT_ARR_HANDLE.set(page, off, value);
                return oldValue;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicXchgLong(int addr, long value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_LONG_ATOMICS) {
                return (long) LONG_ARR_HANDLE.getAndSet(page, off, value);
            }
            synchronized (monitor(addr)) {
                long oldValue = (long) LONG_ARR_HANDLE.get(page, off);
                LONG_ARR_HANDLE.set(page, off, value);
                return oldValue;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicXchgShort(int addr, short value) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_SHORT_ATOMICS) {
                return (short) SHORT_ARR_HANDLE.getAndSet(page, off, value);
            }
            if (HAS_INT_ATOMICS) {
                int alignedOff = off & ~3;
                int shift = (off & 2) * 8;
                int mask = 0xFFFF << shift;

                while (true) {
                    int oldInt = (int) INT_ARR_HANDLE.getVolatile(page, alignedOff);
                    int newInt = (oldInt & ~mask) | ((value & 0xFFFF) << shift);
                    if (INT_ARR_HANDLE.compareAndSet(page, alignedOff, oldInt, newInt)) {
                        return (short) ((oldInt >>> shift) & 0xFFFF);
                    }
                }
            }
            synchronized (monitor(addr)) {
                short oldValue = (short) SHORT_ARR_HANDLE.get(page, off);
                SHORT_ARR_HANDLE.set(page, off, value);
                return oldValue;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }

    @Override
    public byte atomicXorByte(int addr, byte mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_BYTE_ATOMICS) {
                return (byte) BYTE_ARR_HANDLE.getAndBitwiseXor(page, off, mask);
            }
            synchronized (monitor(addr)) {
                byte value = (byte) BYTE_ARR_HANDLE.get(page, off);
                BYTE_ARR_HANDLE.set(page, off, (byte) (value ^ mask));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1);
        }
    }

    @Override
    public int atomicXorInt(int addr, int mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_INT_ATOMICS) {
                return (int) INT_ARR_HANDLE.getAndBitwiseXor(page, off, mask);
            }
            synchronized (monitor(addr)) {
                int value = (int) INT_ARR_HANDLE.get(page, off);
                INT_ARR_HANDLE.set(page, off, value ^ mask);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4);
        }
    }

    @Override
    public long atomicXorLong(int addr, long mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_LONG_ATOMICS) {
                return (long) LONG_ARR_HANDLE.getAndBitwiseXor(page, off, mask);
            }
            synchronized (monitor(addr)) {
                long value = (long) LONG_ARR_HANDLE.get(page, off);
                LONG_ARR_HANDLE.set(page, off, value ^ mask);
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8);
        }
    }

    @Override
    public short atomicXorShort(int addr, short mask) {
        try {
            byte[] page = pages[addr >>> PAGE_SHIFT];
            int off = addr & PAGE_MASK;
            if (HAS_SHORT_ATOMICS) {
                return (short) SHORT_ARR_HANDLE.getAndBitwiseXor(page, off, mask);
            }
            if (HAS_INT_ATOMICS) {
                // leverage that 0x0000 is identity for xor
                int alignedOff = off & ~3;
                int shift = (off & 2) * 8;
                int intMask = ((mask & 0xFFFF) << shift);
                int intValue = (int) INT_ARR_HANDLE.getAndBitwiseXor(page, alignedOff, intMask);
                return (short) ((intValue >>> shift) & 0xFFFF);
            }
            synchronized (monitor(addr)) {
                short value = (short) SHORT_ARR_HANDLE.get(page, off);
                SHORT_ARR_HANDLE.set(page, off, (short) (value ^ mask));
                return value;
            }
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2);
        }
    }
}
