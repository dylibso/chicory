package com.dylibso.chicory.scriptengine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChicoryScriptEngineTest {

    ScriptEngineManager engines;

    @BeforeEach
    public void init() {
        engines = new ScriptEngineManager();
    }

    @Test
    public void scriptEngineLookupByName() {
        Assertions.assertNotNull(engines.getEngineByName("chicory"));
    }

    @Test
    public void scriptEngineLookupByExtension() {
        Assertions.assertNotNull(engines.getEngineByExtension("wasm"));
    }

    @Test
    public void simpleEval() throws Exception {
        ScriptEngine engine = engines.getEngineByExtension("wasm");
        byte[] wasmBytes =
                Files.readAllBytes(
                        Path.of("../wasm-corpus/src/test/resources/compiled/iterfact.wat.wasm"));
        String wasmBase64 = Base64.getEncoder().encodeToString(wasmBytes);
        Bindings bindings = engine.createBindings();
        bindings.put("arg0", 5);
        bindings.put("func", "iterFact");
        Object result = engine.eval(wasmBase64, bindings);
        Assertions.assertEquals(120, result);
    }
}
