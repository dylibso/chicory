package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.types.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents the linear memory in the Wasm program. Can be shared
 * reference b/w the host and the guest.
 */
public class Memory {
    // 64KiB = 65,536
    public static int PAGE_SIZE = (int) Math.pow(2, 16);
    private ByteBuffer buffer;
    private MemoryLimits limits;
    private int nPages;
    private DataSegment[] dataSegments;

    public Memory(MemoryLimits limits) {
        this.limits = limits;
        this.buffer =
                ByteBuffer.allocate(PAGE_SIZE * limits.getInitial()).order(ByteOrder.LITTLE_ENDIAN);
        this.nPages = limits.getInitial();
    }

    public Memory(MemoryLimits limits, DataSegment[] dataSegments) {
        this(limits);
        this.dataSegments = dataSegments;
        this.reinstantiate();
    }

    /**
     * Gets the size of the memory in number of pages
     */
    public int getSize() {
        return nPages;
    }

    public int grow(int size) {
        var prevPages = nPages;
        var numPages = nPages + size;
        // TODO if max is null then we just let it grow as much as it wants?
        if (limits.getMaximum() != null && numPages >= limits.getMaximum())
            throw new RuntimeException("Program exceeded max pages: " + limits.getMaximum());
        var capacity = buffer.capacity() + (PAGE_SIZE * size);
        var result = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        var position = buffer.position();
        buffer.rewind();
        result.put(buffer);
        result.position(position);
        buffer = result;
        nPages = numPages;
        return prevPages;
    }

    public int getInitialSize() {
        return this.limits.getInitial();
    }

    /**
     * This zeros out the memory and re-writes the data segments
     * TODO - there is probably a more efficient way to handle this and do we need to do this?
     */
    public void reinstantiate() {
        this.zero();
        for (var segment : dataSegments) {
            var offsetExpr = segment.getOffset();
            var offsetInstr = offsetExpr[0];
            // TODO how flexible can this be? Do we need to dynamically eval the expression?
            if (offsetInstr.getOpcode() != OpCode.I32_CONST) {
                throw new RuntimeException(
                        "Don't support data segment expressions other than i32.const yet");
            }
            var data = segment.getData();
            var offset = (int) offsetInstr.getOperands()[0];
            System.out.println("Writing data segment " + offset + " " + new String(data));
            for (int i = 0, j = offset; i < data.length; i++, j++) {
                this.buffer.put(j, data[i]);
            }
        }
    }

    public String getString(int offset, int len) {
        try {
            var data = new byte[len];
            for (int i = 0, j = offset; i < len; i++, j++) {
                data[i] = this.buffer.get(j);
            }

            return new String(data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void put(int offset, String data) {
        try {
            var bytes = data.getBytes(StandardCharsets.UTF_8);
            for (int i = 0, j = offset; i < bytes.length; i++, j++) {
                byte b = bytes[i];
                this.buffer.put(j, b);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void put(int offset, byte[] data) {
        try {
            // System.out.println("mem-write@" + offset + " " + data);
            for (int i = 0, j = offset; i < data.length; i++, j++) {
                byte b = data[i];
                this.buffer.put(j, b);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void put(int offset, Value data) {
        try {
            var bytes = data.getData();
            for (int i = 0, j = offset; i < bytes.length; i++, j++) {
                this.buffer.put(j, bytes[i]);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void putI32(int offset, int data) {
        try {
            this.buffer.putInt(offset, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void putF32(int offset, float data) {
        try {
            this.buffer.putFloat(offset, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void putF64(int offset, double data) {
        try {
            this.buffer.putDouble(offset, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void putShort(int offset, short data) {
        try {
            this.buffer.putShort(offset, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void putI64(int offset, long data) {
        try {
            this.buffer.putLong(offset, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void putByte(int offset, byte data) {
        try {
            this.buffer.put(offset, data);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public byte get(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return this.buffer.get(offset);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getI32(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i32(this.buffer.getInt(offset));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getU32(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i64(this.buffer.getLong(offset) & 0xffffffffL);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getI64(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i64(this.buffer.getLong(offset));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getI16(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i32(this.buffer.getShort(offset));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getU16(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i32(this.buffer.getInt(offset) & 0xffff);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getI8U(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i32(this.buffer.get(offset) & 0xff);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getI8(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.i32(this.buffer.get(offset));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getF32(int offset) {
        // System.out.println("mem-read@" + offset);
        try {
            return Value.f32(this.buffer.getInt(offset));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public Value getF64(int offset) {
        try {
            // System.out.println("mem-read@" + offset);
            return Value.f64(this.buffer.getLong(offset));
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void zero() {
        Arrays.fill(this.buffer.array(), (byte) 0);
        this.buffer.position(0);
    }
}
