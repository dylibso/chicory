package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotMethods.CHECK_INTERRUPTION;
import static com.dylibso.chicory.aot.AotMethods.INSTANCE_CALL_HOST_FUNCTION;
import static com.dylibso.chicory.aot.AotMethods.THROW_TRAP_EXCEPTION;
import static com.dylibso.chicory.aot.AotUtil.boxer;
import static com.dylibso.chicory.aot.AotUtil.boxerHandle;
import static com.dylibso.chicory.aot.AotUtil.callIndirectMethodName;
import static com.dylibso.chicory.aot.AotUtil.callIndirectMethodType;
import static com.dylibso.chicory.aot.AotUtil.defaultValue;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeStatic;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeVirtual;
import static com.dylibso.chicory.aot.AotUtil.emitPop;
import static com.dylibso.chicory.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.aot.AotUtil.jvmTypes;
import static com.dylibso.chicory.aot.AotUtil.loadTypeOpcode;
import static com.dylibso.chicory.aot.AotUtil.localType;
import static com.dylibso.chicory.aot.AotUtil.methodNameFor;
import static com.dylibso.chicory.aot.AotUtil.methodTypeFor;
import static com.dylibso.chicory.aot.AotUtil.slotCount;
import static com.dylibso.chicory.aot.AotUtil.storeTypeOpcode;
import static com.dylibso.chicory.aot.AotUtil.unboxer;
import static com.dylibso.chicory.aot.AotUtil.unboxerHandle;
import static com.dylibso.chicory.wasm.types.Instruction.EMPTY_OPERANDS;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Simple Machine implementation that AOT compiles function bodies and runtime-links them
 * via MethodHandle. All compilation is done in a single compile phase during instantiation.
 */
public final class AotMachine implements Machine {

    public static final String DEFAULT_CLASS_NAME = "com.dylibso.chicory.$gen.CompiledModule";

    private static final Instruction FUNCTION_SCOPE =
            new Instruction(-1, OpCode.NOP, EMPTY_OPERANDS);

    private final Module module;
    private final Instance instance;
    private final MethodHandle[] compiledFunctions;
    private final byte[] compiledClass;
    private final List<ValueType> globalTypes;
    private final int functionImports;
    private final List<FunctionType> functionTypes;

    private static final Map<OpCode, BytecodeEmitter> emitters =
            AotEmitters.builder()
                    // ====== Misc ======
                    .intrinsic(OpCode.DROP, AotEmitters::DROP)
                    .intrinsic(OpCode.ELEM_DROP, AotEmitters::ELEM_DROP)
                    .intrinsic(OpCode.SELECT, AotEmitters::SELECT)
                    .intrinsic(OpCode.SELECT_T, AotEmitters::SELECT)

                    // ====== Control Flow ======
                    .intrinsic(OpCode.CALL, AotEmitters::CALL)
                    .intrinsic(OpCode.CALL_INDIRECT, AotEmitters::CALL_INDIRECT)

                    // ====== References ======
                    .intrinsic(OpCode.REF_FUNC, AotEmitters::REF_FUNC)
                    .intrinsic(OpCode.REF_NULL, AotEmitters::REF_NULL)
                    .intrinsic(OpCode.REF_IS_NULL, AotEmitters::REF_IS_NULL)

                    // ====== Locals & Globals ======
                    .intrinsic(OpCode.LOCAL_GET, AotEmitters::LOCAL_GET)
                    .intrinsic(OpCode.LOCAL_SET, AotEmitters::LOCAL_SET)
                    .intrinsic(OpCode.LOCAL_TEE, AotEmitters::LOCAL_TEE)
                    .intrinsic(OpCode.GLOBAL_GET, AotEmitters::GLOBAL_GET)
                    .intrinsic(OpCode.GLOBAL_SET, AotEmitters::GLOBAL_SET)

                    // ====== Tables ======
                    .intrinsic(OpCode.TABLE_GET, AotEmitters::TABLE_GET)
                    .intrinsic(OpCode.TABLE_SET, AotEmitters::TABLE_SET)
                    .intrinsic(OpCode.TABLE_SIZE, AotEmitters::TABLE_SIZE)
                    .intrinsic(OpCode.TABLE_GROW, AotEmitters::TABLE_GROW)
                    .intrinsic(OpCode.TABLE_FILL, AotEmitters::TABLE_FILL)
                    .intrinsic(OpCode.TABLE_COPY, AotEmitters::TABLE_COPY)
                    .intrinsic(OpCode.TABLE_INIT, AotEmitters::TABLE_INIT)

                    // ====== Memory ======
                    .intrinsic(OpCode.MEMORY_INIT, AotEmitters::MEMORY_INIT)
                    .intrinsic(OpCode.MEMORY_COPY, AotEmitters::MEMORY_COPY)
                    .intrinsic(OpCode.MEMORY_FILL, AotEmitters::MEMORY_FILL)
                    .intrinsic(OpCode.MEMORY_GROW, AotEmitters::MEMORY_GROW)
                    .intrinsic(OpCode.MEMORY_SIZE, AotEmitters::MEMORY_SIZE)
                    .intrinsic(OpCode.DATA_DROP, AotEmitters::DATA_DROP)

                    // ====== Load & Store ======
                    .intrinsic(OpCode.I32_LOAD, AotEmitters::I32_LOAD)
                    .intrinsic(OpCode.I32_LOAD8_S, AotEmitters::I32_LOAD8_S)
                    .intrinsic(OpCode.I32_LOAD8_U, AotEmitters::I32_LOAD8_U)
                    .intrinsic(OpCode.I32_LOAD16_S, AotEmitters::I32_LOAD16_S)
                    .intrinsic(OpCode.I32_LOAD16_U, AotEmitters::I32_LOAD16_U)
                    .intrinsic(OpCode.I64_LOAD, AotEmitters::I64_LOAD)
                    .intrinsic(OpCode.I64_LOAD8_S, AotEmitters::I64_LOAD8_S)
                    .intrinsic(OpCode.I64_LOAD8_U, AotEmitters::I64_LOAD8_U)
                    .intrinsic(OpCode.I64_LOAD16_S, AotEmitters::I64_LOAD16_S)
                    .intrinsic(OpCode.I64_LOAD16_U, AotEmitters::I64_LOAD16_U)
                    .intrinsic(OpCode.I64_LOAD32_S, AotEmitters::I64_LOAD32_S)
                    .intrinsic(OpCode.I64_LOAD32_U, AotEmitters::I64_LOAD32_U)
                    .intrinsic(OpCode.F32_LOAD, AotEmitters::F32_LOAD)
                    .intrinsic(OpCode.F64_LOAD, AotEmitters::F64_LOAD)
                    .intrinsic(OpCode.I32_STORE, AotEmitters::I32_STORE)
                    .intrinsic(OpCode.I32_STORE8, AotEmitters::I32_STORE8)
                    .intrinsic(OpCode.I32_STORE16, AotEmitters::I32_STORE16)
                    .intrinsic(OpCode.I64_STORE, AotEmitters::I64_STORE)
                    .intrinsic(OpCode.I64_STORE8, AotEmitters::I64_STORE8)
                    .intrinsic(OpCode.I64_STORE16, AotEmitters::I64_STORE16)
                    .intrinsic(OpCode.I64_STORE32, AotEmitters::I64_STORE32)
                    .intrinsic(OpCode.F32_STORE, AotEmitters::F32_STORE)
                    .intrinsic(OpCode.F64_STORE, AotEmitters::F64_STORE)

                    // ====== I32 ======
                    .intrinsic(OpCode.I32_ADD, AotEmitters::I32_ADD)
                    .intrinsic(OpCode.I32_AND, AotEmitters::I32_AND)
                    .shared(OpCode.I32_CLZ, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_CONST, AotEmitters::I32_CONST)
                    .shared(OpCode.I32_CTZ, OpcodeImpl.class)
                    .shared(OpCode.I32_DIV_S, OpcodeImpl.class)
                    .shared(OpCode.I32_DIV_U, OpcodeImpl.class)
                    .shared(OpCode.I32_EQ, OpcodeImpl.class)
                    .shared(OpCode.I32_EQZ, OpcodeImpl.class)
                    .shared(OpCode.I32_EXTEND_8_S, OpcodeImpl.class)
                    .shared(OpCode.I32_EXTEND_16_S, OpcodeImpl.class)
                    .shared(OpCode.I32_GE_S, OpcodeImpl.class)
                    .shared(OpCode.I32_GE_U, OpcodeImpl.class)
                    .shared(OpCode.I32_GT_S, OpcodeImpl.class)
                    .shared(OpCode.I32_GT_U, OpcodeImpl.class)
                    .shared(OpCode.I32_LE_S, OpcodeImpl.class)
                    .shared(OpCode.I32_LE_U, OpcodeImpl.class)
                    .shared(OpCode.I32_LT_S, OpcodeImpl.class)
                    .shared(OpCode.I32_LT_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_MUL, AotEmitters::I32_MUL)
                    .shared(OpCode.I32_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_OR, AotEmitters::I32_OR)
                    .shared(OpCode.I32_POPCNT, OpcodeImpl.class)
                    .shared(OpCode.I32_REINTERPRET_F32, OpcodeImpl.class)
                    .shared(OpCode.I32_REM_S, OpcodeImpl.class)
                    .shared(OpCode.I32_REM_U, OpcodeImpl.class)
                    .shared(OpCode.I32_ROTL, OpcodeImpl.class)
                    .shared(OpCode.I32_ROTR, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_SHL, AotEmitters::I32_SHL)
                    .intrinsic(OpCode.I32_SHR_S, AotEmitters::I32_SHR_S)
                    .intrinsic(OpCode.I32_SHR_U, AotEmitters::I32_SHR_U)
                    .intrinsic(OpCode.I32_SUB, AotEmitters::I32_SUB)
                    .shared(OpCode.I32_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_WRAP_I64, AotEmitters::I32_WRAP_I64)
                    .intrinsic(OpCode.I32_XOR, AotEmitters::I32_XOR)

                    // ====== I64 ======
                    .intrinsic(OpCode.I64_ADD, AotEmitters::I64_ADD)
                    .intrinsic(OpCode.I64_AND, AotEmitters::I64_AND)
                    .shared(OpCode.I64_CLZ, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_CONST, AotEmitters::I64_CONST)
                    .shared(OpCode.I64_CTZ, OpcodeImpl.class)
                    .shared(OpCode.I64_DIV_S, OpcodeImpl.class)
                    .shared(OpCode.I64_DIV_U, OpcodeImpl.class)
                    .shared(OpCode.I64_EQ, OpcodeImpl.class)
                    .shared(OpCode.I64_EQZ, OpcodeImpl.class)
                    .shared(OpCode.I64_EXTEND_8_S, OpcodeImpl.class)
                    .shared(OpCode.I64_EXTEND_16_S, OpcodeImpl.class)
                    .shared(OpCode.I64_EXTEND_32_S, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_EXTEND_I32_S, AotEmitters::I64_EXTEND_I32_S)
                    .shared(OpCode.I64_EXTEND_I32_U, OpcodeImpl.class)
                    .shared(OpCode.I64_GE_S, OpcodeImpl.class)
                    .shared(OpCode.I64_GE_U, OpcodeImpl.class)
                    .shared(OpCode.I64_GT_S, OpcodeImpl.class)
                    .shared(OpCode.I64_GT_U, OpcodeImpl.class)
                    .shared(OpCode.I64_LE_S, OpcodeImpl.class)
                    .shared(OpCode.I64_LE_U, OpcodeImpl.class)
                    .shared(OpCode.I64_LT_S, OpcodeImpl.class)
                    .shared(OpCode.I64_LT_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_MUL, AotEmitters::I64_MUL)
                    .shared(OpCode.I64_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_OR, AotEmitters::I64_OR)
                    .shared(OpCode.I64_POPCNT, OpcodeImpl.class)
                    .shared(OpCode.I64_REM_S, OpcodeImpl.class)
                    .shared(OpCode.I64_REM_U, OpcodeImpl.class)
                    .shared(OpCode.I64_ROTL, OpcodeImpl.class)
                    .shared(OpCode.I64_ROTR, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_SHL, AotEmitters::I64_SHL)
                    .intrinsic(OpCode.I64_SHR_S, AotEmitters::I64_SHR_S)
                    .intrinsic(OpCode.I64_SHR_U, AotEmitters::I64_SHR_U)
                    .intrinsic(OpCode.I64_SUB, AotEmitters::I64_SUB)
                    .shared(OpCode.I64_REINTERPRET_F64, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_XOR, AotEmitters::I64_XOR)

                    // ====== F32 ======
                    .shared(OpCode.F32_ABS, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_ADD, AotEmitters::F32_ADD)
                    .shared(OpCode.F32_CEIL, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_CONST, AotEmitters::F32_CONST)
                    .shared(OpCode.F32_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(OpCode.F32_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(OpCode.F32_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(OpCode.F32_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(OpCode.F32_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_DEMOTE_F64, AotEmitters::F32_DEMOTE_F64)
                    .intrinsic(OpCode.F32_DIV, AotEmitters::F32_DIV)
                    .shared(OpCode.F32_EQ, OpcodeImpl.class)
                    .shared(OpCode.F32_FLOOR, OpcodeImpl.class)
                    .shared(OpCode.F32_GE, OpcodeImpl.class)
                    .shared(OpCode.F32_GT, OpcodeImpl.class)
                    .shared(OpCode.F32_LE, OpcodeImpl.class)
                    .shared(OpCode.F32_LT, OpcodeImpl.class)
                    .shared(OpCode.F32_MAX, OpcodeImpl.class)
                    .shared(OpCode.F32_MIN, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_MUL, AotEmitters::F32_MUL)
                    .shared(OpCode.F32_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_NEG, AotEmitters::F32_NEG)
                    .shared(OpCode.F32_NEAREST, OpcodeImpl.class)
                    .shared(OpCode.F32_REINTERPRET_I32, OpcodeImpl.class)
                    .shared(OpCode.F32_SQRT, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_SUB, AotEmitters::F32_SUB)
                    .shared(OpCode.F32_TRUNC, OpcodeImpl.class)

                    // ====== F64 ======
                    .shared(OpCode.F64_ABS, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_ADD, AotEmitters::F64_ADD)
                    .shared(OpCode.F64_CEIL, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_CONST, AotEmitters::F64_CONST)
                    .shared(OpCode.F64_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(OpCode.F64_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(OpCode.F64_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(OpCode.F64_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(OpCode.F64_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_DIV, AotEmitters::F64_DIV)
                    .shared(OpCode.F64_EQ, OpcodeImpl.class)
                    .shared(OpCode.F64_FLOOR, OpcodeImpl.class)
                    .shared(OpCode.F64_GE, OpcodeImpl.class)
                    .shared(OpCode.F64_GT, OpcodeImpl.class)
                    .shared(OpCode.F64_LE, OpcodeImpl.class)
                    .shared(OpCode.F64_LT, OpcodeImpl.class)
                    .shared(OpCode.F64_MAX, OpcodeImpl.class)
                    .shared(OpCode.F64_MIN, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_MUL, AotEmitters::F64_MUL)
                    .shared(OpCode.F64_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_NEG, AotEmitters::F64_NEG)
                    .shared(OpCode.F64_NEAREST, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_PROMOTE_F32, AotEmitters::F64_PROMOTE_F32)
                    .shared(OpCode.F64_REINTERPRET_I64, OpcodeImpl.class)
                    .shared(OpCode.F64_SQRT, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_SUB, AotEmitters::F64_SUB)
                    .shared(OpCode.F64_TRUNC, OpcodeImpl.class)
                    .build();

    public AotMachine(Instance instance) {
        this(DEFAULT_CLASS_NAME, instance);
    }

    public AotMachine(String className, Instance instance) {
        this.instance = requireNonNull(instance, "instance");
        this.module = instance.module();

        this.globalTypes = getGlobalTypes(module);

        this.functionImports = module.importSection().count(ExternalType.FUNCTION);
        this.functionTypes = getFunctionTypes(module);

        this.compiledClass = compileClass(className, this.module.functionSection());
        this.compiledFunctions = compile(loadClass(className, compiledClass));
    }

    private static List<ValueType> getGlobalTypes(Module module) {
        var importedGlobals =
                module.importSection().stream()
                        .filter(GlobalImport.class::isInstance)
                        .map(GlobalImport.class::cast)
                        .map(GlobalImport::type);

        var globals = Stream.of(module.globalSection().globals()).map(Global::valueType);

        return Stream.concat(importedGlobals, globals).collect(toUnmodifiableList());
    }

    private static List<FunctionType> getFunctionTypes(Module module) {
        var importedFunctions =
                module.importSection().stream()
                        .filter(FunctionImport.class::isInstance)
                        .map(FunctionImport.class::cast)
                        .map(function -> module.typeSection().types()[function.typeIndex()]);

        var functions = module.functionSection();
        var moduleFunctions =
                IntStream.range(0, functions.functionCount())
                        .mapToObj(i -> functions.getFunctionType(i, module.typeSection()));

        return Stream.concat(importedFunctions, moduleFunctions).collect(toUnmodifiableList());
    }

    @Override
    public long[] call(int funcId, long[] args) throws ChicoryException {
        try {
            return (long[]) compiledFunctions[funcId].invokeExact(args);
        } catch (ChicoryException e) {
            // propagate ChicoryExceptions
            throw e;
        } catch (StackOverflowError e) {
            throw new ChicoryException("call stack exhausted", e);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("undefined element " + e.getMessage(), e);
        } catch (Throwable e) {
            throw new WASMRuntimeException("An underlying Java exception occurred", e);
        }
    }

    private MethodHandle[] compile(Class<?> clazz) {
        var functions = module.functionSection();
        var compiled = new MethodHandle[functionImports + functions.functionCount()];

        for (int i = 0; i < functionImports; i++) {
            compiled[i] = HostFunctionInvoker.handleFor(instance, i);
        }

        for (int i = 0; i < functions.functionCount(); i++) {
            var type = functions.getFunctionType(i, module.typeSection());
            var funcId = functionImports + i;
            try {
                MethodHandle handle =
                        publicLookup()
                                .findStatic(clazz, methodNameFor(funcId), methodTypeFor(type));
                handle =
                        insertArguments(
                                handle,
                                handle.type().parameterCount() - 2,
                                instance.memory(),
                                instance);
                compiled[funcId] = adaptSignature(type, handle);

            } catch (ReflectiveOperationException e) {
                throw new ChicoryException(e);
            }
        }

        return compiled;
    }

    private byte[] compileClass(String className, FunctionSection functions) {
        var internalClassName = className.replace('.', '/');

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                internalClassName,
                null,
                getInternalName(Object.class),
                null);
        classWriter.visitSource("wasm", "wasm");

        emitConstructor(classWriter);

        for (int i = 0; i < functionImports; i++) {
            int funcId = i;
            var type = functionTypes.get(funcId);
            emitFunction(
                    classWriter,
                    methodNameFor(funcId),
                    methodTypeFor(type),
                    asm -> compileHostFunction(funcId, type, asm));
        }

        for (int i = 0; i < functions.functionCount(); i++) {
            var funcId = functionImports + i;
            var type = functionTypes.get(funcId);
            var body = module.codeSection().getFunctionBody(i);

            emitFunction(
                    classWriter,
                    methodNameFor(funcId),
                    methodTypeFor(type),
                    asm -> compileBody(internalClassName, funcId, type, body, asm));
        }

        var allTypes = module.typeSection().types();
        for (int i = 0; i < allTypes.length; i++) {
            var typeId = i;
            var type = allTypes[i];
            emitFunction(
                    classWriter,
                    callIndirectMethodName(typeId),
                    callIndirectMethodType(type),
                    asm -> compileCallIndirect(asm, typeId, type));
        }

        var returnTypes =
                functionTypes.stream()
                        .map(FunctionType::returns)
                        .filter(types -> types.size() > 1)
                        .collect(toSet());
        for (var types : returnTypes) {
            emitFunction(
                    classWriter,
                    valueMethodName(types),
                    valueMethodType(types),
                    asm -> {
                        emitBoxArguments(asm, types);
                        asm.visitInsn(Opcodes.ARETURN);
                    });
        }

        classWriter.visitEnd();

        try {
            return classWriter.toByteArray();
        } catch (MethodTooLargeException e) {
            throw new ChicoryException(
                    String.format(
                            "JVM bytecode too large for WASM method: %s size=%d",
                            e.getMethodName(), e.getCodeSize()),
                    e);
        }
    }

    private static void emitFunction(
            ClassVisitor classWriter,
            String methodName,
            MethodType methodType,
            Consumer<MethodVisitor> consumer) {

        var methodWriter =
                classWriter.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        methodName,
                        methodType.toMethodDescriptorString(),
                        null,
                        null);

        // optimize instruction size to avoid method size limits
        methodWriter = new InstructionAdapter(methodWriter);

        methodWriter.visitCode();
        consumer.accept(methodWriter);
        methodWriter.visitMaxs(0, 0);
        methodWriter.visitEnd();
    }

    private Class<?> loadClass(String className, byte[] classBytes) {
        try {
            Class<?> clazz =
                    new ByteArrayClassLoader(getClass().getClassLoader())
                            .loadFromBytes(className, classBytes);
            // force initialization to run JVM verifier
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (VerifyError e) {
            // run ASM verifier to help with debugging
            try {
                ClassReader reader = new ClassReader(classBytes);
                CheckClassAdapter.verify(reader, true, new PrintWriter(System.out, false, UTF_8));
            } catch (NoClassDefFoundError ignored) {
                // the ASM verifier is an optional dependency
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
    }

    private static MethodHandle adaptSignature(FunctionType type, MethodHandle handle) {
        var argTypes = type.params();
        var argHandlers = new MethodHandle[type.params().size()];
        for (int i = 0; i < argHandlers.length; i++) {
            argHandlers[i] = unboxerHandle(argTypes.get(i));
        }
        MethodHandle result = filterArguments(handle, 0, argHandlers);
        result = result.asSpreader(Value[].class, argTypes.size());

        if (type.returns().isEmpty()) {
            return result.asType(result.type().changeReturnType(Value[].class));
        }
        if (type.returns().size() > 1) {
            return result;
        }
        result = filterReturnValue(result, boxerHandle(type.returns().get(0)));
        return filterReturnValue(result, ValueWrapper.HANDLE);
    }

    private static void emitConstructor(ClassVisitor writer) {
        var cons =
                writer.visitMethod(
                        Opcodes.ACC_PRIVATE, "<init>", getMethodDescriptor(VOID_TYPE), null, null);
        cons.visitCode();

        // super();
        cons.visitVarInsn(Opcodes.ALOAD, 0);
        cons.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                getInternalName(Object.class),
                "<init>",
                getMethodDescriptor(VOID_TYPE),
                false);

        cons.visitInsn(Opcodes.RETURN);
        cons.visitMaxs(0, 0);
        cons.visitEnd();
    }

    private static void compileCallIndirect(MethodVisitor asm, int typeId, FunctionType type) {
        int slot = type.params().stream().mapToInt(AotUtil::slotCount).sum();

        // parameters: arguments, funcTableIdx, tableIdx, instance
        // emitBoxArguments(asm, type.params());
        // asm.visitLdcInsn(LONG);
        for (int i = 0; i < type.params().size(); i++) {
            asm.visitVarInsn(Opcodes.LLOAD, i);
        }
        asm.visitLdcInsn(typeId);
        asm.visitVarInsn(Opcodes.ILOAD, slot); // funcTableIdx
        asm.visitVarInsn(Opcodes.ILOAD, slot + 1); // tableIdx
        asm.visitVarInsn(Opcodes.ALOAD, slot + 2); // instance

        emitInvokeStatic(asm, AotMethods.CALL_INDIRECT);

        asm.visitInsn(returnTypeOpcode(type));
    }

    private static void compileHostFunction(int funcId, FunctionType type, MethodVisitor asm) {
        int slot = type.params().stream().mapToInt(AotUtil::slotCount).sum();

        asm.visitVarInsn(Opcodes.ALOAD, slot + 1); // instance
        asm.visitLdcInsn(funcId);
        emitBoxArguments(asm, type.params());

        emitInvokeVirtual(asm, INSTANCE_CALL_HOST_FUNCTION);

        emitUnboxResult(type, asm);
    }

    private static void emitBoxArguments(MethodVisitor asm, List<ValueType> types) {
        int slot = 0;
        // box the arguments into Value[]
        asm.visitLdcInsn(types.size());
        asm.visitTypeInsn(Opcodes.ANEWARRAY, getInternalName(Value.class));
        for (int i = 0; i < types.size(); i++) {
            asm.visitInsn(Opcodes.DUP);
            asm.visitLdcInsn(i);
            ValueType valueType = types.get(i);
            asm.visitVarInsn(loadTypeOpcode(valueType), slot);
            emitInvokeStatic(asm, boxer(valueType));
            asm.visitInsn(Opcodes.AASTORE);
            slot += slotCount(valueType);
        }
    }

    private static void emitUnboxResult(FunctionType type, MethodVisitor asm) {
        Class<?> returnType = jvmReturnType(type);
        if (returnType == void.class) {
            asm.visitInsn(Opcodes.RETURN);
        } else if (returnType == Value[].class) {
            asm.visitInsn(Opcodes.ARETURN);
        } else {
            // unbox the result from Value[0]
            asm.visitLdcInsn(0);
            asm.visitInsn(Opcodes.AALOAD);
            emitInvokeVirtual(asm, unboxer(type.returns().get(0)));
            asm.visitInsn(returnTypeOpcode(type));
        }
    }

    private void compileBody(
            String internalClassName,
            int funcId,
            FunctionType type,
            FunctionBody body,
            MethodVisitor asm) {

        var ctx =
                new AotContext(
                        internalClassName,
                        globalTypes,
                        functionTypes,
                        module.typeSection().types(),
                        funcId,
                        type,
                        body);

        // initialize local variables to their default values
        int localsCount = type.params().size() + body.localTypes().size();
        for (int i = type.params().size(); i < localsCount; i++) {
            var localType = localType(type, body, i);
            asm.visitLdcInsn(defaultValue(localType));
            asm.visitVarInsn(storeTypeOpcode(localType), ctx.localSlotIndex(i));
        }

        // allocate labels for all label targets
        Map<Integer, Label> labels = new HashMap<>();
        for (AnnotatedInstruction ins : body.instructions()) {
            if (ins.labelTrue() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.put(ins.labelTrue(), new Label());
            }
            if (ins.labelFalse() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.put(ins.labelFalse(), new Label());
            }
            for (int label : ins.labelTable()) {
                labels.put(label, new Label());
            }
        }

        // fake instruction to use for the function's implicit block
        ctx.enterScope(FUNCTION_SCOPE, FunctionType.of(List.of(), type.returns()));

        // compile the function body
        int exitBlockDepth = -1;
        for (int idx = 0; idx < body.instructions().size(); idx++) {
            var ins = body.instructions().get(idx);

            Label label = labels.get(idx);
            if (label != null) {
                asm.visitLabel(label);
            }

            // skip instructions after unconditional control transfer
            if (exitBlockDepth >= 0) {
                if (ins.depth() > exitBlockDepth) {
                    continue;
                }
                if (ins.opcode() != OpCode.ELSE && ins.opcode() != OpCode.END) {
                    continue;
                }

                exitBlockDepth = -1;
                if (ins.opcode() == OpCode.END) {
                    ctx.scopeRestoreStackSize();
                }
            }

            switch (ins.opcode()) {
                case NOP:
                    break;
                case BLOCK:
                case LOOP:
                    ctx.enterScope(ins.scope(), blockType(ins));
                    break;
                case END:
                    ctx.exitScope(ins.scope());
                    break;
                case UNREACHABLE:
                    exitBlockDepth = ins.depth();
                    emitInvokeStatic(asm, THROW_TRAP_EXCEPTION);
                    asm.visitInsn(Opcodes.ATHROW);
                    break;
                case RETURN:
                    exitBlockDepth = ins.depth();
                    emitReturn(asm, type, ctx);
                    break;
                case IF:
                    ctx.popStackSize();
                    ctx.enterScope(ins.scope(), blockType(ins));
                    asm.visitJumpInsn(Opcodes.IFEQ, labels.get(ins.labelFalse()));
                    // use the same starting stack sizes for both sides of the branch
                    if (body.instructions().get(ins.labelFalse() - 1).opcode() == OpCode.ELSE) {
                        ctx.pushStackSizesStack();
                    }
                    break;
                case ELSE:
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTrue()));
                    ctx.popStackSizesStack();
                    break;
                case BR:
                    exitBlockDepth = ins.depth();
                    if (ins.labelTrue() < idx) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    emitUnwindStack(asm, type, body, ins, ins.labelTrue(), ctx);
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTrue()));
                    break;
                case BR_IF:
                    ctx.popStackSize();
                    Label falseLabel = new Label();
                    asm.visitJumpInsn(Opcodes.IFEQ, falseLabel);
                    if (ins.labelTrue() < idx) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    emitUnwindStack(asm, type, body, ins, ins.labelTrue(), ctx);
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTrue()));
                    asm.visitLabel(falseLabel);
                    break;
                case BR_TABLE:
                    exitBlockDepth = ins.depth();
                    ctx.popStackSize();
                    emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    // skip table switch if it only has a default
                    if (ins.labelTable().size() == 1) {
                        asm.visitInsn(Opcodes.POP);
                        emitUnwindStack(asm, type, body, ins, ins.labelTable().get(0), ctx);
                        asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTable().get(0)));
                        break;
                    }
                    // collect unique target labels
                    Map<Integer, Label> targets = new HashMap<>();
                    Label[] table = new Label[ins.labelTable().size() - 1];
                    for (int i = 0; i < table.length; i++) {
                        table[i] =
                                targets.computeIfAbsent(ins.labelTable().get(i), x -> new Label());
                    }
                    // table switch using the last entry of the label table as the default
                    int defaultTarget = ins.labelTable().get(ins.labelTable().size() - 1);
                    Label defaultLabel = targets.computeIfAbsent(defaultTarget, x -> new Label());
                    asm.visitTableSwitchInsn(0, table.length - 1, defaultLabel, table);
                    // generate separate unwinds for each target
                    targets.forEach(
                            (target, tableLabel) -> {
                                asm.visitLabel(tableLabel);
                                emitUnwindStack(asm, type, body, ins, target, ctx);
                                asm.visitJumpInsn(Opcodes.GOTO, labels.get(target));
                            });
                    break;
                default:
                    var emitter = emitters.get(ins.opcode());
                    if (emitter == null) {
                        throw new ChicoryException(
                                "JVM compilation failed: opcode is not supported: " + ins.opcode());
                    }
                    emitter.emit(ctx, ins, asm);
            }
        }

        // implicit return at end of function
        emitReturn(asm, type, ctx);

        if (ctx.stackSizesStack().size() != 1) {
            throw new RuntimeException("Bad stack sizes stack: " + ctx.stackSizesStack());
        }
        if (!ctx.stackSizes().isEmpty()) {
            throw new RuntimeException("Stack sizes not empty: " + ctx.stackSizes());
        }
    }

    private static void emitReturn(MethodVisitor asm, FunctionType type, AotContext ctx) {
        if (type.returns().size() > 1) {
            asm.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    ctx.internalClassName(),
                    valueMethodName(type.returns()),
                    valueMethodType(type.returns()).toMethodDescriptorString(),
                    false);
        }
        asm.visitInsn(returnTypeOpcode(type));
        for (int i = 0; i < type.returns().size(); i++) {
            ctx.popStackSize();
        }
    }

    private void emitUnwindStack(
            MethodVisitor asm,
            FunctionType functionType,
            FunctionBody body,
            AnnotatedInstruction ins,
            int label,
            AotContext ctx) {

        boolean forward = true;
        var target = body.instructions().get(label);
        if (target.address() <= ins.address()) {
            // the loop block is the instruction before the target
            target = body.instructions().get(label - 1);
            forward = false;
        }
        var scope = target.scope();

        FunctionType blockType;
        if (scope.opcode() == OpCode.END) {
            // special scope for the function's implicit block
            scope = FUNCTION_SCOPE;
            blockType = functionType;
        } else {
            blockType = blockType(scope);
        }

        var types = forward ? blockType.returns() : blockType.params();

        // for a backward jump, the initial loop parameters are dropped
        var stackSizes = new ArrayDeque<>(ctx.stackSizes());
        int dropCount = stackSizes.size() - ctx.scopeStackSize(scope);

        // do not drop the return values for a forward jump
        if (forward) {
            dropCount -= types.size();
        }

        if (dropCount == 0) {
            return;
        }

        // save result values
        int slot = ctx.tempSlot();
        for (int i = types.size() - 1; i >= 0; i--) {
            ValueType type = types.get(i);
            asm.visitVarInsn(storeTypeOpcode(type), slot);
            slot += slotCount(type);
            stackSizes.pop();
        }

        // drop intervening values
        for (int i = 0; i < dropCount; i++) {
            emitPop(asm, stackSizes.pop());
        }

        // restore result values
        for (ValueType type : types) {
            slot -= slotCount(type);
            asm.visitVarInsn(loadTypeOpcode(type), slot);
        }
    }

    private FunctionType blockType(Instruction ins) {
        var typeId = (int) ins.operand(0);
        if (typeId == 0x40) {
            return FunctionType.empty();
        }
        if (ValueType.isValid(typeId)) {
            return FunctionType.returning(ValueType.forId(typeId));
        }
        return module.typeSection().types()[typeId];
    }

    private static MethodType valueMethodType(List<ValueType> types) {
        return methodType(Value[].class, jvmTypes(types));
    }

    private static String valueMethodName(List<ValueType> types) {
        return "value_"
                + types.stream()
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .collect(joining("_"));
    }

    private static int returnTypeOpcode(FunctionType type) {
        Class<?> returnType = jvmReturnType(type);
        if (returnType == Value[].class) {
            return Opcodes.ARETURN;
        }
        if (returnType == int.class) {
            return Opcodes.IRETURN;
        }
        if (returnType == long.class) {
            return Opcodes.LRETURN;
        }
        if (returnType == float.class) {
            return Opcodes.FRETURN;
        }
        if (returnType == double.class) {
            return Opcodes.DRETURN;
        }
        if (returnType == void.class) {
            return Opcodes.RETURN;
        }
        throw new ChicoryException("Unsupported return type: " + returnType.getName());
    }

    public byte[] compiledClass() {
        return compiledClass;
    }
}
