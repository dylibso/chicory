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

public class AotEmitters {

    public static class Builder {

        protected final Map<OpCode, BytecodeEmitter> emitters = new HashMap<>();

        public Builder intrinsic(OpCode opCode, BytecodeEmitter emitter) {
            emitters.put(opCode, emitter);
            return this;
        }

        public Builder shared(OpCode opCode, Class<?> staticHelpers) {
            return intrinsic(opCode, AotEmitters.intrinsify(opCode, staticHelpers));
        }

        public Map<OpCode, BytecodeEmitter> build() {
            return Map.copyOf(emitters);
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
        asm.visitVarInsn(opcode, ctx.slotIndex(loadIndex + 1)); // +1 because local 0 is 'this'.
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
        asm.visitVarInsn(opcode, ctx.slotIndex(storeIndex + 1)); // +1 because local 0 is 'this'.
    }

    public static void I32_ADD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IADD);
    }

    public static void I32_AND(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_CONST(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
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

    public static void I32_WRAP_I64(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
    }

    public static void I32_XOR(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IXOR);
    }

    public static void I64_ADD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LADD);
    }

    public static void I64_AND(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LAND);
    }

    public static void I64_CONST(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn(ins.operands()[0]);
    }

    public static void I64_EXTEND_I32_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_MUL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LMUL);
    }

    public static void I64_OR(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LOR);
    }

    public static void I64_REM_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LREM);
    }

    public static void I64_SHL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHL);
    }

    public static void I64_SHR_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHR);
    }

    public static void I64_SHR_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LUSHR);
    }

    public static void I64_SUB(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LSUB);
    }

    public static void I64_XOR(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LXOR);
    }

    public static void F32_ADD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FADD);
    }

    public static void F32_CONST(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn(Float.intBitsToFloat((int) ins.operands()[0]));
    }

    public static void F32_DEMOTE_F64(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.D2F);
    }

    public static void F32_DIV(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FDIV);
    }

    public static void F32_MUL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FMUL);
    }

    public static void F32_NEG(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FNEG);
    }

    public static void F32_SUB(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FSUB);
    }

    public static void F64_ADD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DADD);
    }

    public static void F64_CONST(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn(Double.longBitsToDouble(ins.operands()[0]));
    }

    public static void F64_DIV(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DDIV);
    }

    public static void F64_MUL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DMUL);
    }

    public static void F64_NEG(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DNEG);
    }

    public static void F64_PROMOTE_F32(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.F2D);
    }

    public static void F64_SUB(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DSUB);
    }

    /**
     * The AOT compiler assumes two main ways of implementing opcodes: intrinsics, and shared implementations.
     * Intrinsics refer to WASM opcodes that are implemented in the AOT by assembling JVM bytecode that implements
     * the logic of the opcode. Shared implementations refer to static methods in a public class that do the same,
     * with the term "shared" referring to the fact that these implementations are intended to be used by both the AOT
     * and the interpreter.
     * <p>
     * This method takes an opcode and a class (which must have a public static method annotated as an
     * implementation of the opcode) and creates a BytecodeEmitter that will implement the WASM opcode
     * as a static method call to the implementation provided by the class. That is, it "intrinsifies"
     * the shared implementation by generating a static call to it. The method implementing
     * the opcode must have a signature that exactly matches the stack operands and result type of
     * the opcode, and if its parameters are order-sensitive then they must be in the order that
     * produces the expected result when the JVM's stack and calling convention are used instead of
     * the interpreter's. That is, if order is significant they must be in the order
     * methodName(..., tos - 2, tos - 1, tos) where "tos" is the top-of-stack value.
     *
     * @param opcode the WASM opcode that is implemented by an annotated static method in this class
     * @param staticHelpers the class containing the implementation
     * @return a BytecodeEmitter that will implement the opcode via a call to the shared implementation
     */
    public static BytecodeEmitter intrinsify(OpCode opcode, Class<?> staticHelpers) {
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
