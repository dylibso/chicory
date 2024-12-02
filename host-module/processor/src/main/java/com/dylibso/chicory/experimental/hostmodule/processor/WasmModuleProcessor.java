package com.dylibso.chicory.experimental.hostmodule.processor;

import static com.github.javaparser.StaticJavaParser.parseType;
import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.dylibso.chicory.experimental.hostmodule.annotations.WasmModuleInterface;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.ValueType;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public final class WasmModuleProcessor extends AbstractModuleProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(WasmModuleInterface.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(WasmModuleInterface.class)) {
            log(NOTE, "Generating wasm module helpers for " + element, null);
            try {
                processModule((TypeElement) element);
            } catch (URISyntaxException ex) {
                log(ERROR, "Failed to parse URI from wasmFile", element);
            } catch (IOException ex) {
                log(ERROR, "Failed to load wasmFile", element);
            } catch (AbortProcessingException e) {
                // skip type
            }
        }

        return false;
    }

    private Class javaClassFromValueType(ValueType type, Runnable error) {
        switch (type) {
            case I32:
                return int.class;
            case I64:
                return long.class;
            case F32:
                return float.class;
            case F64:
                return double.class;
            default:
                error.run();
                throw new AbortProcessingException();
        }
    }

    private Expression toLong(ValueType type, NameExpr nameExpr, Runnable error) {
        switch (type) {
            case I32:
                return new CastExpr(parseType("long"), nameExpr);
            case I64:
                return nameExpr;
            case F32:
                return new MethodCallExpr(
                        new NameExpr("Value"), "floatToLong", new NodeList<>(nameExpr));
            case F64:
                return new MethodCallExpr(
                        new NameExpr("Value"), "doubleToLong", new NodeList<>(nameExpr));
            default:
                error.run();
                throw new AbortProcessingException();
        }
    }

    private Expression fromLong(ValueType type, NameExpr nameExpr, Runnable error) {
        switch (type) {
            case I32:
                return new CastExpr(parseType("int"), nameExpr);
            case I64:
                return nameExpr;
            case F32:
                return new MethodCallExpr(
                        new NameExpr("Value"), "longToFloat", new NodeList<>(nameExpr));
            case F64:
                return new MethodCallExpr(
                        new NameExpr("Value"), "longToDouble", new NodeList<>(nameExpr));
            default:
                error.run();
                throw new AbortProcessingException();
        }
    }

    private CompilationUnit newCu(PackageElement pkg) {
        var packageName = pkg.getQualifiedName().toString();
        var cu = (pkg.isUnnamed()) ? new CompilationUnit() : new CompilationUnit(packageName);
        if (!pkg.isUnnamed()) {
            cu.setPackageDeclaration(packageName);
        }
        return cu;
    }

    private void processModule(TypeElement type) throws URISyntaxException, IOException {
        var wasmFile = type.getAnnotation(WasmModuleInterface.class).value();

        WasmModule module;
        if (wasmFile.startsWith("file:")) {
            module = Parser.parse(Path.of(new URI(wasmFile)));
        } else {
            FileObject fileObject =
                    processingEnv
                            .getFiler()
                            .getResource(StandardLocation.CLASS_OUTPUT, "", wasmFile);
            module = Parser.parse(fileObject.openInputStream());
        }

        Map<String, CompilationUnit> writableClasses = new HashMap<>();

        var pkg = getPackageName(type);
        var exportsCu = newCu(pkg);
        String prefix = (pkg.isUnnamed()) ? "" : pkg.getQualifiedName().toString() + ".";

        exportsCu.addImport("com.dylibso.chicory.runtime.Instance");

        var typeName = type.getSimpleName().toString();
        var processorName = new StringLiteralExpr(getClass().getName());
        var exportsInterface =
                exportsCu
                        .addInterface(typeName + "_ModuleExports")
                        .setPublic(true)
                        .addSingleMemberAnnotation(Generated.class, processorName);
        writableClasses.put(prefix + type.getSimpleName() + "_ModuleExports", exportsCu);

        var instanceMethod = exportsInterface.addMethod("instance");
        instanceMethod.setType("Instance");
        instanceMethod.removeBody();

        int importedFunctionCount =
                (int)
                        module.importSection().stream()
                                .filter(i -> i.importType() == ExternalType.FUNCTION)
                                .count();

        // generate module exports
        for (int i = 0; i < module.exportSection().exportCount(); i++) {
            var export = module.exportSection().getExport(i);
            if (export.exportType() != ExternalType.FUNCTION) {
                // TODO: implement support for Memories, Tables, Globals!
                continue;
            }
            var funcType =
                    module.functionSection()
                            .getFunctionType(export.index() - importedFunctionCount);
            var exportType = module.typeSection().getType(funcType);

            var exportMethod =
                    exportsInterface.addMethod(
                            snakeCaseToCamelCase(export.name(), false), Modifier.Keyword.DEFAULT);

            if (exportType.returns().size() == 0) {
                exportMethod.setType(void.class);
            } else if (exportType.returns().size() > 1) {
                exportMethod.setType(long[].class);
            } else {
                var javaType =
                        javaClassFromValueType(
                                exportType.returns().get(0),
                                () -> {
                                    log(
                                            ERROR,
                                            "Unsupported WASM type: "
                                                    + exportType.returns().get(0)
                                                    + " in export: "
                                                    + export.name(),
                                            type);
                                });
                exportMethod.setType(javaType);
            }

            var argPrefix = "arg";
            var handleCallArguments = new ArrayList<Expression>();
            for (var pIdx = 0; pIdx < exportType.params().size(); pIdx++) {
                var param = exportType.params().get(pIdx);
                var argName = argPrefix + pIdx;
                Runnable error =
                        () ->
                                log(
                                        ERROR,
                                        "Unsupported WASM type: "
                                                + param
                                                + " in export: "
                                                + export.name(),
                                        type);
                var javaType = javaClassFromValueType(param, error);
                // signature
                exportMethod.addParameter(javaType, argName);
                // body invocation call arguments
                var argExpr = new NameExpr(argName);
                handleCallArguments.add(toLong(param, argExpr, error));
            }

            var methodBody = exportMethod.createBody();
            var exportCall =
                    new MethodCallExpr(
                            new MethodCallExpr("instance"),
                            "export",
                            NodeList.nodeList(new StringLiteralExpr(export.name())));
            var exportCallHandle =
                    new MethodCallExpr(exportCall, "apply", NodeList.nodeList(handleCallArguments));

            if (exportType.returns().size() == 0) {
                methodBody.addStatement(exportCallHandle).addStatement(new ReturnStmt());
            } else if (exportType.returns().size() > 1) {
                // TODO: not tested now
                methodBody.addStatement(new ReturnStmt(exportCallHandle));
            } else {
                methodBody.addStatement(
                        new AssignExpr(
                                new VariableDeclarationExpr(parseType("long"), "result"),
                                new ArrayAccessExpr(exportCallHandle, new IntegerLiteralExpr(0)),
                                AssignExpr.Operator.ASSIGN));
                methodBody.addStatement(
                        new ReturnStmt(
                                fromLong(
                                        exportType.returns().get(0),
                                        new NameExpr("result"),
                                        () -> {
                                            log(
                                                    ERROR,
                                                    "Unsupported WASM type: "
                                                            + exportType.returns().get(0)
                                                            + " in export: "
                                                            + export.name(),
                                                    type);
                                        })));
            }
        }

        // generate module imports
        if (module.importSection() != null && module.importSection().importCount() > 0) {
            var importsCu = newCu(pkg);
            var importsInterface =
                    importsCu
                            .addInterface(typeName + "_ModuleImports")
                            .setPublic(true)
                            .addSingleMemberAnnotation(Generated.class, processorName);
            writableClasses.put(prefix + type.getSimpleName() + "_ModuleImports", importsCu);

            // group-by module name
            Map<String, List<Import>> importedModules = new HashMap<>();
            for (int i = 0; i < module.importSection().importCount(); i++) {
                var imprt = module.importSection().getImport(i);
                if (imprt.importType() == ExternalType.FUNCTION) {
                    importedModules.computeIfAbsent(imprt.module(), ignored -> new ArrayList<>());
                    importedModules.computeIfPresent(
                            imprt.module(),
                            (k, v) -> {
                                v.add(imprt);
                                return v;
                            });
                }
                // TODO: memory/global/table handling
            }

            for (var imprt : importedModules.entrySet()) {
                var importClassName =
                        type.getSimpleName() + "_" + snakeCaseToCamelCase(imprt.getKey(), true);
                var importClassMethod =
                        importsInterface.addMethod(snakeCaseToCamelCase(imprt.getKey(), false));
                importClassMethod.removeBody();
                importClassMethod.setType(importClassName);

                var cu = newCu(pkg);
                var importInterface =
                        cu.addInterface(importClassName)
                                .setPublic(true)
                                .addSingleMemberAnnotation(Generated.class, processorName);
                writableClasses.put(prefix + importClassName, cu);

                for (var importedFun : imprt.getValue()) {
                    var importType =
                            module.typeSection()
                                    .getType((((FunctionImport) importedFun).typeIndex()));

                    var importMethod =
                            importInterface.addMethod(
                                    snakeCaseToCamelCase(importedFun.name(), false));
                    importMethod.removeBody();

                    if (importType.returns().size() == 0) {
                        importMethod.setType(void.class);
                    } else if (importType.returns().size() > 1) {
                        importMethod.setType(long[].class);
                    } else {
                        var javaType =
                                javaClassFromValueType(
                                        importType.returns().get(0),
                                        () -> {
                                            log(
                                                    ERROR,
                                                    "Unsupported WASM type: "
                                                            + importType.returns().get(0)
                                                            + " in export: "
                                                            + importedFun.name(),
                                                    type);
                                        });
                        importMethod.setType(javaType);
                    }

                    var argPrefix = "arg";
                    for (var pIdx = 0; pIdx < importType.params().size(); pIdx++) {
                        var param = importType.params().get(pIdx);
                        var argName = argPrefix + pIdx;
                        Runnable error =
                                () ->
                                        log(
                                                ERROR,
                                                "Unsupported WASM type: "
                                                        + param
                                                        + " in export: "
                                                        + importedFun.name(),
                                                type);
                        // signature
                        importMethod.addParameter(javaClassFromValueType(param, error), argName);
                    }
                }
            }
        }

        writableClasses.forEach((k, v) -> writeCu(k, v, type));
    }

    private void writeCu(String qualifiedName, CompilationUnit cu, TypeElement type) {
        try (Writer writer = filer().createSourceFile(qualifiedName, type).openWriter()) {
            writer.write(cu.printer(printer()).toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", qualifiedName, e), null);
        }
    }
}
