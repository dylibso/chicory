package com.dylibso.chicory.wasm;

import java.nio.ByteBuffer;

public final class Encoding {

    /**
     * Reads an unsigned integer from {@code byteBuffer}.
     */
    public static long readUnsignedLeb128(ByteBuffer byteBuffer) {
        long result = 0;
        int shift = 0;
        while (true) {
            if (byteBuffer.remaining() == 0) {
                throw new IllegalArgumentException("ULEB128 reached the end of the buffer");
            }

            byte b = byteBuffer.get();
            result |= (long) (b & 0x7F) << shift;

            if ((b & 0x80) == 0) {
                break;
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
        byte currentByte;

        do {
            if (byteBuffer.remaining() == 0) {
                throw new IllegalArgumentException("SLEB128 reached the end of the buffer");
            }

            currentByte = byteBuffer.get();
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
        byte currentByte;
        int size = 64; // 64 bits for i64

        do {
            if (byteBuffer.remaining() == 0) {
                throw new IllegalArgumentException("LEB128 reached the end of the buffer");
            }

            currentByte = byteBuffer.get();
            result |= (long) (currentByte & 0x7F) << shift;
            shift += 7;
        } while ((currentByte & 0x80) != 0);

        // If the final byte read has its sign bit set (0x40), then sign-extend the result
        if (shift < size && (currentByte & 0x40) != 0) {
            result |= -1L << shift;
        }

        return result;
    }

    /**
     * Computes the number of bytes required to encode a given value as LEB128
     * @param value
     * @return
     */
    public static int computeLeb128Size(int value) {
        int size = 0;
        do {
            size++;
            value >>>= 7;
        } while (value != 0);
        return size;
    }
}
