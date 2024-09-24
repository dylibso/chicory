package com.dylibso.chicory.aot;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.util.Objects.requireNonNullElse;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.lang.reflect.Method;
import java.util.List;

public final class AotMethods {

    static final Method CHECK_INTERRUPTION;
    static final Method CALL_INDIRECT;
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
            CALL_INDIRECT =
                    AotMethods.class.getMethod(
                            "callIndirect",
                            long[].class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            INSTANCE_CALL_HOST_FUNCTION =
                    Instance.class.getMethod("callHostFunction", int.class, long[].class);
            INSTANCE_READ_GLOBAL = Instance.class.getMethod("readGlobal", int.class);
            INSTANCE_WRITE_GLOBAL = Instance.class.getMethod("writeGlobal", int.class, long.class);
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
            MEMORY_READ_BYTE =
                    AotMethods.class.getMethod(
                            "memoryReadByte", int.class, int.class, Memory.class);
            MEMORY_READ_SHORT =
                    AotMethods.class.getMethod(
                            "memoryReadShort", int.class, int.class, Memory.class);
            MEMORY_READ_INT =
                    AotMethods.class.getMethod("memoryReadInt", int.class, int.class, Memory.class);
            MEMORY_READ_LONG =
                    AotMethods.class.getMethod(
                            "memoryReadLong", int.class, int.class, Memory.class);
            MEMORY_READ_FLOAT =
                    AotMethods.class.getMethod(
                            "memoryReadFloat", int.class, int.class, Memory.class);
            MEMORY_READ_DOUBLE =
                    AotMethods.class.getMethod(
                            "memoryReadDouble", int.class, int.class, Memory.class);
            MEMORY_WRITE_BYTE =
                    AotMethods.class.getMethod(
                            "memoryWriteByte", int.class, byte.class, int.class, Memory.class);
            MEMORY_WRITE_SHORT =
                    AotMethods.class.getMethod(
                            "memoryWriteShort", int.class, short.class, int.class, Memory.class);
            MEMORY_WRITE_INT =
                    AotMethods.class.getMethod(
                            "memoryWriteInt", int.class, int.class, int.class, Memory.class);
            MEMORY_WRITE_LONG =
                    AotMethods.class.getMethod(
                            "memoryWriteLong", int.class, long.class, int.class, Memory.class);
            MEMORY_WRITE_FLOAT =
                    AotMethods.class.getMethod(
                            "memoryWriteFloat", int.class, float.class, int.class, Memory.class);
            MEMORY_WRITE_DOUBLE =
                    AotMethods.class.getMethod(
                            "memoryWriteDouble", int.class, double.class, int.class, Memory.class);
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
    public static long[] callIndirect(
            long[] args, int typeId, int funcTableIdx, int tableIdx, Instance instance) {
        TableInstance table = instance.table(tableIdx);

        instance = requireNonNullElse(table.instance(funcTableIdx), instance);

        int funcId = (int) table.ref(funcTableIdx);
        if (funcId == REF_NULL_VALUE) {
            throw new ChicoryException("uninitialized element " + funcTableIdx);
        }

        FunctionType expectedType = instance.type(typeId);
        FunctionType actualType = instance.type(instance.functionType(funcId));
        if (!actualType.typesMatch(expectedType)) {
            throw new ChicoryException("indirect call type mismatch");
        }

        checkInterruption();
        // TODO: verify
        // here we should not pass through the external "call" method
        // but directly emit the invocation of the underlying function
        return instance.getMachine().call(funcId, args);
    }

    @UsedByGeneratedCode
    public static boolean isRefNull(int ref) {
        return ref == REF_NULL_VALUE;
    }

    @UsedByGeneratedCode
    public static int tableGet(int index, int tableIndex, Instance instance) {
        return (int) OpcodeImpl.TABLE_GET(instance, tableIndex, index);
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
    public static byte memoryReadByte(int base, int offset, Memory memory) {
        validateBase(base);
        return memory.read(base + offset);
    }

    @UsedByGeneratedCode
    public static short memoryReadShort(int base, int offset, Memory memory) {
        validateBase(base);
        return memory.readShort(base + offset);
    }

    @UsedByGeneratedCode
    public static int memoryReadInt(int base, int offset, Memory memory) {
        validateBase(base);
        return memory.readInt(base + offset);
    }

    @UsedByGeneratedCode
    public static long memoryReadLong(int base, int offset, Memory memory) {
        validateBase(base);
        return memory.readLong(base + offset);
    }

    @UsedByGeneratedCode
    public static float memoryReadFloat(int base, int offset, Memory memory) {
        validateBase(base);
        return memory.readFloat(base + offset);
    }

    @UsedByGeneratedCode
    public static double memoryReadDouble(int base, int offset, Memory memory) {
        validateBase(base);
        return memory.readDouble(base + offset);
    }

    @UsedByGeneratedCode
    public static void memoryWriteByte(int base, byte value, int offset, Memory memory) {
        validateBase(base);
        memory.writeByte(base + offset, value);
    }

    @UsedByGeneratedCode
    public static void memoryWriteShort(int base, short value, int offset, Memory memory) {
        validateBase(base);
        memory.writeShort(base + offset, value);
    }

    @UsedByGeneratedCode
    public static void memoryWriteInt(int base, int value, int offset, Memory memory) {
        validateBase(base);
        memory.writeI32(base + offset, value);
    }

    @UsedByGeneratedCode
    public static void memoryWriteLong(int base, long value, int offset, Memory memory) {
        validateBase(base);
        memory.writeLong(base + offset, value);
    }

    @UsedByGeneratedCode
    public static void memoryWriteFloat(int base, float value, int offset, Memory memory) {
        validateBase(base);
        memory.writeF32(base + offset, value);
    }

    @UsedByGeneratedCode
    public static void memoryWriteDouble(int base, double value, int offset, Memory memory) {
        validateBase(base);
        memory.writeF64(base + offset, value);
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
