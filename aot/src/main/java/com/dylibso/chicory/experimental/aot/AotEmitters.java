package com.dylibso.chicory.experimental.aot;

import static com.dylibso.chicory.experimental.aot.AotUtil.asmType;
import static com.dylibso.chicory.experimental.aot.AotUtil.callIndirectMethodName;
import static com.dylibso.chicory.experimental.aot.AotUtil.callIndirectMethodType;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitInvokeFunction;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitInvokeStatic;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitInvokeVirtual;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitJvmToLong;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitLongToJvm;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitPop;
import static com.dylibso.chicory.experimental.aot.AotUtil.hasTooManyParameters;
import static com.dylibso.chicory.experimental.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.experimental.aot.AotUtil.localType;
import static com.dylibso.chicory.experimental.aot.AotUtil.slotCount;
import static com.dylibso.chicory.experimental.aot.AotUtil.valueMethodName;
import static com.dylibso.chicory.experimental.aot.AotUtil.valueMethodType;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.getType;
import static org.objectweb.asm.commons.InstructionAdapter.OBJECT_TYPE;

import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;

final class AotEmitters {

    private AotEmitters() {}

    @FunctionalInterface
    interface BytecodeEmitter {
        void emit(AotContext context, AotInstruction ins, InstructionAdapter asm);
    }

    static class Builder {

        private final Map<AotOpCode, BytecodeEmitter> emitters = new EnumMap<>(AotOpCode.class);

        public Builder intrinsic(AotOpCode opCode, BytecodeEmitter emitter) {
            emitters.put(opCode, emitter);
            return this;
        }

        public Builder shared(AotOpCode opCode, Class<?> staticHelpers) {
            emitters.put(opCode, AotEmitters.intrinsify(opCode, staticHelpers));
            return this;
        }

        public Map<AotOpCode, BytecodeEmitter> build() {
            return Map.copyOf(emitters);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void TRAP(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitInvokeStatic(asm, AotMethodRefs.THROW_TRAP_EXCEPTION);
        asm.athrow();
    }

    public static void DROP_KEEP(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int keepStart = (int) ins.operand(0) + 1;

        // save result values
        int slot = ctx.tempSlot();
        for (int i = ins.operandCount() - 1; i >= keepStart; i--) {
            var type = ValueType.forId((int) ins.operand(i));
            asm.store(slot, asmType(type));
            slot += slotCount(type);
        }

        // drop intervening values
        for (int i = keepStart - 1; i >= 1; i--) {
            emitPop(asm, ValueType.forId((int) ins.operand(i)));
        }

        // restore result values
        for (int i = keepStart; i < ins.operandCount(); i++) {
            var type = ValueType.forId((int) ins.operand(i));
            slot -= slotCount(type);
            asm.load(slot, asmType(type));
        }
    }

    public static void RETURN(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        if (ctx.getType().returns().size() > 1) {
            asm.invokestatic(
                    ctx.internalClassName(),
                    valueMethodName(ctx.getType().returns()),
                    valueMethodType(ctx.getType().returns()).toMethodDescriptorString(),
                    false);
        }
        asm.areturn(getType(jvmReturnType(ctx.getType())));
    }

    public static void DROP(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitPop(asm, ValueType.forId((int) ins.operand(0)));
    }

    public static void ELEM_DROP(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int index = (int) ins.operand(0);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        asm.iconst(index);
        asm.aconst(null);
        emitInvokeVirtual(asm, AotMethodRefs.INSTANCE_SET_ELEMENT);
    }

    public static void SELECT(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        var type = ValueType.forId((int) ins.operand(0));
        var endLabel = new Label();
        asm.ifne(endLabel);
        if (slotCount(type) == 1) {
            asm.swap();
        } else {
            asm.dup2X2();
            asm.pop2();
        }
        asm.mark(endLabel);
        emitPop(asm, type);
    }

    private static void emitBoxValuesOnStack(
            AotContext ctx, InstructionAdapter asm, List<ValueType> types) {

        // Store values from stack to locals in reverse order
        int slot = ctx.tempSlot() + types.stream().mapToInt(AotUtil::slotCount).sum();
        for (int i = types.size() - 1; i >= 0; i--) {
            ValueType valueType = types.get(i);
            slot -= slotCount(valueType);
            asm.store(slot, asmType(valueType));
        }

        // Create the array
        asm.iconst(types.size());
        asm.newarray(LONG_TYPE);

        // Load from locals and store in array
        slot = ctx.tempSlot();
        for (int i = 0; i < types.size(); i++) {
            ValueType valueType = types.get(i);

            asm.dup(); // Duplicate the array reference
            asm.iconst(i); // Array index
            asm.load(slot, asmType(valueType)); // Load value from local
            slot += slotCount(valueType);
            emitJvmToLong(asm, valueType); // Convert to long
            asm.astore(LONG_TYPE); // Store in array
        }
    }

    public static void CALL(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int funcId = (int) ins.operand(0);
        FunctionType functionType = ctx.functionTypes().get(funcId);

        emitInvokeStatic(asm, AotMethodRefs.CHECK_INTERRUPTION);
        if (hasTooManyParameters(functionType)) {
            emitBoxValuesOnStack(ctx, asm, functionType.params());
        }

        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeFunction(asm, ctx.internalClassName(), funcId, functionType);

        if (functionType.returns().size() > 1) {
            emitUnboxResult(asm, ctx, functionType.returns());
        }
    }

    public static void CALL_INDIRECT(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int typeId = (int) ins.operand(0);
        int tableIdx = (int) ins.operand(1);
        FunctionType functionType = ctx.types()[typeId];

        if (hasTooManyParameters(functionType)) {
            emitBoxValuesOnStack(ctx, asm, functionType.params());
        }

        asm.iconst(tableIdx);
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        // stack: arguments, funcTableIdx, tableIdx, memory, instance

        asm.invokestatic(
                ctx.internalClassName(),
                callIndirectMethodName(typeId),
                callIndirectMethodType(functionType).toMethodDescriptorString(),
                false);

        if (functionType.returns().size() > 1) {
            emitUnboxResult(asm, ctx, functionType.returns());
        }
    }

    public static void REF_FUNC(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
    }

    public static void REF_NULL(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst(REF_NULL_VALUE);
    }

    public static void REF_IS_NULL(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitInvokeStatic(asm, AotMethodRefs.REF_IS_NULL);
    }

    public static void LOCAL_GET(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        var loadIndex = (int) ins.operand(0);
        var localType = localType(ctx.getType(), ctx.getBody(), loadIndex);
        asm.load(ctx.localSlotIndex(loadIndex), asmType(localType));
    }

    public static void LOCAL_SET(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int index = (int) ins.operand(0);
        var localType = localType(ctx.getType(), ctx.getBody(), index);
        asm.store(ctx.localSlotIndex(index), asmType(localType));
    }

    public static void LOCAL_TEE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        if (slotCount(ValueType.forId((int) ins.operand(1))) == 1) {
            asm.dup();
        } else {
            asm.dup2();
        }

        LOCAL_SET(ctx, ins, asm);
    }

    public static void GLOBAL_GET(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int globalIndex = (int) ins.operand(0);

        asm.iconst(globalIndex);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.READ_GLOBAL);

        emitLongToJvm(asm, ctx.globalTypes().get(globalIndex));
    }

    public static void GLOBAL_SET(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        int globalIndex = (int) ins.operand(0);

        emitJvmToLong(asm, ctx.globalTypes().get(globalIndex));
        asm.iconst(globalIndex);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.WRITE_GLOBAL);
    }

    public static void TABLE_GET(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_GET);
    }

    public static void TABLE_SET(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_SET);
    }

    public static void TABLE_SIZE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_SIZE);
    }

    public static void TABLE_GROW(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_GROW);
    }

    public static void TABLE_FILL(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_FILL);
    }

    public static void TABLE_COPY(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.iconst((int) ins.operand(1));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_COPY);
    }

    public static void TABLE_INIT(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.iconst((int) ins.operand(1));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.TABLE_INIT);
    }

    public static void MEMORY_INIT(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.MEMORY_INIT);
    }

    public static void MEMORY_COPY(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.MEMORY_COPY);
    }

    public static void MEMORY_FILL(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.MEMORY_FILL);
    }

    public static void MEMORY_GROW(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.MEMORY_GROW);
    }

    public static void MEMORY_SIZE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.MEMORY_PAGES);
    }

    public static void DATA_DROP(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, AotMethodRefs.MEMORY_DROP);
    }

    public static void I32_ADD(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IADD);
    }

    public static void I32_AND(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_CONST(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
    }

    public static void I32_MUL(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IMUL);
    }

    public static void I32_OR(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IOR);
    }

    public static void I32_SHL(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISHL);
    }

    public static void I32_SHR_S(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISHR);
    }

    public static void I32_SHR_U(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IUSHR);
    }

    public static void I32_SUB(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISUB);
    }

    public static void I32_WRAP_I64(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
    }

    public static void I32_XOR(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IXOR);
    }

    public static void I64_ADD(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LADD);
    }

    public static void I64_AND(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LAND);
    }

    public static void I64_CONST(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.lconst(ins.operand(0));
    }

    public static void I64_EXTEND_I32_S(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_MUL(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LMUL);
    }

    public static void I64_OR(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LOR);
    }

    public static void I64_SHL(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHL);
    }

    public static void I64_SHR_S(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHR);
    }

    public static void I64_SHR_U(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LUSHR);
    }

    public static void I64_SUB(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LSUB);
    }

    public static void I64_XOR(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LXOR);
    }

    public static void F32_ADD(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FADD);
    }

    public static void F32_CONST(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.fconst(intBitsToFloat((int) ins.operand(0)));
    }

    public static void F32_DEMOTE_F64(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.D2F);
    }

    public static void F32_DIV(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FDIV);
    }

    public static void F32_MUL(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FMUL);
    }

    public static void F32_NEG(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FNEG);
    }

    public static void F32_SUB(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FSUB);
    }

    public static void F64_ADD(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DADD);
    }

    public static void F64_CONST(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.dconst(longBitsToDouble(ins.operand(0)));
    }

    public static void F64_DIV(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DDIV);
    }

    public static void F64_MUL(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DMUL);
    }

    public static void F64_NEG(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DNEG);
    }

    public static void F64_PROMOTE_F32(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.F2D);
    }

    public static void F64_SUB(AotContext ctx, AotInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DSUB);
    }

    public static void I32_LOAD(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_READ_INT);
    }

    public static void I32_LOAD8_S(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_READ_BYTE);
    }

    public static void I32_LOAD8_U(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.iconst(0xFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_LOAD16_S(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_READ_SHORT);
    }

    public static void I32_LOAD16_U(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.iconst(0xFFFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void F32_LOAD(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_READ_FLOAT);
    }

    public static void I64_LOAD(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_READ_LONG);
    }

    public static void I64_LOAD8_S(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD8_U(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD8_U(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_S(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_U(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD16_U(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_S(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_U(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        I32_LOAD(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
        asm.lconst(0xFFFF_FFFFL);
        asm.visitInsn(Opcodes.LAND);
    }

    public static void F64_LOAD(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_READ_DOUBLE);
    }

    public static void I32_STORE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_INT);
    }

    public static void I32_STORE8(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.I2B);
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_BYTE);
    }

    public static void I32_STORE16(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.I2S);
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_SHORT);
    }

    public static void F32_STORE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_FLOAT);
    }

    public static void I64_STORE8(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        I32_STORE8(ctx, ins, asm);
    }

    public static void I64_STORE16(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        I32_STORE16(ctx, ins, asm);
    }

    public static void I64_STORE32(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_INT);
    }

    public static void I64_STORE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_LONG);
    }

    public static void F64_STORE(AotContext ctx, AotInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, AotMethodRefs.MEMORY_WRITE_DOUBLE);
    }

    private static void emitLoadOrStore(
            AotContext ctx, AotInstruction ins, InstructionAdapter asm, Method method) {
        long offset = ins.operand(1);

        if (offset < 0 || offset >= Integer.MAX_VALUE) {
            emitInvokeStatic(asm, AotMethodRefs.THROW_OUT_OF_BOUNDS_MEMORY_ACCESS);
            asm.athrow();
        }

        asm.iconst((int) offset);
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, method);
    }

    private static void emitUnboxResult(
            InstructionAdapter asm, AotContext ctx, List<ValueType> types) {
        asm.store(ctx.tempSlot(), OBJECT_TYPE);
        for (int i = 0; i < types.size(); i++) {
            asm.load(ctx.tempSlot(), OBJECT_TYPE);
            asm.iconst(i);
            asm.aload(LONG_TYPE);
            emitLongToJvm(asm, types.get(i));
        }
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
    public static BytecodeEmitter intrinsify(AotOpCode opcode, Class<?> staticHelpers) {
        for (var method : staticHelpers.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(OpCodeIdentifier.class)
                    && method.getAnnotation(OpCodeIdentifier.class).value() == opcode.opcode()) {
                return (ctx, ins, asm) -> emitInvokeStatic(asm, method);
            }
        }
        throw new IllegalArgumentException(
                "Static helper "
                        + staticHelpers.getName()
                        + " does not provide an implementation of opcode "
                        + opcode.name());
    }
}
