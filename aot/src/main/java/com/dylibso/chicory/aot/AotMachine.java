package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotEmitters.emitLocalStore;
import static com.dylibso.chicory.aot.AotEmitters.emitThrowTrapException;
import static com.dylibso.chicory.aot.AotUtil.boxer;
import static com.dylibso.chicory.aot.AotUtil.defaultValue;
import static com.dylibso.chicory.aot.AotUtil.jvmParameterTypes;
import static com.dylibso.chicory.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.aot.AotUtil.localType;
import static com.dylibso.chicory.aot.AotUtil.methodTypeFor;
import static com.dylibso.chicory.aot.AotUtil.unboxer;
import static com.dylibso.chicory.wasm.types.OpCode.*;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Simple Machine implementation that AOT compiles function bodies and runtime-links them
 * via MethodHandle. All compilation is done in a single compile phase during instantiation.
 */
public class AotMachine implements Machine {

    protected final Module module;
    protected final Instance instance;
    protected final MethodHandle[] compiledFunctions;

    protected static final Map<OpCode, BytecodeEmitter> emitters =
            AotEmitters.builder()
                    // ====== Misc ======
                    .intrinsic(DROP, AotEmitters::DROP)
                    .intrinsic(SELECT, AotEmitters::SELECT)

                    // ====== Locals & Globals ======
                    .intrinsic(LOCAL_GET, AotEmitters::LOCAL_GET)
                    .intrinsic(LOCAL_SET, AotEmitters::LOCAL_SET)

                    // ====== Load & Store ======
                    .intrinsic(I32_LOAD, AotEmitters::I32_LOAD)
                    .intrinsic(I32_LOAD8_S, AotEmitters::I32_LOAD8_S)
                    .intrinsic(I32_LOAD8_U, AotEmitters::I32_LOAD8_U)
                    .intrinsic(I32_LOAD16_S, AotEmitters::I32_LOAD16_S)
                    .intrinsic(I32_LOAD16_U, AotEmitters::I32_LOAD16_U)
                    .intrinsic(I64_LOAD, AotEmitters::I64_LOAD)
                    .intrinsic(I64_LOAD8_S, AotEmitters::I64_LOAD8_S)
                    .intrinsic(I64_LOAD8_U, AotEmitters::I64_LOAD8_U)
                    .intrinsic(I64_LOAD16_S, AotEmitters::I64_LOAD16_S)
                    .intrinsic(I64_LOAD16_U, AotEmitters::I64_LOAD16_U)
                    .intrinsic(I64_LOAD32_S, AotEmitters::I64_LOAD32_S)
                    .intrinsic(I64_LOAD32_U, AotEmitters::I64_LOAD32_U)
                    .intrinsic(F32_LOAD, AotEmitters::F32_LOAD)
                    .intrinsic(F64_LOAD, AotEmitters::F64_LOAD)
                    .intrinsic(I32_STORE, AotEmitters::I32_STORE)
                    .intrinsic(I32_STORE8, AotEmitters::I32_STORE8)
                    .intrinsic(I32_STORE16, AotEmitters::I32_STORE16)
                    .intrinsic(I64_STORE, AotEmitters::I64_STORE)
                    .intrinsic(I64_STORE8, AotEmitters::I64_STORE8)
                    .intrinsic(I64_STORE16, AotEmitters::I64_STORE16)
                    .intrinsic(I64_STORE32, AotEmitters::I64_STORE32)
                    .intrinsic(F32_STORE, AotEmitters::F32_STORE)
                    .intrinsic(F64_STORE, AotEmitters::F64_STORE)

                    // ====== I32 ======
                    .intrinsic(I32_ADD, AotEmitters::I32_ADD)
                    .intrinsic(I32_AND, AotEmitters::I32_AND)
                    .shared(I32_CLZ, OpcodeImpl.class)
                    .intrinsic(I32_CONST, AotEmitters::I32_CONST)
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
                    .shared(I32_REINTERPRET_F32, OpcodeImpl.class)
                    .shared(I32_REM_S, OpcodeImpl.class)
                    .shared(I32_REM_U, OpcodeImpl.class)
                    .shared(I32_ROTL, OpcodeImpl.class)
                    .shared(I32_ROTR, OpcodeImpl.class)
                    .intrinsic(I32_SHL, AotEmitters::I32_SHL)
                    .intrinsic(I32_SHR_S, AotEmitters::I32_SHR_S)
                    .intrinsic(I32_SHR_U, AotEmitters::I32_SHR_U)
                    .intrinsic(I32_SUB, AotEmitters::I32_SUB)
                    .shared(I32_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(I32_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(I32_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(I32_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(I32_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(I32_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(I32_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(I32_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(I32_WRAP_I64, AotEmitters::I32_WRAP_I64)
                    .intrinsic(I32_XOR, AotEmitters::I32_XOR)

                    // ====== I64 ======
                    .intrinsic(I64_ADD, AotEmitters::I64_ADD)
                    .intrinsic(I64_AND, AotEmitters::I64_AND)
                    .shared(I64_CLZ, OpcodeImpl.class)
                    .intrinsic(I64_CONST, AotEmitters::I64_CONST)
                    .shared(I64_CTZ, OpcodeImpl.class)
                    .shared(I64_DIV_S, OpcodeImpl.class)
                    .shared(I64_DIV_U, OpcodeImpl.class)
                    .shared(I64_EQ, OpcodeImpl.class)
                    .shared(I64_EQZ, OpcodeImpl.class)
                    .shared(I64_EXTEND_8_S, OpcodeImpl.class)
                    .shared(I64_EXTEND_16_S, OpcodeImpl.class)
                    .shared(I64_EXTEND_32_S, OpcodeImpl.class)
                    .intrinsic(I64_EXTEND_I32_S, AotEmitters::I64_EXTEND_I32_S)
                    .shared(I64_EXTEND_I32_U, OpcodeImpl.class)
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
                    .shared(I64_REM_S, OpcodeImpl.class)
                    .shared(I64_REM_U, OpcodeImpl.class)
                    .shared(I64_ROTL, OpcodeImpl.class)
                    .shared(I64_ROTR, OpcodeImpl.class)
                    .intrinsic(I64_SHL, AotEmitters::I64_SHL)
                    .intrinsic(I64_SHR_S, AotEmitters::I64_SHR_S)
                    .intrinsic(I64_SHR_U, AotEmitters::I64_SHR_U)
                    .intrinsic(I64_SUB, AotEmitters::I64_SUB)
                    .shared(I64_REINTERPRET_F64, OpcodeImpl.class)
                    .shared(I64_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(I64_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(I64_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(I64_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(I64_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(I64_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(I64_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(I64_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(I64_XOR, AotEmitters::I64_XOR)

                    // ====== F32 ======
                    .shared(F32_ABS, OpcodeImpl.class)
                    .intrinsic(F32_ADD, AotEmitters::F32_ADD)
                    .shared(F32_CEIL, OpcodeImpl.class)
                    .intrinsic(F32_CONST, AotEmitters::F32_CONST)
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
                    .shared(F32_REINTERPRET_I32, OpcodeImpl.class)
                    .shared(F32_SQRT, OpcodeImpl.class)
                    .intrinsic(F32_SUB, AotEmitters::F32_SUB)
                    .shared(F32_TRUNC, OpcodeImpl.class)

                    // ====== F64 ======
                    .shared(F64_ABS, OpcodeImpl.class)
                    .intrinsic(F64_ADD, AotEmitters::F64_ADD)
                    .shared(F64_CEIL, OpcodeImpl.class)
                    .intrinsic(F64_CONST, AotEmitters::F64_CONST)
                    .shared(F64_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(F64_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(F64_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(F64_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(F64_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(F64_DIV, AotEmitters::F64_DIV)
                    .shared(F64_EQ, OpcodeImpl.class)
                    .shared(F64_FLOOR, OpcodeImpl.class)
                    .shared(F64_GE, OpcodeImpl.class)
                    .shared(F64_GT, OpcodeImpl.class)
                    .shared(F64_LE, OpcodeImpl.class)
                    .shared(F64_LT, OpcodeImpl.class)
                    .shared(F64_MAX, OpcodeImpl.class)
                    .shared(F64_MIN, OpcodeImpl.class)
                    .intrinsic(F64_MUL, AotEmitters::F64_MUL)
                    .shared(F64_NE, OpcodeImpl.class)
                    .intrinsic(F64_NEG, AotEmitters::F64_NEG)
                    .shared(F64_NEAREST, OpcodeImpl.class)
                    .intrinsic(F64_PROMOTE_F32, AotEmitters::F64_PROMOTE_F32)
                    .shared(F64_REINTERPRET_I64, OpcodeImpl.class)
                    .shared(F64_SQRT, OpcodeImpl.class)
                    .intrinsic(F64_SUB, AotEmitters::F64_SUB)
                    .shared(F64_TRUNC, OpcodeImpl.class)
                    .build();

    public AotMachine(Module module, Instance instance) {
        this.module = module;
        this.instance = requireNonNull(instance, "instance");

        this.compiledFunctions = compile();
    }

    @Override
    public Value[] call(int funcId, Value[] args) throws ChicoryException {
        try {
            Value result = (Value) compiledFunctions[funcId].invoke(args);
            return new Value[] {result};
        } catch (ChicoryException e) {
            // propagate ChicoryExceptions
            throw e;
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

    private MethodHandle[] compile() {
        int importCount = module.wasmModule().importSection().count(ExternalType.FUNCTION);
        var functions = module.wasmModule().functionSection();
        var compiled = new MethodHandle[importCount + functions.functionCount()];

        for (int i = 0; i < importCount; i++) {
            compiled[i] = HostFunctionInvoker.HANDLE.bindTo(instance);
        }

        for (int i = 0; i < functions.functionCount(); i++) {
            var type = functions.getFunctionType(i, module.wasmModule().typeSection());
            var body = module.wasmModule().codeSection().getFunctionBody(i);
            var funcId = importCount + i;
            try {
                compiled[funcId] = compile(funcId, type, body);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ChicoryException(e);
            } catch (InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        return compiled;
    }

    private MethodHandle compile(int funcId, FunctionType type, FunctionBody body)
            throws NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException,
                    InstantiationException {

        var className = classNameFor(funcId);
        var internalClassName = className.replace('.', '/');

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC,
                internalClassName,
                null,
                getInternalName(Object.class),
                null);
        classWriter.visitSource("wasm", "wasm");

        // private final Memory memory;
        classWriter.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "memory",
                getDescriptor(Memory.class),
                null,
                null);

        emitConstructor(internalClassName, classWriter);

        var implWriter =
                classWriter.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "call",
                        getMethodDescriptor(
                                getType(jvmReturnType(type)),
                                Arrays.stream(jvmParameterTypes(type))
                                        .map(Type::getType)
                                        .toArray(Type[]::new)),
                        null,
                        null);

        implWriter.visitCode();

        compileBody(internalClassName, funcId, type, body, implWriter);

        implWriter.visitMaxs(0, 0);
        implWriter.visitEnd();

        classWriter.visitEnd();

        Class<?> clazz = loadClass(className, classWriter.toByteArray());
        Object target = clazz.getConstructor(Memory.class).newInstance(instance.memory());
        MethodHandle handle =
                MethodHandles.lookup()
                        .findVirtual(clazz, "call", methodTypeFor(type))
                        .bindTo(target);

        return adaptSignature(type, handle);
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
                CheckClassAdapter.verify(reader, true, new PrintWriter(System.out));
            } catch (NoClassDefFoundError ignored) {
                // the ASM verifier is an optional dependency
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
    }

    private static MethodHandle adaptSignature(FunctionType type, MethodHandle handle)
            throws IllegalAccessException {
        var argTypes = type.params();
        var argHandlers = new MethodHandle[type.params().size()];
        for (int i = 0; i < argHandlers.length; i++) {
            argHandlers[i] = publicLookup().unreflect(unboxer(argTypes.get(i)));
        }
        MethodHandle result = filterArguments(handle, 0, argHandlers);
        result = result.asSpreader(Value[].class, argTypes.size());
        if (type.returns().isEmpty()) {
            return result;
        }
        return filterReturnValue(result, publicLookup().unreflect(boxer(type.returns().get(0))));
    }

    private static String classNameFor(int funcId) {
        // TODO - use debug information (if available) to make a nice name
        return "com.dylibso.chicory.$gen.Function$" + funcId;
    }

    private static void emitConstructor(String internalClassName, ClassWriter writer) {
        var cons =
                writer.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        getMethodDescriptor(VOID_TYPE, getType(Memory.class)),
                        null,
                        null);
        cons.visitCode();

        // super();
        cons.visitVarInsn(Opcodes.ALOAD, 0);
        cons.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                getInternalName(Object.class),
                "<init>",
                getMethodDescriptor(VOID_TYPE),
                false);

        // this.memory = memory;
        cons.visitVarInsn(Opcodes.ALOAD, 0);
        cons.visitVarInsn(Opcodes.ALOAD, 1);
        cons.visitFieldInsn(
                Opcodes.PUTFIELD, internalClassName, "memory", getDescriptor(Memory.class));

        cons.visitInsn(Opcodes.RETURN);
        cons.visitMaxs(0, 0);
        cons.visitEnd();
    }

    private static boolean tryEmit(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var emitter = emitters.get(ins.opcode());
        if (emitter == null) {
            throw new ChicoryException(
                    "JVM compilation failed: opcode is not supported: " + ins.opcode());
        }
        try {
            emitter.emit(ctx, ins, asm);
            return true;
        } catch (EmitterTrapException e) {
            return false;
        }
    }

    private static void compileBody(
            String internalClassName,
            int funcId,
            FunctionType type,
            FunctionBody body,
            MethodVisitor asm) {

        var ctx = new AotContext(funcId, type, body);

        // initialize local variables to their default values
        int localsCount = type.params().size() + body.localTypes().size();
        for (int i = type.params().size(); i < localsCount; i++) {
            asm.visitLdcInsn(defaultValue(localType(type, body, i)));
            emitLocalStore(ctx, asm, i);
        }

        // memory = this.memory;
        asm.visitVarInsn(Opcodes.ALOAD, 0);
        asm.visitFieldInsn(
                Opcodes.GETFIELD, internalClassName, "memory", getDescriptor(Memory.class));
        asm.visitVarInsn(Opcodes.ASTORE, ctx.memorySlot());

        for (var ins : body.instructions()) {
            switch (ins.opcode()) {
                    // TODO - handle control flow & other "bookkeeping" opcodes here
                case NOP:
                case END:
                    break;
                case UNREACHABLE:
                    emitThrowTrapException(asm);
                    return;
                case RETURN:
                    asm.visitInsn(returnTypeOpcode(type));
                    return;
                default:
                    if (!tryEmit(ctx, ins, asm)) {
                        return;
                    }
            }
        }

        asm.visitInsn(returnTypeOpcode(type));

        if (jvmReturnType(type) != void.class) {
            ctx.popStackSize();
        }
        if (!ctx.stackSizes().isEmpty()) {
            throw new RuntimeException("Stack sizes not empty: " + ctx.stackSizes());
        }
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
        if (returnType == void.class) {
            return Opcodes.RETURN;
        }
        throw new ChicoryException("Unsupported return type: " + returnType.getName());
    }
}
