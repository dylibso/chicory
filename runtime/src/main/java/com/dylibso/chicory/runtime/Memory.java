package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static java.lang.Math.min;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.exceptions.UninstantiableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
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
public final class Memory {

    /**
     * A WebAssembly page size is 64KiB = 65,536 bytes.
     */
    public static final int PAGE_SIZE = 65536;

    /**
     * Maximum number of pages allowed by the runtime.
     * WASM supports 2^16 pages, but we must limit based on the maximum JVM array size.
     * This limit is {@code Integer.MAX_VALUE / PAGE_SIZE}.
     */
    public static final int RUNTIME_MAX_PAGES = 32767;

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

    public int initialPages() {
        return this.limits.initialPages();
    }

    public int maximumPages() {
        return min(this.limits.maximumPages(), RUNTIME_MAX_PAGES);
    }

    /**
     * This zeros out the memory and re-writes the data segments
     * TODO - there is probably a more efficient way to handle this and do we need to do this?
     */
    public void initialize(Instance instance, DataSegment[] dataSegments) {
        this.dataSegments = dataSegments;
        if (dataSegments == null) {
            return;
        }

        for (var s : dataSegments) {
            if (s instanceof ActiveDataSegment) {
                var segment = (ActiveDataSegment) s;
                if (segment.index() != 0) {
                    throw new InvalidException("unknown memory " + segment.index());
                }
                var offsetExpr = segment.offsetInstructions();
                if (offsetExpr.size() > 1) {
                    throw new InvalidException(
                            "type mismatch, constant expression required, expected only one"
                                    + " initialization instruction");
                }
                var data = segment.data();
                var offsetValue = computeConstantValue(instance, offsetExpr);
                if (offsetValue.type() != ValueType.I32) {
                    throw new InvalidException(
                            "type mismatch, expected I32 but found "
                                    + offsetValue.type()
                                    + " in offset memory initialization");
                }
                var offset = offsetValue.asInt();
                write(offset, data);
            } else if (s instanceof PassiveDataSegment) {
                // Passive segment should be skipped
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

    public void writeString(int offset, String data, Charset charSet) {
        write(offset, data.getBytes(charSet));
    }

    public void writeString(int offset, String data) {
        writeString(offset, data, StandardCharsets.UTF_8);
    }

    public String readString(int addr, int len) {
        return readString(addr, len, StandardCharsets.UTF_8);
    }

    public String readString(int addr, int len, Charset charSet) {
        return new String(readBytes(addr, len), charSet);
    }

    public void writeCString(int offset, String str) {
        writeCString(offset, str, StandardCharsets.UTF_8);
    }

    public void writeCString(int offset, String str, Charset charSet) {
        writeString(offset, str + '\0', charSet);
    }

    public String readCString(int addr, Charset charSet) {
        int c = addr;
        while (read(c) != '\0') {
            c++;
        }
        return new String(readBytes(addr, c - addr), charSet);
    }

    public String readCString(int addr) {
        return readCString(addr, StandardCharsets.UTF_8);
    }

    public void write(int addr, Value data) {
        write(addr, data.data());
    }

    public void write(int addr, byte[] data) {
        write(addr, data, 0, data.length);
    }

    public void write(int addr, byte[] data, int offset, int size) {
        try {
            buffer.position(addr);
            buffer.put(data, offset, size);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | BufferOverflowException e) {
            throw new UninstantiableException("out of bounds memory access");
        }
    }

    public byte read(int addr) {
        try {
            return buffer.get(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new UninstantiableException("out of bounds memory access");
        }
    }

    public byte[] readBytes(int addr, int len) {
        try {
            var bytes = new byte[len];
            buffer.position(addr);
            buffer.get(bytes);
            return bytes;
        } catch (IllegalArgumentException
                | BufferUnderflowException
                | NegativeArraySizeException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void writeI32(int addr, int data) {
        try {
            buffer.putInt(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public int readInt(int addr) {
        try {
            return buffer.getInt(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public long readI32(int addr) {
        return readInt(addr);
    }

    public long readU32(int addr) {
        return Integer.toUnsignedLong(readInt(addr));
    }

    public void writeLong(int addr, long data) {
        try {
            buffer.putLong(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public long readLong(int addr) {
        try {
            return buffer.getLong(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public long readI64(int addr) {
        return readLong(addr);
    }

    public void writeShort(int addr, short data) {
        try {
            buffer.putShort(addr, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public short readShort(int addr) {
        try {
            return buffer.getShort(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public long readI16(int addr) {
        return readShort(addr);
    }

    public long readU16(int addr) {
        try {
            return buffer.getShort(addr) & 0xffff;
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

    public long readU8(int addr) {
        try {
            return read(addr) & 0xFF;
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public long readI8(int addr) {
        try {
            return read(addr);
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

    public long readF32(int addr) {
        try {
            return buffer.getInt(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public float readFloat(int addr) {
        try {
            return buffer.getFloat(addr);
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

    public double readDouble(int addr) {
        try {
            return buffer.getDouble(addr);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public long readF64(int addr) {
        try {
            return buffer.getLong(addr);
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

    @SuppressWarnings("ByteBufferBackingArray")
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

    public DataSegment[] dataSegments() {
        return dataSegments;
    }

    public MemoryLimits limits() {
        return limits;
    }
}
