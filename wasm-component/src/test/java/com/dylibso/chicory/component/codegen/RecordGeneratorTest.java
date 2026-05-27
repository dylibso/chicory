package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Record Generator")
class RecordGeneratorTest {

    @Nested
    @DisplayName("Single Record Generation")
    class SingleRecordGenerationTests {
        @Test
        @DisplayName("generates a single record class")
        void generatesASingleRecordClass(@TempDir Path tempDir) throws IOException {
            RecordType personRecord = new RecordType("person");
            personRecord.addField("name", PrimitiveType.STRING);
            personRecord.addField("age", PrimitiveType.I32);
            personRecord.addField("active", PrimitiveType.BOOL);

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("person", personRecord);

            new RecordGenerator(componentDefinition, "com.example.generated")
                    .generate(personRecord, tempDir);

            Path classFile =
                    tempDir.resolve("com")
                            .resolve("example")
                            .resolve("generated")
                            .resolve("Person.java");
            String generatedCode = Files.readString(classFile);

            assertTrue(Files.exists(classFile));
            assertNotNull(generatedCode);
            assertTrue(generatedCode.contains("public class Person"));
            assertTrue(generatedCode.contains("private String name"));
            assertTrue(generatedCode.contains("private int age"));
            assertTrue(generatedCode.contains("private boolean active"));
            assertTrue(generatedCode.contains("public String getName()"));
            assertTrue(generatedCode.contains("public int getAge()"));
            assertTrue(generatedCode.contains("public boolean getActive()"));
            assertTrue(generatedCode.contains("public long[] encode(Memory memory)"));
            assertTrue(
                    generatedCode.contains(
                            "public static Person decode(long[] encoded, Memory memory)"));
            assertTrue(generatedCode.contains("@WitRecord(\"person\")"));
            assertTrue(generatedCode.contains("@WitField(order = 0)"));
        }
    }

    @Nested
    @DisplayName("Multiple Record Generation")
    class MultipleRecordGenerationTests {
        @Test
        @DisplayName("generates all registered record classes")
        void generatesAllRegisteredRecordClasses(@TempDir Path tempDir) throws IOException {
            RecordType personRecord = new RecordType("person");
            personRecord.addField("name", PrimitiveType.STRING);
            personRecord.addField("age", PrimitiveType.I32);

            RecordType statusRecord = new RecordType("user-status");
            statusRecord.addField("active", PrimitiveType.BOOL);

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("person", personRecord);
            componentDefinition.typeRegistry().register("user-status", statusRecord);

            assertEquals(2, componentDefinition.typeRegistry().getAllRecordTypes().size());

            new RecordGenerator(componentDefinition, "com.example.generated").generateAll(tempDir);

            Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
            assertTrue(Files.exists(packageDir.resolve("Person.java")));
            assertTrue(Files.exists(packageDir.resolve("UserStatus.java")));
        }
    }

    @Nested
    @DisplayName("Generated Structure")
    class GeneratedStructureTests {
        @Test
        @DisplayName("includes package constructors and setters")
        void includesPackageConstructorsAndSetters(@TempDir Path tempDir) throws IOException {
            RecordType personRecord = new RecordType("person");
            personRecord.addField("name", PrimitiveType.STRING);
            personRecord.addField("age", PrimitiveType.I32);

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("person", personRecord);

            new RecordGenerator(componentDefinition, "com.example.generated")
                    .generate(personRecord, tempDir);

            String code =
                    Files.readString(
                            tempDir.resolve("com")
                                    .resolve("example")
                                    .resolve("generated")
                                    .resolve("Person.java"));

            assertTrue(code.contains("package com.example.generated;"));
            assertTrue(code.contains("import com.dylibso.chicory.component.annotation.WitRecord;"));
            assertTrue(code.contains("import java.util.HashMap;"));
            assertTrue(code.contains("public Person() {}"));
            assertTrue(code.contains("public Person(String name, int age)"));
            assertTrue(code.contains("public void setName(String name)"));
            assertTrue(code.contains("public void setAge(int age)"));
        }
    }
}
