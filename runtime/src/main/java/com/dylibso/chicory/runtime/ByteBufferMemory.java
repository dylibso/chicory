package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static java.lang.Math.min;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Represents the linear memory in the Wasm program. Can be shared
 * reference b/w the host and the guest.
 */
public final class ByteBufferMemory implements Memory {

    private final MemoryLimits limits;

    private DataSegment[] dataSegments;

    private ByteBuffer buffer;

    private int nPages;

    public ByteBufferMemory(MemoryLimits limits) {
        this.limits = limits;
        this.buffer = allocateByteBuffer(PAGE_SIZE * limits.initialPages());
        this.nPages = limits.initialPages();
    }

    private static ByteBuffer allocateByteBuffer(int capacity) {
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
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

        var oldBuffer = buffer;
        var newBuffer = allocateByteBuffer(oldBuffer.capacity() + (PAGE_SIZE * size));
        var position = oldBuffer.position();
        oldBuffer.rewind();
        newBuffer.put(oldBuffer);
        newBuffer.position(position);

        buffer = newBuffer;
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
                        buffer.limit(),
                        (msg) -> new UninstantiableException(msg));
                buffer.position(offset);
                buffer.put(data, 0, data.length);
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
                            + limit;
            throw exceptionFactory.apply(errorMsg);
        }
    }

    private static void checkBounds(int addr, int size, int limit) {
        checkBounds(addr, size, limit, (msg) -> new WasmRuntimeException(msg));
    }

    @Override
    public void initPassiveSegment(int segmentId, int dest, int offset, int size) {
        var segment = dataSegments[segmentId];
        write(dest, segment.data(), offset, size);
    }

    @Override
    public void write(int addr, byte[] data, int offset, int size) {
        checkBounds(addr, size, buffer.limit());
        if (!(data.length >= (offset + size))) {
            throw new WasmRuntimeException(
                    "out of bounds memory access: attempted to access data with length: "
                            + data.length
                            + " at address: "
                            + (offset + size));
        }
        buffer.position(addr);
        buffer.put(data, offset, size);
    }

    @Override
    public byte read(int addr) {
        checkBounds(addr, 1, buffer.limit());
        return buffer.get(addr);
    }

    @Override
    public byte[] readBytes(int addr, int len) {
        checkBounds(addr, len, buffer.limit());
        var bytes = new byte[len];
        buffer.position(addr);
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public void writeI32(int addr, int data) {
        checkBounds(addr, 4, buffer.limit());
        buffer.putInt(addr, data);
    }

    @Override
    public int readInt(int addr) {
        checkBounds(addr, 4, buffer.limit());
        return buffer.getInt(addr);
    }

    @Override
    public void writeLong(int addr, long data) {
        checkBounds(addr, 8, buffer.limit());
        buffer.putLong(addr, data);
    }

    @Override
    public long readLong(int addr) {
        checkBounds(addr, 8, buffer.limit());
        return buffer.getLong(addr);
    }

    @Override
    public void writeShort(int addr, short data) {
        checkBounds(addr, 2, buffer.limit());
        buffer.putShort(addr, data);
    }

    @Override
    public short readShort(int addr) {
        checkBounds(addr, 2, buffer.limit());
        return buffer.getShort(addr);
    }

    @Override
    public long readU16(int addr) {
        checkBounds(addr, 2, buffer.limit());
        return buffer.getShort(addr) & 0xffff;
    }

    @Override
    public void writeByte(int addr, byte data) {
        checkBounds(addr, 1, buffer.limit());
        buffer.put(addr, data);
    }

    @Override
    public void writeF32(int addr, float data) {
        checkBounds(addr, 4, buffer.limit());
        buffer.putFloat(addr, data);
    }

    @Override
    public long readF32(int addr) {
        checkBounds(addr, 4, buffer.limit());
        return buffer.getInt(addr);
    }

    @Override
    public float readFloat(int addr) {
        checkBounds(addr, 4, buffer.limit());
        return buffer.getFloat(addr);
    }

    @Override
    public void writeF64(int addr, double data) {
        checkBounds(addr, 8, buffer.limit());
        buffer.putDouble(addr, data);
    }

    @Override
    public double readDouble(int addr) {
        checkBounds(addr, 8, buffer.limit());
        return buffer.getDouble(addr);
    }

    @Override
    public long readF64(int addr) {
        checkBounds(addr, 8, buffer.limit());
        return buffer.getLong(addr);
    }

    @Override
    public void zero() {
        fill((byte) 0, 0, buffer.capacity());
    }

    @Override
    @SuppressWarnings("ByteBufferBackingArray")
    public void fill(byte value, int fromIndex, int toIndex) {
        checkBounds(fromIndex, toIndex - fromIndex, buffer.limit());
        // see https://appsintheopen.com/posts/53-resetting-bytebuffers-to-zero-in-java
        Arrays.fill(buffer.array(), fromIndex, toIndex, value);
        buffer.position(0);
    }

    @Override
    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }
}
