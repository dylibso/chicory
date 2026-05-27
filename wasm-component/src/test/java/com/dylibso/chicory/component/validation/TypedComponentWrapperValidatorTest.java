package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.Color;
import com.example.generated.ExampleComponent;
import com.example.generated.Person;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Typed Component Wrapper")
class TypedComponentWrapperValidatorTest {
    private static ComponentModel componentModel;
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
        hostFunctions.register("host-get-input", hostArgs -> "hello-from-host");

        componentModel = ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
        component = new ExampleComponent(componentModel);
    }

    @Nested
    @DisplayName("Primitive Operations")
    class PrimitiveOperationTests {
        @Test
        @DisplayName("typed add matches untyped add")
        void typedAddMatchesUntypedAdd() throws Exception {
            int typed = component.add(5, 3);
            Object untyped = componentModel.callExport("add", 5, 3);
            assertEquals(((Number) untyped).intValue(), typed);
        }

        @Test
        @DisplayName("typed wrapper handles multiple primitive operations")
        void typedWrapperHandlesMultiplePrimitiveOperations() throws Exception {
            assertEquals(28L, component.multiply(4L, 7L));
            assertTrue(component.isPositive(42));
        }
    }

    @Nested
    @DisplayName("Record Operations")
    class RecordOperationTests {
        @Test
        @DisplayName("typed wrapper returns Person instances")
        void typedWrapperReturnsPersonInstances() throws Exception {
            Object untyped = componentModel.callExport("create-person", "Alice", 30);
            assertInstanceOf(Person.class, untyped);

            Person typed = component.createPerson("Bob", 25);
            assertEquals("Bob", typed.getName());
            assertEquals(25, typed.getAge());
        }

        @Test
        @DisplayName("typed wrapper accepts Person inputs")
        void typedWrapperAcceptsPersonInputs() throws Exception {
            Person person = new Person("Charlie", 35, true);
            assertEquals(
                    "Charlie is 35 years old (active: true)", component.describePerson(person));
        }
    }

    @Nested
    @DisplayName("Variant Operations")
    class VariantOperationTests {
        @Test
        @DisplayName("typed wrapper returns a Color subtype")
        void typedWrapperReturnsAColorSubtype() throws Exception {
            Object untyped = componentModel.callExport("pick-color", 0);
            assertInstanceOf(Color.class, untyped);

            Color typed = component.pickColor(0);
            assertInstanceOf(Color.Red.class, typed);
            assertEquals("red", typed.getCaseName());
        }

        @Test
        @DisplayName("variant results support pattern matching")
        void variantResultsSupportPatternMatching() throws Exception {
            assertEquals("Red", colorName(component.pickColor(0)));
            assertEquals("Green", colorName(component.pickColor(1)));
            assertEquals("Blue", colorName(component.pickColor(2)));
        }
    }

    @Nested
    @DisplayName("List Operations")
    class ListOperationTests {
        @Test
        @DisplayName("typed wrapper returns typed string lists")
        void typedWrapperReturnsTypedStringLists() throws Exception {
            List<Person> people =
                    List.of(new Person("Diana", 32, true), new Person("Eve", 28, true));
            assertEquals(List.of("Diana", "Eve"), component.getNames(people));
        }

        @Test
        @DisplayName("typed wrapper supports end-to-end list workflows")
        void typedWrapperSupportsEndToEndListWorkflows() throws Exception {
            List<Person> people =
                    List.of(
                            new Person("Frank", 29, true),
                            new Person("Grace", 26, false),
                            new Person("Henry", 31, true));

            List<Person> filtered = component.filterHighValuePeople(people, 28);
            assertEquals(2, filtered.size());
            assertEquals(
                    List.of("Frank", "Henry"),
                    filtered.stream()
                            .map(Person::getName)
                            .collect(java.util.stream.Collectors.toList()));
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
                TypedComponentWrapperValidatorTest.class
                        .getClassLoader()
                        .getResourceAsStream(resourceName)) {
            assertTrue(input != null, () -> "Missing test resource: " + resourceName);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBinaryResource(String resourceName) throws IOException {
        try (var input =
                TypedComponentWrapperValidatorTest.class
                        .getClassLoader()
                        .getResourceAsStream(resourceName)) {
            assertTrue(input != null, () -> "Missing test resource: " + resourceName);
            return input.readAllBytes();
        }
    }
}
