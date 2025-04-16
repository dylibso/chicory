package com.dylibso.chicory.wabt;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Wat2WasmTest {

    @Test
    @Order(0)
    public void shouldRunWat2Wasm() throws Exception {
        var result = Wat2Wasm.parse(new File("../wasm-corpus/src/main/resources/wat/iterfact.wat"));

        assertTrue(result.length > 0);
        assertTrue(new String(result, UTF_8).contains("iterFact"));
    }

    @Test
    @Order(1)
    public void shouldRunWat2WasmOnString() {
        var moduleInstance =
                Instance.builder(
                                Parser.parse(
                                        Wat2Wasm.parse(
                                                "(module (func (export \"add\") (param $x"
                                                        + " i32) (param $y i32) (result i32)"
                                                        + " (i32.add (local.get $x) (local.get"
                                                        + " $y))))")))
                        .withInitialize(true)
                        .build();

        var addFunction = moduleInstance.export("add");
        var results = addFunction.apply(1, 41);
        assertEquals(42L, results[0]);
    }

    @Test
    @Order(2)
    public void shouldThrowMalformedException() throws Exception {
        var exitException =
                assertThrows(
                        WasiExitException.class,
                        () ->
                                Wat2Wasm.parse(
                                        new File(
                                                "src/test/resources/utf8-invalid-encoding-spec.0.wat")));

        assertEquals(1, exitException.exitCode());
    }

    @Test
    @Order(3)
    public void canCompile50kFunctions() throws IOException {
        var funcCount = 50_000;
        buildBigWasm(funcCount, 0);
    }

    @Test
    @Order(4)
    public void canCompileBigFunctions() throws IOException {
        var funcCount = 10;
        buildBigWasm(funcCount, 15_000);
    }

    public static final class Context {
        public final ArrayList<Integer> functions = new ArrayList<>();
        public final ArrayList<Integer> instructions = new ArrayList<>();
    }

    private byte[] buildBigWasm(int funcCount, int funcSize) {
        var ctx = new Context();
        for (int i = 0; i < funcCount; i++) {
            ctx.functions.add(i + 1);
        }
        for (int i = 0; i < funcSize; i++) {
            ctx.instructions.add(i + 1);
        }

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty(
                "classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();

        Template t = velocityEngine.getTemplate("/big.wat");

        VelocityContext context = new VelocityContext();
        context.put("functions", ctx.functions);
        context.put("instructions", ctx.instructions);

        StringWriter writer = new StringWriter();
        t.merge(context, writer);
        writer.flush();
        String wat = writer.toString();

        return Wat2Wasm.parse(wat);
    }
}
