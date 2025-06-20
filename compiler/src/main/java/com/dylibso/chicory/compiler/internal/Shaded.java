package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.runtime.MemCopyWorkaround.shouldUseMemWorkaround;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.ChicoryInterruptedException;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.MemCopyWorkaround;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.WasmException;
import com.dylibso.chicory.runtime.WasmRuntimeException;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.types.FunctionType;

/**
 * This class will get shaded into the compiled code.
 */
public final class Shaded {

    private Shaded() {}

    public static long[] callIndirect(long[] args, int typeId, int funcId, Instance instance) {
        FunctionType expectedType = instance.type(typeId);
        FunctionType actualType = instance.type(instance.functionType(funcId));
        if (!actualType.typesMatch(expectedType)) {
            throw throwIndirectCallTypeMismatch();
        }
        return instance.getMachine().call(funcId, args);
    }

    public static long[] callIndirect(long[] args, int funcId, Instance instance) {
        return instance.getMachine().call(funcId, args);
    }

    public static long[] callHostFunction(Instance instance, int funcId, long[] args) {
        var imprt = instance.imports().function(funcId);
        return imprt.handle().apply(instance, args);
    }

    public static boolean isRefNull(int ref) {
        return ref == REF_NULL_VALUE;
    }

    public static int tableGet(int index, int tableIndex, Instance instance) {
        return OpcodeImpl.TABLE_GET(instance, tableIndex, index);
    }

    public static void tableSet(int index, int value, int tableIndex, Instance instance) {
        instance.table(tableIndex).setRef(index, value, instance);
    }

    public static int tableGrow(int value, int size, int tableIndex, Instance instance) {
        return instance.table(tableIndex).grow(size, value, instance);
    }

    public static int tableSize(int tableIndex, Instance instance) {
        return instance.table(tableIndex).size();
    }

    public static void tableFill(
            int offset, int value, int size, int tableIndex, Instance instance) {
        OpcodeImpl.TABLE_FILL(instance, tableIndex, size, value, offset);
    }

    public static void tableCopy(
            int d, int s, int size, int dstTableIndex, int srcTableIndex, Instance instance) {
        OpcodeImpl.TABLE_COPY(instance, srcTableIndex, dstTableIndex, size, s, d);
    }

    public static void tableInit(
            int offset, int elemidx, int size, int elementidx, int tableidx, Instance instance) {
        OpcodeImpl.TABLE_INIT(instance, tableidx, elementidx, size, elemidx, offset);
    }

    private static final boolean memCopyWorkaround;

    static {
        var prop = System.getProperty("chicory.memCopyWorkaround");

        if (prop != null) {
            memCopyWorkaround = Boolean.valueOf(prop);
        } else {
            memCopyWorkaround = shouldUseMemWorkaround();
        }
    }

    public static void memoryCopy(int destination, int offset, int size, Memory memory) {
        if (memCopyWorkaround) {
            // Use this workaround to avoid a bug in some JVMs (Temurin 17)
            MemCopyWorkaround.memoryCopy(destination, offset, size, memory);
        } else {
            // Go back to the original implementation, once that bug is no longer an issue:
            memory.copy(destination, offset, size);
        }
    }

    public static void memoryFill(int offset, byte value, int size, Memory memory) {
        int end = size + offset;
        memory.fill(value, offset, end);
    }

    public static void memoryInit(
            int destination, int offset, int size, int segmentId, Memory memory) {
        memory.initPassiveSegment(segmentId, destination, offset, size);
    }

    public static int memoryGrow(int size, Memory memory) {
        return memory.grow(size);
    }

    public static void memoryDrop(int segment, Memory memory) {
        memory.drop(segment);
    }

    public static int memoryPages(Memory memory) {
        return memory.pages();
    }

    public static byte memoryReadByte(int base, int offset, Memory memory) {
        return memory.read(getAddr(base, offset));
    }

    public static short memoryReadShort(int base, int offset, Memory memory) {
        return memory.readShort(getAddr(base, offset));
    }

    public static int memoryReadInt(int base, int offset, Memory memory) {
        return memory.readInt(getAddr(base, offset));
    }

    public static long memoryReadLong(int base, int offset, Memory memory) {
        return memory.readLong(getAddr(base, offset));
    }

    public static float memoryReadFloat(int base, int offset, Memory memory) {
        return memory.readFloat(getAddr(base, offset));
    }

    public static double memoryReadDouble(int base, int offset, Memory memory) {
        return memory.readDouble(getAddr(base, offset));
    }

    public static void memoryWriteByte(int base, byte value, int offset, Memory memory) {
        memory.writeByte(getAddr(base, offset), value);
    }

    public static void memoryWriteShort(int base, short value, int offset, Memory memory) {
        memory.writeShort(getAddr(base, offset), value);
    }

    public static void memoryWriteInt(int base, int value, int offset, Memory memory) {
        memory.writeI32(getAddr(base, offset), value);
    }

    public static void memoryWriteLong(int base, long value, int offset, Memory memory) {
        memory.writeLong(getAddr(base, offset), value);
    }

    public static void memoryWriteFloat(int base, float value, int offset, Memory memory) {
        memory.writeF32(getAddr(base, offset), value);
    }

    public static void memoryWriteDouble(int base, double value, int offset, Memory memory) {
        memory.writeF64(getAddr(base, offset), value);
    }

    // let the following memory access throw if the base is negative
    public static int getAddr(int base, int offset) {
        return (base < 0) ? base : base + offset;
    }

    public static RuntimeException throwCallStackExhausted(StackOverflowError e) {
        throw new ChicoryException("call stack exhausted", e);
    }

    public static RuntimeException throwIndirectCallTypeMismatch() {
        return new ChicoryException("indirect call type mismatch");
    }

    public static RuntimeException throwOutOfBoundsMemoryAccess() {
        throw new WasmRuntimeException("out of bounds memory access");
    }

    public static RuntimeException throwTrapException() {
        throw new TrapException("Trapped on unreachable instruction");
    }

    public static RuntimeException throwUnknownFunction(int index) {
        throw new InvalidException(String.format("unknown function %d", index));
    }

    public static void checkInterruption() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ChicoryInterruptedException("Thread interrupted");
        }
    }

    public static long readGlobal(int index, Instance instance) {
        return instance.global(index).getValue();
    }

    public static void writeGlobal(long value, int index, Instance instance) {
        instance.global(index).setValue(value);
    }

    /**
     * Creates a WasmException for the given tag and arguments
     */
    public static WasmException createWasmException(long[] args, int tagNumber, Instance instance) {
        if (args == null) {
            args = new long[0];
        }
        WasmException e = new WasmException(instance, tagNumber, args);
        instance.registerException(e);
        return e;
    }

    public static boolean exceptionMatches(WasmException exception, int tag, Instance instance) {
        if (exception.instance() == instance && exception.tagIdx() == tag) {
            return true;
        }

        var currentCatchTag = instance.tag(tag);
        var exceptionTag = exception.instance().tag(exception.tagIdx());
        return tag < instance.imports().tagCount()
                && currentCatchTag.type().typesMatch(exceptionTag.type())
                && currentCatchTag.type().returnsMatch(exceptionTag.type());
    }
}
