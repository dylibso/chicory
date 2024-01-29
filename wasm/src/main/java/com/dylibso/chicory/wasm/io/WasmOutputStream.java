package com.dylibso.chicory.wasm.io;

import static com.dylibso.chicory.wasm.io.Ranges.*;

import com.dylibso.chicory.wasm.op.Op;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.function.ObjLongConsumer;

/**
 * A stream which can write WASM content.
 */
public abstract class WasmOutputStream implements AutoCloseable {
    /**
     * Construct a new instance targeting a file.
     *
     * @param path the file path (must not be {@code null})
     * @return the new stream (not {@code null})
     * @throws WasmIOException if creating the stream failed for some reason
     */
    public static WasmOutputStream open(Path path) throws WasmIOException {
        try {
            return of(
                    FileChannel.open(
                            path,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new WasmIOException("Failed to open " + path, e);
        }
    }

    /**
     * Construct a new instance targeting a writable file channel.
     *
     * @param fileChannel the file channel (must not be {@code null})
     * @return the new stream (not {@code null})
     * @throws WasmIOException if creating the stream failed for some reason
     */
    public static WasmOutputStream of(FileChannel fileChannel) throws WasmIOException {
        return new FileChannelWasmOutputStream(fileChannel);
    }

    /**
     * {@return a temporary WASM output stream}
     * When the temporary stream is closed, the given {@code onClose} handler will be called with an input stream
     * over the written data, along with the total number of bytes written.
     * After the handler completes, the input stream is closed and any temporary storage associated with the output
     * stream is released.
     *
     * @param onClose the handler to call once the temporary stream is closed (must not be {@code null})
     * @return the new stream (not {@code null})
     * @throws WasmIOException if creating the stream failed for some reason
     */
    public static WasmOutputStream temporary(ObjLongConsumer<WasmInputStream> onClose)
            throws WasmIOException {
        return new TemporaryWasmOutputStream(onClose);
    }

    /**
     * {@return the current stream position (which is equal to the number of bytes written)}
     */
    public abstract long position();

    // raw data

    /**
     * Write a raw 8-bit unsigned byte.
     * Only the bottom 8 bits will be written.
     *
     * @param val the 8-bit unsigned integer
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public abstract void rawByte(int val) throws WasmIOException;

    /**
     * Write a raw 8-bit unsigned byte.
     * Only the bottom 8 bits will be written.
     *
     * @param val the 8-bit unsigned integer
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public final void rawByte(long val) throws WasmIOException {
        rawByte((int) val);
    }

    /**
     * Write a raw 16-bit little-endian value to the stream.
     * Only the bottom 16 bits will be written.
     *
     * @param val the 16-bit integer value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public void raw16le(int val) throws WasmIOException {
        rawByte(val);
        rawByte(val >>> 8);
    }

    /**
     * Write a raw 32-bit little-endian value to the stream.
     *
     * @param val the 32-bit integer value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public void raw32le(int val) throws WasmIOException {
        raw16le(val);
        raw16le(val >>> 16);
    }

    /**
     * Write a raw 64-bit little-endian value to the stream.
     *
     * @param val the 64-bit integer value
     * @throws WasmIOException if an underlying I/O error occurred
     * @throws WasmEOFException if the end-of-file was reached unexpectedly
     */
    public void raw64le(long val) throws WasmIOException {
        raw32le((int) val);
        raw32le((int) (val >>> 32));
    }

    /**
     * Write a raw 32-bit little-endian floating point value to the stream.
     *
     * @param val the 32-bit floating point value
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public final void f32(float val) throws WasmIOException {
        raw32le(Float.floatToRawIntBits(val));
    }

    /**
     * Write a raw 64-bit little-endian floating point value to the stream.
     *
     * @param val the 32-bit floating point value
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public final void f64(double val) throws WasmIOException {
        raw64le(Double.doubleToRawLongBits(val));
    }

    // transfer and bulk I/O

    /**
     * Write the given raw byte array.
     *
     * @param bytes the array containing the raw bytes to write (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public final void rawBytes(byte[] bytes) throws WasmIOException {
        rawBytes(bytes, 0, bytes.length);
    }

    /**
     * Write a portion of the given raw byte array.
     *
     * @param bytes the array containing the raw bytes to write (must not be {@code null})
     * @param off the offset into the array to begin writing
     * @param len the number of bytes to write
     * @throws IndexOutOfBoundsException if the offset and/or length are invalid
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public abstract void rawBytes(byte[] bytes, int off, int len) throws WasmIOException;

    /**
     * Write raw bytes from the given buffer.
     *
     * @param buffer the buffer containing the raw bytes to write (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public abstract void rawBytes(final ByteBuffer buffer) throws WasmIOException;

    /**
     * Transfer all of the remaining bytes from the given input stream into the output stream.
     *
     * @param is the input stream to read from (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void transferFrom(InputStream is) throws WasmIOException {
        try {
            is.transferTo(asOutputStream());
        } catch (IOException e) {
            throw new WasmIOException("Transfer failed", e);
        }
    }

    /**
     * {@return a view of this stream as an <code>OutputStream</code>}
     */
    public OutputStream asOutputStream() {
        return new AsOutputStream();
    }

    // unsigned LEB

    /**
     * Write an unsigned 8-bit integer.
     *
     * @param value the 8-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u8(int value) throws WasmIOException {
        if (value < U8_MIN || value > U8_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    /**
     * Write an unsigned 8-bit integer.
     *
     * @param value the 8-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u8(long value) throws WasmIOException {
        if (value < U8_MIN || value > U8_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    /**
     * Write an unsigned 16-bit integer.
     *
     * @param value the 16-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u16(int value) throws WasmIOException {
        if (value < U16_MIN || value > U16_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    /**
     * Write an unsigned 16-bit integer.
     *
     * @param value the 16-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u16(long value) throws WasmIOException {
        if (value < U16_MIN || value > U16_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    /**
     * Write an unsigned 31-bit integer.
     *
     * @param value the 31-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u31(int value) throws WasmIOException {
        if (value < U31_MIN || value > U31_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    /**
     * Write an unsigned 31-bit integer.
     *
     * @param value the 31-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u31(long value) throws WasmIOException {
        if (value < U31_MIN || value > U31_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    /**
     * Write an unsigned 32-bit integer.
     *
     * @param value the 32-bit integer to write
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u32(int value) throws WasmIOException {
        // always in range
        uleb(Integer.toUnsignedLong(value));
    }

    /**
     * Write an unsigned 31-bit integer.
     *
     * @param value the 31-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void u32(long value) throws WasmIOException {
        if (value < U32_MIN || value > U32_MAX) {
            throw outOfRange();
        }
        uleb(value);
    }

    // signed LEB

    /**
     * Write a signed 32-bit integer.
     *
     * @param value the 32-bit integer to write
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void s32(int value) throws WasmIOException {
        // always in range
        sleb(value);
    }

    /**
     * Write a signed 32-bit integer.
     *
     * @param value the 32-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void s32(long value) throws WasmIOException {
        if (value < S32_MIN || value > S32_MAX) {
            throw outOfRange();
        }
        sleb(value);
    }

    /**
     * Write a signed 33-bit integer.
     *
     * @param value the 33-bit integer to write
     * @throws WasmRangeException if the value is outside the valid range
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void s33(long value) throws WasmIOException {
        if (value < S33_MIN || value > S33_MAX) {
            throw outOfRange();
        }
        sleb(value);
    }

    /**
     * Write a signed 64-bit integer.
     *
     * @param value the 64-bit integer to write
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void s64(long value) throws WasmIOException {
        // always in range
        sleb(value);
    }

    // types

    /**
     * Write a WASM value type.
     *
     * @param type the type to write
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void type(ValueType type) throws WasmIOException {
        rawByte(type.id());
    }

    /**
     * Write a WASM function type.
     *
     * @param type the type to write
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void type(FunctionType type) throws WasmIOException {
        rawByte(0x60);
        typeVec(type.params());
        typeVec(type.returns());
    }

    /**
     * Write a WASM mutability type.
     *
     * @param type the type to write
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void mut(MutabilityType type) throws WasmIOException {
        rawByte(type.id());
    }

    // vectors

    /**
     * Write a vector of bytes.
     *
     * @param bytes the array containing the bytes to write (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void byteVec(byte[] bytes) throws WasmIOException {
        byteVec(bytes, 0, bytes.length);
    }

    /**
     * Write a vector of bytes.
     *
     * @param bytes the array containing the bytes to write (must not be {@code null})
     * @param off the offset into the array to begin writing
     * @param len the number of bytes to write
     * @throws IndexOutOfBoundsException if the offset and/or length are invalid
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void byteVec(final byte[] bytes, final int off, final int len) {
        if (len < 0) {
            throw invalidLength();
        }
        u31(len);
        rawBytes(bytes, off, len);
    }

    /**
     * Write a vector of types.
     *
     * @param types the types to write (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void typeVec(Collection<ValueType> types) throws WasmIOException {
        int len = types.size();
        if (len < 0) {
            throw invalidLength();
        }
        u31(len);
        types.forEach(this::type);
    }

    /**
     * Write a UTF-8 string as a byte vector.
     *
     * @param string the string to write (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void utf8(String string) throws WasmIOException {
        byteVec(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write a WASM opcode to the stream.
     *
     * @param op the opcode to write (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    public void op(Op op) throws WasmIOException {
        rawByte(op.opcode());
        int so = op.secondaryOpcode();
        if (so != -1) {
            u8(so);
        }
    }

    // state

    public void close() throws WasmIOException {}

    // internal

    private static WasmRangeException outOfRange() {
        return new WasmRangeException("Value is out of range for type");
    }

    private static IndexOutOfBoundsException invalidLength() {
        return new IndexOutOfBoundsException("Invalid length");
    }

    private void uleb(long value) throws WasmIOException {
        for (int bits = 64 - Long.numberOfLeadingZeros(value); bits > 7; bits -= 7) {
            rawByte(0x80 | value & 0x7f);
            value >>>= 7;
        }
    }

    private void sleb(long value) throws WasmIOException {
        // always write one extra bit for sign
        if (value < 0) {
            for (int bits = 65 - Long.numberOfLeadingZeros(~value); bits > 7; bits -= 7) {
                rawByte(0x80 | value & 0x7f);
                value >>= 7;
            }
            rawByte(value & 0x7f);
        } else {
            for (int bits = 65 - Long.numberOfLeadingZeros(value); bits > 7; bits -= 7) {
                rawByte(0x80 | value & 0x7f);
                value >>= 7;
            }
            rawByte(value);
        }
    }

    final class AsOutputStream extends OutputStream {
        public void write(final int b) throws IOException {
            try {
                rawByte(b);
            } catch (WasmIOException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else {
                    throw new IOException(e);
                }
            }
        }

        public void write(final byte[] b, final int off, final int len) {
            rawBytes(b, off, len);
        }

        public void close() throws IOException {
            super.close();
        }

        WasmOutputStream enclosing() {
            return WasmOutputStream.this;
        }
    }
}
