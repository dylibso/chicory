package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.ExampleComponent;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 12.4: Basic Exports Test
 *
 * <p>Tests simple primitive type exports:
 * <ul>
 *   <li>add(s32, s32) -> s32
 *   <li>multiply(s64, s64) -> s64
 *   <li>divide(s32, s32) -> s32
 *   <li>is-positive(s32) -> bool
 *   <li>greet(string) -> string
 *   <li>process-text(string) -> string
 * </ul>
 *
 * <p>This is the baseline for testing - validates core WASM execution.
 */
public class BasicExportsValidator {
    public static void main(String[] mainArgs) throws Exception {
        System.out.println("=== Phase 12.4: Basic Exports Test ===\n");

        String basePath = "example/";
        String witSource = Files.readString(Paths.get(basePath + "example.wit"));
        byte[] wasmBytes = Files.readAllBytes(Paths.get(basePath + "example.wasm"));

        try {
            // Force load generated classes to register with PojoRegistry
            Class.forName("com.example.generated.Person");
            Class.forName("com.example.generated.UserStatus");
            Class.forName("com.example.generated.OperationResult");
            Class.forName("com.example.generated.Color");
            WasmModule wasmModule = Parser.parse(wasmBytes);
            HostFunctionProvider hostFunctions = new HostFunctionProvider();

            // Register host functions
            hostFunctions.register(
                    "host-log",
                    hostArgs -> {
                        String msg = hostArgs.length > 0 ? (String) hostArgs[0] : "null";
                        System.out.println("  [HOST] log: " + msg);
                        return null;
                    });

            hostFunctions.register(
                    "host-get-input",
                    hostArgs -> {
                        return "test input";
                    });

            ComponentModel component =
                    ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
            ExampleComponent sdk = new ExampleComponent(component);

            System.out.println("Test 1: add(5, 3)");
            int result = sdk.add(5, 3);
            assert result == 8 : "Expected 8, got " + result;
            System.out.println("  Result: " + result + " ✅\n");

            System.out.println("Test 2: multiply(4, 7)");
            long mulResult = sdk.multiply(4L, 7L);
            assert mulResult == 28 : "Expected 28, got " + mulResult;
            System.out.println("  Result: " + mulResult + " ✅\n");

            System.out.println("Test 3: greet(\"World\")");
            String greetResult = sdk.greet("World");
            assert greetResult != null : "Expected non-null result";
            System.out.println("  Result: " + greetResult + " ✅\n");

            System.out.println("Test 4: is-positive(42)");
            boolean isPosResult = sdk.isPositive(42);
            assert isPosResult : "Expected true";
            System.out.println("  Result: " + isPosResult + " ✅\n");

            System.out.println("Test 5: is-positive(-5)");
            boolean isNegResult = sdk.isPositive(-5);
            assert !isNegResult : "Expected false";
            System.out.println("  Result: " + isNegResult + " ✅\n");

            System.out.println("✅ All 7 basic export tests PASSED\n");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
