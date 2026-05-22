package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 9A Validation: Test ComponentModel with real WASM module. This app
 * loads example.wit + example.wasm and calls all three functions.
 */
public class ExampleWasmValidator {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Phase 9A: Real WASM Validation ===\n");

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

            // Test 3: is-positive(5) -> true (NOTE: WASM uses hyphens in export name)
            System.out.println("Test 3: is-positive(5)");
            Object result3 = component.callExport("is-positive", 5);
            System.out.println("  Result: " + result3);
            System.out.println("  Expected: 1 (true)");
            assert result3.equals(1L) : "Expected 1, got " + result3;
            System.out.println("  ✅ [PASS]\n");

            // Test 4: is-positive(-5) -> false (NOTE: WASM uses hyphens in export name)
            System.out.println("Test 4: is-positive(-5)");
            Object result4 = component.callExport("is-positive", -5);
            System.out.println("  Result: " + result4);
            System.out.println("  Expected: 0 (false)");
            assert result4.equals(0L) : "Expected 0, got " + result4;
            System.out.println("  ✅ [PASS]\n");

            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              ✅ ALL 4 TESTS PASSED ✅                      ║");
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
