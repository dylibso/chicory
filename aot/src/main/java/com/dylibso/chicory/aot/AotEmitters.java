package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotMethods.*;
import static com.dylibso.chicory.aot.AotUtil.boxer;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeStatic;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeVirtual;
import static com.dylibso.chicory.aot.AotUtil.stackSize;
import static com.dylibso.chicory.aot.AotUtil.unboxer;
import static com.dylibso.chicory.aot.AotUtil.validateArgumentType;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.aot.AotUtil.StackSize;
import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.EnumMap;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AotEmitters {

    public static class Builder {

        protected final Map<OpCode, BytecodeEmitter> emitters = new EnumMap<>(OpCode.class);

        public Builder intrinsic(OpCode opCode, BytecodeEmitter emitter) {
            BytecodeEmitter wrapped = (ctx, ins, asm) -> {
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

    public static void ELEM_DROP(AotContext ctx, Instruction ins, MethodVisitor asm) {
        int index = (int) ins.operands()[0];
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        asm.visitLdcInsn(index);
        asm.visitInsn(Opcodes.ACONST_NULL);
        emitInvokeVirtual(asm, INSTANCE_SET_ELEMENT);
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

    public static void REF_FUNC(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
    }

    public static void REF_NULL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn(REF_NULL_VALUE);
    }

    public static void REF_IS_NULL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitInvokeStatic(asm, REF_IS_NULL);
    }

    public static void LOCAL_GET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        var loadIndex = (int) ins.operands()[0];
        var localType = AotUtil.localType(ctx.getType(), ctx.getBody(), loadIndex);
        int opcode;
        switch (localType) {
            case I32:
            case ExternRef:
            case FuncRef:
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
    }

    public static void emitLocalStore(AotContext ctx, MethodVisitor asm, int storeIndex) {
        var localType = AotUtil.localType(ctx.getType(), ctx.getBody(), storeIndex);
        int opcode;
        switch (localType) {
            case I32:
            case ExternRef:
            case FuncRef:
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

    public static void LOCAL_TEE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        StackSize stackSize = ctx.popStackSize();
        emitPop(asm, stackSize);
        ctx.pushStackSize(stackSize);

        LOCAL_SET(ctx, ins, asm);
    }

    public static void GLOBAL_GET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        int globalIndex = (int) ins.operands()[0];

        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        asm.visitLdcInsn(globalIndex);
        emitInvokeVirtual(asm, INSTANCE_READ_GLOBAL);

        Method unboxer = unboxer(ctx.globalTypes().get(globalIndex));
        emitInvokeVirtual(asm, unboxer);

        ctx.pushStackSize(stackSize(unboxer.getReturnType()));
    }

    public static void GLOBAL_SET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        int globalIndex = (int) ins.operands()[0];

        emitInvokeStatic(asm, boxer(ctx.globalTypes().get(globalIndex)));
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        asm.visitInsn(Opcodes.SWAP);
        asm.visitLdcInsn(globalIndex);
        asm.visitInsn(Opcodes.SWAP);
        emitInvokeVirtual(asm, INSTANCE_WRITE_GLOBAL);
    }

    public static void TABLE_GET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_GET);
    }

    public static void TABLE_SET(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_SET);
    }

    public static void TABLE_SIZE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_SIZE);
    }

    public static void TABLE_GROW(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_GROW);
    }

    public static void TABLE_FILL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_FILL);
    }

    public static void TABLE_COPY(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitLdcInsn((int) ins.operands()[1]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_COPY);
    }

    public static void TABLE_INIT(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitLdcInsn((int) ins.operands()[0]);
        asm.visitLdcInsn((int) ins.operands()[1]);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.instanceSlot());
        emitInvokeStatic(asm, TABLE_INIT);
    }

    public static void MEMORY_INIT(AotContext ctx, Instruction ins, MethodVisitor asm) {
        int segmentId = (int) ins.operands()[0];
        asm.visitLdcInsn(segmentId);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        emitInvokeStatic(asm, MEMORY_INIT);
    }

    public static void MEMORY_COPY(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        emitInvokeStatic(asm, MEMORY_COPY);
    }

    public static void MEMORY_FILL(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        emitInvokeStatic(asm, MEMORY_FILL);
    }

    public static void MEMORY_GROW(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.SWAP);
        emitInvokeVirtual(asm, MEMORY_GROW);
    }

    public static void MEMORY_SIZE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        emitInvokeVirtual(asm, MEMORY_PAGES);
    }

    public static void DATA_DROP(AotContext ctx, Instruction ins, MethodVisitor asm) {
        int segmentId = (int) ins.operands()[0];
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitLdcInsn(segmentId);
        emitInvokeVirtual(asm, MEMORY_DROP);
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
        emitRead(ctx, ins, asm, MEMORY_READ_INT);
    }

    public static void I32_LOAD8_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, MEMORY_READ_BYTE);
    }

    public static void I32_LOAD8_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.visitLdcInsn(0xFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_LOAD16_S(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, MEMORY_READ_SHORT);
    }

    public static void I32_LOAD16_U(AotContext ctx, Instruction ins, MethodVisitor asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.visitLdcInsn(0xFFFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void F32_LOAD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, MEMORY_READ_FLOAT);
    }

    public static void I64_LOAD(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitRead(ctx, ins, asm, MEMORY_READ_LONG);
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
        emitRead(ctx, ins, asm, MEMORY_READ_DOUBLE);
    }

    public static void emitRead(AotContext ctx, Instruction ins, MethodVisitor asm, Method method) {
        long offset = ins.operands()[1];
        emitThrowIfInvalidOffset(asm, offset);

        asm.visitInsn(Opcodes.DUP);
        emitInvokeStatic(asm, VALIDATE_BASE);

        // int address = base + offset;
        asm.visitLdcInsn((int) offset);
        asm.visitInsn(Opcodes.IADD);

        // memory.readType(address)
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.SWAP);
        emitInvokeVirtual(asm, method);
    }

    public static void I32_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX32Store(ctx, ins, asm, MEMORY_WRITE_INT);
    }

    public static void I32_STORE8(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2B);
        emitX32Store(ctx, ins, asm, MEMORY_WRITE_BYTE);
    }

    public static void I32_STORE16(AotContext ctx, Instruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2S);
        emitX32Store(ctx, ins, asm, MEMORY_WRITE_SHORT);
    }

    public static void F32_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX32Store(ctx, ins, asm, MEMORY_WRITE_FLOAT);
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
        emitX32Store(ctx, ins, asm, MEMORY_WRITE_INT);
    }

    private static void emitX32Store(AotContext ctx, Instruction ins, MethodVisitor asm, Method method) {

        long offset = ins.operands()[1];
        emitThrowIfInvalidOffset(asm, offset);

        // validateBase(base);
        asm.visitInsn(Opcodes.SWAP);
        asm.visitInsn(Opcodes.DUP);
        emitInvokeStatic(asm, VALIDATE_BASE);

        // int address = base + offset;
        asm.visitLdcInsn((int) offset);
        asm.visitInsn(Opcodes.IADD);

        // memory.writeType(address, (type) value)
        asm.visitInsn(Opcodes.SWAP);
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        asm.visitInsn(Opcodes.DUP_X2);
        asm.visitInsn(Opcodes.POP);
        emitInvokeVirtual(asm, method);
    }

    public static void I64_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX64Store(ctx, ins, asm, MEMORY_WRITE_LONG);
    }

    public static void F64_STORE(AotContext ctx, Instruction ins, MethodVisitor asm) {
        emitX64Store(ctx, ins, asm, MEMORY_WRITE_DOUBLE);
    }

    private static void emitX64Store(AotContext ctx, Instruction ins, MethodVisitor asm, Method method) {
        long offset = ins.operands()[1];
        emitThrowIfInvalidOffset(asm, offset);

        // validateBase(base);
        asm.visitInsn(Opcodes.DUP2_X1);
        asm.visitInsn(Opcodes.POP2);
        asm.visitInsn(Opcodes.DUP);
        emitInvokeStatic(asm, VALIDATE_BASE);

        // int address = base + offset;
        asm.visitLdcInsn((int) offset);
        asm.visitInsn(Opcodes.IADD);

        // memory.writeType(address, value)
        asm.visitVarInsn(Opcodes.ALOAD, ctx.memorySlot());
        emitInvokeStatic(asm, method);
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
                    emitInvokeStatic(asm, method);

                    for (int i = 0; i < popCount; i++) {
                        ctx.popStackSize();
                    }
                    ctx.pushStackSize(stackSize);
                };
            }
        }
        throw new IllegalArgumentException("Static helper "
                + staticHelpers.getName()
                + " does not provide an implementation of opcode "
                + opcode.name());
    }

    private static void updateStackSize(AotContext ctx, OpCode opCode) {
        switch (opCode) {
            case TABLE_GROW:
            case LOCAL_SET:
            case GLOBAL_SET:
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
            case TABLE_SET:
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
            case MEMORY_COPY:
            case MEMORY_FILL:
            case MEMORY_INIT:
            case TABLE_FILL:
            case TABLE_COPY:
            case TABLE_INIT:
                ctx.popStackSize();
                ctx.popStackSize();
                ctx.popStackSize();
                break;
            case REF_FUNC:
            case REF_NULL:
            case MEMORY_SIZE:
            case TABLE_SIZE:
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
            case ELEM_DROP:
            case REF_IS_NULL:
            case TABLE_GET:
            case MEMORY_GROW:
            case DATA_DROP:
            case LOCAL_TEE:
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
            case SELECT_T:
            case LOCAL_GET:
            case GLOBAL_GET:
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
            emitInvokeStatic(asm, THROW_OUT_OF_BOUNDS_MEMORY_ACCESS);
            asm.visitInsn(Opcodes.ATHROW);
            throw new EmitterTrapException();
        }
    }

    public static void emitThrowTrapException(MethodVisitor asm) {
        emitInvokeStatic(asm, THROW_TRAP_EXCEPTION);
        asm.visitInsn(Opcodes.ATHROW);
    }
}
