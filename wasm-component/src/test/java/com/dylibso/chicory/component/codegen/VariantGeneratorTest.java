package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.VariantType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VariantGeneratorTest {

    @Test
    void testGenerateVariantWithoutData(@TempDir Path tempDir) throws IOException {
        // Create a variant type: color { red, green, blue }
        VariantType colorVariant = new VariantType("color");
        colorVariant.addCase("red", Optional.empty());
        colorVariant.addCase("green", Optional.empty());
        colorVariant.addCase("blue", Optional.empty());

        // Create component definition
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("color", colorVariant);

        // Generate variant class
        VariantGenerator generator = new VariantGenerator(componentDef, "com.example.generated");
        generator.generate(colorVariant, tempDir);

        // Verify generated file exists
        Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
        Path classFile = packageDir.resolve("Color.java");
        assert Files.exists(classFile) : "Generated Color.java file should exist";

        // Read and verify generated code
        String generatedCode = Files.readString(classFile);
        assertNotNull(generatedCode);
        assert generatedCode.contains("public abstract class Color")
                : "Should contain abstract Color class";
        assert generatedCode.contains("@WitVariant(\"color\")")
                : "Should have @WitVariant annotation";
        assert generatedCode.contains("public static class Red extends Color")
                : "Should have Red inner class";
        assert generatedCode.contains("public static class Green extends Color")
                : "Should have Green inner class";
        assert generatedCode.contains("public static class Blue extends Color")
                : "Should have Blue inner class";
        assert generatedCode.contains("public long[] encode(Memory memory)")
                : "Should have encode method";
        assert generatedCode.contains("public static Color decode(long[] encoded, Memory memory)")
                : "Should have decode static method";
        assert generatedCode.contains("public String getCaseName()")
                : "Should have getCaseName getter";
    }

    @Test
    void testGenerateVariantWithData(@TempDir Path tempDir) throws IOException {
        // Create a variant type: operation-result { ok(string), err(s32) }
        VariantType resultVariant = new VariantType("operation-result");
        resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
        resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

        // Create component definition
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("operation-result", resultVariant);

        // Generate variant class
        VariantGenerator generator = new VariantGenerator(componentDef, "com.example.generated");
        generator.generate(resultVariant, tempDir);

        // Verify generated file exists
        Path classFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("OperationResult.java");
        assert Files.exists(classFile) : "Generated OperationResult.java file should exist";

        // Read and verify generated code
        String generatedCode = Files.readString(classFile);
        assertNotNull(generatedCode);
        assert generatedCode.contains("public abstract class OperationResult")
                : "Should contain abstract OperationResult class";
        assert generatedCode.contains("public static class Ok extends OperationResult")
                : "Should have Ok inner class";
        assert generatedCode.contains("public final String value;")
                : "Ok case should have String value field";
        assert generatedCode.contains("public static class Err extends OperationResult")
                : "Should have Err inner class";
        assert generatedCode.contains("public final int value;")
                : "Err case should have int value field";
    }

    @Test
    void testGenerateMultipleVariants(@TempDir Path tempDir) throws IOException {
        // Create variant types
        VariantType colorVariant = new VariantType("color");
        colorVariant.addCase("red", Optional.empty());
        colorVariant.addCase("green", Optional.empty());

        VariantType resultVariant = new VariantType("operation-result");
        resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
        resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

        // Create component definition
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("color", colorVariant);
        componentDef.typeRegistry().register("operation-result", resultVariant);

        // Generate all variant classes
        VariantGenerator generator = new VariantGenerator(componentDef, "com.example.generated");
        generator.generateAll(tempDir);

        // Verify both files exist
        Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
        assert Files.exists(packageDir.resolve("Color.java")) : "Should have generated Color.java";
        assert Files.exists(packageDir.resolve("OperationResult.java"))
                : "Should have generated OperationResult.java";
    }

    @Test
    void testGeneratedVariantStructure(@TempDir Path tempDir) throws IOException {
        VariantType resultVariant = new VariantType("operation-result");
        resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
        resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");
        componentDef.typeRegistry().register("operation-result", resultVariant);

        VariantGenerator generator = new VariantGenerator(componentDef, "com.example.generated");
        generator.generate(resultVariant, tempDir);

        Path classFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("OperationResult.java");
        String code = Files.readString(classFile);

        // Verify package and imports
        assert code.contains("package com.example.generated;") : "Should have correct package";
        assert code.contains("import com.dylibso.chicory.component.annotation.WitVariant;")
                : "Should import WitVariant";
        assert code.contains("import com.dylibso.chicory.component.annotation.WitCase;")
                : "Should import WitCase";
        assert code.contains("import com.dylibso.chicory.component.VariantValue;")
                : "Should import VariantValue";

        // Verify switch statement in decode
        assert code.contains("switch (variant.caseName())") : "Should have switch in decode method";
        assert code.contains("case \"ok\":") : "Should have ok case";
        assert code.contains("case \"err\":") : "Should have err case";

        // Verify constructors for inner classes
        assert code.contains("public Ok(String value)") : "Ok should have String constructor";
        assert code.contains("public Err(int value)") : "Err should have int constructor";
    }
}
