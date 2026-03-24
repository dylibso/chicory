package com.dylibso.chicory.codegen;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
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
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;

public final class ModuleInterfaceCodegen {

    private final WasmModule module;
    private final String packageName;
    private final String typeName;
    private final String generatorName;

    private ModuleInterfaceCodegen(Builder builder) {
        if (builder.module == null) {
            throw new IllegalArgumentException("module is required");
        }
        if (builder.typeName == null) {
            throw new IllegalArgumentException("typeName is required");
        }
        this.module = builder.module;
        this.packageName = builder.packageName == null ? "" : builder.packageName;
        this.typeName = builder.typeName;
        this.generatorName =
                builder.generatorName == null
                        ? ModuleInterfaceCodegen.class.getName()
                        : builder.generatorName;
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;
        private String packageName;
        private String typeName;
        private String generatorName;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder withTypeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder withGeneratorName(String generatorName) {
            this.generatorName = generatorName;
            return this;
        }

        public ModuleInterfaceCodegen build() {
            return new ModuleInterfaceCodegen(this);
        }
    }

    public Map<String, CompilationUnit> generate() {
        Map<String, CompilationUnit> result = new LinkedHashMap<>();
        String prefix = packageName.isEmpty() ? "" : packageName + ".";

        var exportsCu = newCu();
        exportsCu.addImport("com.dylibso.chicory.runtime.Instance");

        var processorName = new StringLiteralExpr(generatorName);
        var exportsClass =
                exportsCu
                        .addClass(typeName + "_ModuleExports", Modifier.Keyword.PUBLIC)
                        .setPublic(true)
                        .addSingleMemberAnnotation(Generated.class, processorName);
        result.put(prefix + typeName + "_ModuleExports", exportsCu);

        var functionImports =
                module.importSection().stream()
                        .filter(i -> i.importType() == ExternalType.FUNCTION)
                        .map(i -> (FunctionImport) i)
                        .toArray(FunctionImport[]::new);

        var exportsConstructor = exportsClass.addConstructor(Modifier.Keyword.PUBLIC);
        exportsConstructor.addParameter(parseType("Instance"), "instance");

        // generate module exports
        var exportCallHandle =
                new MethodCallExpr(new NameExpr("instance"), "exports", NodeList.nodeList());
        var exportNames = new ArrayList<String>();

        for (int i = 0; i < module.exportSection().exportCount(); i++) {
            var export = module.exportSection().getExport(i);

            var name =
                    deduplicatedMethodName(
                            CodegenUtils.snakeCaseToCamelCase(export.name(), false), exportNames);
            exportNames.add(name);

            var exportFieldName = new NameExpr("field_" + name);
            Type exportFieldType = null;
            switch (export.exportType()) {
                case MEMORY:
                    exportFieldType = parseType("Memory");
                    break;
                case GLOBAL:
                    exportFieldType = parseType("GlobalInstance");
                    break;
                case TABLE:
                    exportFieldType = parseType("TableInstance");
                    break;
                case FUNCTION:
                    exportFieldType = parseType("ExportFunction");
                    break;
            }
            exportsClass.addField(
                    exportFieldType,
                    exportFieldName.getName().asString(),
                    Modifier.Keyword.PRIVATE,
                    Modifier.Keyword.FINAL);

            var exportMethod = exportsClass.addMethod(name, Modifier.Keyword.PUBLIC);

            var exportMethodBodyGen = exportMethodBodyGen(exportCallHandle, export.name());

            if (export.exportType() == ExternalType.MEMORY) {
                exportsCu.addImport("com.dylibso.chicory.runtime.Memory");

                exportsConstructor
                        .getBody()
                        .addStatement(
                                new AssignExpr(
                                        exportFieldName,
                                        exportMethodBodyGen.apply("memory"),
                                        AssignExpr.Operator.ASSIGN));

                exportMethod.setType(exportFieldType);
                exportMethod.createBody().addStatement(new ReturnStmt(exportFieldName));
                continue;
            } else if (export.exportType() == ExternalType.GLOBAL) {
                exportsCu.addImport("com.dylibso.chicory.runtime.GlobalInstance");

                exportsConstructor
                        .getBody()
                        .addStatement(
                                new AssignExpr(
                                        exportFieldName,
                                        exportMethodBodyGen.apply("global"),
                                        AssignExpr.Operator.ASSIGN));

                exportMethod.setType(exportFieldType);
                exportMethod.createBody().addStatement(new ReturnStmt(exportFieldName));
                continue;
            } else if (export.exportType() == ExternalType.TABLE) {
                exportsCu.addImport("com.dylibso.chicory.runtime.TableInstance");

                exportsConstructor
                        .getBody()
                        .addStatement(
                                new AssignExpr(
                                        exportFieldName,
                                        exportMethodBodyGen.apply("table"),
                                        AssignExpr.Operator.ASSIGN));

                exportMethod.setType(exportFieldType);
                exportMethod.createBody().addStatement(new ReturnStmt(exportFieldName));
                continue;
            }
            // it should be a function here
            assert (export.exportType() == ExternalType.FUNCTION);
            exportsCu.addImport("com.dylibso.chicory.runtime.ExportFunction");

            var funcType =
                    (export.index() >= functionImports.length)
                            ? module.functionSection()
                                    .getFunctionType(export.index() - functionImports.length)
                            : functionImports[export.index()].typeIndex();
            var exportType = module.typeSection().getType(funcType);

            var argPrefix = "arg";
            var handleCallArguments = new ArrayList<Expression>();
            for (var pIdx = 0; pIdx < exportType.params().size(); pIdx++) {
                var param = exportType.params().get(pIdx);
                var argName = argPrefix + pIdx;
                var javaType = javaClassFromValueType(param);
                // signature
                exportMethod.addParameter(javaType, argName);
                // body invocation call arguments
                var argExpr = new NameExpr(argName);
                handleCallArguments.add(toLong(param, argExpr, exportsCu));
            }

            var methodBody = exportMethod.createBody();
            var exportCall =
                    new MethodCallExpr(
                            exportCallHandle,
                            "function",
                            NodeList.nodeList(new StringLiteralExpr(export.name())));
            exportsConstructor
                    .getBody()
                    .addStatement(
                            new AssignExpr(
                                    exportFieldName, exportCall, AssignExpr.Operator.ASSIGN));

            var exportApplyHandle =
                    new MethodCallExpr(
                            exportFieldName, "apply", NodeList.nodeList(handleCallArguments));

            if (exportType.returns().size() == 0) {
                exportMethod.setType(void.class);
                methodBody.addStatement(exportApplyHandle).addStatement(new ReturnStmt());
            } else if (exportType.returns().size() > 1) {
                exportMethod.setType(long[].class);
                methodBody.addStatement(new ReturnStmt(exportApplyHandle));
            } else {
                exportMethod.setType(javaClassFromValueType(exportType.returns().get(0)));
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
                                        exportsCu)));
            }
        }

        // generate module imports
        if (module.importSection() != null && module.importSection().importCount() > 0) {
            var importsCu = newCu();
            var importsInterface =
                    importsCu
                            .addInterface(typeName + "_ModuleImports")
                            .setPublic(true)
                            .addSingleMemberAnnotation(Generated.class, processorName);
            result.put(prefix + typeName + "_ModuleImports", importsCu);

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

            if (importedModules.size() > 0) {
                var toImportValuesBody = new BlockStmt();
                toImportValuesBody.addStatement(
                        new AssignExpr(
                                new VariableDeclarationExpr(
                                        parseType("ImportValues.Builder"), "imports"),
                                new MethodCallExpr(new NameExpr("ImportValues"), "builder"),
                                AssignExpr.Operator.ASSIGN));

                for (var imprt : importedModules.entrySet()) {
                    var importClassName =
                            typeName
                                    + "_"
                                    + CodegenUtils.snakeCaseToCamelCase(imprt.getKey(), true);
                    var importClassMethod =
                            importsInterface.addMethod(
                                    CodegenUtils.snakeCaseToCamelCase(imprt.getKey(), false));
                    importClassMethod.removeBody();
                    importClassMethod.setType(importClassName);

                    var cu = newCu();
                    var importInterface =
                            cu.addInterface(importClassName)
                                    .setPublic(true)
                                    .addSingleMemberAnnotation(Generated.class, processorName);
                    result.put(prefix + importClassName, cu);

                    for (var importedFun : imprt.getValue()) {
                        var importMethod =
                                importInterface.addMethod(
                                        CodegenUtils.snakeCaseToCamelCase(
                                                importedFun.name(), false));

                        var importFunctionCall =
                                new MethodCallExpr(
                                        new MethodCallExpr(imprt.getKey()), importedFun.name());
                        java.util.function.Function<String, Expression> importObj =
                                importObjType ->
                                        new ObjectCreationExpr(
                                                null,
                                                parseClassOrInterfaceType(importObjType),
                                                NodeList.nodeList(
                                                        new StringLiteralExpr(imprt.getKey()),
                                                        new StringLiteralExpr(importedFun.name()),
                                                        importFunctionCall));

                        if (importedFun.importType() == ExternalType.MEMORY) {
                            cu.addImport("com.dylibso.chicory.runtime.Memory");
                            importMethod.setType("Memory");
                            importMethod.removeBody();

                            importsCu.addImport("com.dylibso.chicory.runtime.ImportMemory");
                            toImportValuesBody.addStatement(
                                    new MethodCallExpr(
                                            new NameExpr("imports"),
                                            "addMemory",
                                            NodeList.nodeList(importObj.apply("ImportMemory"))));
                            continue;
                        } else if (importedFun.importType() == ExternalType.GLOBAL) {
                            cu.addImport("com.dylibso.chicory.runtime.GlobalInstance");
                            importMethod.setType("GlobalInstance");
                            importMethod.removeBody();

                            importsCu.addImport("com.dylibso.chicory.runtime.ImportGlobal");
                            toImportValuesBody.addStatement(
                                    new MethodCallExpr(
                                            new NameExpr("imports"),
                                            "addGlobal",
                                            NodeList.nodeList(importObj.apply("ImportGlobal"))));
                            continue;
                        } else if (importedFun.importType() == ExternalType.TABLE) {
                            cu.addImport("com.dylibso.chicory.runtime.TableInstance");
                            importMethod.setType("TableInstance");
                            importMethod.removeBody();

                            importsCu.addImport("com.dylibso.chicory.runtime.ImportTable");
                            toImportValuesBody.addStatement(
                                    new MethodCallExpr(
                                            new NameExpr("imports"),
                                            "addTable",
                                            NodeList.nodeList(importObj.apply("ImportTable"))));
                            continue;
                        }
                        // we now know it's a function
                        assert (importedFun.importType() == ExternalType.FUNCTION);
                        // needed to generate the functions signatures
                        importsCu.addImport(List.class);
                        importsCu.addImport(ValType.class);
                        importsCu.addImport("com.dylibso.chicory.runtime.Instance");
                        importsCu.addImport("com.dylibso.chicory.runtime.HostFunction");

                        var importType =
                                module.typeSection()
                                        .getType(((FunctionImport) importedFun).typeIndex());
                        importMethod.removeBody();

                        // build lambda return
                        var functionBodyStatement = new BlockStmt();

                        List<Expression> parameters = new ArrayList<>();
                        for (int i = 0; i < importType.params().size(); i++) {
                            var p = importType.params().get(i);

                            parameters.add(
                                    fromLong(
                                            p,
                                            new ArrayAccessExpr(
                                                    new NameExpr("args"),
                                                    new IntegerLiteralExpr(Integer.toString(i))),
                                            importsCu));
                        }

                        var importApplyHandle =
                                new MethodCallExpr(
                                        new MethodCallExpr(
                                                CodegenUtils.snakeCaseToCamelCase(
                                                        imprt.getKey(), false)),
                                        CodegenUtils.snakeCaseToCamelCase(
                                                importedFun.name(), false),
                                        NodeList.nodeList(parameters));

                        if (importType.returns().size() == 0) {
                            importMethod.setType(void.class);
                            functionBodyStatement.addStatement(importApplyHandle);
                            functionBodyStatement.addStatement(
                                    new ReturnStmt(new NullLiteralExpr()));
                        } else if (importType.returns().size() == 1) {
                            importMethod.setType(
                                    javaClassFromValueType(importType.returns().get(0)));
                            functionBodyStatement.addStatement(
                                    new ReturnStmt(
                                            new ArrayCreationExpr(
                                                    parseType("long"),
                                                    NodeList.nodeList(new ArrayCreationLevel()),
                                                    new ArrayInitializerExpr(
                                                            NodeList.nodeList(
                                                                    toLong(
                                                                            importType
                                                                                    .returns()
                                                                                    .get(0),
                                                                            importApplyHandle,
                                                                            importsCu))))));
                        } else {
                            importMethod.setType(long[].class);
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
                                                                new Parameter(
                                                                                parseType("long"),
                                                                                "args")
                                                                        .setVarArgs(true)),
                                                        functionBodyStatement)));

                        toImportValuesBody.addStatement(
                                new MethodCallExpr(
                                        new NameExpr("imports"),
                                        "addFunction",
                                        NodeList.nodeList(importedHostFunctionBinding)));

                        var argPrefix = "arg";
                        for (var pIdx = 0; pIdx < importType.params().size(); pIdx++) {
                            var param = importType.params().get(pIdx);
                            var argName = argPrefix + pIdx;
                            // signature
                            importMethod.addParameter(javaClassFromValueType(param), argName);
                        }
                    }
                }
                var toImportsBuilder = new MethodCallExpr(new NameExpr("imports"), "build");
                toImportValuesBody.addStatement(new ReturnStmt(toImportsBuilder));

                var toImportValuesMethod =
                        importsInterface.addMethod("toImportValues", Modifier.Keyword.DEFAULT);
                importsCu.addImport("com.dylibso.chicory.runtime.ImportValues");
                toImportValuesMethod.setType("ImportValues");
                toImportValuesMethod.setBody(toImportValuesBody);
            }
        }

        return result;
    }

    private CompilationUnit newCu() {
        if (packageName.isEmpty()) {
            return new CompilationUnit();
        }
        var cu = new CompilationUnit(packageName);
        cu.setPackageDeclaration(packageName);
        return cu;
    }

    private static java.util.function.Function<String, Expression> exportMethodBodyGen(
            Expression exportCallHandle, String exportName) {
        return accessor ->
                new MethodCallExpr(
                        exportCallHandle,
                        accessor,
                        NodeList.nodeList(new StringLiteralExpr(exportName)));
    }

    static Class<?> javaClassFromValueType(ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
                return int.class;
            case ValType.ID.I64:
                return long.class;
            case ValType.ID.F32:
                return float.class;
            case ValType.ID.F64:
                return double.class;
            default:
                if (ValType.TypeIdxCode.EXTERN.code() == type.typeIdx()) {
                    return long.class;
                }
                throw new IllegalArgumentException(
                        "javaClassFromValueType - Unsupported WASM type: " + type);
        }
    }

    static Expression toLong(ValType type, Expression nameExpr, CompilationUnit cu) {
        switch (type.opcode()) {
            case ValType.ID.I32:
                return new CastExpr(parseType("long"), nameExpr);
            case ValType.ID.I64:
                return nameExpr;
            case ValType.ID.F32:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "floatToLong", new NodeList<>(nameExpr));
            case ValType.ID.F64:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "doubleToLong", new NodeList<>(nameExpr));
            default:
                if (ValType.TypeIdxCode.EXTERN.code() == type.typeIdx()) {
                    return nameExpr;
                }
                throw new IllegalArgumentException("toLong - Unsupported WASM type: " + type);
        }
    }

    static Expression fromLong(ValType type, Expression nameExpr, CompilationUnit cu) {
        switch (type.opcode()) {
            case ValType.ID.I32:
                return new CastExpr(parseType("int"), nameExpr);
            case ValType.ID.I64:
                return nameExpr;
            case ValType.ID.F32:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "longToFloat", new NodeList<>(nameExpr));
            case ValType.ID.F64:
                cu.addImport(Value.class);
                return new MethodCallExpr(
                        new NameExpr("Value"), "longToDouble", new NodeList<>(nameExpr));
            default:
                if (ValType.TypeIdxCode.EXTERN.code() == type.typeIdx()) {
                    return nameExpr;
                }
                throw new IllegalArgumentException("fromLong - Unsupported WASM type: " + type);
        }
    }

    private Expression listOfValueTypes(List<ValType> valTypes) {
        List<Expression> values =
                valTypes.stream()
                        .map(
                                vt -> {
                                    switch (vt.opcode()) {
                                        case ValType.ID.I32:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValType"), "I32");
                                        case ValType.ID.I64:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValType"), "I64");
                                        case ValType.ID.F32:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValType"), "F32");
                                        case ValType.ID.F64:
                                            return new FieldAccessExpr(
                                                    new NameExpr("ValType"), "F64");
                                        default:
                                            if (ValType.TypeIdxCode.EXTERN.code() == vt.typeIdx()) {
                                                return new FieldAccessExpr(
                                                        new NameExpr("ValType"), "ExternRef");
                                            }
                                            throw new IllegalArgumentException(
                                                    "listOfValueTypes - Unsupported WASM type: "
                                                            + vt);
                                    }
                                })
                        .collect(Collectors.toList());
        return new MethodCallExpr(new NameExpr("List"), "of", NodeList.nodeList(values));
    }

    static String deduplicatedMethodName(String name, List<String> names) {
        if (!names.contains(name)) {
            return name;
        } else {
            return deduplicatedMethodName("_" + name, names);
        }
    }
}
