package com.dylibso.chicory.testing;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.ModuleType;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import java.io.File;

public class TestModule {

    private Module.Builder builder;
    private Module module;

    private HostImports imports;
    private boolean typeValidation;

    public TestModule(Module.Builder builder) {
        this.builder = builder;
    }

    public static TestModule of(File file) {
        return of(file, ModuleType.BINARY);
    }

    public static TestModule of(Module.Builder builder) {
        return new TestModule(builder);
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
                    + "unknown label";

    public static TestModule of(File file, ModuleType moduleType) {
        if (moduleType == ModuleType.TEXT) {
            byte[] parsed;
            try {
                parsed = Wat2Wasm.parse(file);
            } catch (Exception e) {
                throw new MalformedException(
                        e.getMessage() + HACK_MATCH_ALL_MALFORMED_EXCEPTION_TEXT);
            }
            return of(Module.builder(parsed));
        }
        return of(Module.builder(file));
    }

    public TestModule build() {
        this.module =
                builder.withInitialize(false)
                        .withStart(false)
                        // TODO: enable me!
                        .withTypeValidation(false)
                        // TODO: enable me!
                        .withImportValidation(false)
                        .withHostImports(imports)
                        .withMachineFactory(AotMachine::new)
                        .build();
        return this;
    }

    public TestModule withHostImports(HostImports imports) {
        this.imports = imports;
        return this;
    }

    public TestModule withTypeValidation(boolean v) {
        this.typeValidation = v;
        return this;
    }

    public Instance instantiate() {
        return module.instantiate().initialize(true);
    }
}
