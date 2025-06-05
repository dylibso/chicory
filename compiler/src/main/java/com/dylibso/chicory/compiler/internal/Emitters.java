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
import static com.dylibso.chicory.compiler.internal.OpCodeEmitters.EMITTERS;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.CHECK_INTERRUPTION;
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
import com.dylibso.chicory.runtime.WasmException;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.CatchOpCode;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;

final class Emitters {
    public static final long[] EMPTY = new long[0];

    private Emitters() {}

    public static Emitter create(OpCode opcode, long... operands) {
        var operandEmitter = EMITTERS.get(opcode);
        return context -> operandEmitter.emit(context, operands);
    }

    public static void TRAP(Context ctx) {
        var asm = ctx.asm();
        emitInvokeStatic(asm, ShadedRefs.THROW_TRAP_EXCEPTION);
        asm.athrow();
    }

    public static ValType valType(long id, Context ctx) {
        return ValType.builder().fromId(id).build(ctx::type);
    }

    public static Emitter DROP_KEEP(long[] operands) {
        return ctx -> {
            var asm = ctx.asm();

            int keepStart = (int) operands[0] + 1;

            // save result values
            int slot = ctx.tempSlot();
            for (int i = operands.length - 1; i >= keepStart; i--) {
                var type = valType(operands[i], ctx);
                asm.store(slot, asmType(type));
                slot += slotCount(type);
            }

            // drop intervening values
            for (int i = keepStart - 1; i >= 1; i--) {
                emitPop(asm, valType(operands[i], ctx));
            }

            // restore result values
            for (int i = keepStart; i < operands.length; i++) {
                var type = valType(operands[i], ctx);
                slot -= slotCount(type);
                asm.load(slot, asmType(type));
            }
        };
    }

    public static void RETURN(Context ctx) {
        var asm = ctx.asm();
        if (ctx.getType().returns().size() > 1) {
            asm.invokestatic(
                    ctx.internalClassName(),
                    valueMethodName(ctx.getType().returns()),
                    valueMethodType(ctx.getType().returns()).toMethodDescriptorString(),
                    false);
        }
        asm.areturn(getType(jvmReturnType(ctx.getType())));
    }

    public static void DROP(Context ctx, long[] operands) {
        var asm = ctx.asm();
        emitPop(asm, valType(operands[0], ctx));
    }

    public static void ELEM_DROP(Context ctx, long[] operands) {
        var asm = ctx.asm();
        int index = (int) operands[0];
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        asm.iconst(index);
        asm.aconst(null);
        emitInvokeVirtual(asm, ShadedRefs.INSTANCE_SET_ELEMENT);
    }

    public static void SELECT(Context ctx, long[] operands) {
        var asm = ctx.asm();
        var type = valType(operands[0], ctx);
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

    public static void CALL(Context ctx, long[] operands) {
        var asm = ctx.asm();
        int funcId = (int) operands[0];
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

    public static void CALL_INDIRECT(Context ctx, long[] operands) {
        var asm = ctx.asm();
        int typeId = (int) operands[0];
        int tableIdx = (int) operands[1];
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

    public static void REF_FUNC(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
    }

    public static void REF_NULL(Context ctx) {
        var asm = ctx.asm();
        asm.iconst(REF_NULL_VALUE);
    }

    public static void REF_IS_NULL(Context ctx) {
        var asm = ctx.asm();
        emitInvokeStatic(asm, ShadedRefs.REF_IS_NULL);
    }

    public static void LOCAL_GET(Context ctx, long[] operands) {
        var asm = ctx.asm();
        var loadIndex = (int) operands[0];
        var localType = localType(ctx.getType(), ctx.getBody(), loadIndex);
        asm.load(ctx.localSlotIndex(loadIndex), asmType(localType));
    }

    public static void LOCAL_SET(Context ctx, long[] operands) {
        var asm = ctx.asm();
        int index = (int) operands[0];
        var localType = localType(ctx.getType(), ctx.getBody(), index);
        asm.store(ctx.localSlotIndex(index), asmType(localType));
    }

    public static void LOCAL_TEE(Context ctx, long[] operands) {
        var asm = ctx.asm();
        if (slotCount(valType(operands[1], ctx)) == 1) {
            asm.dup();
        } else {
            asm.dup2();
        }

        LOCAL_SET(ctx, operands);
    }

    public static void GLOBAL_GET(Context ctx, long[] operands) {
        var asm = ctx.asm();
        int globalIndex = (int) operands[0];

        asm.iconst(globalIndex);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.READ_GLOBAL);

        emitLongToJvm(asm, ctx.globalTypes().get(globalIndex));
    }

    public static void GLOBAL_SET(Context ctx, long[] operands) {
        var asm = ctx.asm();
        int globalIndex = (int) operands[0];

        emitJvmToLong(asm, ctx.globalTypes().get(globalIndex));
        asm.iconst(globalIndex);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.WRITE_GLOBAL);
    }

    public static void TABLE_GET(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_GET);
    }

    public static void TABLE_SET(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_SET);
    }

    public static void TABLE_SIZE(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_SIZE);
    }

    public static void TABLE_GROW(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_GROW);
    }

    public static void TABLE_FILL(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_FILL);
    }

    public static void TABLE_COPY(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.iconst((int) operands[1]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_COPY);
    }

    public static void TABLE_INIT(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.iconst((int) operands[1]);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.TABLE_INIT);
    }

    public static void MEMORY_INIT(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_INIT);
    }

    public static void MEMORY_COPY(Context ctx) {
        var asm = ctx.asm();
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_COPY);
    }

    public static void MEMORY_FILL(Context ctx) {
        var asm = ctx.asm();
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_FILL);
    }

    public static void MEMORY_GROW(Context ctx) {
        var asm = ctx.asm();
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_GROW);
    }

    public static void MEMORY_SIZE(Context ctx) {
        var asm = ctx.asm();
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_PAGES);
    }

    public static void DATA_DROP(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.MEMORY_DROP);
    }

    public static void I32_ADD(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.IADD);
    }

    public static void I32_AND(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_CONST(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.iconst((int) operands[0]);
    }

    public static void I32_MUL(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.IMUL);
    }

    public static void I32_OR(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.IOR);
    }

    public static void I32_SHL(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.ISHL);
    }

    public static void I32_SHR_S(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.ISHR);
    }

    public static void I32_SHR_U(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.IUSHR);
    }

    public static void I32_SUB(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.ISUB);
    }

    public static void I32_WRAP_I64(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
    }

    public static void I32_XOR(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.IXOR);
    }

    public static void I64_ADD(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.LADD);
    }

    public static void I64_AND(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.LAND);
    }

    public static void I64_CONST(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.lconst(operands[0]);
    }

    public static void I64_EXTEND_I32_S(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_MUL(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.LMUL);
    }

    public static void I64_OR(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.LOR);
    }

    public static void I64_SHL(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHL);
    }

    public static void I64_SHR_S(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LSHR);
    }

    public static void I64_SHR_U(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
        asm.visitInsn(Opcodes.LUSHR);
    }

    public static void I64_SUB(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.LSUB);
    }

    public static void I64_XOR(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.LXOR);
    }

    public static void F32_ADD(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.FADD);
    }

    public static void F32_CONST(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.fconst(intBitsToFloat((int) operands[0]));
    }

    public static void F32_DEMOTE_F64(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.D2F);
    }

    public static void F32_DIV(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.FDIV);
    }

    public static void F32_MUL(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.FMUL);
    }

    public static void F32_NEG(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.FNEG);
    }

    public static void F32_SUB(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.FSUB);
    }

    public static void F64_ADD(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.DADD);
    }

    public static void F64_CONST(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.dconst(longBitsToDouble(operands[0]));
    }

    public static void F64_DIV(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.DDIV);
    }

    public static void F64_MUL(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.DMUL);
    }

    public static void F64_NEG(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.DNEG);
    }

    public static void F64_PROMOTE_F32(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.F2D);
    }

    public static void F64_SUB(Context ctx) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.DSUB);
    }

    public static void I32_LOAD(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_READ_INT);
    }

    public static void I32_LOAD8_S(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_READ_BYTE);
    }

    public static void I32_LOAD8_U(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD8_S(ctx, operands);
        asm.iconst(0xFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void I32_LOAD16_S(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_READ_SHORT);
    }

    public static void I32_LOAD16_U(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD16_S(ctx, operands);
        asm.iconst(0xFFFF);
        asm.visitInsn(Opcodes.IAND);
    }

    public static void F32_LOAD(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_READ_FLOAT);
    }

    public static void I64_LOAD(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_READ_LONG);
    }

    public static void I64_LOAD8_S(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD8_S(ctx, operands);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD8_U(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD8_U(ctx, operands);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_S(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD16_S(ctx, operands);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD16_U(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD16_U(ctx, operands);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_S(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD(ctx, operands);
        asm.visitInsn(Opcodes.I2L);
    }

    public static void I64_LOAD32_U(Context ctx, long[] operands) {
        var asm = ctx.asm();
        I32_LOAD(ctx, operands);
        asm.visitInsn(Opcodes.I2L);
        asm.lconst(0xFFFF_FFFFL);
        asm.visitInsn(Opcodes.LAND);
    }

    public static void F64_LOAD(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_READ_DOUBLE);
    }

    public static void I32_STORE(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_INT);
    }

    public static void I32_STORE8(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.I2B);
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_BYTE);
    }

    public static void I32_STORE16(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.I2S);
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_SHORT);
    }

    public static void F32_STORE(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_FLOAT);
    }

    public static void I64_STORE8(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
        I32_STORE8(ctx, operands);
    }

    public static void I64_STORE16(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
        I32_STORE16(ctx, operands);
    }

    public static void I64_STORE32(Context ctx, long[] operands) {
        var asm = ctx.asm();
        asm.visitInsn(Opcodes.L2I);
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_INT);
    }

    public static void I64_STORE(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_LONG);
    }

    public static void F64_STORE(Context ctx, long[] operands) {
        emitLoadOrStore(ctx, operands, ShadedRefs.MEMORY_WRITE_DOUBLE);
    }

    private static void emitLoadOrStore(Context ctx, long[] operands, Method method) {
        var asm = ctx.asm();
        long offset = operands[1];

        if (offset < 0 || offset >= Integer.MAX_VALUE) {
            emitInvokeStatic(asm, ShadedRefs.THROW_OUT_OF_BOUNDS_MEMORY_ACCESS);
            asm.athrow();
        }

        asm.iconst((int) offset);
        asm.load(ctx.memorySlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, method);
    }

    private static void emitUnboxResult(InstructionAdapter asm, Context ctx, List<ValType> types) {
        asm.store(ctx.tempSlot(), OBJECT_TYPE);
        for (int i = 0; i < types.size(); i++) {
            asm.load(ctx.tempSlot(), OBJECT_TYPE);
            asm.iconst(i);
            asm.aload(LONG_TYPE);
            emitLongToJvm(asm, types.get(i));
        }
    }

    public static void THROW(Context ctx, long[] operands) {
        var asm = ctx.asm();

        int tagNumber = (int) operands[0];
        var type = ctx.getTagFunctionType(tagNumber);

        // emmit:
        // call createWasmException(long[] args, int tagNumber, Instance instance)
        emitBoxValuesOnStack(ctx, asm, type.params());
        asm.iconst(tagNumber);
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        emitInvokeStatic(asm, ShadedRefs.CREATE_WASM_EXCEPTION);
        asm.athrow();
    }

    public static void THROW_REF(Context ctx) {
        var asm = ctx.asm();
        // The exception reference is already on the stack as an integer
        // Get the instance and retrieve the exception
        asm.load(ctx.instanceSlot(), OBJECT_TYPE);
        asm.swap(); // Swap instance and exception reference
        emitInvokeVirtual(asm, ShadedRefs.INSTANCE_GET_EXCEPTION);
        asm.athrow();
    }

    public static class TryCatchBlock {
        final AnnotatedInstruction ins;
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label after = new Label();

        public TryCatchBlock(AnnotatedInstruction ins) {
            this.ins = ins;
        }
    }

    public static Emitter TRY_CATCH_BLOCK(Label start, Label endLabel, Label handlerLabel) {
        return (ctx) -> {
            var asm = ctx.asm();
            asm.visitTryCatchBlock(
                    start, endLabel, handlerLabel, getInternalName(WasmException.class));
        };
    }

    public static Emitter CATCH_CONDITION(
            CatchOpCode.Catch cond, Label resolvedLabel, Label afterCatchLabel) {

        return (ctx) -> {
            var asm = ctx.asm();
            switch (cond.opcode()) {
                case CATCH:
                case CATCH_REF:
                    // Compare tag
                    asm.load(ctx.tempSlot(), OBJECT_TYPE);
                    asm.iconst(cond.tag());
                    asm.load(ctx.instanceSlot(), OBJECT_TYPE);
                    emitInvokeStatic(asm, EXCEPTION_MATCHES);
                    asm.ifeq(afterCatchLabel);

                    // Get the tag type to know what
                    // parameter types to unbox
                    var tagFuncType = ctx.getTagFunctionType(cond.tag());
                    if (!tagFuncType.params().isEmpty()) {
                        // unbox the exception args
                        asm.load(ctx.tempSlot(), OBJECT_TYPE);
                        asm.invokevirtual(
                                getInternalName(WasmException.class),
                                "args",
                                getMethodDescriptor(getType(long[].class)),
                                false);

                        // Store the array in a local
                        // variable
                        var argsSlot = ctx.tempSlot() + 1;
                        asm.store(argsSlot, OBJECT_TYPE);

                        // Unbox each argument from the
                        // long[] array and push onto stack
                        for (int j = 0; j < tagFuncType.params().size(); j++) {
                            var param = tagFuncType.params().get(j);
                            asm.load(argsSlot, OBJECT_TYPE);
                            asm.iconst(j);
                            asm.aload(LONG_TYPE);
                            emitLongToJvm(asm, param);
                        }
                    }

                    if (cond.opcode() == CatchOpCode.CATCH_REF) {
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
                    // Note: operand index is offset by 3
                    // since first 3 operands
                    // are internal labels
                    asm.goTo(resolvedLabel);

                    break;

                case CATCH_ALL:
                    // Always matches, no tag comparison
                    // needed
                    asm.goTo(resolvedLabel);
                    break;

                case CATCH_ALL_REF:
                    // Always matches, register exception
                    // and push its index
                    asm.load(ctx.instanceSlot(), OBJECT_TYPE);
                    asm.load(ctx.tempSlot(), OBJECT_TYPE);
                    asm.invokevirtual(
                            getInternalName(Instance.class),
                            "registerException",
                            getMethodDescriptor(INT_TYPE, getType(WasmException.class)),
                            false);
                    asm.goTo(resolvedLabel);
                    break;
            }

            // Mark the label for next check
            asm.mark(afterCatchLabel);
        };
    }

    public static Emitter LABEL(Label label) {
        return (ctx) -> {
            var asm = ctx.asm();
            if (label != null) {
                label.info = Boolean.TRUE; // Mark label as used.
                asm.mark(label);
            }
        };
    }

    public static Emitter GOTO(Label targetLabel) {
        return (ctx) -> {
            var asm = ctx.asm();

            if (targetLabel.info != null) {
                emitInvokeStatic(asm, CHECK_INTERRUPTION);
            }
            asm.goTo(targetLabel);
        };
    }

    public static Emitter IFEQ(Label targetLabel) {
        return (ctx) -> {
            var asm = ctx.asm();
            if (targetLabel.info != null) {
                throw new ChicoryException("Unexpected backward jump");
            }
            asm.ifeq(targetLabel);
        };
    }

    public static Emitter IFNE(Label targetLabel) {
        return (ctx) -> {
            var asm = ctx.asm();
            if (targetLabel.info != null) {
                Label skip = new Label();
                asm.ifeq(skip);
                emitInvokeStatic(asm, CHECK_INTERRUPTION);
                asm.goTo(targetLabel);
                asm.mark(skip);

            } else {
                asm.ifne(targetLabel);
            }
        };
    }

    public static Emitter SWITCH(Label[] labels) {
        return (ctx) -> {
            var asm = ctx.asm();

            if (Arrays.stream(labels).anyMatch(l -> l.info != null)) {
                emitInvokeStatic(asm, CHECK_INTERRUPTION);
            }
            // table switch using the last entry of the table as the default
            Label[] table = new Label[labels.length - 1];
            for (int i = 0; i < table.length; i++) {
                table[i] = labels[i];
            }
            Label defaultLabel = labels[table.length];
            asm.tableswitch(0, table.length - 1, defaultLabel, table);
        };
    }
}
