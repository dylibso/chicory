package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.Color;
import com.example.generated.ExampleComponent;
import com.example.generated.OperationResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Variants")
class VariantsValidatorTest {
    private static ExampleComponent component;

    @BeforeAll
    static void setupComponent() throws Exception {
        String witSource = readTextResource("example.wit");
        byte[] wasmBytes = readBinaryResource("example.wasm");

        Class.forName("com.example.generated.Color");
        Class.forName("com.example.generated.OperationResult");

        WasmModule wasmModule = Parser.parse(wasmBytes);
        HostFunctionProvider hostFunctions = new HostFunctionProvider();
        hostFunctions.register("host-log", hostArgs -> null);
        hostFunctions.register("host-get-input", hostArgs -> "test input");

        ComponentModel componentModel =
                ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
        component = new ExampleComponent(componentModel);
    }

    @Nested
    @DisplayName("Empty Cases")
    class EmptyCaseTests {
        @Test
        @DisplayName("pick-color(0) returns red")
        void pickColorReturnsRed() throws Exception {
            Color color = component.pickColor(0);
            assertInstanceOf(Color.Red.class, color);
            assertEquals("red", color.getCaseName());
        }

        @Test
        @DisplayName("pick-color(1) returns green")
        void pickColorReturnsGreen() throws Exception {
            Color color = component.pickColor(1);
            assertInstanceOf(Color.Green.class, color);
            assertEquals("green", color.getCaseName());
        }

        @Test
        @DisplayName("pick-color(2) returns blue")
        void pickColorReturnsBlue() throws Exception {
            Color color = component.pickColor(2);
            assertInstanceOf(Color.Blue.class, color);
            assertEquals("blue", color.getCaseName());
        }
    }

    @Nested
    @DisplayName("Cases With Data")
    class DataCaseTests {
        @Test
        @DisplayName("get-result returns an OperationResult")
        void getResultReturnsOperationResult() throws Exception {
            OperationResult result = component.getResult();
            assertNotNull(result);
            assertEquals("ok", result.getCaseName());
        }

        @Test
        @DisplayName("get-result exposes the ok payload")
        void getResultExposesTheOkPayload() throws Exception {
            OperationResult.Ok result =
                    assertInstanceOf(OperationResult.Ok.class, component.getResult());
            assertEquals("Success!", result.value);
        }
    }

    @Nested
    @DisplayName("Pattern Matching")
    class PatternMatchingTests {
        @Test
        @DisplayName("all color cases can be pattern matched")
        void allColorCasesCanBePatternMatched() throws Exception {
            assertEquals("Red", colorName(component.pickColor(0)));
            assertEquals("Green", colorName(component.pickColor(1)));
            assertEquals("Blue", colorName(component.pickColor(2)));
        }
    }

    private static String colorName(Color color) {
        if (color instanceof Color.Red) {
            return "Red";
        }
        if (color instanceof Color.Green) {
            return "Green";
        }
        if (color instanceof Color.Blue) {
            return "Blue";
        }
        return "Unknown";
    }

    private static String readTextResource(String resourceName) throws IOException {
        try (var input =
                VariantsValidatorTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBinaryResource(String resourceName) throws IOException {
        try (var input =
                VariantsValidatorTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return input.readAllBytes();
        }
    }
}
