package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.AotInterpreterMachine;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Element;
import java.lang.reflect.Method;

public final class MethodRefs {

    static final Method CHECK_INTERRUPTION;
    static final Method CALL_INDIRECT;
    static final Method CALL_INDIRECT_ON_INTERPRETER;
    static final Method INSTANCE_MEMORY;
    static final Method CALL_HOST_FUNCTION;
    static final Method READ_GLOBAL;
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
    static final Method TABLE_REQUIRED_REF;
    static final Method TABLE_INSTANCE;
    static final Method THROW_CALL_STACK_EXHAUSTED;
    static final Method THROW_INDIRECT_CALL_TYPE_MISMATCH;
    static final Method THROW_OUT_OF_BOUNDS_MEMORY_ACCESS;
    static final Method THROW_TRAP_EXCEPTION;
    static final Method THROW_UNKNOWN_FUNCTION;
    static final Method AOT_INTERPRETER_MACHINE_CALL;

    static {
        try {
            CHECK_INTERRUPTION = Methods.class.getMethod("checkInterruption");
            CALL_INDIRECT =
                    Methods.class.getMethod(
                            "callIndirect", long[].class, int.class, int.class, Instance.class);
            CALL_INDIRECT_ON_INTERPRETER =
                    Methods.class.getMethod(
                            "callIndirect", long[].class, int.class, Instance.class);
            INSTANCE_MEMORY = Instance.class.getMethod("memory");
            CALL_HOST_FUNCTION =
                    Methods.class.getMethod(
                            "callHostFunction", Instance.class, int.class, long[].class);
            READ_GLOBAL = Methods.class.getMethod("readGlobal", int.class, Instance.class);
            WRITE_GLOBAL =
                    Methods.class.getMethod("writeGlobal", long.class, int.class, Instance.class);
            INSTANCE_SET_ELEMENT = Instance.class.getMethod("setElement", int.class, Element.class);
            INSTANCE_TABLE = Instance.class.getMethod("table", int.class);
            MEMORY_COPY =
                    Methods.class.getMethod(
                            "memoryCopy", int.class, int.class, int.class, Memory.class);
            MEMORY_FILL =
                    Methods.class.getMethod(
                            "memoryFill", int.class, byte.class, int.class, Memory.class);
            MEMORY_INIT =
                    Methods.class.getMethod(
                            "memoryInit", int.class, int.class, int.class, int.class, Memory.class);
            MEMORY_GROW = Methods.class.getMethod("memoryGrow", int.class, Memory.class);
            MEMORY_DROP = Methods.class.getMethod("memoryDrop", int.class, Memory.class);
            MEMORY_PAGES = Methods.class.getMethod("memoryPages", Memory.class);
            MEMORY_READ_BYTE =
                    Methods.class.getMethod("memoryReadByte", int.class, int.class, Memory.class);
            MEMORY_READ_SHORT =
                    Methods.class.getMethod("memoryReadShort", int.class, int.class, Memory.class);
            MEMORY_READ_INT =
                    Methods.class.getMethod("memoryReadInt", int.class, int.class, Memory.class);
            MEMORY_READ_LONG =
                    Methods.class.getMethod("memoryReadLong", int.class, int.class, Memory.class);
            MEMORY_READ_FLOAT =
                    Methods.class.getMethod("memoryReadFloat", int.class, int.class, Memory.class);
            MEMORY_READ_DOUBLE =
                    Methods.class.getMethod("memoryReadDouble", int.class, int.class, Memory.class);
            MEMORY_WRITE_BYTE =
                    Methods.class.getMethod(
                            "memoryWriteByte", int.class, byte.class, int.class, Memory.class);
            MEMORY_WRITE_SHORT =
                    Methods.class.getMethod(
                            "memoryWriteShort", int.class, short.class, int.class, Memory.class);
            MEMORY_WRITE_INT =
                    Methods.class.getMethod(
                            "memoryWriteInt", int.class, int.class, int.class, Memory.class);
            MEMORY_WRITE_LONG =
                    Methods.class.getMethod(
                            "memoryWriteLong", int.class, long.class, int.class, Memory.class);
            MEMORY_WRITE_FLOAT =
                    Methods.class.getMethod(
                            "memoryWriteFloat", int.class, float.class, int.class, Memory.class);
            MEMORY_WRITE_DOUBLE =
                    Methods.class.getMethod(
                            "memoryWriteDouble", int.class, double.class, int.class, Memory.class);
            REF_IS_NULL = Methods.class.getMethod("isRefNull", int.class);
            TABLE_GET = Methods.class.getMethod("tableGet", int.class, int.class, Instance.class);
            TABLE_SET =
                    Methods.class.getMethod(
                            "tableSet", int.class, int.class, int.class, Instance.class);
            TABLE_SIZE = Methods.class.getMethod("tableSize", int.class, Instance.class);
            TABLE_GROW =
                    Methods.class.getMethod(
                            "tableGrow", int.class, int.class, int.class, Instance.class);
            TABLE_FILL =
                    Methods.class.getMethod(
                            "tableFill",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_COPY =
                    Methods.class.getMethod(
                            "tableCopy",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_INIT =
                    Methods.class.getMethod(
                            "tableInit",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_REQUIRED_REF = TableInstance.class.getMethod("requiredRef", int.class);
            TABLE_INSTANCE = TableInstance.class.getMethod("instance", int.class);
            THROW_CALL_STACK_EXHAUSTED =
                    Methods.class.getMethod("throwCallStackExhausted", StackOverflowError.class);
            THROW_INDIRECT_CALL_TYPE_MISMATCH =
                    Methods.class.getMethod("throwIndirectCallTypeMismatch");
            THROW_OUT_OF_BOUNDS_MEMORY_ACCESS =
                    Methods.class.getMethod("throwOutOfBoundsMemoryAccess");
            THROW_TRAP_EXCEPTION = Methods.class.getMethod("throwTrapException");
            THROW_UNKNOWN_FUNCTION = Methods.class.getMethod("throwUnknownFunction", int.class);

            AOT_INTERPRETER_MACHINE_CALL =
                    AotInterpreterMachine.class.getMethod("call", int.class, long[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private MethodRefs() {}
}
