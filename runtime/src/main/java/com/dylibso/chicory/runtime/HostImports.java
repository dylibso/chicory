package com.dylibso.chicory.runtime;

public class HostImports {
    private final HostFunction[] functions;
    private final HostGlobal[] globals;
    private final HostMemory[] memories;
    private final HostTable[] tables;

    public HostImports() {
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostFunction[] functions) {
        this.functions = functions;
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostGlobal[] globals) {
        this.functions = new HostFunction[] {};
        this.globals = globals;
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostMemory[] memories) {
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = memories;
        this.tables = new HostTable[] {};
    }

    public HostImports(HostTable[] tables) {
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = tables;
    }

    public HostImports(HostFunction[] functions, HostGlobal[] globals, HostMemory[] memories, HostTable[] tables) {
        this.functions = functions;
        this.globals = globals;
        this.memories = memories;
        this.tables = tables;
    }

    public HostFunction[] getFunctions() {
        return functions;
    }

    public HostGlobal[] getGlobals() {
        return globals;
    }

    public HostMemory[] getMemories() {
        return memories;
    }

    public HostTable[] getTables() {
        return tables;
    }
}
