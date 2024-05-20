package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotUtil.boxer;
import static com.dylibso.chicory.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.aot.AotUtil.unboxer;
import static com.dylibso.chicory.wasm.types.OpCode.*;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;

import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Simple Machine implementation that AOT compiles function bodies and runtime-links them
 * via MethodHandle. All compilation is done in a single compile phase during instantiation.
 */
public class AotMachine implements Machine {

    protected final Module module;
    protected final MethodHandle[] compiledFunctions;

    protected static final Map<OpCode, BytecodeEmitter> emitters =
            AotEmitters.builder()
                    // ====== Locals & Globals ======
                    .intrinsic(LOCAL_GET, AotEmitters::LOCAL_GET)
                    .intrinsic(LOCAL_SET, AotEmitters::LOCAL_SET)

                    // ====== I32 ======
                    .intrinsic(I32_ADD, AotEmitters::I32_ADD)
                    .intrinsic(I32_AND, AotEmitters::I32_AND)
                    .shared(I32_CLZ, OpcodeImpl.class)
                    .shared(I32_CTZ, OpcodeImpl.class)
                    .shared(I32_DIV_S, OpcodeImpl.class)
                    .shared(I32_DIV_U, OpcodeImpl.class)
                    .shared(I32_EQ, OpcodeImpl.class)
                    .shared(I32_EQZ, OpcodeImpl.class)
                    .shared(I32_EXTEND_8_S, OpcodeImpl.class)
                    .shared(I32_EXTEND_16_S, OpcodeImpl.class)
                    .shared(I32_GE_S, OpcodeImpl.class)
                    .shared(I32_GE_U, OpcodeImpl.class)
                    .shared(I32_GT_S, OpcodeImpl.class)
                    .shared(I32_GT_U, OpcodeImpl.class)
                    .shared(I32_LE_S, OpcodeImpl.class)
                    .shared(I32_LE_U, OpcodeImpl.class)
                    .shared(I32_LT_S, OpcodeImpl.class)
                    .shared(I32_LT_U, OpcodeImpl.class)
                    .intrinsic(I32_MUL, AotEmitters::I32_MUL)
                    .shared(I32_NE, OpcodeImpl.class)
                    .intrinsic(I32_OR, AotEmitters::I32_OR)
                    .shared(I32_POPCNT, OpcodeImpl.class)
                    .intrinsic(I32_REM_S, AotEmitters::I32_REM_S)
                    .shared(I32_REM_U, OpcodeImpl.class)
                    .shared(I32_ROTL, OpcodeImpl.class)
                    .shared(I32_ROTR, OpcodeImpl.class)
                    .intrinsic(I32_SHL, AotEmitters::I32_SHL)
                    .intrinsic(I32_SHR_S, AotEmitters::I32_SHR_S)
                    .intrinsic(I32_SHR_U, AotEmitters::I32_SHR_U)
                    .intrinsic(I32_SUB, AotEmitters::I32_SUB)
                    .intrinsic(I32_XOR, AotEmitters::I32_XOR)

                    // ====== I64 ======
                    .intrinsic(I64_ADD, AotEmitters::I64_ADD)
                    .intrinsic(I64_AND, AotEmitters::I64_AND)
                    .shared(I64_CLZ, OpcodeImpl.class)
                    .shared(I64_CTZ, OpcodeImpl.class)
                    .shared(I64_DIV_S, OpcodeImpl.class)
                    .shared(I64_DIV_U, OpcodeImpl.class)
                    .shared(I64_EQ, OpcodeImpl.class)
                    .shared(I64_EQZ, OpcodeImpl.class)
                    .shared(I64_EXTEND_8_S, OpcodeImpl.class)
                    .shared(I64_EXTEND_16_S, OpcodeImpl.class)
                    .shared(I64_EXTEND_32_S, OpcodeImpl.class)
                    .shared(I64_GE_S, OpcodeImpl.class)
                    .shared(I64_GE_U, OpcodeImpl.class)
                    .shared(I64_GT_S, OpcodeImpl.class)
                    .shared(I64_GT_U, OpcodeImpl.class)
                    .shared(I64_LE_S, OpcodeImpl.class)
                    .shared(I64_LE_U, OpcodeImpl.class)
                    .shared(I64_LT_S, OpcodeImpl.class)
                    .shared(I64_LT_U, OpcodeImpl.class)
                    .intrinsic(I64_MUL, AotEmitters::I64_MUL)
                    .shared(I64_NE, OpcodeImpl.class)
                    .intrinsic(I64_OR, AotEmitters::I64_OR)
                    .shared(I64_POPCNT, OpcodeImpl.class)
                    .intrinsic(I64_REM_S, AotEmitters::I64_REM_S)
                    .shared(I64_REM_U, OpcodeImpl.class)
                    .shared(I64_ROTL, OpcodeImpl.class)
                    .shared(I64_ROTR, OpcodeImpl.class)
                    .intrinsic(I64_SHL, AotEmitters::I64_SHL)
                    .intrinsic(I64_SHR_S, AotEmitters::I64_SHR_S)
                    .intrinsic(I64_SHR_U, AotEmitters::I64_SHR_U)
                    .intrinsic(I64_SUB, AotEmitters::I64_SUB)
                    .intrinsic(I64_XOR, AotEmitters::I64_XOR)

                    // ====== F32 ======
                    .shared(F32_ABS, OpcodeImpl.class)
                    .intrinsic(F32_ADD, AotEmitters::F32_ADD)
                    .shared(F32_CEIL, OpcodeImpl.class)
                    .shared(F32_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(F32_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(F32_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(F32_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(F32_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(F32_DEMOTE_F64, AotEmitters::F32_DEMOTE_F64)
                    .intrinsic(F32_DIV, AotEmitters::F32_DIV)
                    .shared(F32_EQ, OpcodeImpl.class)
                    .shared(F32_FLOOR, OpcodeImpl.class)
                    .shared(F32_GE, OpcodeImpl.class)
                    .shared(F32_GT, OpcodeImpl.class)
                    .shared(F32_LE, OpcodeImpl.class)
                    .shared(F32_LT, OpcodeImpl.class)
                    .shared(F32_MAX, OpcodeImpl.class)
                    .shared(F32_MIN, OpcodeImpl.class)
                    .intrinsic(F32_MUL, AotEmitters::F32_MUL)
                    .shared(F32_NE, OpcodeImpl.class)
                    .intrinsic(F32_NEG, AotEmitters::F32_NEG)
                    .shared(F32_NEAREST, OpcodeImpl.class)
                    .shared(F32_SQRT, OpcodeImpl.class)
                    .intrinsic(F32_SUB, AotEmitters::F32_SUB)
                    .shared(F32_TRUNC, OpcodeImpl.class)

                    // ====== F64 ======
                    .shared(F64_CONVERT_I64_U, OpcodeImpl.class)
                    .build();

    public AotMachine(Module module) {
        this.module = module;
        compiledFunctions = new MethodHandle[module.wasmModule().functionSection().functionCount()];
        compile();
    }

    @Override
    public Value[] call(int funcId, Value[] args, boolean popResults) throws ChicoryException {
        try {
            Value result = (Value) compiledFunctions[funcId].invoke(args);
            return new Value[] {result};
        } catch (ChicoryException e) {
            // propagate ChicoryExceptions
            throw e;
        } catch (ArithmeticException e) {
            if (e.getMessage().equalsIgnoreCase("/ by zero")
                    || e.getMessage()
                            .contains("divide by zero")) { // On Linux i64 throws "BigInteger divide
                // by zero"
                throw new WASMRuntimeException("integer divide by zero: " + e.getMessage(), e);
            }
            throw new WASMRuntimeException(e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("undefined element " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WASMRuntimeException("An underlying Java exception occurred", e);
        } catch (Throwable e) {
            throw new WASMRuntimeException("An underlying Java error occurred", e);
        }
    }

    @Override
    public List<StackFrame> getStackTrace() {
        return List.of();
    }

    private void compile() {
        var functions = module.wasmModule().functionSection();
        for (int i = 0; i < functions.functionCount(); i++) {
            var type = functions.getFunctionType(i, module.wasmModule().typeSection());
            var body = module.wasmModule().codeSection().getFunctionBody(i);
            try {
                compiledFunctions[i] = compile(i, type, body);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ChicoryException(e);
            } catch (InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private MethodHandle compile(int funcId, FunctionType type, FunctionBody body)
            throws NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException,
                    InstantiationException {

        var functionName = nameFor(funcId);
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC,
                functionName,
                null,
                Type.getInternalName(Object.class),
                null);
        classWriter.visitSource("wasm", "wasm");

        makeDefaultConstructor(classWriter);

        var implWriter =
                classWriter.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "call",
                        Type.getMethodDescriptor(
                                Type.getType(jvmReturnType(type)),
                                Arrays.stream(AotUtil.jvmParameterTypes(type))
                                        .map(Type::getType)
                                        .toArray(Type[]::new)),
                        null,
                        null);

        implWriter.visitCode();

        compileBody(funcId, type, body, implWriter);

        implWriter.visitMaxs(0, 0);
        implWriter.visitEnd();

        classWriter.visitEnd();

        var handle = AotUtil.loadCallHandle(functionName, type, classWriter.toByteArray());
        return adaptSignature(type, handle);
    }

    private MethodHandle adaptSignature(FunctionType type, MethodHandle handle)
            throws NoSuchMethodException, IllegalAccessException {
        var argTypes = type.params();
        var argHandlers = new MethodHandle[type.params().size()];
        for (int i = 0; i < argHandlers.length; i++) {
            argHandlers[i] = unboxer(argTypes.get(i));
        }
        MethodHandle result = filterArguments(handle, 0, argHandlers);
        result = result.asSpreader(Value[].class, argTypes.size());
        return filterReturnValue(result, boxer(type.returns().get(0)));
    }

    private String nameFor(int funcId) {
        // TODO - use debug information (if available) to make a nice name
        return "fn$" + funcId;
    }

    private void makeDefaultConstructor(ClassWriter cls) {
        var cons =
                cls.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        Type.getMethodDescriptor(Type.VOID_TYPE),
                        null,
                        null);
        cons.visitCode();
        cons.visitVarInsn(Opcodes.ALOAD, 0);
        cons.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                Type.getType(Object.class).getInternalName(),
                "<init>",
                Type.getMethodType(Type.VOID_TYPE).getDescriptor(),
                false);
        cons.visitInsn(Opcodes.RETURN);
        cons.visitMaxs(0, 0);
        cons.visitEnd();
    }

    private boolean tryEmit(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var emitter = emitters.get(ins.opcode());
        if (emitter != null) {
            emitter.emit(ctx, ins, asm);
            return true;
        }
        return false;
    }

    private void compileBody(int funcId, FunctionType type, FunctionBody body, MethodVisitor asm) {

        var ctx = new AotContext(funcId, type, body);

        for (var ins : body.instructions()) {
            switch (ins.opcode()) {
                    // TODO - handle control flow & other "bookkeeping" opcodes here
                case END:
                    break;
                default:
                    if (!tryEmit(ctx, ins, asm)) {
                        throw new ChicoryException(
                                "JVM compilation failed: opcode "
                                        + ins.opcode().name()
                                        + " is not supported");
                    }
            }
        }

        asm.visitInsn(returnTypeOpcode(type));
    }

    private static int returnTypeOpcode(FunctionType type) {
        Class<?> returnType = jvmReturnType(type);
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
        throw new ChicoryException("Unsupported return type: " + returnType.getName());
    }
}
