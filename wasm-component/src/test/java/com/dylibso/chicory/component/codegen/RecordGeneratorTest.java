package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordGeneratorTest {

    @Test
    void testGenerateSingleRecord(@TempDir Path tempDir) throws IOException {
        // Create a simple record type: person { name: string, age: s32, active: bool }
        RecordType personRecord = new RecordType("person");
        personRecord.addField("name", PrimitiveType.STRING);
        personRecord.addField("age", PrimitiveType.I32);
        personRecord.addField("active", PrimitiveType.BOOL);

        // Create component definition and add record to registry
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("person", personRecord);

        // Generate record class
        RecordGenerator generator = new RecordGenerator(componentDef, "com.example.generated");
        generator.generate(personRecord, tempDir);

        // Verify generated file exists
        Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
        Path classFile = packageDir.resolve("Person.java");
        assert Files.exists(classFile) : "Generated Person.java file should exist";

        // Read and verify generated code
        String generatedCode = Files.readString(classFile);
        assertNotNull(generatedCode);
        assert generatedCode.contains("public class Person") : "Should contain Person class";
        assert generatedCode.contains("private String name") : "Should have name field";
        assert generatedCode.contains("private int age") : "Should have age field";
        assert generatedCode.contains("private boolean active") : "Should have active field";
        assert generatedCode.contains("public String getName()") : "Should have getName getter";
        assert generatedCode.contains("public int getAge()") : "Should have getAge getter";
        assert generatedCode.contains("public boolean getActive()")
                : "Should have getActive getter";
        assert generatedCode.contains("public long[] encode(Memory memory)")
                : "Should have encode method";
        assert generatedCode.contains("public static Person decode(long[] encoded, Memory memory)")
                : "Should have decode method";
        assert generatedCode.contains("@WitRecord(\"person\")")
                : "Should have @WitRecord annotation";
        assert generatedCode.contains("@WitField(order = 0)") : "Should have @WitField annotations";
    }

    @Test
    void testGenerateMultipleRecords(@TempDir Path tempDir) throws IOException {
        // Create record types
        RecordType personRecord = new RecordType("person");
        personRecord.addField("name", PrimitiveType.STRING);
        personRecord.addField("age", PrimitiveType.I32);

        RecordType statusRecord = new RecordType("user-status");
        statusRecord.addField("active", PrimitiveType.BOOL);

        // Create component definition
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("person", personRecord);
        componentDef.typeRegistry().register("user-status", statusRecord);

        // Verify types are registered
        assert componentDef.typeRegistry().getAllRecordTypes().size() == 2
                : "Should have 2 record types";

        // Generate all record classes
        RecordGenerator generator = new RecordGenerator(componentDef, "com.example.generated");
        generator.generateAll(tempDir);

        // Verify both files exist
        Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
        // List files in directory for debugging
        if (Files.exists(packageDir)) {
            Files.list(packageDir)
                    .forEach(p -> System.out.println("Generated: " + p.getFileName()));
        }
        assert Files.exists(packageDir.resolve("Person.java"))
                : "Should have generated Person.java";
        assert Files.exists(packageDir.resolve("UserStatus.java"))
                : "Should have generated UserStatus.java";
    }

    @Test
    void testGeneratedClassStructure(@TempDir Path tempDir) throws IOException {
        RecordType personRecord = new RecordType("person");
        personRecord.addField("name", PrimitiveType.STRING);
        personRecord.addField("age", PrimitiveType.I32);

        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("person", personRecord);

        RecordGenerator generator = new RecordGenerator(componentDef, "com.example.generated");
        generator.generate(personRecord, tempDir);

        Path classFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("Person.java");
        String code = Files.readString(classFile);

        // Verify package and imports
        assert code.contains("package com.example.generated;") : "Should have correct package";
        assert code.contains("import com.dylibso.chicory.component.annotation.WitRecord;")
                : "Should import WitRecord";
        assert code.contains("import java.util.HashMap;") : "Should import HashMap for encode";

        // Verify constructors
        assert code.contains("public Person() {}") : "Should have no-arg constructor";
        assert code.contains("public Person(String name, int age)")
                : "Should have all-args constructor";

        // Verify setters
        assert code.contains("public void setName(String name)") : "Should have name setter";
        assert code.contains("public void setAge(int age)") : "Should have age setter";
    }
}
