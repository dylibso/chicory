package com.dylibso.chicory.corpus;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public final class WatGenerator {

    private WatGenerator() {
        // Prevent instantiation
    }

    public static String bigWat(int funcCount, int funcSize) {
        ArrayList<Integer> functions = new ArrayList<>();
        ArrayList<Integer> instructions = new ArrayList<>();
        for (int i = 0; i < funcCount; i++) {
            functions.add(i + 1);
        }
        for (int i = 0; i < funcSize; i++) {
            instructions.add(i + 1);
        }

        return render(
                "/com/dylibso/chicory/corpus/big.wat",
                Map.of(
                        "functions", functions,
                        "instructions", instructions));
    }

    public static String methodTooLarge(int funcSize) {
        ArrayList<Integer> instructions = new ArrayList<>();
        for (int i = 0; i < funcSize; i++) {
            instructions.add(i + 1);
        }

        return render(
                "/com/dylibso/chicory/corpus/method_too_large.wat",
                Map.of("instructions", instructions));
    }

    private static String render(String template, Map<String, Object> map) {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty(
                "classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();

        Template t = velocityEngine.getTemplate(template);

        VelocityContext context = new VelocityContext();
        for (var entry : map.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

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
