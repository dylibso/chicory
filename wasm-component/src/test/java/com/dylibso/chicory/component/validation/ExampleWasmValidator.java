package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 9A/9B/9C Validation: Test ComponentModel with real WASM module.
 * Tests exports (9A), strings (9B), and imports (9C).
 */
public class ExampleWasmValidator {
    public static void main(String[] mainArgs) throws Exception {
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

            // ===== SETUP HOST FUNCTIONS (Phase 9C) =====
            System.out.println("Setting up host functions...");
            HostFunctionProvider hostFunctions = new HostFunctionProvider();

            // State to capture host function calls
            StringBuilder hostLogOutput = new StringBuilder();
            String[] hostInputResult = {"Hello from host!"};
            boolean[] hostLogCalled = {false};
            boolean[] hostGetInputCalled = {false};

            // Register host-log implementation
            hostFunctions.register(
                    "host-log",
                    hostArgs -> {
                        String msg = hostArgs.length > 0 ? (String) hostArgs[0] : "null";
                        System.out.println("  [✅ HOST CALLED] host-log(\"" + msg + "\")");
                        hostLogOutput.append(msg);
                        hostLogCalled[0] = true;
                        return null; // void function
                    });

            // Register host-get-input implementation
            hostFunctions.register(
                    "host-get-input",
                    hostArgs -> {
                        System.out.println(
                                "  [✅ HOST CALLED] host-get-input() → \""
                                        + hostInputResult[0]
                                        + "\"");
                        hostGetInputCalled[0] = true;
                        return hostInputResult[0];
                    });

            System.out.println("[OK] Host functions registered\n");

            // Load component
            System.out.println("Loading ComponentModel...");
            ComponentModel component =
                    ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
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
            System.out.println("=== PHASE 9C: IMPORTS (BIDIRECTIONAL CALLS) ===\n");

            // Test 7: Guest calls host-log
            System.out.println("Test 7: test-host-call-log(\"Message from guest\")");
            hostLogCalled[0] = false;
            component.callExport("test-host-call-log", "Message from guest");
            assert hostLogCalled[0] : "host-log was not called by guest";
            assert hostLogOutput.toString().contains("Message from guest");
            System.out.println("  ✅ [PASS] - Guest successfully called host function\n");

            // Test 8: Guest calls host-get-input
            System.out.println("Test 8: test-host-call-get-input()");
            hostGetInputCalled[0] = false;
            Object result8 = component.callExport("test-host-call-get-input");
            assert hostGetInputCalled[0] : "host-get-input was not called by guest";
            assert result8.equals("Hello from host!")
                    : "Expected 'Hello from host!', got " + result8;
            System.out.println("  Result: " + result8);
            System.out.println(
                    "  ✅ [PASS] - Guest successfully called host function and got return value\n");

            CanonicalAbi.clearContext();

            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║   ✅ BIDIRECTIONAL CALLS WORKING! PHASE 9 COMPLETE! ✅    ║");
            System.out.println("║                                                            ║");
            System.out.println("║  Summary:                                                  ║");
            System.out.println("║  - Guest → Host: ✅ WORKING                               ║");
            System.out.println("║  - Host → Guest: ✅ WORKING                               ║");
            System.out.println("║  - String parameters: ✅ WORKING                          ║");
            System.out.println("║  - Memory coordination: ✅ WORKING                        ║");
            System.out.println("║                                                            ║");
            System.out.println("║  8/8 Tests Passed                                          ║");
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
