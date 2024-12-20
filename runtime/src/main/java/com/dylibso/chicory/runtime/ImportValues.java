package com.dylibso.chicory.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class ImportValues {
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

    private ImportValues(
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
        private Collection<ImportFunction> functions;
        private Collection<ImportGlobal> globals;
        private Collection<ImportMemory> memories;
        private Collection<ImportTable> tables;
        private Collection<ImportTag> tags;

        Builder() {}

        public Builder withFunctions(Collection<ImportFunction> functions) {
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

        public Builder withGlobals(Collection<ImportGlobal> globals) {
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

        public Builder withMemories(Collection<ImportMemory> memories) {
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

        public Builder withTables(Collection<ImportTable> tables) {
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

        public Builder withTags(Collection<ImportTag> tags) {
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
                                    ? NO_EXTERNAL_FUNCTIONS
                                    : functions.toArray(NO_EXTERNAL_FUNCTIONS),
                            globals == null
                                    ? NO_EXTERNAL_GLOBALS
                                    : globals.toArray(NO_EXTERNAL_GLOBALS),
                            memories == null
                                    ? NO_EXTERNAL_MEMORIES
                                    : memories.toArray(NO_EXTERNAL_MEMORIES),
                            tables == null
                                    ? NO_EXTERNAL_TABLES
                                    : tables.toArray(NO_EXTERNAL_TABLES),
                            tags == null ? NO_EXTERNAL_TAGS : tags.toArray(NO_EXTERNAL_TAGS));
            return importValues;
        }
    }
}
