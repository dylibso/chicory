package com.dylibso.chicory.runtime;

public class HostImports {
    private static final HostFunction[] NO_HOST_FUNCTIONS = new HostFunction[0];
    private static final HostGlobal[] NO_HOST_GLOBALS = new HostGlobal[0];
    private static final HostMemory[] NO_HOST_MEMORIES = new HostMemory[0];
    private static final HostTable[] NO_HOST_TABLES = new HostTable[0];

    private final HostFunction[] functions;
    private final HostGlobal[] globals;
    private final HostMemory[] memories;
    private final HostTable[] tables;
    private FromHost[] index;

    public HostImports() {
        this.functions = NO_HOST_FUNCTIONS;
        this.globals = NO_HOST_GLOBALS;
        this.memories = NO_HOST_MEMORIES;
        this.tables = NO_HOST_TABLES;
    }

    public HostImports(HostFunction[] functions) {
        this.functions = functions.clone();
        this.globals = NO_HOST_GLOBALS;
        this.memories = NO_HOST_MEMORIES;
        this.tables = NO_HOST_TABLES;
    }

    public HostImports(HostGlobal[] globals) {
        this.functions = NO_HOST_FUNCTIONS;
        this.globals = globals.clone();
        this.memories = NO_HOST_MEMORIES;
        this.tables = NO_HOST_TABLES;
    }

    public HostImports(HostMemory[] memories) {
        this.functions = NO_HOST_FUNCTIONS;
        this.globals = NO_HOST_GLOBALS;
        this.memories = memories.clone();
        this.tables = NO_HOST_TABLES;
    }

    public HostImports(HostMemory memory) {
        this.functions = NO_HOST_FUNCTIONS;
        this.globals = NO_HOST_GLOBALS;
        this.memories = new HostMemory[] {memory};
        this.tables = NO_HOST_TABLES;
    }

    public HostImports(HostTable[] tables) {
        this.functions = NO_HOST_FUNCTIONS;
        this.globals = NO_HOST_GLOBALS;
        this.memories = NO_HOST_MEMORIES;
        this.tables = tables.clone();
    }

    public HostImports(HostFunction[] functions, HostGlobal[] globals, HostMemory memory, HostTable[] tables) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = new HostMemory[] {memory};
        this.tables = tables.clone();
    }

    public HostImports(HostFunction[] functions, HostGlobal[] globals, HostMemory[] memories, HostTable[] tables) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = memories.clone();
        this.tables = tables.clone();
    }

    public HostFunction[] functions() {
        return functions.clone();
    }

    public int functionCount() {
        return functions.length;
    }

    public HostFunction function(int idx) {
        return functions[idx];
    }

    public HostGlobal[] globals() {
        return globals;
    }

    public int globalCount() {
        return globals.length;
    }

    public HostGlobal global(int idx) {
        return globals[idx];
    }

    public HostMemory[] memories() {
        return memories;
    }

    public int memoryCount() {
        return memories.length;
    }

    public HostMemory memory(int idx) {
        return memories[idx];
    }

    public HostTable[] tables() {
        return tables;
    }

    public int tableCount() {
        return tables.length;
    }

    public HostTable table(int idx) {
        return tables[idx];
    }

    public FromHost[] index() {
        return index;
    }

    public void setIndex(FromHost[] index) {
        this.index = index;
    }
}
