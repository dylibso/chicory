package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.ExampleComponent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Basic Exports")
class BasicExportsValidator {
    private static ExampleComponent component;

    @BeforeAll
    static void setupComponent() throws Exception {
        String witSource = readTextResource("example.wit");
        byte[] wasmBytes = readBinaryResource("example.wasm");

        Class.forName("com.example.generated.Person");
        Class.forName("com.example.generated.UserStatus");
        Class.forName("com.example.generated.OperationResult");
        Class.forName("com.example.generated.Color");

        WasmModule wasmModule = Parser.parse(wasmBytes);
        HostFunctionProvider hostFunctions = new HostFunctionProvider();
        hostFunctions.register("host-log", hostArgs -> null);
        hostFunctions.register("host-get-input", hostArgs -> "test input");

        ComponentModel componentModel =
                ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
        component = new ExampleComponent(componentModel);
    }

    @Nested
    @DisplayName("Integer Operations")
    class IntegerOperationsTests {
        @Test
        @DisplayName("add(5, 3) returns 8")
        void addReturnsExpectedSum() throws Exception {
            assertEquals(8, component.add(5, 3));
        }

        @Test
        @DisplayName("multiply(4, 7) returns 28")
        void multiplyReturnsExpectedProduct() throws Exception {
            assertEquals(28L, component.multiply(4L, 7L));
        }

        @Test
        @DisplayName("is-positive(42) returns true")
        void isPositiveReturnsTrueForPositiveNumbers() throws Exception {
            assertTrue(component.isPositive(42));
        }

        @Test
        @DisplayName("is-positive(-5) returns false")
        void isPositiveReturnsFalseForNegativeNumbers() throws Exception {
            assertFalse(component.isPositive(-5));
        }
    }

    @Nested
    @DisplayName("String Operations")
    class StringOperationsTests {
        @Test
        @DisplayName("greet(\"World\") returns greeting")
        void greetReturnsGreeting() throws Exception {
            String result = component.greet("World");
            assertNotNull(result);
            assertEquals("Hello, World!", result);
        }
    }

    private static String readTextResource(String resourceName) throws IOException {
        try (var input =
                BasicExportsValidator.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBinaryResource(String resourceName) throws IOException {
        try (var input =
                BasicExportsValidator.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return input.readAllBytes();
        }
    }
}
