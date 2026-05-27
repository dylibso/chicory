package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Component Wrapper Generator")
class ComponentWrapperGeneratorTest {

    @Nested
    @DisplayName("Wrapper Generation")
    class WrapperGenerationTests {
        @Test
        @DisplayName("generates a wrapper for simple exports")
        void generatesAWrapperForSimpleExports(@TempDir Path tempDir) throws IOException {
            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");

            ComponentDefinition.FunctionSignature add =
                    new ComponentDefinition.FunctionSignature("add");
            add.addParameter("a", PrimitiveType.I32);
            add.addParameter("b", PrimitiveType.I32);
            add.addReturnType(PrimitiveType.I32);
            componentDefinition.addExport(add);

            ComponentDefinition.FunctionSignature greet =
                    new ComponentDefinition.FunctionSignature("greet");
            greet.addParameter("name", PrimitiveType.STRING);
            greet.addReturnType(PrimitiveType.STRING);
            componentDefinition.addExport(greet);

            new ComponentWrapperGenerator(componentDefinition, "com.example.generated")
                    .generate(tempDir);

            Path classFile =
                    tempDir.resolve("com")
                            .resolve("example")
                            .resolve("generated")
                            .resolve("ExampleComponent.java");
            String generatedCode = Files.readString(classFile);

            assertTrue(Files.exists(classFile));
            assertNotNull(generatedCode);
            assertTrue(generatedCode.contains("public class ExampleComponent"));
            assertTrue(generatedCode.contains("private final ComponentModel componentModel"));
            assertTrue(
                    generatedCode.contains(
                            "public ExampleComponent(ComponentModel componentModel)"));
            assertTrue(generatedCode.contains("public int add(int a, int b)"));
            assertTrue(generatedCode.contains("public String greet(String name)"));
            assertTrue(generatedCode.contains("componentModel.callExport(\"add\""));
            assertTrue(generatedCode.contains("componentModel.callExport(\"greet\""));
            assertTrue(generatedCode.contains("return ((Number) result).intValue();"));
            assertTrue(generatedCode.contains("return (String) result;"));
        }

        @Test
        @DisplayName("generates wrappers for void exports")
        void generatesWrappersForVoidExports(@TempDir Path tempDir) throws IOException {
            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            ComponentDefinition.FunctionSignature log =
                    new ComponentDefinition.FunctionSignature("log");
            log.addParameter("msg", PrimitiveType.STRING);
            componentDefinition.addExport(log);

            new ComponentWrapperGenerator(componentDefinition, "com.example.generated")
                    .generate(tempDir);

            String code =
                    Files.readString(
                            tempDir.resolve("com")
                                    .resolve("example")
                                    .resolve("generated")
                                    .resolve("ExampleComponent.java"));

            assertTrue(code.contains("public void log(String msg)"));
            assertTrue(code.contains("componentModel.callExport(\"log\", msg);"));
        }
    }

    @Nested
    @DisplayName("Generated Structure")
    class GeneratedStructureTests {
        @Test
        @DisplayName("includes package imports and annotations")
        void includesPackageImportsAndAnnotations(@TempDir Path tempDir) throws IOException {
            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            ComponentDefinition.FunctionSignature function =
                    new ComponentDefinition.FunctionSignature("test");
            function.addParameter("x", PrimitiveType.I32);
            function.addReturnType(PrimitiveType.I32);
            componentDefinition.addExport(function);

            new ComponentWrapperGenerator(componentDefinition, "com.example.generated")
                    .generate(tempDir);

            String code =
                    Files.readString(
                            tempDir.resolve("com")
                                    .resolve("example")
                                    .resolve("generated")
                                    .resolve("ExampleComponent.java"));

            assertTrue(code.contains("package com.example.generated;"));
            assertTrue(code.contains("import com.dylibso.chicory.component.ComponentModel;"));
            assertTrue(
                    code.contains("import com.dylibso.chicory.component.annotation.WitComponent;"));
            assertTrue(code.contains("@WitComponent(\"example\")"));
            assertTrue(code.contains("throws Exception"));
        }

        @Test
        @DisplayName("marshals multiple parameters in order")
        void marshalsMultipleParametersInOrder(@TempDir Path tempDir) throws IOException {
            ComponentDefinition componentDefinition =
                    new ComponentDefinition("com.example", "example");
            ComponentDefinition.FunctionSignature process =
                    new ComponentDefinition.FunctionSignature("process");
            process.addParameter("a", PrimitiveType.I32);
            process.addParameter("b", PrimitiveType.I64);
            process.addParameter("c", PrimitiveType.STRING);
            process.addReturnType(PrimitiveType.BOOL);
            componentDefinition.addExport(process);

            new ComponentWrapperGenerator(componentDefinition, "com.example.generated")
                    .generate(tempDir);

            String code =
                    Files.readString(
                            tempDir.resolve("com")
                                    .resolve("example")
                                    .resolve("generated")
                                    .resolve("ExampleComponent.java"));

            assertTrue(code.contains("public boolean process(int a, long b, String c)"));
            assertTrue(code.contains("componentModel.callExport(\"process\", a, b, c)"));
            assertTrue(code.contains("return (Boolean) result;"));
        }
    }
}
