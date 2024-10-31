package com.dylibso.chicory.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportValues {
    private static final ImportFunction[] NO_EXTERNAL_FUNCTIONS = new ImportFunction[0];
    private static final ImportGlobal[] NO_EXTERNAL_GLOBALS = new ImportGlobal[0];
    private static final ImportMemory[] NO_EXTERNAL_MEMORIES = new ImportMemory[0];
    private static final ImportTable[] NO_EXTERNAL_TABLES = new ImportTable[0];
    private static final ImportTag[] NO_EXTERNAL_TAGS = new ImportTag[0];

    private final ImportFunction[] functions;
    private final ImportGlobal[] globals;
    private final ImportMemory[] memories;
    private final ImportTable[] tables;
    private final ImportTag[] tags;

    public ImportValues() {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
        this.tags = NO_EXTERNAL_TAGS;
    }

    public ImportValues(HostFunction[] functions) {
        this.functions = functions.clone();
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
        this.tags = NO_EXTERNAL_TAGS;
    }

    public ImportValues(ImportGlobal[] globals) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = globals.clone();
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
        this.tags = NO_EXTERNAL_TAGS;
    }

    public ImportValues(ImportMemory[] memories) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = memories.clone();
        this.tables = NO_EXTERNAL_TABLES;
        this.tags = NO_EXTERNAL_TAGS;
    }

    public ImportValues(ImportMemory memory) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = new ImportMemory[] {memory};
        this.tables = NO_EXTERNAL_TABLES;
        this.tags = NO_EXTERNAL_TAGS;
    }

    public ImportValues(ImportTable[] tables) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = tables.clone();
        this.tags = NO_EXTERNAL_TAGS;
    }

    public ImportValues(ImportTag[] tags) {
        this.functions = NO_EXTERNAL_FUNCTIONS;
        this.globals = NO_EXTERNAL_GLOBALS;
        this.memories = NO_EXTERNAL_MEMORIES;
        this.tables = NO_EXTERNAL_TABLES;
        this.tags = tags.clone();
    }

    public ImportValues(
            ImportFunction[] functions,
            ImportGlobal[] globals,
            ImportMemory memory,
            ImportTable[] tables,
            ImportTag[] tags) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = new ImportMemory[] {memory};
        this.tables = tables.clone();
        this.tags = tags.clone();
    }

    public ImportValues(
            ImportFunction[] functions,
            ImportGlobal[] globals,
            ImportMemory[] memories,
            ImportTable[] tables,
            ImportTag[] tags) {
        this.functions = functions.clone();
        this.globals = globals.clone();
        this.memories = memories.clone();
        this.tables = tables.clone();
        this.tags = tags.clone();
    }

    public ImportFunction[] functions() {
        return functions.clone();
    }

    public int functionCount() {
        return functions.length;
    }

    public ImportFunction function(int idx) {
        return functions[idx];
    }

    public ImportGlobal[] globals() {
        return globals;
    }

    public int globalCount() {
        return globals.length;
    }

    public ImportGlobal global(int idx) {
        return globals[idx];
    }

    public ImportMemory[] memories() {
        return memories;
    }

    public int memoryCount() {
        return memories.length;
    }

    public ImportMemory memory(int idx) {
        return memories[idx];
    }

    public ImportTable[] tables() {
        return tables;
    }

    public int tableCount() {
        return tables.length;
    }

    public ImportTable table(int idx) {
        return tables[idx];
    }

    public ImportTag[] tags() {
        return tags;
    }

    public int tagCount() {
        return tags.length;
    }

    public ImportTag tag(int idx) {
        return tags[idx];
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ImportValues empty() {
        return new Builder().build();
    }

    public static final class Builder {
        private List<ImportFunction> functions;
        private List<ImportGlobal> globals;
        private List<ImportMemory> memories;
        private List<ImportTable> tables;
        private List<ImportTag> tags;

        Builder() {}

        public Builder withFunctions(List<ImportFunction> functions) {
            this.functions = functions;
            return this;
        }

        public Builder addFunction(ImportFunction... function) {
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            Collections.addAll(this.functions, function);
            return this;
        }

        public Builder withGlobals(List<ImportGlobal> globals) {
            this.globals = globals;
            return this;
        }

        public Builder addGlobal(ImportGlobal... global) {
            if (this.globals == null) {
                this.globals = new ArrayList<>();
            }
            Collections.addAll(this.globals, global);
            return this;
        }

        public Builder withMemories(List<ImportMemory> memories) {
            this.memories = memories;
            return this;
        }

        public Builder addMemory(ImportMemory... memory) {
            if (this.memories == null) {
                this.memories = new ArrayList<>();
            }
            Collections.addAll(this.memories, memory);
            return this;
        }

        public Builder withTables(List<ImportTable> tables) {
            this.tables = tables;
            return this;
        }

        public Builder addTable(ImportTable... table) {
            if (this.tables == null) {
                this.tables = new ArrayList<>();
            }
            Collections.addAll(this.tables, table);
            return this;
        }

        public Builder withTags(List<ImportTag> tags) {
            this.tags = tags;
            return this;
        }

        public Builder addTag(ImportTag... tag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            Collections.addAll(this.tags, tag);
            return this;
        }

        public ImportValues build() {
            final ImportValues importValues =
                    new ImportValues(
                            functions == null
                                    ? new HostFunction[0]
                                    : functions.toArray(new HostFunction[0]),
                            globals == null
                                    ? new ImportGlobal[0]
                                    : globals.toArray(new ImportGlobal[0]),
                            memories == null
                                    ? new ImportMemory[0]
                                    : memories.toArray(new ImportMemory[0]),
                            tables == null
                                    ? new ImportTable[0]
                                    : tables.toArray(new ImportTable[0]),
                            tags == null ? new ImportTag[0] : tags.toArray(new ImportTag[0]));
            return importValues;
        }
    }
}
