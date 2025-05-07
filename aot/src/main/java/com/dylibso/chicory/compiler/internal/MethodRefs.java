package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.CompilerInterpreterMachine;
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
            CHECK_INTERRUPTION = GeneratedMethods.class.getMethod("checkInterruption");
            CALL_INDIRECT =
                    GeneratedMethods.class.getMethod(
                            "callIndirect", long[].class, int.class, int.class, Instance.class);
            CALL_INDIRECT_ON_INTERPRETER =
                    GeneratedMethods.class.getMethod(
                            "callIndirect", long[].class, int.class, Instance.class);
            INSTANCE_MEMORY = Instance.class.getMethod("memory");
            CALL_HOST_FUNCTION =
                    GeneratedMethods.class.getMethod(
                            "callHostFunction", Instance.class, int.class, long[].class);
            READ_GLOBAL = GeneratedMethods.class.getMethod("readGlobal", int.class, Instance.class);
            WRITE_GLOBAL =
                    GeneratedMethods.class.getMethod(
                            "writeGlobal", long.class, int.class, Instance.class);
            INSTANCE_SET_ELEMENT = Instance.class.getMethod("setElement", int.class, Element.class);
            INSTANCE_TABLE = Instance.class.getMethod("table", int.class);
            MEMORY_COPY =
                    GeneratedMethods.class.getMethod(
                            "memoryCopy", int.class, int.class, int.class, Memory.class);
            MEMORY_FILL =
                    GeneratedMethods.class.getMethod(
                            "memoryFill", int.class, byte.class, int.class, Memory.class);
            MEMORY_INIT =
                    GeneratedMethods.class.getMethod(
                            "memoryInit", int.class, int.class, int.class, int.class, Memory.class);
            MEMORY_GROW = GeneratedMethods.class.getMethod("memoryGrow", int.class, Memory.class);
            MEMORY_DROP = GeneratedMethods.class.getMethod("memoryDrop", int.class, Memory.class);
            MEMORY_PAGES = GeneratedMethods.class.getMethod("memoryPages", Memory.class);
            MEMORY_READ_BYTE =
                    GeneratedMethods.class.getMethod(
                            "memoryReadByte", int.class, int.class, Memory.class);
            MEMORY_READ_SHORT =
                    GeneratedMethods.class.getMethod(
                            "memoryReadShort", int.class, int.class, Memory.class);
            MEMORY_READ_INT =
                    GeneratedMethods.class.getMethod(
                            "memoryReadInt", int.class, int.class, Memory.class);
            MEMORY_READ_LONG =
                    GeneratedMethods.class.getMethod(
                            "memoryReadLong", int.class, int.class, Memory.class);
            MEMORY_READ_FLOAT =
                    GeneratedMethods.class.getMethod(
                            "memoryReadFloat", int.class, int.class, Memory.class);
            MEMORY_READ_DOUBLE =
                    GeneratedMethods.class.getMethod(
                            "memoryReadDouble", int.class, int.class, Memory.class);
            MEMORY_WRITE_BYTE =
                    GeneratedMethods.class.getMethod(
                            "memoryWriteByte", int.class, byte.class, int.class, Memory.class);
            MEMORY_WRITE_SHORT =
                    GeneratedMethods.class.getMethod(
                            "memoryWriteShort", int.class, short.class, int.class, Memory.class);
            MEMORY_WRITE_INT =
                    GeneratedMethods.class.getMethod(
                            "memoryWriteInt", int.class, int.class, int.class, Memory.class);
            MEMORY_WRITE_LONG =
                    GeneratedMethods.class.getMethod(
                            "memoryWriteLong", int.class, long.class, int.class, Memory.class);
            MEMORY_WRITE_FLOAT =
                    GeneratedMethods.class.getMethod(
                            "memoryWriteFloat", int.class, float.class, int.class, Memory.class);
            MEMORY_WRITE_DOUBLE =
                    GeneratedMethods.class.getMethod(
                            "memoryWriteDouble", int.class, double.class, int.class, Memory.class);
            REF_IS_NULL = GeneratedMethods.class.getMethod("isRefNull", int.class);
            TABLE_GET =
                    GeneratedMethods.class.getMethod(
                            "tableGet", int.class, int.class, Instance.class);
            TABLE_SET =
                    GeneratedMethods.class.getMethod(
                            "tableSet", int.class, int.class, int.class, Instance.class);
            TABLE_SIZE = GeneratedMethods.class.getMethod("tableSize", int.class, Instance.class);
            TABLE_GROW =
                    GeneratedMethods.class.getMethod(
                            "tableGrow", int.class, int.class, int.class, Instance.class);
            TABLE_FILL =
                    GeneratedMethods.class.getMethod(
                            "tableFill",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_COPY =
                    GeneratedMethods.class.getMethod(
                            "tableCopy",
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            Instance.class);
            TABLE_INIT =
                    GeneratedMethods.class.getMethod(
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
                    GeneratedMethods.class.getMethod(
                            "throwCallStackExhausted", StackOverflowError.class);
            THROW_INDIRECT_CALL_TYPE_MISMATCH =
                    GeneratedMethods.class.getMethod("throwIndirectCallTypeMismatch");
            THROW_OUT_OF_BOUNDS_MEMORY_ACCESS =
                    GeneratedMethods.class.getMethod("throwOutOfBoundsMemoryAccess");
            THROW_TRAP_EXCEPTION = GeneratedMethods.class.getMethod("throwTrapException");
            THROW_UNKNOWN_FUNCTION =
                    GeneratedMethods.class.getMethod("throwUnknownFunction", int.class);

            AOT_INTERPRETER_MACHINE_CALL =
                    CompilerInterpreterMachine.class.getMethod("call", int.class, long[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private MethodRefs() {}
}
