package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.VariantType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Variant Generator")
class VariantGeneratorTest {

    @Nested
    @DisplayName("Single Variant Generation")
    class SingleVariantGenerationTests {
        @Test
        @DisplayName("generates variants without payloads")
        void generatesVariantsWithoutPayloads(@TempDir Path tempDir) throws IOException {
            VariantType colorVariant = new VariantType("color");
            colorVariant.addCase("red", Optional.empty());
            colorVariant.addCase("green", Optional.empty());
            colorVariant.addCase("blue", Optional.empty());

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("color", colorVariant);

            new VariantGenerator(componentDefinition, "com.example.generated")
                    .generate(colorVariant, tempDir);

            Path classFile =
                    tempDir.resolve("com")
                            .resolve("example")
                            .resolve("generated")
                            .resolve("Color.java");
            String generatedCode = Files.readString(classFile);

            assertTrue(Files.exists(classFile));
            assertNotNull(generatedCode);
            assertTrue(generatedCode.contains("public abstract class Color"));
            assertTrue(generatedCode.contains("@WitVariant(\"color\")"));
            assertTrue(generatedCode.contains("public static class Red extends Color"));
            assertTrue(generatedCode.contains("public static class Green extends Color"));
            assertTrue(generatedCode.contains("public static class Blue extends Color"));
            assertTrue(generatedCode.contains("public long[] encode(Memory memory)"));
            assertTrue(
                    generatedCode.contains(
                            "public static Color decode(long[] encoded, Memory memory)"));
            assertTrue(generatedCode.contains("public String getCaseName()"));
        }

        @Test
        @DisplayName("generates variants with payloads")
        void generatesVariantsWithPayloads(@TempDir Path tempDir) throws IOException {
            VariantType resultVariant = new VariantType("operation-result");
            resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
            resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("operation-result", resultVariant);

            new VariantGenerator(componentDefinition, "com.example.generated")
                    .generate(resultVariant, tempDir);

            String generatedCode =
                    Files.readString(
                            tempDir.resolve("com")
                                    .resolve("example")
                                    .resolve("generated")
                                    .resolve("OperationResult.java"));

            assertNotNull(generatedCode);
            assertTrue(generatedCode.contains("public abstract class OperationResult"));
            assertTrue(generatedCode.contains("public static class Ok extends OperationResult"));
            assertTrue(generatedCode.contains("public final String value;"));
            assertTrue(generatedCode.contains("public static class Err extends OperationResult"));
            assertTrue(generatedCode.contains("public final int value;"));
        }
    }

    @Nested
    @DisplayName("Multiple Variant Generation")
    class MultipleVariantGenerationTests {
        @Test
        @DisplayName("generates all registered variants")
        void generatesAllRegisteredVariants(@TempDir Path tempDir) throws IOException {
            VariantType colorVariant = new VariantType("color");
            colorVariant.addCase("red", Optional.empty());
            colorVariant.addCase("green", Optional.empty());

            VariantType resultVariant = new VariantType("operation-result");
            resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
            resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("color", colorVariant);
            componentDefinition.typeRegistry().register("operation-result", resultVariant);

            new VariantGenerator(componentDefinition, "com.example.generated").generateAll(tempDir);

            Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
            assertTrue(Files.exists(packageDir.resolve("Color.java")));
            assertTrue(Files.exists(packageDir.resolve("OperationResult.java")));
        }
    }

    @Nested
    @DisplayName("Generated Structure")
    class GeneratedStructureTests {
        @Test
        @DisplayName("includes imports switch cases and constructors")
        void includesImportsSwitchCasesAndConstructors(@TempDir Path tempDir) throws IOException {
            VariantType resultVariant = new VariantType("operation-result");
            resultVariant.addCase("ok", Optional.of(PrimitiveType.STRING));
            resultVariant.addCase("err", Optional.of(PrimitiveType.I32));

            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            componentDefinition.typeRegistry().register("operation-result", resultVariant);

            new VariantGenerator(componentDefinition, "com.example.generated")
                    .generate(resultVariant, tempDir);

            String code =
                    Files.readString(
                            tempDir.resolve("com")
                                    .resolve("example")
                                    .resolve("generated")
                                    .resolve("OperationResult.java"));

            assertTrue(code.contains("package com.example.generated;"));
            assertTrue(
                    code.contains("import com.dylibso.chicory.component.annotation.WitVariant;"));
            assertTrue(code.contains("import com.dylibso.chicory.component.annotation.WitCase;"));
            assertTrue(code.contains("import com.dylibso.chicory.component.VariantValue;"));
            assertTrue(code.contains("switch (variant.caseName())"));
            assertTrue(code.contains("case \"ok\":"));
            assertTrue(code.contains("case \"err\":"));
            assertTrue(code.contains("public Ok(String value)"));
            assertTrue(code.contains("public Err(int value)"));
        }
    }
}
