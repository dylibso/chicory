package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.ModuleType;
import java.io.File;

public class TestModule {

    private final File file;

    private Module module;

    private Instance instance;

    public static TestModule of(File file) {
        return new TestModule(file);
    }

    public static TestModule of(File file, ModuleType moduleType) {
        if (moduleType == ModuleType.TEXT) {
            throw new UnsupportedOperationException(
                    "Parsing of textual WASM sources is not implemented yet.");
        }
        return of(file);
    }

    public TestModule(File file) {
        this.file = file;
    }

    public TestModule build() {
        if (this.module == null) {
            this.module = Module.builder(file).build();
        }
        return this;
    }

    public TestModule instantiate() {
        return this.instantiate(new HostImports());
    }

    public TestModule instantiate(HostImports imports) {
        if (this.instance == null) {
            this.instance =
                    module.withInitialize(false)
                            .withStart(false)
                            .withHostImports(imports)
                            .instantiate();
        }
        return this;
    }

    public File file() {
        return file;
    }

    public Module module() {
        return module;
    }

    public Instance instance() {
        return instance;
    }
}
