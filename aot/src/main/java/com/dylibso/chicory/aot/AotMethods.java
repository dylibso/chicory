package com.dylibso.chicory.aot;

import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import java.lang.reflect.Method;
import java.util.List;

public final class AotMethods {

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
    static final Method VALIDATE_BASE;
    static final Method THROW_OUT_OF_BOUNDS_MEMORY_ACCESS;
    static final Method THROW_TRAP_EXCEPTION;

    static {
        try {
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
}
