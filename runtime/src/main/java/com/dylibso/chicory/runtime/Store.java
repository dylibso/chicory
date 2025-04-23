package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;

/**
 * The runtime storage for all function, global, memory, table instances.
 */
public class Store {
    final LinkedHashMap<QualifiedName, ImportFunction> functions = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ImportGlobal> globals = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ImportMemory> memories = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ImportTable> tables = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ImportTag> tags = new LinkedHashMap<>();

    public Store() {}

    /**
     * Add a function to the store.
     */
    public Store addFunction(ImportFunction... function) {
        for (var f : function) {
            functions.put(new QualifiedName(f.module(), f.name()), f);
        }
        return this;
    }

    /**
     * Add a global to the store.
     */
    public Store addGlobal(ImportGlobal... global) {
        for (var g : global) {
            globals.put(new QualifiedName(g.module(), g.name()), g);
        }
        return this;
    }

    /**
     * Add a memory to the store.
     */
    public Store addMemory(ImportMemory... memory) {
        for (var m : memory) {
            memories.put(new QualifiedName(m.module(), m.name()), m);
        }
        return this;
    }

    /**
     * Add a table to the store.
     */
    public Store addTable(ImportTable... table) {
        for (var t : table) {
            tables.put(new QualifiedName(t.module(), t.name()), t);
        }
        return this;
    }

    /**
     * Add a tag to the store.
     */
    public Store addTag(ImportTag... tag) {
        for (var t : tag) {
            tags.put(new QualifiedName(t.module(), t.name()), t);
        }
        return this;
    }

    /**
     * Add the contents of a {@link ImportValues} instance to the store.
     */
    public Store addImportValues(ImportValues importValues) {
        return this.addGlobal(importValues.globals())
                .addFunction(importValues.functions())
                .addMemory(importValues.memories())
                .addTable(importValues.tables())
                .addTag(importValues.tags());
    }

    /**
     * Convert the contents of a store to a {@link ImportValues} instance.
     */
    public ImportValues toImportValues() {
        return ImportValues.builder()
                .withFunctions(functions.values())
                .withGlobals(globals.values())
                .withMemories(memories.values())
                .withTables(tables.values())
                .withTags(tags.values())
                .build();
    }

    /**
     * Register an instance in the store with the given name.
     * All the exported functions, globals, memories, and tables are added to the store
     * with the given name.
     *
     * For instance, if a module named "myModule" exports a function
     * named "myFunction", the function will be added to the store with the name "myFunction.myModule".
     *
     */
    public Store register(String name, Instance instance) {
        ExportSection exportSection = instance.module().exportSection();
        for (int i = 0; i < exportSection.exportCount(); i++) {
            Export export = exportSection.getExport(i);
            String exportName = export.name();
            switch (export.exportType()) {
                case FUNCTION:
                    ExportFunction f = instance.export(exportName);
                    FunctionType ftype = instance.exportType(exportName);
                    this.addFunction(
                            new ImportFunction(
                                    name, exportName, ftype, (inst, args) -> f.apply(args)));
                    break;

                case TABLE:
                    this.addTable(
                            new ImportTable(name, exportName, instance.table(export.index())));
                    break;

                case MEMORY:
                    this.addMemory(new ImportMemory(name, exportName, instance.memory()));
                    break;

                case GLOBAL:
                    GlobalInstance g = instance.global(export.index());
                    this.addGlobal(new ImportGlobal(name, exportName, g));
                    break;

                case TAG:
                    this.addTag(new ImportTag(name, exportName, instance.tag(export.index())));
                    break;
            }
        }
        return this;
    }

    /**
     * A shorthand for instantiating a module and registering it in the store.
     */
    public Instance instantiate(String name, WasmModule m) {
        return this.instantiate(
                name, imports -> Instance.builder(m).withImportValues(imports).build());
    }

    /**
     * Creates an instance with the given factory and registers the result in the store.
     */
    public Instance instantiate(String name, Function<ImportValues, Instance> instanceFactory) {
        ImportValues importValues = this.toImportValues();
        Instance instance = instanceFactory.apply(importValues);
        register(name, instance);
        return instance;
    }

    /**
     * QualifiedName is internally used to use pairs (moduleName, name) as keys in the store.
     */
    static class QualifiedName {
        private final String module;
        private final String name;

        public QualifiedName(String module, String name) {
            this.module = module;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof QualifiedName)) {
                return false;
            }
            QualifiedName qualifiedName = (QualifiedName) o;
            return Objects.equals(module, qualifiedName.module)
                    && Objects.equals(name, qualifiedName.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, name);
        }
    }
}
