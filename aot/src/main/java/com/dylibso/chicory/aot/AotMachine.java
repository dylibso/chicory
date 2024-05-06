package com.dylibso.chicory.aot;

import static com.dylibso.chicory.wasm.types.OpCode.*;

import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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

    protected static final Map<OpCode, IntrinsicEmitter> intrinsics =
            Map.of(
                    LOCAL_GET, AotIntrinsics::LOCAL_GET,
                    I32_ADD, AotIntrinsics::I32_ADD,
                    I32_SUB, AotIntrinsics::I32_SUB,
                    I32_MUL, AotIntrinsics.intrinsify(I32_MUL, OpcodeImpl.class),
                    I32_REM_S, AotIntrinsics.intrinsify(I32_REM_S, OpcodeImpl.class)
                    );

    public AotMachine(Module module) {
        this.module = module;
        compiledFunctions = new MethodHandle[module.wasmModule().functionSection().functionCount()];
        compile();
    }

    @Override
    public Value[] call(int funcId, Value[] args, boolean popResults) throws ChicoryException {
        try {
            var result = (int) compiledFunctions[funcId].invoke(args);
            return new Value[] {Value.i32(result)};
        } catch (Throwable e) {
            throw new RuntimeException(e);
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
                                Type.getType(AotUtil.jvmReturnType(type)),
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
            argHandlers[i] = filterFor(argTypes.get(i));
        }
        return MethodHandles.filterArguments(handle, 0, argHandlers)
                .asSpreader(Value[].class, argTypes.size());
    }

    private MethodHandle filterFor(ValueType type)
            throws NoSuchMethodException, IllegalAccessException {
        return AotUtil.unboxer(type);
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

    private boolean tryEmitIntrinsic(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var emitter = intrinsics.get(ins.opcode());
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
                    if (!tryEmitIntrinsic(ctx, ins, asm)) {
                        // TODO - throw appropriate error because we couldn't match the instruction
                    }
            }
        }

        asm.visitInsn(Opcodes.IRETURN);
    }

    private void generateOpCodeImplHelperCall(AotContext ctx, Instruction ins, MethodVisitor asm) {}
}
