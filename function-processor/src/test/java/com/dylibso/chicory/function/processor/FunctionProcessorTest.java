package com.dylibso.chicory.function.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

class FunctionProcessorTest {

    @Test
    void generateModules() {
        Compilation compilation =
                javac().withProcessors(new FunctionProcessor())
                        .compile(
                                JavaFileObjects.forResource("BasicMath.java"),
                                JavaFileObjects.forResource("Box.java"),
                                JavaFileObjects.forResource("NoPackage.java"),
                                JavaFileObjects.forResource("Simple.java"));

        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.BasicMath_ModuleFactory")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("BasicMathGenerated.java"));

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.Simple_ModuleFactory")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("SimpleGenerated.java"));

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.Nested_ModuleFactory")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("NestedGenerated.java"));

        assertThat(compilation)
                .generatedSourceFile("NoPackage_ModuleFactory")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("NoPackageGenerated.java"));
    }

    @Test
    void invalidParameterType() {
        Compilation compilation =
                javac().withProcessors(new FunctionProcessor())
                        .compile(JavaFileObjects.forResource("InvalidParameter.java"));

        assertThat(compilation).failed();

        assertThat(compilation)
                .hadErrorContaining("Unsupported WASM type: java.lang.String")
                .inFile(JavaFileObjects.forResource("InvalidParameter.java"))
                .onLineContaining("public long concat(int a, String s) {");
    }

    @Test
    void invalidReturnType() {
        Compilation compilation =
                javac().withProcessors(new FunctionProcessor())
                        .compile(JavaFileObjects.forResource("InvalidReturn.java"));

        assertThat(compilation).failed();

        assertThat(compilation)
                .hadErrorContaining("Unsupported WASM type: java.lang.String")
                .inFile(JavaFileObjects.forResource("InvalidReturn.java"))
                .onLineContaining("public String toString(int x) {");
    }
}
