package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for Phase 11 code generation.
 * Tests the full workflow: from WIT component definition to generated Java classes.
 */
class CodeGenerationIntegrationTest {

    @Test
    void testGenerateExampleComponentWithRecordsAndVariants(@TempDir Path tempDir)
            throws IOException {
        // Build component definition similar to example.wit
        ComponentDefinition componentDef = new ComponentDefinition("example", "example");

        // Create record types
        RecordType personRecord = new RecordType("person");
        personRecord.addField("name", PrimitiveType.STRING);
        personRecord.addField("age", PrimitiveType.I32);
        personRecord.addField("active", PrimitiveType.BOOL);

        RecordType userStatusRecord = new RecordType("user-status");
        userStatusRecord.addField("active", PrimitiveType.BOOL);

        // Create variant types
        VariantType colorVariant = new VariantType("color");
        colorVariant.addCase("red", Optional.empty());
        colorVariant.addCase("green", Optional.empty());
        colorVariant.addCase("blue", Optional.empty());

        VariantType resultVariant = new VariantType("operation-result");
        resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
        resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

        // Register types
        componentDef.typeRegistry().register("person", personRecord);
        componentDef.typeRegistry().register("user-status", userStatusRecord);
        componentDef.typeRegistry().register("color", colorVariant);
        componentDef.typeRegistry().register("operation-result", resultVariant);

        // Add export functions
        ComponentDefinition.FunctionSignature add =
                new ComponentDefinition.FunctionSignature("add");
        add.addParameter("a", PrimitiveType.I32);
        add.addParameter("b", PrimitiveType.I32);
        add.addReturnType(PrimitiveType.I32);
        componentDef.addExport(add);

        ComponentDefinition.FunctionSignature describePerson =
                new ComponentDefinition.FunctionSignature("describe-person");
        describePerson.addParameter("p", personRecord);
        describePerson.addReturnType(PrimitiveType.STRING);
        componentDef.addExport(describePerson);

        ComponentDefinition.FunctionSignature createPerson =
                new ComponentDefinition.FunctionSignature("create-person");
        createPerson.addParameter("name", PrimitiveType.STRING);
        createPerson.addParameter("age", PrimitiveType.I32);
        createPerson.addReturnType(personRecord);
        componentDef.addExport(createPerson);

        ComponentDefinition.FunctionSignature pickColor =
                new ComponentDefinition.FunctionSignature("pick-color");
        pickColor.addParameter("index", PrimitiveType.I32);
        pickColor.addReturnType(colorVariant);
        componentDef.addExport(pickColor);

        ComponentDefinition.FunctionSignature getResult =
                new ComponentDefinition.FunctionSignature("get-result");
        getResult.addReturnType(resultVariant);
        componentDef.addExport(getResult);

        // Generate all classes
        String outputPackage = "com.example.generated";
        WitToJavaGenerator generator = new WitToJavaGenerator(componentDef, tempDir, outputPackage);
        generator.generate();

        // Verify record classes
        Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
        Path personFile = packageDir.resolve("Person.java");
        Path userStatusFile = packageDir.resolve("UserStatus.java");

        assert Files.exists(personFile) : "Should generate Person.java";
        assert Files.exists(userStatusFile) : "Should generate UserStatus.java";

        // Verify variant classes
        Path colorFile = packageDir.resolve("Color.java");
        Path operationResultFile = packageDir.resolve("OperationResult.java");

        assert Files.exists(colorFile) : "Should generate Color.java";
        assert Files.exists(operationResultFile) : "Should generate OperationResult.java";

        // Verify component wrapper
        Path componentFile = packageDir.resolve("ExampleComponent.java");
        assert Files.exists(componentFile) : "Should generate ExampleComponent.java";

        // Verify component wrapper content
        String componentCode = Files.readString(componentFile);
        assert componentCode.contains("public int add(int a, int b)")
                : "Should have typed add method";
        assert componentCode.contains("public String describePerson(Person p)")
                : "Should have describe-person method with Person parameter";
        assert componentCode.contains("public Person createPerson(String name, int age)")
                : "Should have create-person method returning Person";
        assert componentCode.contains("public Color pickColor(int index)")
                : "Should have pick-color method";
        assert componentCode.contains("public OperationResult getResult()")
                : "Should have get-result method";
    }

    @Test
    void testGeneratedRecordCodeContent(@TempDir Path tempDir) throws IOException {
        RecordType personRecord = new RecordType("person");
        personRecord.addField("name", PrimitiveType.STRING);
        personRecord.addField("age", PrimitiveType.I32);

        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("person", personRecord);

        RecordGenerator recordGen = new RecordGenerator(componentDef, "com.example.generated");
        recordGen.generate(personRecord, tempDir);

        Path personFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("Person.java");
        String code = Files.readString(personFile);

        // Verify structure
        assert code.contains("@WitRecord(\"person\")") : "Should have @WitRecord annotation";
        assert code.contains("public class Person") : "Should be public class Person";
        assert code.contains("private String name") : "Should have name field";
        assert code.contains("private int age") : "Should have age field";

        // Verify constructors
        assert code.contains("public Person() {}") : "Should have no-arg constructor";
        assert code.contains("public Person(String name, int age)")
                : "Should have all-args constructor";

        // Verify encode/decode
        assert code.contains("public long[] encode(Memory memory)") : "Should have encode method";
        assert code.contains("public static Person decode(long[] encoded, Memory memory)")
                : "Should have decode method";

        // Verify helper method
        assert code.contains("private static RecordType createRecordType()")
                : "Should have createRecordType helper";
    }

    @Test
    void testGeneratedVariantCodeContent(@TempDir Path tempDir) throws IOException {
        VariantType resultVariant = new VariantType("operation-result");
        resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
        resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("operation-result", resultVariant);

        VariantGenerator variantGen = new VariantGenerator(componentDef, "com.example.generated");
        variantGen.generate(resultVariant, tempDir);

        Path resultFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("OperationResult.java");
        String code = Files.readString(resultFile);

        // Verify structure
        assert code.contains("@WitVariant(\"operation-result\")")
                : "Should have @WitVariant annotation";
        assert code.contains("public abstract class OperationResult")
                : "Should be abstract base class";

        // Verify case classes
        assert code.contains("public static class Ok extends OperationResult")
                : "Should have Ok case";
        assert code.contains("public static class Err extends OperationResult")
                : "Should have Err case";
        assert code.contains("public final String value;") : "Ok should have String value";
        assert code.contains("public final int value;") : "Err should have int value";

        // Verify encode/decode
        assert code.contains("public long[] encode(Memory memory)") : "Should have encode method";
        assert code.contains("public static OperationResult decode(long[] encoded, Memory memory)")
                : "Should have decode method";

        // Verify switch statement
        assert code.contains("switch (variant.caseName())") : "Should have switch in decode";
    }

    @Test
    void testAllClassesGeneratedForCompleteComponent(@TempDir Path tempDir) throws IOException {
        ComponentDefinition componentDef = new ComponentDefinition("myapp", "myapp");

        // Create all types
        RecordType personRecord = new RecordType("person");
        personRecord.addField("name", PrimitiveType.STRING);

        VariantType statusVariant = new VariantType("status");
        statusVariant.addCase("active", Optional.empty());
        statusVariant.addCase("inactive", Optional.empty());

        componentDef.typeRegistry().register("person", personRecord);
        componentDef.typeRegistry().register("status", statusVariant);

        // Add exports
        ComponentDefinition.FunctionSignature func =
                new ComponentDefinition.FunctionSignature("process");
        func.addParameter("p", personRecord);
        func.addReturnType(statusVariant);
        componentDef.addExport(func);

        // Generate
        WitToJavaGenerator generator = new WitToJavaGenerator(componentDef, tempDir, "com.myapp");
        generator.generate();

        // Verify all files exist
        Path packageDir = tempDir.resolve("com").resolve("myapp");
        assertEquals(3, Files.list(packageDir).count(), "Should have 3 generated classes");
        assert Files.exists(packageDir.resolve("Person.java")) : "Should have Person.java";
        assert Files.exists(packageDir.resolve("Status.java")) : "Should have Status.java";
        assert Files.exists(packageDir.resolve("MyappComponent.java"))
                : "Should have MyappComponent.java";
    }
}
