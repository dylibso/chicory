package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.DataSegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public final class LockStepMemory implements Memory {

    private final Memory memory;
    private final String name;
    private final ArrayBlockingQueue<Event> eventsOut = new ArrayBlockingQueue<>(1);
    private ArrayBlockingQueue<Event> eventsIn;
    private long eventCounter;

    private LockStepMemory(String name, Memory memory) {
        this.name = name;
        this.memory = memory;
    }

    public static LockStepMemory[] create(
            String name1, Memory memory1, String name2, Memory memory2) {
        LockStepMemory lockStepMemory1 = new LockStepMemory(name1, memory1);
        LockStepMemory lockStepMemory2 = new LockStepMemory(name2, memory2);
        lockStepMemory1.eventsIn = lockStepMemory2.eventsOut;
        lockStepMemory2.eventsIn = lockStepMemory1.eventsOut;
        return new LockStepMemory[] {lockStepMemory1, lockStepMemory2};
    }

    public long eventCounter() {
        return eventCounter;
    }

    static final class Event {
        final String name;
        final long eventId;
        final String method;
        final List<Object> args;
        final Object result;

        Event(String name, long eventId, String method, List<Object> args, Object result) {
            this.name = name;
            this.eventId = eventId;
            this.method = method;
            this.args = args;
            this.result = result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Event event = (Event) o;
            return eventId == event.eventId
                    && Objects.equals(method, event.method)
                    && Objects.equals(args, event.args)
                    && Objects.equals(result, event.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, method, args, result);
        }
    }

    static final class ByteArrayWrapper {
        private final byte[] data;

        ByteArrayWrapper(byte[] data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ByteArrayWrapper that = (ByteArrayWrapper) o;
            return Arrays.equals(this.data, that.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }

    private void exchange(String method, Object result, Object... args) {
        exchange(new Event(this.name, eventCounter++, method, List.of(args), result));
    }

    private void exchange(Event expected) {
        try {
            eventsOut.put(expected);
            Event actual = eventsIn.take();
            if (expected.eventId % 1_000_000 == 0) {
                System.out.println(
                        String.format(
                                "%s: %s %d %s %s",
                                name,
                                expected.method,
                                expected.eventId,
                                expected.args,
                                expected.result));
            }

            if (!expected.equals(actual)) {

                throw new IllegalStateException(
                        String.format(
                                "Events of sync: \n"
                                        + "  %s event - id: %d, method: %s, args: %s, result:"
                                        + " %s\n"
                                        + "  %s event - id: %d, method: %s, args: %s, result:"
                                        + " %s\n",
                                expected.name,
                                expected.eventId,
                                expected.method,
                                expected.args,
                                expected.result,
                                actual.name,
                                actual.eventId,
                                actual.method,
                                actual.args,
                                actual.result));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void initPassiveSegment(int segmentId, int dest, int offset, int size) {
        memory.initPassiveSegment(segmentId, dest, offset, size);
        exchange("initPassiveSegment", null, segmentId, dest, offset, size);
    }

    @Override
    public int pages() {
        int result = memory.pages();
        exchange("pages", result);
        return result;
    }

    @Override
    public int grow(int size) {
        int result = memory.grow(size);
        exchange("grow", result, size);
        return result;
    }

    @Override
    public void fill(byte value, int fromIndex, int toIndex) {
        memory.fill(value, fromIndex, toIndex);
        exchange("fill", null, fromIndex, toIndex);
    }

    @Override
    public void writeI32(int addr, int data) {
        memory.writeI32(addr, data);
        exchange("writeI32", null, addr, data);
    }

    @Override
    public void writeLong(int addr, long data) {
        memory.writeLong(addr, data);
        exchange("writeLong", null, addr, data);
    }

    @Override
    public long readF64(int addr) {
        long result = memory.readF64(addr);
        exchange("readF64", result, addr);
        return result;
    }

    @Override
    public void writeByte(int addr, byte data) {
        memory.writeByte(addr, data);
        exchange("writeByte", null, addr, data);
    }

    @Override
    public int initialPages() {
        int result = memory.initialPages();
        exchange("initialPages", result);
        return result;
    }

    @Override
    public int maximumPages() {
        int result = memory.maximumPages();
        exchange("maximumPages", result);
        return result;
    }

    @Override
    public void writeF32(int addr, float data) {
        memory.writeF32(addr, data);
        exchange("writeF32", null, addr, data);
    }

    @Override
    public void initialize(Instance instance, DataSegment[] dataSegments) {
        memory.initialize(instance, dataSegments);
        exchange("initialize", null);
    }

    @Override
    public void zero() {
        memory.zero();
        exchange("zero", null);
    }

    //    private static final VarHandle SHORT_ARR_HANDLE =
    //            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOAT_ARR_HANDLE =
            MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);

    private float toFloat(long value) {
        byte[] bytes = new byte[4];
        INT_ARR_HANDLE.set(bytes, 0, (int) value);
        return (float) FLOAT_ARR_HANDLE.get(bytes, 0);
    }

    //    private static final VarHandle LONG_ARR_HANDLE =
    //            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    //    private static final VarHandle DOUBLE_ARR_HANDLE =
    //            MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);
    //    private float toDouble(long value) {
    //        byte[] bytes = new byte[8];
    //        LONG_ARR_HANDLE.set(bytes, 0, value);
    //        return (float) DOUBLE_ARR_HANDLE.get(bytes, 0);
    //    }

    @Override
    public long readF32(int addr) {
        long result = memory.readF32(addr);
        exchange("readFloat", toFloat(result), addr); // remapped.
        return result;
    }

    @Override
    public float readFloat(int addr) {
        float result = memory.readFloat(addr);
        exchange("readFloat", result, addr);
        return result;
    }

    @Override
    public long readU16(int addr) {
        long result = memory.readU16(addr);
        exchange("readShort", (short) result, addr); // remapped.
        return result;
    }

    @Override
    public short readShort(int addr) {
        short result = memory.readShort(addr);
        exchange("readShort", result, addr);
        return result;
    }

    @Override
    public void writeF64(int addr, double data) {
        memory.writeF64(addr, data);
        exchange("writeF64", null, addr, data);
    }

    @Override
    public byte[] readBytes(int addr, int len) {
        byte[] result = memory.readBytes(addr, len);
        exchange("readBytes", new ByteArrayWrapper(result), addr, len);
        return result;
    }

    @Override
    public void writeShort(int addr, short data) {
        memory.writeShort(addr, data);
        exchange("writeShort", null, addr, data);
    }

    @Override
    public double readDouble(int addr) {
        double result = memory.readDouble(addr);
        exchange("readDouble", result, addr);
        return result;
    }

    @Override
    public void drop(int segment) {
        memory.drop(segment);
        exchange("drop", null, segment);
    }

    @Override
    public long readU32(int addr) {
        long result = memory.readU32(addr);
        exchange("readInt", (int) result, addr); // remapped.
        return result;
    }

    @Override
    public long readI32(int addr) {
        long result = memory.readI32(addr);
        exchange("readInt", (int) result, addr); // remapped.
        return result;
    }

    @Override
    public int readInt(int addr) {
        int result = memory.readInt(addr);
        exchange("readInt", result, addr);
        return result;
    }

    @Override
    public long readI64(int addr) {
        long result = memory.readI64(addr);
        exchange("readLong", result, addr); // remapped.
        return result;
    }

    @Override
    public long readLong(int addr) {
        long result = memory.readLong(addr);
        exchange("readLong", result, addr);
        return result;
    }

    @Override
    public long readU8(int addr) {
        long result = memory.readU8(addr);
        exchange("read", (byte) result, addr); // remapped.
        return result;
    }

    @Override
    public long readI8(int addr) {
        byte result = memory.read(addr);
        exchange("read", result, addr); // remapped.
        return result;
    }

    @Override
    public byte read(int addr) {
        byte result = memory.read(addr);
        exchange("read", result, addr);
        return result;
    }

    @Override
    public void writeString(int offset, String data, Charset charSet) {
        memory.writeString(offset, data, charSet);
        exchange("writeString", null, offset, data, charSet);
    }

    @Override
    public void writeString(int offset, String data) {
        memory.writeString(offset, data);
        exchange("writeString", null, offset, data);
    }

    @Override
    public long readI16(int addr) {
        long result = memory.readI16(addr);
        exchange("readI16", result, addr);
        return result;
    }

    @Override
    public void write(int addr, byte[] data, int offset, int size) {
        memory.write(addr, data, offset, size);
        exchange("write", null, addr, new ByteArrayWrapper(data), offset, size);
    }

    @Override
    public void write(int addr, byte[] data) {
        memory.write(addr, data);
        exchange("write", null, addr, new ByteArrayWrapper(data));
    }

    @Override
    public void copy(int dest, int src, int size) {
        memory.copy(dest, src, size);
        exchange("copy", null, dest, src, size);
    }

    @Override
    public String readCString(int addr) {
        String result = memory.readCString(addr);
        exchange("readCString", result, addr);
        return result;
    }

    @Override
    public String readCString(int addr, Charset charSet) {
        String result = memory.readCString(addr, charSet);
        exchange("readCString", result, addr, charSet);
        return result;
    }

    @Override
    public String readString(int addr, int len) {
        String result = memory.readString(addr, len);
        exchange("readString", result, addr, len);
        return result;
    }

    @Override
    public String readString(int addr, int len, Charset charSet) {
        String result = memory.readString(addr, len, charSet);
        exchange("readString", result, addr, len, charSet);
        return result;
    }

    @Override
    public void writeCString(int offset, String str, Charset charSet) {
        memory.writeCString(offset, str, charSet);
        exchange("writeCString", null, offset, str, charSet);
    }

    @Override
    public void writeCString(int offset, String str) {
        memory.writeCString(offset, str);
        exchange("writeCString", null, offset, str);
    }
}
