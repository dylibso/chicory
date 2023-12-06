package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
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
            this.module = Module.build(file);
        }
        return this;
    }

    public TestModule instantiate() {
        if (this.instance == null) {
            this.instance = module.instantiate();
        }
        return this;
    }

    public TestModule instantiate(HostFunction[] funcs) {
        if (this.instance == null) {
            this.instance = module.instantiate(funcs);
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
