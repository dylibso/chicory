package com.dylibso.chicory.wasm.types;

public final class SectionId {
    public static final int CUSTOM = 0;
    public static final int TYPE = 1;
    public static final int IMPORT = 2;
    public static final int FUNCTION = 3;
    public static final int TABLE = 4;
    public static final int MEMORY = 5;
    public static final int GLOBAL = 6;
    public static final int EXPORT = 7;
    public static final int START = 8;
    public static final int ELEMENT = 9;
    public static final int CODE = 10;
    public static final int DATA = 11;
    public static final int DATA_COUNT = 12;
    public static final int TAG = 13;

    private SectionId() {}
}
