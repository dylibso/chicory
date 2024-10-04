package com.dylibso.chicory.aot;

import com.dylibso.chicory.aot.runtime.AotMethods;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Element;
import java.lang.reflect.Method;

public final class AotMethodRefs {

    static final Method CHECK_INTERRUPTION;
    static final Method CALL_INDIRECT;
    static final Method INSTANCE_MEMORY;
    static final Method INSTANCE_CALL_HOST_FUNCTION;
    static final Method INSTANCE_READ_GLOBAL;
    static final Method WRITE_GLOBAL;
    static final Method INSTANCE_SET_ELEMENT;
    static final Method INSTANCE_TABLE;
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
    static final Method TABLE_REF;
    static final Method TABLE_INSTANCE;
    static final Method VALIDATE_BASE;
    static final Method THROW_CALL_STACK_EXHAUSTED;
    static final Method THROW_INDIRECT_CALL_TYPE_MISMATCH;
    static final Method THROW_OUT_OF_BOUNDS_MEMORY_ACCESS;
    static final Method THROW_TRAP_EXCEPTION;
    static final Method THROW_UNKNOWN_FUNCTION;

    static {
        try {
            CHECK_INTERRUPTION = AotMethods.class.getMethod("checkInterruption");
            CALL_INDIRECT =
                    AotMethods.class.getMethod(
                            "callIndirect", long[].class, int.class, int.class, Instance.class);
            INSTANCE_MEMORY = Instance.class.getMethod("memory");
            INSTANCE_CALL_HOST_FUNCTION =
                    Instance.class.getMethod("callHostFunction", int.class, long[].class);
            INSTANCE_READ_GLOBAL = Instance.class.getMethod("readGlobal", int.class);
            WRITE_GLOBAL =
                    AotMethods.class.getMethod(
                            "writeGlobal", long.class, int.class, Instance.class);
            INSTANCE_SET_ELEMENT = Instance.class.getMethod("setElement", int.class, Element.class);
            INSTANCE_TABLE = Instance.class.getMethod("table", int.class);
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
            TABLE_REF = AotMethods.class.getMethod("tableRef", TableInstance.class, int.class);
            TABLE_INSTANCE = TableInstance.class.getMethod("instance", int.class);
            VALIDATE_BASE = AotMethods.class.getMethod("validateBase", int.class);
            THROW_CALL_STACK_EXHAUSTED =
                    AotMethods.class.getMethod("throwCallStackExhausted", StackOverflowError.class);
            THROW_INDIRECT_CALL_TYPE_MISMATCH =
                    AotMethods.class.getMethod("throwIndirectCallTypeMismatch");
            THROW_OUT_OF_BOUNDS_MEMORY_ACCESS =
                    AotMethods.class.getMethod("throwOutOfBoundsMemoryAccess");
            THROW_TRAP_EXCEPTION = AotMethods.class.getMethod("throwTrapException");
            THROW_UNKNOWN_FUNCTION = AotMethods.class.getMethod("throwUnknownFunction", int.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private AotMethodRefs() {}
}
