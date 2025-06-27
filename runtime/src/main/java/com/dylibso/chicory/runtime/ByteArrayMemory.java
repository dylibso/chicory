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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final Map<Integer, Integer> alignments;

    private final MemAllocStrategy allocStrategy;

    public ByteArrayMemory(MemoryLimits limits) {
        this(limits, new DefaultMemAllocStrategy(Memory.bytes(limits.maximumPages())));
    }

    public ByteArrayMemory(MemoryLimits limits, MemAllocStrategy allocStrategy) {
        this.allocStrategy = allocStrategy;
        this.limits = limits;
        this.buffer = new byte[allocStrategy.initial(PAGE_SIZE * limits.initialPages())];
        this.nPages = limits.initialPages();
        this.alignments = (limits.shared()) ? new HashMap<>() : null;
    }

    // atomic wait handling
    private final Map<Integer, AtomicInteger> monitors = new ConcurrentHashMap<>();

    // Wait until value at address != expected
    @Override
    public int waitOn(int address, int expected, long timeout) {
        timeout = (timeout < 0) ? Long.MAX_VALUE : timeout;
        AtomicInteger monitor =
                monitors.compute(
                        address,
                        (k, v) -> {
                            if (v == null) {
                                return new AtomicInteger(1);
                            } else {
                                v.incrementAndGet();
                                return v;
                            }
                        });
        long millis = timeout / 1_000_000L;
        int nanos = (int) (timeout % 1_000_000L);

        try {
            synchronized (monitor) {
                if (((int) INT_ARR_HANDLE.getVolatile(buffer, address)) == expected) {
                    try {
                        monitor.wait(millis, nanos);
                    } catch (InterruptedException ie) {
                        return 0; // wake
                    }
                    return 2; // timeout
                } else {
                    return 1; // not-equal
                }
            }
        } finally {
            monitor.decrementAndGet();
        }
    }

    @Override
    public int waitOn(int address, long expected, long timeout) {
        timeout = (timeout < 0) ? Long.MAX_VALUE : timeout;
        AtomicInteger monitor =
                monitors.compute(
                        address,
                        (k, v) -> {
                            if (v == null) {
                                return new AtomicInteger(1);
                            } else {
                                v.incrementAndGet();
                                return v;
                            }
                        });
        long millis = timeout / 1_000_000L;
        int nanos = (int) (timeout % 1_000_000L);

        try {
            synchronized (monitor) {
                if (((long) LONG_ARR_HANDLE.getVolatile(buffer, address)) == expected) {
                    try {
                        monitor.wait(millis, nanos);
                    } catch (InterruptedException ie) {
                        return 0; // wake
                    }
                    return 2; // timeout
                } else {
                    return 1; // not-equal
                }
            }
        } finally {
            monitor.decrementAndGet();
        }
    }

    // Notify all waiters at this address
    @Override
    public int notifyAddress(int address) {
        if (!shared()) {
            return 0;
        }

        AtomicInteger monitor = monitors.get(address);
        synchronized (monitor) {
            monitor.notifyAll();
        }
        monitors.remove(address);
        return monitor.get();
    }

    private byte[] allocateByteBuffer(int capacity) {
        if (capacity > buffer.length) {
            int nextCapacity = allocStrategy.next(buffer.length, capacity);
            return new byte[nextCapacity];
        } else {
            return buffer;
        }
    }

    @Override
    public Map<Integer, Integer> alignments() {
        return alignments;
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
