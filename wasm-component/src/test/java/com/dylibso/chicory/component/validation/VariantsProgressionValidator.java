package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.Color;
import com.example.generated.ExampleComponent;
import com.example.generated.OperationResult;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 12.4: Variant Types Test
 *
 * <p>Tests variant (sealed class) type handling:
 * <ul>
 *   <li>get-result() -> operation-result (variant with data)
 *   <li>pick-color(s32) -> color (variant without data)
 * </ul>
 *
 * <p>Shows variant pattern matching and case extraction.
 */
public class VariantsProgressionValidator {
    public static void main(String[] mainArgs) throws Exception {
        System.out.println("=== Phase 12.4: Variants Progression Test ===\n");

        String basePath = "example/";
        String witSource = Files.readString(Paths.get(basePath + "example.wit"));
        byte[] wasmBytes = Files.readAllBytes(Paths.get(basePath + "example.wasm"));

        try {
            WasmModule wasmModule = Parser.parse(wasmBytes);
            HostFunctionProvider hostFunctions = new HostFunctionProvider();
            hostFunctions.register("host-log", hostArgs -> null);
            hostFunctions.register("host-get-input", hostArgs -> "test input");
            ComponentModel component =
                    ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
            ExampleComponent sdk = new ExampleComponent(component);

            System.out.println("=== EMPTY VARIANTS ===\n");

            System.out.println("Test 1: pick-color(0) -> Red");
            Color color0 = sdk.pickColor(0);
            assert color0 instanceof Color.Red : "Expected Color.Red";
            System.out.println("  Result: " + color0);
            System.out.println("  Case: " + color0.getCaseName() + " ✅\n");

            System.out.println("Test 2: pick-color(1) -> Green");
            Color color1 = sdk.pickColor(1);
            assert color1 instanceof Color.Green : "Expected Color.Green";
            System.out.println("  Result: " + color1);
            System.out.println("  Case: " + color1.getCaseName() + " ✅\n");

            System.out.println("Test 3: pick-color(2) -> Blue");
            Color color2 = sdk.pickColor(2);
            assert color2 instanceof Color.Blue : "Expected Color.Blue";
            System.out.println("  Result: " + color2);
            System.out.println("  Case: " + color2.getCaseName() + " ✅\n");

            System.out.println("=== VARIANTS WITH DATA ===\n");

            System.out.println("Test 4: get-result() -> OperationResult");
            OperationResult result = sdk.getResult();
            assert result != null : "Expected OperationResult";
            System.out.println("  Result: " + result);
            System.out.println("  Case: " + result.getCaseName());

            if (result instanceof OperationResult.Ok) {
                OperationResult.Ok ok = (OperationResult.Ok) result;
                System.out.println("  Data: " + ok.value);
                assert ok.value != null : "Expected data in Ok case";
                System.out.println("  ✅\n");
            } else if (result instanceof OperationResult.Err) {
                OperationResult.Err err = (OperationResult.Err) result;
                System.out.println("  Error code: " + err.value);
                System.out.println("  ✅\n");
            }

            System.out.println("=== PATTERN MATCHING ===\n");

            System.out.println("Test 5: Pattern match on all color variants");
            Color[] colors = {sdk.pickColor(0), sdk.pickColor(1), sdk.pickColor(2)};
            String[] names = {"Red", "Green", "Blue"};
            for (int i = 0; i < colors.length; i++) {
                Color c = colors[i];
                String name = "";
                if (c instanceof Color.Red) {
                    name = "Red";
                } else if (c instanceof Color.Green) {
                    name = "Green";
                } else if (c instanceof Color.Blue) {
                    name = "Blue";
                }
                System.out.println("  Color[" + i + "]: " + name);
                assert name.equals(names[i]) : "Expected " + names[i];
            }
            System.out.println("  ✅\n");

            System.out.println("Test 6: Extract and use variant data");
            OperationResult opResult = sdk.getResult();
            String caseName = opResult.getCaseName();
            System.out.println("  Case name: " + caseName);

            if (opResult instanceof OperationResult.Ok) {
                OperationResult.Ok okResult = (OperationResult.Ok) opResult;
                System.out.println("  Extracted data: " + okResult.value);
                assert okResult.value != null && okResult.value.length() > 0
                        : "Expected non-empty message";
            }
            System.out.println("  ✅\n");

            System.out.println("✅ All 6 variant tests PASSED\n");
            System.out.println("Key Features:");
            System.out.println("  ✓ Empty variants (Red, Green, Blue)");
            System.out.println("  ✓ Variants with associated data (Ok<string>, Err<s32>)");
            System.out.println("  ✓ Type-safe pattern matching with instanceof");
            System.out.println("  ✓ Case name extraction via getCaseName()");
            System.out.println("  ✓ Data extraction from variant cases");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
