package com.dylibso.chicory.aot;

import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AotIntrinsics {

    public static class Builder {

        protected final Map<OpCode, IntrinsicEmitter> intrinsics = new HashMap<>();

        public Builder with(OpCode opCode, IntrinsicEmitter emitter) {
            intrinsics.put(opCode, emitter);
            return this;
        }

        public Builder intrinsify(OpCode opCode, Class<?> staticHelpers) {
            return with(opCode, AotIntrinsics.intrinsify(opCode, staticHelpers));
        }

        public Map<OpCode, IntrinsicEmitter> build() {
            return Map.copyOf(intrinsics);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void LOCAL_GET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var loadIndex = (int) ins.operands()[0];
        var localType = AotUtil.localType(ctx.getType(), ctx.getBody(), loadIndex);
        int opcode;
        switch (localType) {
            case I32:
                opcode = Opcodes.ILOAD;
                break;
            case I64:
                opcode = Opcodes.LLOAD;
                break;
            case F32:
                opcode = Opcodes.FLOAD;
                break;
            case F64:
                opcode = Opcodes.DLOAD;
                break;
            default:
                throw new IllegalArgumentException("Unsupported load target type: " + localType);
        }
        asm.visitVarInsn(opcode, loadIndex + 1); // +1 because local 0 is 'this'.
    }

    public static void LOCAL_SET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var storeIndex = (int) ins.operands()[0];
        var localType = AotUtil.localType(ctx.getType(), ctx.getBody(), storeIndex);
        int opcode;
        switch (localType) {
            case I32:
                opcode = Opcodes.ISTORE;
                break;
            case I64:
                opcode = Opcodes.LSTORE;
                break;
            case F32:
                opcode = Opcodes.FSTORE;
                break;
            case F64:
                opcode = Opcodes.DSTORE;
                break;
            default:
                throw new IllegalArgumentException("Unsupported store target type: " + localType);
        }
        asm.visitVarInsn(opcode, storeIndex + 1); // +1 because local 0 is 'this'.
    }

    public static void I32_ADD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IADD);
    }

    public static void I32_AND(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_MUL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IMUL);
    }

    public static void I32_OR(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IOR);
    }

    public static void I32_REM_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IREM);
    }

    public static void I32_SHL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISHL);
    }

    public static void I32_SHR_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISHR);
    }

    public static void I32_SHR_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IUSHR);
    }

    public static void I32_SUB(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISUB);
    }

    public static void I32_XOR(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IXOR);
    }



    public static IntrinsicEmitter intrinsify(OpCode opcode, Class<?> staticHelpers) {
        for (var method : staticHelpers.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(OpCodeIdentifier.class)
                    && method.getAnnotation(OpCodeIdentifier.class).value().equals(opcode)) {
                return (ctx, ins, asm) -> {
                    // TODO - should check above if method descriptor matches so that
                    // registration of this intrinsic fails if the signature does not
                    // match what is needed for this opcode.
                    asm.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(staticHelpers),
                            method.getName(),
                            Type.getMethodDescriptor(method),
                            false);
                };
            }
        }
        throw new IllegalArgumentException(
                "Static helper "
                        + staticHelpers.getName()
                        + " does not provide an implementation of opcode "
                        + opcode.name());
    }
}
