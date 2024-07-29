package com.dylibso.chicory.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HostImports {
    private static final HostFunction[] NO_HOST_FUNCTIONS = new HostFunction[0];
    private static final HostGlobal[] NO_HOST_GLOBALS = new HostGlobal[0];
    private static final HostMemory[] NO_HOST_MEMORIES = new HostMemory[0];
    private static final HostTable[] NO_HOST_TABLES = new HostTable[0];

    private final HostFunction[] functions;
    private final HostGlobal[] globals;
    private final HostMemory[] memories;
    private final HostTable[] tables;

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

    public HostImports(
            HostFunction[] functions, HostGlobal[] globals, HostMemory memory, HostTable[] tables) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = new HostMemory[] {memory};
        this.tables = tables.clone();
    }

    public HostImports(
            HostFunction[] functions,
            HostGlobal[] globals,
            HostMemory[] memories,
            HostTable[] tables) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static HostImports empty() {
        return new Builder().build();
    }

    public static final class Builder {
        private List<HostFunction> functions;
        private List<HostGlobal> globals;
        private List<HostMemory> memories;
        private List<HostTable> tables;

        Builder() {}

        public Builder withFunctions(List<HostFunction> functions) {
            this.functions = functions;
            return this;
        }

        public Builder addFunction(HostFunction... function) {
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            Collections.addAll(this.functions, function);
            return this;
        }

        public Builder withGlobals(List<HostGlobal> globals) {
            this.globals = globals;
            return this;
        }

        public Builder addGlobal(HostGlobal... global) {
            if (this.globals == null) {
                this.globals = new ArrayList<>();
            }
            Collections.addAll(this.globals, global);
            return this;
        }

        public Builder withMemories(List<HostMemory> memories) {
            this.memories = memories;
            return this;
        }

        public Builder addMemory(HostMemory... memory) {
            if (this.memories == null) {
                this.memories = new ArrayList<>();
            }
            Collections.addAll(this.memories, memory);
            return this;
        }

        public Builder withTables(List<HostTable> tables) {
            this.tables = tables;
            return this;
        }

        public Builder addTable(HostTable... table) {
            if (this.tables == null) {
                this.tables = new ArrayList<>();
            }
            Collections.addAll(this.tables, table);
            return this;
        }

        public HostImports build() {
            final HostImports hostImports =
                    new HostImports(
                            functions == null
                                    ? new HostFunction[0]
                                    : functions.toArray(new HostFunction[0]),
                            globals == null
                                    ? new HostGlobal[0]
                                    : globals.toArray(new HostGlobal[0]),
                            memories == null
                                    ? new HostMemory[0]
                                    : memories.toArray(new HostMemory[0]),
                            tables == null ? new HostTable[0] : tables.toArray(new HostTable[0]));
            return hostImports;
        }
    }
}
