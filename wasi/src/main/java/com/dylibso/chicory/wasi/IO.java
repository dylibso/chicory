package com.dylibso.chicory.wasi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Provides implementations of null stream utilities that are compatible with Android
 * devices running API levels below 33. This class contains implementations equivalent to
 * {@code OutputStream.nullOutputStream()} and {@code InputStream.nullInputStream()}
 * which were introduced in Android API Level 33.
 */
final class IO {
    private IO() {}

    /**
     * Returns an {@link OutputStream} that discards all bytes written to it.
     * This implementation provides compatibility for Android devices running below API Level 33,
     * where {@code OutputStream.nullOutputStream()} is not available.
     * <p>
     * This implementation is copied from {@code OutputStream.nullOutputStream()}.
     *
     * @return an output stream that discards all bytes written to it
     */
    public static OutputStream nullOutputStream() {
        return new OutputStream() {
            private volatile boolean closed;

            private void ensureOpen() throws IOException {
                if (closed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public void write(int b) throws IOException {
                ensureOpen();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                ensureOpen();
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }

    /**
     * Returns an {@link InputStream} that contains no bytes.
     * This implementation provides compatibility for Android devices running below API Level 33,
     * where {@code InputStream.nullInputStream()} is not available.
     * <p>
     * This implementation is copied from {@code InputStream.nullInputStream()}.
     *
     * @return an input stream that contains no bytes
     */
    public static InputStream nullInputStream() {
        return new InputStream() {
            private volatile boolean closed;

            private void ensureOpen() throws IOException {
                if (closed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public int available() throws IOException {
                ensureOpen();
                return 0;
            }

            @Override
            public int read() throws IOException {
                ensureOpen();
                return -1;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (len == 0) {
                    return 0;
                }
                ensureOpen();
                return -1;
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                ensureOpen();
                return new byte[0];
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                ensureOpen();
                return 0;
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                if (len < 0) {
                    throw new IllegalArgumentException("len < 0");
                }
                ensureOpen();
                return new byte[0];
            }

            @Override
            public long skip(long n) throws IOException {
                ensureOpen();
                return 0L;
            }

            /* This method was added in Java 12+ */
            // @Override
            // public void skipNBytes(long n) throws IOException {
            //     ensureOpen();
            //     if (n > 0) {
            //         throw new EOFException();
            //     }
            // }

            @Override
            public long transferTo(OutputStream out) throws IOException {
                Objects.requireNonNull(out);
                ensureOpen();
                return 0L;
            }

            @Override
            public void close() throws IOException {
                closed = true;
            }
        };
    }
}
