package com.dylibso.chicory.component.codegen;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.types.PrimitiveType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComponentWrapperGeneratorTest {

    @Test
    void testGenerateSimpleComponentWrapper(@TempDir Path tempDir) throws IOException {
        // Create component definition with simple export functions
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");

        // Add simple function: add(s32, s32) -> s32
        ComponentDefinition.FunctionSignature addFunc =
                new ComponentDefinition.FunctionSignature("add");
        addFunc.addParameter("a", PrimitiveType.I32);
        addFunc.addParameter("b", PrimitiveType.I32);
        addFunc.addReturnType(PrimitiveType.I32);
        componentDef.addExport(addFunc);

        // Add string function: greet(string) -> string
        ComponentDefinition.FunctionSignature greetFunc =
                new ComponentDefinition.FunctionSignature("greet");
        greetFunc.addParameter("name", PrimitiveType.STRING);
        greetFunc.addReturnType(PrimitiveType.STRING);
        componentDef.addExport(greetFunc);

        // Generate component wrapper
        ComponentWrapperGenerator generator =
                new ComponentWrapperGenerator(componentDef, "com.example.generated");
        generator.generate(tempDir);

        // Verify generated file exists
        Path packageDir = tempDir.resolve("com").resolve("example").resolve("generated");
        Path classFile = packageDir.resolve("ExampleComponent.java");
        assert Files.exists(classFile) : "Generated ExampleComponent.java file should exist";

        // Read and verify generated code
        String generatedCode = Files.readString(classFile);
        assertNotNull(generatedCode);
        assert generatedCode.contains("public class ExampleComponent")
                : "Should contain ExampleComponent class";
        assert generatedCode.contains("private final ComponentModel componentModel")
                : "Should have ComponentModel field";
        assert generatedCode.contains("public ExampleComponent(ComponentModel componentModel)")
                : "Should have constructor";
        assert generatedCode.contains("public int add(int a, int b)")
                : "Should have typed add method";
        assert generatedCode.contains("public String greet(String name)")
                : "Should have typed greet method";
        assert generatedCode.contains("componentModel.callExport(\"add\"")
                : "Should call add export";
        assert generatedCode.contains("componentModel.callExport(\"greet\"")
                : "Should call greet export";
        assert generatedCode.contains("return ((Number) result).intValue();")
                : "Should cast int return values";
        assert generatedCode.contains("return (String) result;")
                : "Should cast string return values";
    }

    @Test
    void testGenerateComponentWithVoidFunction(@TempDir Path tempDir) throws IOException {
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");

        // Add void function: log(string)
        ComponentDefinition.FunctionSignature logFunc =
                new ComponentDefinition.FunctionSignature("log");
        logFunc.addParameter("msg", PrimitiveType.STRING);
        componentDef.addExport(logFunc);

        ComponentWrapperGenerator generator =
                new ComponentWrapperGenerator(componentDef, "com.example.generated");
        generator.generate(tempDir);

        Path classFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("ExampleComponent.java");
        String code = Files.readString(classFile);

        assert code.contains("public void log(String msg)") : "Should have void method";
        assert code.contains("componentModel.callExport(\"log\", msg);")
                : "Should call function without return casting";
    }

    @Test
    void testGeneratedComponentStructure(@TempDir Path tempDir) throws IOException {
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");

        ComponentDefinition.FunctionSignature func =
                new ComponentDefinition.FunctionSignature("test");
        func.addParameter("x", PrimitiveType.I32);
        func.addReturnType(PrimitiveType.I32);
        componentDef.addExport(func);

        ComponentWrapperGenerator generator =
                new ComponentWrapperGenerator(componentDef, "com.example.generated");
        generator.generate(tempDir);

        Path classFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("ExampleComponent.java");
        String code = Files.readString(classFile);

        // Verify package and imports
        assert code.contains("package com.example.generated;") : "Should have correct package";
        assert code.contains("import com.dylibso.chicory.component.ComponentModel;")
                : "Should import ComponentModel";
        assert code.contains("import com.dylibso.chicory.component.annotation.WitComponent;")
                : "Should import WitComponent";
        assert code.contains("@WitComponent(\"example\")") : "Should have @WitComponent annotation";

        // Verify method structure
        assert code.contains("throws Exception") : "Methods should declare throws Exception";
    }

    @Test
    void testMultipleParameterMarshalling(@TempDir Path tempDir) throws IOException {
        ComponentDefinition componentDef = new ComponentDefinition("com.example", "example");

        // Function with multiple parameters
        ComponentDefinition.FunctionSignature func =
                new ComponentDefinition.FunctionSignature("process");
        func.addParameter("a", PrimitiveType.I32);
        func.addParameter("b", PrimitiveType.I64);
        func.addParameter("c", PrimitiveType.STRING);
        func.addReturnType(PrimitiveType.BOOL);
        componentDef.addExport(func);

        ComponentWrapperGenerator generator =
                new ComponentWrapperGenerator(componentDef, "com.example.generated");
        generator.generate(tempDir);

        Path classFile =
                tempDir.resolve("com")
                        .resolve("example")
                        .resolve("generated")
                        .resolve("ExampleComponent.java");
        String code = Files.readString(classFile);

        assert code.contains("public boolean process(int a, long b, String c)")
                : "Should have correct parameter types";
        assert code.contains("componentModel.callExport(\"process\", a, b, c)")
                : "Should pass all parameters";
        assert code.contains("return (Boolean) result;") : "Should cast boolean return";
    }
}
