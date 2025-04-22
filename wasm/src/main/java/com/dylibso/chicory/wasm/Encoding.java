package com.dylibso.chicory.wasm;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for handling WebAssembly binary encoding.
 * Provides methods for reading various Wasm data types from a ByteBuffer
 * according to the WebAssembly specification.
 * See <a href="https://webassembly.github.io/spec/core/binary/index.html">Binary Format</a>
 */
public final class Encoding {

    // https://webassembly.github.io/spec/core/binary/values.html#integers
    /** Maximum number of bytes for a LEB128 encoded 32-bit integer. */
    public static final int MAX_VARINT_LEN_32 = 5; // ceil(32/7)

    /** Maximum number of bytes for a LEB128 encoded 64-bit integer. */
    public static final int MAX_VARINT_LEN_64 = 10; // ceil(64/7)

    /** Private constructor to prevent instantiation of utility class. */
    private Encoding() {}

    /**
     * Reads a standard 32-bit signed integer from the buffer.
     *
     * @param buffer The ByteBuffer to read from.
     * @return The integer value read.
     * @throws MalformedException if there are not enough bytes remaining.
     */
    static int readInt(ByteBuffer buffer) {
        if (buffer.remaining() < 4) {
            throw new MalformedException("length out of bounds");
        }
        return buffer.getInt();
    }

    /**
     * Reads a single byte from the buffer.
     *
     * @param buffer The ByteBuffer to read from.
     * @return The byte value read.
     * @throws MalformedException if there are no bytes remaining.
     */
    static byte readByte(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new MalformedException("length out of bounds");
        }
        return buffer.get();
    }

    /**
     * Reads a sequence of bytes from the buffer into the destination array.
     *
     * @param buffer The ByteBuffer to read from.
     * @param dest The byte array to read into.
     * @throws MalformedException if there are not enough bytes remaining to fill the destination array.
     */
    static void readBytes(ByteBuffer buffer, byte[] dest) {
        if (buffer.remaining() < dest.length) {
            throw new MalformedException("length out of bounds");
        }
        buffer.get(dest);
    }

    // https://webassembly.github.io/spec/core/syntax/values.html#integers
    /** Minimum value for a signed 32-bit integer (-2<sup>31</sup>). */
    public static final long MIN_SIGNED_INT = Integer.MIN_VALUE; // -2^(32-1)

    /** Maximum value for a signed 32-bit integer (2<sup>31</sup> - 1). */
    public static final long MAX_SIGNED_INT = Integer.MAX_VALUE; // 2^(32-1)-1

    /** Maximum value for an unsigned 32-bit integer (2<sup>32</sup> - 1). Represented as a long. */
    public static final long MAX_UNSIGNED_INT = 0xFFFFFFFFL; // 2^(32)-1

    /**
     * Read an unsigned I32 from the buffer. We can't fit an unsigned 32bit int
     * into a java int, so we must use a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @return the resulting long value.
     * @throws MalformedException if the integer is too large or malformed.
     */
    public static long readVarUInt32(ByteBuffer buffer) {
        var value = readUnsignedLeb128(buffer, MAX_VARINT_LEN_32);
        if (value < 0 || value > MAX_UNSIGNED_INT) {
            throw new MalformedException("integer too large");
        }
        return value;
    }

    /**
     * Read a signed I32 from the buffer. We can't fit an unsigned 32bit int into a java int, so we must use a long to use the same type as unsigned.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @return the resulting long value (even though it's read as 32-bit, returned as long for consistency with unsigned).
     * @throws MalformedException if the integer is too large or malformed.
     */
    public static long readVarSInt32(ByteBuffer buffer) {
        var value = readSigned32Leb128(buffer);
        if (value < MIN_SIGNED_INT || value > MAX_SIGNED_INT) {
            throw new MalformedException("integer too large");
        }
        return value;
    }

    /**
     * Read a signed I64 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @return the resulting long value.
     * @throws MalformedException if the integer representation is too long or malformed.
     */
    public static long readVarSInt64(ByteBuffer buffer) {
        return readSigned64Leb128(buffer);
    }

    /**
     * Read a F64 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#floating-point">2.2.3. Floating-Point</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @return the resulting long containing the bits of the float64 value.
     * @throws MalformedException if there are not enough bytes remaining.
     */
    public static long readFloat64(ByteBuffer buffer) {
        return buffer.getLong();
    }

    /**
     * Read a F32 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#floating-point">2.2.3. Floating-Point</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @return the resulting int containing the bits of the float32 value.
     * @throws MalformedException if there are not enough bytes remaining.
     */
    public static long readFloat32(ByteBuffer buffer) {
        return readInt(buffer);
    }

    /**
     * Read a symbol name from the buffer as UTF-8 String.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#names%E2%91%A0">2.2.4. Names</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @return the resulting String.
     * @throws MalformedException if the length or encoding is invalid.
     */
    public static String readName(ByteBuffer buffer) {
        return readName(buffer, true);
    }

    /**
     * Read a symbol name from the buffer as UTF-8 String.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#names">2.2.4. Names</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer to read from.
     * @param checkMalformed if true, verifies that the name is a valid identifier.
     * @return the resulting String.
     * @throws MalformedException if the length is invalid, encoding is malformed UTF-8 (and checkMalformed is true).
     */
    public static String readName(ByteBuffer buffer, boolean checkMalformed) {
        var length = (int) readVarUInt32(buffer);
        byte[] bytes = new byte[length];
        readBytes(buffer, bytes);
        var name = new String(bytes, StandardCharsets.UTF_8);
        if (checkMalformed && !isValidIdentifier(name)) {
            throw new MalformedException("malformed UTF-8 encoding");
        }
        return name;
    }

    /**
     * Checks if a string is a valid WebAssembly identifier.
     * According to the spec, identifiers can contain any Unicode character
     * suitable for use in identifiers. This implementation simplifies slightly
     * by checking for ASCII range or if `Character.isUnicodeIdentifierPart` returns true.
     *
     * @param string The string to check.
     * @return true if the string is considered a valid identifier, false otherwise.
     */
    private static boolean isValidIdentifier(String string) {
        return string.chars().allMatch(ch -> ch < 0x80 || Character.isUnicodeIdentifierPart(ch));
    }

    /**
     * Reads an unsigned LEB128 encoded integer from the ByteBuffer.
     *
     * @param byteBuffer The ByteBuffer to read from.
     * @param maxVarInt The maximum number of bytes allowed for the LEB128 representation.
     * @return The decoded unsigned integer value as a long.
     * @throws MalformedException if the buffer runs out of bytes or the integer representation is too long.
     */
    public static long readUnsignedLeb128(ByteBuffer byteBuffer, int maxVarInt) {
        long result = 0;
        int shift = 0;
        int i = 0;
        while (true) {
            i++;
            if (byteBuffer.remaining() == 0) {
                throw new MalformedException("length out of bounds");
            }
            byte b = byteBuffer.get();
            result |= (long) (b & 0x7F) << shift;

            if ((b & 0x80) == 0) {
                break;
            }
            if (i >= maxVarInt) {
                throw new MalformedException("integer representation too long");
            }

            shift += 7;
        }

        return result;
    }

    /**
     * Reads an unsigned integer from {@code byteBuffer}.
     *
     * @param byteBuffer The ByteBuffer to read from.
     * @return The decoded signed 32-bit integer value as a long.
     * @throws MalformedException if the buffer runs out of bytes or the integer representation is too long.
     */
    public static long readSigned32Leb128(ByteBuffer byteBuffer) {
        long result = 0;
        int shift = 0;
        int i = 0;
        byte currentByte;

        do {
            i++;
            if (byteBuffer.remaining() == 0) {
                throw new MalformedException("length out of bounds");
            }

            currentByte = byteBuffer.get();
            if ((currentByte & 0x80) != 0 && i >= MAX_VARINT_LEN_32) {
                throw new MalformedException("integer representation too long");
            }
            result |= (long) (currentByte & 0x7F) << shift;
            shift += 7;
        } while ((currentByte & 0x80) != 0);

        // If the final byte read has its sign bit set (0x40), then sign-extend the result
        if ((currentByte & 0x40) != 0) {
            result |= -(1L << shift);
        }

        return result;
    }

    /**
     * Reads a signed LEB128 encoded 64-bit integer from the ByteBuffer.
     *
     * @param byteBuffer The ByteBuffer to read from.
     * @return The decoded signed 64-bit integer value as a long.
     * @throws MalformedException if the buffer runs out of bytes, the integer representation is too long, or the integer is too large.
     */
    public static long readSigned64Leb128(ByteBuffer byteBuffer) {
        long result = 0;
        int shift = 0;
        int i = 0;
        byte currentByte;
        int size = 64; // 64 bits for i64

        do {
            i++;
            if (byteBuffer.remaining() == 0) {
                throw new MalformedException("length out of bounds");
            }

            currentByte = byteBuffer.get();
            if ((currentByte & 0x80) != 0 && i >= MAX_VARINT_LEN_64) {
                throw new MalformedException("integer representation too long");
            }
            result |= (long) (currentByte & 0x7F) << shift;
            shift += 7;
        } while ((currentByte & 0x80) != 0);

        // If the final byte read has its sign bit set (0x40), then sign-extend the result
        if (shift < size && (currentByte & 0x40) != 0) {
            result |= -1L << shift;
        }

        if (i >= MAX_VARINT_LEN_64 && currentByte != 0 && currentByte < 0x7F) {
            throw new MalformedException("integer too large");
        }

        return result;
    }
}
