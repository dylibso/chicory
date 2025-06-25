package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.runtime.MemCopyWorkaround.shouldUseMemWorkaround;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.ChicoryInterruptedException;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.MemCopyWorkaround;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.Stratum;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.WasmException;
import com.dylibso.chicory.runtime.WasmRuntimeException;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        ChicoryException error = new ChicoryException("call stack exhausted", e);
        enhanceStackTrace(error);
        throw error;
    }

    public static RuntimeException throwIndirectCallTypeMismatch() {
        ChicoryException error = new ChicoryException("indirect call type mismatch");
        enhanceStackTrace(error);
        return error;
    }

    public static RuntimeException throwOutOfBoundsMemoryAccess() {
        WasmRuntimeException error = new WasmRuntimeException("out of bounds memory access");
        enhanceStackTrace(error);
        throw error;
    }

    public static RuntimeException throwTrapException() {
        TrapException error = new TrapException("Trapped on unreachable instruction");
        enhanceStackTrace(error);
        throw error;
    }

    public static RuntimeException throwUnknownFunction(int index) {
        InvalidException error = new InvalidException(String.format("unknown function %d", index));
        enhanceStackTrace(error);
        throw error;
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

    private static Stratum[] STRATA_BY_FUNC_GROUP;
    private static String funcGroupClassPrefix;

    public static void init(Class<?>[] classes, String funcGroupClassPrefix) {
        Shaded.funcGroupClassPrefix = funcGroupClassPrefix;
        STRATA_BY_FUNC_GROUP = new Stratum[classes.length];

        for (int i = 0; i < classes.length; i++) {
            var clazz = classes[i];
            String resource = "/" + clazz.getName().replace('.', '/') + ".smap";
            try (var is = clazz.getResourceAsStream(resource)) {
                if (is == null) {
                    throw new ChicoryException(
                            "Class " + clazz.getName() + " does not have a .smap resource");
                }
                var smap = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                STRATA_BY_FUNC_GROUP[i] = Stratum.parseSMapString(smap);
            } catch (IOException e) {
                throw new ChicoryException(
                        "Class " + clazz.getName() + " does not have a static initStrata method");
            }
        }
    }

    private static void enhanceStackTrace(Throwable e) {
        if (STRATA_BY_FUNC_GROUP == null) {
            return;
        }

        var elements = e.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            var element = elements[i];
            if (element.getClassName().startsWith(funcGroupClassPrefix)) {

                String suffix = element.getClassName().substring(funcGroupClassPrefix.length());
                var group = Integer.parseInt(suffix);
                if (group < STRATA_BY_FUNC_GROUP.length) {
                    var stratum = STRATA_BY_FUNC_GROUP[group];
                    var lineMapping = stratum.getInputLine(element.getLineNumber());
                    if (lineMapping != null) {

                        var path = lineMapping.filePath();
                        String functionName = stratum.getFunctionMapping(element.getLineNumber());
                        if (functionName == null) {
                            functionName = element.getMethodName();
                        }
                        elements[i] =
                                new StackTraceElement(
                                        element.getClassName(),
                                        functionName,
                                        path,
                                        (int) lineMapping.line());
                    }
                }
            }
        }
        e.setStackTrace(elements);
    }
}
