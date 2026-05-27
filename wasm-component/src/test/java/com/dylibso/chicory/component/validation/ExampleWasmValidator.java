package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.Color;
import com.example.generated.ExampleComponent;
import com.example.generated.OperationResult;
import com.example.generated.Person;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * JUnit 5 test suite for WASM Component Model validation.
 * Tests include: Primitives, Strings, Imports, Records, Variants, Lists, and Complex Nested Types.
 */
@DisplayName("WASM Component Example Tests")
@ExtendWith(MockitoExtension.class)
class ExampleWasmValidatorTest {
    private static ExampleComponent component;

    // Mocks for host function validations
    private static Function<Object[], Object> hostLogMock;
    private static Function<Object[], Object> hostGetInputMock;

    @BeforeAll
    static void setupComponent() throws Exception {
        // Load WIT and WASM files from test resources directory
        ClassLoader classLoader = ExampleWasmValidatorTest.class.getClassLoader();

        // Load example.wit
        String witSource;
        try (var input = classLoader.getResourceAsStream("example.wit")) {
            if (input == null) {
                throw new IllegalStateException(
                        "Could not find example.wit in test resources. Place it in"
                                + " src/test/resources/");
            }
            witSource = new String(input.readAllBytes());
        }

        // Load example.wasm
        byte[] wasmBytes;
        try (var input = classLoader.getResourceAsStream("example.wasm")) {
            if (input == null) {
                throw new IllegalStateException(
                        "Could not find example.wasm in test resources. Place it in"
                                + " src/test/resources/");
            }
            wasmBytes = input.readAllBytes();
        }

        // Force load generated classes to register with PojoRegistry
        Class.forName("com.example.generated.Person");
        Class.forName("com.example.generated.UserStatus");
        Class.forName("com.example.generated.OperationResult");
        Class.forName("com.example.generated.Color");

        // Parse WASM bytes to WasmModule
        WasmModule wasmModule = Parser.parse(wasmBytes);

        // Setup host functions with mocks
        HostFunctionProvider hostFunctions = new HostFunctionProvider();

        // Create mocks for host functions
        hostLogMock = mock(Function.class);
        hostGetInputMock = mock(Function.class);

        // Configure default behavior for host-log
        when(hostLogMock.apply(any()))
                .thenAnswer(
                        invocation -> {
                            Object[] args = invocation.getArgument(0);
                            String msg = args.length > 0 ? (String) args[0] : "null";
                            System.out.println("  [✅ HOST CALLED] host-log(\"" + msg + "\")");
                            return null;
                        });

        // Configure default behavior for host-get-input
        when(hostGetInputMock.apply(any()))
                .thenAnswer(
                        invocation -> {
                            System.out.println(
                                    "  [✅ HOST CALLED] host-get-input() → \"Hello from host!\"");
                            return "Hello from host!";
                        });

        // Register mocked host functions
        hostFunctions.register("host-log", hostLogMock::apply);
        hostFunctions.register(
                "host-get-input",
                hostArgs -> {
                    System.out.println("  [✅ HOST CALLED] host-get-input() → \"Hello from host!\"");
                    return "Hello from host!";
                });

        // Load component using ComponentModel
        ComponentModel componentModel =
                ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);

        // Wrap in ExampleComponent utility class
        component = new ExampleComponent(componentModel);

        // Setup string encoding context with realloc
        var realloc = componentModel.getInstance().export("cabi_realloc");
        if (realloc != null) {
            CanonicalAbi.withContext(realloc, componentModel.getInstance().memory());
        }
    }

    @BeforeEach
    void resetMocks() {
        reset(hostLogMock, hostGetInputMock);

        // Reconfigure default behavior after reset
        when(hostLogMock.apply(any()))
                .thenAnswer(
                        invocation -> {
                            Object[] args = invocation.getArgument(0);
                            String msg = args.length > 0 ? (String) args[0] : "null";
                            System.out.println("  [✅ HOST CALLED] host-log(\"" + msg + "\")");
                            return null;
                        });

        when(hostGetInputMock.apply(any()))
                .thenAnswer(
                        invocation -> {
                            System.out.println(
                                    "  [✅ HOST CALLED] host-get-input() → \"Hello from host!\"");
                            return "Hello from host!";
                        });
    }

    @Nested
    @DisplayName("Primitives")
    class PrimitivesTests {
        @Test
        @DisplayName("add(5, 3) returns 8")
        void testAddNumbers() throws Exception {
            int result = component.add(5, 3);
            assertEquals(8, result);
        }

        @Test
        @DisplayName("multiply(10, 20) returns 200")
        void testMultiplyNumbers() throws Exception {
            long result = component.multiply(10L, 20L);
            assertEquals(200L, result);
        }

        @Test
        @DisplayName("isPositive(5) returns true")
        void testIsPositiveWithPositive() throws Exception {
            boolean result = component.isPositive(5);
            assertTrue(result);
        }

        @Test
        @DisplayName("isPositive(-5) returns false")
        void testIsPositiveWithNegative() throws Exception {
            boolean result = component.isPositive(-5);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Strings")
    class StringsTests {
        @Test
        @DisplayName("greet(\"Alice\") returns \"Hello, Alice!\"")
        void testGreetString() throws Exception {
            String result = component.greet("Alice");
            assertEquals("Hello, Alice!", result);
        }

        @Test
        @DisplayName("processText(\"hello\") returns \"HELLO\"")
        void testProcessTextUppercase() throws Exception {
            String result = component.processText("hello");
            assertEquals("HELLO", result);
        }
    }

    @Nested
    @DisplayName("Imports (Bidirectional Calls)")
    class ImportsTests {
        @Test
        @DisplayName("testHostCallLog - Guest calls host-log function")
        void testHostCallLog() throws Exception {
            component.testHostCallLog("Message from guest");

            // Verify the mock was called
            verify(hostLogMock).apply(any());

            // Capture and verify the arguments
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(hostLogMock).apply(captor.capture());
            Object[] args = captor.getValue();
            assertEquals(
                    "Message from guest",
                    args[0],
                    "Expected host-log to be called with 'Message from guest'");
        }

        @Test
        @DisplayName("testHostCallGetInput - Guest calls host-get-input and gets return value")
        void testHostCallGetInput() throws Exception {
            String result = component.testHostCallGetInput();

            // Verify the return value
            assertEquals(
                    "Input: Hello from host!",
                    result,
                    "Expected guest to receive 'Hello from host!' from host");
        }
    }

    @Nested
    @DisplayName("Records")
    class RecordsTests {
        @Test
        @DisplayName("describePerson with Person record parameter")
        void testDescribePersonRecord() throws Exception {
            Person person = new Person("Alice", 30, true);
            String result = component.describePerson(person);
            assertEquals("Alice is 30 years old (active: true)", result);
        }

        @Test
        @DisplayName("createPerson returns Person record")
        void testCreatePersonRecord() throws Exception {
            Person result = component.createPerson("Bob", 25);
            assertNotNull(result);
            assertEquals("Bob", result.getName());
            assertEquals(25, result.getAge());
            assertTrue(result.getActive());
        }
    }

    @Nested
    @DisplayName("Variants")
    class VariantsTests {
        @Test
        @DisplayName("getResult returns OperationResult.Ok variant")
        void testGetResultVariant() throws Exception {
            OperationResult result = component.getResult();
            assertNotNull(result);
            assertInstanceOf(OperationResult.Ok.class, result);
            OperationResult.Ok okResult = (OperationResult.Ok) result;
            assertEquals("Success!", okResult.value);
        }

        @Test
        @DisplayName("pickColor(0) returns Color.Red variant")
        void testPickColorRed() throws Exception {
            Color result = component.pickColor(0);
            assertNotNull(result);
            assertInstanceOf(Color.Red.class, result);
            assertEquals("red", result.getCaseName());
        }

        @Test
        @DisplayName("pickColor(1) returns Color.Green variant")
        void testPickColorGreen() throws Exception {
            Color result = component.pickColor(1);
            assertNotNull(result);
            assertInstanceOf(Color.Green.class, result);
            assertEquals("green", result.getCaseName());
        }
    }

    @Nested
    @DisplayName("Lists")
    class ListsTests {
        @Test
        @DisplayName("repeatString returns list of strings")
        void testRepeatStringList() throws Exception {
            List<String> result = component.repeatString("hello", 3);
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("hello", result.get(0));
            assertEquals("hello", result.get(1));
            assertEquals("hello", result.get(2));
        }

        @Test
        @DisplayName("sumNumbers sums a list of integers")
        void testSumNumbersList() throws Exception {
            List<Integer> numbers = new ArrayList<>();
            numbers.add(10);
            numbers.add(20);
            numbers.add(30);
            int result = component.sumNumbers(numbers);
            assertEquals(60, result);
        }

        @Test
        @DisplayName("getNames extracts names from list of Person records")
        void testGetNamesListOfRecords() throws Exception {
            List<Person> people = new ArrayList<>();
            people.add(new Person("Alice", 30, true));
            people.add(new Person("Bob", 25, true));
            List<String> result = component.getNames(people);
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Alice", result.get(0));
            assertEquals("Bob", result.get(1));
        }
    }

    @Nested
    @DisplayName("Complex Nested Types")
    class ComplexNestedTypesTests {
        @Test
        @DisplayName("filterHighValuePeople filters records by age")
        void testFilterHighValuePeople() throws Exception {
            List<Person> people = new ArrayList<>();
            people.add(new Person("Charlie", 22, true));
            people.add(new Person("Diana", 35, true));

            List<Person> result = component.filterHighValuePeople(people, 28);
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Diana", result.get(0).getName());
            assertEquals(35, result.get(0).getAge());
        }
    }
}
