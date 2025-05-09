package com.dylibso.chicory.wasm;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class Encoding {

    // https://webassembly.github.io/spec/core/binary/values.html#integers
    public static final int MAX_VARINT_LEN_32 = 5; // ceil(32/7)
    public static final int MAX_VARINT_LEN_64 = 10; // ceil(64/7)

    private Encoding() {}

    static int readInt(ByteBuffer buffer) {
        if (buffer.remaining() < 4) {
            throw new MalformedException("length out of bounds");
        }
        return buffer.getInt();
    }

    static byte readByte(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new MalformedException("length out of bounds");
        }
        return buffer.get();
    }

    static void readBytes(ByteBuffer buffer, byte[] dest) {
        if (buffer.remaining() < dest.length) {
            throw new MalformedException("length out of bounds");
        }
        buffer.get(dest);
    }

    // https://webassembly.github.io/spec/core/syntax/values.html#integers
    public static final long MIN_SIGNED_INT = Integer.MIN_VALUE; // -2^(32-1)
    public static final long MAX_SIGNED_INT = Integer.MAX_VALUE; // 2^(32-1)-1
    public static final long MAX_UNSIGNED_INT = 0xFFFFFFFFL; // 2^(32)-1

    /**
     * Read an unsigned I32 from the buffer. We can't fit an unsigned 32bit int
     * into a java int, so we must use a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer
     * @return the resulting long
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
     * @param buffer the byte buffer
     * @return the resulting long
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
     * @param buffer the byte buffer
     * @return the resulting long
     */
    public static long readVarSInt64(ByteBuffer buffer) {
        return readSigned64Leb128(buffer);
    }

    /**
     * Read a F64 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#floating-point">2.2.3. Floating-Point</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer
     * @return the resulting long
     */
    public static long readFloat64(ByteBuffer buffer) {
        return buffer.getLong();
    }

    /**
     * Read a F32 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#floating-point">2.2.3. Floating-Point</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer
     * @return the resulting long
     */
    public static long readFloat32(ByteBuffer buffer) {
        return readInt(buffer);
    }

    /**
     * Read a symbol name from the buffer as UTF-8 String.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#names%E2%91%A0">2.2.4. Names</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer
     * @return the resulting long
     */
    public static String readName(ByteBuffer buffer) {
        return readName(buffer, true);
    }

    /**
     * Read a symbol name from the buffer as UTF-8 String.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#names">2.2.4. Names</a> of the WebAssembly Core Specification.
     *
     * @param buffer the byte buffer
     * @param checkMalformed verify if it's a valid identifier
     * @return the resulting long
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

    private static boolean isValidIdentifier(String string) {
        return string.chars().allMatch(ch -> ch < 0x80 || Character.isUnicodeIdentifierPart(ch));
    }

    /**
     * Reads an unsigned integer from {@code byteBuffer}.
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

            if (i >= maxVarInt || byteBuffer.remaining() == 0) {
                throw new MalformedException("integer representation too long");
            }

            shift += 7;
        }

        return result;
    }

    /**
     * Reads an unsigned integer from {@code byteBuffer}.
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
