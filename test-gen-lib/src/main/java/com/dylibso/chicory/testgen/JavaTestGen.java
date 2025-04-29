package com.dylibso.chicory.testgen;

import com.dylibso.chicory.testgen.wast.ActionType;
import com.dylibso.chicory.testgen.wast.Command;
import com.dylibso.chicory.testgen.wast.CommandType;
import com.dylibso.chicory.testgen.wast.WasmValue;
import com.dylibso.chicory.testgen.wast.WasmValueType;
import com.dylibso.chicory.testgen.wast.Wast;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.StringEscapeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaTestGen {

    private static final String TEST_MODULE_NAME = "testModule";

    private final List<String> excludedTests;

    private final List<String> excludedMalformedWasts;

    private final List<String> excludedInvalidWasts;

    private final List<String> excludedUninstantiableWasts;

    private final List<String> excludedUnlinkableWasts;

    public JavaTestGen(
            List<String> excludedTests,
            List<String> excludedMalformedWasts,
            List<String> excludedInvalidWasts,
            List<String> excludedUninstantiableWasts,
            List<String> excludedUnlinkableWasts) {
        this.excludedTests = excludedTests;
        this.excludedMalformedWasts = excludedMalformedWasts;
        this.excludedInvalidWasts = excludedInvalidWasts;
        this.excludedUninstantiableWasts = excludedUninstantiableWasts;
        this.excludedUnlinkableWasts = excludedUnlinkableWasts;
    }

    public CompilationUnit generate(String name, Wast wast, String wasmClasspath) {
        var cu = new CompilationUnit("com.dylibso.chicory.test.gen");
        var testName =
                "SpecV1" + StringUtils.capitalize(StringUtils.escapedCamelCase(name)) + "Test";

        // all the imports
        // junit imports
        cu.addImport("java.io.File");
        cu.addImport("org.junit.jupiter.api.Disabled");
        cu.addImport("org.junit.jupiter.api.Test");
        cu.addImport("org.junit.jupiter.api.DisplayName");
        cu.addImport("org.junit.jupiter.api.MethodOrderer");
        cu.addImport("org.junit.jupiter.api.TestMethodOrder");
        cu.addImport("org.junit.jupiter.api.Order");
        cu.addImport("org.junit.jupiter.api.TestInstance");
        cu.addImport("org.junit.jupiter.api.Assertions.assertArrayEquals", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertEquals", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertTrue", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertDoesNotThrow", true, false);

        // testing imports
        cu.addImport("com.dylibso.chicory.testing.TestModule");
        cu.addImport("com.dylibso.chicory.testing.ArgsAdapter");

        // runtime imports
        cu.addImport("com.dylibso.chicory.wasm.ChicoryException");
        cu.addImport("com.dylibso.chicory.runtime.WasmException");
        cu.addImport("com.dylibso.chicory.runtime.ExportFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");

        // base imports
        cu.addImport("com.dylibso.chicory.wasm.InvalidException");
        cu.addImport("com.dylibso.chicory.wasm.MalformedException");
        cu.addImport("com.dylibso.chicory.wasm.UninstantiableException");
        cu.addImport("com.dylibso.chicory.wasm.UnlinkableException");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");

        cu.addImport("com.dylibso.chicory.wasm.types.Value.vecTo8", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.vecTo16", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.vecTo32", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.vecToF32", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.vecToF64", true, false);

        cu.addImport("com.dylibso.chicory.wasm.types.Value.i8ToVec", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.i16ToVec", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.i32ToVec", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.i64ToVec", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.f64ToVec", true, false);
        cu.addImport("com.dylibso.chicory.wasm.types.Value.f32ToVec", true, false);

        // import for Store instance
        cu.addImport("com.dylibso.chicory.runtime.Store");
        // import for shared Spectest host module
        cu.addImport("com.dylibso.chicory.testing.Spectest");

        var testClass = cu.addClass(testName);
        testClass.addSingleMemberAnnotation(
                "TestMethodOrder", new NameExpr("MethodOrderer.OrderAnnotation.class"));
        testClass.addSingleMemberAnnotation(
                "TestInstance",
                new FieldAccessExpr(
                        new FieldAccessExpr(new NameExpr("TestInstance"), "Lifecycle"),
                        "PER_CLASS"));

        MethodDeclaration method;
        int testNumber = 0;
        int moduleInstantiationNumber = 0;
        String lastModuleVarName = null;
        int fallbackVarNumber = 0;

        var excludedMethods =
                excludedTests.stream()
                        .filter(t -> t.startsWith(testName))
                        .map(t -> t.substring(testName.length() + 1))
                        .collect(Collectors.toList());

        testClass.addFieldWithInitializer(
                "Store",
                "store",
                new NameExpr("new Store().addImportValues(Spectest.toImportValues())"));

        String currentWasmFile = null;
        for (var cmd : wast.commands()) {
            switch (cmd.type()) {
                case MODULE:
                    {
                        currentWasmFile = getWasmFile(cmd, wasmClasspath);
                        lastModuleVarName = TEST_MODULE_NAME + moduleInstantiationNumber;
                        String lastInstanceVarName = lastModuleVarName + "Instance";
                        moduleInstantiationNumber++;
                        if (cmd.name() != null) {
                            lastModuleVarName = cmd.name().replace("$", "");
                            lastInstanceVarName = cmd.name().replace("$", "") + "Instance";
                        }
                        testClass.addFieldWithInitializer(
                                "Instance",
                                lastInstanceVarName,
                                new NullLiteralExpr(),
                                Modifier.Keyword.PUBLIC,
                                Modifier.Keyword.STATIC);

                        var instantiateMethodName = "instantiate_" + lastInstanceVarName;
                        var instantiateMethod =
                                testClass.addMethod(instantiateMethodName, Modifier.Keyword.PUBLIC);
                        // It needs to be a test to be executed
                        instantiateMethod.addAnnotation("Test");
                        instantiateMethod.addSingleMemberAnnotation(
                                "Order", new IntegerLiteralExpr(Integer.toString(testNumber++)));
                        instantiateMethod.addSingleMemberAnnotation(
                                "DisplayName",
                                new StringLiteralExpr(
                                        formatWastFileCoordinates(
                                                wast.sourceFilename().getName(),
                                                cmd.line(),
                                                cmd.filename())));

                        instantiateMethod.setBody(
                                new BlockStmt()
                                        .addStatement(
                                                new AssignExpr(
                                                        new NameExpr(lastInstanceVarName),
                                                        generateModuleInstantiation(
                                                                currentWasmFile,
                                                                getExcluded(
                                                                        CommandType.ASSERT_INVALID,
                                                                        name)),
                                                        AssignExpr.Operator.ASSIGN)));
                        break;
                    }
                case ACTION:
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                case ASSERT_EXHAUSTION:
                case ASSERT_EXCEPTION:
                    {
                        method =
                                createTestMethod(
                                        wast.sourceFilename().getName(),
                                        cmd,
                                        testClass,
                                        testNumber++,
                                        excludedMethods);

                        var baseVarName = StringUtils.escapedCamelCase(cmd.action().field());
                        var varNum = fallbackVarNumber++;
                        var varName = "var" + (baseVarName.isEmpty() ? varNum : baseVarName);
                        String moduleName = lastModuleVarName;
                        if (cmd.action().module() != null) {
                            moduleName = cmd.action().module().replace("$", "");
                        }
                        var fieldExport = generateFieldExport(varName, cmd, moduleName);
                        if (fieldExport.isPresent()) {
                            method.getBody().get().addStatement(fieldExport.get());
                        }

                        if (cmd.type() == CommandType.ACTION) {
                            for (var expr : generateInvoke(varName, cmd)) {
                                method.getBody().get().addStatement(expr);
                            }
                        } else {
                            for (var expr : generateAssert(varName, cmd)) {
                                method.getBody().get().addStatement(expr);
                            }
                        }
                        break;
                    }
                case REGISTER:
                    String lastInstanceVarName = lastModuleVarName + "Instance";

                    generateRegisterInstance(cmd.as(), lastInstanceVarName);

                    var instantiateMethodName = "register_" + lastInstanceVarName;
                    var instantiateMethod =
                            testClass.addMethod(instantiateMethodName, Modifier.Keyword.PUBLIC);
                    // It needs to be a test to be executed
                    instantiateMethod.addAnnotation("Test");
                    instantiateMethod.addSingleMemberAnnotation(
                            "Order", new IntegerLiteralExpr(Integer.toString(testNumber++)));

                    instantiateMethod.setBody(
                            new BlockStmt()
                                    .addStatement(
                                            generateRegisterInstance(
                                                    cmd.as(), lastInstanceVarName)));

                    break;
                case ASSERT_MALFORMED:
                case ASSERT_INVALID:
                case ASSERT_UNINSTANTIABLE:
                case ASSERT_UNLINKABLE:
                    {
                        method =
                                createTestMethod(
                                        wast.sourceFilename().getName(),
                                        cmd,
                                        testClass,
                                        testNumber++,
                                        excludedMethods);
                        generateAssertThrows(
                                wasmClasspath,
                                cmd,
                                method,
                                getExcluded(cmd.type(), name),
                                getExceptionType(cmd.type()));
                        break;
                    }
                default:
                    throw new IllegalArgumentException(
                            "command type not yet supported " + cmd.type());
            }
        }

        return cu;
    }

    private static String formatWastFileCoordinates(String wastName, int line, String wasmName) {
        if (wasmName != null) {
            return wastName + ":" + line + " @ " + wasmName;
        } else {
            return wastName + ":" + line;
        }
    }

    private boolean getExcluded(CommandType typ, String name) {
        switch (typ) {
            case ASSERT_MALFORMED:
                return excludedMalformedWasts.contains(name + ".wast");
            case ASSERT_INVALID:
                return excludedInvalidWasts.contains(name + ".wast");
            case ASSERT_UNINSTANTIABLE:
                return excludedUninstantiableWasts.contains(name + ".wast");
            case ASSERT_UNLINKABLE:
                return excludedUnlinkableWasts.contains(name + ".wast");
            case ASSERT_EXHAUSTION:
            case ASSERT_TRAP:
            case ASSERT_EXCEPTION:
                return false;
            default:
                throw new IllegalArgumentException(typ + "not implemented");
        }
    }

    private String getExceptionType(CommandType typ) {
        switch (typ) {
            case ASSERT_MALFORMED:
                return "MalformedException";
            case ASSERT_INVALID:
                return "InvalidException";
            case ASSERT_UNINSTANTIABLE:
                return "UninstantiableException";
            case ASSERT_UNLINKABLE:
                return "UnlinkableException";
            case ASSERT_TRAP:
            case ASSERT_EXHAUSTION:
                return "ChicoryException";
            case ASSERT_EXCEPTION:
                return "WasmException";
            default:
                throw new IllegalArgumentException(typ + "not implemented");
        }
    }

    private MethodDeclaration createTestMethod(
            String wastName,
            Command cmd,
            ClassOrInterfaceDeclaration testClass,
            int testNumber,
            List<String> excludedTests) {
        var methodName = "test" + testNumber;
        var method = testClass.addMethod(methodName, Modifier.Keyword.PUBLIC);
        if (excludedTests.contains(methodName)) {
            method.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("Disabled"), new StringLiteralExpr("Test excluded")));
        }
        method.addAnnotation("Test");
        method.addSingleMemberAnnotation(
                "Order", new IntegerLiteralExpr(Integer.toString(testNumber)));
        method.addSingleMemberAnnotation(
                "DisplayName",
                new StringLiteralExpr(
                        formatWastFileCoordinates(wastName, cmd.line(), cmd.filename())));

        return method;
    }

    private Optional<Expression> generateFieldExport(
            String varName, Command cmd, String moduleName) {
        if (cmd.action() != null && cmd.action().field() != null) {
            var accessor = (cmd.action().type() == ActionType.INVOKE) ? "function" : "global";
            var declarator =
                    new VariableDeclarator()
                            .setName(varName)
                            .setType("var")
                            .setInitializer(
                                    new NameExpr(
                                            moduleName
                                                    + "Instance.exports()."
                                                    + accessor
                                                    + "(\""
                                                    + StringEscapeUtils.escapeJava(
                                                            cmd.action().field())
                                                    + "\")"));
            Expression varDecl = new VariableDeclarationExpr(declarator);
            return Optional.of(varDecl);
        } else {
            return Optional.empty();
        }
    }

    private List<Expression> generateAssert(String varName, Command cmd) {
        assert (cmd.type() == CommandType.ASSERT_RETURN
                || cmd.type() == CommandType.ASSERT_TRAP
                || cmd.type() == CommandType.ASSERT_EXCEPTION
                || cmd.type() == CommandType.ASSERT_EXHAUSTION);
        assert (cmd.expected() != null);
        assert (cmd.expected().length > 0);
        assert (cmd.action().type() == ActionType.INVOKE);

        var args =
                (cmd.action().args() != null)
                        ? Arrays.stream(cmd.action().args())
                                .map(WasmValue::toArgsValue)
                                .collect(Collectors.toList())
                        : List.<String>of();

        var adaptedArgs =
                (args == null || args.size() == 0)
                        ? ""
                        : args.stream().collect(Collectors.joining(").add(", ".add(", ")"));

        // Function or Global
        var invocationMethod =
                (cmd.action().type() == ActionType.INVOKE)
                        ? ".apply(ArgsAdapter.builder()" + adaptedArgs + ".build()" + ")"
                        : ".getValue()";

        if (cmd.type() == CommandType.ASSERT_TRAP
                || cmd.type() == CommandType.ASSERT_EXHAUSTION
                || cmd.type() == CommandType.ASSERT_EXCEPTION) {
            var assertDecl =
                    new NameExpr(
                            "var exception ="
                                    + " assertThrows("
                                    + getExceptionType(cmd.type())
                                    + ".class, () -> "
                                    + varName
                                    + invocationMethod
                                    + ")");
            if (cmd.text() != null) {
                return List.of(assertDecl, exceptionMessageMatch(cmd.text()));
            } else {
                return List.of(assertDecl);
            }
        } else if (cmd.type() == CommandType.ASSERT_RETURN) {
            List<Expression> exprs = new ArrayList<>();
            var resVarName = (cmd.action().type() == ActionType.INVOKE) ? "results" : "result";
            exprs.add(new NameExpr("var " + resVarName + " = " + varName + invocationMethod));

            for (int i = 0; i < cmd.expected().length; i++) {
                var expected = cmd.expected()[i];
                var expectedVar = expected.toExpectedValue();
                var resultVar =
                        (cmd.action().type() == ActionType.INVOKE)
                                ? expected.toResultValue(resVarName + "[" + i + "]")
                                : expected.toResultValue(resVarName);

                if (expected.type().equals(WasmValueType.V128)) {
                    exprs.add(new NameExpr("var expected = " + resultVar));
                    switch (expected.laneType()) {
                        case I8:
                            exprs.add(
                                    new NameExpr(
                                            "assertArrayEquals(expected," + " vecTo8(results))"));
                            break;
                        case I16:
                            exprs.add(
                                    new NameExpr("assertArrayEquals(expected, vecTo16(results))"));
                            break;
                        case I32:
                            exprs.add(
                                    new NameExpr(
                                            "assertArrayEquals(expected," + " vecTo32(results))"));
                            break;
                        case F32:
                            exprs.add(
                                    new NameExpr(
                                            "assertArrayEquals(expected," + " vecToF32(results))"));
                            break;
                    }

                } else {
                    exprs.add(new NameExpr("assertEquals(" + expectedVar + ", " + resultVar + ")"));
                }
            }

            return exprs;
        } else {
            throw new IllegalArgumentException("Unhandled command type " + cmd.type());
        }
    }

    private List<Expression> generateInvoke(String varName, Command cmd) {
        assert cmd.type() == CommandType.ACTION;

        String invocationMethod;
        if (cmd.action().type() == ActionType.INVOKE) {
            var args =
                    Arrays.stream(cmd.action().args())
                            .map(WasmValue::toArgsValue)
                            .collect(Collectors.joining(", "));
            invocationMethod = ".apply(" + args + ")";
        } else {
            throw new IllegalArgumentException("Unhandled action type " + cmd.action().type());
        }

        var assertDecl =
                new NameExpr(
                        "var exception = assertDoesNotThrow(() -> "
                                + varName
                                + invocationMethod
                                + ")");
        return List.of(assertDecl);
    }

    private static final String TAB = "  ";
    private static final String INDENT = TAB + TAB + TAB + TAB + TAB;

    private static NameExpr generateModuleInstantiation(String wasmFile, boolean excludeInvalid) {
        return new NameExpr(
                "TestModule.of(\""
                        + wasmFile
                        + "\")\n"
                        + ((excludeInvalid) ? INDENT + ".withTypeValidation(false)\n" : "")
                        + INDENT
                        + ".instantiate(store)");
    }

    private static NameExpr generateRegisterInstance(String name, String instance) {
        return new NameExpr("store.register(\"" + name + "\", " + instance + ")");
    }

    private String getWasmFile(Command cmd, String wasmClasspath) {
        return wasmClasspath + "/" + cmd.filename();
    }

    private void generateAssertThrows(
            String wasmClasspath,
            Command cmd,
            MethodDeclaration method,
            boolean excluded,
            String exceptionType) {

        String wasmFile = getWasmFile(cmd, wasmClasspath);

        var assignementStmt = (cmd.text() != null) ? "var exception = " : "";

        var assertThrows =
                new NameExpr(
                        assignementStmt
                                + "assertThrows("
                                + exceptionType
                                + ".class, () -> "
                                + generateModuleInstantiation(wasmFile, false)
                                + ")");

        method.getBody().get().addStatement(assertThrows);
        if (cmd.text() != null) {
            method.getBody().get().addStatement(exceptionMessageMatch(cmd.text()));
        }

        if (excluded) {
            method.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("Disabled"), new StringLiteralExpr("Test excluded")));
        }
    }

    private Expression exceptionMessageMatch(String text) {
        return new NameExpr(
                "assertTrue(exception.getMessage().contains(\""
                        + text
                        + "\"), \"'\" + exception.getMessage() + \"' doesn't contain: '"
                        + text
                        + "\")");
    }
}
