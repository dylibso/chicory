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
    private final LinkedHashMap<Store.Key, HostFunction> functions = new LinkedHashMap<>();
    private final LinkedHashMap<Store.Key, HostGlobal> globals = new LinkedHashMap<>();
    private final LinkedHashMap<Store.Key, HostMemory> memories = new LinkedHashMap<>();
    private final LinkedHashMap<Store.Key, HostTable> tables = new LinkedHashMap<>();

    public Store() {}

    public Store addFunction(HostFunction... function) {
        for (var f : function) {
            functions.put(new Key(f.moduleName(), f.fieldName()), f);
        }
        return this;
    }

    public Store addGlobal(HostGlobal... global) {
        for (var g : global) {
            globals.put(new Key(g.moduleName(), g.fieldName()), g);
        }
        return this;
    }

    public Store addMemory(HostMemory... memory) {
        for (var m : memory) {
            memories.put(new Key(m.moduleName(), m.fieldName()), m);
        }
        return this;
    }

    public Store addTable(HostTable... table) {
        for (var t : table) {
            tables.put(new Key(t.moduleName(), t.fieldName()), t);
        }
        return this;
    }

    public Store addHostImports(HostImports hostImports) {
        return this.addGlobal(hostImports.globals())
                .addFunction(hostImports.functions())
                .addMemory(hostImports.memories())
                .addTable(hostImports.tables());
    }

    public HostImports toHostImports() {
        return new HostImports(
                functions.values().toArray(new HostFunction[0]),
                globals.values().toArray(new HostGlobal[0]),
                memories.values().toArray(new HostMemory[0]),
                tables.values().toArray(new HostTable[0]));
    }

    public Instance instantiate(String name, Module m) {
        HostImports hostImports = toHostImports();
        Instance instance = Instance.builder(m).withHostImports(hostImports).build();
        register(name, instance);
        return instance;
    }

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

    private static class Key {
        private final String moduleName;
        private final String name;

        public Key(String moduleName, String name) {
            this.moduleName = moduleName;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(moduleName, key.moduleName) && Objects.equals(name, key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, name);
        }
    }
}
