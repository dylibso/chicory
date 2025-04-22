package com.dylibso.chicory.wasm;

// Spec: https://webassembly.github.io/spec/core/appendix/implementation.html#syntactic-limits
// From: https://github.com/WebKit/webkit/blob/main/Source/JavaScriptCore/wasm/WasmLimits.h
/**
 * Defines various limits related to WebAssembly structures and execution,
 * based on the specification and common implementation constraints.
 * See <a href="https://webassembly.github.io/spec/core/appendix/implementation.html#syntactic-limits">Syntactic Limits</a>
 * and potentially implementation-specific limits (e.g., from WebKit).
 */
public final class WasmLimits {

    /** Maximum number of types allowed in a module. */
    public static final int MAX_TYPES = 1000000;

    /** Maximum number of functions allowed in a module. */
    public static final int MAX_FUNCTIONS = 1000000;

    /** Maximum number of imports allowed in a module. */
    public static final int MAX_IMPORTS = 100000;

    /** Maximum number of exports allowed in a module. */
    public static final int MAX_EXPORTS = 100000;

    /** Maximum number of exceptions (tags) allowed in a module. */
    public static final int MAX_EXCEPTIONS = 100000;

    /** Maximum number of globals allowed in a module. */
    public static final int MAX_GLOBALS = 1000000;

    /** Maximum number of data segments allowed in a module. */
    public static final int MAX_DATA_SEGMENTS = 100000;

    /** Maximum number of fields allowed in a struct type. */
    public static final int MAX_STRUCT_FIELD_COUNT = 10000;

    /** Maximum number of arguments for the `array.new_fixed` instruction. */
    public static final int MAX_ARRAY_NEW_FIXED_ARGS = 10000;

    /** Maximum number of types within a single recursion group. */
    public static final int MAX_RECURSION_GROUP_COUNT = 1000000;

    /** Maximum number of recursion groups allowed. */
    public static final int MAX_NUMBER_OF_RECURSION_GROUPS = 1000000;

    /** Maximum number of supertypes a subtype can declare. */
    public static final int MAX_SUBTYPE_SUPERTYPE_COUNT = 1;

    /** Maximum depth allowed in the subtype hierarchy. */
    public static final int MAX_SUBTYPE_DEPTH = 63;

    /** Maximum size (in bytes) of a string literal. */
    public static final int MAX_STRING_SIZE = 100000;

    /** Maximum size (in bytes) of a Wasm module. */
    public static final int MAX_MODULE_SIZE = 1024 * 1024 * 1024; // 1 GiB

    /** Maximum size (in bytes) of a single function's code. */
    public static final int MAX_FUNCTION_SIZE = 7654321;

    /** Maximum number of local variables allowed in a function. */
    public static final int MAX_FUNCTION_LOCALS = 50000;

    /** Maximum number of parameters allowed for a function. */
    public static final int MAX_FUNCTION_PARAMS = 1000;

    /** Maximum number of return values allowed for a function. */
    public static final int MAX_FUNCTION_RETURNS = 1000;

    /** Maximum number of entries allowed in a table. */
    public static final int MAX_TABLE_ENTRIES = 10000000;

    /** Maximum number of tables allowed in a module. */
    public static final int MAX_TABLES = 1000000;

    private WasmLimits() {}
}
