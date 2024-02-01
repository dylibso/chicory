package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.Machine.computeConstantValue;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import com.dylibso.chicory.wasm.types.Value;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents the linear memory in the Wasm program. Can be shared
 * reference b/w the host and the guest.
 */
public final class Memory {

    /**
     * A WebAssembly page size is 64KiB = 65,536 bits.
     */
    public static final int PAGE_SIZE = 2 << 15;

    private final MemoryLimits limits;

    private DataSegment[] dataSegments;

    private ByteBuffer buffer;

    private int nPages;

    public Memory(MemoryLimits limits) {
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
    public int pages() {
        return nPages;
    }

    public int grow(int size) {

        var prevPages = nPages;
        var numPages = prevPages + size;

        if (numPages > limits.maximumPages()) {
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

    public int initialPages() {
        return this.limits.initialPages();
    }

    public int maximumPages() {
        return this.limits.maximumPages();
    }

    /**
     * This zeros out the memory and re-writes the data segments
     * TODO - there is probably a more efficient way to handle this and do we need to do this?
     */
    public void initialize(Instance instance, DataSegment[] dataSegments) {
        this.dataSegments = dataSegments;
        this.zero();

        if (dataSegments == null) {
            return;
        }

        for (var s : dataSegments) {
            if (s instanceof ActiveDataSegment) {
                var segment = (ActiveDataSegment) s;
                var offsetExpr = segment.offsetInstructions();
                var data = segment.data();
                var offset = computeConstantValue(instance, offsetExpr).asInt();
                write(offset, data);
            } else if (s instanceof PassiveDataSegment) {
                // System.out.println("Skipping passive segment " + s);
            } else {
                throw new ChicoryException("Data segment should be active or passive: " + s);
            }
        }
    }

    public void initPassiveSegment(int segmentId, int dest, int offset, int size) {
        var segment = dataSegments[segmentId];
        if (!(segment instanceof PassiveDataSegment)) {
            // Wasm test suite expects this trap message, even though it would be
            // more informative to specifically identify the segment type mismatch
            throw new WASMRuntimeException("out of bounds memory access");
        }
        write(dest, segment.data(), offset, size);
    }

    public void writeString(int offset, String data) {
        write(offset, data.getBytes(StandardCharsets.UTF_8));
    }

    public String readString(int addr, int len) {
        return new String(readBytes(addr, len), StandardCharsets.UTF_8);
    }

    public void write(int addr, byte[] data) {
        write(addr, data, 0, data.length);
    }

    public void write(int addr, byte[] data, int offset, int size) {
        try {
            buffer.position(addr);
            buffer.put(data, offset, size);
        } catch (Exception e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public byte read(int addr) {
        try {
            return buffer.get(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public byte[] readBytes(int addr, int len) {
        try {
            var bytes = new byte[len];
            buffer.position(addr);
            buffer.get(bytes);
            return bytes;
        } catch (Exception e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void write(int addr, Value data) {
        write(addr, data.data());
    }

    public void writeI32(int addr, int data) {
        try {
            buffer.putInt(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readI32(int addr) {
        try {
            return Value.i32(buffer.getInt(addr));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readU32(int addr) {
        try {
            return Value.i32(buffer.getInt(addr) & 0xffffffffL);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void writeLong(int addr, long data) {
        try {
            buffer.putLong(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readI64(int addr) {
        try {
            return Value.i64(buffer.getLong(addr));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void writeShort(int addr, short data) {
        try {
            buffer.putShort(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readI16(int addr) {
        try {
            return Value.i32(buffer.getShort(addr));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readU16(int addr) {
        try {
            return Value.i32(buffer.getShort(addr) & 0xffff);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void writeByte(int addr, byte data) {
        try {
            buffer.put(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readU8(int addr) {
        try {
            return Value.i32(read(addr) & 0xFF);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readI8(int addr) {
        try {
            return Value.i32(read(addr));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void writeF32(int addr, float data) {
        try {
            buffer.putFloat(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readF32(int addr) {
        try {
            return Value.f32(buffer.getInt(addr));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void writeF64(int addr, double data) {
        try {
            buffer.putDouble(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value readF64(int addr) {
        try {
            return Value.f64(buffer.getLong(addr));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void zero() {
        this.fill((byte) 0);
    }

    public void fill(byte value) {
        fill(value, 0, buffer.capacity());
    }

    public void fill(byte value, int fromIndex, int toIndex) {
        try {
            // see https://appsintheopen.com/posts/53-resetting-bytebuffers-to-zero-in-java
            Arrays.fill(buffer.array(), fromIndex, toIndex, value);
            buffer.position(0);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void copy(int dest, int src, int size) {
        write(dest, readBytes(src, size));
    }

    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }
}
