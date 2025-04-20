package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.WasmException;
import com.dylibso.chicory.runtime.internal.CompilerInterpreterMachine;
import com.dylibso.chicory.wasm.types.Element;
import java.lang.reflect.Method;

public final class ShadedRefs {

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
    static final Method INIT;

    // Exception handling methods
    static final Method CREATE_WASM_EXCEPTION;
    static final Method INSTANCE_GET_EXCEPTION;
    static final Method EXCEPTION_MATCHES;

    static {
        try {
            CHECK_INTERRUPTION = Shaded.class.getMethod("checkInterruption");
            CALL_INDIRECT =
                    Shaded.class.getMethod(
                            "callIndirect", long[].class, int.class, int.class, Instance.class);
            CALL_INDIRECT_ON_INTERPRETER =
                    Shaded.class.getMethod("callIndirect", long[].class, int.class, Instance.class);
            INSTANCE_MEMORY = Instance.class.getMethod("memory");
            CALL_HOST_FUNCTION =
                    Shaded.class.getMethod(
                            "callHostFunction", Instance.class, int.class, long[].class);
            READ_GLOBAL = Shaded.class.getMethod("readGlobal", int.class, Instance.class);
            WRITE_GLOBAL =
                    Shaded.class.getMethod("writeGlobal", long.class, int.class, Instance.class);
            INSTANCE_SET_ELEMENT = Instance.class.getMethod("setElement", int.class, Element.class);
            INSTANCE_TABLE = Instance.class.getMethod("table", int.class);
            MEMORY_COPY =
                    Shaded.class.getMethod(
                            "memoryCopy", int.class, int.class, int.class, Memory.class);
            MEMORY_FILL =
                    Shaded.class.getMethod(
                            "memoryFill", int.class, byte.class, int.class, Memory.class);
            MEMORY_INIT =
                    Shaded.class.getMethod(
                            "memoryInit", int.class, int.class, int.class, int.class, Memory.class);
            MEMORY_GROW = Shaded.class.getMethod("memoryGrow", int.class, Memory.class);
            MEMORY_DROP = Shaded.class.getMethod("memoryDrop", int.class, Memory.class);
            MEMORY_PAGES = Shaded.class.getMethod("memoryPages", Memory.class);
            MEMORY_READ_BYTE =
                    Shaded.class.getMethod("memoryReadByte", int.class, int.class, Memory.class);
            MEMORY_READ_SHORT =
                    Shaded.class.getMethod("memoryReadShort", int.class, int.class, Memory.class);
            MEMORY_READ_INT =
                    Shaded.class.getMethod("memoryReadInt", int.class, int.class, Memory.class);
            MEMORY_READ_LONG =
                    Shaded.class.getMethod("memoryReadLong", int.class, int.class, Memory.class);
            MEMORY_READ_FLOAT =
                    Shaded.class.getMethod("memoryReadFloat", int.class, int.class, Memory.class);
            MEMORY_READ_DOUBLE =
                    Shaded.class.getMethod("memoryReadDouble", int.class, int.class, Memory.class);
            MEMORY_WRITE_BYTE =
                    Shaded.class.getMethod(
                            "memoryWriteByte", int.class, byte.class, int.class, Memory.class);
            MEMORY_WRITE_SHORT =
                    Shaded.class.getMethod(
                            "memoryWriteShort", int.class, short.class, int.class, Memory.class);
            MEMORY_WRITE_INT =
                    Shaded.class.getMethod(
                            "memoryWriteInt", int.class, int.class, int.class, Memory.class);
            MEMORY_WRITE_LONG =
                    Shaded.class.getMethod(
                            "memoryWriteLong", int.class, long.class, int.class, Memory.class);
            MEMORY_WRITE_FLOAT =
                    Shaded.class.getMethod(
                            "memoryWriteFloat", int.class, float.class, int.class, Memory.class);
            MEMORY_WRITE_DOUBLE =
                    Shaded.class.getMethod(
                            "memoryWriteDouble", int.class, double.class, int.class, Memory.class);
            REF_IS_NULL = Shaded.class.getMethod("isRefNull", int.class);
            TABLE_GET = Shaded.class.getMethod("tableGet", int.class, int.class, Instance.class);
            TABLE_SET =
                    Shaded.class.getMethod(
                            "tableSet", int.class, int.class, int.class, Instance.class);
            TABLE_SIZE = Shaded.class.getMethod("tableSize", int.class, Instance.class);
            TABLE_GROW =
                    Shaded.class.getMethod(
                            "tableGrow", int.class, int.class, int.class, Instance.class);
            TABLE_FILL =
                    Shaded.class.getMethod(
                            "tableFill",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_COPY =
                    Shaded.class.getMethod(
                            "tableCopy",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_INIT =
                    Shaded.class.getMethod(
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
                    Shaded.class.getMethod("throwCallStackExhausted", StackOverflowError.class);
            THROW_INDIRECT_CALL_TYPE_MISMATCH =
                    Shaded.class.getMethod("throwIndirectCallTypeMismatch");
            THROW_OUT_OF_BOUNDS_MEMORY_ACCESS =
                    Shaded.class.getMethod("throwOutOfBoundsMemoryAccess");
            THROW_TRAP_EXCEPTION = Shaded.class.getMethod("throwTrapException");
            THROW_UNKNOWN_FUNCTION = Shaded.class.getMethod("throwUnknownFunction", int.class);

            AOT_INTERPRETER_MACHINE_CALL =
                    CompilerInterpreterMachine.class.getMethod("call", int.class, long[].class);

            // Exception handling methods
            CREATE_WASM_EXCEPTION =
                    Shaded.class.getMethod(
                            "createWasmException", long[].class, int.class, Instance.class);
            INSTANCE_GET_EXCEPTION = Instance.class.getMethod("exn", int.class);
            EXCEPTION_MATCHES =
                    Shaded.class.getMethod(
                            "exceptionMatches", WasmException.class, int.class, Instance.class);

            INIT = Shaded.class.getMethod("init", String[].class, String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private ShadedRefs() {}
}
