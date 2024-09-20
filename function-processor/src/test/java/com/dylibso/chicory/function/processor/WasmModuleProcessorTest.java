package com.dylibso.chicory.function.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

class WasmModuleProcessorTest {

    @Test
    void generateModules() {
        Compilation compilation =
                javac().withProcessors(new WasmModuleProcessor())
                        .compile(resource("wasm/Demo.java"));

        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation)
                .generatedSourceFile("chicory.testing.Demo_ModuleFactory")
                .hasSourceEquivalentTo(resource("wasm/DemoGenerated.java"));
    }

    private static JavaFileObject resource(String resource) {
        return JavaFileObjects.forResource(resource);
    }
}
