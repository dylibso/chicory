package com.dylibso.chicory.approvals;

import static com.dylibso.chicory.wasm.Parser.parse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.compiler.internal.Compiler;
import com.dylibso.chicory.compiler.internal.Shaded;
import com.dylibso.chicory.corpus.CorpusResources;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

// To approve everything use the env var: `APPROVAL_TESTS_USE_REPORTER=AutoApproveReporter`
public class ApprovalTest {

    @Test
    public void verifyBranching() {
        verifyGeneratedBytecode("branching.wat.wasm");
    }

    @Test
    public void verifyBrTable() {
        verifyGeneratedBytecode("br_table.wat.wasm");
    }

    @Test
    public void verifyLotsOfArgs() throws Exception {
        var destPath =
                Path.of(
                        "src/test/resources/com/dylibso/chicory/approvals/ApprovalTest.verifyLotsOfArgs.approved.txt");

        if (!Files.exists(destPath)) {
            Files.writeString(destPath, renderLotsOfArgs(), StandardOpenOption.CREATE_NEW);
        }

        verifyGeneratedBytecode("lots-of-args.wat.wasm");
    }

    @Test
    public void verifyFloat() {
        verifyGeneratedBytecode("float.wat.wasm");
    }

    @Test
    public void verifyHelloWasi() {
        verifyGeneratedBytecode("hello-wasi.wat.wasm");
    }

    @Test
    public void verifyI32() {
        verifyGeneratedBytecode("i32.wat.wasm");
    }

    @Test
    public void verifyI32Renamed() {
        var module = parse(CorpusResources.getResource("compiled/i32.wat.wasm"));
        var result = Compiler.builder(module).withClassName("FOO").build().compile();
        verifyClass(result.classBytes(), (name) -> !name.equals("FOO"));
    }

    @Test
    public void verifyIterFact() {
        verifyGeneratedBytecode("iterfact.wat.wasm");
    }

    @Test
    public void verifyKitchenSink() {
        verifyGeneratedBytecode("kitchensink.wat.wasm");
    }

    @Test
    public void verifyMemory() {
        verifyGeneratedBytecode("memory.wat.wasm");
    }

    @Test
    public void verifyStart() {
        verifyGeneratedBytecode("start.wat.wasm");
    }

    @Test
    public void verifyTrap() {
        verifyGeneratedBytecode("trap.wat.wasm");
    }

    @Test
    public void verifyExceptions() {
        verifyGeneratedBytecode("exceptions.wat.wasm", (name) -> !name.contains("FuncGroup"));
    }

    @Test
    public void verifyGc() {
        verifyGeneratedBytecode("gc.wat.wasm", (name) -> !name.contains("FuncGroup"));
    }

    @Test
    public void functions10() {
        var module = parse(CorpusResources.getResource("compiled/functions_10.wat.wasm"));
        var result = Compiler.builder(module).withMaxFunctionsPerClass(5).build().compile();
        verifyClass(result.classBytes(), ApprovalTest::SKIP_Methods_CLASS);
    }

    private static void verifyGeneratedBytecode(String name) {
        verifyGeneratedBytecode(name, ApprovalTest::SKIP_Methods_CLASS);
    }

    private static void verifyGeneratedBytecode(
            String name, Function<String, Boolean> classSkipper) {
        var module = parse(CorpusResources.getResource("compiled/" + name));
        var result = Compiler.builder(module).build().compile();
        verifyClass(result.classBytes(), classSkipper);
    }

    private static boolean SKIP_Methods_CLASS(String name) {
        return name.endsWith("Shaded");
    }

    private static void verifyClass(
            Map<String, byte[]> classBytes, Function<String, Boolean> classSkipper) {
        var writer = new StringWriter();

        for (byte[] bytes : classBytes.values()) {
            ClassReader cr = new ClassReader(bytes);
            if (classSkipper.apply(cr.getClassName())) {
                continue;
            }
            cr.accept(new TraceClassVisitor(new PrintWriter(writer)), 0);
            writer.append("\n");
        }

        String output = writer.toString();
        output = output.replaceAll("(?m)^ {3}FRAME.*\\n", "");
        output = output.replaceAll("(?m)^ {4}MAX(STACK|LOCALS) = \\d+\\n", "");
        output = output.replaceAll("(?m)^ {4}(LINENUMBER|LOCALVARIABLE) .*\\n", "");
        output = output.replaceAll("(?m)^ *// .*\\n", "");
        output = output.replaceAll("\\n{3,}", "\n\n");
        output = output.strip() + "\n";

        Approvals.verify(output);

        assertFalse(
                output.contains(getInternalName(Shaded.class)),
                "Class contains non-inlined reference to " + Shaded.class.getName());
    }

    private static String renderLotsOfArgs() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty(
                "classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();

        Template t = velocityEngine.getTemplate("ApprovalTest.verifyLotsOfArgs.approved.template");

        VelocityContext context = new VelocityContext();
        context.put("iconst", IntStream.range(0, 293).toArray());
        context.put("istore", IntStream.range(0, 296).toArray());
        context.put("splats1", IntStream.range(6, 128).toArray());
        context.put("splats2", IntStream.range(128, 300).toArray());
        context.put("splats3", IntStream.range(6, 128).toArray());
        context.put("splats4", IntStream.range(128, 300).toArray());

        StringWriter writer = new StringWriter();
        t.merge(context, writer);
        writer.flush();
        return writer.toString();
    }
}
