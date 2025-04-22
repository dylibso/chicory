package com.dylibso.chicory.wasm.types;

/**
 * Defines the standard numerical identifiers for WebAssembly sections.
 */
public final class SectionId {
    /** Identifier for the Custom Section (ID 0). */
    public static final int CUSTOM = 0;

    /** Identifier for the Type Section (ID 1). */
    public static final int TYPE = 1;

    /** Identifier for the Import Section (ID 2). */
    public static final int IMPORT = 2;

    /** Identifier for the Function Section (ID 3). */
    public static final int FUNCTION = 3;

    /** Identifier for the Table Section (ID 4). */
    public static final int TABLE = 4;

    /** Identifier for the Memory Section (ID 5). */
    public static final int MEMORY = 5;

    /** Identifier for the Global Section (ID 6). */
    public static final int GLOBAL = 6;

    /** Identifier for the Export Section (ID 7). */
    public static final int EXPORT = 7;

    /** Identifier for the Start Section (ID 8). */
    public static final int START = 8;

    /** Identifier for the Element Section (ID 9). */
    public static final int ELEMENT = 9;

    /** Identifier for the Code Section (ID 10). */
    public static final int CODE = 10;

    /** Identifier for the Data Section (ID 11). */
    public static final int DATA = 11;

    /** Identifier for the Data Count Section (ID 12). */
    public static final int DATA_COUNT = 12;

    /** Identifier for the Tag Section (ID 13), part of the Exception Handling proposal. */
    public static final int TAG = 13;

    private SectionId() {}
}
