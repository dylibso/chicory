package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.runtime.CompiledModule;
import com.dylibso.chicory.wabt.Wat2WasmModule;
import org.junit.jupiter.api.Test;

public class ImplementsCompiledModuleTest {

    @Test
    public void testImplementsCompiledModule() {
        CompiledModule module = new Wat2WasmModule();
        assertNotNull(module.wasmModule());
        assertNotNull(module.machineFactory());
    }
}
