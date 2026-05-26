package com.dylibso.chicory.component;

import com.dylibso.chicory.wasm.Encoding;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Reads Component Model binary format from a byte buffer.
 *
 * <p>The Component Model spec encodes component metadata using:
 * - LEB128 variable-length integers (see Encoding utility)
 * - UTF-8 strings with LEB128 length prefix
 * - Type tables with kind bytes and type-specific data
 *
 * <p>This reader provides convenient methods for parsing the binary format.
 */
public class BinaryComponentReader {

    private final ByteBuffer buffer;

    public BinaryComponentReader(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes);
    }

    /**
     * Check if there are more bytes to read.
     *
     * @return true if buffer has remaining bytes
     */
    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    /**
     * Read a single byte.
     *
     * @return byte value
     * @throws ComponentModelException if buffer is empty
     */
    public byte readByte() throws ComponentModelException {
        if (!buffer.hasRemaining()) {
            throw new ComponentModelException("Unexpected end of component binary");
        }
        return buffer.get();
    }

    /**
     * Read an unsigned LEB128 integer.
     *
     * @return unsigned integer as long
     * @throws ComponentModelException if LEB128 encoding is invalid
     */
    public long readUnsigned() throws ComponentModelException {
        try {
            return Encoding.readUnsignedLeb128(buffer, Encoding.MAX_VARINT_LEN_32);
        } catch (Exception e) {
            throw new ComponentModelException(
                    "Failed to read unsigned LEB128: " + e.getMessage(), e);
        }
    }

    /**
     * Read a signed LEB128 integer.
     *
     * @return signed integer as long
     * @throws ComponentModelException if LEB128 encoding is invalid
     */
    public long readSigned() throws ComponentModelException {
        try {
            return Encoding.readVarSInt32(buffer);
        } catch (Exception e) {
            throw new ComponentModelException("Failed to read signed LEB128: " + e.getMessage(), e);
        }
    }

    /**
     * Read a UTF-8 string with LEB128 length prefix.
     *
     * @return parsed string
     * @throws ComponentModelException if string encoding is invalid
     */
    public String readString() throws ComponentModelException {
        try {
            long length = readUnsigned();
            if (length < 0 || length > 1_000_000) { // sanity check
                throw new ComponentModelException("String length too large: " + length);
            }
            byte[] bytes = new byte[(int) length];
            if (buffer.remaining() < length) {
                throw new ComponentModelException("String data out of bounds");
            }
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (ComponentModelException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentModelException("Failed to read string: " + e.getMessage(), e);
        }
    }

    /**
     * Read a type index (unsigned integer).
     *
     * @return type index
     * @throws ComponentModelException if invalid
     */
    public int readTypeIndex() throws ComponentModelException {
        long value = readUnsigned();
        if (value > Integer.MAX_VALUE) {
            throw new ComponentModelException("Type index too large: " + value);
        }
        return (int) value;
    }

    /**
     * Skip N bytes.
     *
     * @param count number of bytes to skip
     * @throws ComponentModelException if not enough bytes available
     */
    public void skip(int count) throws ComponentModelException {
        if (buffer.remaining() < count) {
            throw new ComponentModelException(
                    "Cannot skip " + count + " bytes, only " + buffer.remaining() + " remaining");
        }
        buffer.position(buffer.position() + count);
    }

    /**
     * Get current position in buffer.
     *
     * @return current position
     */
    public int position() {
        return buffer.position();
    }

    /**
     * Get total bytes remaining.
     *
     * @return remaining byte count
     */
    public int remaining() {
        return buffer.remaining();
    }
}
