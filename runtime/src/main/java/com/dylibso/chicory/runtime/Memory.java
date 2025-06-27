package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.types.DataSegment;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public interface Memory {

    /**
     * A WebAssembly page size is 64KiB = 65,536 bytes.
     */
    public static final int PAGE_SIZE = 65536;

    /**
     * Maximum number of pages allowed by the runtime.
     * WASM supports 2^16 pages, but we must limit based on the maximum JVM array size.
     * This limit is {@code Integer.MAX_VALUE / PAGE_SIZE}.
     */
    public static final int RUNTIME_MAX_PAGES = 32767;

    static int bytes(int pages) {
        return PAGE_SIZE * Math.min(pages, RUNTIME_MAX_PAGES);
    }

    int pages();

    int grow(int size);

    int initialPages();

    int maximumPages();

    boolean shared();

    Map<Integer, Integer> alignments();

    default void setAlignment(int addr, int alignment) {
        if (shared()) {
            alignments().put(addr, alignment);
        }
    }

    default void checkAlignment(int addr, int expected) {
        if (shared()) {
            if (!alignments().containsKey(addr)) {
                setAlignment(addr, expected);
            } else if (alignments().get(addr) < expected) {
                // TODO: verify this is empirical since the spec says:
                // It is a validation error if the alignment field of the memory access immediate
                // has any other value than the natural alignment for that access size.
                // https://github.com/WebAssembly/threads/blob/main/proposals/threads/Overview.md#alignment
                throw new InvalidException(
                        "unaligned atomic, alignment found: "
                                + alignments().get(addr)
                                + ", alignment expected: "
                                + expected);
            }
        }
    }

    void initialize(Instance instance, DataSegment[] dataSegments);

    void initPassiveSegment(int segmentId, int dest, int offset, int size);

    default void writeString(int offset, String data, Charset charSet) {
        write(offset, data.getBytes(charSet));
    }

    default void writeString(int offset, String data) {
        writeString(offset, data, StandardCharsets.UTF_8);
    }

    default String readString(int addr, int len) {
        return readString(addr, len, StandardCharsets.UTF_8);
    }

    default String readString(int addr, int len, Charset charSet) {
        return new String(readBytes(addr, len), charSet);
    }

    default void writeCString(int offset, String str) {
        writeCString(offset, str, StandardCharsets.UTF_8);
    }

    default void writeCString(int offset, String str, Charset charSet) {
        writeString(offset, str + '\0', charSet);
    }

    default String readCString(int addr, Charset charSet) {
        int c = addr;
        while (read(c) != '\0') {
            c++;
        }
        return new String(readBytes(addr, c - addr), charSet);
    }

    default String readCString(int addr) {
        return readCString(addr, StandardCharsets.UTF_8);
    }

    default void write(int addr, byte[] data) {
        write(addr, data, 0, data.length);
    }

    void write(int addr, byte[] data, int offset, int size);

    byte read(int addr);

    byte[] readBytes(int addr, int len);

    void writeI32(int addr, int data);

    int readInt(int addr);

    default long readI32(int addr) {
        return readInt(addr);
    }

    default long readU32(int addr) {
        return Integer.toUnsignedLong(readInt(addr));
    }

    void writeLong(int addr, long data);

    long readLong(int addr);

    default long readI64(int addr) {
        return readLong(addr);
    }

    void writeShort(int addr, short data);

    short readShort(int addr);

    default long readI16(int addr) {
        return readShort(addr);
    }

    long readU16(int addr);

    void writeByte(int addr, byte data);

    default long readU8(int addr) {
        return read(addr) & 0xFF;
    }

    default long readI8(int addr) {
        return read(addr);
    }

    void writeF32(int addr, float data);

    long readF32(int addr);

    float readFloat(int addr);

    void writeF64(int addr, double data);

    double readDouble(int addr);

    long readF64(int addr);

    void zero();

    void fill(byte value, int fromIndex, int toIndex);

    default void copy(int dest, int src, int size) {
        write(dest, readBytes(src, size));
    }

    void drop(int segment);
}
