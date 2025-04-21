package com.dylibso.chicory.corpus;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public final class WatGenerator {

    private WatGenerator() {
        // Prevent instantiation
    }

    static final class Context {
        public final ArrayList<Integer> functions = new ArrayList<>();
        public final ArrayList<Integer> instructions = new ArrayList<>();
    }

    public static String bigWat(int funcCount, int funcSize) {
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

        Template t = velocityEngine.getTemplate("/com/dylibso/chicory/corpus/big.wat");

        VelocityContext context = new VelocityContext();
        context.put("functions", ctx.functions);
        context.put("instructions", ctx.instructions);

        StringWriter writer = new StringWriter();
        t.merge(context, writer);
        writer.flush();
        return writer.toString();
    }

    public static void main(String[] args) {
        var remaining = new ArrayDeque<>(List.of(args));
        int funcCount = 50_000;
        int funcSize = 0;
        if (!remaining.isEmpty()) {
            funcCount = Integer.parseInt(remaining.removeFirst());
        }
        if (!remaining.isEmpty()) {
            funcSize = Integer.parseInt(remaining.removeFirst());
        }
        System.out.println(bigWat(funcCount, funcSize));
    }
}
