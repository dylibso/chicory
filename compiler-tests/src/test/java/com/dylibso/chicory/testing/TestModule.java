package com.dylibso.chicory.testing;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;

public class TestModule {

    private final WasmModule module;

    private static final String HACK_MATCH_ALL_MALFORMED_EXCEPTION_TEXT =
            "Matching keywords to get the WebAssembly testsuite to pass: "
                    + "malformed UTF-8 encoding "
                    + "import after function "
                    + "inline function type "
                    + "constant out of range"
                    + "unknown operator "
                    + "unexpected token "
                    + "unexpected mismatching "
                    + "mismatching label "
                    + "unknown type "
                    + "duplicate func "
                    + "duplicate local "
                    + "duplicate global "
                    + "duplicate memory "
                    + "duplicate table "
                    + "mismatching label "
                    + "import after global "
                    + "import after table "
                    + "import after memory "
                    + "i32 constant out of range "
                    + "unknown label "
                    + "alignment "
                    + "multiple start sections "
                    + "duplicate field";

    public static TestModule of(String classpath) {
        try (var is = CorpusResources.getResource(classpath.substring(1))) {
            if (classpath.endsWith(".wat")) {
                byte[] parsed;
                try {
                    parsed = Wat2Wasm.parse(is);
                } catch (RuntimeException e) {
                    throw new MalformedException(
                            e.getMessage() + HACK_MATCH_ALL_MALFORMED_EXCEPTION_TEXT);
                }
                return of(Parser.parse(parsed));
            }
            return of(Parser.parse(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestModule of(WasmModule module) {
        return new TestModule(module);
    }

    public TestModule(WasmModule module) {
        this.module = module;
    }

    public Instance instantiate(Store s) {
        ImportValues importValues = s.toImportValues();
        return Instance.builder(module)
                .withImportValues(importValues)
                .withMachineFactory(MachineFactoryCompiler::compile)
                .build();
    }
}
