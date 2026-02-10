package com.dylibso.chicory.source.compiler;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.source.compiler.internal.Compiler;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SourceCompilerTest {

    @Test
    public void compilesSimpleAddModuleToJavaSource() throws Exception {
        WasmModule module = loadWat("/wat/add.wat");

        // Compile to Java sources using the new source compiler
        Compiler compiler =
                Compiler.builder(module)
                        .withClassName("com.dylibso.chicory.testgen.AddMachine")
                        .build();

        // Compile and print to stdout (simple test)
        compiler.compile();
    }

    private static WasmModule loadWat(String classpath) throws IOException {
        try (var is = CorpusResources.getResource(classpath.substring(1))) {
            byte[] wasm = Wat2Wasm.parse(is);
            return Parser.parse(wasm);
        }
    }
}
