package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.exceptions.MalformedException;
import java.nio.ByteBuffer;

public final class Encoding {

    // https://webassembly.github.io/spec/core/binary/values.html#integers
    public static final int MAX_VARINT_LEN_32 = 5; // ceil(32/7)
    public static final int MAX_VARINT_LEN_64 = 10; // ceil(64/7)

    private Encoding() {}

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
            if (i >= maxVarInt) {
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
