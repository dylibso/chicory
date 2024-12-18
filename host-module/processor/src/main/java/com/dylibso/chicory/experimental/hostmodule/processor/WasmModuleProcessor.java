package com.dylibso.chicory.experimental.hostmodule.processor;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
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
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
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
import java.util.stream.Collectors;
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

    private Expression toLong(
            ValueType type, Expression nameExpr, Runnable error, CompilationUnit cu) {
        switch (type) {
            case I32:
                return new CastExpr(parseType("long"), nameExpr);
            case I64:
                return nameExpr;
            case F32:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "floatToLong", new NodeList<>(nameExpr));
            case F64:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "doubleToLong", new NodeList<>(nameExpr));
            default:
                error.run();
                throw new AbortProcessingException();
        }
    }

    private Expression fromLong(
            ValueType type, Expression nameExpr, Runnable error, CompilationUnit cu) {
        switch (type) {
            case I32:
                return new CastExpr(parseType("int"), nameExpr);
            case I64:
                return nameExpr;
            case F32:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "longToFloat", new NodeList<>(nameExpr));
            case F64:
                cu.addImport(Value.class);
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

    private Expression listOfValueTypes(List<ValueType> valueTypes) {
        List<Expression> values =
                valueTypes.stream()
                        .map(
                                vt -> {
                                    switch (vt) {
                                        case I32:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValueType"), "I32");
                                        case I64:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValueType"), "I64");
                                        case F32:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValueType"), "F32");
                                        case F64:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValueType"), "F64");
                                        default:
                                            // TODO: use the logger
                                            System.err.println("Unsupported WASM type: " + vt);
                                            throw new AbortProcessingException();
                                    }
                                })
                        .collect(Collectors.toList());
        return new MethodCallExpr(new NameExpr("List"), "of", NodeList.nodeList(values));
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
        var exportCallHandle =
                new MethodCallExpr(new MethodCallExpr("instance"), "exports", NodeList.nodeList());
        for (int i = 0; i < module.exportSection().exportCount(); i++) {
            var export = module.exportSection().getExport(i);

            var exportMethod =
                    exportsInterface.addMethod(
                            snakeCaseToCamelCase(export.name(), false), Modifier.Keyword.DEFAULT);
            if (export.exportType() == ExternalType.MEMORY) {
                exportsCu.addImport("com.dylibso.chicory.runtime.Memory");
                exportMethod.setType("Memory");
                exportMethod
                        .createBody()
                        .addStatement(
                                new ReturnStmt(
                                        new MethodCallExpr(
                                                exportCallHandle,
                                                "memory",
                                                NodeList.nodeList(
                                                        new StringLiteralExpr(export.name())))));
                continue;
            } else if (export.exportType() == ExternalType.GLOBAL) {
                exportsCu.addImport("com.dylibso.chicory.runtime.GlobalInstance");
                exportMethod.setType("GlobalInstance");
                exportMethod
                        .createBody()
                        .addStatement(
                                new ReturnStmt(
                                        new MethodCallExpr(
                                                exportCallHandle,
                                                "global",
                                                NodeList.nodeList(
                                                        new StringLiteralExpr(export.name())))));
                continue;
            } else if (export.exportType() == ExternalType.TABLE) {
                exportsCu.addImport("com.dylibso.chicory.runtime.TableInstance");
                exportMethod.setType("TableInstance");
                exportMethod
                        .createBody()
                        .addStatement(
                                new ReturnStmt(
                                        new MethodCallExpr(
                                                exportCallHandle,
                                                "table",
                                                NodeList.nodeList(
                                                        new StringLiteralExpr(export.name())))));
                continue;
            }
            // it should be a function here
            assert (export.exportType() == ExternalType.FUNCTION);

            var funcType =
                    module.functionSection()
                            .getFunctionType(export.index() - importedFunctionCount);
            var exportType = module.typeSection().getType(funcType);

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
                handleCallArguments.add(toLong(param, argExpr, error, exportsCu));
            }

            var methodBody = exportMethod.createBody();
            var exportCall =
                    new MethodCallExpr(
                            exportCallHandle,
                            "function",
                            NodeList.nodeList(new StringLiteralExpr(export.name())));
            var exportApplyHandle =
                    new MethodCallExpr(exportCall, "apply", NodeList.nodeList(handleCallArguments));

            if (exportType.returns().size() == 0) {
                methodBody.addStatement(exportApplyHandle).addStatement(new ReturnStmt());
            } else if (exportType.returns().size() > 1) {
                // TODO: not tested now
                methodBody.addStatement(new ReturnStmt(exportApplyHandle));
            } else {
                methodBody.addStatement(
                        new AssignExpr(
                                new VariableDeclarationExpr(parseType("long"), "result"),
                                new ArrayAccessExpr(exportApplyHandle, new IntegerLiteralExpr("0")),
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
                                        },
                                        exportsCu)));
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

                importedModules.computeIfAbsent(imprt.module(), ignored -> new ArrayList<>());
                importedModules.computeIfPresent(
                        imprt.module(),
                        (k, v) -> {
                            v.add(imprt);
                            return v;
                        });
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

                var toImportValuesMethod =
                        importsInterface.addMethod("toImportValues", Modifier.Keyword.DEFAULT);
                importsCu.addImport("com.dylibso.chicory.runtime.ImportValues");
                toImportValuesMethod.setType("ImportValues");
                var toImportValuesBody = toImportValuesMethod.createBody();
                ArrayList<Expression> importedMemories = new ArrayList<>();
                ArrayList<Expression> importedGlobals = new ArrayList<>();
                ArrayList<Expression> importedTables = new ArrayList<>();
                ArrayList<Expression> importedFunctions = new ArrayList<>();

                for (var importedFun : imprt.getValue()) {
                    var importMethod =
                            importInterface.addMethod(
                                    snakeCaseToCamelCase(importedFun.name(), false));

                    var importFunctionCall =
                            new MethodCallExpr(
                                    new MethodCallExpr(imprt.getKey()), importedFun.name());

                    if (importedFun.importType() == ExternalType.MEMORY) {
                        cu.addImport("com.dylibso.chicory.runtime.Memory");
                        importMethod.setType("Memory");
                        importMethod.removeBody();

                        importsCu.addImport("com.dylibso.chicory.runtime.ImportMemory");
                        importedMemories.add(
                                new ObjectCreationExpr(
                                        null,
                                        parseClassOrInterfaceType("ImportMemory"),
                                        NodeList.nodeList(
                                                new StringLiteralExpr(imprt.getKey()),
                                                new StringLiteralExpr(importedFun.name()),
                                                importFunctionCall)));
                        continue;
                    } else if (importedFun.importType() == ExternalType.GLOBAL) {
                        cu.addImport("com.dylibso.chicory.runtime.GlobalInstance");
                        importMethod.setType("GlobalInstance");
                        importMethod.removeBody();

                        // TODO: should be similar to Memory
                        // importedGlobals.add(
                        //        accessExportedEntity(imprt.getKey(), "global",
                        // importedFun.name()));
                        continue;
                    } else if (importedFun.importType() == ExternalType.TABLE) {
                        cu.addImport("com.dylibso.chicory.runtime.TableInstance");
                        importMethod.setType("TableInstance");
                        importMethod.removeBody();

                        // TODO: should be similar to Memory
                        // importedTables.add(
                        //        accessExportedEntity(imprt.getKey(), "table",
                        // importedFun.name()));
                        continue;
                    }
                    // we now know it's a function
                    assert (importedFun.importType() == ExternalType.FUNCTION);
                    // needed to generate the functions signatures
                    importsCu.addImport(List.class);
                    importsCu.addImport(ValueType.class);
                    importsCu.addImport("com.dylibso.chicory.runtime.Instance");
                    importsCu.addImport("com.dylibso.chicory.runtime.HostFunction");

                    var importType =
                            module.typeSection()
                                    .getType((((FunctionImport) importedFun).typeIndex()));
                    importMethod.removeBody();

                    //                    new HostFunction(
                    //                            "spectest",
                    //                            "print_f64_f64",
                    //                            List.of(ValueType.F64, ValueType.F64),
                    //                            List.of(),
                    //                            noop)

                    // build lambda return
                    var functionBodyStatement = new BlockStmt();

                    List<Expression> parameters = new ArrayList<>();
                    for (int i = 0; i < importType.params().size(); i++) {
                        var p = importType.params().get(i);
                        Runnable error =
                                () ->
                                        log(
                                                ERROR,
                                                "Unsupported WASM type: "
                                                        + p
                                                        + " in import: "
                                                        + importedFun.name(),
                                                type);

                        parameters.add(
                                fromLong(
                                        p,
                                        new ArrayAccessExpr(
                                                new NameExpr("args"),
                                                new IntegerLiteralExpr(Integer.toString(i))),
                                        error,
                                        importsCu));
                    }

                    var importApplyHandle =
                            new MethodCallExpr(
                                    new MethodCallExpr(imprt.getKey()),
                                    importedFun.name(),
                                    NodeList.nodeList(parameters));

                    if (importType.returns().size() == 0) {
                        functionBodyStatement.addStatement(importApplyHandle);
                        functionBodyStatement.addStatement(new ReturnStmt(new NullLiteralExpr()));
                    } else if (importType.returns().size() == 1) {
                        Runnable error =
                                () ->
                                        log(
                                                ERROR,
                                                "Unsupported WASM type: "
                                                        + importType.returns().get(0)
                                                        + " in import: "
                                                        + importedFun.name(),
                                                type);

                        functionBodyStatement.addStatement(
                                new ReturnStmt(
                                        new ArrayCreationExpr(
                                                parseType("long"),
                                                NodeList.nodeList(new ArrayCreationLevel()),
                                                new ArrayInitializerExpr(
                                                        NodeList.nodeList(
                                                                toLong(
                                                                        importType.returns().get(0),
                                                                        importApplyHandle,
                                                                        error,
                                                                        importsCu))))));
                    } else {
                        functionBodyStatement.addStatement(new ReturnStmt(importApplyHandle));
                    }

                    var importedHostFunctionBinding =
                            new ObjectCreationExpr(
                                    null,
                                    parseClassOrInterfaceType("HostFunction"),
                                    NodeList.nodeList(
                                            new StringLiteralExpr(imprt.getKey()),
                                            new StringLiteralExpr(importedFun.name()),
                                            listOfValueTypes(importType.params()),
                                            listOfValueTypes(importType.returns()),
                                            // (Instance instance, long... args) -> null;
                                            new LambdaExpr(
                                                    NodeList.nodeList(
                                                            new Parameter(
                                                                    parseType("Instance"),
                                                                    new SimpleName("instance")),
                                                            new Parameter(parseType("long"), "args")
                                                                    .setVarArgs(true)),
                                                    functionBodyStatement)));

                    importedFunctions.add(importedHostFunctionBinding);

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

                // now let's write the import values body
                toImportValuesBody.addStatement(
                        new AssignExpr(
                                new VariableDeclarationExpr(
                                        parseType("ImportValues.Builder"), "imports"),
                                new MethodCallExpr(new NameExpr("ImportValues"), "builder"),
                                AssignExpr.Operator.ASSIGN));
                // TODO: the import is not enough, I should complete it with the module name etc.
                // etc.
                for (var importTable : importedTables) {
                    toImportValuesBody.addStatement(
                            new MethodCallExpr(
                                    new NameExpr("imports"),
                                    "addTable",
                                    NodeList.nodeList(importTable)));
                }
                for (var importGlobal : importedGlobals) {
                    toImportValuesBody.addStatement(
                            new MethodCallExpr(
                                    new NameExpr("imports"),
                                    "addGlobal",
                                    NodeList.nodeList(importGlobal)));
                }
                for (var importMemory : importedMemories) {
                    toImportValuesBody.addStatement(
                            new MethodCallExpr(
                                    new NameExpr("imports"),
                                    "addMemory",
                                    NodeList.nodeList(importMemory)));
                }
                for (var importFunction : importedFunctions) {
                    toImportValuesBody.addStatement(
                            new MethodCallExpr(
                                    new NameExpr("imports"),
                                    "addFunction",
                                    NodeList.nodeList(importFunction)));
                }
                var toImportsBuilder = new MethodCallExpr(new NameExpr("imports"), "build");
                toImportValuesBody.addStatement(new ReturnStmt(toImportsBuilder));
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
