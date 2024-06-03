package com.dylibso.chicory.aot;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.Value;
import java.lang.reflect.Method;
import java.util.List;

public final class AotMethods {

    static final Method CHECK_INTERRUPTION;
    static final Method INSTANCE_CALL_HOST_FUNCTION;
    static final Method INSTANCE_READ_GLOBAL;
    static final Method INSTANCE_WRITE_GLOBAL;
    static final Method INSTANCE_SET_ELEMENT;
    static final Method MEMORY_COPY;
    static final Method MEMORY_FILL;
    static final Method MEMORY_INIT;
    static final Method MEMORY_GROW;
    static final Method MEMORY_DROP;
    static final Method MEMORY_PAGES;
    static final Method MEMORY_READ_BYTE;
    static final Method MEMORY_READ_SHORT;
    static final Method MEMORY_READ_INT;
    static final Method MEMORY_READ_LONG;
    static final Method MEMORY_READ_FLOAT;
    static final Method MEMORY_READ_DOUBLE;
    static final Method MEMORY_WRITE_BYTE;
    static final Method MEMORY_WRITE_SHORT;
    static final Method MEMORY_WRITE_INT;
    static final Method MEMORY_WRITE_LONG;
    static final Method MEMORY_WRITE_FLOAT;
    static final Method MEMORY_WRITE_DOUBLE;
    static final Method REF_IS_NULL;
    static final Method TABLE_GET;
    static final Method TABLE_SET;
    static final Method TABLE_SIZE;
    static final Method TABLE_GROW;
    static final Method TABLE_FILL;
    static final Method TABLE_COPY;
    static final Method TABLE_INIT;
    static final Method VALIDATE_BASE;
    static final Method THROW_OUT_OF_BOUNDS_MEMORY_ACCESS;
    static final Method THROW_TRAP_EXCEPTION;

    static {
        try {
            CHECK_INTERRUPTION = AotMethods.class.getMethod("checkInterruption");
            INSTANCE_CALL_HOST_FUNCTION =
                    Instance.class.getMethod("callHostFunction", int.class, Value[].class);
            INSTANCE_READ_GLOBAL = Instance.class.getMethod("readGlobal", int.class);
            INSTANCE_WRITE_GLOBAL = Instance.class.getMethod("writeGlobal", int.class, Value.class);
            INSTANCE_SET_ELEMENT = Instance.class.getMethod("setElement", int.class, Element.class);
            MEMORY_COPY =
                    AotMethods.class.getMethod(
                            "memoryCopy", int.class, int.class, int.class, Memory.class);
            MEMORY_FILL =
                    AotMethods.class.getMethod(
                            "memoryFill", int.class, byte.class, int.class, Memory.class);
            MEMORY_INIT =
                    AotMethods.class.getMethod(
                            "memoryInit", int.class, int.class, int.class, int.class, Memory.class);
            MEMORY_GROW = Memory.class.getMethod("grow", int.class);
            MEMORY_DROP = Memory.class.getMethod("drop", int.class);
            MEMORY_PAGES = Memory.class.getMethod("pages");
            MEMORY_READ_BYTE = Memory.class.getMethod("read", int.class);
            MEMORY_READ_SHORT = Memory.class.getMethod("readShort", int.class);
            MEMORY_READ_INT = Memory.class.getMethod("readInt", int.class);
            MEMORY_READ_LONG = Memory.class.getMethod("readLong", int.class);
            MEMORY_READ_FLOAT = Memory.class.getMethod("readFloat", int.class);
            MEMORY_READ_DOUBLE = Memory.class.getMethod("readDouble", int.class);
            MEMORY_WRITE_BYTE = Memory.class.getMethod("writeByte", int.class, byte.class);
            MEMORY_WRITE_SHORT = Memory.class.getMethod("writeShort", int.class, short.class);
            MEMORY_WRITE_INT = Memory.class.getMethod("writeI32", int.class, int.class);
            MEMORY_WRITE_LONG =
                    AotMethods.class.getMethod(
                            "memoryWriteLong", long.class, int.class, Memory.class);
            MEMORY_WRITE_FLOAT = Memory.class.getMethod("writeF32", int.class, float.class);
            MEMORY_WRITE_DOUBLE =
                    AotMethods.class.getMethod(
                            "memoryWriteDouble", double.class, int.class, Memory.class);
            REF_IS_NULL = AotMethods.class.getMethod("isRefNull", int.class);
            TABLE_GET =
                    AotMethods.class.getMethod("tableGet", int.class, int.class, Instance.class);
            TABLE_SET =
                    AotMethods.class.getMethod(
                            "tableSet", int.class, int.class, int.class, Instance.class);
            TABLE_SIZE = AotMethods.class.getMethod("tableSize", int.class, Instance.class);
            TABLE_GROW =
                    AotMethods.class.getMethod(
                            "tableGrow", int.class, int.class, int.class, Instance.class);
            TABLE_FILL =
                    AotMethods.class.getMethod(
                            "tableFill",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_COPY =
                    AotMethods.class.getMethod(
                            "tableCopy",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_INIT =
                    AotMethods.class.getMethod(
                            "tableInit",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            VALIDATE_BASE = AotMethods.class.getMethod("validateBase", int.class);
            THROW_OUT_OF_BOUNDS_MEMORY_ACCESS =
                    AotMethods.class.getMethod("throwOutOfBoundsMemoryAccess");
            THROW_TRAP_EXCEPTION = AotMethods.class.getMethod("throwTrapException");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private AotMethods() {}

    @UsedByGeneratedCode
    public static boolean isRefNull(int ref) {
        return ref == REF_NULL_VALUE;
    }

    @UsedByGeneratedCode
    public static int tableGet(int index, int tableIndex, Instance instance) {
        return OpcodeImpl.TABLE_GET(instance, tableIndex, index).asFuncRef();
    }

    @UsedByGeneratedCode
    public static void tableSet(int index, int value, int tableIndex, Instance instance) {
        instance.table(tableIndex).setRef(index, value, instance);
    }

    @UsedByGeneratedCode
    public static int tableGrow(int value, int size, int tableIndex, Instance instance) {
        return instance.table(tableIndex).grow(size, value, instance);
    }

    @UsedByGeneratedCode
    public static int tableSize(int tableIndex, Instance instance) {
        return instance.table(tableIndex).size();
    }

    @UsedByGeneratedCode
    public static void tableFill(
            int offset, int value, int size, int tableIndex, Instance instance) {
        OpcodeImpl.TABLE_FILL(instance, tableIndex, size, value, offset);
    }

    @UsedByGeneratedCode
    public static void tableCopy(
            int d, int s, int size, int dstTableIndex, int srcTableIndex, Instance instance) {
        OpcodeImpl.TABLE_COPY(instance, srcTableIndex, dstTableIndex, size, s, d);
    }

    @UsedByGeneratedCode
    public static void tableInit(
            int offset, int elemidx, int size, int elementidx, int tableidx, Instance instance) {
        OpcodeImpl.TABLE_INIT(instance, tableidx, elementidx, size, elemidx, offset);
    }

    @UsedByGeneratedCode
    public static void memoryCopy(int destination, int offset, int size, Memory memory) {
        memory.copy(destination, offset, size);
    }

    @UsedByGeneratedCode
    public static void memoryFill(int offset, byte value, int size, Memory memory) {
        int end = size + offset;
        memory.fill(value, offset, end);
    }

    @UsedByGeneratedCode
    public static void memoryInit(
            int destination, int offset, int size, int segmentId, Memory memory) {
        memory.initPassiveSegment(segmentId, destination, offset, size);
    }

    @UsedByGeneratedCode
    public static void memoryWriteLong(long value, int address, Memory memory) {
        memory.writeLong(address, value);
    }

    @UsedByGeneratedCode
    public static void memoryWriteDouble(double value, int address, Memory memory) {
        memory.writeF64(address, value);
    }

    @UsedByGeneratedCode
    public static void validateBase(int base) {
        if (base < 0) {
            throwOutOfBoundsMemoryAccess();
        }
    }

    @UsedByGeneratedCode
    public static RuntimeException throwOutOfBoundsMemoryAccess() {
        throw new WASMRuntimeException("out of bounds memory access");
    }

    @UsedByGeneratedCode
    public static RuntimeException throwTrapException() {
        throw new TrapException("Trapped on unreachable instruction", List.of());
    }

    @UsedByGeneratedCode
    public static void checkInterruption() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ChicoryException("Thread interrupted");
        }
    }
}
