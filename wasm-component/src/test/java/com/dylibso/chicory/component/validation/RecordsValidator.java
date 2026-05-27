package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.ExampleComponent;
import com.example.generated.Person;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Records")
class RecordsValidator {
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
    @DisplayName("Record Inputs")
    class RecordInputTests {
        @Test
        @DisplayName("describe-person accepts active Person values")
        void describePersonAcceptsActivePersonValues() throws Exception {
            Person alice = new Person("Alice", 30, true);
            assertEquals("Alice is 30 years old (active: true)", component.describePerson(alice));
        }

        @Test
        @DisplayName("describe-person accepts inactive Person values")
        void describePersonAcceptsInactivePersonValues() throws Exception {
            Person bob = new Person("Bob", 25, false);
            assertEquals("Bob is 25 years old (active: false)", component.describePerson(bob));
        }
    }

    @Nested
    @DisplayName("Record Outputs")
    class RecordOutputTests {
        @Test
        @DisplayName("create-person returns the requested fields")
        void createPersonReturnsRequestedFields() throws Exception {
            Person person = component.createPerson("Charlie", 35);
            assertEquals("Charlie", person.getName());
            assertEquals(35, person.getAge());
            assertTrue(person.getActive());
        }

        @Test
        @DisplayName("create-person supports multiple inputs")
        void createPersonSupportsMultipleInputs() throws Exception {
            Person person = component.createPerson("Diana", 28);
            assertEquals("Diana", person.getName());
            assertEquals(28, person.getAge());
        }
    }

    @Nested
    @DisplayName("Record Workflows")
    class RecordWorkflowTests {
        @Test
        @DisplayName("record fields remain accessible after creation")
        void recordFieldsRemainAccessibleAfterCreation() throws Exception {
            Person person = component.createPerson("Eve", 40);
            assertEquals("Eve", person.getName());
            assertEquals(40, person.getAge());
            assertEquals(true, person.getActive());
        }

        @Test
        @DisplayName("created records can be passed back into exports")
        void createdRecordsCanBePassedBackIntoExports() throws Exception {
            Person person = component.createPerson("Frank", 32);
            assertEquals("Frank is 32 years old (active: true)", component.describePerson(person));
        }
    }

    private static String readTextResource(String resourceName) throws IOException {
        try (var input =
                RecordsValidator.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBinaryResource(String resourceName) throws IOException {
        try (var input =
                RecordsValidator.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(input, () -> "Missing test resource: " + resourceName);
            return input.readAllBytes();
        }
    }
}
