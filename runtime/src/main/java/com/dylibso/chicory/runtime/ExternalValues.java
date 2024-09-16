package com.dylibso.chicory.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalValues {
    private static final ExternalFunction[] NO_EXTERNAL_FUNCTIONS = new ExternalFunction[0];
    private static final ExternalGlobal[] NO_EXTERNAL_GLOBALS = new ExternalGlobal[0];
    private static final ExternalMemory[] NO_EXTERNAL_MEMORIES = new ExternalMemory[0];
    private static final ExternalTable[] NO_EXTERNAL_TABLES = new ExternalTable[0];

    private final ExternalFunction[] functions;
    private final ExternalGlobal[] globals;
    private final ExternalMemory[] memories;
    private final ExternalTable[] tables;

    public ExternalValues() {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
    }

    public ExternalValues(HostFunction[] functions) {
        this.functions = functions.clone();
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
    }

    public ExternalValues(ExternalGlobal[] globals) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = globals.clone();
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
    }

    public ExternalValues(ExternalMemory[] memories) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = memories.clone();
        this.tables = NO_EXTERNAL_TABLES;
    }

    public ExternalValues(ExternalMemory memory) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = new ExternalMemory[] {memory};
        this.tables = NO_EXTERNAL_TABLES;
    }

    public ExternalValues(ExternalTable[] tables) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = tables.clone();
    }

    public ExternalValues(
            ExternalFunction[] functions,
            ExternalGlobal[] globals,
            ExternalMemory memory,
            ExternalTable[] tables) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = new ExternalMemory[] {memory};
        this.tables = tables.clone();
    }

    public ExternalValues(
            ExternalFunction[] functions,
            ExternalGlobal[] globals,
            ExternalMemory[] memories,
            ExternalTable[] tables) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = memories.clone();
        this.tables = tables.clone();
    }

    public ExternalFunction[] functions() {
        return functions.clone();
    }

    public int functionCount() {
        return functions.length;
    }

    public ExternalFunction function(int idx) {
        return functions[idx];
    }

    public ExternalGlobal[] globals() {
        return globals;
    }

    public int globalCount() {
        return globals.length;
    }

    public ExternalGlobal global(int idx) {
        return globals[idx];
    }

    public ExternalMemory[] memories() {
        return memories;
    }

    public int memoryCount() {
        return memories.length;
    }

    public ExternalMemory memory(int idx) {
        return memories[idx];
    }

    public ExternalTable[] tables() {
        return tables;
    }

    public int tableCount() {
        return tables.length;
    }

    public ExternalTable table(int idx) {
        return tables[idx];
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExternalValues empty() {
        return new Builder().build();
    }

    public static final class Builder {
        private List<ExternalFunction> functions;
        private List<ExternalGlobal> globals;
        private List<ExternalMemory> memories;
        private List<ExternalTable> tables;

        Builder() {}

        public Builder withFunctions(List<ExternalFunction> functions) {
            this.functions = functions;
            return this;
        }

        public Builder addFunction(ExternalFunction... function) {
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            Collections.addAll(this.functions, function);
            return this;
        }

        public Builder withGlobals(List<ExternalGlobal> globals) {
            this.globals = globals;
            return this;
        }

        public Builder addGlobal(ExternalGlobal... global) {
            if (this.globals == null) {
                this.globals = new ArrayList<>();
            }
            Collections.addAll(this.globals, global);
            return this;
        }

        public Builder withMemories(List<ExternalMemory> memories) {
            this.memories = memories;
            return this;
        }

        public Builder addMemory(ExternalMemory... memory) {
            if (this.memories == null) {
                this.memories = new ArrayList<>();
            }
            Collections.addAll(this.memories, memory);
            return this;
        }

        public Builder withTables(List<ExternalTable> tables) {
            this.tables = tables;
            return this;
        }

        public Builder addTable(ExternalTable... table) {
            if (this.tables == null) {
                this.tables = new ArrayList<>();
            }
            Collections.addAll(this.tables, table);
            return this;
        }

        public ExternalValues build() {
            final ExternalValues externalValues =
                    new ExternalValues(
                            functions == null
                                    ? new HostFunction[0]
                                    : functions.toArray(new HostFunction[0]),
                            globals == null
                                    ? new ExternalGlobal[0]
                                    : globals.toArray(new ExternalGlobal[0]),
                            memories == null
                                    ? new ExternalMemory[0]
                                    : memories.toArray(new ExternalMemory[0]),
                            tables == null
                                    ? new ExternalTable[0]
                                    : tables.toArray(new ExternalTable[0]));
            return externalValues;
        }
    }
}
