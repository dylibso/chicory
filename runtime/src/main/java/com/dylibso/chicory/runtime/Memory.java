package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
        this.buffer = ByteBuffer.allocate(PAGE_SIZE * limits.getInitial()).order(ByteOrder.LITTLE_ENDIAN);
        this.nPages = limits.getInitial();
    }

    public Memory(MemoryLimits limits, DataSegment[] dataSegments) {
        this(limits);
        this.dataSegments = dataSegments;
        this.reinstantiate();
    }

    public void grow() {
        // TODO if max is null then we just let it grow as much as it wants?
        if (limits.getMaximum() != null && this.nPages > limits.getMaximum()) throw new RuntimeException("Program exceeded max pages: " + limits.getMaximum());
        var capacity = this.buffer.capacity() + PAGE_SIZE;
        var result = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        var position = this.buffer.position();
        this.buffer.rewind();
        result.put(this.buffer);
        result.position(position);
        this.buffer = result;
        this.nPages++;
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
                throw new RuntimeException("Don't support data segment expressions other than i32.const yet");
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
        var data = new byte[len];
        for (int i = 0, j = offset; i < len; i++, j++) {
            data[i] = this.buffer.get(j);
        }

        return new String(data);
    }

    public void put(int offset, String data) {
        var bytes = data.getBytes(StandardCharsets.UTF_8);
        for (int i = 0, j = offset; i < bytes.length; i++, j++) {
            byte b = bytes[i];
            this.buffer.put(j, b);
        }
    }

    public void put(int offset, byte[] data) {
        //System.out.println("mem-write@" + offset + " " + data);
        for (int i = 0, j = offset; i < data.length; i++, j++) {
            byte b = data[i];
            this.buffer.put(j, b);
        }
    }

    public void put(int offset, Value data) {
        var bytes = data.getData();
        for (int i = 0, j = offset; i < bytes.length; i++, j++) {
            this.buffer.put(j, bytes[i]);
        }
    }

    public void putI32(int offset, int data) {
        System.out.println("mem-write@" + offset + " " + data);
        this.buffer.putInt(offset, data);
    }

    public void putShort(int offset, short data) {
        System.out.println("mem-write@" + offset + " " + data);
        this.buffer.putShort(offset, data);
    }

    public void putI64(int offset, long data) {
        System.out.println("mem-write@" + offset + " " + data);
        this.buffer.putLong(offset, data);
    }

    public void putByte(int offset, byte data) {
        //System.out.println("mem-write@" + offset + " " + data);
        this.buffer.put(offset, data);
    }

    public byte get(int offset) {
        //System.out.println("mem-read@" + offset);
        return this.buffer.get(offset);
    }

    public Value getI32(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i32(this.buffer.getInt(offset));
    }

    public Value getU32(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i64(this.buffer.getLong(offset) & 0xffffffffL);
    }

    public Value getI64(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i64(this.buffer.getLong(offset));
    }

    public Value getI16(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i32(this.buffer.getShort(offset));
    }

    public Value getU16(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i32(this.buffer.getInt(offset) & 0xffff);
    }

    public Value getI8U(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i32(this.buffer.get(offset) & 0xff);
    }

    public Value getI8(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.i32(this.buffer.get(offset));
    }

    public Value getF32(int offset) {
        //System.out.println("mem-read@" + offset);
        return Value.f32(this.buffer.getInt(offset));
    }

    public Value getF64(int offset) {
        System.out.println("mem-read@" + offset);
        return Value.f64(this.buffer.getLong(offset));
    }

    public void zero() {
        Arrays.fill(this.buffer.array(), (byte)0);
        this.buffer.position(0);
    }
}
