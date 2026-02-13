package com.dylibso.chicory.source.compiler;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.source.compiler.internal.Compiler;
import com.dylibso.chicory.source.compiler.internal.CompilerResult;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    public void compilesWat2WasmAndDumpsSource() throws Exception {
        Path wasmPath = Path.of("../wabt/src/main/resources/wat2wasm");
        if (!Files.exists(wasmPath)) {
            System.out.println("wat2wasm binary not found at " + wasmPath + ", skipping");
            return;
        }

        byte[] wasmBytes = Files.readAllBytes(wasmPath);
        WasmModule module = Parser.parse(wasmBytes);
        System.out.println(
                "Parsed wat2wasm: " + module.codeSection().functionBodies().length + " functions");

        Compiler compiler =
                Compiler.builder(module)
                        .withClassName("com.dylibso.chicory.gen.Wat2WasmMachine")
                        .build();

        CompilerResult result = compiler.compile();

        // Dump generated source to target/source-dump/wat2wasm/
        Path dumpDir = Path.of("target/source-dump/wat2wasm");
        Files.createDirectories(dumpDir);
        for (var entry : result.collector().sourceFiles().entrySet()) {
            String fileName = entry.getKey().replace('.', '/') + ".java";
            Path outFile = dumpDir.resolve(fileName);
            Files.createDirectories(outFile.getParent());
            Files.writeString(outFile, entry.getValue());
            System.out.println("Dumped " + outFile + " (" + entry.getValue().length() + " chars)");
        }
    }

    @Test
    public void dumpWasiTestSource() throws Exception {
        Path wasmPath =
                Path.of("../wasi-testsuite/tests/rust/testsuite/wasm32-wasip1/close_preopen.wasm");
        if (!Files.exists(wasmPath)) {
            System.out.println("WASI test wasm not found at " + wasmPath + ", skipping");
            return;
        }
        byte[] wasmBytes = Files.readAllBytes(wasmPath);
        WasmModule module = Parser.parse(wasmBytes);

        Compiler compiler =
                Compiler.builder(module)
                        .withClassName("com.dylibso.chicory.gen.WasiTestMachine")
                        .build();

        CompilerResult result = compiler.compile();

        Path dumpDir = Path.of("target/source-dump/wasi-diag");
        Files.createDirectories(dumpDir);
        for (var entry : result.collector().sourceFiles().entrySet()) {
            String fileName = entry.getKey().replace('.', '/') + ".java";
            Path outFile = dumpDir.resolve(fileName);
            Files.createDirectories(outFile.getParent());
            Files.writeString(outFile, entry.getValue());
            System.out.println("Dumped " + outFile + " (" + entry.getValue().length() + " chars)");
        }
    }

    private static WasmModule loadWat(String classpath) throws IOException {
        try (var is = CorpusResources.getResource(classpath.substring(1))) {
            byte[] wasm = Wat2Wasm.parse(is);
            return Parser.parse(wasm);
        }
    }
}
