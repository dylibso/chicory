package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.component.ListValue;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.Color;
import com.example.generated.ExampleComponent;
import com.example.generated.OperationResult;
import com.example.generated.Person;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Bidirectional POJO Usage")
class BidirectionalPojoValidatorTest {
    private static ComponentModel componentModel;
    private static ExampleComponent component;

    @BeforeAll
    static void setupComponent() throws Exception {
        String witSource = readTextResource("example.wit");
        byte[] wasmBytes = readBinaryResource("example.wasm");

        Class.forName("com.example.generated.OperationResult");
        Class.forName("com.example.generated.Color");
        Class.forName("com.example.generated.Person");

        WasmModule wasmModule = Parser.parse(wasmBytes);
        HostFunctionProvider hostFunctions = new HostFunctionProvider();
        hostFunctions.register("host-log", hostArgs -> null);
        hostFunctions.register("host-get-input", hostArgs -> "input-from-host");

        componentModel = ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
        component = new ExampleComponent(componentModel);
    }

    @Nested
    @DisplayName("Records")
    class RecordTests {
        @Test
        @DisplayName("describe-person accepts a Person input")
        void describePersonAcceptsPojoInput() throws Exception {
            Person alice = new Person("Alice", 30, true);
            assertEquals("Alice is 30 years old (active: true)", component.describePerson(alice));
        }

        @Test
        @DisplayName("create-person returns a Person output")
        void createPersonReturnsPojoOutput() throws Exception {
            Person person = component.createPerson("Bob", 25);
            assertEquals("Bob", person.getName());
            assertEquals(25, person.getAge());
            assertTrue(person.getActive());
        }

        @Test
        @DisplayName("callExport accepts a Person instance directly")
        void callExportAcceptsPojoInstance() throws Exception {
            Person person = new Person("Charlie", 35, true);
            Object result = componentModel.callExport("describe-person", person);
            assertInstanceOf(String.class, result);
            assertEquals("Charlie is 35 years old (active: true)", result);
        }

        @Test
        @DisplayName("record fields remain available across round trips")
        void recordRoundTripPreservesFieldValues() throws Exception {
            Person person = new Person("Diana", 28, false);
            assertEquals("Diana is 28 years old (active: false)", component.describePerson(person));
        }
    }

    @Nested
    @DisplayName("Variants")
    class VariantTests {
        @Test
        @DisplayName("get-result returns an ok result")
        void getResultReturnsSuccessVariant() throws Exception {
            OperationResult result = component.getResult();
            OperationResult.Ok ok = assertInstanceOf(OperationResult.Ok.class, result);
            assertEquals("ok", result.getCaseName());
            assertEquals("Success!", ok.value);
        }

        @Test
        @DisplayName("pick-color returns the green case")
        void pickColorReturnsGreenCase() throws Exception {
            Color result = component.pickColor(1);
            assertInstanceOf(Color.Green.class, result);
            assertEquals("green", result.getCaseName());
        }

        @Test
        @DisplayName("variant values support pattern matching")
        void variantValuesSupportPatternMatching() throws Exception {
            Color result = component.pickColor(2);
            assertEquals("Blue", colorName(result));
        }

        @Test
        @DisplayName("all color variants can be loaded")
        void allColorVariantsCanBeLoaded() throws Exception {
            assertEquals("red", component.pickColor(0).getCaseName());
            assertEquals("green", component.pickColor(1).getCaseName());
            assertEquals("blue", component.pickColor(2).getCaseName());
        }
    }

    @Nested
    @DisplayName("Lists")
    class ListTests {
        @Test
        @DisplayName("filter-high-value-people accepts map-based list input")
        void filterHighValuePeopleAcceptsMapBasedInput() throws Exception {
            List<Object> people = new ArrayList<>();
            people.add(createPersonMap("Eve", 32, true));
            people.add(createPersonMap("Frank", 29, true));

            Object result = componentModel.callExport("filter-high-value-people", people, 28);
            assertNotNull(result);
            if (result instanceof ListValue) {
                assertEquals(2, ((ListValue) result).elements().size());
            } else {
                assertEquals(2, ((List<?>) result).size());
            }
        }

        @Test
        @DisplayName("filter-high-value-people accepts Person list input")
        void filterHighValuePeopleAcceptsPojoListInput() throws Exception {
            List<Person> people =
                    List.of(new Person("Grace", 31, true), new Person("Henry", 27, true));
            List<Person> result = component.filterHighValuePeople(people, 28);
            assertEquals(1, result.size());
            assertEquals("Grace", result.get(0).getName());
        }

        @Test
        @DisplayName("get-names extracts values from Person lists")
        void getNamesExtractsValuesFromPojoLists() throws Exception {
            List<Person> people =
                    List.of(new Person("Alice", 30, true), new Person("Bob", 25, true));
            assertEquals(List.of("Alice", "Bob"), component.getNames(people));
        }

        @Test
        @DisplayName("repeat-string and sum-numbers work with typed lists")
        void typedListOperationsSupportRoundTrips() throws Exception {
            assertEquals(List.of("hello", "hello", "hello"), component.repeatString("hello", 3));
            assertEquals(60, component.sumNumbers(List.of(10, 20, 30)));
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

    private static Map<String, Object> createPersonMap(String name, int age, boolean active) {
        Map<String, Object> person = new HashMap<>();
        person.put("name", name);
        person.put("age", age);
        person.put("active", active);
        return person;
    }

    private static String readTextResource(String resourceName) throws IOException {
        try (var input =
                BidirectionalPojoValidatorTest.class
                        .getClassLoader()
                        .getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBinaryResource(String resourceName) throws IOException {
        try (var input =
                BidirectionalPojoValidatorTest.class
                        .getClassLoader()
                        .getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return input.readAllBytes();
        }
    }
}
