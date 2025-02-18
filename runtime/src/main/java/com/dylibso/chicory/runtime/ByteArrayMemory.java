package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.alloc.DefaultMemAllocStrategy;
import com.dylibso.chicory.runtime.alloc.MemAllocStrategy;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Function;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static java.lang.Math.min;

/**
 * Represents the linear memory in the Wasm program. Can be shared
 * reference b/w the host and the guest.
 */
public final class ByteArrayMemory implements Memory {

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
                        (PAGE_SIZE * nPages),
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

    //    private static void checkBounds(int addr, int size, int limit) {
    //        checkBounds(addr, size, limit, (msg) -> new WasmRuntimeException(msg));
    //    }

    private static RuntimeException throwOutOfBounds(int addr, int size, int limit) {
        var errorMsg =
                "out of bounds memory access: attempted to access address: "
                        + addr
                        + " but limit is: "
                        + limit
                        + " and size: "
                        + size;
        return new WasmRuntimeException(errorMsg);
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
            System.arraycopy(data, 0, buffer, addr + offset, size);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, size, sizeInBytes());
        }
    }

    @Override
    public byte read(int addr) {
        // checkBounds(addr, 1, (PAGE_SIZE * nPages));
        try {
            return buffer[addr];
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 1, sizeInBytes());
        }
    }

    @Override
    public byte[] readBytes(int addr, int len) {
        // checkBounds(addr, len, (PAGE_SIZE * nPages));
        try {
            var bytes = new byte[len];
            System.arraycopy(buffer, addr, bytes, 0, len);
            return bytes;
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 1, sizeInBytes());
        }
    }

    @Override
    public void writeI32(int addr, int data) {
        // checkBounds(addr, 4, (PAGE_SIZE * nPages));
        try {
            buffer.putInt(addr, data);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 4, sizeInBytes());
        }
    }

    @Override
    public int readInt(int addr) {
        // checkBounds(addr, 4, (PAGE_SIZE * nPages));
        try {
            return buffer.getInt(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 4, sizeInBytes());
        }
    }

    @Override
    public void writeLong(int addr, long data) {
        // checkBounds(addr, 8, (PAGE_SIZE * nPages));
        try {
            buffer.putLong(addr, data);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 8, sizeInBytes());
        }
    }

    @Override
    public long readLong(int addr) {
        // checkBounds(addr, 8, (PAGE_SIZE * nPages));
        try {
            return buffer.getLong(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 8, sizeInBytes());
        }
    }

    @Override
    public void writeShort(int addr, short data) {
        // checkBounds(addr, 2, (PAGE_SIZE * nPages));
        try {
            buffer.putShort(addr, data);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 2, sizeInBytes());
        }
    }

    @Override
    public short readShort(int addr) {
        // checkBounds(addr, 2, (PAGE_SIZE * nPages));
        try {
            return buffer.getShort(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 2, sizeInBytes());
        }
    }

    @Override
    public long readU16(int addr) {
        // checkBounds(addr, 2, (PAGE_SIZE * nPages));
        try {
            return buffer.getShort(addr) & 0xffff;
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 2, sizeInBytes());
        }
    }

    @Override
    public void writeByte(int addr, byte data) {
        // checkBounds(addr, 1, (PAGE_SIZE * nPages));
        try {
            buffer.put(addr, data);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 1, sizeInBytes());
        }
    }

    @Override
    public void writeF32(int addr, float data) {
        // checkBounds(addr, 4, (PAGE_SIZE * nPages));
        try {
            buffer.putFloat(addr, data);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 4, sizeInBytes());
        }
    }

    @Override
    public long readF32(int addr) {
        // checkBounds(addr, 4, (PAGE_SIZE * nPages));
        try {
            return buffer.getInt(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 4, sizeInBytes());
        }
    }

    @Override
    public float readFloat(int addr) {
        // checkBounds(addr, 4, (PAGE_SIZE * nPages));
        try {
            return buffer.getFloat(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 4, sizeInBytes());
        }
    }

    @Override
    public void writeF64(int addr, double data) {
        // checkBounds(addr, 8, (PAGE_SIZE * nPages));
        try {
            buffer.putDouble(addr, data);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 8, sizeInBytes());
        }
    }

    @Override
    public double readDouble(int addr) {
        // checkBounds(addr, 8, (PAGE_SIZE * nPages));
        try {
            return buffer.getDouble(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 8, sizeInBytes());
        }
    }

    @Override
    public long readF64(int addr) {
        // checkBounds(addr, 8, (PAGE_SIZE * nPages));
        try {
            return buffer.getLong(addr);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(addr, 8, sizeInBytes());
        }
    }

    @Override
    public void zero() {
        fill((byte) 0, 0, (PAGE_SIZE * nPages));
    }

    @Override
    @SuppressWarnings("ByteBufferBackingArray")
    public void fill(byte value, int fromIndex, int toIndex) {
        // checkBounds(fromIndex, toIndex - fromIndex, (PAGE_SIZE * nPages));
        // see https://appsintheopen.com/posts/53-resetting-bytebuffers-to-zero-in-java
        try {
            Arrays.fill(buffer.array(), fromIndex, toIndex, value);
            buffer.position(0);
        } catch (IndexOutOfBoundsException
                | BufferOverflowException
                | BufferUnderflowException
                | IllegalArgumentException
                | NegativeArraySizeException e) {
            throw throwOutOfBounds(fromIndex, toIndex - fromIndex, sizeInBytes());
        }
    }

    @Override
    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }
}
