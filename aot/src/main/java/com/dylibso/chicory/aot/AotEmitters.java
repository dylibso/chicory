package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotUtil.stackSize;
import static com.dylibso.chicory.aot.AotUtil.validateArgumentType;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import com.dylibso.chicory.aot.AotUtil.StackSize;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AotEmitters {

    public static class Builder {

        protected final Map<OpCode, BytecodeEmitter> emitters = new EnumMap<>(OpCode.class);

        public Builder intrinsic(OpCode opCode, BytecodeEmitter emitter) {
            BytecodeEmitter wrapped =
                    (ctx, ins, asm) -> {
                        emitter.emit(ctx, ins, asm);
                        updateStackSize(ctx, opCode);
                    };
            emitters.put(opCode, wrapped);
            return this;
        }

        public Builder shared(OpCode opCode, Class<?> staticHelpers) {
            emitters.put(opCode, AotEmitters.intrinsify(opCode, staticHelpers));
            return this;
        }

        public Map<OpCode, BytecodeEmitter> build() {
            return Map.copyOf(emitters);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void DROP(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitPop(asm, ctx.popStackSize());
    }

    public static void SELECT(AotContext ctx, Instruction ins, MethodVisitor asm) {
        ctx.popStackSize();
        StackSize stackSize = ctx.popStackSize();

        var endLabel = new Label();
        asm.visitJumpInsn(Opcodes.IFNE, endLabel);
        if (stackSize == StackSize.ONE) {
            asm.visitInsn(Opcodes.SWAP);
        } else {
            asm.visitInsn(Opcodes.DUP2_X2);
            asm.visitInsn(Opcodes.POP2);
        }
        asm.visitLabel(endLabel);
        emitPop(asm, stackSize);
    }

    public static void LOCAL_GET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var loadIndex = (int) ins.operands()[0];
        var localType = AotUtil.localType(ctx.getType(), ctx.getBody(), loadIndex);
        int opcode;
        switch (localType) {
            case I32:
                opcode = Opcodes.ILOAD;
                ctx.pushStackSize(StackSize.ONE);
                break;
            case I64:
                opcode = Opcodes.LLOAD;
                ctx.pushStackSize(StackSize.TWO);
                break;
            case F32:
                opcode = Opcodes.FLOAD;
                ctx.pushStackSize(StackSize.ONE);
                break;
            case F64:
                opcode = Opcodes.DLOAD;
                ctx.pushStackSize(StackSize.TWO);
                break;
            default:
                throw new IllegalArgumentException("Unsupported load target type: " + localType);
        }
        asm.visitVarInsn(opcode, ctx.localSlotIndex(loadIndex));
    }

    public static void LOCAL_SET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitLocalStore(ctx, asm, (int) ins.operands()[0]);
        ctx.popStackSize();
    }

    public static void emitLocalStore(AotContext ctx, MethodVisitor asm, int storeIndex) {
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
        asm.visitVarInsn(opcode, ctx.localSlotIndex(storeIndex));
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

    public static void I32_LOAD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, "readInt", INT_TYPE);
    }

    public static void I32_LOAD8_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, "read", BYTE_TYPE);
    }

    public static void I32_LOAD8_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.visitLdcInsn(0xFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_LOAD16_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, "readShort", SHORT_TYPE);
    }

    public static void I32_LOAD16_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.visitLdcInsn(0xFFFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void F32_LOAD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, "readFloat", FLOAT_TYPE);
    }

    public static void I64_LOAD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, "readLong", LONG_TYPE);
    }

    public static void I64_LOAD8_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD8_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD8_U(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD16_U(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
        asm.visitLdcInsn(0xFFFF_FFFFL);
        asm.visitInsn(Opcodes.LAND);
    }

    public static void F64_LOAD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, "readDouble", DOUBLE_TYPE);
    }

    public static void emitRead(
            AotContext ctx,
            Instruction ins,
            MethodVisitor asm,
            String readMethod,
            Type returnType) {

        long offset = ins.operands()[1];
        emitThrowIfInvalidOffset(asm, offset);

        asm.visitInsn(Opcodes.DUP);
        emitValidateBase(asm);

        // int address = base + offset;
        asm.visitLdcInsn((int) offset);
        asm.visitInsn(Opcodes.IADD);

        // memory.readType(address)
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.SWAP);
        asm.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                getInternalName(Memory.class),
                readMethod,
                getMethodDescriptor(returnType, INT_TYPE),
                false);
    }

    public static void I32_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX32Store(ctx, ins, asm, "writeI32", INT_TYPE);
    }

    public static void I32_STORE8(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2B);
        emitX32Store(ctx, ins, asm, "writeByte", BYTE_TYPE);
    }

    public static void I32_STORE16(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2S);
        emitX32Store(ctx, ins, asm, "writeShort", SHORT_TYPE);
    }

    public static void F32_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX32Store(ctx, ins, asm, "writeF32", FLOAT_TYPE);
    }

    public static void I64_STORE8(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        I32_STORE8(ctx, ins, asm);
    }

    public static void I64_STORE16(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        I32_STORE16(ctx, ins, asm);
    }

    public static void I64_STORE32(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        emitX32Store(ctx, ins, asm, "writeI32", INT_TYPE);
    }

    private static void emitX32Store(
            AotContext ctx, Instruction ins, MethodVisitor asm, String writeMethod, Type argType) {

        long offset = ins.operands()[1];
        emitThrowIfInvalidOffset(asm, offset);

        // validateBase(base);
        asm.visitInsn(Opcodes.SWAP);
        asm.visitInsn(Opcodes.DUP);
        emitValidateBase(asm);

        // int address = base + offset;
        asm.visitLdcInsn((int) offset);
        asm.visitInsn(Opcodes.IADD);

        // memory.writeType(address, (type) value)
        asm.visitInsn(Opcodes.SWAP);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.DUP_X2);
        asm.visitInsn(Opcodes.POP);
        asm.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                getInternalName(Memory.class),
                writeMethod,
                getMethodDescriptor(VOID_TYPE, INT_TYPE, argType),
                false);
    }

    public static void I64_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX64StoreSetup(ctx, ins, asm);

        // memory.writeLong(address, value)
        asm.visitVarInsn(Opcodes.LSTORE, ctx.longSlot());
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.SWAP);
        asm.visitVarInsn(Opcodes.LLOAD, ctx.longSlot());
        asm.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                getInternalName(Memory.class),
                "writeLong",
                getMethodDescriptor(VOID_TYPE, INT_TYPE, LONG_TYPE),
                false);
    }

    public static void F64_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX64StoreSetup(ctx, ins, asm);

        // memory.writeF64(address, value)
        asm.visitVarInsn(Opcodes.DSTORE, ctx.doubleSlot());
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.SWAP);
        asm.visitVarInsn(Opcodes.DLOAD, ctx.doubleSlot());
        asm.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                getInternalName(Memory.class),
                "writeF64",
                getMethodDescriptor(VOID_TYPE, INT_TYPE, DOUBLE_TYPE),
                false);
    }

    private static void emitX64StoreSetup(AotContext ctx, Instruction ins, MethodVisitor asm) {
        long offset = ins.operands()[1];
        emitThrowIfInvalidOffset(asm, offset);

        // validateBase(base);
        asm.visitInsn(Opcodes.DUP2_X1);
        asm.visitInsn(Opcodes.POP2);
        asm.visitInsn(Opcodes.DUP);
        emitValidateBase(asm);

        // int address = base + offset;
        asm.visitLdcInsn((int) offset);
        asm.visitInsn(Opcodes.IADD);

        // move value to top of stack
        asm.visitInsn(Opcodes.DUP_X2);
        asm.visitInsn(Opcodes.POP);
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
                // get stack usage and stack value size
                for (Parameter parameter : method.getParameters()) {
                    validateArgumentType(parameter.getType());
                }
                int popCount = method.getParameterCount();
                StackSize stackSize = stackSize(method.getReturnType());

                return (ctx, ins, asm) -> {
                    // TODO - should check above if method descriptor matches so that
                    // registration of this intrinsic fails if the signature does not
                    // match what is needed for this opcode.
                    asm.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            getInternalName(staticHelpers),
                            method.getName(),
                            getMethodDescriptor(method),
                            false);

                    for (int i = 0; i < popCount; i++) {
                        ctx.popStackSize();
                    }
                    ctx.pushStackSize(stackSize);
                };
            }
        }
        throw new IllegalArgumentException(
                "Static helper "
                        + staticHelpers.getName()
                        + " does not provide an implementation of opcode "
                        + opcode.name());
    }

    private static void updateStackSize(AotContext ctx, OpCode opCode) {
        switch (opCode) {
            case I32_ADD:
            case I32_AND:
            case I32_MUL:
            case I32_OR:
            case I32_REM_S:
            case I32_SHL:
            case I32_SHR_S:
            case I32_SHR_U:
            case I32_SUB:
            case I32_XOR:
            case I64_ADD:
            case I64_AND:
            case I64_MUL:
            case I64_OR:
            case I64_REM_S:
            case I64_SHL:
            case I64_SHR_S:
            case I64_SHR_U:
            case I64_SUB:
            case I64_XOR:
            case F32_ADD:
            case F32_DIV:
            case F32_MUL:
            case F32_SUB:
            case F64_ADD:
            case F64_DIV:
            case F64_MUL:
            case F64_SUB:
                ctx.popStackSize();
                break;
            case I32_STORE:
            case I32_STORE8:
            case I32_STORE16:
            case F32_STORE:
            case I64_STORE:
            case I64_STORE8:
            case I64_STORE16:
            case I64_STORE32:
            case F64_STORE:
                ctx.popStackSize();
                ctx.popStackSize();
                break;
            case I32_CONST:
            case F32_CONST:
                ctx.pushStackSize(StackSize.ONE);
                break;
            case I64_CONST:
            case F64_CONST:
                ctx.pushStackSize(StackSize.TWO);
                break;
            case I32_WRAP_I64:
            case F32_DEMOTE_F64:
                ctx.popStackSize();
                ctx.pushStackSize(StackSize.ONE);
                break;
            case I64_EXTEND_I32_S:
            case I64_LOAD:
            case I64_LOAD8_S:
            case I64_LOAD8_U:
            case I64_LOAD16_S:
            case I64_LOAD16_U:
            case I64_LOAD32_S:
            case I64_LOAD32_U:
            case F64_PROMOTE_F32:
            case F64_LOAD:
                ctx.popStackSize();
                ctx.pushStackSize(StackSize.TWO);
                break;
            case F32_NEG:
            case F64_NEG:
            case I32_LOAD:
            case I32_LOAD8_S:
            case I32_LOAD8_U:
            case I32_LOAD16_S:
            case I32_LOAD16_U:
            case F32_LOAD:
                // no change to stack size
                break;
            case DROP:
            case SELECT:
            case LOCAL_GET:
            case LOCAL_SET:
                // handled in the opcode implementation
                break;
            default:
                throw new IllegalArgumentException("Unhandled opcode: " + opCode);
        }
    }

    private static void emitPop(MethodVisitor asm, StackSize size) {
        asm.visitInsn(size == StackSize.ONE ? Opcodes.POP : Opcodes.POP2);
    }

    private static void emitThrowIfInvalidOffset(MethodVisitor asm, long offset) {
        if (offset < 0 || offset >= Integer.MAX_VALUE) {
            emitThrowOutOfBoundsMemoryAccess(asm);
            throw new EmitterTrapException();
        }
    }

    private static void emitValidateBase(MethodVisitor asm) {
        asm.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                getInternalName(AotEmitters.class),
                "validateBase",
                getMethodDescriptor(VOID_TYPE, INT_TYPE),
                false);
    }

    private static void emitThrowOutOfBoundsMemoryAccess(MethodVisitor asm) {
        asm.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                getInternalName(AotEmitters.class),
                "throwOutOfBoundsMemoryAccess",
                getMethodDescriptor(getType(RuntimeException.class)),
                false);
        asm.visitInsn(Opcodes.ATHROW);
    }

    public static void emitThrowTrapException(MethodVisitor asm) {
        asm.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                getInternalName(AotEmitters.class),
                "throwTrapException",
                getMethodDescriptor(getType(RuntimeException.class)),
                false);
        asm.visitInsn(Opcodes.ATHROW);
    }

    @UsedByGeneratedCode
    public static void validateBase(int base) {
        if (base < 0) {
            throwOutOfBoundsMemoryAccess();
        }
    }

    @UsedByGeneratedCode
    public static RuntimeException throwOutOfBoundsMemoryAccess() {
        throw new WASMRuntimeException("out of bounds memory access");
    }

    @UsedByGeneratedCode
    public static RuntimeException throwTrapException() {
        throw new TrapException("Trapped on unreachable instruction", List.of());
    }
}
