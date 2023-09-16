package com.dylibso.chicory.wasm.types;

public class Export {
    private String name;
    private ExportDesc desc;

    public Export(String name, ExportDesc desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public ExportDesc getDesc() {
        return desc;
    }
}
