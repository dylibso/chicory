package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.DataSegment;
import java.nio.charset.Charset;

public interface Memory {

    /**
     * A WebAssembly page size is 64KiB = 65,536 bytes.
     */
    int PAGE_SIZE = 65536;

    /**
     * Maximum number of pages allowed by the runtime.
     * WASM supports 2^16 pages, but we must limit based on the maximum JVM array size.
     * This limit is {@code Integer.MAX_VALUE / PAGE_SIZE}.
     */
    int RUNTIME_MAX_PAGES = 32767;

    int pages();

    int grow(int size);

    int initialPages();

    int maximumPages();

    void initialize(Instance instance, DataSegment[] dataSegments);

    void initPassiveSegment(int segmentId, int dest, int offset, int size);

    void writeString(int offset, String data, Charset charSet);

    void writeString(int offset, String data);

    String readString(int addr, int len);

    String readString(int addr, int len, Charset charSet);

    void writeCString(int offset, String str);

    void writeCString(int offset, String str, Charset charSet);

    String readCString(int addr, Charset charSet);

    String readCString(int addr);

    void write(int addr, byte[] data);

    void write(int addr, byte[] data, int offset, int size);

    byte read(int addr);

    byte[] readBytes(int addr, int len);

    void writeI32(int addr, int data);

    int readInt(int addr);

    long readI32(int addr);

    long readU32(int addr);

    void writeLong(int addr, long data);

    long readLong(int addr);

    long readI64(int addr);

    void writeShort(int addr, short data);

    short readShort(int addr);

    long readI16(int addr);

    long readU16(int addr);

    void writeByte(int addr, byte data);

    long readU8(int addr);

    long readI8(int addr);

    void writeF32(int addr, float data);

    long readF32(int addr);

    float readFloat(int addr);

    void writeF64(int addr, double data);

    double readDouble(int addr);

    long readF64(int addr);

    void zero();

    void fill(byte value);

    @SuppressWarnings("ByteBufferBackingArray")
    void fill(byte value, int fromIndex, int toIndex);

    void copy(int dest, int src, int size);

    void drop(int segment);
}
