package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static java.lang.Math.min;

import com.dylibso.chicory.runtime.alloc.DefaultMemAllocStrategy;
import com.dylibso.chicory.runtime.alloc.MemAllocStrategy;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
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

    private final MemoryLimits limits;
    private DataSegment[] dataSegments;
    private byte[] buffer;
    private int nPages;

    private final MemAllocStrategy allocStrategy;

    public ByteArrayMemory(MemoryLimits limits) {
        this(limits, new DefaultMemAllocStrategy(Memory.bytes(limits.maximumPages())));
    }

    public ByteArrayMemory(MemoryLimits limits, MemAllocStrategy allocStrategy) {
        this.allocStrategy = allocStrategy;
        this.limits = limits;
        this.buffer = new byte[allocStrategy.initial(PAGE_SIZE * limits.initialPages())];
        this.nPages = limits.initialPages();
        if (limits.shared()) {
            waitStates = new ConcurrentHashMap<>();
        } else {
            waitStates = null;
        }
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
    public int waitOn(int address, int expected, long timeout) {
        return waitOn(address, () -> readInt(address) == expected, timeout);
    }

    @Override
    public int waitOn(int address, long expected, long timeout) {
        return waitOn(address, () -> readLong(address) == expected, timeout);
    }

    // Notify waiters at this address
    @Override
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

    private byte[] allocateByteBuffer(int capacity) {
        if (capacity > buffer.length) {
            int nextCapacity = allocStrategy.next(buffer.length, capacity);
            return new byte[nextCapacity];
        } else {
            return buffer;
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
        var prevPages = nPages;
        var numPages = prevPages + size;

        if (numPages > maximumPages() || numPages < prevPages) {
            return -1;
        }

        var newBuffer = allocateByteBuffer(PAGE_SIZE * numPages);
        if (newBuffer != buffer) {
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }

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
                checkBounds(
                        offset,
                        data.length,
                        sizeInBytes(),
                        (msg) -> new UninstantiableException(msg));
                System.arraycopy(data, 0, buffer, offset, data.length);
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

    private static RuntimeException outOfBoundsException(
            RuntimeException e, int addr, int size, int limit) {
        if (e instanceof IndexOutOfBoundsException
                || e instanceof BufferOverflowException
                || e instanceof BufferUnderflowException
                || e instanceof IllegalArgumentException
                || e instanceof NegativeArraySizeException) {
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
        try {
            System.arraycopy(data, offset, buffer, addr, size);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, size, sizeInBytes());
        }
    }

    @Override
    public byte read(int addr) {
        try {
            return buffer[addr];
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1, sizeInBytes());
        }
    }

    @Override
    public byte[] readBytes(int addr, int len) {
        try {
            var bytes = new byte[len];
            System.arraycopy(buffer, addr, bytes, 0, len);
            return bytes;
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, len, sizeInBytes());
        }
    }

    @Override
    public void writeI32(int addr, int data) {
        try {
            INT_ARR_HANDLE.set(buffer, addr, data);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4, sizeInBytes());
        }
    }

    @Override
    public int readInt(int addr) {
        try {
            return (int) INT_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4, sizeInBytes());
        }
    }

    @Override
    public void writeLong(int addr, long data) {
        try {
            LONG_ARR_HANDLE.set(buffer, addr, data);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8, sizeInBytes());
        }
    }

    @Override
    public long readLong(int addr) {
        try {
            return (long) LONG_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8, sizeInBytes());
        }
    }

    @Override
    public void writeShort(int addr, short data) {
        try {
            SHORT_ARR_HANDLE.set(buffer, addr, data);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2, sizeInBytes());
        }
    }

    @Override
    public short readShort(int addr) {
        try {
            return (short) SHORT_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2, sizeInBytes());
        }
    }

    @Override
    public long readU16(int addr) {
        try {
            return (short) SHORT_ARR_HANDLE.get(buffer, addr) & 0xffff;
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 2, sizeInBytes());
        }
    }

    @Override
    public void writeByte(int addr, byte data) {
        try {
            buffer[addr] = data;
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 1, sizeInBytes());
        }
    }

    @Override
    public void writeF32(int addr, float data) {
        try {
            FLOAT_ARR_HANDLE.set(buffer, addr, data);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4, sizeInBytes());
        }
    }

    @Override
    public long readF32(int addr) {
        try {
            return (int) INT_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4, sizeInBytes());
        }
    }

    @Override
    public float readFloat(int addr) {
        try {
            return (float) FLOAT_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 4, sizeInBytes());
        }
    }

    @Override
    public void writeF64(int addr, double data) {
        try {
            DOUBLE_ARR_HANDLE.set(buffer, addr, data);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8, sizeInBytes());
        }
    }

    @Override
    public double readDouble(int addr) {
        try {
            return (double) DOUBLE_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8, sizeInBytes());
        }
    }

    @Override
    public long readF64(int addr) {
        try {
            return (long) LONG_ARR_HANDLE.get(buffer, addr);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, addr, 8, sizeInBytes());
        }
    }

    @Override
    public void zero() {
        fill((byte) 0, 0, sizeInBytes());
    }

    @Override
    @SuppressWarnings("ByteBufferBackingArray")
    public void fill(byte value, int fromIndex, int toIndex) {
        // see https://appsintheopen.com/posts/53-resetting-bytebuffers-to-zero-in-java
        try {
            Arrays.fill(buffer, fromIndex, toIndex, value);
        } catch (RuntimeException e) {
            throw outOfBoundsException(e, fromIndex, toIndex - fromIndex, sizeInBytes());
        }
    }

    @Override
    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }
}
