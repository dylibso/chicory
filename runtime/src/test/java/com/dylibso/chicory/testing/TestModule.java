package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import java.io.File;

public class TestModule {

    private final File file;

    private Module module;

    private Instance instance;

    public static TestModule of(File file) {
        return new TestModule(file);
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
        if (this.instance == null) {
            this.instance = module.instantiate();
        }
        return this;
    }

    public TestModule instantiate(HostImports imports) {
        if (this.instance == null) {
            this.instance = module.instantiate(imports);
        }
        return this;
    }

    public File getFile() {
        return file;
    }

    public Module getModule() {
        return module;
    }

    public Instance getInstance() {
        return instance;
    }
}
