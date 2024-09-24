package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * The runtime storage for all function, global, memory, table instances.
 */
public class Store {
    final LinkedHashMap<QualifiedName, ExternalFunction> functions = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ExternalGlobal> globals = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ExternalMemory> memories = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, ExternalTable> tables = new LinkedHashMap<>();

    public Store() {}

    /**
     * Add a function to the store.
     */
    public Store addFunction(ExternalFunction... function) {
        for (var f : function) {
            functions.put(new QualifiedName(f.module(), f.name()), f);
        }
        return this;
    }

    /**
     * Add a global to the store.
     */
    public Store addGlobal(ExternalGlobal... global) {
        for (var g : global) {
            globals.put(new QualifiedName(g.module(), g.name()), g);
        }
        return this;
    }

    /**
     * Add a memory to the store.
     */
    public Store addMemory(ExternalMemory... memory) {
        for (var m : memory) {
            memories.put(new QualifiedName(m.module(), m.name()), m);
        }
        return this;
    }

    /**
     * Add a table to the store.
     */
    public Store addTable(ExternalTable... table) {
        for (var t : table) {
            tables.put(new QualifiedName(t.module(), t.name()), t);
        }
        return this;
    }

    /**
     * Add the contents of a {@link ExternalValues} instance to the store.
     */
    public Store addExternalValues(ExternalValues externalValues) {
        return this.addGlobal(externalValues.globals())
                .addFunction(externalValues.functions())
                .addMemory(externalValues.memories())
                .addTable(externalValues.tables());
    }

    /**
     * Convert the contents of a store to a {@link ExternalValues} instance.
     */
    public ExternalValues toExternalValues() {
        return new ExternalValues(
                functions.values().toArray(new ExternalFunction[0]),
                globals.values().toArray(new ExternalGlobal[0]),
                memories.values().toArray(new ExternalMemory[0]),
                tables.values().toArray(new ExternalTable[0]));
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
                            new ExternalFunction(
                                    name,
                                    exportName,
                                    (inst, args) -> f.apply(args),
                                    ftype.params(),
                                    ftype.returns()));
                    break;

                case TABLE:
                    this.addTable(
                            new ExternalTable(name, exportName, instance.table(export.index())));
                    break;

                case MEMORY:
                    this.addMemory(new ExternalMemory(name, exportName, instance.memory()));
                    break;

                case GLOBAL:
                    GlobalInstance g = instance.global(export.index());
                    this.addGlobal(new ExternalGlobal(name, exportName, g));
                    break;
            }
        }
        return this;
    }

    /**
     * A shorthand for instantiating a module and registering it in the store.
     */
    public Instance instantiate(String name, Module m) {
        ExternalValues externalValues = this.toExternalValues();
        Instance instance = Instance.builder(m).withExternalValues(externalValues).build();
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
