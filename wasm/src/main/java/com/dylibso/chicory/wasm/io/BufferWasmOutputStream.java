package com.dylibso.chicory.wasm.io;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 */
class BufferWasmOutputStream extends WasmOutputStream {
    final ByteBuffer buffer;
    long position;

    BufferWasmOutputStream(final ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("Buffer must have little-endian order");
        }
        this.buffer = buffer;
    }

    public long position() {
        return position;
    }

    public void rawByte(final int val) throws WasmIOException {
        try {
            buffer.put((byte) val);
        } catch (BufferOverflowException e) {
            throw new WasmIOException(e);
        }
        position++;
    }

    public void rawBytes(final byte[] bytes, final int off, final int len) throws WasmIOException {
        if (len <= 0) {
            return;
        }
        buffer.put(bytes, off, len);
        position += len;
    }

    public void rawBytes(final ByteBuffer buffer) throws WasmIOException {
        this.buffer.put(buffer);
    }

    public void raw16le(final int val) throws WasmIOException {
        if (buffer.remaining() < 2) {
            super.raw16le(val);
        } else {
            buffer.putShort((short) val);
            position += 2;
        }
    }

    public void raw32le(final int val) throws WasmIOException {
        if (buffer.remaining() < 4) {
            super.raw32le(val);
        } else {
            buffer.putInt(val);
            position += 4;
        }
    }

    public void raw64le(final long val) throws WasmIOException {
        if (buffer.remaining() < 8) {
            super.raw64le(val);
        } else {
            buffer.putLong(val);
            position += 8;
        }
    }
}
