package com.dylibso.chicory.function.processor;

import static com.github.javaparser.StaticJavaParser.parseMethodDeclaration;
import static com.github.javaparser.StaticJavaParser.parseType;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static javax.tools.Diagnostic.Kind.ERROR;

import com.dylibso.chicory.function.annotations.Allocate;
import com.dylibso.chicory.function.annotations.Buffer;
import com.dylibso.chicory.function.annotations.CString;
import com.dylibso.chicory.function.annotations.Free;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.function.annotations.WasmModule;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public final class WasmModuleProcessor extends AbstractModuleProcessor {
    public WasmModuleProcessor() {
        super(WasmModule.class);
    }

    @Override
    protected void processModule(TypeElement type) {
        var pkg = getPackageName(type);
        var cu = createCompilationUnit(pkg, type);

        cu.addImport("com.dylibso.chicory.runtime.ExportFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Memory");
        cu.addImport("com.dylibso.chicory.wasm.types.FunctionType");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.wasm.types.ValueType");
        cu.addImport("java.nio.charset.StandardCharsets");
        cu.addImport("java.util.List");

        var typeName = type.getSimpleName().toString();
        var moduleFactory = cu.addClass(typeName + "_ModuleFactory").setPublic(true).setFinal(true);
        addGeneratedAnnotation(moduleFactory);

        moduleFactory.addConstructor().setPrivate(true);

        // instance factory method
        moduleFactory
                .addMethod("create")
                .setPublic(true)
                .setStatic(true)
                .addParameter("Instance", "instance")
                .setType(typeName)
                .setBody(
                        new BlockStmt()
                                .addStatement(
                                        new ReturnStmt(
                                                new ObjectCreationExpr()
                                                        .setType(typeName + "_Instance")
                                                        .addArgument(new NameExpr("instance")))));

        // nested instance class
        var instance =
                new ClassOrInterfaceDeclaration()
                        .setName(typeName + "_Instance")
                        .setPrivate(true)
                        .setStatic(true)
                        .setFinal(true)
                        .addImplementedType(typeName);
        moduleFactory.addMember(instance);

        // declare memory field
        instance.addField("Memory", "memory").setPrivate(true).setFinal(true);

        // assign memory field in constructor
        var constructorBody = new BlockStmt();
        constructorBody.addStatement(
                new ExpressionStmt(
                        new AssignExpr(
                                new NameExpr("memory"),
                                methodCall("instance", "memory"),
                                AssignExpr.Operator.ASSIGN)));

        // find annotated methods
        var methods =
                elements().getAllMembers(type).stream()
                        .filter(member -> member instanceof ExecutableElement)
                        .filter(member -> annotatedWith(member, WasmExport.class))
                        .map(member -> (ExecutableElement) member)
                        .collect(toList());

        // find allocate and free functions
        Optional<String> allocate = findAllocate(methods);
        Optional<String> free = findFree(methods);

        // declare fields and collect methods
        var functions =
                methods.stream()
                        .map(
                                member ->
                                        processMethod(
                                                member, instance, constructorBody, allocate, free))
                        .collect(toCollection(NodeList::new));

        // declare constructor
        instance.addConstructor()
                .setPublic(true)
                .addParameter("Instance", "instance")
                .setBody(constructorBody);

        // declare methods
        for (MethodDeclaration function : functions) {
            instance.addMember(function);
        }

        declareCheckTypeMethods(instance);

        writeSourceFile(cu, pkg, type, "_ModuleFactory");
    }

    private MethodDeclaration processMethod(
            ExecutableElement executable,
            ClassOrInterfaceDeclaration classDef,
            BlockStmt constructor,
            Optional<String> allocate,
            Optional<String> free) {

        // compute function name
        var methodName = executable.getSimpleName().toString();
        var wasmName = executable.getAnnotation(WasmExport.class).value();
        if (wasmName.isEmpty()) {
            wasmName = camelCaseToSnakeCase(methodName);
        }

        // declare field
        String fieldName = "_" + methodName;
        classDef.addField("ExportFunction", fieldName).setPrivate(true).setFinal(true);

        // assign field in constructor
        constructor.addStatement(
                new ExpressionStmt(
                        new AssignExpr(
                                new NameExpr(fieldName),
                                methodCall("instance", "export", new StringLiteralExpr(wasmName)),
                                AssignExpr.Operator.ASSIGN)));

        // compute parameter types and argument conversions
        BlockStmt method = new BlockStmt();
        List<Statement> cleanup = new ArrayList<>();
        var parameters = new NodeList<Parameter>();
        var paramTypes = new NodeList<Expression>();
        var arguments = new NodeList<Expression>();
        for (VariableElement parameter : executable.getParameters()) {
            var name = "_" + parameter.getSimpleName().toString();
            var nameExpr = new NameExpr(name);
            var paramTypeName = parameter.asType().toString();
            switch (parameter.asType().toString()) {
                case "int":
                    paramTypes.add(valueType("I32"));
                    arguments.add(methodCall("Value", "i32", nameExpr));
                    break;
                case "long":
                    paramTypes.add(valueType("I64"));
                    arguments.add(methodCall("Value", "i64", nameExpr));
                    break;
                case "float":
                    paramTypes.add(valueType("F32"));
                    arguments.add(methodCall("Value", "fromFloat", nameExpr));
                    break;
                case "double":
                    paramTypes.add(valueType("F64"));
                    arguments.add(methodCall("Value", "fromDouble", nameExpr));
                    break;
                case "java.lang.String":
                    paramTypeName = "String";
                    // validation
                    boolean buffer = annotatedWith(parameter, Buffer.class);
                    boolean cstring = annotatedWith(parameter, CString.class);
                    if (!buffer && !cstring) {
                        log(ERROR, "Missing annotation for WASM type: java.lang.String", parameter);
                        throw new AbortProcessingException();
                    }
                    if (allocate.isEmpty()) {
                        log(ERROR, "No method is annotated with @Allocate", parameter);
                        throw new AbortProcessingException();
                    }
                    if (free.isEmpty()) {
                        log(ERROR, "No method is annotated with @Free", parameter);
                        throw new AbortProcessingException();
                    }
                    // byte[] bytes$name = $name.getBytes(StandardCharsets.UTF_8);
                    var bytesName = "bytes" + name;
                    var utf8 = new FieldAccessExpr(new NameExpr("StandardCharsets"), "UTF_8");
                    var getBytes = methodCall(name, "getBytes", utf8);
                    var bytesLength = new FieldAccessExpr(new NameExpr(bytesName), "length");
                    method.addStatement(declareVariable(parseType("byte[]"), bytesName, getBytes));
                    // int ptr$name = malloc(...);
                    var ptrName = "ptr" + name;
                    var ptrNameExpr = new NameExpr(ptrName);
                    Expression allocateLength = bytesLength;
                    if (cstring) {
                        allocateLength =
                                new BinaryExpr(
                                        bytesLength,
                                        new IntegerLiteralExpr("1"),
                                        BinaryExpr.Operator.PLUS);
                    }
                    method.addStatement(
                            declareVariable(
                                    parseType("int"),
                                    ptrName,
                                    new MethodCallExpr(allocate.get(), allocateLength)));
                    // memory.write(ptr$name, bytes$name);
                    method.addStatement(
                            new ExpressionStmt(
                                    methodCall(
                                            "memory",
                                            "write",
                                            ptrNameExpr,
                                            new NameExpr(bytesName))));
                    if (cstring) {
                        // memory.writeByte(ptr$name + bytes$name.length, (byte) 0);
                        method.addStatement(
                                new ExpressionStmt(
                                        methodCall(
                                                "memory",
                                                "writeByte",
                                                new BinaryExpr(
                                                        ptrNameExpr,
                                                        bytesLength,
                                                        BinaryExpr.Operator.PLUS),
                                                new CastExpr(
                                                        parseType("byte"),
                                                        new IntegerLiteralExpr("0")))));
                    }
                    // free(ptr$name);
                    cleanup.add(new ExpressionStmt(new MethodCallExpr(free.get(), ptrNameExpr)));
                    // arguments
                    paramTypes.add(valueType("I32"));
                    arguments.add(methodCall("Value", "i32", ptrNameExpr));
                    if (buffer) {
                        paramTypes.add(valueType("I32"));
                        arguments.add(methodCall("Value", "i32", bytesLength));
                    }
                    break;
                default:
                    log(ERROR, "Unsupported WASM type: " + parameter.asType(), parameter);
                    throw new AbortProcessingException();
            }
            parameters.add(new Parameter(parseType(paramTypeName), name));
        }

        // wrap arguments
        var wrappedArgs =
                new ArrayCreationExpr(parseType("Value"))
                        .setInitializer(new ArrayInitializerExpr(arguments));
        method.addStatement(declareVariable(parseType("Value[]"), "args", wrappedArgs));

        // function invocation
        var invoke = methodCall(fieldName, "apply", new NameExpr("args"));

        // compute return type and conversion
        String returnTypeName = executable.getReturnType().toString();
        String resultType = returnTypeName;
        Expression result;
        NodeList<Expression> returnType;
        switch (returnTypeName) {
            case "void":
                returnType = new NodeList<>();
                result = null;
                break;
            case "int":
                returnType = new NodeList<>(valueType("I32"));
                result = new MethodCallExpr(arrayZero(invoke), "asInt");
                break;
            case "long":
                returnType = new NodeList<>(valueType("I64"));
                result = new MethodCallExpr(arrayZero(invoke), "asLong");
                break;
            case "float":
                returnType = new NodeList<>(valueType("F32"));
                result = new MethodCallExpr(arrayZero(invoke), "asFloat");
                break;
            case "double":
                returnType = new NodeList<>(valueType("F64"));
                result = new MethodCallExpr(arrayZero(invoke), "asDouble");
                break;
            case "com.dylibso.chicory.wasm.types.Value[]":
                returnType = null;
                resultType = "Value[]";
                result = invoke;
                break;
            default:
                log(ERROR, "Unsupported WASM type: " + returnTypeName, executable);
                throw new AbortProcessingException();
        }

        // assign return value
        if (result != null) {
            method.addStatement(declareVariable(parseType(resultType), "result", result));
        } else {
            method.addStatement(new ExpressionStmt(invoke));
        }

        // cleanup
        cleanup.forEach(method::addStatement);

        // return result
        if (result != null) {
            method.addStatement(new ReturnStmt(new NameExpr("result")));
        }

        // check type in constructor
        Expression checkExpected;
        if (returnType != null) {
            checkExpected =
                    methodCall(
                            "FunctionType",
                            "of",
                            new MethodCallExpr(new NameExpr("List"), "of", paramTypes),
                            new MethodCallExpr(new NameExpr("List"), "of", returnType));
        } else {
            checkExpected = new MethodCallExpr(new NameExpr("List"), "of", paramTypes);
        }
        constructor.addStatement(
                new ExpressionStmt(
                        new MethodCallExpr(
                                "checkType",
                                new NameExpr("instance"),
                                new StringLiteralExpr(wasmName),
                                checkExpected)));

        return new MethodDeclaration()
                .setPublic(true)
                .setName(methodName)
                .addMarkerAnnotation(Override.class)
                .setParameters(parameters)
                .setType(resultType)
                .setBody(method);
    }

    private Optional<String> findAllocate(List<ExecutableElement> methods) {
        var list =
                methods.stream()
                        .filter(member -> annotatedWith(member, Allocate.class))
                        .collect(toList());
        if (list.isEmpty()) {
            return Optional.empty();
        }
        if (list.size() > 1) {
            log(ERROR, "Method with @Allocate previously declared", list.get(1));
            throw new AbortProcessingException();
        }
        ExecutableElement method = list.get(0);
        if (method.getParameters().toString().equals("[int]")) {
            log(ERROR, "Method with @Allocate must have a single 'int' parameter", method);
            throw new AbortProcessingException();
        }
        if (!method.getReturnType().toString().equals("int")) {
            log(ERROR, "Method with @Allocate must return 'int'", method);
            throw new AbortProcessingException();
        }
        return Optional.of(method.getSimpleName().toString());
    }

    private Optional<String> findFree(List<ExecutableElement> methods) {
        var list =
                methods.stream()
                        .filter(member -> annotatedWith(member, Free.class))
                        .collect(toList());
        if (list.isEmpty()) {
            return Optional.empty();
        }
        if (list.size() > 1) {
            log(ERROR, "Method with @Free previously declared", list.get(1));
            throw new AbortProcessingException();
        }
        ExecutableElement method = list.get(0);
        if (method.getParameters().toString().equals("[int]")) {
            log(ERROR, "Method with @Free must have a single 'int' parameter", method);
            throw new AbortProcessingException();
        }
        if (!method.getReturnType().toString().equals("void")) {
            log(ERROR, "Method with @Free must return 'void'", method);
            throw new AbortProcessingException();
        }
        return Optional.of("free");
    }

    private static void declareCheckTypeMethods(ClassOrInterfaceDeclaration classDef) {
        classDef.addMember(
                parseMethodDeclaration(
                        "private static void checkType(Instance instance, String name, FunctionType"
                                + " expected) {\n"
                                + "    checkType(name, expected, instance.exportType(name));\n"
                                + "}\n"));

        classDef.addMember(
                parseMethodDeclaration(
                        "private static void checkType(Instance instance, String name,"
                            + " List<ValueType> expected) {\n"
                            + "    checkType(name, expected, instance.exportType(name).params());\n"
                            + "}\n"));

        classDef.addMember(
                parseMethodDeclaration(
                        "private static <T> void checkType(String name, T expected, T actual) {\n"
                            + "    if (!expected.equals(actual)) {\n"
                            + "        throw new IllegalArgumentException(String.format(\n"
                            + "                \"Function type mismatch for '%s': expected %s <=>"
                            + " actual %s\", name, expected, actual));\n"
                            + "    }\n"
                            + "}\n"));
    }

    private static Expression methodCall(String scope, String name, Expression... arguments) {
        return new MethodCallExpr(new NameExpr(scope), name, new NodeList<>(arguments));
    }

    private static Statement declareVariable(Type type, String name, Expression result) {
        return new ExpressionStmt(
                new VariableDeclarationExpr(new VariableDeclarator(type, name, result)));
    }

    private static Expression arrayZero(Expression array) {
        return new ArrayAccessExpr(array, new IntegerLiteralExpr("0"));
    }
}
