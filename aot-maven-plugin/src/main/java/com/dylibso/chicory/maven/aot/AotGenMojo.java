package com.dylibso.chicory.maven.aot;

import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ValueType;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin generates an invokable library from the compiled Wasm
 */
@Mojo(name = "wasm-aot-gen", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class AotGenMojo extends AbstractMojo {

    /**
     * the wasm module to be used
     */
    @Parameter(required = true)
    private File wasmFile;

    /**
     * the name to be used by the generated class
     */
    @Parameter(required = true, defaultValue = "chicory.gen.CompiledModule")
    private String name;

    /**
     * the target folder to generate classes
     */
    @Parameter(
            required = true,
            defaultValue = "${project.basedir}/target/generated-resources/chicory-aot")
    private File targetClassFolder;

    /**
     * the target source folder to generate the Machine implementation
     */
    @Parameter(
            required = true,
            defaultValue = "${project.basedir}/target/generated-sources/chicory-aot")
    private File targetSourceFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    private String valueTypeToConverter(ValueType vt) {
        switch (vt) {
            case I32:
                return "asInt";
            case I64:
                return "asLong";
            case F32:
                return "asFloat";
            case F64:
                return "asDouble";
            default:
                throw new IllegalArgumentException("type not supported " + vt);
        }
    }

    private String javaToValueTypeToConverter(ValueType vt) {
        switch (vt) {
            case I32:
                return "i32";
            case I64:
                return "i64";
            case F32:
                return "fromFloat";
            case F64:
                return "fromDouble";
            default:
                throw new IllegalArgumentException("type not supported " + vt);
        }
    }

    @Override
    @SuppressWarnings({"StringSplitter", "deprecation"})
    public void execute() throws MojoExecutionException {
        var instance =
                Instance.builder(Parser.parse(wasmFile))
                        .withMachineFactory(inst -> new AotMachine(name, inst))
                        .withSkipImportMapping(true)
                        .withStart(false)
                        .build();
        var compiled = ((AotMachine) instance.getMachine()).compiledClass();
        var split = name.split("\\.");
        var finalFolder = targetClassFolder.toPath();
        var finalSourceFolder = targetSourceFolder.toPath();
        for (int i = 0; i < (split.length - 1); i++) {
            finalFolder = finalFolder.resolve(split[i]);
            finalSourceFolder = finalSourceFolder.resolve(split[i]);
        }
        finalFolder.toFile().mkdirs();
        var packageName = split[0];
        for (int i = 1; i < (split.length - 1); i++) {
            packageName += "." + split[i];
        }

        // Generate static Machine implementation
        finalSourceFolder.toFile().mkdirs();
        final SourceRoot dest = new SourceRoot(finalSourceFolder);

        var machineName = split[split.length - 1] + "Machine";

        var cu = new CompilationUnit(packageName);
        cu.setPackageDeclaration(packageName);
        cu.setStorage(finalSourceFolder.resolve(machineName + ".java"));

        cu.addImport("java.util.function.BiFunction");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Machine");
        cu.addImport("com.dylibso.chicory.wasm.exceptions.ChicoryException");

        var clazz = cu.addClass(machineName, Modifier.Keyword.PUBLIC);
        clazz.addImplementedType("Machine");

        // TODO: find an idiomatic way to do this with JavaParser
        var funcsInit = new NameExpr("new BiFunction[" + instance.functionCount() + "]");
        clazz.addFieldWithInitializer(
                parseType("BiFunction<Instance, Value[], Value[]>[]"),
                "funcs",
                funcsInit,
                Modifier.Keyword.PRIVATE,
                Modifier.Keyword.FINAL,
                Modifier.Keyword.STATIC);

        var callMethod = clazz.addMethod("call", Modifier.Keyword.PUBLIC);
        callMethod.addAnnotation("Override");
        callMethod.setType(parseType("Value[]"));
        callMethod.addParameter("int", "funcId");
        callMethod.addParameter(parseType("Value[]"), "args");
        var callMethodBody = callMethod.createBody();
        callMethodBody.addStatement(
                new ReturnStmt(
                        new MethodCallExpr(
                                new ArrayAccessExpr(new NameExpr("funcs"), new NameExpr("funcId")),
                                new SimpleName("apply"),
                                NodeList.nodeList(
                                        new NameExpr("instance"), new NameExpr("args")))));

        var staticInit = clazz.addStaticInitializer();

        for (var i = 0; i < instance.functionCount(); i++) {
            var type = instance.type(instance.functionType(i));

            var bodyParams = new Expression[type.params().size() + 2];
            for (var paramIdx = 0; paramIdx < type.params().size(); paramIdx++) {
                bodyParams[paramIdx] =
                        new MethodCallExpr(
                                new ArrayAccessExpr(
                                        new NameExpr("args"), new IntegerLiteralExpr(paramIdx)),
                                new SimpleName(valueTypeToConverter(type.params().get(paramIdx))));
            }
            bodyParams[type.params().size()] =
                    new MethodCallExpr(new NameExpr("instance"), new SimpleName("memory"));
            bodyParams[type.params().size() + 1] = new NameExpr("instance");

            var handleBlock = new BlockStmt();
            var body =
                    new MethodCallExpr(
                            new NameExpr(name),
                            new SimpleName("func_" + i),
                            NodeList.nodeList(bodyParams));

            if (type.returns().size() == 0) {
                handleBlock.addStatement(body);
                handleBlock.addStatement(new ReturnStmt(new NullLiteralExpr()));
            } else if (type.returns().size() == 1) {
                var bodyConversion =
                        new MethodCallExpr(
                                new NameExpr("Value"),
                                javaToValueTypeToConverter(type.returns().get(0)),
                                NodeList.nodeList(body));
                handleBlock.addStatement(
                        new ReturnStmt(
                                new ArrayCreationExpr(parseType("Value"))
                                        .setInitializer(
                                                new ArrayInitializerExpr(
                                                        NodeList.nodeList(bodyConversion)))));
            } else {
                // TODO: this should be tested
                handleBlock.addStatement(new ReturnStmt(body));
            }

            var handle =
                    new LambdaExpr()
                            .addParameter("Instance", "instance")
                            .addParameter(
                                    new com.github.javaparser.ast.body.Parameter(
                                                    parseType("Value"), "args")
                                            .setVarArgs(true))
                            .setEnclosingParameters(true)
                            .setBody(handleBlock);

            staticInit.addStatement(
                    new AssignExpr(
                            new ArrayAccessExpr(new NameExpr("funcs"), new IntegerLiteralExpr(i)),
                            handle,
                            AssignExpr.Operator.ASSIGN));
        }

        clazz.addField(
                parseType("Instance"),
                "instance",
                Modifier.Keyword.PRIVATE,
                Modifier.Keyword.FINAL);

        var constr = clazz.addConstructor(Modifier.Keyword.PUBLIC);
        constr.addParameter(parseType("Instance"), "instance");
        constr.setBody(
                new BlockStmt()
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("this.instance"),
                                        new NameExpr("instance"),
                                        AssignExpr.Operator.ASSIGN)));

        dest.add(cu);
        dest.saveAll();

        var targetFile = finalFolder.resolve(split[split.length - 1] + ".class");
        try {
            Files.write(targetFile, compiled);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write " + name, e);
        }

        Resource resource = new Resource();
        resource.setDirectory(targetClassFolder.getPath());
        project.addResource(resource);
        project.addCompileSourceRoot(targetSourceFolder.getPath());
    }
}
