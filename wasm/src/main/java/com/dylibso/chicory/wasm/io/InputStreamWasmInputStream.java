package com.dylibso.chicory.wasm.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A WASM input stream based on {@link InputStream}.
 */
class InputStreamWasmInputStream extends WasmInputStream {

    private long position;
    private final BufferedInputStream is;
    private final ByteBuffer littleBuffer =
            ByteBuffer.wrap(new byte[8]).order(ByteOrder.LITTLE_ENDIAN);

    InputStreamWasmInputStream(final InputStream is) {
        this.is =
                is instanceof BufferedInputStream
                        ? (BufferedInputStream) is
                        : new BufferedInputStream(is);
    }

    public long position() {
        return position;
    }

    public void skip(long count) {
        // todo: java 12+: is.skipNBytes(count);
        if (count <= 0) {
            return;
        }

        try {
            long t;
            while (count > 0) {
                t = is.skip(count);
                if (t == 0) {
                    if (is.read() == -1) {
                        throw new WasmEOFException();
                    }
                    t = 1;
                }
                count -= t;
                position += t;
            }
        } catch (IOException e) {
            throw new WasmIOException(e);
        }
    }

    public int rawByteOpt() {
        try {
            int res = is.read();
            if (res >= 0) {
                position++;
                return res & 0xff;
            } else {
                return -1;
            }
        } catch (IOException e) {
            throw new WasmIOException(e);
        }
    }

    public int peekRawByteOpt() {
        try {
            is.mark(1);
            int res = is.read();
            is.reset();
            return res;
        } catch (IOException e) {
            throw new WasmIOException(e);
        }
    }

    private static final byte[] NO_BYTES = new byte[0];

    public byte[] rawBytes(final int len) {
        if (len == 0) {
            return NO_BYTES;
        }
        // todo: java 12+: return is.readNBytes(len);
        return readAll(new byte[len], 0, len);
    }

    public void rawBytes(final ByteBuffer buf) {
        if (buf.hasArray()) {
            byte[] array = buf.array();
            int pos = buf.position();
            int offs = buf.arrayOffset() + pos;
            int lim = buf.limit();
            int cnt = lim - pos;
            readAll(array, offs, cnt);
            buf.position(pos + cnt);
        } else {
            // slow path
            while (buf.hasRemaining()) {
                buf.put((byte) rawByte());
            }
        }
    }

    private byte[] readAll(final byte[] buf, final int off, final int len) {
        int res;
        for (int p = 0; p < len; p += res) {
            try {
                res = is.read(buf, p + off, len - p);
            } catch (IOException e) {
                throw new WasmIOException(e);
            }
            if (res == -1) {
                throw new WasmEOFException();
            }
            position += res;
        }
        return buf;
    }

    public int raw32le() {
        readAll(littleBuffer.array(), 0, 4);
        return littleBuffer.getInt(0);
    }

    public long raw64le() {
        readAll(littleBuffer.array(), 0, 8);
        return littleBuffer.getLong(0);
    }

    public void close() throws WasmIOException {
        try {
            is.close();
        } catch (IOException e) {
            throw new WasmIOException(e);
        }
    }
}
