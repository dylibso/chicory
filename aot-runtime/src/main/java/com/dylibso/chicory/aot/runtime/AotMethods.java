package com.dylibso.chicory.aot.runtime;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.util.Objects.requireNonNullElse;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.List;

public final class AotMethods {

    private AotMethods() {}

    @UsedByGeneratedCode
    public static Value[] callIndirect(
            Value[] args, int typeId, int funcTableIdx, int tableIdx, Instance instance) {
        TableInstance table = instance.table(tableIdx);

        instance = requireNonNullElse(table.instance(funcTableIdx), instance);

        int funcId = table.ref(funcTableIdx).asFuncRef();
        if (funcId == REF_NULL_VALUE) {
            throw new ChicoryException("uninitialized element " + funcTableIdx);
        }

        FunctionType expectedType = instance.type(typeId);
        FunctionType actualType = instance.type(instance.functionType(funcId));
        if (!actualType.typesMatch(expectedType)) {
            throw new ChicoryException("indirect call type mismatch");
        }

        checkInterruption();
        return instance.getMachine().call(funcId, args);
    }

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
