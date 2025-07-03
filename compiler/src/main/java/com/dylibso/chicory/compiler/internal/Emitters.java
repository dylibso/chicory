package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.asmType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.callIndirectMethodName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.callIndirectMethodType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeFunction;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeStatic;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeVirtual;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitJvmToLong;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitLongToJvm;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitPop;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.hasTooManyParameters;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.jvmReturnType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.localType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.slotCount;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.valueMethodName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.valueMethodType;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.EXCEPTION_MATCHES;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;
import static org.objectweb.asm.commons.InstructionAdapter.OBJECT_TYPE;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.runtime.WasmException;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;

final class Emitters {

    private Emitters() {}

    @FunctionalInterface
    interface BytecodeEmitter {
        void emit(Context context, CompilerInstruction ins, InstructionAdapter asm);
    }

    static class Builder {

        private final Map<CompilerOpCode, BytecodeEmitter> emitters =
                new EnumMap<>(CompilerOpCode.class);

        public Builder intrinsic(CompilerOpCode opCode, BytecodeEmitter emitter) {
            emitters.put(opCode, emitter);
            return this;
        }

        public Builder shared(CompilerOpCode opCode, Class<?> staticHelpers) {
            emitters.put(opCode, Emitters.intrinsify(opCode, staticHelpers));
            return this;
        }

        public Map<CompilerOpCode, BytecodeEmitter> build() {
            return Map.copyOf(emitters);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void TRAP(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitInvokeStatic(asm, ShadedRefs.THROW_TRAP_EXCEPTION);
        asm.athrow();
    }

    public static ValType valType(long id, Context ctx) {
        return ValType.builder().fromId(id).build(ctx::type);
    }

    public static void DROP_KEEP(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int keepStart = (int) ins.operand(0) + 1;

        // save result values
        int slot = ctx.tempSlot();
        for (int i = ins.operandCount() - 1; i >= keepStart; i--) {
            var type = valType(ins.operand(i), ctx);
            asm.store(slot, asmType(type));
            slot += slotCount(type);
        }

        // drop intervening values
        for (int i = keepStart - 1; i >= 1; i--) {
            emitPop(asm, valType(ins.operand(i), ctx));
        }

        // restore result values
        for (int i = keepStart; i < ins.operandCount(); i++) {
            var type = valType(ins.operand(i), ctx);
            slot -= slotCount(type);
            asm.load(slot, asmType(type));
        }
    }

    public static void RETURN(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        if (ctx.getType().returns().size() > 1) {
            asm.invokestatic(
                    ctx.internalClassName(),
                    valueMethodName(ctx.getType().returns()),
                    valueMethodType(ctx.getType().returns()).toMethodDescriptorString(),
                    false);
        }
        asm.areturn(getType(jvmReturnType(ctx.getType())));
    }

    public static void DROP(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitPop(asm, valType(ins.operand(0), ctx));
    }

    public static void ELEM_DROP(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int index = (int) ins.operand(0);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        asm.iconst(index);
        asm.aconst(null);
        emitInvokeVirtual(asm, ShadedRefs.INSTANCE_SET_ELEMENT);
    }

    public static void SELECT(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        var type = valType(ins.operand(0), ctx);
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
            Context ctx, InstructionAdapter asm, List<ValType> types) {

        // Store values from stack to locals in reverse order
        int slot = ctx.tempSlot() + types.stream().mapToInt(CompilerUtil::slotCount).sum();
        for (int i = types.size() - 1; i >= 0; i--) {
            ValType valType = types.get(i);
            slot -= slotCount(valType);
            asm.store(slot, asmType(valType));
        }

        // Create the array
        asm.iconst(types.size());
        asm.newarray(LONG_TYPE);

        // Load from locals and store in array
        slot = ctx.tempSlot();
        for (int i = 0; i < types.size(); i++) {
            ValType valType = types.get(i);

            asm.dup(); // Duplicate the array reference
            asm.iconst(i); // Array index
            asm.load(slot, asmType(valType)); // Load value from local
            slot += slotCount(valType);
            emitJvmToLong(asm, valType); // Convert to long
            asm.astore(LONG_TYPE); // Store in array
        }
    }

    public static void CALL(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int funcId = (int) ins.operand(0);
        FunctionType functionType = ctx.functionTypes().get(funcId);

        emitInvokeStatic(asm, ShadedRefs.CHECK_INTERRUPTION);
        if (hasTooManyParameters(functionType)) {
            emitBoxValuesOnStack(ctx, asm, functionType.params());
        }

        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeFunction(
                asm,
                ctx.internalClassName() + ctx.classNameForFuncGroup(funcId),
                funcId,
                functionType);

        if (functionType.returns().size() > 1) {
            emitUnboxResult(asm, ctx, functionType.returns());
        }
    }

    public static void CALL_INDIRECT(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
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

    public static void REF_FUNC(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
    }

    public static void REF_NULL(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst(REF_NULL_VALUE);
    }

    public static void REF_IS_NULL(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitInvokeStatic(asm, ShadedRefs.REF_IS_NULL);
    }

    public static void LOCAL_GET(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        var loadIndex = (int) ins.operand(0);
        var localType = localType(ctx.getType(), ctx.getBody(), loadIndex);
        asm.load(ctx.localSlotIndex(loadIndex), asmType(localType));
    }

    public static void LOCAL_SET(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int index = (int) ins.operand(0);
        var localType = localType(ctx.getType(), ctx.getBody(), index);
        asm.store(ctx.localSlotIndex(index), asmType(localType));
    }

    public static void LOCAL_TEE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        if (slotCount(valType(ins.operand(1), ctx)) == 1) {
            asm.dup();
        } else {
            asm.dup2();
        }

        LOCAL_SET(ctx, ins, asm);
    }

    public static void GLOBAL_GET(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int globalIndex = (int) ins.operand(0);

        asm.iconst(globalIndex);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.READ_GLOBAL);

        emitLongToJvm(asm, ctx.globalTypes().get(globalIndex));
    }

    public static void GLOBAL_SET(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int globalIndex = (int) ins.operand(0);

        emitJvmToLong(asm, ctx.globalTypes().get(globalIndex));
        asm.iconst(globalIndex);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.WRITE_GLOBAL);
    }

    public static void TABLE_GET(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_GET);
    }

    public static void TABLE_SET(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_SET);
    }

    public static void TABLE_SIZE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_SIZE);
    }

    public static void TABLE_GROW(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_GROW);
    }

    public static void TABLE_FILL(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_FILL);
    }

    public static void TABLE_COPY(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.iconst((int) ins.operand(1));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_COPY);
    }

    public static void TABLE_INIT(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.iconst((int) ins.operand(1));
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_INIT);
    }

    public static void MEMORY_INIT(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_INIT);
    }

    public static void MEMORY_COPY(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_COPY);
    }

    public static void MEMORY_FILL(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_FILL);
    }

    public static void MEMORY_GROW(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_GROW);
    }

    public static void MEMORY_SIZE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_PAGES);
    }

    public static void DATA_DROP(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_DROP);
    }

    public static void I32_ADD(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IADD);
    }

    public static void I32_AND(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_CONST(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.iconst((int) ins.operand(0));
    }

    public static void I32_MUL(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IMUL);
    }

    public static void I32_OR(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IOR);
    }

    public static void I32_SHL(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISHL);
    }

    public static void I32_SHR_S(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISHR);
    }

    public static void I32_SHR_U(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IUSHR);
    }

    public static void I32_SUB(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.ISUB);
    }

    public static void I32_WRAP_I64(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
    }

    public static void I32_XOR(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.IXOR);
    }

    public static void I64_ADD(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LADD);
    }

    public static void I64_AND(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LAND);
    }

    public static void I64_CONST(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.lconst(ins.operand(0));
    }

    public static void I64_EXTEND_I32_S(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_MUL(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LMUL);
    }

    public static void I64_OR(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LOR);
    }

    public static void I64_SHL(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHL);
    }

    public static void I64_SHR_S(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHR);
    }

    public static void I64_SHR_U(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LUSHR);
    }

    public static void I64_SUB(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LSUB);
    }

    public static void I64_XOR(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.LXOR);
    }

    public static void F32_ADD(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FADD);
    }

    public static void F32_CONST(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.fconst(intBitsToFloat((int) ins.operand(0)));
    }

    public static void F32_DEMOTE_F64(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.D2F);
    }

    public static void F32_DIV(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FDIV);
    }

    public static void F32_MUL(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FMUL);
    }

    public static void F32_NEG(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FNEG);
    }

    public static void F32_SUB(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.FSUB);
    }

    public static void F64_ADD(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DADD);
    }

    public static void F64_CONST(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.dconst(longBitsToDouble(ins.operand(0)));
    }

    public static void F64_DIV(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DDIV);
    }

    public static void F64_MUL(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DMUL);
    }

    public static void F64_NEG(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DNEG);
    }

    public static void F64_PROMOTE_F32(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.F2D);
    }

    public static void F64_SUB(Context ctx, CompilerInstruction ins, MethodVisitor asm) {
        asm.visitInsn(Opcodes.DSUB);
    }

    public static void I32_LOAD(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_READ_INT);
    }

    public static void I32_LOAD8_S(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_READ_BYTE);
    }

    public static void I32_LOAD8_U(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.iconst(0xFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_LOAD16_S(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_READ_SHORT);
    }

    public static void I32_LOAD16_U(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.iconst(0xFFFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void F32_LOAD(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_READ_FLOAT);
    }

    public static void I64_LOAD(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_READ_LONG);
    }

    public static void I64_LOAD8_S(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD8_S(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD8_U(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD8_U(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_S(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD16_S(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_U(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD16_U(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_S(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_U(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        I32_LOAD(ctx, ins, asm);
        asm.visitInsn(Opcodes.I2L);
        asm.lconst(0xFFFF_FFFFL);
        asm.visitInsn(Opcodes.LAND);
    }

    public static void F64_LOAD(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_READ_DOUBLE);
    }

    public static void I32_STORE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_INT);
    }

    public static void I32_STORE8(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.I2B);
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_BYTE);
    }

    public static void I32_STORE16(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.I2S);
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_SHORT);
    }

    public static void F32_STORE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_FLOAT);
    }

    public static void I64_STORE8(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        I32_STORE8(ctx, ins, asm);
    }

    public static void I64_STORE16(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        I32_STORE16(ctx, ins, asm);
    }

    public static void I64_STORE32(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_INT);
    }

    public static void I64_STORE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_LONG);
    }

    public static void F64_STORE(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_WRITE_DOUBLE);
    }

    public static void ATOMIC_INT_READ_BYTE(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_BYTE_READ);
    }

    public static void ATOMIC_INT_READ_SHORT(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_SHORT_READ);
    }

    public static void ATOMIC_INT_READ(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_READ);
    }

    public static void ATOMIC_LONG_READ(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_READ);
    }

    public static void ATOMIC_LONG_READ_BYTE(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_BYTE_READ);
    }

    public static void ATOMIC_LONG_READ_SHORT(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_SHORT_READ);
    }

    public static void ATOMIC_LONG_READ_INT(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_INT_READ);
    }

    public static void ATOMIC_INT_STORE(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_WRITE);
    }

    public static void ATOMIC_INT_STORE_BYTE(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_BYTE_WRITE);
    }

    public static void ATOMIC_INT_STORE_SHORT(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_SHORT_WRITE);
    }

    public static void ATOMIC_LONG_STORE(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_WRITE);
    }

    public static void ATOMIC_LONG_STORE_BYTE(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_BYTE_WRITE);
    }

    public static void ATOMIC_LONG_STORE_SHORT(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_SHORT_WRITE);
    }

    public static void ATOMIC_LONG_STORE_INT(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.visitInsn(Opcodes.L2I);
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_INT_WRITE);
    }

    public static void ATOMIC_INT_RMW_ADD(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_ADD);
    }

    public static void ATOMIC_INT_RMW_SUB(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_SUB);
    }

    public static void ATOMIC_INT_RMW_AND(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_AND);
    }

    public static void ATOMIC_INT_RMW_OR(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_OR);
    }

    public static void ATOMIC_INT_RMW_XOR(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_XOR);
    }

    public static void ATOMIC_INT_RMW_XCHG(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_XCHG);
    }

    public static void ATOMIC_INT_RMW_CMPXCHG(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW_CMPXCHG);
    }

    public static void ATOMIC_INT_RMW8_ADD_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_ADD_U);
    }

    public static void ATOMIC_INT_RMW8_SUB_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_SUB_U);
    }

    public static void ATOMIC_INT_RMW8_AND_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_AND_U);
    }

    public static void ATOMIC_INT_RMW8_OR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_OR_U);
    }

    public static void ATOMIC_INT_RMW8_XOR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_XOR_U);
    }

    public static void ATOMIC_INT_RMW8_XCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_XCHG_U);
    }

    public static void ATOMIC_INT_RMW8_CMPXCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW8_CMPXCHG_U);
    }

    public static void ATOMIC_INT_RMW16_ADD_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_ADD_U);
    }

    public static void ATOMIC_INT_RMW16_SUB_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_SUB_U);
    }

    public static void ATOMIC_INT_RMW16_AND_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_AND_U);
    }

    public static void ATOMIC_INT_RMW16_OR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_OR_U);
    }

    public static void ATOMIC_INT_RMW16_XOR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_XOR_U);
    }

    public static void ATOMIC_INT_RMW16_XCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_XCHG_U);
    }

    public static void ATOMIC_INT_RMW16_CMPXCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_INT_RMW16_CMPXCHG_U);
    }

    // I64 variants
    public static void ATOMIC_LONG_RMW_ADD(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_ADD);
    }

    public static void ATOMIC_LONG_RMW_SUB(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_SUB);
    }

    public static void ATOMIC_LONG_RMW_AND(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_AND);
    }

    public static void ATOMIC_LONG_RMW_OR(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_OR);
    }

    public static void ATOMIC_LONG_RMW_XOR(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_XOR);
    }

    public static void ATOMIC_LONG_RMW_XCHG(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_XCHG);
    }

    public static void ATOMIC_LONG_RMW_CMPXCHG(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW_CMPXCHG);
    }

    public static void ATOMIC_LONG_RMW8_ADD_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_ADD_U);
    }

    public static void ATOMIC_LONG_RMW8_SUB_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_SUB_U);
    }

    public static void ATOMIC_LONG_RMW8_AND_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_AND_U);
    }

    public static void ATOMIC_LONG_RMW8_OR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_OR_U);
    }

    public static void ATOMIC_LONG_RMW8_XOR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_XOR_U);
    }

    public static void ATOMIC_LONG_RMW8_XCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_XCHG_U);
    }

    public static void ATOMIC_LONG_RMW8_CMPXCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW8_CMPXCHG_U);
    }

    public static void ATOMIC_LONG_RMW16_ADD_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_ADD_U);
    }

    public static void ATOMIC_LONG_RMW16_SUB_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_SUB_U);
    }

    public static void ATOMIC_LONG_RMW16_AND_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_AND_U);
    }

    public static void ATOMIC_LONG_RMW16_OR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_OR_U);
    }

    public static void ATOMIC_LONG_RMW16_XOR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_XOR_U);
    }

    public static void ATOMIC_LONG_RMW16_XCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_XCHG_U);
    }

    public static void ATOMIC_LONG_RMW16_CMPXCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW16_CMPXCHG_U);
    }

    public static void ATOMIC_LONG_RMW32_ADD_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_ADD_U);
    }

    public static void ATOMIC_LONG_RMW32_SUB_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_SUB_U);
    }

    public static void ATOMIC_LONG_RMW32_AND_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_AND_U);
    }

    public static void ATOMIC_LONG_RMW32_OR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_OR_U);
    }

    public static void ATOMIC_LONG_RMW32_XOR_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_XOR_U);
    }

    public static void ATOMIC_LONG_RMW32_XCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_XCHG_U);
    }

    public static void ATOMIC_LONG_RMW32_CMPXCHG_U(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_LONG_RMW32_CMPXCHG_U);
    }

    // Wait/Notify
    public static void MEM_ATOMIC_WAIT32(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_WAIT32);
    }

    public static void MEM_ATOMIC_WAIT64(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_WAIT64);
    }

    public static void MEM_ATOMIC_NOTIFY(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        emitLoadOrStore(ctx, ins, asm, ShadedRefs.MEMORY_ATOMIC_NOTIFY);
    }

    private static void emitLoadOrStore(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm, Method method) {
        long offset = ins.operand(1);

        if (offset < 0 || offset >= Integer.MAX_VALUE) {
            emitInvokeStatic(asm, ShadedRefs.THROW_OUT_OF_BOUNDS_MEMORY_ACCESS);
            asm.athrow();
        }

        asm.iconst((int) offset);
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, method);
    }

    private static void emitUnboxResult(InstructionAdapter asm, Context ctx, List<ValType> types) {
        emitUnboxResult(asm, types, ctx.tempSlot());
    }

    private static void emitUnboxResult(InstructionAdapter asm, List<ValType> types, int tempSlot) {
        asm.store(tempSlot, OBJECT_TYPE);
        for (int i = 0; i < types.size(); i++) {
            asm.load(tempSlot, OBJECT_TYPE);
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
    public static BytecodeEmitter intrinsify(CompilerOpCode opcode, Class<?> staticHelpers) {
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

    public static void THROW(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        int tagNumber = (int) ins.operand(0);
        var type = ctx.tagFunctionType(tagNumber);

        // emmit:
        // call createWasmException(long[] args, int tagNumber, Instance instance)
        emitBoxValuesOnStack(ctx, asm, type.params());
        asm.iconst(tagNumber);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.CREATE_WASM_EXCEPTION);
        asm.athrow();
    }

    public static void THROW_REF(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        // The exception reference is already on the stack as an integer
        // Get the instance and retrieve the exception
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        asm.swap(); // Swap instance and exception reference
        emitInvokeVirtual(asm, ShadedRefs.INSTANCE_GET_EXCEPTION);
        asm.athrow();
    }

    public static void CATCH_START(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.store(ctx.tempSlot(), OBJECT_TYPE);
    }

    public static void CATCH_END(Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        asm.load(ctx.tempSlot(), OBJECT_TYPE);
        asm.athrow();
    }

    public static void CATCH_UNBOX_PARAMS(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        var tag = (int) ins.operand(0);
        // Get the tag type to know what
        // parameter types to unbox
        var tagFuncType = ctx.tagFunctionType(tag);
        if (!tagFuncType.params().isEmpty()) {
            // unbox the exception args
            asm.load(ctx.tempSlot(), OBJECT_TYPE);
            asm.invokevirtual(
                    getInternalName(WasmException.class),
                    "args",
                    getMethodDescriptor(getType(long[].class)),
                    false);

            // Store the array in a local variable
            // Unbox each argument from the
            // long[] array and push onto stack
            emitUnboxResult(asm, tagFuncType.params(), ctx.tempSlot() + 1);
        }
    }

    public static void CATCH_COMPARE_TAG(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        var tag = (int) ins.operand(0);
        // Compare tag
        asm.load(ctx.tempSlot(), OBJECT_TYPE);
        asm.iconst(tag);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, EXCEPTION_MATCHES);
    }

    public static void CATCH_REGISTER_EXCEPTION(
            Context ctx, CompilerInstruction ins, InstructionAdapter asm) {
        // Register exception and push its
        // index
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        asm.load(ctx.tempSlot(), OBJECT_TYPE);
        asm.invokevirtual(
                getInternalName(Instance.class),
                "registerException",
                getMethodDescriptor(INT_TYPE, getType(WasmException.class)),
                false);
    }
}
