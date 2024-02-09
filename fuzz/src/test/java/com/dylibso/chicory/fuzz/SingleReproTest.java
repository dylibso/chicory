package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.FunctionType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;

public class SingleReproTest extends TestModule {
    private static final String CHICORY_FUZZ_SEED_KEY = "CHICORY_FUZZ_SEED";
    private static final String CHICORY_FUZZ_TYPES_KEY = "CHICORY_FUZZ_TYPES";

    WasmSmithWrapper smith = new WasmSmithWrapper();

    boolean enableSingleReproducer() {
        return System.getenv(CHICORY_FUZZ_SEED_KEY) != null
                && System.getenv(CHICORY_FUZZ_TYPES_KEY) != null;
    }

    @Test
    @EnabledIf("enableSingleReproducer")
    void singleReproducer() throws Exception {
        var seed = System.getenv(CHICORY_FUZZ_SEED_KEY);
        var types = InstructionTypes.fromString(System.getenv(CHICORY_FUZZ_TYPES_KEY));
        var targetWasm =
                smith.run(seed.substring(0, Math.min(seed.length(), 32)), "test.wasm", types);

        var module = Module.builder(targetWasm).build();
        var instance = module.instantiate(new HostImports(), false);

        testModule(targetWasm, module, instance);
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> module.instantiate());
    }
}
