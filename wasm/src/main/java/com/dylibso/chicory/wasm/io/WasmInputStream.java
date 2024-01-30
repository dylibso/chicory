package com.dylibso.chicory.wasm.io;

import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A stream which can read WASM content.
 */
public abstract class WasmInputStream implements Closeable {
    /**
     * Construct a new instance from a file.
     *
     * @param path the file path (must not be {@code null})
     * @return the new stream (not {@code null})
     * @throws WasmIOException if opening the stream failed for some reason
     */
    public static WasmInputStream open(Path path) throws WasmIOException {
        try {
            return of(Files.newInputStream(path));
        } catch (IOException e) {
            throw new WasmIOException("Failed to open " + path, e);
        }
    }

    /**
     * Construct a new instance from a buffer.
     *
     * @param buffer the buffer containing the file contents (must not be {@code null})
     * @return the new stream (not {@code null})
     */
    public static WasmInputStream of(ByteBuffer buffer) {
        return new BufferWasmInputStream(buffer);
    }

    /**
     * Construct a new instance from a byte array.
     *
     * @param bytes the byte array containing the file contents (must not be {@code null})
     * @param off the offset in the byte array
     * @param len the number of bytes to include
     * @return the new stream (not {@code null})
     */
    public static WasmInputStream of(byte[] bytes, int off, int len) {
        return of(ByteBuffer.wrap(bytes, off, len));
    }

    /**
     * Construct a new instance from a byte array.
     *
     * @param bytes the byte array containing the file contents (must not be {@code null})
     * @return the new stream (not {@code null})
     */
    public static WasmInputStream of(byte[] bytes) {
        return of(bytes, 0, bytes.length);
    }

    /**
     * Construct a new instance from an input stream.
     *
     * @param is the input stream (must not be {@code null})
     * @return the new stream (not {@code null})
     */
    public static WasmInputStream of(InputStream is) {
        return new InputStreamWasmInputStream(is);
    }

    /**
     * {@return the current stream position (which is equal to the number of bytes read)}
     */
    public abstract long position();

    /**
     * Skip some number of bytes in this stream.
     *
     * @param count the number of bytes to skip
     * @throws WasmIOException if an I/O error occurs or the end-of-file was reached
     */
    public abstract void skip(final long count) throws WasmIOException;

    /**
     * Create a substream of this stream with a fixed size.
     * Closing the substream will advance the outer stream to the first byte after {@code size}.
     *
     * @param size the fixed size
     * @return the substream (must not be {@code null})
     */
    public WasmInputStream slice(final long size) {
        return new Slice(this, size);
    }

    // unsigned LEB

    /**
     * Read an unsigned 8-bit integer.
     *
     * @return the 8-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int u8() throws WasmIOException {
        return (int) uleb(8);
    }

    /**
     * Read an unsigned 16-bit integer.
     *
     * @return the 16-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int u16() throws WasmIOException {
        return (int) uleb(16);
    }

    /**
     * Read an unsigned 31-bit integer.
     *
     * @return the 31-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int u31() throws WasmIOException {
        return (int) uleb(31);
    }

    /**
     * Read an unsigned 32-bit integer as an {@code int}.
     *
     * @return the 32-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int u32() throws WasmIOException {
        return (int) uleb(32);
    }

    /**
     * Read an unsigned 32-bit integer as a {@code long}.
     *
     * @return the 32-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public long u32Long() throws WasmIOException {
        return uleb(32);
    }

    /**
     * Read an unsigned 64-bit integer as a {@code long}.
     *
     * @return the 64-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public long u64() throws WasmIOException {
        return uleb(64);
    }

    // signed LEB

    /**
     * Read a signed 32-bit integer.
     *
     * @return the 32-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int s32() throws WasmIOException {
        return (int) sleb(32);
    }

    /**
     * Read a signed 33-bit integer.
     *
     * @return the 33-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public long s33() throws WasmIOException {
        return sleb(33);
    }

    /**
     * Read a signed 64-bit integer.
     *
     * @return the 64-bit integer
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public long s64() throws WasmIOException {
        return sleb(64);
    }

    // raw data

    /**
     * Read a raw 8-bit unsigned byte.
     * The value will be in the range <code>[{@link Ranges#U8_MIN}, {@link Ranges#U8_MAX}]</code>.
     *
     * @return the 8-bit unsigned integer
     * @throws WasmRangeException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int rawByte() throws WasmIOException {
        int res = rawByteOpt();
        if (res == -1) {
            throw new WasmEOFException();
        }
        return res;
    }

    /**
     * Read an optional raw 8-bit unsigned byte.
     * The value will be in the range <code>[{@link Ranges#U8_MIN}, {@link Ranges#U8_MAX}]</code>.
     *
     * @return the 8-bit unsigned integer, or {@code -1} if the end-of-file was reached
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public abstract int rawByteOpt() throws WasmIOException;

    /**
     * Read an optional raw 8-bit unsigned byte, without consuming it.
     * The value will be in the range <code>[{@link Ranges#U8_MIN}, {@link Ranges#U8_MAX}]</code>.
     *
     * @return the 8-bit unsigned integer
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int peekRawByte() throws WasmIOException {
        int res = peekRawByteOpt();
        if (res == -1) {
            throw new WasmEOFException();
        }
        return res;
    }

    /**
     * Read an optional raw 8-bit unsigned byte, without consuming it.
     * The value will be in the range <code>[{@link Ranges#U8_MIN}, {@link Ranges#U8_MAX}]</code>.
     *
     * @return the 8-bit unsigned integer, or {@code -1} if the end-of-file would be reached
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public abstract int peekRawByteOpt() throws WasmIOException;

    /**
     * Read a raw byte array of the given size.
     *
     * @param len the number of raw bytes to read
     * @return a byte array of length {@code len} which contains the bytes
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public abstract byte[] rawBytes(final int len) throws WasmIOException;

    /**
     * Read raw bytes into the given buffer.
     * All of the remaining space in the buffer will be filled.
     *
     * @param buf the buffer to fill
     */
    public abstract void rawBytes(final ByteBuffer buf);

    /**
     * Read a raw 32-bit little-endian value from the stream.
     *
     * @return the 32-bit integer value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public abstract int raw32le() throws WasmIOException;

    /**
     * Read a raw 64-bit little-endian value from the stream.
     *
     * @return the 64-bit integer value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public abstract long raw64le() throws WasmIOException;

    /**
     * Read a raw 32-bit little-endian floating point value from the stream.
     *
     * @return the 32-bit floating point value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public float f32() throws WasmIOException {
        return Float.intBitsToFloat(raw32le());
    }

    /**
     * Read a raw 64-bit little-endian floating point value from the stream.
     *
     * @return the 64-bit floating point value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public double f64() throws WasmIOException {
        return Double.longBitsToDouble(raw64le());
    }

    // vectors

    private static final byte[] NO_BYTES = new byte[0];

    /**
     * Read a WASM-style byte vector from the stream.
     *
     * @return the byte vector (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public byte[] byteVec() throws WasmIOException {
        int len = u31();
        if (len == 0) {
            return NO_BYTES;
        }
        return rawBytes(len);
    }

    private static final int[] NO_INTS = new int[0];

    /**
     * Read a WASM-style integer vector from the stream.
     * Each integer is a 32-bit unsigned value stored in an {@code int}.
     *
     * @return the integer vector (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public int[] u32Vec() throws WasmIOException {
        int len = u31();
        if (len == 0) {
            return NO_INTS;
        }
        int[] vec = new int[len];
        for (int i = 0; i < len; i++) {
            vec[i] = u32();
        }
        return vec;
    }

    /**
     * Read a WASM-style UTF-8 encoded string from the stream.
     *
     * @return the string (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public String utf8() throws WasmIOException {
        int len = u31();
        StringBuilder sb = new StringBuilder(len);
        // we must decode strictly in order to conform with validation requirements.
        int i = 0;
        while (i < len) {
            // at least one byte
            i++;
            int a = rawByte();
            if (a < 0b1_0000000) {
                // one-byte form
                sb.appendCodePoint(a);
                continue;
            }
            if (a < 0b110_00000) {
                // invalid early continuation byte
                throw malformed();
            }
            // at least two bytes
            if (i == len) {
                // truncated sequence
                throw malformed();
            }
            i++;
            int b = rawByte();
            // check continuation byte format
            if (b < 0b10_000000 || b > 0b10_111111) {
                throw malformed();
            }
            if (a < 0b1110_0000) {
                // two-byte form
                int cp = (a & 0b000_11111) << 6 | b & 0b00_111111;
                if (cp <= 0x7f) {
                    // overlong code point
                    throw malformed();
                }
                sb.appendCodePoint(cp);
                continue;
            }
            // at least three bytes
            if (i == len) {
                // truncated sequence
                throw malformed();
            }
            i++;
            int c = rawByte();
            // check continuation byte format
            if (c < 0b10_000000 || c > 0b10_111111) {
                throw malformed();
            }
            if (a < 0b1111_0000) {
                // three-byte form
                int cp = (a & 0b0000_1111) << 12 | (b & 0b00_111111) << 6 | c & 0b00_111111;
                if (cp <= 0x7ff) {
                    // overlong code point
                    throw malformed();
                }
                if (Character.MIN_SURROGATE <= cp && cp <= Character.MAX_SURROGATE) {
                    // surrogate code points not allowed
                    throw malformed();
                }
                sb.appendCodePoint(cp);
                continue;
            }
            // must be four bytes, or an error
            if (i == len) {
                // truncated sequence
                throw malformed();
            }
            i++;
            int d = rawByte();
            // check continuation byte format
            if (d < 0b10_000000 || d > 0b10_111111) {
                throw malformed();
            }
            if (a < 0b1111_1000) {
                // four-byte form
                int cp =
                        (a & 0b00000_111) << 18
                                | (b & 0b00_111111) << 12
                                | (c & 0b00_111111) << 6
                                | d & 0b00_111111;
                if (cp <= 0xffff || cp > Character.MAX_CODE_POINT) {
                    // overlong code point, or out of code point range
                    throw malformed();
                }
                sb.appendCodePoint(cp);
                continue;
            }
            // five or more bytes; invalid
            throw malformed();
        }
        return sb.toString();
    }

    private WasmParseException malformed() {
        return new WasmParseException("malformed UTF-8 encoding");
    }

    @Override
    public void close() throws WasmIOException {}

    /**
     * Read a WASM function type from the stream.
     *
     * @return the function type (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public FunctionType funcType() throws WasmIOException {
        if (rawByte() != 0x60) {
            throw new WasmParseException("Expected 0x60 (function type)");
        }
        List<ValueType> parameterTypes = typeVec();
        List<ValueType> resultTypes = typeVec();
        return FunctionType.of(parameterTypes, resultTypes);
    }

    /**
     * Read a WASM-style type vector from the stream.
     *
     * @return the type vector (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public List<ValueType> typeVec() throws WasmIOException {
        int cnt = u31();
        switch (cnt) {
            case 0:
                return List.of();
            case 1:
                return List.of(type());
            case 2:
                return List.of(type(), type());
            default:
                ValueType[] types = new ValueType[cnt];
                for (int i = 0; i < cnt; i++) {
                    types[i] = type();
                }
                return List.of(types);
        }
    }

    /**
     * Read a value type from the stream.
     *
     * @return the value type (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public ValueType type() throws WasmIOException {
        try {
            return ValueType.forId(rawByte());
        } catch (IllegalArgumentException e) {
            throw badType(e);
        }
    }

    /**
     * Read a WASM reference type from the stream.
     *
     * @return the reference type (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public ValueType refType() throws WasmIOException {
        try {
            return ValueType.refTypeForId(rawByte());
        } catch (IllegalArgumentException e) {
            throw badType(e);
        }
    }

    /**
     * Read a WASM mutability type from the stream.
     *
     * @return the mutability type (not {@code null})
     * @throws WasmParseException if the format of the stream is invalid
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public MutabilityType mut() throws WasmIOException {
        try {
            return MutabilityType.forId(rawByte());
        } catch (IllegalArgumentException e) {
            throw malformedMutability(e);
        }
    }

    private static WasmParseException badType(Throwable cause) {
        return new WasmParseException("Invalid type", cause);
    }

    private static WasmParseException malformedMutability(Throwable cause) {
        throw new WasmParseException("malformed mutability", cause);
    }

    /**
     * Read a ULEB value returning up to 64 significant bits, with range validation.
     *
     * @param bits the number of significant bits allowed
     * @return the value
     * @throws WasmIOException if an underlying I/O error occurs or the value exceeds the allowed range
     */
    private long uleb(int bits) throws WasmIOException {
        int maxShift = (bits / 7) * 7;
        int lastBits = bits % 7;
        int b;
        long result = 0;
        int shift;
        // read low n bits
        for (shift = 0; shift < maxShift; shift += 7) {
            b = rawByte();
            result |= (b & 0x7fL) << shift;
            if (b < 0x80) {
                return result;
            }
        }
        // check last valid byte
        b = rawByte();
        int lastMask = (-1 << lastBits) & 0x7f;
        if ((b & lastMask) != 0) {
            throw rangeError();
        }
        result |= (b & 0x7fL) << shift;
        // and forbid bits in the remainder
        while (b >= 0x80) {
            b = rawByte();
            if ((b & 0x7f) != 0) {
                throw rangeError();
            }
        }
        return result;
    }

    private long sleb(int bits) throws WasmIOException {
        int maxShift = (bits / 7) * 7;
        int lastBits = bits % 7 - 1;

        int b;
        long result = 0;
        int shift;
        // read low n bits
        for (shift = 0; shift < maxShift; shift += 7) {
            b = rawByte();
            assert shift < 64;
            result |= (b & 0x7fL) << shift;
            if (b < 0x80) {
                if ((b & 0x40) != 0) {
                    // negative
                    result |= -1L << (shift + 7);
                }
                return result;
            }
        }
        // check last valid byte
        b = rawByte();
        result |= (b & 0x7fL) << shift;
        int lastMask = (-1 << lastBits) & 0x7f;
        if ((b & lastMask) == lastMask) {
            if (shift < 57) {
                result |= -1L << (shift + 7);
            }
            // enforce negative in the remainder
            while (b >= 0x80) {
                b = rawByte();
                if ((b & 0x7f) != 0x7f) {
                    throw rangeError();
                }
            }
        } else if ((b & lastMask) == 0) {
            // enforce positive in the remainder
            while (b >= 0x80) {
                b = rawByte();
                if ((b & 0x7f) != 0) {
                    throw rangeError();
                }
            }
        } else {
            throw rangeError();
        }
        return result;
    }

    private static WasmRangeException rangeError() {
        return new WasmRangeException("Range error in ULEB value");
    }

    private static class Slice extends WasmInputStream {
        private boolean closed;
        private final WasmInputStream outer;
        private final long end;

        public Slice(final WasmInputStream outer, final long size) {
            this.end = outer.position() + size;
            this.outer = outer;
        }

        public long position() {
            return outer.position();
        }

        public void skip(final long count) throws WasmIOException {
            if (count > remaining() || closed && count > 0) {
                throw new WasmEOFException();
            }
        }

        public int rawByteOpt() throws WasmIOException {
            if (remaining() == 0 || closed) {
                return -1;
            } else {
                return outer.rawByteOpt();
            }
        }

        public int peekRawByteOpt() throws WasmIOException {
            if (remaining() == 0 || closed) {
                return -1;
            } else {
                return outer.peekRawByteOpt();
            }
        }

        public byte[] rawBytes(final int len) throws WasmIOException {
            if (len > remaining() || closed && len > 0) {
                throw new WasmEOFException();
            }
            return outer.rawBytes(len);
        }

        public void rawBytes(final ByteBuffer buf) {
            if (buf.remaining() > remaining() || closed && buf.hasRemaining()) {
                throw new WasmEOFException();
            }
            outer.rawBytes(buf);
        }

        public int raw32le() throws WasmIOException {
            if (remaining() < 4 || closed) {
                throw new WasmEOFException();
            }
            return outer.raw32le();
        }

        public long raw64le() throws WasmIOException {
            if (remaining() < 8 || closed) {
                throw new WasmEOFException();
            }
            return outer.raw64le();
        }

        private long remaining() {
            return end - outer.position();
        }

        public void close() throws WasmIOException {
            if (!closed) {
                closed = true;
                outer.skip(remaining());
            }
        }
    }
}
