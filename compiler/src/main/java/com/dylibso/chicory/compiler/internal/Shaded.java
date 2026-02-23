package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.runtime.MemCopyWorkaround.shouldUseMemWorkaround;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.ChicoryInterruptedException;
import com.dylibso.chicory.runtime.ConstantEvaluators;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.MemCopyWorkaround;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.WasmArray;
import com.dylibso.chicory.runtime.WasmException;
import com.dylibso.chicory.runtime.WasmI31Ref;
import com.dylibso.chicory.runtime.WasmRuntimeException;
import com.dylibso.chicory.runtime.WasmStruct;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.Arrays;

/**
 * This class will get shaded into the compiled code.
 */
public final class Shaded {

    private Shaded() {}

    public static long[] callIndirect(long[] args, int typeId, int funcId, Instance instance) {
        int actualTypeIdx = instance.functionType(funcId);
        if (actualTypeIdx != typeId
                && !ValType.heapTypeSubtype(
                        actualTypeIdx, typeId, instance.module().typeSection())) {
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

    public static int refAsNonNull(int ref) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null reference");
        }
        return ref;
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

    public static int i32_ge_u(int a, int b) {
        if (memCopyWorkaround) {
            // Use this workaround to avoid a bug in some JVMs (Temurin 17)
            return MemCopyWorkaround.i32_ge_u(a, b);
        } else {
            // Go back to the original implementation, once that bug is no longer an issue:
            return OpcodeImpl.I32_GE_U(a, b);
        }
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

    public static int memoryAtomicIntByteRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicReadByte(ptr));
    }

    public static int memoryAtomicIntShortRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(memory.atomicReadShort(ptr));
    }

    public static int memoryAtomicIntRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicReadInt(ptr);
    }

    public static long memoryAtomicLongRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicReadLong(ptr);
    }

    public static long memoryAtomicLongByteRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicReadByte(ptr));
    }

    public static long memoryAtomicLongShortRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicReadShort(ptr));
    }

    public static long memoryAtomicLongIntRead(int base, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicReadInt(ptr));
    }

    public static void memoryAtomicIntWrite(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        memory.atomicWriteInt(ptr, value);
    }

    public static void memoryAtomicIntByteWrite(int base, byte value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        memory.atomicWriteByte(ptr, value);
    }

    public static void memoryAtomicIntShortWrite(int base, short value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        memory.atomicWriteShort(ptr, value);
    }

    public static void memoryAtomicLongWrite(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        memory.atomicWriteLong(ptr, value);
    }

    public static void memoryAtomicLongByteWrite(int base, byte value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        memory.atomicWriteByte(ptr, value);
    }

    public static void memoryAtomicLongShortWrite(
            int base, short value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        memory.atomicWriteShort(ptr, value);
    }

    public static void memoryAtomicLongIntWrite(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        memory.atomicWriteInt(ptr, value);
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

    public static RuntimeException throwNullFunctionReference() {
        throw new TrapException("null function reference");
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

    public static int readGlobalRef(int index, Instance instance) {
        long val = instance.global(index).getValue();
        if (Value.isI31(val)) {
            var i31 = new WasmI31Ref(Value.decodeI31U(val));
            return instance.registerGcRef(i31);
        }
        return (int) val;
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

    // I32 32-bit RMW ops
    public static int memoryAtomicIntRmwAdd(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAddInt(ptr, value);
    }

    public static int memoryAtomicIntRmwSub(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAddInt(ptr, -value);
    }

    public static int memoryAtomicIntRmwAnd(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAndInt(ptr, value);
    }

    public static int memoryAtomicIntRmwOr(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicOrInt(ptr, value);
    }

    public static int memoryAtomicIntRmwXor(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicXorInt(ptr, value);
    }

    public static int memoryAtomicIntRmwXchg(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicXchgInt(ptr, value);
    }

    public static int memoryAtomicIntRmwCmpxchg(
            int base, int expected, int replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicCmpxchgInt(ptr, expected, replacement);
    }

    // I32 8-bit RMW ops
    public static int memoryAtomicIntRmw8AddU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicAddByte(ptr, (byte) value));
    }

    public static int memoryAtomicIntRmw8SubU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicAddByte(ptr, (byte) -value));
    }

    public static int memoryAtomicIntRmw8AndU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicAndByte(ptr, (byte) value));
    }

    public static int memoryAtomicIntRmw8OrU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicOrByte(ptr, (byte) value));
    }

    public static int memoryAtomicIntRmw8XorU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicXorByte(ptr, (byte) value));
    }

    public static int memoryAtomicIntRmw8XchgU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(memory.atomicXchgByte(ptr, (byte) value));
    }

    public static int memoryAtomicIntRmw8CmpxchgU(
            int base, int expected, int replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedInt(
                memory.atomicCmpxchgByte(ptr, (byte) expected, (byte) replacement));
    }

    // I32 16-bit RMW ops
    public static int memoryAtomicIntRmw16AddU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAddShort(ptr, (short) value) & 0xFFFF;
    }

    public static int memoryAtomicIntRmw16SubU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(memory.atomicAddShort(ptr, (short) -value));
    }

    public static int memoryAtomicIntRmw16AndU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(memory.atomicAndShort(ptr, (short) value));
    }

    public static int memoryAtomicIntRmw16OrU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(memory.atomicOrShort(ptr, (short) value));
    }

    public static int memoryAtomicIntRmw16XorU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(memory.atomicXorShort(ptr, (short) value));
    }

    public static int memoryAtomicIntRmw16XchgU(int base, int value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(memory.atomicXchgShort(ptr, (short) value));
    }

    public static int memoryAtomicIntRmw16CmpxchgU(
            int base, int expected, int replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedInt(
                memory.atomicCmpxchgShort(ptr, (short) expected, (short) replacement));
    }

    // I64 8-bit RMW ops
    public static long memoryAtomicLongRmw8AddU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicAddByte(ptr, (byte) value));
    }

    public static long memoryAtomicLongRmw8SubU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicAddByte(ptr, (byte) -value));
    }

    public static long memoryAtomicLongRmw8AndU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicAndByte(ptr, (byte) value));
    }

    public static long memoryAtomicLongRmw8OrU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicOrByte(ptr, (byte) value));
    }

    public static long memoryAtomicLongRmw8XorU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicXorByte(ptr, (byte) value));
    }

    public static long memoryAtomicLongRmw8XchgU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(memory.atomicXchgByte(ptr, (byte) value));
    }

    public static long memoryAtomicLongRmw8CmpxchgU(
            int base, long expected, long replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return Byte.toUnsignedLong(
                memory.atomicCmpxchgByte(ptr, (byte) expected, (byte) replacement));
    }

    // I64 16-bit RMW ops
    public static long memoryAtomicLongRmw16AddU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicAddShort(ptr, (short) value));
    }

    public static long memoryAtomicLongRmw16SubU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicAddShort(ptr, (short) -value));
    }

    public static long memoryAtomicLongRmw16AndU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicAndShort(ptr, (short) value));
    }

    public static long memoryAtomicLongRmw16OrU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicOrShort(ptr, (short) value));
    }

    public static long memoryAtomicLongRmw16XorU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicXorShort(ptr, (short) value));
    }

    public static long memoryAtomicLongRmw16XchgU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(memory.atomicXchgShort(ptr, (short) value));
    }

    public static long memoryAtomicLongRmw16CmpxchgU(
            int base, long expected, long replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Short.toUnsignedLong(
                memory.atomicCmpxchgShort(ptr, (short) expected, (short) replacement));
    }

    // I64 32-bit RMW ops
    public static long memoryAtomicLongRmw32AddU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicAddInt(ptr, (int) value));
    }

    public static long memoryAtomicLongRmw32SubU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicAddInt(ptr, (int) -value));
    }

    public static long memoryAtomicLongRmw32AndU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicAndInt(ptr, (int) value));
    }

    public static long memoryAtomicLongRmw32OrU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicOrInt(ptr, (int) value));
    }

    public static long memoryAtomicLongRmw32XorU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicXorInt(ptr, (int) value));
    }

    public static long memoryAtomicLongRmw32XchgU(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(memory.atomicXchgInt(ptr, (int) value));
    }

    public static long memoryAtomicLongRmw32CmpxchgU(
            int base, long expected, long replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return Integer.toUnsignedLong(
                memory.atomicCmpxchgInt(ptr, (int) expected, (int) replacement));
    }

    // I64 64-bit RMW ops
    public static long memoryAtomicLongRmwAdd(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAddLong(ptr, value);
    }

    public static long memoryAtomicLongRmwSub(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAddLong(ptr, -value);
    }

    public static long memoryAtomicLongRmwAnd(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicAndLong(ptr, value);
    }

    public static long memoryAtomicLongRmwOr(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicOrLong(ptr, value);
    }

    public static long memoryAtomicLongRmwXor(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicXorLong(ptr, value);
    }

    public static long memoryAtomicLongRmwXchg(int base, long value, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicXchgLong(ptr, value);
    }

    public static long memoryAtomicLongRmwCmpxchg(
            int base, long expected, long replacement, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicCmpxchgLong(ptr, expected, replacement);
    }

    // Wait/Notify
    public static int memoryAtomicWait32(
            int base, int expected, long timeout, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicWait(ptr, expected, timeout);
    }

    public static int memoryAtomicWait64(
            int base, long expected, long timeout, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        return memory.atomicWait(ptr, expected, timeout);
    }

    public static int memoryAtomicNotify(int base, int count, int offset, Memory memory) {
        var ptr = getAddr(base, offset);
        return memory.atomicNotify(ptr, count);
    }

    public static void memoryAtomicFence(Memory memory) {
        memory.atomicFence();
    }

    // ========= GC Operations =========

    public static int structNew(long[] fields, int typeIdx, Instance instance) {
        var struct = new WasmStruct(typeIdx, fields);
        return instance.registerGcRef(struct);
    }

    public static int structNewDefault(int typeIdx, Instance instance) {
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var fields = new long[st.fieldTypes().length];
        for (int i = 0; i < fields.length; i++) {
            var ft = st.fieldTypes()[i];
            if (ft.storageType().valType() != null && ft.storageType().valType().isReference()) {
                fields[i] = Value.REF_NULL_VALUE;
            }
        }
        var struct = new WasmStruct(typeIdx, fields);
        return instance.registerGcRef(struct);
    }

    public static long structGet(int ref, int typeIdx, int fieldIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null structure reference");
        }
        var struct = (WasmStruct) instance.gcRef(ref);
        return struct.field(fieldIdx);
    }

    public static long structGetS(int ref, int typeIdx, int fieldIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null structure reference");
        }
        var struct = (WasmStruct) instance.gcRef(ref);
        var val = struct.field(fieldIdx);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var ft = st.fieldTypes()[fieldIdx];
        if (ft.storageType().packedType() != null) {
            val = ft.storageType().packedType().signExtend(val);
        }
        return val;
    }

    public static long structGetU(int ref, int typeIdx, int fieldIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null structure reference");
        }
        var struct = (WasmStruct) instance.gcRef(ref);
        var val = struct.field(fieldIdx);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var ft = st.fieldTypes()[fieldIdx];
        if (ft.storageType().packedType() != null) {
            val = val & ft.storageType().packedType().mask();
        }
        return val;
    }

    public static void structSet(int ref, long val, int typeIdx, int fieldIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null structure reference");
        }
        var struct = (WasmStruct) instance.gcRef(ref);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var ft = st.fieldTypes()[fieldIdx];
        if (ft.storageType().packedType() != null) {
            val = val & ft.storageType().packedType().mask();
        }
        struct.setField(fieldIdx, val);
    }

    public static int arrayNew(long initVal, int len, int typeIdx, Instance instance) {
        var elems = new long[len];
        Arrays.fill(elems, initVal);
        var arr = new WasmArray(typeIdx, elems);
        return instance.registerGcRef(arr);
    }

    public static int arrayNewDefault(int len, int typeIdx, Instance instance) {
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        var elems = new long[len];
        if (at.fieldType().storageType().valType() != null
                && at.fieldType().storageType().valType().isReference()) {
            Arrays.fill(elems, Value.REF_NULL_VALUE);
        }
        var arr = new WasmArray(typeIdx, elems);
        return instance.registerGcRef(arr);
    }

    public static int arrayNewFixed(long[] vals, int typeIdx, Instance instance) {
        var arr = new WasmArray(typeIdx, vals);
        return instance.registerGcRef(arr);
    }

    public static int arrayNewData(
            int offset, int len, int typeIdx, int dataIdx, Instance instance) {
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        var elemSize = at.fieldType().storageType().byteSize();
        var data = instance.dataSegmentData(dataIdx);
        if ((long) offset + (long) len * elemSize > data.length) {
            throw new TrapException("out of bounds memory access");
        }
        var elems = new long[len];
        for (int i = 0; i < len; i++) {
            var byteOff = offset + i * elemSize;
            elems[i] = readFromData(data, byteOff, elemSize);
        }
        var arr = new WasmArray(typeIdx, elems);
        return instance.registerGcRef(arr);
    }

    public static int arrayNewElem(
            int offset, int len, int typeIdx, int elemIdx, Instance instance) {
        var element = instance.element(elemIdx);
        if (element == null || offset + len > element.elementCount()) {
            throw new TrapException("out of bounds table access");
        }
        var elems = new long[len];
        for (int i = 0; i < len; i++) {
            elems[i] =
                    elementValueToRef(computeElementValue(instance, elemIdx, offset + i), instance);
        }
        var arr = new WasmArray(typeIdx, elems);
        return instance.registerGcRef(arr);
    }

    public static long arrayGet(int ref, int idx, int typeIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (idx < 0 || idx >= arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        return arr.get(idx);
    }

    public static long arrayGetS(int ref, int idx, int typeIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (idx < 0 || idx >= arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var val = arr.get(idx);
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            val = at.fieldType().storageType().packedType().signExtend(val);
        }
        return val;
    }

    public static long arrayGetU(int ref, int idx, int typeIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (idx < 0 || idx >= arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var val = arr.get(idx);
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            val = val & at.fieldType().storageType().packedType().mask();
        }
        return val;
    }

    public static void arraySet(int ref, int idx, long val, int typeIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (idx < 0 || idx >= arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            val = val & at.fieldType().storageType().packedType().mask();
        }
        arr.set(idx, val);
    }

    public static int arrayLen(int ref, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        return arr.length();
    }

    public static void arrayFill(
            int ref, int offset, long val, int len, int typeIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (offset + len > arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            val = val & at.fieldType().storageType().packedType().mask();
        }
        for (int i = 0; i < len; i++) {
            arr.set(offset + i, val);
        }
    }

    public static void arrayCopy(
            int dstRef, int dstOff, int srcRef, int srcOff, int len, Instance instance) {
        if (dstRef == REF_NULL_VALUE || srcRef == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var dst = (WasmArray) instance.gcRef(dstRef);
        var src = (WasmArray) instance.gcRef(srcRef);
        if (dstOff + len > dst.length() || srcOff + len > src.length()) {
            throw new TrapException("out of bounds array access");
        }
        if (dstOff <= srcOff) {
            for (int i = 0; i < len; i++) {
                dst.set(dstOff + i, src.get(srcOff + i));
            }
        } else {
            for (int i = len - 1; i >= 0; i--) {
                dst.set(dstOff + i, src.get(srcOff + i));
            }
        }
    }

    public static void arrayInitData(
            int ref, int dstOff, int srcOff, int len, int typeIdx, int dataIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        var elemSize = at.fieldType().storageType().byteSize();
        var data = instance.dataSegmentData(dataIdx);
        if (dstOff + len > arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        if ((long) srcOff + (long) len * elemSize > data.length) {
            throw new TrapException("out of bounds memory access");
        }
        for (int i = 0; i < len; i++) {
            var byteOff = srcOff + i * elemSize;
            arr.set(dstOff + i, readFromData(data, byteOff, elemSize));
        }
    }

    public static void arrayInitElem(
            int ref, int dstOff, int srcOff, int len, int typeIdx, int elemIdx, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        var element = instance.element(elemIdx);
        if (dstOff + len > arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var elementCount = (element == null) ? 0 : element.elementCount();
        if (srcOff + len > elementCount) {
            throw new TrapException("out of bounds table access");
        }
        if (len == 0) {
            return;
        }
        for (int i = 0; i < len; i++) {
            arr.set(
                    dstOff + i,
                    elementValueToRef(
                            computeElementValue(instance, elemIdx, srcOff + i), instance));
        }
    }

    public static int refTest(int ref, int heapType, int srcHeapType, Instance instance) {
        return instance.heapTypeMatch(ref, false, heapType, srcHeapType) ? 1 : 0;
    }

    public static int refTestNull(int ref, int heapType, int srcHeapType, Instance instance) {
        return instance.heapTypeMatch(ref, true, heapType, srcHeapType) ? 1 : 0;
    }

    public static int castTest(int ref, int heapType, int srcHeapType, Instance instance) {
        if (!instance.heapTypeMatch(ref, false, heapType, srcHeapType)) {
            throw new TrapException("cast failure");
        }
        return ref;
    }

    public static int castTestNull(int ref, int heapType, int srcHeapType, Instance instance) {
        if (!instance.heapTypeMatch(ref, true, heapType, srcHeapType)) {
            throw new TrapException("cast failure");
        }
        return ref;
    }

    public static boolean heapTypeMatch(
            int ref, boolean nullable, int heapType, int srcHeapType, Instance instance) {
        return instance.heapTypeMatch(ref, nullable, heapType, srcHeapType);
    }

    public static int refI31(int val, Instance instance) {
        var i31 = new WasmI31Ref(val & 0x7FFFFFFF);
        return instance.registerGcRef(i31);
    }

    public static int i31GetS(int ref, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null i31 reference");
        }
        var i31 = (WasmI31Ref) instance.gcRef(ref);
        int val = i31.value();
        // sign extend from 31 bits
        return (val << 1) >> 1;
    }

    public static int i31GetU(int ref, Instance instance) {
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null i31 reference");
        }
        var i31 = (WasmI31Ref) instance.gcRef(ref);
        return i31.value() & 0x7FFFFFFF;
    }

    public static int refEq(int a, int b, Instance instance) {
        if (a == b) {
            return 1;
        }
        if (a == REF_NULL_VALUE || b == REF_NULL_VALUE) {
            return 0;
        }
        var gcA = instance.gcRef(a);
        var gcB = instance.gcRef(b);
        if (gcA instanceof WasmI31Ref && gcB instanceof WasmI31Ref) {
            return ((WasmI31Ref) gcA).value() == ((WasmI31Ref) gcB).value() ? 1 : 0;
        }
        return 0;
    }

    private static long elementValueToRef(long val, Instance instance) {
        if (Value.isI31(val)) {
            var i31 = new WasmI31Ref(Value.decodeI31U(val));
            return instance.registerGcRef(i31);
        }
        return val;
    }

    private static long computeElementValue(Instance instance, int elemIdx, int offset) {
        var element = instance.element(elemIdx);
        var init = element.initializers().get(offset);
        return ConstantEvaluators.computeConstantValue(instance, init)[0];
    }

    private static long readFromData(byte[] data, int offset, int size) {
        long val = 0;
        for (int i = 0; i < size; i++) {
            val |= (long) (data[offset + i] & 0xFF) << (i * 8);
        }
        return val;
    }

    public static void dataDrop(int segment, Instance instance) {
        if (instance.memory() != null) {
            instance.memory().drop(segment);
        } else {
            instance.dropDataSegment(segment);
        }
    }
}
