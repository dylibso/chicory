package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Code Generation Integration")
class CodeGenerationIntegrationTest {

    @Nested
    @DisplayName("Full Workflow")
    class FullWorkflowTests {
        @Test
        @DisplayName("generates records, variants, and wrappers for the example component")
        void generatesRecordsVariantsAndWrappersForTheExampleComponent(@TempDir Path tempDir)
                throws IOException {
            ComponentDefinition componentDefinition = new ComponentDefinition("example", "example");

            RecordType personRecord = new RecordType("person");
            personRecord.addField("name", PrimitiveType.STRING);
            personRecord.addField("age", PrimitiveType.I32);
            personRecord.addField("active", PrimitiveType.BOOL);

            RecordType userStatusRecord = new RecordType("user-status");
            userStatusRecord.addField("active", PrimitiveType.BOOL);

            VariantType colorVariant = new VariantType("color");
            colorVariant.addCase("red", Optional.empty());
            colorVariant.addCase("green", Optional.empty());
            colorVariant.addCase("blue", Optional.empty());

            VariantType resultVariant = new VariantType("operation-result");
            resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
            resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

            componentDefinition.typeRegistry().register("person", personRecord);
            componentDefinition.typeRegistry().register("user-status", userStatusRecord);
            componentDefinition.typeRegistry().register("color", colorVariant);
            componentDefinition.typeRegistry().register("operation-result", resultVariant);

            ComponentDefinition.FunctionSignature add =
                    new ComponentDefinition.FunctionSignature("add");
            add.addParameter("a", PrimitiveType.I32);
            add.addParameter("b", PrimitiveType.I32);
            add.addReturnType(PrimitiveType.I32);
            componentDefinition.addExport(add);

            ComponentDefinition.FunctionSignature describePerson =
                    new ComponentDefinition.FunctionSignature("describe-person");
            describePerson.addParameter("p", personRecord);
            describePerson.addReturnType(PrimitiveType.STRING);
            componentDefinition.addExport(describePerson);

            ComponentDefinition.FunctionSignature createPerson =
                    new ComponentDefinition.FunctionSignature("create-person");
            createPerson.addParameter("name", PrimitiveType.STRING);
            createPerson.addParameter("age", PrimitiveType.I32);
            createPerson.addReturnType(personRecord);
            componentDefinition.addExport(createPerson);

            ComponentDefinition.FunctionSignature pickColor =
                    new ComponentDefinition.FunctionSignature("pick-color");
            pickColor.addParameter("index", PrimitiveType.I32);
            pickColor.addReturnType(colorVariant);
            componentDefinition.addExport(pickColor);

            ComponentDefinition.FunctionSignature getResult =
                    new ComponentDefinition.FunctionSignature("get-result");
            getResult.addReturnType(resultVariant);
            componentDefinition.addExport(getResult);

            new WitToJavaGenerator(componentDefinition, tempDir, "com.example.generated")
                    .generate();

            Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
            assertTrue(Files.exists(packageDir.resolve("Person.java")));
            assertTrue(Files.exists(packageDir.resolve("UserStatus.java")));
            assertTrue(Files.exists(packageDir.resolve("Color.java")));
            assertTrue(Files.exists(packageDir.resolve("OperationResult.java")));
            assertTrue(Files.exists(packageDir.resolve("ExampleComponent.java")));

            String componentCode = Files.readString(packageDir.resolve("ExampleComponent.java"));
            assertTrue(componentCode.contains("public int add(int a, int b)"));
            assertTrue(componentCode.contains("public String describePerson(Person p)"));
            assertTrue(componentCode.contains("public Person createPerson(String name, int age)"));
            assertTrue(componentCode.contains("public Color pickColor(int index)"));
            assertTrue(componentCode.contains("public OperationResult getResult()"));
        }

        @Test
        @DisplayName("generates all classes for a complete component")
        void generatesAllClassesForACompleteComponent(@TempDir Path tempDir) throws IOException {
            ComponentDefinition componentDefinition = new ComponentDefinition("myapp", "myapp");

            RecordType personRecord = new RecordType("person");
            personRecord.addField("name", PrimitiveType.STRING);

            VariantType statusVariant = new VariantType("status");
            statusVariant.addCase("active", Optional.empty());
            statusVariant.addCase("inactive", Optional.empty());

            componentDefinition.typeRegistry().register("person", personRecord);
            componentDefinition.typeRegistry().register("status", statusVariant);

            ComponentDefinition.FunctionSignature process =
                    new ComponentDefinition.FunctionSignature("process");
            process.addParameter("p", personRecord);
            process.addReturnType(statusVariant);
            componentDefinition.addExport(process);

            new WitToJavaGenerator(componentDefinition, tempDir, "com.myapp").generate();

            Path packageDir = tempDir.resolve("com").resolve("myapp");
            try (Stream<Path> files = Files.list(packageDir)) {
                assertEquals(3, files.count());
            }
            assertTrue(Files.exists(packageDir.resolve("Person.java")));
            assertTrue(Files.exists(packageDir.resolve("Status.java")));
            assertTrue(Files.exists(packageDir.resolve("MyappComponent.java")));
        }
    }

    @Nested
    @DisplayName("Generated Content")
    class GeneratedContentTests {
        @Test
        @DisplayName("generates expected record code")
        void generatesExpectedRecordCode(@TempDir Path tempDir) throws IOException {
            RecordType personRecord = new RecordType("person");
            personRecord.addField("name", PrimitiveType.STRING);
            personRecord.addField("age", PrimitiveType.I32);

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("person", personRecord);

            new RecordGenerator(componentDefinition, "com.example.generated")
                    .generate(personRecord, tempDir);

            Path personFile =
                    tempDir.resolve("com")
                            .resolve("example")
                            .resolve("generated")
                            .resolve("Person.java");
            String code = Files.readString(personFile);

            assertTrue(code.contains("@WitRecord(\"person\")"));
            assertTrue(code.contains("public class Person"));
            assertTrue(code.contains("private String name"));
            assertTrue(code.contains("private int age"));
            assertTrue(code.contains("public Person() {}"));
            assertTrue(code.contains("public Person(String name, int age)"));
            assertTrue(code.contains("public long[] encode(Memory memory)"));
            assertTrue(code.contains("public static Person decode(long[] encoded, Memory memory)"));
            assertTrue(code.contains("private static RecordType createRecordType()"));
        }

        @Test
        @DisplayName("generates expected variant code")
        void generatesExpectedVariantCode(@TempDir Path tempDir) throws IOException {
            VariantType resultVariant = new VariantType("operation-result");
            resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
            resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("operation-result", resultVariant);

            new VariantGenerator(componentDefinition, "com.example.generated")
                    .generate(resultVariant, tempDir);

            Path resultFile =
                    tempDir.resolve("com")
                            .resolve("example")
                            .resolve("generated")
                            .resolve("OperationResult.java");
            String code = Files.readString(resultFile);

            assertTrue(code.contains("@WitVariant(\"operation-result\")"));
            assertTrue(code.contains("public abstract class OperationResult"));
            assertTrue(code.contains("public static class Ok extends OperationResult"));
            assertTrue(code.contains("public static class Err extends OperationResult"));
            assertTrue(code.contains("public final String value;"));
            assertTrue(code.contains("public final int value;"));
            assertTrue(code.contains("public long[] encode(Memory memory)"));
            assertTrue(
                    code.contains(
                            "public static OperationResult decode(long[] encoded, Memory memory)"));
            assertTrue(code.contains("switch (variant.caseName())"));
        }
    }
}
