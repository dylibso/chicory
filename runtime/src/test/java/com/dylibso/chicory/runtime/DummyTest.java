package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.imports.SpecV1LinkingHostFuncs;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import org.junit.jupiter.api.Test;

public class DummyTest {

    public static Instance MgInstance =
            Module.builder(new File("target/compiled-wast/linking/spec.5.wasm"))
                    .build()
                    .instantiate(SpecV1LinkingHostFuncs.Mg());

    public static Instance NgInstance =
            Module.builder(new File("target/compiled-wast/linking/spec.6.wasm"))
                    .build()
                    .instantiate(SpecV1LinkingHostFuncs.Ng());

    @Test()
    public void test() {
        ExportFunction varSetMut = MgInstance.export("set_mut");
        var setResults = varSetMut.apply(Value.i32(Integer.parseUnsignedInt("241")));
        ExportFunction varMgMutGlob = NgInstance.export("Mg.mut_glob");
        var results = varMgMutGlob.apply();
        assertEquals(Integer.parseUnsignedInt("241"), results[0].asInt());
    }
}
