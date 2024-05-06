package com.dylibso.chicory.aot;

import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.reflect.Modifier;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AotIntrinsics {

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

    public static void I32_ADD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IADD);
    }

    public static void I32_SUB(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISUB);
    }

    public static IntrinsicEmitter intrinsify(OpCode opcode, Class<?> staticHelpers) {
        for (var method : staticHelpers.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(OpCodeIdentifier.class)
                    && method.getAnnotation(OpCodeIdentifier.class).value().equals(opcode)) {
                return (ctx, ins, asm) -> {
                    // TODO - should check above if method descriptor matches so that
                    // registration of this intrinsic fails
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
