package com.dylibso.chicory;

import static com.github.javaparser.StaticJavaParser.parseType;
import static com.github.javaparser.utils.StringEscapeUtils.escapeJava;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.ValueType;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin will generate bindings for Wasm modules
 */
@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES, threadSafe = true)
public class BindGenMojo extends AbstractMojo {

    private final Log log = new SystemStreamLog();

    /**
     * Package name
     */
    @Parameter(required = true, defaultValue = "com.dylibso.chicory.gen")
    private String packageName;

    /**
     * Name of the bindings
     */
    @Parameter(required = true, defaultValue = "Module")
    private String name;

    /**
     * Ignore modules
     */
    @Parameter(required = true, defaultValue = "[]]")
    private List<String> ignoredModules;

    // TODO: check multiple executions on multiple files
    /**
     * Location of the source wasm file
     */
    @Parameter(required = true)
    private File source;

    /**
     * Location for the binding generated sources.
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/chciory-bindgen")
    private File sourceDestinationFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    private static String camelCase(String s) {
        var sb = new StringBuilder();
        boolean toUpper = false;
        for (int i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (c == '_') {
                toUpper = true;
            } else if (toUpper) {
                sb.append(Character.toUpperCase(c));
                toUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isSupported(ValueType vt) {
        switch (vt) {
            case I32:
            case I64:
            case F32:
            case F64:
                return true;
            default:
                return false;
        }
    }

    private String valueTypeToJava(ValueType vt) {
        switch (vt) {
            case I32:
                return "int";
            case I64:
                return "long";
            case F32:
                return "float";
            case F64:
                return "double";
            default:
                throw new IllegalArgumentException("type not supported " + vt);
        }
    }

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
                return "f32";
            case F64:
                return "f64";
            default:
                throw new IllegalArgumentException("type not supported " + vt);
        }
    }

    @SuppressWarnings("StringSplitter")
    private Path qualifiedDestinationFolder() {
        var res = sourceDestinationFolder.toPath();
        for (var p : packageName.split("\\.")) {
            res = res.resolve(p);
        }
        return res;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void execute() throws MojoExecutionException {
        // Create destination folders
        if (!sourceDestinationFolder.mkdirs()) {
            log.warn("Failed to create folder: " + sourceDestinationFolder);
        }

        // load the user wasm module
        var module = Parser.parse(source);

        // generate the bindings
        final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());

        var cu = new CompilationUnit(packageName);
        cu.setStorage(qualifiedDestinationFolder().resolve(name + ".java"));

        cu.addImport("java.util.List");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.wasm.types.ValueType");
        cu.addImport("com.dylibso.chicory.runtime.HostFunction");
        cu.addImport("com.dylibso.chicory.runtime.HostMemory");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Memory");

        var clazz = cu.addClass(name, Modifier.Keyword.ABSTRACT, Modifier.Keyword.PUBLIC);
        var instanceMethod =
                clazz.addMethod("instance", Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);
        instanceMethod.setType("Instance").removeBody();
        var instanceCall = new MethodCallExpr("instance");

        int importedFunctionCount = 0;
        boolean importedMemory = false;

        for (int importIdx = 0; importIdx < module.importSection().importCount(); importIdx++) {
            var imprt = module.importSection().getImport(importIdx);

            if (imprt.importType() == ExternalType.MEMORY) {
                var method =
                        clazz.addMethod(
                                camelCase(imprt.name()),
                                Modifier.Keyword.PUBLIC,
                                Modifier.Keyword.ABSTRACT);
                method.setType("Memory").removeBody();
                var toHostMemsMethod = clazz.addMethod("toHostMemory", Modifier.Keyword.PUBLIC);
                toHostMemsMethod.setType("HostMemory");
                var toHostMemsBody = toHostMemsMethod.createBody();
                var memMapping =
                        new ObjectCreationExpr()
                                .setType("HostMemory")
                                .addArgument(new StringLiteralExpr(imprt.moduleName()))
                                .addArgument(new StringLiteralExpr(imprt.name()))
                                .addArgument(new MethodCallExpr("memory"));
                toHostMemsBody.addStatement(new ReturnStmt(memMapping));
                importedMemory = true;
            } else if (imprt.importType() == ExternalType.FUNCTION) {
                importedFunctionCount++;
                var importName = camelCase(imprt.name());
                var method =
                        clazz.addMethod(
                                importName, Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);

                if (ignoredModules.contains(imprt.moduleName())) {
                    log.warn("Skipping generation for imported module " + imprt.moduleName());
                    continue;
                }
                var importType = module.typeSection().getType(((FunctionImport) imprt).typeIndex());
                var functionInvoc = new MethodCallExpr(importName);
                var handleBody = new BlockStmt();

                for (int paramIdx = 0; paramIdx < importType.params().size(); paramIdx++) {
                    var argName = "arg" + paramIdx;
                    functionInvoc.addArgument(
                            new MethodCallExpr(
                                    new ArrayAccessExpr(
                                            new NameExpr("args"), new IntegerLiteralExpr(paramIdx)),
                                    new SimpleName(
                                            valueTypeToConverter(
                                                    importType.params().get(paramIdx)))));
                    method.addParameter(
                            valueTypeToJava(importType.params().get(paramIdx)), argName);
                }
                method.removeBody();

                if (importType.returns().size() == 0) {
                    method.setType("void");
                    handleBody.addStatement(functionInvoc);
                    handleBody.addStatement(
                            new ReturnStmt(new ArrayCreationExpr(parseType("Value"))));
                } else if (importType.returns().size() == 1
                        && isSupported(importType.returns().get(0))) {
                    method.setType(valueTypeToJava(importType.returns().get(0)));

                    var arrayReturn = new ArrayCreationExpr(parseType("Value"));
                    var arrayReturnInit = new ArrayInitializerExpr();
                    arrayReturnInit.setValues(
                            NodeList.nodeList(
                                    new MethodCallExpr(
                                            new NameExpr("Value"),
                                            new SimpleName(
                                                    javaToValueTypeToConverter(
                                                            importType.returns().get(0))),
                                            NodeList.nodeList(functionInvoc))));

                    arrayReturn.setInitializer(arrayReturnInit);

                    handleBody.addStatement(new ReturnStmt(arrayReturn));
                } else {
                    method.setType("Value[]");
                    handleBody.addStatement(new ReturnStmt(functionInvoc));
                }
            } else {
                // TODO: explicitly export also the other
                log.warn(
                        "[chicory-bindgen] no generated code for import type: "
                                + imprt.moduleName()
                                + "."
                                + imprt.name()
                                + " of type "
                                + imprt.importType());
            }
        }

        // now the exports
        for (int exportIdx = 0; exportIdx < module.exportSection().exportCount(); exportIdx++) {
            var export = module.exportSection().getExport(exportIdx);

            if (export.exportType() == ExternalType.MEMORY && !importedMemory) {
                var method = clazz.addMethod(camelCase(export.name()), Modifier.Keyword.PUBLIC);
                method.setType("Memory");
                var body = method.createBody();
                body.addStatement(
                        new ReturnStmt(new MethodCallExpr(instanceCall, new SimpleName("memory"))));
            } else if (export.exportType() == ExternalType.FUNCTION) {
                var method = clazz.addMethod(camelCase(export.name()), Modifier.Keyword.PUBLIC);

                var funcTypeIdx =
                        module.functionSection()
                                .getFunctionType(export.index() - importedFunctionCount);
                var exportType = module.typeSection().getType(funcTypeIdx);

                var returnType = "Value[]";
                var hasReturns = false;
                var hasJavaReturn = false;
                String javaReturnConverter = null;
                var returns = exportType.returns();
                switch (returns.size()) {
                    case 0:
                        returnType = "void";
                        hasReturns = false;
                        break;
                    case 1:
                        if (isSupported(returns.get(0))) {
                            returnType = valueTypeToJava(returns.get(0));
                            hasJavaReturn = true;
                            javaReturnConverter = valueTypeToConverter(returns.get(0));
                        }
                        // fallthrough
                    default:
                        hasReturns = true;
                        break;
                }
                method.setType(returnType);

                var params = exportType.params();
                Expression[] args = new Expression[params.size()];
                for (int paramIdx = 0; paramIdx < params.size(); paramIdx++) {
                    var argName = "arg" + paramIdx;
                    method.addParameter(valueTypeToJava(params.get(paramIdx)), argName);
                    args[paramIdx] =
                            new MethodCallExpr(
                                    new NameExpr("Value"),
                                    new SimpleName(
                                            javaToValueTypeToConverter(params.get(paramIdx))),
                                    NodeList.nodeList(new NameExpr(argName)));
                }

                var methodBody = method.createBody();
                Expression methodCall =
                        new MethodCallExpr(
                                new MethodCallExpr(
                                        instanceCall,
                                        new SimpleName("export"),
                                        NodeList.nodeList(
                                                new StringLiteralExpr(escapeJava(export.name())))),
                                new SimpleName("apply"),
                                NodeList.nodeList(args));
                if (!hasReturns) {
                    methodBody.addStatement(methodCall);
                } else {
                    if (hasJavaReturn) {
                        methodCall =
                                new MethodCallExpr(
                                        new ArrayAccessExpr(methodCall, new IntegerLiteralExpr()),
                                        new SimpleName(javaReturnConverter));
                    }
                    methodBody.addStatement(new ReturnStmt(methodCall));
                }
            } else {
                // TODO: explicitly export also the other types
                log.warn(
                        "[chicory-bindgen] no generated code for export type: "
                                + export.exportType());
            }
        }

        dest.add(cu);
        dest.saveAll();

        // Add the generated tests to the source root
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }
}
