package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.imports.SpecV1TableInitHostFuncs;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import org.junit.jupiter.api.Test;

public class DummyTest {

    @Test
    public void dummy() {
        var module =
                Module.build(new File("target/compiled-wast/table_init/spec.1.wasm"))
                        .instantiate(SpecV1TableInitHostFuncs.fallback());

        // Initialize tables
        module.getExport("test").apply();
        // Call the 7th value
        ExportFunction varCheck = module.getExport("check");
        var results = varCheck.apply(Value.i32(Integer.parseUnsignedInt("7")));
        assertEquals(Integer.parseUnsignedInt("2"), results[0].asInt());
    }
}
