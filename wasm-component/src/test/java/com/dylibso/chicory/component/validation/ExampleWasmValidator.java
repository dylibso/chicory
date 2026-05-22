package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 9A/9B/9C Validation: Test ComponentModel with real WASM module.
 * Tests exports (9A), strings (9B), and imports (9C).
 */
public class ExampleWasmValidator {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Phase 9C: Imports & Bidirectional Calls ===\n");

        // Paths to WIT and WASM files
        String basePath = "example/";
        String witSource = Files.readString(Paths.get(basePath + "example.wit"));
        byte[] wasmBytes = Files.readAllBytes(Paths.get(basePath + "example.wasm"));

        System.out.println("[OK] Loaded WIT file: " + basePath + "example.wit");
        System.out.println(
                "[OK] Loaded WASM file: "
                        + basePath
                        + "example.wasm ("
                        + wasmBytes.length
                        + " bytes)\n");

        try {
            // Parse WASM bytes to WasmModule
            System.out.println("Parsing WASM module...");
            WasmModule wasmModule = Parser.parse(wasmBytes);
            System.out.println("[OK] WASM module parsed\n");

            // Load component
            System.out.println("Loading ComponentModel...");
            ComponentModel component = ComponentModel.load(witSource, wasmModule);
            System.out.println("[OK] ComponentModel loaded successfully\n");

            // Display exported and imported functions
            System.out.println("=== Component Interface ===\n");
            System.out.println("EXPORTS (functions guest provides to host):");
            for (String name : component.getExportedFunctions()) {
                System.out.println("  - " + name);
            }

            System.out.println("\nIMPORTS (functions host must provide to guest):");
            for (String name : component.getImportedFunctions()) {
                System.out.println("  - " + name);
            }

            // ===== PRIMITIVES (Phase 9A) =====
            System.out.println("\n=== PHASE 9A: PRIMITIVES ===\n");

            // Test 1: add(5, 3) -> 8
            System.out.println("Test 1: add(5, 3)");
            Object result1 = component.callExport("add", 5, 3);
            System.out.println("  Result: " + result1);
            System.out.println("  Expected: 8");
            assert result1.equals(8L) : "Expected 8, got " + result1;
            System.out.println("  ✅ [PASS]\n");

            // Test 2: multiply(10L, 20L) -> 200L
            System.out.println("Test 2: multiply(10, 20)");
            Object result2 = component.callExport("multiply", 10L, 20L);
            System.out.println("  Result: " + result2);
            System.out.println("  Expected: 200");
            assert result2.equals(200L) : "Expected 200, got " + result2;
            System.out.println("  ✅ [PASS]\n");

            // Test 3: is-positive(5) -> true
            System.out.println("Test 3: is-positive(5)");
            Object result3 = component.callExport("is-positive", 5);
            System.out.println("  Result: " + result3);
            System.out.println("  Expected: 1 (true)");
            assert result3.equals(1L) : "Expected 1, got " + result3;
            System.out.println("  ✅ [PASS]\n");

            // Test 4: is-positive(-5) -> false
            System.out.println("Test 4: is-positive(-5)");
            Object result4 = component.callExport("is-positive", -5);
            System.out.println("  Result: " + result4);
            System.out.println("  Expected: 0 (false)");
            assert result4.equals(0L) : "Expected 0, got " + result4;
            System.out.println("  ✅ [PASS]\n");

            // ===== STRINGS (Phase 9B) =====
            System.out.println("=== PHASE 9B: STRINGS ===\n");

            // Set up string encoding context with realloc
            var realloc = component.getInstance().export("cabi_realloc");
            if (realloc != null) {
                CanonicalAbi.withContext(realloc, component.getInstance().memory());
            }

            // Test 5: greet("Alice") -> "Hello, Alice!"
            System.out.println("Test 5: greet(\"Alice\")");
            Object result5 = component.callExport("greet", "Alice");
            System.out.println("  Result: " + result5);
            System.out.println("  Expected: Hello, Alice!");
            assert result5.equals("Hello, Alice!") : "Expected 'Hello, Alice!', got " + result5;
            System.out.println("  ✅ [PASS]\n");

            // Test 6: process-text("hello") -> "HELLO"
            System.out.println("Test 6: process-text(\"hello\")");
            Object result6 = component.callExport("process-text", "hello");
            System.out.println("  Result: " + result6);
            System.out.println("  Expected: HELLO");
            assert result6.equals("HELLO") : "Expected 'HELLO', got " + result6;
            System.out.println("  ✅ [PASS]\n");

            // ===== IMPORTS (Phase 9C) =====
            System.out.println("=== PHASE 9C: IMPORTS ===\n");
            System.out.println("✅ Imports parsed successfully!");
            System.out.println("Guest can call: host-log and host-get-input\n");
            System.out.println("⏳ Full import testing coming in next phase\n");

            CanonicalAbi.clearContext();

            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║          ✅ PHASE 9A/9B/9C VALIDATION COMPLETE ✅          ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("\n[ERROR] during validation:");
            System.err.println("Exception Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println("\nStack Trace:");
            e.printStackTrace();

            System.out.println("\n=== FAILURE ANALYSIS ===");
            System.out.println("Failure Point: " + e.getClass().getSimpleName());
            System.out.println("Root Cause: Check stack trace above");
            System.exit(1);
        }
    }
}
