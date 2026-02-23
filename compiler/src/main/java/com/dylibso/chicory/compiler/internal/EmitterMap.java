package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.OpcodeImpl;
import java.util.Map;

final class EmitterMap {

    public static final Map<CompilerOpCode, Emitters.BytecodeEmitter> EMITTERS =
            Emitters.builder()
                    // ====== Misc ======
                    .intrinsic(CompilerOpCode.DROP_KEEP, Emitters::DROP_KEEP)
                    .intrinsic(CompilerOpCode.TRAP, Emitters::TRAP)
                    .intrinsic(CompilerOpCode.RETURN, Emitters::RETURN)
                    .intrinsic(CompilerOpCode.DROP, Emitters::DROP)
                    .intrinsic(CompilerOpCode.ELEM_DROP, Emitters::ELEM_DROP)
                    .intrinsic(CompilerOpCode.SELECT, Emitters::SELECT)

                    // ====== Control Flow ======
                    .intrinsic(CompilerOpCode.CALL, Emitters::CALL)
                    .intrinsic(CompilerOpCode.CALL_INDIRECT, Emitters::CALL_INDIRECT)

                    // ====== Exception Handling ======
                    .intrinsic(CompilerOpCode.THROW, Emitters::THROW)
                    .intrinsic(CompilerOpCode.THROW_REF, Emitters::THROW_REF)
                    .intrinsic(CompilerOpCode.CATCH_COMPARE_TAG, Emitters::CATCH_COMPARE_TAG)
                    .intrinsic(CompilerOpCode.CATCH_UNBOX_PARAMS, Emitters::CATCH_UNBOX_PARAMS)
                    .intrinsic(
                            CompilerOpCode.CATCH_REGISTER_EXCEPTION,
                            Emitters::CATCH_REGISTER_EXCEPTION)
                    .intrinsic(CompilerOpCode.CATCH_START, Emitters::CATCH_START)
                    .intrinsic(CompilerOpCode.CATCH_END, Emitters::CATCH_END)

                    // ====== References ======
                    .intrinsic(CompilerOpCode.REF_FUNC, Emitters::REF_FUNC)
                    .intrinsic(CompilerOpCode.REF_NULL, Emitters::REF_NULL)
                    .intrinsic(CompilerOpCode.REF_IS_NULL, Emitters::REF_IS_NULL)
                    .intrinsic(CompilerOpCode.REF_EQ, Emitters::REF_EQ)
                    .intrinsic(CompilerOpCode.REF_AS_NON_NULL, Emitters::REF_AS_NON_NULL)

                    // ====== Locals & Globals ======
                    .intrinsic(CompilerOpCode.LOCAL_GET, Emitters::LOCAL_GET)
                    .intrinsic(CompilerOpCode.LOCAL_SET, Emitters::LOCAL_SET)
                    .intrinsic(CompilerOpCode.LOCAL_TEE, Emitters::LOCAL_TEE)
                    .intrinsic(CompilerOpCode.GLOBAL_GET, Emitters::GLOBAL_GET)
                    .intrinsic(CompilerOpCode.GLOBAL_SET, Emitters::GLOBAL_SET)

                    // ====== Tables ======
                    .intrinsic(CompilerOpCode.TABLE_GET, Emitters::TABLE_GET)
                    .intrinsic(CompilerOpCode.TABLE_SET, Emitters::TABLE_SET)
                    .intrinsic(CompilerOpCode.TABLE_SIZE, Emitters::TABLE_SIZE)
                    .intrinsic(CompilerOpCode.TABLE_GROW, Emitters::TABLE_GROW)
                    .intrinsic(CompilerOpCode.TABLE_FILL, Emitters::TABLE_FILL)
                    .intrinsic(CompilerOpCode.TABLE_COPY, Emitters::TABLE_COPY)
                    .intrinsic(CompilerOpCode.TABLE_INIT, Emitters::TABLE_INIT)

                    // ====== Memory ======
                    .intrinsic(CompilerOpCode.MEMORY_INIT, Emitters::MEMORY_INIT)
                    .intrinsic(CompilerOpCode.MEMORY_COPY, Emitters::MEMORY_COPY)
                    .intrinsic(CompilerOpCode.MEMORY_FILL, Emitters::MEMORY_FILL)
                    .intrinsic(CompilerOpCode.MEMORY_GROW, Emitters::MEMORY_GROW)
                    .intrinsic(CompilerOpCode.MEMORY_SIZE, Emitters::MEMORY_SIZE)
                    .intrinsic(CompilerOpCode.DATA_DROP, Emitters::DATA_DROP)

                    // ====== Load & Store ======
                    .intrinsic(CompilerOpCode.I32_LOAD, Emitters::I32_LOAD)
                    .intrinsic(CompilerOpCode.I32_LOAD8_S, Emitters::I32_LOAD8_S)
                    .intrinsic(CompilerOpCode.I32_LOAD8_U, Emitters::I32_LOAD8_U)
                    .intrinsic(CompilerOpCode.I32_LOAD16_S, Emitters::I32_LOAD16_S)
                    .intrinsic(CompilerOpCode.I32_LOAD16_U, Emitters::I32_LOAD16_U)
                    .intrinsic(CompilerOpCode.I64_LOAD, Emitters::I64_LOAD)
                    .intrinsic(CompilerOpCode.I64_LOAD8_S, Emitters::I64_LOAD8_S)
                    .intrinsic(CompilerOpCode.I64_LOAD8_U, Emitters::I64_LOAD8_U)
                    .intrinsic(CompilerOpCode.I64_LOAD16_S, Emitters::I64_LOAD16_S)
                    .intrinsic(CompilerOpCode.I64_LOAD16_U, Emitters::I64_LOAD16_U)
                    .intrinsic(CompilerOpCode.I64_LOAD32_S, Emitters::I64_LOAD32_S)
                    .intrinsic(CompilerOpCode.I64_LOAD32_U, Emitters::I64_LOAD32_U)
                    .intrinsic(CompilerOpCode.F32_LOAD, Emitters::F32_LOAD)
                    .intrinsic(CompilerOpCode.F64_LOAD, Emitters::F64_LOAD)
                    .intrinsic(CompilerOpCode.I32_STORE, Emitters::I32_STORE)
                    .intrinsic(CompilerOpCode.I32_STORE8, Emitters::I32_STORE8)
                    .intrinsic(CompilerOpCode.I32_STORE16, Emitters::I32_STORE16)
                    .intrinsic(CompilerOpCode.I64_STORE, Emitters::I64_STORE)
                    .intrinsic(CompilerOpCode.I64_STORE8, Emitters::I64_STORE8)
                    .intrinsic(CompilerOpCode.I64_STORE16, Emitters::I64_STORE16)
                    .intrinsic(CompilerOpCode.I64_STORE32, Emitters::I64_STORE32)
                    .intrinsic(CompilerOpCode.F32_STORE, Emitters::F32_STORE)
                    .intrinsic(CompilerOpCode.F64_STORE, Emitters::F64_STORE)

                    // === Threads =====
                    .intrinsic(CompilerOpCode.ATOMIC_INT_LOAD, Emitters::ATOMIC_INT_READ)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_LOAD_BYTE, Emitters::ATOMIC_INT_READ_BYTE)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_LOAD_SHORT, Emitters::ATOMIC_INT_READ_SHORT)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_LOAD, Emitters::ATOMIC_LONG_READ)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_LOAD_BYTE, Emitters::ATOMIC_LONG_READ_BYTE)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_LOAD_SHORT, Emitters::ATOMIC_LONG_READ_SHORT)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_LOAD_INT, Emitters::ATOMIC_LONG_READ_INT)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_STORE, Emitters::ATOMIC_INT_STORE)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_STORE_BYTE, Emitters::ATOMIC_INT_STORE_BYTE)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_STORE_SHORT, Emitters::ATOMIC_INT_STORE_SHORT)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_STORE, Emitters::ATOMIC_LONG_STORE)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_STORE_BYTE, Emitters::ATOMIC_LONG_STORE_BYTE)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_STORE_SHORT,
                            Emitters::ATOMIC_LONG_STORE_SHORT)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_STORE_INT, Emitters::ATOMIC_LONG_STORE_INT)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW_ADD, Emitters::ATOMIC_INT_RMW_ADD)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW_SUB, Emitters::ATOMIC_INT_RMW_SUB)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW_AND, Emitters::ATOMIC_INT_RMW_AND)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW_OR, Emitters::ATOMIC_INT_RMW_OR)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW_XOR, Emitters::ATOMIC_INT_RMW_XOR)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW_XCHG, Emitters::ATOMIC_INT_RMW_XCHG)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW_CMPXCHG, Emitters::ATOMIC_INT_RMW_CMPXCHG)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW8_ADD_U, Emitters::ATOMIC_INT_RMW8_ADD_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW8_SUB_U, Emitters::ATOMIC_INT_RMW8_SUB_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW8_AND_U, Emitters::ATOMIC_INT_RMW8_AND_U)
                    .intrinsic(CompilerOpCode.ATOMIC_INT_RMW8_OR_U, Emitters::ATOMIC_INT_RMW8_OR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW8_XOR_U, Emitters::ATOMIC_INT_RMW8_XOR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW8_XCHG_U, Emitters::ATOMIC_INT_RMW8_XCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW8_CMPXCHG_U,
                            Emitters::ATOMIC_INT_RMW8_CMPXCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_ADD_U, Emitters::ATOMIC_INT_RMW16_ADD_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_SUB_U, Emitters::ATOMIC_INT_RMW16_SUB_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_AND_U, Emitters::ATOMIC_INT_RMW16_AND_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_OR_U, Emitters::ATOMIC_INT_RMW16_OR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_XOR_U, Emitters::ATOMIC_INT_RMW16_XOR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_XCHG_U,
                            Emitters::ATOMIC_INT_RMW16_XCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_INT_RMW16_CMPXCHG_U,
                            Emitters::ATOMIC_INT_RMW16_CMPXCHG_U)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_RMW_ADD, Emitters::ATOMIC_LONG_RMW_ADD)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_RMW_SUB, Emitters::ATOMIC_LONG_RMW_SUB)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_RMW_AND, Emitters::ATOMIC_LONG_RMW_AND)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_RMW_OR, Emitters::ATOMIC_LONG_RMW_OR)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_RMW_XOR, Emitters::ATOMIC_LONG_RMW_XOR)
                    .intrinsic(CompilerOpCode.ATOMIC_LONG_RMW_XCHG, Emitters::ATOMIC_LONG_RMW_XCHG)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW_CMPXCHG,
                            Emitters::ATOMIC_LONG_RMW_CMPXCHG)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_ADD_U, Emitters::ATOMIC_LONG_RMW8_ADD_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_SUB_U, Emitters::ATOMIC_LONG_RMW8_SUB_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_AND_U, Emitters::ATOMIC_LONG_RMW8_AND_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_OR_U, Emitters::ATOMIC_LONG_RMW8_OR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_XOR_U, Emitters::ATOMIC_LONG_RMW8_XOR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_XCHG_U,
                            Emitters::ATOMIC_LONG_RMW8_XCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW8_CMPXCHG_U,
                            Emitters::ATOMIC_LONG_RMW8_CMPXCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_ADD_U,
                            Emitters::ATOMIC_LONG_RMW16_ADD_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_SUB_U,
                            Emitters::ATOMIC_LONG_RMW16_SUB_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_AND_U,
                            Emitters::ATOMIC_LONG_RMW16_AND_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_OR_U, Emitters::ATOMIC_LONG_RMW16_OR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_XOR_U,
                            Emitters::ATOMIC_LONG_RMW16_XOR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_XCHG_U,
                            Emitters::ATOMIC_LONG_RMW16_XCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW16_CMPXCHG_U,
                            Emitters::ATOMIC_LONG_RMW16_CMPXCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_ADD_U,
                            Emitters::ATOMIC_LONG_RMW32_ADD_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_SUB_U,
                            Emitters::ATOMIC_LONG_RMW32_SUB_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_AND_U,
                            Emitters::ATOMIC_LONG_RMW32_AND_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_OR_U, Emitters::ATOMIC_LONG_RMW32_OR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_XOR_U,
                            Emitters::ATOMIC_LONG_RMW32_XOR_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_XCHG_U,
                            Emitters::ATOMIC_LONG_RMW32_XCHG_U)
                    .intrinsic(
                            CompilerOpCode.ATOMIC_LONG_RMW32_CMPXCHG_U,
                            Emitters::ATOMIC_LONG_RMW32_CMPXCHG_U)
                    .intrinsic(CompilerOpCode.MEM_ATOMIC_WAIT32, Emitters::MEM_ATOMIC_WAIT32)
                    .intrinsic(CompilerOpCode.MEM_ATOMIC_WAIT64, Emitters::MEM_ATOMIC_WAIT64)
                    .intrinsic(CompilerOpCode.MEM_ATOMIC_NOTIFY, Emitters::MEM_ATOMIC_NOTIFY)
                    .intrinsic(CompilerOpCode.ATOMIC_FENCE, Emitters::MEM_ATOMIC_FENCE)

                    // ====== I32 ======
                    .intrinsic(CompilerOpCode.I32_ADD, Emitters::I32_ADD)
                    .intrinsic(CompilerOpCode.I32_AND, Emitters::I32_AND)
                    .shared(CompilerOpCode.I32_CLZ, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I32_CONST, Emitters::I32_CONST)
                    .shared(CompilerOpCode.I32_CTZ, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_DIV_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_DIV_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_EQ, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_EQZ, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_EXTEND_8_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_EXTEND_16_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_GE_S, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I32_GE_U, Emitters::I32_GE_U)
                    .shared(CompilerOpCode.I32_GT_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_GT_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_LE_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_LE_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_LT_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_LT_U, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I32_MUL, Emitters::I32_MUL)
                    .shared(CompilerOpCode.I32_NE, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I32_OR, Emitters::I32_OR)
                    .shared(CompilerOpCode.I32_POPCNT, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_REINTERPRET_F32, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_REM_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_REM_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_ROTL, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_ROTR, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I32_SHL, Emitters::I32_SHL)
                    .intrinsic(CompilerOpCode.I32_SHR_S, Emitters::I32_SHR_S)
                    .intrinsic(CompilerOpCode.I32_SHR_U, Emitters::I32_SHR_U)
                    .intrinsic(CompilerOpCode.I32_SUB, Emitters::I32_SUB)
                    .shared(CompilerOpCode.I32_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I32_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I32_WRAP_I64, Emitters::I32_WRAP_I64)
                    .intrinsic(CompilerOpCode.I32_XOR, Emitters::I32_XOR)

                    // ====== I64 ======
                    .intrinsic(CompilerOpCode.I64_ADD, Emitters::I64_ADD)
                    .intrinsic(CompilerOpCode.I64_AND, Emitters::I64_AND)
                    .shared(CompilerOpCode.I64_CLZ, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I64_CONST, Emitters::I64_CONST)
                    .shared(CompilerOpCode.I64_CTZ, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_DIV_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_DIV_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_EQ, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_EQZ, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_EXTEND_8_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_EXTEND_16_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_EXTEND_32_S, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I64_EXTEND_I32_S, Emitters::I64_EXTEND_I32_S)
                    .shared(CompilerOpCode.I64_EXTEND_I32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_GE_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_GE_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_GT_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_GT_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_LE_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_LE_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_LT_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_LT_U, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I64_MUL, Emitters::I64_MUL)
                    .shared(CompilerOpCode.I64_NE, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I64_OR, Emitters::I64_OR)
                    .shared(CompilerOpCode.I64_POPCNT, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_REM_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_REM_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_ROTL, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_ROTR, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I64_SHL, Emitters::I64_SHL)
                    .intrinsic(CompilerOpCode.I64_SHR_S, Emitters::I64_SHR_S)
                    .intrinsic(CompilerOpCode.I64_SHR_U, Emitters::I64_SHR_U)
                    .intrinsic(CompilerOpCode.I64_SUB, Emitters::I64_SUB)
                    .shared(CompilerOpCode.I64_REINTERPRET_F64, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_F32_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_F32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_F64_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_F64_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_SAT_F32_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_SAT_F32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_SAT_F64_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.I64_TRUNC_SAT_F64_U, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.I64_XOR, Emitters::I64_XOR)

                    // ====== F32 ======
                    .shared(CompilerOpCode.F32_ABS, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F32_ADD, Emitters::F32_ADD)
                    .shared(CompilerOpCode.F32_CEIL, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F32_CONST, Emitters::F32_CONST)
                    .shared(CompilerOpCode.F32_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F32_DEMOTE_F64, Emitters::F32_DEMOTE_F64)
                    .intrinsic(CompilerOpCode.F32_DIV, Emitters::F32_DIV)
                    .shared(CompilerOpCode.F32_EQ, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_FLOOR, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_GE, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_GT, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_LE, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_LT, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_MAX, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_MIN, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F32_MUL, Emitters::F32_MUL)
                    .shared(CompilerOpCode.F32_NE, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F32_NEG, Emitters::F32_NEG)
                    .shared(CompilerOpCode.F32_NEAREST, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_REINTERPRET_I32, OpcodeImpl.class)
                    .shared(CompilerOpCode.F32_SQRT, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F32_SUB, Emitters::F32_SUB)
                    .shared(CompilerOpCode.F32_TRUNC, OpcodeImpl.class)

                    // ====== F64 ======
                    .shared(CompilerOpCode.F64_ABS, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_ADD, Emitters::F64_ADD)
                    .shared(CompilerOpCode.F64_CEIL, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_CONST, Emitters::F64_CONST)
                    .shared(CompilerOpCode.F64_CONVERT_I32_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_CONVERT_I32_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_CONVERT_I64_S, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_CONVERT_I64_U, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_COPYSIGN, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_DIV, Emitters::F64_DIV)
                    .shared(CompilerOpCode.F64_EQ, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_FLOOR, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_GE, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_GT, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_LE, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_LT, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_MAX, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_MIN, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_MUL, Emitters::F64_MUL)
                    .shared(CompilerOpCode.F64_NE, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_NEG, Emitters::F64_NEG)
                    .shared(CompilerOpCode.F64_NEAREST, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_PROMOTE_F32, Emitters::F64_PROMOTE_F32)
                    .shared(CompilerOpCode.F64_REINTERPRET_I64, OpcodeImpl.class)
                    .shared(CompilerOpCode.F64_SQRT, OpcodeImpl.class)
                    .intrinsic(CompilerOpCode.F64_SUB, Emitters::F64_SUB)
                    .shared(CompilerOpCode.F64_TRUNC, OpcodeImpl.class)

                    // ====== GC ======
                    .intrinsic(CompilerOpCode.CALL_REF, Emitters::CALL_REF)
                    .intrinsic(CompilerOpCode.STRUCT_NEW, Emitters::STRUCT_NEW)
                    .intrinsic(CompilerOpCode.STRUCT_NEW_DEFAULT, Emitters::STRUCT_NEW_DEFAULT)
                    .intrinsic(CompilerOpCode.STRUCT_GET, Emitters::STRUCT_GET)
                    .intrinsic(CompilerOpCode.STRUCT_GET_S, Emitters::STRUCT_GET_S)
                    .intrinsic(CompilerOpCode.STRUCT_GET_U, Emitters::STRUCT_GET_U)
                    .intrinsic(CompilerOpCode.STRUCT_SET, Emitters::STRUCT_SET)
                    .intrinsic(CompilerOpCode.ARRAY_NEW, Emitters::ARRAY_NEW)
                    .intrinsic(CompilerOpCode.ARRAY_NEW_DEFAULT, Emitters::ARRAY_NEW_DEFAULT)
                    .intrinsic(CompilerOpCode.ARRAY_NEW_FIXED, Emitters::ARRAY_NEW_FIXED)
                    .intrinsic(CompilerOpCode.ARRAY_NEW_DATA, Emitters::ARRAY_NEW_DATA)
                    .intrinsic(CompilerOpCode.ARRAY_NEW_ELEM, Emitters::ARRAY_NEW_ELEM)
                    .intrinsic(CompilerOpCode.ARRAY_GET, Emitters::ARRAY_GET)
                    .intrinsic(CompilerOpCode.ARRAY_GET_S, Emitters::ARRAY_GET_S)
                    .intrinsic(CompilerOpCode.ARRAY_GET_U, Emitters::ARRAY_GET_U)
                    .intrinsic(CompilerOpCode.ARRAY_SET, Emitters::ARRAY_SET)
                    .intrinsic(CompilerOpCode.ARRAY_LEN, Emitters::ARRAY_LEN)
                    .intrinsic(CompilerOpCode.ARRAY_FILL, Emitters::ARRAY_FILL)
                    .intrinsic(CompilerOpCode.ARRAY_COPY, Emitters::ARRAY_COPY)
                    .intrinsic(CompilerOpCode.ARRAY_INIT_DATA, Emitters::ARRAY_INIT_DATA)
                    .intrinsic(CompilerOpCode.ARRAY_INIT_ELEM, Emitters::ARRAY_INIT_ELEM)
                    .intrinsic(CompilerOpCode.REF_TEST, Emitters::REF_TEST)
                    .intrinsic(CompilerOpCode.REF_TEST_NULL, Emitters::REF_TEST_NULL)
                    .intrinsic(CompilerOpCode.CAST_TEST, Emitters::CAST_TEST)
                    .intrinsic(CompilerOpCode.CAST_TEST_NULL, Emitters::CAST_TEST_NULL)
                    .intrinsic(CompilerOpCode.REF_I31, Emitters::REF_I31)
                    .intrinsic(CompilerOpCode.I31_GET_S, Emitters::I31_GET_S)
                    .intrinsic(CompilerOpCode.I31_GET_U, Emitters::I31_GET_U)
                    .intrinsic(CompilerOpCode.ANY_CONVERT_EXTERN, Emitters::ANY_CONVERT_EXTERN)
                    .intrinsic(CompilerOpCode.EXTERN_CONVERT_ANY, Emitters::EXTERN_CONVERT_ANY)
                    .intrinsic(CompilerOpCode.BR_ON_NULL_CHECK, Emitters::BR_ON_NULL_CHECK)
                    .intrinsic(CompilerOpCode.BR_ON_NON_NULL_CHECK, Emitters::BR_ON_NON_NULL_CHECK)
                    .intrinsic(CompilerOpCode.BR_ON_CAST_CHECK, Emitters::BR_ON_CAST_CHECK)
                    .intrinsic(
                            CompilerOpCode.BR_ON_CAST_FAIL_CHECK, Emitters::BR_ON_CAST_FAIL_CHECK)
                    .build();

    private EmitterMap() {}
}
