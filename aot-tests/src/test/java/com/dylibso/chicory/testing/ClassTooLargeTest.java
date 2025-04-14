package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.experimental.aot.AotMachine;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.jupiter.api.Test;

public class ClassTooLargeTest {

    @Test
    public void testFunc50k() throws IOException {
        var funcCount = 50_000;
        var instance =
                Instance.builder(Parser.parse(buildHugeWasm(funcCount, 0)))
                        .withMachineFactory(AotMachine::new)
                        .withStart(false)
                        .build();

        funcCount = 1000;
        var expected = 0;
        for (int i = 1; i <= funcCount; i++) {
            expected += i;
        }
        ExportFunction func1 = instance.export("func_" + funcCount);
        assertEquals(expected, func1.apply(0)[0]);
    }

    @Test
    public void testManyBigFuncs() throws IOException {
        var funcCount = 10;
        var instance =
                Instance.builder(Parser.parse(buildHugeWasm(funcCount, 15_000)))
                        .withMachineFactory(AotMachine::new)
                        .withStart(false)
                        .build();

        var expected = 0;
        for (int i = 1; i <= funcCount; i++) {
            expected += i;
        }
        ExportFunction func1 = instance.export("func_" + funcCount);
        assertEquals(expected, func1.apply(0)[0]);
    }

    public static final class Context {
        public final ArrayList<Integer> functions = new ArrayList<>();
        public final ArrayList<Integer> instructions = new ArrayList<>();
    }

    private byte[] buildHugeWasm(int funcCount, int funcSize) {
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

        Template t = velocityEngine.getTemplate("/experimental/aot/class-too-large.wat");

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
