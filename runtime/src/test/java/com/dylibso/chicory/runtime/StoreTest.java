package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StoreTest {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    @Test
    public void nameClashesShouldOverwriteTheStore() {
        Store store = new Store();

        HostFunction f1 = new HostFunction("m", "f", FunctionType.empty(), null);
        store.addFunction(f1);
        assertEquals(f1, store.functions.get(new Store.QualifiedName("m", "f")));

        HostFunction f2 = new HostFunction("m", "f", FunctionType.empty(), null);
        store.addFunction(f2);
        assertEquals(f2, store.functions.get(new Store.QualifiedName("m", "f")));
    }

    @Test
    public void exportsShouldBeRegistered() {
        var instance = Instance.builder(loadModule("compiled/exports.wat.wasm")).build();
        var store = new Store();
        String moduleName = "exports-module";
        store.register(moduleName, instance);

        // memories
        assertEquals(1, store.memories.size());
        assertTrue(store.memories.containsKey(new Store.QualifiedName(moduleName, "mem")));

        // tables
        assertEquals(1, store.tables.size());
        assertTrue(store.tables.containsKey(new Store.QualifiedName(moduleName, "tab")));

        // globals
        assertEquals(4, store.globals.size());
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob1")));
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob2")));
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob3")));
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob4")));

        // funcs
        assertEquals(4, store.functions.size());
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-1")));
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-2")));
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-3")));
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-4")));
    }

    @Test
    public void instantiateShouldRegisterInstance() {
        var store = new Store();
        String moduleName = "exports-module";
        var inst = store.instantiate(moduleName, loadModule("compiled/exports.wat.wasm"));
        assertNotNull(inst);

        // memories
        assertEquals(1, store.memories.size());
        assertTrue(store.memories.containsKey(new Store.QualifiedName(moduleName, "mem")));

        // tables
        assertEquals(1, store.tables.size());
        assertTrue(store.tables.containsKey(new Store.QualifiedName(moduleName, "tab")));

        // globals
        assertEquals(4, store.globals.size());
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob1")));
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob2")));
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob3")));
        assertTrue(store.globals.containsKey(new Store.QualifiedName(moduleName, "glob4")));

        // funcs
        assertEquals(4, store.functions.size());
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-1")));
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-2")));
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-3")));
        assertTrue(store.functions.containsKey(new Store.QualifiedName(moduleName, "get-4")));
    }

    @Test
    public void registerMultipleInstancesDifferentNamesCauseNoClash() {
        var store = new Store();

        String name1 = "exports-module-1";
        store.instantiate(name1, loadModule("compiled/exports.wat.wasm"));

        String name2 = "exports-module-2";
        store.instantiate(name2, loadModule("compiled/exports.wat.wasm"));

        var names = List.of(name1, name2);

        // memories
        assertEquals(2, store.memories.size());
        for (var name : names) {
            assertTrue(store.memories.containsKey(new Store.QualifiedName(name, "mem")));
        }

        // tables
        assertEquals(2, store.tables.size());
        for (var name : names) {
            assertTrue(store.tables.containsKey(new Store.QualifiedName(name, "tab")));
        }

        // globals
        assertEquals(8, store.globals.size());
        for (var name : names) {
            assertTrue(store.globals.containsKey(new Store.QualifiedName(name, "glob1")));
            assertTrue(store.globals.containsKey(new Store.QualifiedName(name, "glob2")));
            assertTrue(store.globals.containsKey(new Store.QualifiedName(name, "glob3")));
            assertTrue(store.globals.containsKey(new Store.QualifiedName(name, "glob4")));
        }

        // funcs
        assertEquals(8, store.functions.size());
        for (var name : names) {
            assertTrue(store.functions.containsKey(new Store.QualifiedName(name, "get-1")));
            assertTrue(store.functions.containsKey(new Store.QualifiedName(name, "get-2")));
            assertTrue(store.functions.containsKey(new Store.QualifiedName(name, "get-3")));
            assertTrue(store.functions.containsKey(new Store.QualifiedName(name, "get-4")));
        }
    }

    @Test
    public void moduleInstantiationShouldBeConfigurable() {
        var store = new Store();

        String name1 = "exports-module-1";
        store.instantiate(
                name1,
                imports ->
                        Instance.builder(loadModule("compiled/exports.wat.wasm"))
                                .withImportValues(imports)
                                .withStart(false)
                                .build());

        String name2 = "exports-module-2";
        store.instantiate(
                name2,
                imports ->
                        Instance.builder(loadModule("compiled/exports.wat.wasm"))
                                .withImportValues(imports)
                                .build());

        assertEquals(2, store.memories.size());
    }
}
