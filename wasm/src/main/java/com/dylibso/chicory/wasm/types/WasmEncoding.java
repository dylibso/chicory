package com.dylibso.chicory.wasm.types;

/**
 * Represents the different binary encodings used for operands in WebAssembly instructions.
 * This helps the parser determine how to read the immediate arguments following an opcode.
 */
public enum WasmEncoding {
    /** Unsigned LEB128 encoded integer. */
    VARUINT,
    /** Signed LEB128 encoded 32-bit integer. */
    VARSINT32,
    /** Signed LEB128 encoded 64-bit integer. */
    VARSINT64,
    /** 32-bit float encoded as 4 bytes (little-endian). */
    FLOAT32,
    /** 64-bit float encoded as 8 bytes (little-endian). */
    FLOAT64,
    /** A vector (count followed by elements) of VARUINT encoded integers. Used for br_table targets. */
    VEC_VARUINT,
    /** A vector (count followed by elements) representing catch clauses for try_table. */
    VEC_CATCH,
    /** A single byte. */
    BYTE,
    /** 128-bit vector value encoded as 16 bytes (little-endian). */
    V128
}
