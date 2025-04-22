package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.WasmEncoding.BYTE;
import static com.dylibso.chicory.wasm.types.WasmEncoding.FLOAT32;
import static com.dylibso.chicory.wasm.types.WasmEncoding.FLOAT64;
import static com.dylibso.chicory.wasm.types.WasmEncoding.V128;
import static com.dylibso.chicory.wasm.types.WasmEncoding.VARSINT32;
import static com.dylibso.chicory.wasm.types.WasmEncoding.VARSINT64;
import static com.dylibso.chicory.wasm.types.WasmEncoding.VARUINT;
import static com.dylibso.chicory.wasm.types.WasmEncoding.VEC_CATCH;
import static com.dylibso.chicory.wasm.types.WasmEncoding.VEC_VARUINT;

import java.util.List;

/**
 * Represents the set of possible WebAssembly instructions.
 * Each enum constant corresponds to a specific operation, identified by a unique byte code.
 * Many opcodes also define a signature, indicating the types of immediate operands that follow the opcode byte.
 */
public enum OpCode {
    /** Unreachable instruction. */
    UNREACHABLE(0x00),
    /** No operation. */
    NOP(0x01),
    /** Block instruction. */
    BLOCK(0x02, List.of(VARUINT)),
    /** Loop instruction. */
    LOOP(0x03, List.of(VARUINT)),
    /** If instruction. */
    IF(0x04, List.of(VARUINT)),
    /** Else instruction. */
    ELSE(0x05),
    /** Throw instruction. */
    THROW(0x08, List.of(VARUINT)),
    /** Throw reference instruction. */
    THROW_REF(0x0A),
    /** End instruction. */
    END(0x0B),
    /** Branch instruction. */
    BR(0x0C, List.of(VARUINT)),
    /** Branch if instruction. */
    BR_IF(0x0D, List.of(VARUINT)),
    /** Branch table instruction. */
    BR_TABLE(0x0E, List.of(VEC_VARUINT, VARUINT)),
    /** Return instruction. */
    RETURN(0x0F),
    /** Call instruction. */
    CALL(0x10, List.of(VARUINT)),
    /** Call indirect instruction. */
    CALL_INDIRECT(0x11, List.of(VARUINT, VARUINT)),
    /** Return call instruction. */
    RETURN_CALL(0x12, List.of(VARUINT)),
    /** Return call indirect. */
    RETURN_CALL_INDIRECT(0x13, List.of(VARUINT, VARUINT)),
    /** Call reference instruction. */
    CALL_REF(0x14, List.of(VARUINT)),
    /** Drop instruction. */
    DROP(0x1A),
    /** Select instruction. */
    SELECT(0x1B),
    /** Select typed instruction. */
    SELECT_T(0x1C, List.of(VEC_VARUINT)),
    /** Try table instruction. */
    TRY_TABLE(0x1F, List.of(VARUINT, VEC_CATCH)),
    /** Local get instruction. */
    LOCAL_GET(0x20, List.of(VARUINT)),
    /** Local set instruction. */
    LOCAL_SET(0x21, List.of(VARUINT)),
    /** Local tee instruction. */
    LOCAL_TEE(0x22, List.of(VARUINT)),
    /** Global get instruction. */
    GLOBAL_GET(0x23, List.of(VARUINT)),
    /** Global set instruction. */
    GLOBAL_SET(0x24, List.of(VARUINT)),
    /** Table get instruction. */
    TABLE_GET(0x25, List.of(VARUINT)),
    /** Table set instruction. */
    TABLE_SET(0x26, List.of(VARUINT)),
    /** I32 load instruction. */
    I32_LOAD(0x28, List.of(VARUINT, VARUINT)),
    /** I64 load instruction. */
    I64_LOAD(0x29, List.of(VARUINT, VARUINT)),
    /** F32 load instruction. */
    F32_LOAD(0x2A, List.of(VARUINT, VARUINT)),
    /** F64 load instruction. */
    F64_LOAD(0x2B, List.of(VARUINT, VARUINT)),
    /** I32 load 8 signed. */
    I32_LOAD8_S(0x2C, List.of(VARUINT, VARUINT)),
    /** I32 load 8 unsigned. */
    I32_LOAD8_U(0x2D, List.of(VARUINT, VARUINT)),
    /** I32 load 16 signed. */
    I32_LOAD16_S(0x2E, List.of(VARUINT, VARUINT)),
    /** I32 load 16 unsigned. */
    I32_LOAD16_U(0x2F, List.of(VARUINT, VARUINT)),
    /** I64 load 8 signed. */
    I64_LOAD8_S(0x30, List.of(VARUINT, VARUINT)),
    /** I64 load 8 unsigned. */
    I64_LOAD8_U(0x31, List.of(VARUINT, VARUINT)),
    /** I64 load 16 signed. */
    I64_LOAD16_S(0x32, List.of(VARUINT, VARUINT)),
    /** I64 load 16 unsigned. */
    I64_LOAD16_U(0x33, List.of(VARUINT, VARUINT)),
    /** I64 load 32 signed. */
    I64_LOAD32_S(0x34, List.of(VARUINT, VARUINT)),
    /** I64 load 32 unsigned. */
    I64_LOAD32_U(0x35, List.of(VARUINT, VARUINT)),
    /** I32 store instruction. */
    I32_STORE(0x36, List.of(VARUINT, VARUINT)),
    /** I64 store instruction. */
    I64_STORE(0x37, List.of(VARUINT, VARUINT)),
    /** F32 store instruction. */
    F32_STORE(0x38, List.of(VARUINT, VARUINT)),
    /** F64 store instruction. */
    F64_STORE(0x39, List.of(VARUINT, VARUINT)),
    /** I32 store 8 bit. */
    I32_STORE8(0x3A, List.of(VARUINT, VARUINT)),
    /** I32 store 16 bit. */
    I32_STORE16(0x3B, List.of(VARUINT, VARUINT)),
    /** I64 store 8 bit. */
    I64_STORE8(0x3C, List.of(VARUINT, VARUINT)),
    /** I64 store 16 bit. */
    I64_STORE16(0x3D, List.of(VARUINT, VARUINT)),
    /** I64 store 32 bit. */
    I64_STORE32(0x3E, List.of(VARUINT, VARUINT)),
    /** Memory size instruction. */
    MEMORY_SIZE(0x3F),
    /** Memory grow instruction. */
    MEMORY_GROW(0x40),
    /** I32 constant instruction. */
    I32_CONST(0x41, List.of(VARSINT32)),
    /** I64 constant instruction. */
    I64_CONST(0x42, List.of(VARSINT64)),
    /** F32 constant instruction. */
    F32_CONST(0x43, List.of(FLOAT32)),
    /** F64 constant instruction. */
    F64_CONST(0x44, List.of(FLOAT64)),
    /** I32 equals zero. */
    I32_EQZ(0x45),
    /** I32 equals. */
    I32_EQ(0x46),
    /** I32 not equals. */
    I32_NE(0x47),
    /** I32 less than signed. */
    I32_LT_S(0x48),
    /** I32 less than unsigned. */
    I32_LT_U(0x49),
    /** I32 greater than signed. */
    I32_GT_S(0x4A),
    /** I32 greater than unsigned. */
    I32_GT_U(0x4B),
    /** I32 less equal signed. */
    I32_LE_S(0x4C),
    /** I32 less equal unsigned. */
    I32_LE_U(0x4D),
    /** I32 greater equal signed. */
    I32_GE_S(0x4E),
    /** I32 greater equal unsigned. */
    I32_GE_U(0x4F),
    /** I64 equals zero. */
    I64_EQZ(0x50),
    /** I64 equals. */
    I64_EQ(0x51),
    /** I64 not equals. */
    I64_NE(0x52),
    /** I64 less than signed. */
    I64_LT_S(0x53),
    /** I64 less than unsigned. */
    I64_LT_U(0x54),
    /** I64 greater than signed. */
    I64_GT_S(0x55),
    /** I64 greater than unsigned. */
    I64_GT_U(0x56),
    /** I64 less equal signed. */
    I64_LE_S(0x57),
    /** I64 less equal unsigned. */
    I64_LE_U(0x58),
    /** I64 greater equal signed. */
    I64_GE_S(0x59),
    /** I64 greater equal unsigned. */
    I64_GE_U(0x5A),
    /** F32 equals. */
    F32_EQ(0x5B),
    /** F32 not equals. */
    F32_NE(0x5C),
    /** F32 less than. */
    F32_LT(0x5D),
    /** F32 greater than. */
    F32_GT(0x5E),
    /** F32 less equal. */
    F32_LE(0x5F),
    /** F32 greater equal. */
    F32_GE(0x60),
    /** F64 equals. */
    F64_EQ(0x61),
    /** F64 not equals. */
    F64_NE(0x62),
    /** F64 less than. */
    F64_LT(0x63),
    /** F64 greater than. */
    F64_GT(0x64),
    /** F64 less equal. */
    F64_LE(0x65),
    /** F64 greater equal. */
    F64_GE(0x66),
    /** I32 count leading zeros. */
    I32_CLZ(0x67),
    /** I32 count trailing zeros. */
    I32_CTZ(0x68),
    /** I32 population count. */
    I32_POPCNT(0x69),
    /** I32 add. */
    I32_ADD(0x6A),
    /** I32 subtract. */
    I32_SUB(0x6B),
    /** I32 multiply. */
    I32_MUL(0x6C),
    /** I32 divide signed. */
    I32_DIV_S(0x6D),
    /** I32 divide unsigned. */
    I32_DIV_U(0x6E),
    /** I32 remainder signed. */
    I32_REM_S(0x6F),
    /** I32 remainder unsigned. */
    I32_REM_U(0x70),
    /** I32 bitwise AND. */
    I32_AND(0x71),
    /** I32 bitwise OR. */
    I32_OR(0x72),
    /** I32 bitwise XOR. */
    I32_XOR(0x73),
    /** I32 shift left. */
    I32_SHL(0x74),
    /** I32 shift right signed. */
    I32_SHR_S(0x75),
    /** I32 shift right unsigned. */
    I32_SHR_U(0x76),
    /** I32 rotate left. */
    I32_ROTL(0x77),
    /** I32 rotate right. */
    I32_ROTR(0x78),
    /** I64 count leading zeros. */
    I64_CLZ(0x79),
    /** I64 count trailing zeros. */
    I64_CTZ(0x7A),
    /** I64 population count. */
    I64_POPCNT(0x7B),
    /** I64 add. */
    I64_ADD(0x7C),
    /** I64 subtract. */
    I64_SUB(0x7D),
    /** I64 multiply. */
    I64_MUL(0x7E),
    /** I64 divide signed. */
    I64_DIV_S(0x7F),
    /** I64 divide unsigned. */
    I64_DIV_U(0x80),
    /** I64 remainder signed. */
    I64_REM_S(0x81),
    /** I64 remainder unsigned. */
    I64_REM_U(0x82),
    /** I64 bitwise AND. */
    I64_AND(0x83),
    /** I64 bitwise OR. */
    I64_OR(0x84),
    /** I64 bitwise XOR. */
    I64_XOR(0x85),
    /** I64 shift left. */
    I64_SHL(0x86),
    /** I64 shift right signed. */
    I64_SHR_S(0x87),
    /** I64 shift right unsigned. */
    I64_SHR_U(0x88),
    /** I64 rotate left. */
    I64_ROTL(0x89),
    /** I64 rotate right. */
    I64_ROTR(0x8A),
    /** F32 absolute value. */
    F32_ABS(0x8B),
    /** F32 negate. */
    F32_NEG(0x8C),
    /** F32 ceiling. */
    F32_CEIL(0x8D),
    /** F32 floor. */
    F32_FLOOR(0x8E),
    /** F32 truncate. */
    F32_TRUNC(0x8F),
    /** F32 nearest integer. */
    F32_NEAREST(0x90),
    /** F32 square root. */
    F32_SQRT(0x91),
    /** F32 add. */
    F32_ADD(0x92),
    /** F32 subtract. */
    F32_SUB(0x93),
    /** F32 multiply. */
    F32_MUL(0x94),
    /** F32 divide. */
    F32_DIV(0x95),
    /** F32 minimum. */
    F32_MIN(0x96),
    /** F32 maximum. */
    F32_MAX(0x97),
    /** F32 copy sign. */
    F32_COPYSIGN(0x98),
    /** F64 absolute value. */
    F64_ABS(0x99),
    /** F64 negate. */
    F64_NEG(0x9A),
    /** F64 ceiling. */
    F64_CEIL(0x9B),
    /** F64 floor. */
    F64_FLOOR(0x9C),
    /** F64 truncate. */
    F64_TRUNC(0x9D),
    /** F64 nearest integer. */
    F64_NEAREST(0x9E),
    /** F64 square root. */
    F64_SQRT(0x9F),
    /** F64 add. */
    F64_ADD(0xA0),
    /** F64 subtract. */
    F64_SUB(0xA1),
    /** F64 multiply. */
    F64_MUL(0xA2),
    /** F64 divide. */
    F64_DIV(0xA3),
    /** F64 minimum. */
    F64_MIN(0xA4),
    /** F64 maximum. */
    F64_MAX(0xA5),
    /** F64 copy sign. */
    F64_COPYSIGN(0xA6),
    /** I32 wrap i64. */
    I32_WRAP_I64(0xA7),
    /** I32 truncate f32 signed. */
    I32_TRUNC_F32_S(0xA8),
    /** I32 truncate f32 unsigned. */
    I32_TRUNC_F32_U(0xA9),
    /** I32 truncate f64 signed. */
    I32_TRUNC_F64_S(0xAA),
    /** I32 truncate f64 unsigned. */
    I32_TRUNC_F64_U(0xAB),
    /** I64 extend i32 signed. */
    I64_EXTEND_I32_S(0xAC),
    /** I64 extend i32 unsigned. */
    I64_EXTEND_I32_U(0xAD),
    /** I64 truncate f32 signed. */
    I64_TRUNC_F32_S(0xAE),
    /** I64 truncate f32 unsigned. */
    I64_TRUNC_F32_U(0xAF),
    /** I64 truncate f64 signed. */
    I64_TRUNC_F64_S(0xB0),
    /** I64 truncate f64 unsigned. */
    I64_TRUNC_F64_U(0xB1),
    /** F32 convert i32 signed. */
    F32_CONVERT_I32_S(0xB2),
    /** F32 convert i32 unsigned. */
    F32_CONVERT_I32_U(0xB3),
    /** F32 convert i64 signed. */
    F32_CONVERT_I64_S(0xB4),
    /** F32 convert i64 unsigned. */
    F32_CONVERT_I64_U(0xB5),
    /** F32 demote f64. */
    F32_DEMOTE_F64(0xB6),
    /** F64 convert i32 signed. */
    F64_CONVERT_I32_S(0xB7),
    /** F64 convert i32 unsigned. */
    F64_CONVERT_I32_U(0xB8),
    /** F64 convert i64 signed. */
    F64_CONVERT_I64_S(0xB9),
    /** F64 convert i64 unsigned. */
    F64_CONVERT_I64_U(0xBA),
    /** F64 promote f32. */
    F64_PROMOTE_F32(0xBB),
    /** I32 reinterpret f32. */
    I32_REINTERPRET_F32(0xBC),
    /** I64 reinterpret f64. */
    I64_REINTERPRET_F64(0xBD),
    /** F32 reinterpret i32. */
    F32_REINTERPRET_I32(0xBE),
    /** F64 reinterpret i64. */
    F64_REINTERPRET_I64(0xBF),
    /** I32 extend 8 signed. */
    I32_EXTEND_8_S(0xC0),
    /** I32 extend 16 signed. */
    I32_EXTEND_16_S(0xC1),
    /** I64 extend 8 signed. */
    I64_EXTEND_8_S(0xC2),
    /** I64 extend 16 signed. */
    I64_EXTEND_16_S(0xC3),
    /** I64 extend 32 signed. */
    I64_EXTEND_32_S(0xC4),
    /** Reference null instruction. */
    REF_NULL(0xD0, List.of(VARUINT)),
    /** Reference is null check. */
    REF_IS_NULL(0xD1),
    /** Reference function instruction. */
    REF_FUNC(0xD2, List.of(VARUINT)),
    /** I32 trunc sat f32 s. */
    I32_TRUNC_SAT_F32_S(0xFC00),
    /** I32 trunc sat f32 u. */
    I32_TRUNC_SAT_F32_U(0xFC01),
    /** I32 trunc sat f64 s. */
    I32_TRUNC_SAT_F64_S(0xFC02),
    /** I32 trunc sat f64 u. */
    I32_TRUNC_SAT_F64_U(0xFC03),
    /** I64 trunc sat f32 s. */
    I64_TRUNC_SAT_F32_S(0xFC04),
    /** I64 trunc sat f32 u. */
    I64_TRUNC_SAT_F32_U(0xFC05),
    /** I64 trunc sat f64 s. */
    I64_TRUNC_SAT_F64_S(0xFC06),
    /** I64 trunc sat f64 u. */
    I64_TRUNC_SAT_F64_U(0xFC07),
    /** Memory initialize instruction. */
    MEMORY_INIT(0xFC08, List.of(VARUINT, VARUINT)),
    /** Data drop instruction. */
    DATA_DROP(0xFC09, List.of(VARUINT)),
    /** Memory copy instruction. */
    MEMORY_COPY(0xFC0A, List.of(VARUINT, VARUINT)),
    /** Memory fill instruction. */
    MEMORY_FILL(0xFC0B, List.of(VARUINT)),
    /** Table initialize instruction. */
    TABLE_INIT(0xFC0C, List.of(VARUINT, VARUINT)),
    /** Element drop instruction. */
    ELEM_DROP(0xFC0D, List.of(VARUINT)),
    /** Table copy instruction. */
    TABLE_COPY(0xFC0E, List.of(VARUINT, VARUINT)),
    /** Table grow instruction. */
    TABLE_GROW(0xFC0F, List.of(VARUINT)),
    /** Table size instruction. */
    TABLE_SIZE(0xFC10, List.of(VARUINT)),
    /** Table fill instruction. */
    TABLE_FILL(0xFC11, List.of(VARUINT)),
    /** V128 load instruction. */
    V128_LOAD(0xFD00, List.of(VARUINT, VARUINT)),
    /** V128 load 8x8 signed. */
    V128_LOAD8x8_S(0xFD01, List.of(VARUINT, VARUINT)),
    /** V128 load 8x8 unsigned. */
    V128_LOAD8x8_U(0xFD02, List.of(VARUINT, VARUINT)),
    /** V128 load 16x4 signed. */
    V128_LOAD16x4_S(0xFD03, List.of(VARUINT, VARUINT)),
    /** V128 load 16x4 unsigned. */
    V128_LOAD16x4_U(0xFD04, List.of(VARUINT, VARUINT)),
    /** V128 load 32x2 signed. */
    V128_LOAD32x2_S(0xFD05, List.of(VARUINT, VARUINT)),
    /** V128 load 32x2 unsigned. */
    V128_LOAD32x2_U(0xFD06, List.of(VARUINT, VARUINT)),
    /** V128 load 8 splat. */
    V128_LOAD8_SPLAT(0xFD07, List.of(VARUINT, VARUINT)),
    /** V128 load 16 splat. */
    V128_LOAD16_SPLAT(0xFD08, List.of(VARUINT, VARUINT)),
    /** V128 load 32 splat. */
    V128_LOAD32_SPLAT(0xFD09, List.of(VARUINT, VARUINT)),
    /** V128 load 64 splat. */
    V128_LOAD64_SPLAT(0xFD0A, List.of(VARUINT, VARUINT)),
    /** V128 store instruction. */
    V128_STORE(0xFD0B, List.of(VARUINT, VARUINT)),
    /** V128 constant instruction. */
    V128_CONST(0xFD0C, List.of(V128)),
    /** I8x16 shuffle instruction. */
    I8x16_SHUFFLE(0xFD0D, List.of(V128)),
    /** I8x16 swizzle instruction. */
    I8x16_SWIZZLE(0xFD0E),
    /** I8x16 splat instruction. */
    I8x16_SPLAT(0xFD0F),
    /** I16x8 splat instruction. */
    I16x8_SPLAT(0xFD10),
    /** I32x4 splat instruction. */
    I32x4_SPLAT(0xFD11),
    /** I64x2 splat instruction. */
    I64x2_SPLAT(0xFD12),
    /** F32x4 splat instruction. */
    F32x4_SPLAT(0xFD13),
    /** F64x2 splat instruction. */
    F64x2_SPLAT(0xFD14),
    /** I8x16 extract lane signed. */
    I8x16_EXTRACT_LANE_S(0xFD15, List.of(BYTE)),
    /** I8x16 extract lane unsigned. */
    I8x16_EXTRACT_LANE_U(0xFD16, List.of(BYTE)),
    /** I8x16 replace lane. */
    I8x16_REPLACE_LANE(0xFD17, List.of(BYTE)),
    /** I16x8 extract lane signed. */
    I16x8_EXTRACT_LANE_S(0xFD18, List.of(BYTE)),
    /** I16x8 extract lane unsigned. */
    I16x8_EXTRACT_LANE_U(0xFD19, List.of(BYTE)),
    /** I16x8 replace lane. */
    I16x8_REPLACE_LANE(0xFD1A, List.of(BYTE)),
    /** I32x4 extract lane. */
    I32x4_EXTRACT_LANE(0xFD1B, List.of(BYTE)),
    /** I32x4 replace lane. */
    I32x4_REPLACE_LANE(0xFD1C, List.of(BYTE)),
    /** I64x2 extract lane. */
    I64x2_EXTRACT_LANE(0xFD1D, List.of(BYTE)),
    /** I64x2 replace lane. */
    I64x2_REPLACE_LANE(0xFD1E, List.of(BYTE)),
    /** F32x4 extract lane. */
    F32x4_EXTRACT_LANE(0xFD1F, List.of(BYTE)),
    /** F32x4 replace lane. */
    F32x4_REPLACE_LANE(0xFD20, List.of(BYTE)),
    /** F64x2 extract lane. */
    F64x2_EXTRACT_LANE(0xFD21, List.of(BYTE)),
    /** F64x2 replace lane. */
    F64x2_REPLACE_LANE(0xFD22, List.of(BYTE)),
    /** I8x16 equals. */
    I8x16_EQ(0xFD23),
    /** I8x16 not equals. */
    I8x16_NE(0xFD24),
    /** I8x16 less than signed. */
    I8x16_LT_S(0xFD25),
    /** I8x16 less than unsigned. */
    I8x16_LT_U(0xFD26),
    /** I8x16 greater than signed. */
    I8x16_GT_S(0xFD27),
    /** I8x16 greater than unsigned. */
    I8x16_GT_U(0xFD28),
    /** I8x16 less equal signed. */
    I8x16_LE_S(0xFD29),
    /** I8x16 less equal unsigned. */
    I8x16_LE_U(0xFD2A),
    /** I8x16 greater equal signed. */
    I8x16_GE_S(0xFD2B),
    /** I8x16 greater equal unsigned. */
    I8x16_GE_U(0xFD2C),
    /** I16x8 equals. */
    I16x8_EQ(0xFD2D),
    /** I16x8 not equals. */
    I16x8_NE(0xFD2E),
    /** I16x8 less than signed. */
    I16x8_LT_S(0xFD2F),
    /** I16x8 less than unsigned. */
    I16x8_LT_U(0xFD30),
    /** I16x8 greater than signed. */
    I16x8_GT_S(0xFD31),
    /** I16x8 greater than unsigned. */
    I16x8_GT_U(0xFD32),
    /** I16x8 less equal signed. */
    I16x8_LE_S(0xFD33),
    /** I16x8 less equal unsigned. */
    I16x8_LE_U(0xFD34),
    /** I16x8 greater equal signed. */
    I16x8_GE_S(0xFD35),
    /** I16x8 greater equal unsigned. */
    I16x8_GE_U(0xFD36),
    /** I32x4 equals. */
    I32x4_EQ(0xFD37),
    /** I32x4 not equals. */
    I32x4_NE(0xFD38),
    /** I32x4 less than signed. */
    I32x4_LT_S(0xFD39),
    /** I32x4 less than unsigned. */
    I32x4_LT_U(0xFD3A),
    /** I32x4 greater than signed. */
    I32x4_GT_S(0xFD3B),
    /** I32x4 greater than unsigned. */
    I32x4_GT_U(0xFD3C),
    /** I32x4 less equal signed. */
    I32x4_LE_S(0xFD3D),
    /** I32x4 less equal unsigned. */
    I32x4_LE_U(0xFD3E),
    /** I32x4 greater equal signed. */
    I32x4_GE_S(0xFD3F),
    /** I32x4 greater equal unsigned. */
    I32x4_GE_U(0xFD40),
    /** F32x4 equals. */
    F32x4_EQ(0xFD41),
    /** F32x4 not equals. */
    F32x4_NE(0xFD42),
    /** F32x4 less than. */
    F32x4_LT(0xFD43),
    /** F32x4 greater than. */
    F32x4_GT(0xFD44),
    /** F32x4 less equal. */
    F32x4_LE(0xFD45),
    /** F32x4 greater equal. */
    F32x4_GE(0xFD46),
    /** F64x2 equals. */
    F64x2_EQ(0xFD47),
    /** F64x2 not equals. */
    F64x2_NE(0xFD48),
    /** F64x2 less than. */
    F64x2_LT(0xFD49),
    /** F64x2 greater than. */
    F64x2_GT(0xFD4A),
    /** F64x2 less equal. */
    F64x2_LE(0xFD4B),
    /** F64x2 greater equal. */
    F64x2_GE(0xFD4C),
    /** V128 bitwise NOT. */
    V128_NOT(0xFD4D),
    /** V128 bitwise AND. */
    V128_AND(0xFD4E),
    /** V128 bitwise ANDNOT. */
    V128_ANDNOT(0xFD4F),
    /** V128 bitwise OR. */
    V128_OR(0xFD50),
    /** V128 bitwise XOR. */
    V128_XOR(0xFD51),
    /** V128 bitselect instruction. */
    V128_BITSELECT(0xFD52),
    /** V128 any true. */
    V128_ANY_TRUE(0xFD53),
    /** V128 load 8 lane. */
    V128_LOAD8_LANE(0xFD54, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 load 16 lane. */
    V128_LOAD16_LANE(0xFD55, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 load 32 lane. */
    V128_LOAD32_LANE(0xFD56, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 load 64 lane. */
    V128_LOAD64_LANE(0xFD57, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 store 8 lane. */
    V128_STORE8_LANE(0xFD58, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 store 16 lane. */
    V128_STORE16_LANE(0xFD59, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 store 32 lane. */
    V128_STORE32_LANE(0xFD5A, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 store 64 lane. */
    V128_STORE64_LANE(0xFD5B, List.of(VARUINT, VARUINT, VARUINT)),
    /** V128 load 32 zero. */
    V128_LOAD32_ZERO(0xFD5C, List.of(VARUINT, VARUINT)),
    /** V128 load 64 zero. */
    V128_LOAD64_ZERO(0xFD5D, List.of(VARUINT, VARUINT)),
    /** F32x4 demote low f64x2 zero. */
    F32x4_DEMOTE_LOW_F64x2_ZERO(0xFD5E),
    /** F64x2 promote low f32x4. */
    F64x2_PROMOTE_LOW_F32x4(0xFD5F),
    /** I8x16 absolute value. */
    I8x16_ABS(0xFD60),
    /** I8x16 negate. */
    I8x16_NEG(0xFD61),
    /** I8x16 population count. */
    I8x16_POPCNT(0xFD62),
    /** I8x16 all true. */
    I8x16_ALL_TRUE(0xFD63),
    /** I8x16 bitmask. */
    I8x16_BITMASK(0xFD64),
    /** I8x16 narrow i16x8 signed. */
    I8x16_NARROW_I16x8_S(0xFD65),
    /** I8x16 narrow i16x8 unsigned. */
    I8x16_NARROW_I16x8_U(0xFD66),
    /** F32x4 ceiling. */
    F32x4_CEIL(0xFD67),
    /** F32x4 floor. */
    F32x4_FLOOR(0xFD68),
    /** F32x4 truncate. */
    F32x4_TRUNC(0xFD69),
    /** F32x4 nearest integer. */
    F32x4_NEAREST(0xFD6A),
    /** I8x16 shift left. */
    I8x16_SHL(0xFD6B),
    /** I8x16 shift right signed. */
    I8x16_SHR_S(0xFD6C),
    /** I8x16 shift right unsigned. */
    I8x16_SHR_U(0xFD6D),
    /** I8x16 add. */
    I8x16_ADD(0xFD6E),
    /** I8x16 add saturate signed. */
    I8x16_ADD_SAT_S(0xFD6F),
    /** I8x16 add saturate unsigned. */
    I8x16_ADD_SAT_U(0xFD70),
    /** I8x16 subtract. */
    I8x16_SUB(0xFD71),
    /** I8x16 subtract saturate signed. */
    I8x16_SUB_SAT_S(0xFD72),
    /** I8x16 subtract saturate unsigned. */
    I8x16_SUB_SAT_U(0xFD73),
    /** F64x2 ceiling. */
    F64x2_CEIL(0xFD74),
    /** F64x2 floor. */
    F64x2_FLOOR(0xFD75),
    /** I8x16 minimum signed. */
    I8x16_MIN_S(0xFD76),
    /** I8x16 minimum unsigned. */
    I8x16_MIN_U(0xFD77),
    /** I8x16 maximum signed. */
    I8x16_MAX_S(0xFD78),
    /** I8x16 maximum unsigned. */
    I8x16_MAX_U(0xFD79),
    /** F64x2 truncate. */
    F64x2_TRUNC(0xFD7A),
    /** I8x16 average unsigned. */
    I8x16_AVGR_U(0xFD7B),
    /** I16x8 extadd pairwise i8x16 s. */
    I16x8_EXTADD_PAIRWISE_I8x16_S(0xFD7C),
    /** I16x8 extadd pairwise i8x16 u. */
    I16x8_EXTADD_PAIRWISE_I8x16_U(0xFD7D),
    /** I32x4 extadd pairwise i16x8 s. */
    I32x4_EXTADD_PAIRWISE_I16x8_S(0xFD7E),
    /** I32x4 extadd pairwise i16x8 u. */
    I32x4_EXTADD_PAIRWISE_I16x8_U(0xFD7F),
    /** I16x8 absolute value. */
    I16x8_ABS(0xFD80),
    /** I16x8 negate. */
    I16x8_NEG(0xFD81),
    /** I16x8 Q15 mulr sat s. */
    I16x8_Q15MULR_SAT_S(0xFD82),
    /** I16x8 all true. */
    I16x8_ALL_TRUE(0xFD83),
    /** I16x8 bitmask. */
    I16x8_BITMASK(0xFD84),
    /** I16x8 narrow i32x4 signed. */
    I16x8_NARROW_I32x4_S(0xFD85),
    /** I16x8 narrow i32x4 unsigned. */
    I16x8_NARROW_I32x4_U(0xFD86),
    /** I16x8 extend low i8x16 s. */
    I16x8_EXTEND_LOW_I8x16_S(0xFD87),
    /** I16x8 extend high i8x16 s. */
    I16x8_EXTEND_HIGH_I8x16_S(0xFD88),
    /** I16x8 extend low i8x16 u. */
    I16x8_EXTEND_LOW_I8x16_U(0xFD89),
    /** I16x8 extend high i8x16 u. */
    I16x8_EXTEND_HIGH_I8x16_U(0xFD8A),
    /** I16x8 shift left. */
    I16x8_SHL(0xFD8B),
    /** I16x8 shift right signed. */
    I16x8_SHR_S(0xFD8C),
    /** I16x8 shift right unsigned. */
    I16x8_SHR_U(0xFD8D),
    /** I16x8 add. */
    I16x8_ADD(0xFD8E),
    /** I16x8 add saturate signed. */
    I16x8_ADD_SAT_S(0xFD8F),
    /** I16x8 add saturate unsigned. */
    I16x8_ADD_SAT_U(0xFD90),
    /** I16x8 subtract. */
    I16x8_SUB(0xFD91),
    /** I16x8 subtract saturate signed. */
    I16x8_SUB_SAT_S(0xFD92),
    /** I16x8 subtract saturate unsigned. */
    I16x8_SUB_SAT_U(0xFD93),
    /** F64x2 nearest integer. */
    F64x2_NEAREST(0xFD94),
    /** I16x8 multiply. */
    I16x8_MUL(0xFD95),
    /** I16x8 minimum signed. */
    I16x8_MIN_S(0xFD96),
    /** I16x8 minimum unsigned. */
    I16x8_MIN_U(0xFD97),
    /** I16x8 maximum signed. */
    I16x8_MAX_S(0xFD98),
    /** I16x8 maximum unsigned. */
    I16x8_MAX_U(0xFD99),
    /** I16x8 average unsigned. */
    I16x8_AVGR_U(0xFD9B),
    /** I16x8 extmul low i8x16 s. */
    I16x8_EXTMUL_LOW_I8x16_S(0xFD9C),
    /** I16x8 extmul high i8x16 s. */
    I16x8_EXTMUL_HIGH_I8x16_S(0xFD9D),
    /** I16x8 extmul low i8x16 u. */
    I16x8_EXTMUL_LOW_I8x16_U(0xFD9E),
    /** I16x8 extmul high i8x16 u. */
    I16x8_EXTMUL_HIGH_I8x16_U(0xFD9F),
    /** I32x4 absolute value. */
    I32x4_ABS(0xFDA0),
    /** I32x4 negate. */
    I32x4_NEG(0xFDA1),
    /** I32x4 all true. */
    I32x4_ALL_TRUE(0xFDA3),
    /** I32x4 bitmask. */
    I32x4_BITMASK(0xFDA4),
    /** I32x4 extend low i16x8 s. */
    I32x4_EXTEND_LOW_I16x8_S(0xFDA7),
    /** I32x4 extend high i16x8 s. */
    I32x4_EXTEND_HIGH_I16x8_S(0xFDA8),
    /** I32x4 extend low i16x8 u. */
    I32x4_EXTEND_LOW_I16x8_U(0xFDA9),
    /** I32x4 extend high i16x8 u. */
    I32x4_EXTEND_HIGH_I16x8_U(0xFDAA),
    /** I32x4 shift left. */
    I32x4_SHL(0xFDAB),
    /** I32x4 shift right signed. */
    I32x4_SHR_S(0xFDAC),
    /** I32x4 shift right unsigned. */
    I32x4_SHR_U(0xFDAD),
    /** I32x4 add. */
    I32x4_ADD(0xFDAE),
    /** I32x4 subtract. */
    I32x4_SUB(0xFDB1),
    /** I32x4 multiply. */
    I32x4_MUL(0xFDB5),
    /** I32x4 minimum signed. */
    I32x4_MIN_S(0xFDB6),
    /** I32x4 minimum unsigned. */
    I32x4_MIN_U(0xFDB7),
    /** I32x4 maximum signed. */
    I32x4_MAX_S(0xFDB8),
    /** I32x4 maximum unsigned. */
    I32x4_MAX_U(0xFDB9),
    /** I32x4 dot product i16x8 signed. */
    I32x4_DOT_I16x8_S(0xFDBA),
    /** I32x4 extmul low i16x8 s. */
    I32x4_EXTMUL_LOW_I16x8_S(0xFDBC),
    /** I32x4 extmul high i16x8 s. */
    I32x4_EXTMUL_HIGH_I16x8_S(0xFDBD),
    /** I32x4 extmul low i16x8 u. */
    I32x4_EXTMUL_LOW_I16x8_U(0xFDBE),
    /** I32x4 extmul high i16x8 u. */
    I32x4_EXTMUL_HIGH_I16x8_U(0xFDBF),
    /** I64x2 absolute value. */
    I64x2_ABS(0xFDC0),
    /** I64x2 negate. */
    I64x2_NEG(0xFDC1),
    /** I64x2 all true. */
    I64x2_ALL_TRUE(0xFDC3),
    /** I64x2 bitmask. */
    I64x2_BITMASK(0xFDC4),
    /** I64x2 extend low i32x4 s. */
    I64x2_EXTEND_LOW_I32x4_S(0xFDC7),
    /** I64x2 extend high i32x4 s. */
    I64x2_EXTEND_HIGH_I32x4_S(0xFDC8),
    /** I64x2 extend low i32x4 u. */
    I64x2_EXTEND_LOW_I32x4_U(0xFDC9),
    /** I64x2 extend high i32x4 u. */
    I64x2_EXTEND_HIGH_I32x4_U(0xFDCA),
    /** I64x2 shift left. */
    I64x2_SHL(0xFDCB),
    /** I64x2 shift right signed. */
    I64x2_SHR_S(0xFDCC),
    /** I64x2 shift right unsigned. */
    I64x2_SHR_U(0xFDCD),
    /** I64x2 add. */
    I64x2_ADD(0xFDCE),
    /** I64x2 subtract. */
    I64x2_SUB(0xFDD1),
    /** I64x2 multiply. */
    I64x2_MUL(0xFDD5),
    /** I64x2 equals. */
    I64x2_EQ(0xFDD6),
    /** I64x2 not equals. */
    I64x2_NE(0xFDD7),
    /** I64x2 less than signed. */
    I64x2_LT_S(0xFDD8),
    /** I64x2 greater than signed. */
    I64x2_GT_S(0xFDD9),
    /** I64x2 less equal signed. */
    I64x2_LE_S(0xFDDA),
    /** I64x2 greater equal signed. */
    I64x2_GE_S(0xFDDB),
    /** I64x2 extmul low i32x4 s. */
    I64x2_EXTMUL_LOW_I32x4_S(0xFDDC),
    /** I64x2 extmul high i32x4 s. */
    I64x2_EXTMUL_HIGH_I32x4_S(0xFDDD),
    /** I64x2 extmul low i32x4 u. */
    I64x2_EXTMUL_LOW_I32x4_U(0xFDDE),
    /** I64x2 extmul high i32x4 u. */
    I64x2_EXTMUL_HIGH_I32x4_U(0xFDDF),
    /** F32x4 absolute value. */
    F32x4_ABS(0xFDE0),
    /** F32x4 negate. */
    F32x4_NEG(0xFDE1),
    /** F32x4 square root. */
    F32x4_SQRT(0xFDE3),
    /** F32x4 add. */
    F32x4_ADD(0xFDE4),
    /** F32x4 subtract. */
    F32x4_SUB(0xFDE5),
    /** F32x4 multiply. */
    F32x4_MUL(0xFDE6),
    /** F32x4 divide. */
    F32x4_DIV(0xFDE7),
    /** F32x4 minimum. */
    F32x4_MIN(0xFDE8),
    /** F32x4 maximum. */
    F32x4_MAX(0xFDE9),
    /** F32x4 parallel minimum. */
    F32x4_PMIN(0xFDEA),
    /** F32x4 parallel maximum. */
    F32x4_PMAX(0xFDEB),
    /** F64x2 absolute value. */
    F64x2_ABS(0xFDEC),
    /** F64x2 negate. */
    F64x2_NEG(0xFDED),
    /** F64x2 square root. */
    F64x2_SQRT(0xFDEF),
    /** F64x2 add. */
    F64x2_ADD(0xFDF0),
    /** F64x2 subtract. */
    F64x2_SUB(0xFDF1),
    /** F64x2 multiply. */
    F64x2_MUL(0xFDF2),
    /** F64x2 divide. */
    F64x2_DIV(0xFDF3),
    /** F64x2 minimum. */
    F64x2_MIN(0xFDF4),
    /** F64x2 maximum. */
    F64x2_MAX(0xFDF5),
    /** F64x2 parallel minimum. */
    F64x2_PMIN(0xFDF6),
    /** F64x2 parallel maximum. */
    F64x2_PMAX(0xFDF7),
    /** I32x4 trunc sat f32x4 s. */
    I32x4_TRUNC_SAT_F32X4_S(0xFDF8),
    /** I32x4 trunc sat f32x4 u. */
    I32x4_TRUNC_SAT_F32X4_U(0xFDF9),
    /** F32x4 convert i32x4 signed. */
    F32x4_CONVERT_I32x4_S(0xFDFA),
    /** F32x4 convert i32x4 unsigned. */
    F32x4_CONVERT_I32x4_U(0xFDFB),
    /** I32x4 trunc sat f64x2 s zero. */
    I32x4_TRUNC_SAT_F64x2_S_ZERO(0xFDFC),
    /** I32x4 trunc sat f64x2 u zero. */
    I32x4_TRUNC_SAT_F64x2_U_ZERO(0xFDFD),
    /** F64x2 convert low i32x4 s. */
    F64x2_CONVERT_LOW_I32x4_S(0xFDFE),
    /** F64x2 convert low i32x4 u. */
    F64x2_CONVERT_LOW_I32x4_U(0xFDFF),
    ;

    private static final int OP_CODES_SIZE = 0xFF00;

    // trick: the enum constructor cannot access its own static fields
    // but can access another class
    private static final class OpCodes {
        private OpCodes() {}

        private static final OpCode[] byOpCode = new OpCode[OP_CODES_SIZE];
        private static final List<WasmEncoding>[] signatures = new List[OP_CODES_SIZE];
    }

    private final int opcode;

    OpCode(int opcode) {
        this(opcode, List.of());
    }

    OpCode(int opcode, List<WasmEncoding> signature) {
        this.opcode = opcode;
        OpCodes.byOpCode[opcode] = this;
        OpCodes.signatures[opcode] = signature;
    }

    /**
     * Returns the numeric byte code (or multi-byte code for some prefixed instructions) for this opcode.
     *
     * @return the opcode value.
     */
    public int opcode() {
        return opcode;
    }

    /**
     * Retrieves the {@link OpCode} enum constant corresponding to the given numeric opcode value.
     *
     * @param opcode the numeric opcode value.
     * @return the corresponding {@link OpCode} constant, or {@code null} if the opcode is invalid or unassigned.
     */
    public static OpCode byOpCode(int opcode) {
        return OpCodes.byOpCode[opcode];
    }

    /**
     * Retrieves the signature (list of expected operand encodings) for the given opcode.
     *
     * @param opcode the {@link OpCode} constant.
     * @return an unmodifiable {@link List} of {@link WasmEncoding} values representing the expected operand types, or an empty list if the opcode takes no immediate operands.
     */
    public static List<WasmEncoding> signature(OpCode opcode) {
        return OpCodes.signatures[opcode.opcode()];
    }
}
