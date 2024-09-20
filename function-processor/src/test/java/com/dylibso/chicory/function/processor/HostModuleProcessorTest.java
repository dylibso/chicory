package com.dylibso.chicory.function.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

class HostModuleProcessorTest {

    @Test
    void generateModules() {
        Compilation compilation =
                javac().withProcessors(new HostModuleProcessor())
                        .compile(
                                resource("host/BasicMath.java"),
                                resource("host/Box.java"),
                                resource("host/NoPackage.java"),
                                resource("host/Simple.java"));

        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.BasicMath_ModuleFactory")
                .hasSourceEquivalentTo(resource("host/BasicMathGenerated.java"));

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.Simple_ModuleFactory")
                .hasSourceEquivalentTo(resource("host/SimpleGenerated.java"));

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.Nested_ModuleFactory")
                .hasSourceEquivalentTo(resource("host/NestedGenerated.java"));

        assertThat(compilation)
                .generatedSourceFile("NoPackage_ModuleFactory")
                .hasSourceEquivalentTo(resource("host/NoPackageGenerated.java"));
    }

    @Test
    void invalidParameterTypeUnsupported() {
        Compilation compilation =
                javac().withProcessors(new HostModuleProcessor())
                        .compile(resource("host/InvalidParameterUnsupported.java"));

        assertThat(compilation).failed();

        assertThat(compilation)
                .hadErrorContaining("Unsupported WASM type: java.math.BigDecimal")
                .inFile(resource("host/InvalidParameterUnsupported.java"))
                .onLineContaining("public long square(BigDecimal x) {");
    }

    @Test
    void invalidParameterTypeString() {
        Compilation compilation =
                javac().withProcessors(new HostModuleProcessor())
                        .compile(resource("host/InvalidParameterString.java"));

        assertThat(compilation).failed();

        assertThat(compilation)
                .hadErrorContaining("Missing annotation for WASM type: java.lang.String")
                .inFile(resource("host/InvalidParameterString.java"))
                .onLineContaining("public long concat(int a, String s) {");
    }

    @Test
    void invalidReturnType() {
        Compilation compilation =
                javac().withProcessors(new HostModuleProcessor())
                        .compile(resource("host/InvalidReturn.java"));

        assertThat(compilation).failed();

        assertThat(compilation)
                .hadErrorContaining("Unsupported WASM type: java.lang.String")
                .inFile(resource("host/InvalidReturn.java"))
                .onLineContaining("public String toString(int x) {");
    }

    private static JavaFileObject resource(String resource) {
        return JavaFileObjects.forResource(resource);
    }
}
