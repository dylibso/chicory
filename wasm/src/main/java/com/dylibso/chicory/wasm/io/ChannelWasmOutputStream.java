package com.dylibso.chicory.wasm.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

/**
 *
 */
abstract class ChannelWasmOutputStream extends BufferWasmOutputStream {

    abstract WritableByteChannel channel() throws WasmIOException;

    ChannelWasmOutputStream() {
        super(ByteBuffer.allocateDirect(8192).order(ByteOrder.LITTLE_ENDIAN));
    }

    public void rawByte(final int val) throws WasmIOException {
        if (!buffer.hasRemaining()) {
            flush();
        }
        super.rawByte((byte) val);
    }

    public void rawBytes(final byte[] bytes, int off, int len) throws WasmIOException {
        int rem = buffer.remaining();
        int cnt = Math.min(len, rem);
        super.rawBytes(bytes, off, cnt);
        while (len > 0) {
            flush();
            len -= cnt;
            off += cnt;
            rem = buffer.remaining();
            cnt = Math.min(len, rem);
            super.rawBytes(bytes, off, cnt);
        }
    }

    void flush() throws WasmIOException {
        buffer.flip();
        try {
            while (buffer.hasRemaining()) {
                //noinspection resource
                channel().write(buffer);
            }
        } catch (IOException e) {
            throw new WasmIOException(e);
        } finally {
            buffer.compact();
        }
    }
}
