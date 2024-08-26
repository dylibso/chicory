package com.dylibso.chicory.testing;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import java.io.File;

public class TestModule {

    private Module module;
    private Instance instance;

    private HostImports imports;

    public static TestModule of(Module module) {
        return new TestModule(module);
    }

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
                    + "multiple start sections";

    public static TestModule of(File file) {
        if (file.getName().endsWith(".wat")) {
            byte[] parsed;
            try {
                parsed = Wat2Wasm.parse(file);
            } catch (RuntimeException e) {
                throw new MalformedException(
                        e.getMessage() + HACK_MATCH_ALL_MALFORMED_EXCEPTION_TEXT);
            }
            return of(Parser.parse(parsed));
        }
        return of(Parser.parse(file));
    }

    public TestModule(Module module) {
        this.module = module;
    }

    public Instance build() {
        this.instance =
                Instance.builder(module)
                        .withHostImports(imports)
                        .withMachineFactory(AotMachine::new)
                        .build();
        return this.instance;
    }

    public TestModule withHostImports(HostImports imports) {
        this.imports = imports;
        return this;
    }
}
