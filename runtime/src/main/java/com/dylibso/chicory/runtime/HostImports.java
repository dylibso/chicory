package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;

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

    public FromHost[] index() {
        return index;
    }

    public void setIndex(FromHost[] index) {
        this.index = index;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(HostImports imports) {
        return new Builder(imports);
    }

    public static final class Builder {
        private List<HostFunction> functions;
        private List<HostGlobal> globals;
        private List<HostMemory> memories;
        private List<HostTable> tables;
        private List<FromHost> index;

        Builder() {}

        Builder(HostImports imports) {
            this.functions = new ArrayList<>();
            this.globals = new ArrayList<>();
            this.memories = new ArrayList<>();
            this.tables = new ArrayList<>();
            Collections.addAll(this.functions, imports.functions);
            Collections.addAll(this.globals, imports.globals);
            Collections.addAll(this.memories, imports.memories);
            Collections.addAll(this.tables, imports.tables);
        }

        public static final class ImportBuilder {
            private final Builder builder;
            private final String moduleName;
            private final String fieldName;

            ImportBuilder(Builder builder, String moduleName, String fieldName) {
                this.builder = builder;
                this.moduleName = moduleName;
                this.fieldName = fieldName;
            }

            public Builder withGlobal(Value value) {
                return builder.addGlobal(
                        new HostGlobal(moduleName, fieldName, new GlobalInstance(value)));
            }

            public Builder withGlobal(MutabilityType mutability, Value value) {
                return builder.addGlobal(
                        new HostGlobal(
                                moduleName, fieldName, new GlobalInstance(value), mutability));
            }

            public Builder withMutableGlobal(Value value) {
                return builder.addGlobal(
                        new HostGlobal(
                                moduleName,
                                fieldName,
                                new GlobalInstance(value),
                                MutabilityType.Var));
            }

            public Builder withMemory() {
                return builder.addMemory(
                        new HostMemory(
                                moduleName, fieldName, new Memory(MemoryLimits.defaultLimits())));
            }

            public Builder withMemory(MemoryLimits limits) {
                return builder.addMemory(new HostMemory(moduleName, fieldName, new Memory(limits)));
            }

            public Builder withMemory(int initial) {
                return builder.addMemory(
                        new HostMemory(
                                moduleName, fieldName, new Memory(new MemoryLimits(initial))));
            }

            public Builder withMemory(int initial, int max) {
                return builder.addMemory(
                        new HostMemory(
                                moduleName, fieldName, new Memory(new MemoryLimits(initial, max))));
            }

            public Builder withTable() {
                return builder.addTable(
                        new HostTable(
                                moduleName,
                                fieldName,
                                new TableInstance(
                                        new Table(ValueType.FuncRef, Limits.unbounded()))));
            }

            public Builder withTable(ValueType t) {
                return builder.addTable(
                        new HostTable(
                                moduleName,
                                fieldName,
                                new TableInstance(new Table(t, Limits.unbounded()))));
            }

            public Builder withTable(Limits limits) {
                return builder.addTable(
                        new HostTable(
                                moduleName,
                                fieldName,
                                new TableInstance(new Table(ValueType.FuncRef, limits))));
            }

            public Builder withTable(int min) {
                return builder.addTable(
                        new HostTable(
                                moduleName,
                                fieldName,
                                new TableInstance(new Table(ValueType.FuncRef, new Limits(min)))));
            }

            public Builder withTable(int min, int max) {
                return builder.addTable(
                        new HostTable(
                                moduleName,
                                fieldName,
                                new TableInstance(
                                        new Table(ValueType.FuncRef, new Limits(min, max)))));
            }

            // TODO: check - this mechanism is limited by the available primitive functional
            // interfaces
            public Builder withProcedure(Runnable f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    f.run();
                                    return new Value[] {};
                                },
                                moduleName,
                                fieldName,
                                List.of(),
                                List.of()));
            }

            public Builder withProcedure(Function<Instance, Runnable> consumer) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    consumer.apply(instance).run();
                                    return new Value[] {};
                                },
                                moduleName,
                                fieldName,
                                List.of(),
                                List.of()));
            }

            public Builder withSupplier(IntSupplier f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) ->
                                        new Value[] {Value.i32(f.getAsInt())},
                                moduleName,
                                fieldName,
                                List.of(),
                                List.of(ValueType.I32)));
            }

            public Builder withSupplier(Function<Instance, IntSupplier> consumer) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    consumer.apply(instance).getAsInt();
                                    return new Value[] {};
                                },
                                moduleName,
                                fieldName,
                                List.of(),
                                List.of()));
            }

            public Builder withSupplier(LongSupplier f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) ->
                                        new Value[] {Value.i64(f.getAsLong())},
                                moduleName,
                                fieldName,
                                List.of(),
                                List.of(ValueType.I64)));
            }

            // ... complete me
            public Builder withConsumer(IntConsumer f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    f.accept(args[0].asInt());
                                    return new Value[] {};
                                },
                                moduleName,
                                fieldName,
                                List.of(ValueType.I32),
                                List.of()));
            }

            public Builder withConsumer(LongConsumer f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    f.accept(args[0].asLong());
                                    return new Value[] {};
                                },
                                moduleName,
                                fieldName,
                                List.of(ValueType.I64),
                                List.of()));
            }

            // ... complete me
            public Builder withFunction(IntUnaryOperator f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    return new Value[] {Value.i32(f.applyAsInt(args[0].asInt()))};
                                },
                                moduleName,
                                fieldName,
                                List.of(ValueType.I32),
                                List.of(ValueType.I32)));
            }

            public Builder withFunction(LongUnaryOperator f) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    return new Value[] {Value.i64(f.applyAsLong(args[0].asLong()))};
                                },
                                moduleName,
                                fieldName,
                                List.of(ValueType.I64),
                                List.of(ValueType.I64)));
            }

            // and now let's see how a fallback might look like
            public Builder withFunction(Function<Instance, Function<Value[], Value[]>> consumer) {
                return builder.addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    return consumer.apply(instance).apply(args);
                                },
                                moduleName,
                                fieldName,
                                List.of(ValueType.I64),
                                List.of(ValueType.I64)));
            }
        }

        public ImportBuilder withNewImport(String moduleName, String fieldName) {
            return new ImportBuilder(this, moduleName, fieldName);
        }

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

        public Builder withIndex(List<FromHost> index) {
            this.index = index;
            return this;
        }

        public Builder addIndex(FromHost... i) {
            if (this.index == null) {
                this.index = new ArrayList<>();
            }
            Collections.addAll(this.index, i);
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
            if (index != null) {
                hostImports.setIndex(index.toArray(new FromHost[0]));
            }
            return hostImports;
        }
    }
}
