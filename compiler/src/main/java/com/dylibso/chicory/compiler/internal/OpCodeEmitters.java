package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeStatic;

import com.dylibso.chicory.runtime.OpCodeIdentifier;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map;

final class OpCodeEmitters {

    interface OpCodeEmitter {
        void emit(Context context, long... operands);
    }

    public static final Map<OpCode, OpCodeEmitter> EMITTERS =
            builder()
                    // ====== Misc ======
                    .intrinsic(OpCode.RETURN, Emitters::RETURN)
                    .intrinsic(OpCode.DROP, Emitters::DROP)
                    .intrinsic(OpCode.ELEM_DROP, Emitters::ELEM_DROP)
                    .intrinsic(OpCode.SELECT, Emitters::SELECT)

                    // ====== Control Flow ======
                    .intrinsic(OpCode.CALL, Emitters::CALL)
                    .intrinsic(OpCode.CALL_INDIRECT, Emitters::CALL_INDIRECT)

                    // ====== Exception Handling ======
                    .intrinsic(OpCode.THROW, Emitters::THROW)
                    .intrinsic(OpCode.THROW_REF, Emitters::THROW_REF)

                    // ====== References ======
                    .intrinsic(OpCode.REF_FUNC, Emitters::REF_FUNC)
                    .intrinsic(OpCode.REF_NULL, Emitters::REF_NULL)
                    .intrinsic(OpCode.REF_IS_NULL, Emitters::REF_IS_NULL)

                    // ====== Locals & Globals ======
                    .intrinsic(OpCode.LOCAL_GET, Emitters::LOCAL_GET)
                    .intrinsic(OpCode.LOCAL_SET, Emitters::LOCAL_SET)
                    .intrinsic(OpCode.LOCAL_TEE, Emitters::LOCAL_TEE)
                    .intrinsic(OpCode.GLOBAL_GET, Emitters::GLOBAL_GET)
                    .intrinsic(OpCode.GLOBAL_SET, Emitters::GLOBAL_SET)

                    // ====== Tables ======
                    .intrinsic(OpCode.TABLE_GET, Emitters::TABLE_GET)
                    .intrinsic(OpCode.TABLE_SET, Emitters::TABLE_SET)
                    .intrinsic(OpCode.TABLE_SIZE, Emitters::TABLE_SIZE)
                    .intrinsic(OpCode.TABLE_GROW, Emitters::TABLE_GROW)
                    .intrinsic(OpCode.TABLE_FILL, Emitters::TABLE_FILL)
                    .intrinsic(OpCode.TABLE_COPY, Emitters::TABLE_COPY)
                    .intrinsic(OpCode.TABLE_INIT, Emitters::TABLE_INIT)

                    // ====== Memory ======
                    .intrinsic(OpCode.MEMORY_INIT, Emitters::MEMORY_INIT)
                    .intrinsic(OpCode.MEMORY_COPY, Emitters::MEMORY_COPY)
                    .intrinsic(OpCode.MEMORY_FILL, Emitters::MEMORY_FILL)
                    .intrinsic(OpCode.MEMORY_GROW, Emitters::MEMORY_GROW)
                    .intrinsic(OpCode.MEMORY_SIZE, Emitters::MEMORY_SIZE)
                    .intrinsic(OpCode.DATA_DROP, Emitters::DATA_DROP)

                    // ====== Load & Store ======
                    .intrinsic(OpCode.I32_LOAD, Emitters::I32_LOAD)
                    .intrinsic(OpCode.I32_LOAD8_S, Emitters::I32_LOAD8_S)
                    .intrinsic(OpCode.I32_LOAD8_U, Emitters::I32_LOAD8_U)
                    .intrinsic(OpCode.I32_LOAD16_S, Emitters::I32_LOAD16_S)
                    .intrinsic(OpCode.I32_LOAD16_U, Emitters::I32_LOAD16_U)
                    .intrinsic(OpCode.I64_LOAD, Emitters::I64_LOAD)
                    .intrinsic(OpCode.I64_LOAD8_S, Emitters::I64_LOAD8_S)
                    .intrinsic(OpCode.I64_LOAD8_U, Emitters::I64_LOAD8_U)
                    .intrinsic(OpCode.I64_LOAD16_S, Emitters::I64_LOAD16_S)
                    .intrinsic(OpCode.I64_LOAD16_U, Emitters::I64_LOAD16_U)
                    .intrinsic(OpCode.I64_LOAD32_S, Emitters::I64_LOAD32_S)
                    .intrinsic(OpCode.I64_LOAD32_U, Emitters::I64_LOAD32_U)
                    .intrinsic(OpCode.F32_LOAD, Emitters::F32_LOAD)
                    .intrinsic(OpCode.F64_LOAD, Emitters::F64_LOAD)
                    .intrinsic(OpCode.I32_STORE, Emitters::I32_STORE)
                    .intrinsic(OpCode.I32_STORE8, Emitters::I32_STORE8)
                    .intrinsic(OpCode.I32_STORE16, Emitters::I32_STORE16)
                    .intrinsic(OpCode.I64_STORE, Emitters::I64_STORE)
                    .intrinsic(OpCode.I64_STORE8, Emitters::I64_STORE8)
                    .intrinsic(OpCode.I64_STORE16, Emitters::I64_STORE16)
                    .intrinsic(OpCode.I64_STORE32, Emitters::I64_STORE32)
                    .intrinsic(OpCode.F32_STORE, Emitters::F32_STORE)
                    .intrinsic(OpCode.F64_STORE, Emitters::F64_STORE)

                    // ====== I32 ======
                    .intrinsic(OpCode.I32_ADD, Emitters::I32_ADD)
                    .intrinsic(OpCode.I32_AND, Emitters::I32_AND)
                    .shared(OpCode.I32_CLZ, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_CONST, Emitters::I32_CONST)
                    .shared(OpCode.I32_CTZ, OpcodeImpl.class)
                    .shared(OpCode.I32_DIV_S, OpcodeImpl.class)
                    .shared(OpCode.I32_DIV_U, OpcodeImpl.class)
                    .shared(OpCode.I32_EQ, OpcodeImpl.class)
                    .shared(OpCode.I32_EQZ, OpcodeImpl.class)
                    .shared(OpCode.I32_EXTEND_8_S, OpcodeImpl.class)
                    .shared(OpCode.I32_EXTEND_16_S, OpcodeImpl.class)
                    .shared(OpCode.I32_GE_S, OpcodeImpl.class)
                    .shared(OpCode.I32_GE_U, OpcodeImpl.class)
                    .shared(OpCode.I32_GT_S, OpcodeImpl.class)
                    .shared(OpCode.I32_GT_U, OpcodeImpl.class)
                    .shared(OpCode.I32_LE_S, OpcodeImpl.class)
                    .shared(OpCode.I32_LE_U, OpcodeImpl.class)
                    .shared(OpCode.I32_LT_S, OpcodeImpl.class)
                    .shared(OpCode.I32_LT_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_MUL, Emitters::I32_MUL)
                    .shared(OpCode.I32_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_OR, Emitters::I32_OR)
                    .shared(OpCode.I32_POPCNT, OpcodeImpl.class)
                    .shared(OpCode.I32_REINTERPRET_F32, OpcodeImpl.class)
                    .shared(OpCode.I32_REM_S, OpcodeImpl.class)
                    .shared(OpCode.I32_REM_U, OpcodeImpl.class)
                    .shared(OpCode.I32_ROTL, OpcodeImpl.class)
                    .shared(OpCode.I32_ROTR, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_SHL, Emitters::I32_SHL)
                    .intrinsic(OpCode.I32_SHR_S, Emitters::I32_SHR_S)
                    .intrinsic(OpCode.I32_SHR_U, Emitters::I32_SHR_U)
                    .intrinsic(OpCode.I32_SUB, Emitters::I32_SUB)
                    .shared(OpCode.I32_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I32_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I32_WRAP_I64, Emitters::I32_WRAP_I64)
                    .intrinsic(OpCode.I32_XOR, Emitters::I32_XOR)

                    // ====== I64 ======
                    .intrinsic(OpCode.I64_ADD, Emitters::I64_ADD)
                    .intrinsic(OpCode.I64_AND, Emitters::I64_AND)
                    .shared(OpCode.I64_CLZ, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_CONST, Emitters::I64_CONST)
                    .shared(OpCode.I64_CTZ, OpcodeImpl.class)
                    .shared(OpCode.I64_DIV_S, OpcodeImpl.class)
                    .shared(OpCode.I64_DIV_U, OpcodeImpl.class)
                    .shared(OpCode.I64_EQ, OpcodeImpl.class)
                    .shared(OpCode.I64_EQZ, OpcodeImpl.class)
                    .shared(OpCode.I64_EXTEND_8_S, OpcodeImpl.class)
                    .shared(OpCode.I64_EXTEND_16_S, OpcodeImpl.class)
                    .shared(OpCode.I64_EXTEND_32_S, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_EXTEND_I32_S, Emitters::I64_EXTEND_I32_S)
                    .shared(OpCode.I64_EXTEND_I32_U, OpcodeImpl.class)
                    .shared(OpCode.I64_GE_S, OpcodeImpl.class)
                    .shared(OpCode.I64_GE_U, OpcodeImpl.class)
                    .shared(OpCode.I64_GT_S, OpcodeImpl.class)
                    .shared(OpCode.I64_GT_U, OpcodeImpl.class)
                    .shared(OpCode.I64_LE_S, OpcodeImpl.class)
                    .shared(OpCode.I64_LE_U, OpcodeImpl.class)
                    .shared(OpCode.I64_LT_S, OpcodeImpl.class)
                    .shared(OpCode.I64_LT_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_MUL, Emitters::I64_MUL)
                    .shared(OpCode.I64_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_OR, Emitters::I64_OR)
                    .shared(OpCode.I64_POPCNT, OpcodeImpl.class)
                    .shared(OpCode.I64_REM_S, OpcodeImpl.class)
                    .shared(OpCode.I64_REM_U, OpcodeImpl.class)
                    .shared(OpCode.I64_ROTL, OpcodeImpl.class)
                    .shared(OpCode.I64_ROTR, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_SHL, Emitters::I64_SHL)
                    .intrinsic(OpCode.I64_SHR_S, Emitters::I64_SHR_S)
                    .intrinsic(OpCode.I64_SHR_U, Emitters::I64_SHR_U)
                    .intrinsic(OpCode.I64_SUB, Emitters::I64_SUB)
                    .shared(OpCode.I64_REINTERPRET_F64, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(OpCode.I64_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(OpCode.I64_XOR, Emitters::I64_XOR)

                    // ====== F32 ======
                    .shared(OpCode.F32_ABS, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_ADD, Emitters::F32_ADD)
                    .shared(OpCode.F32_CEIL, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_CONST, Emitters::F32_CONST)
                    .shared(OpCode.F32_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(OpCode.F32_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(OpCode.F32_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(OpCode.F32_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(OpCode.F32_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_DEMOTE_F64, Emitters::F32_DEMOTE_F64)
                    .intrinsic(OpCode.F32_DIV, Emitters::F32_DIV)
                    .shared(OpCode.F32_EQ, OpcodeImpl.class)
                    .shared(OpCode.F32_FLOOR, OpcodeImpl.class)
                    .shared(OpCode.F32_GE, OpcodeImpl.class)
                    .shared(OpCode.F32_GT, OpcodeImpl.class)
                    .shared(OpCode.F32_LE, OpcodeImpl.class)
                    .shared(OpCode.F32_LT, OpcodeImpl.class)
                    .shared(OpCode.F32_MAX, OpcodeImpl.class)
                    .shared(OpCode.F32_MIN, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_MUL, Emitters::F32_MUL)
                    .shared(OpCode.F32_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_NEG, Emitters::F32_NEG)
                    .shared(OpCode.F32_NEAREST, OpcodeImpl.class)
                    .shared(OpCode.F32_REINTERPRET_I32, OpcodeImpl.class)
                    .shared(OpCode.F32_SQRT, OpcodeImpl.class)
                    .intrinsic(OpCode.F32_SUB, Emitters::F32_SUB)
                    .shared(OpCode.F32_TRUNC, OpcodeImpl.class)

                    // ====== F64 ======
                    .shared(OpCode.F64_ABS, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_ADD, Emitters::F64_ADD)
                    .shared(OpCode.F64_CEIL, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_CONST, Emitters::F64_CONST)
                    .shared(OpCode.F64_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(OpCode.F64_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(OpCode.F64_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(OpCode.F64_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(OpCode.F64_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_DIV, Emitters::F64_DIV)
                    .shared(OpCode.F64_EQ, OpcodeImpl.class)
                    .shared(OpCode.F64_FLOOR, OpcodeImpl.class)
                    .shared(OpCode.F64_GE, OpcodeImpl.class)
                    .shared(OpCode.F64_GT, OpcodeImpl.class)
                    .shared(OpCode.F64_LE, OpcodeImpl.class)
                    .shared(OpCode.F64_LT, OpcodeImpl.class)
                    .shared(OpCode.F64_MAX, OpcodeImpl.class)
                    .shared(OpCode.F64_MIN, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_MUL, Emitters::F64_MUL)
                    .shared(OpCode.F64_NE, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_NEG, Emitters::F64_NEG)
                    .shared(OpCode.F64_NEAREST, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_PROMOTE_F32, Emitters::F64_PROMOTE_F32)
                    .shared(OpCode.F64_REINTERPRET_I64, OpcodeImpl.class)
                    .shared(OpCode.F64_SQRT, OpcodeImpl.class)
                    .intrinsic(OpCode.F64_SUB, Emitters::F64_SUB)
                    .shared(OpCode.F64_TRUNC, OpcodeImpl.class)
                    .build();

    private OpCodeEmitters() {}

    private static final class Builder {

        private final Map<OpCode, OpCodeEmitter> emitters = new EnumMap<>(OpCode.class);

        public Builder intrinsic(OpCode opCode, Emitter emitter) {
            emitters.put(opCode, (ctx, operands) -> emitter.emit(ctx));
            return this;
        }

        public Builder intrinsic(OpCode opCode, OpCodeEmitter emitter) {
            emitters.put(opCode, emitter);
            return this;
        }

        public Builder shared(OpCode opCode, Class<?> staticHelpers) {
            emitters.put(opCode, intrinsify(opCode, staticHelpers));
            return this;
        }

        public Map<OpCode, OpCodeEmitter> build() {
            return Map.copyOf(emitters);
        }
    }

    private static Builder builder() {
        return new Builder();
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
     * @param opcode        the WASM opcode that is implemented by an annotated static method in this class
     * @param staticHelpers the class containing the implementation
     * @return a BytecodeEmitter that will implement the opcode via a call to the shared implementation
     */
    private static OpCodeEmitter intrinsify(OpCode opcode, Class<?> staticHelpers) {
        for (var method : staticHelpers.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(OpCodeIdentifier.class)) {
                assert method.getAnnotation(OpCodeIdentifier.class) != null;
                if (method.getAnnotation(OpCodeIdentifier.class).value() == opcode) {
                    return (ctx, operands) -> emitInvokeStatic(ctx.asm(), method);
                }
            }
        }
        throw new IllegalArgumentException(
                "Static helper "
                        + staticHelpers.getName()
                        + " does not provide an implementation of opcode "
                        + opcode.name());
    }
}
