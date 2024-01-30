package com.dylibso.chicory.wasm.io;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A WASM input stream backed by a byte buffer.
 */
class BufferWasmInputStream extends WasmInputStream {
    private final ByteBuffer buffer;

    BufferWasmInputStream(final ByteBuffer buffer) {
        super();
        // make our position independent, and zero-based
        this.buffer = buffer.slice();
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public long position() {
        return buffer.position();
    }

    public void skip(final long count) {
        if (count <= 0) {
            return;
        }
        long newOffs = buffer.position() + count;
        if (newOffs > buffer.limit()) {
            throw new WasmEOFException();
        }
        buffer.position((int) newOffs);
    }

    public int rawByteOpt() {
        return buffer.hasRemaining() ? Byte.toUnsignedInt(buffer.get()) : -1;
    }

    public int peekRawByteOpt() {
        return buffer.hasRemaining() ? Byte.toUnsignedInt(buffer.get(buffer.position())) : -1;
    }

    public byte[] rawBytes(final int len) {
        byte[] buf = new byte[len];
        try {
            buffer.get(buf);
        } catch (BufferUnderflowException ignored) {
            throw new WasmEOFException();
        }
        return buf;
    }

    public void rawBytes(final ByteBuffer buf) {
        int srcPos = buffer.position();
        int srcLim = buffer.limit();
        int dstPos = buf.position();
        int dstLim = buf.limit();
        int copySize = dstLim - dstPos;
        if (copySize <= srcLim - srcPos) {
            // reduce limit to size of copy
            buffer.limit(srcPos + copySize);
            try {
                buf.put(buffer);
            } finally {
                // restore limit
                buffer.limit(srcLim);
            }
        } else {
            throw new WasmEOFException();
        }
    }

    public int raw32le() {
        try {
            return buffer.getInt();
        } catch (BufferUnderflowException ignored) {
            throw new WasmEOFException();
        }
    }

    public long raw64le() {
        try {
            return buffer.getLong();
        } catch (BufferUnderflowException ignored) {
            throw new WasmEOFException();
        }
    }
}
