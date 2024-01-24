package com.dylibso.chicory.wasm.types;

public class ExportDesc {
    private final long index;
    private final ExportDescType type;

    public ExportDesc(long index, ExportDescType type) {
        this.index = index;
        this.type = type;
    }

    public long index() {
        return index;
    }

    public ExportDescType type() {
        return type;
    }
}
