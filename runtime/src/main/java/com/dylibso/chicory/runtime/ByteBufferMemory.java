package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static java.lang.Math.min;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents the linear memory in the Wasm program. Can be shared
 * reference b/w the host and the guest.
 */
public class ByteBufferMemory implements Memory {

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
                var offset = computeConstantValue(instance, offsetExpr);
                write((int) offset, data);
            } else if (s instanceof PassiveDataSegment) {
                // Passive segment should be skipped
            } else {
                throw new ChicoryException("Data segment should be active or passive: " + s);
            }
        }
    }

    @Override
    public void initPassiveSegment(int segmentId, int dest, int offset, int size) {
        var segment = dataSegments[segmentId];
        if (!(segment instanceof PassiveDataSegment)) {
            // Wasm test suite expects this trap message, even though it would be
            // more informative to specifically identify the segment type mismatch
            throw new WasmRuntimeException("out of bounds memory access");
        }
        write(dest, segment.data(), offset, size);
    }

    @Override
    public void writeString(int offset, String data, Charset charSet) {
        write(offset, data.getBytes(charSet));
    }

    @Override
    public void writeString(int offset, String data) {
        writeString(offset, data, StandardCharsets.UTF_8);
    }

    @Override
    public String readString(int addr, int len) {
        return readString(addr, len, StandardCharsets.UTF_8);
    }

    @Override
    public String readString(int addr, int len, Charset charSet) {
        return new String(readBytes(addr, len), charSet);
    }

    @Override
    public void writeCString(int offset, String str) {
        writeCString(offset, str, StandardCharsets.UTF_8);
    }

    @Override
    public void writeCString(int offset, String str, Charset charSet) {
        writeString(offset, str + '\0', charSet);
    }

    @Override
    public String readCString(int addr, Charset charSet) {
        int c = addr;
        while (read(c) != '\0') {
            c++;
        }
        return new String(readBytes(addr, c - addr), charSet);
    }

    @Override
    public String readCString(int addr) {
        return readCString(addr, StandardCharsets.UTF_8);
    }

    @Override
    public void write(int addr, byte[] data) {
        write(addr, data, 0, data.length);
    }

    @Override
    public void write(int addr, byte[] data, int offset, int size) {
        try {
            buffer.position(addr);
            buffer.put(data, offset, size);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | BufferOverflowException e) {
            throw new UninstantiableException("out of bounds memory access");
        }
    }

    @Override
    public byte read(int addr) {
        try {
            return buffer.get(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new UninstantiableException("out of bounds memory access");
        }
    }

    @Override
    public byte[] readBytes(int addr, int len) {
        try {
            var bytes = new byte[len];
            buffer.position(addr);
            buffer.get(bytes);
            return bytes;
        } catch (IllegalArgumentException
                | BufferUnderflowException
                | NegativeArraySizeException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public void writeI32(int addr, int data) {
        try {
            buffer.putInt(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public int readInt(int addr) {
        try {
            return buffer.getInt(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readI32(int addr) {
        return readInt(addr);
    }

    @Override
    public long readU32(int addr) {
        return Integer.toUnsignedLong(readInt(addr));
    }

    @Override
    public void writeLong(int addr, long data) {
        try {
            buffer.putLong(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readLong(int addr) {
        try {
            return buffer.getLong(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readI64(int addr) {
        return readLong(addr);
    }

    @Override
    public void writeShort(int addr, short data) {
        try {
            buffer.putShort(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public short readShort(int addr) {
        try {
            return buffer.getShort(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readI16(int addr) {
        return readShort(addr);
    }

    @Override
    public long readU16(int addr) {
        try {
            return buffer.getShort(addr) & 0xffff;
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public void writeByte(int addr, byte data) {
        try {
            buffer.put(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readU8(int addr) {
        try {
            return read(addr) & 0xFF;
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readI8(int addr) {
        try {
            return read(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public void writeF32(int addr, float data) {
        try {
            buffer.putFloat(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readF32(int addr) {
        try {
            return buffer.getInt(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public float readFloat(int addr) {
        try {
            return buffer.getFloat(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public void writeF64(int addr, double data) {
        try {
            buffer.putDouble(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public double readDouble(int addr) {
        try {
            return buffer.getDouble(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public long readF64(int addr) {
        try {
            return buffer.getLong(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public void zero() {
        this.fill((byte) 0);
    }

    @Override
    public void fill(byte value) {
        fill(value, 0, buffer.capacity());
    }

    @Override
    @SuppressWarnings("ByteBufferBackingArray")
    public void fill(byte value, int fromIndex, int toIndex) {
        try {
            // see https://appsintheopen.com/posts/53-resetting-bytebuffers-to-zero-in-java
            Arrays.fill(buffer.array(), fromIndex, toIndex, value);
            buffer.position(0);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new WasmRuntimeException("out of bounds memory access");
        }
    }

    @Override
    public void copy(int dest, int src, int size) {
        write(dest, readBytes(src, size));
    }

    @Override
    public void drop(int segment) {
        dataSegments[segment] = PassiveDataSegment.EMPTY;
    }
}
