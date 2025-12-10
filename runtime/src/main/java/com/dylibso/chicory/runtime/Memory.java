package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.DataSegment;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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

    @Deprecated
    Object lock(int address);

    int waitOn(int address, int expected, long timeout);

    int waitOn(int address, long expected, long timeout);

    int notify(int address, int maxThreads);

    default int atomicReadInt(int addr) {
        synchronized (lock(addr)) {
            return readInt(addr);
        }
    }

    default long atomicReadLong(int addr) {
        synchronized (lock(addr)) {
            return readLong(addr);
        }
    }

    default short atomicReadShort(int addr) {
        synchronized (lock(addr)) {
            return readShort(addr);
        }
    }

    default byte atomicReadByte(int addr) {
        synchronized (lock(addr)) {
            return read(addr);
        }
    }

    default void atomicWriteInt(int addr, int value) {
        synchronized (lock(addr)) {
            writeI32(addr, value);
        }
    }

    default void atomicWriteLong(int addr, long value) {
        synchronized (lock(addr)) {
            writeLong(addr, value);
        }
    }

    default void atomicWriteShort(int addr, short value) {
        synchronized (lock(addr)) {
            writeShort(addr, value);
        }
    }

    default void atomicWriteByte(int addr, byte value) {
        synchronized (lock(addr)) {
            writeByte(addr, value);
        }
    }

    default int atomicAddInt(int addr, int delta) {
        synchronized (lock(addr)) {
            int val = readInt(addr);
            writeI32(addr, val + delta);
            return val;
        }
    }

    default int atomicAndInt(int addr, int mask) {
        synchronized (lock(addr)) {
            int val = readInt(addr);
            writeI32(addr, val & mask);
            return val;
        }
    }

    default int atomicOrInt(int addr, int mask) {
        synchronized (lock(addr)) {
            int val = readInt(addr);
            writeI32(addr, val | mask);
            return val;
        }
    }

    default int atomicXorInt(int addr, int mask) {
        synchronized (lock(addr)) {
            int val = readInt(addr);
            writeI32(addr, val ^ mask);
            return val;
        }
    }

    default int atomicXchgInt(int addr, int value) {
        synchronized (lock(addr)) {
            int val = readInt(addr);
            writeI32(addr, value);
            return val;
        }
    }

    default int atomicCmpxchgInt(int addr, int expected, int replacement) {
        synchronized (lock(addr)) {
            int val = readInt(addr);
            if (val == expected) {
                writeI32(addr, replacement);
            }
            return val;
        }
    }

    default long atomicAddLong(int addr, long delta) {
        synchronized (lock(addr)) {
            long val = readLong(addr);
            writeLong(addr, val + delta);
            return val;
        }
    }

    default long atomicAndLong(int addr, long mask) {
        synchronized (lock(addr)) {
            long val = readLong(addr);
            writeLong(addr, val & mask);
            return val;
        }
    }

    default long atomicOrLong(int addr, long mask) {
        synchronized (lock(addr)) {
            long val = readLong(addr);
            writeLong(addr, val | mask);
            return val;
        }
    }

    default long atomicXorLong(int addr, long mask) {
        synchronized (lock(addr)) {
            long val = readLong(addr);
            writeLong(addr, val ^ mask);
            return val;
        }
    }

    default long atomicXchgLong(int addr, long value) {
        synchronized (lock(addr)) {
            long val = readLong(addr);
            writeLong(addr, value);
            return val;
        }
    }

    default long atomicCmpxchgLong(int addr, long expected, long replacement) {
        synchronized (lock(addr)) {
            long val = readLong(addr);
            if (val == expected) {
                writeLong(addr, replacement);
            }
            return val;
        }
    }

    default short atomicAddShort(int addr, short delta) {
        synchronized (lock(addr)) {
            short val = readShort(addr);
            writeShort(addr, (short) (val + delta));
            return val;
        }
    }

    default short atomicAndShort(int addr, short mask) {
        synchronized (lock(addr)) {
            short val = readShort(addr);
            writeShort(addr, (short) (val & mask));
            return val;
        }
    }

    default short atomicOrShort(int addr, short mask) {
        synchronized (lock(addr)) {
            short val = readShort(addr);
            writeShort(addr, (short) (val | mask));
            return val;
        }
    }

    default short atomicXorShort(int addr, short mask) {
        synchronized (lock(addr)) {
            short val = readShort(addr);
            writeShort(addr, (short) (val ^ mask));
            return val;
        }
    }

    default short atomicXchgShort(int addr, short value) {
        synchronized (lock(addr)) {
            short val = readShort(addr);
            writeShort(addr, value);
            return val;
        }
    }

    default short atomicCmpxchgShort(int addr, short expected, short replacement) {
        synchronized (lock(addr)) {
            short val = readShort(addr);
            if (val == expected) {
                writeShort(addr, replacement);
            }
            return val;
        }
    }

    default byte atomicAddByte(int addr, byte delta) {
        synchronized (lock(addr)) {
            byte val = read(addr);
            writeByte(addr, (byte) (val + delta));
            return val;
        }
    }

    default byte atomicAndByte(int addr, byte mask) {
        synchronized (lock(addr)) {
            byte val = read(addr);
            writeByte(addr, (byte) (val & mask));
            return val;
        }
    }

    default byte atomicOrByte(int addr, byte mask) {
        synchronized (lock(addr)) {
            byte val = read(addr);
            writeByte(addr, (byte) (val | mask));
            return val;
        }
    }

    default byte atomicXorByte(int addr, byte mask) {
        synchronized (lock(addr)) {
            byte val = read(addr);
            writeByte(addr, (byte) (val ^ mask));
            return val;
        }
    }

    default byte atomicXchgByte(int addr, byte value) {
        synchronized (lock(addr)) {
            byte val = read(addr);
            writeByte(addr, value);
            return val;
        }
    }

    default byte atomicCmpxchgByte(int addr, byte expected, byte replacement) {
        synchronized (lock(addr)) {
            byte val = read(addr);
            if (val == expected) {
                writeByte(addr, replacement);
            }
            return val;
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
