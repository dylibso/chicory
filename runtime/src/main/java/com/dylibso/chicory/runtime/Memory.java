package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.*;
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

    private final DataSegment[] dataSegments;

    private ByteBuffer buffer;

    private int nPages;

    public Memory(MemoryLimits limits) {
        this(limits, null);
    }

    public Memory(MemoryLimits limits, DataSegment[] dataSegments) {
        this.limits = limits;
        this.buffer = allocateByteBuffer(PAGE_SIZE * limits.getInitial());
        this.nPages = limits.getInitial();
        this.dataSegments = dataSegments;
        this.reinstantiate();
    }

    private static ByteBuffer allocateByteBuffer(int capacity) {
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Gets the size of the memory in number of pages
     */
    public int getSize() {
        return nPages;
    }

    public int grow(int size) {

        var prevPages = nPages;
        var numPages = prevPages + size;

        if (numPages > limits.getMaximum()) {
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

    public int getInitialSize() {
        return this.limits.getInitial();
    }

    /**
     * This zeros out the memory and re-writes the data segments
     * TODO - there is probably a more efficient way to handle this and do we need to do this?
     */
    public void reinstantiate() {
        this.zero();

        if (dataSegments == null) {
            return;
        }

        for (var s : dataSegments) {
            if (s instanceof ActiveDataSegment) {
                var segment = (ActiveDataSegment) s;
                var offsetExpr = segment.getOffset();
                var offsetInstr = offsetExpr[0];
                // TODO how flexible can this be? Do we need to dynamically eval the expression?
                if (offsetInstr.getOpcode() != OpCode.I32_CONST) {
                    throw new RuntimeException(
                            "Don't support data segment expressions other than i32.const yet");
                }
                var data = segment.getData();
                var offset = (int) offsetInstr.getOperands()[0];
                // System.out.println("Writing data segment " + offset + " " + new String(data));
                // TODO is there a cleaner way doing buffer.put(offset, data)?
                for (int i = 0, j = offset; i < data.length; i++, j++) {
                    this.buffer.put(j, data[i]);
                }
            } else if (s instanceof PassiveDataSegment) {
                // System.out.println("Skipping passive segment " + s);
            } else {
                throw new ChicoryException("Data segment should be active or passive: " + s);
            }
        }
    }

    public void copy(int dest, int src, int size) {
        var data = new byte[size];
        this.buffer.get(data, src, size);
        try {
            // TODO why can't i just write this array to the buffer without the loop?
            for (var i = 0; i < size; i++) {
                this.buffer.put(dest + i, data[i]);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
    }

    public void initPassiveSegment(int segmentId, int dest, int offset, int size) {
        var segment = dataSegments[segmentId];
        if (!(segment instanceof PassiveDataSegment)) {
            throw new ChicoryException(
                    "data segment with id "
                            + " is not a passive segment and cannot be initialized at runtime");
        }
        var data = segment.getData();
        // TODO why doesn't this API work?
        // this.buffer.put(target, segment.getData(), offset, size);
        int j = dest;
        try {
            for (var i = offset; i < size; i++) {
                this.buffer.put(j++, data[i]);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("out of bounds memory access");
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
        // see https://appsintheopen.com/posts/53-resetting-bytebuffers-to-zero-in-java
        Arrays.fill(this.buffer.array(), (byte) 0);
        this.buffer.position(0);
    }
}
