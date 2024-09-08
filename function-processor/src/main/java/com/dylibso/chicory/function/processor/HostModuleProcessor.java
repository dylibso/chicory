package com.dylibso.chicory.function.processor;

import static com.github.javaparser.StaticJavaParser.parseType;
import static javax.tools.Diagnostic.Kind.ERROR;

import com.dylibso.chicory.function.annotations.Buffer;
import com.dylibso.chicory.function.annotations.CString;
import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public final class HostModuleProcessor extends AbstractModuleProcessor {
    public HostModuleProcessor() {
        super(HostModule.class);
    }

    @Override
    protected void processModule(TypeElement type) {
        var moduleName = type.getAnnotation(HostModule.class).value();

        var functions = new NodeList<Expression>();
        for (Element member : elements().getAllMembers(type)) {
            if (member instanceof ExecutableElement && annotatedWith(member, WasmExport.class)) {
                functions.add(processMethod(member, (ExecutableElement) member, moduleName));
            }
        }

        var pkg = getPackageName(type);
        var cu = createCompilationUnit(pkg, type);
        cu.addImport("com.dylibso.chicory.runtime.HostFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.wasm.types.ValueType");
        cu.addImport("java.util.List");

        var typeName = type.getSimpleName().toString();
        var classDef = cu.addClass(typeName + "_ModuleFactory").setPublic(true).setFinal(true);
        addGeneratedAnnotation(classDef);

        classDef.addConstructor().setPrivate(true);

        var newHostFunctions =
                new ArrayCreationExpr(
                        parseType("HostFunction"),
                        new NodeList<>(new ArrayCreationLevel()),
                        new ArrayInitializerExpr(functions));

        classDef.addMethod("toHostFunctions")
                .setPublic(true)
                .setStatic(true)
                .addParameter(typeName, "functions")
                .setType("HostFunction[]")
                .setBody(new BlockStmt(new NodeList<>(new ReturnStmt(newHostFunctions))));

        writeSourceFile(cu, pkg, type, "_ModuleFactory");
    }

    private Expression processMethod(
            Element member, ExecutableElement executable, String moduleName) {
        // compute function name
        var name = executable.getAnnotation(WasmExport.class).value();
        if (name.isEmpty()) {
            name = camelCaseToSnakeCase(executable.getSimpleName().toString());
        }

        // compute parameter types and argument conversions
        NodeList<Expression> paramTypes = new NodeList<>();
        NodeList<Expression> arguments = new NodeList<>();
        for (VariableElement parameter : executable.getParameters()) {
            var argExpr = argExpr(paramTypes.size());
            switch (parameter.asType().toString()) {
                case "int":
                    paramTypes.add(valueType("I32"));
                    arguments.add(new MethodCallExpr(argExpr, "asInt"));
                    break;
                case "long":
                    paramTypes.add(valueType("I64"));
                    arguments.add(new MethodCallExpr(argExpr, "asLong"));
                    break;
                case "float":
                    paramTypes.add(valueType("F32"));
                    arguments.add(new MethodCallExpr(argExpr, "asFloat"));
                    break;
                case "double":
                    paramTypes.add(valueType("F64"));
                    arguments.add(new MethodCallExpr(argExpr, "asDouble"));
                    break;
                case "java.lang.String":
                    if (annotatedWith(parameter, Buffer.class)) {
                        var lenExpr = argExpr(paramTypes.size() + 1);
                        paramTypes.add(valueType("I32"));
                        paramTypes.add(valueType("I32"));
                        arguments.add(
                                new MethodCallExpr(
                                        new MethodCallExpr(new NameExpr("instance"), "memory"),
                                        "readString",
                                        new NodeList<>(
                                                new MethodCallExpr(argExpr, "asInt"),
                                                new MethodCallExpr(lenExpr, "asInt"))));
                    } else if (annotatedWith(parameter, CString.class)) {
                        paramTypes.add(valueType("I32"));
                        paramTypes.add(valueType("I32"));
                        arguments.add(
                                new MethodCallExpr(
                                        new MethodCallExpr(new NameExpr("instance"), "memory"),
                                        "readCString",
                                        new NodeList<>(new MethodCallExpr(argExpr, "asInt"))));
                    } else {
                        log(ERROR, "Missing annotation for WASM type: java.lang.String", parameter);
                        throw new AbortProcessingException();
                    }
                    break;
                case "com.dylibso.chicory.runtime.Instance":
                    arguments.add(new NameExpr("instance"));
                    break;
                case "com.dylibso.chicory.runtime.Memory":
                    arguments.add(new MethodCallExpr(new NameExpr("instance"), "memory"));
                    break;
                default:
                    log(ERROR, "Unsupported WASM type: " + parameter.asType(), parameter);
                    throw new AbortProcessingException();
            }
        }

        // compute return type and conversion
        String returnName = executable.getReturnType().toString();
        NodeList<Expression> returnType = new NodeList<>();
        String returnExpr = null;
        switch (returnName) {
            case "void":
                break;
            case "int":
                returnType.add(valueType("I32"));
                returnExpr = "i32";
                break;
            case "long":
                returnType.add(valueType("I64"));
                returnExpr = "i64";
                break;
            case "float":
                returnType.add(valueType("F32"));
                returnExpr = "fromFloat";
                break;
            case "double":
                returnType.add(valueType("F64"));
                returnExpr = "fromDouble";
                break;
            default:
                log(ERROR, "Unsupported WASM type: " + returnName, executable);
                throw new AbortProcessingException();
        }

        // function invocation
        Expression invocation =
                new MethodCallExpr(
                        new NameExpr("functions"), member.getSimpleName().toString(), arguments);

        // convert return value
        BlockStmt handleBody = new BlockStmt();
        if (returnType.isEmpty()) {
            handleBody.addStatement(invocation).addStatement(new ReturnStmt(new NullLiteralExpr()));
        } else {
            var result = new VariableDeclarator(parseType(returnName), "result", invocation);
            var boxed =
                    new MethodCallExpr(
                            new NameExpr("Value"),
                            returnExpr,
                            new NodeList<>(new NameExpr("result")));
            var wrapped =
                    new ArrayCreationExpr(
                            parseType("Value"),
                            new NodeList<>(new ArrayCreationLevel()),
                            new ArrayInitializerExpr(new NodeList<>(boxed)));
            handleBody
                    .addStatement(new ExpressionStmt(new VariableDeclarationExpr(result)))
                    .addStatement(new ReturnStmt(wrapped));
        }

        // lambda for host function
        var handle =
                new LambdaExpr()
                        .addParameter("Instance", "instance")
                        .addParameter(new Parameter(parseType("Value"), "args").setVarArgs(true))
                        .setEnclosingParameters(true)
                        .setBody(handleBody);

        // create host function
        var function =
                new ObjectCreationExpr()
                        .setType("HostFunction")
                        .addArgument(handle)
                        .addArgument(new StringLiteralExpr(moduleName))
                        .addArgument(new StringLiteralExpr(name))
                        .addArgument(new MethodCallExpr(new NameExpr("List"), "of", paramTypes))
                        .addArgument(new MethodCallExpr(new NameExpr("List"), "of", returnType));
        function.setLineComment("");
        return function;
    }

    private static Expression argExpr(int n) {
        return new ArrayAccessExpr(new NameExpr("args"), new IntegerLiteralExpr(String.valueOf(n)));
    }
}
