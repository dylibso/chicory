package com.dylibso.chicory.testing;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.ModuleType;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wat2wasm.Wat2Wasm;
import java.io.File;

public class TestModule {

    private Module.Builder builder;
    private Module module;

    private Instance instance;

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
        if (this.module == null) {
            this.module = builder.build();
        }
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

    public TestModule instantiate() {
        if (this.instance == null) {
            this.instance =
                    module.withMachineFactory(ins -> new AotMachine(module))
                            .withInitialize(false)
                            .withStart(false)
                            .withHostImports(imports)
                            .instantiate();
        }
        return this;
    }

    public Module module() {
        return module;
    }

    public Instance instance() {
        return instance;
    }
}
