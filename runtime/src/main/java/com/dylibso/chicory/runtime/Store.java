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
    final LinkedHashMap<QualifiedName, HostFunction> functions = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, HostGlobal> globals = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, HostMemory> memories = new LinkedHashMap<>();
    final LinkedHashMap<QualifiedName, HostTable> tables = new LinkedHashMap<>();

    public Store() {}

    /**
     * Add a function to the store.
     */
    public Store addFunction(HostFunction... function) {
        for (var f : function) {
            functions.put(new QualifiedName(f.moduleName(), f.fieldName()), f);
        }
        return this;
    }

    /**
     * Add a global to the store.
     */
    public Store addGlobal(HostGlobal... global) {
        for (var g : global) {
            globals.put(new QualifiedName(g.moduleName(), g.fieldName()), g);
        }
        return this;
    }

    /**
     * Add a memory to the store.
     */
    public Store addMemory(HostMemory... memory) {
        for (var m : memory) {
            memories.put(new QualifiedName(m.moduleName(), m.fieldName()), m);
        }
        return this;
    }

    /**
     * Add a table to the store.
     */
    public Store addTable(HostTable... table) {
        for (var t : table) {
            tables.put(new QualifiedName(t.moduleName(), t.fieldName()), t);
        }
        return this;
    }

    /**
     * Add the contents of a {@link HostImports} instance to the store.
     */
    public Store addHostImports(HostImports hostImports) {
        return this.addGlobal(hostImports.globals())
                .addFunction(hostImports.functions())
                .addMemory(hostImports.memories())
                .addTable(hostImports.tables());
    }

    /**
     * Convert the contents of a store to a {@link HostImports} instance.
     */
    public HostImports toHostImports() {
        return new HostImports(
                functions.values().toArray(new HostFunction[0]),
                globals.values().toArray(new HostGlobal[0]),
                memories.values().toArray(new HostMemory[0]),
                tables.values().toArray(new HostTable[0]));
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
                            new HostFunction(
                                    (inst, args) -> f.apply(args),
                                    name,
                                    exportName,
                                    ftype.params(),
                                    ftype.returns()));
                    break;

                case TABLE:
                    this.addTable(new HostTable(name, exportName, instance.table(export.index())));
                    break;

                case MEMORY:
                    this.addMemory(new HostMemory(name, exportName, instance.memory()));
                    break;

                case GLOBAL:
                    GlobalInstance g = instance.global(export.index());
                    this.addGlobal(new HostGlobal(name, exportName, g));
                    break;
            }
        }
        return this;
    }

    /**
     * A shorthand for instantiating a module and registering it in the store.
     */
    public Instance instantiate(String name, Module m) {
        HostImports hostImports = toHostImports();
        Instance instance = Instance.builder(m).withHostImports(hostImports).build();
        register(name, instance);
        return instance;
    }

    /**
     * QualifiedName is internally used to use pairs (moduleName, name) as keys in the store.
     */
    static class QualifiedName {
        private final String moduleName;
        private final String fieldName;

        public QualifiedName(String moduleName, String fieldName) {
            this.moduleName = moduleName;
            this.fieldName = fieldName;
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
            return Objects.equals(moduleName, qualifiedName.moduleName)
                    && Objects.equals(fieldName, qualifiedName.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, fieldName);
        }
    }
}
