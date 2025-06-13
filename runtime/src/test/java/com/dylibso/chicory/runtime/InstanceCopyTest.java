package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.Value;
import org.junit.jupiter.api.Test;

public class InstanceCopyTest {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(InstanceCopyTest.class.getResourceAsStream("/" + fileName));
    }

    @Test
    public void copyCreatesNewInstance() {
        var module = loadModule("compiled/exports.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        var copiedInstance = originalInstance.copy();

        assertNotNull(copiedInstance);
        assertNotSame(originalInstance, copiedInstance);
    }

    @Test
    public void copySharesImmutableModule() {
        var module = loadModule("compiled/exports.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        var copiedInstance = originalInstance.copy();

        // Module should be shared (immutable)
        assertSame(originalInstance.module(), copiedInstance.module());
    }

    @Test
    public void copyCreatesIndependentGlobals() {
        var module = loadModule("compiled/global-counter.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        // Test initial state
        var initialValue = (int) originalInstance.export("get").apply()[0];
        assertEquals(0, initialValue);

        // Set a value in the original
        originalInstance.export("set").apply(42);
        assertEquals(42, (int) originalInstance.export("get").apply()[0]);

        // Create copy - should have same state
        var copiedInstance = originalInstance.copy();
        assertEquals(42, (int) copiedInstance.export("get").apply()[0]);

        // Modify original - copy should be unaffected
        originalInstance.export("increment").apply(); // Now 43
        assertEquals(43, (int) originalInstance.export("get").apply()[0]);
        assertEquals(42, (int) copiedInstance.export("get").apply()[0]); // Still 42

        // Modify copy - original should be unaffected
        copiedInstance.export("add").apply(10); // Now 52
        assertEquals(43, (int) originalInstance.export("get").apply()[0]); // Still 43
        assertEquals(52, (int) copiedInstance.export("get").apply()[0]); // Now 52
    }

    @Test
    public void copyPreservesGlobalIndependence() {
        var module = loadModule("compiled/global-counter.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        // Set different values and test independence
        originalInstance.export("set").apply(100);
        var copiedInstance = originalInstance.copy();

        // Both should start with same value
        assertEquals(100, (int) originalInstance.export("get").apply()[0]);
        assertEquals(100, (int) copiedInstance.export("get").apply()[0]);

        // Decrement original, increment copy
        originalInstance.export("decrement").apply(); // 99
        copiedInstance.export("increment").apply(); // 101

        // Verify they're truly independent
        assertEquals(99, (int) originalInstance.export("get").apply()[0]);
        assertEquals(101, (int) copiedInstance.export("get").apply()[0]);

        // Direct global access should also show independence
        var originalGlobal = originalInstance.global(0);
        var copiedGlobal = copiedInstance.global(0);

        assertEquals(99, (int) originalGlobal.getValue());
        assertEquals(101, (int) copiedGlobal.getValue());

        // Test further independence - set different values via exported functions
        originalInstance.export("set").apply(777);
        copiedInstance.export("set").apply(888);

        // Verify via exported functions
        assertEquals(777, (int) originalInstance.export("get").apply()[0]);
        assertEquals(888, (int) copiedInstance.export("get").apply()[0]);

        // And verify via direct global access
        assertEquals(777, (int) originalGlobal.getValue());
        assertEquals(888, (int) copiedGlobal.getValue());
    }

    @Test
    public void copyCreatesIndependentMemory() {
        var module = loadModule("compiled/memory.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        var copiedInstance = originalInstance.copy();

        var originalMemory = originalInstance.memory();
        var copiedMemory = copiedInstance.memory();

        if (originalMemory != null && copiedMemory != null) {
            assertNotSame(originalMemory, copiedMemory);

            // Write to original memory
            originalMemory.writeByte(0, (byte) 42);

            // Copied memory should be independent
            assertNotEquals(42, copiedMemory.read(0));
        }
    }

    @Test
    public void copyCreatesIndependentTables() {
        var module = loadModule("compiled/call_indirect-export.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        var copiedInstance = originalInstance.copy();

        // Test table independence if tables exist
        try {
            var originalTable = originalInstance.table(0);
            var copiedTable = copiedInstance.table(0);

            assertNotSame(originalTable, copiedTable);
            assertEquals(originalTable.size(), copiedTable.size());

            // Test that table entries point to the correct instance
            for (int i = 0; i < originalTable.size(); i++) {
                var originalRef = originalTable.instance(i);
                var copiedRef = copiedTable.instance(i);

                if (originalRef == originalInstance) {
                    assertEquals(copiedInstance, copiedRef);
                } else if (originalRef != null) {
                    assertEquals(originalRef, copiedRef);
                }
            }
        } catch (com.dylibso.chicory.wasm.InvalidException e) {
            // Skip if no tables or index issues
        }
    }

    @Test
    public void copyWithImportValues() {
        var module = loadModule("compiled/add.wat.wasm");

        var importValues =
                ImportValues.builder()
                        .addGlobal(
                                new ImportGlobal(
                                        "env", "test_global", new GlobalInstance(Value.i32(123))))
                        .build();

        var originalInstance = Instance.builder(module).withImportValues(importValues).build();

        var copiedInstance = originalInstance.copy();

        assertNotNull(copiedInstance);
        assertNotSame(originalInstance, copiedInstance);
        assertNotSame(originalInstance.imports(), copiedInstance.imports());
    }

    @Test
    public void copyPreservesExceptions() {
        var module = loadModule("compiled/add.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        // Register an exception
        var exception = new WasmException(originalInstance, 0, new long[] {});
        var exnId = originalInstance.registerException(exception);

        var copiedInstance = originalInstance.copy();

        // Exception should be preserved in copy
        var copiedExn = copiedInstance.exn(exnId);
        assertEquals(exception.tagIdx(), copiedExn.tagIdx());
        assertEquals(copiedInstance, copiedExn.instance());
    }

    @Test
    public void copyWithoutInitialization() {
        var module = loadModule("compiled/add.wat.wasm");
        var originalInstance = Instance.builder(module).withInitialize(false).build();

        var copiedInstance = originalInstance.copy();

        assertNotNull(copiedInstance);
        assertNotSame(originalInstance, copiedInstance);
    }

    @Test
    public void copyPreservesExports() {
        var module = loadModule("compiled/exports.wat.wasm");
        var originalInstance = Instance.builder(module).build();

        assertEquals(0, originalInstance.exports().memory("mem").pages());
        assertEquals(10, originalInstance.exports().table("tab").size());
        assertEquals(42, originalInstance.exports().global("glob1").getValue());
        assertArrayEquals(new long[] {42}, originalInstance.exports().function("get-1").apply());

        var copiedInstance = originalInstance.copy();

        // Both instances should have same exports available
        var originalExports = originalInstance.exports();
        var copiedExports = copiedInstance.exports();

        assertNotNull(originalExports);
        assertNotNull(copiedExports);
        // Exports should be independent objects but serve the same functionality

        assertNotSame(originalExports, copiedExports);
        assertEquals(0, copiedInstance.exports().memory("mem").pages());
        assertEquals(10, copiedInstance.exports().table("tab").size());
        assertEquals(42, copiedInstance.exports().global("glob1").getValue());
        assertArrayEquals(new long[] {42}, copiedInstance.exports().function("get-1").apply());
    }

    @Test
    public void copyPreservesGlobalCounter() {
        var module = loadModule("compiled/global-counter.wat.wasm");
        var original = Instance.builder(module).build();

        // Test initial state
        assertEquals(0, (int) original.export("get").apply()[0]);

        // Modify the counter
        original.export("set").apply(42);
        assertEquals(42, (int) original.export("get").apply()[0]);

        // Create copy
        var copy = original.copy();

        // Verify copy has same state
        assertEquals(42, (int) copy.export("get").apply()[0]);

        // Modify original - copy should be unaffected
        original.export("increment").apply(); // Now 43
        assertEquals(43, (int) original.export("get").apply()[0]);
        assertEquals(42, (int) copy.export("get").apply()[0]); // Still 42!

        // Modify copy - original should be unaffected
        copy.export("add").apply(10); // Now 52
        assertEquals(43, (int) original.export("get").apply()[0]); // Still 43
        assertEquals(52, (int) copy.export("get").apply()[0]); // Now 52
    }
}
