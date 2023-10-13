package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.StringUtils.*;
import static com.dylibso.chicory.maven.wast.ActionType.INVOKE;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.WasmValue;
import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.StringEscapeUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.plugin.logging.Log;

public class JavaTestGen {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Log log;
    private final File baseDir;
    private final File sourceTargetFolder;
    private final List<String> excludedTests;

    public JavaTestGen(Log log, File baseDir, File sourceTargetFolder, List<String> excludedTests) {
        this.log = log;
        this.baseDir = baseDir;
        this.sourceTargetFolder = sourceTargetFolder;
        this.excludedTests = excludedTests;
    }

    private static final String INSTANCE_NAME = "instance";

    public CompilationUnit generate(
            SourceRoot dest, File specFile, File wasmFilesFolder, boolean ordered) {
        Wast wast;
        try {
            wast = mapper.readValue(specFile, Wast.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var cu = new CompilationUnit("com.dylibso.chicory.test.gen");
        var name = specFile.toPath().getParent().toFile().getName();
        var testName = "SpecV1" + capitalize(escapedCamelCase(name)) + "Test";
        cu.setStorage(sourceTargetFolder.toPath().resolve(testName + ".java"));

        // all the imports
        // junit imports
        cu.addImport("org.junit.jupiter.api.Disabled");
        cu.addImport("org.junit.jupiter.api.Test");
        if (ordered) {
            cu.addImport("org.junit.jupiter.api.MethodOrderer");
            cu.addImport("org.junit.jupiter.api.TestMethodOrder");
            cu.addImport("org.junit.jupiter.api.Order");
        }
        cu.addImport("org.junit.jupiter.api.Assertions.assertEquals", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertTrue", true, false);

        // runtime imports
        cu.addImport("com.dylibso.chicory.runtime.exceptions.WASMRuntimeException");
        cu.addImport("com.dylibso.chicory.runtime.ExportFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Module");
        cu.addImport("com.dylibso.chicory.runtime.ModuleType");

        // base imports
        cu.addImport("com.dylibso.chicory.wasm.exceptions.InvalidException");
        cu.addImport("com.dylibso.chicory.wasm.exceptions.MalformedException");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");

        var testClass = cu.addClass(testName);
        if (ordered) {
            testClass.addSingleMemberAnnotation(
                    "TestMethodOrder",
                    new ClassExpr(new ClassOrInterfaceType("MethodOrderer.OrderAnnotation")));
        }

        MethodDeclaration method;
        int testNumber = 0;
        int moduleInstantiationNumber = 0;
        int fallbackVarNumber = 0;
        for (var cmd : wast.getCommands()) {
            var excludedMethods =
                    excludedTests.stream()
                            .filter(t -> t.startsWith(testName))
                            .map(t -> t.replace(testName + ".", ""))
                            .collect(Collectors.toList());

            switch (cmd.getType()) {
                case MODULE:
                    testClass.addFieldWithInitializer(
                            parseClassOrInterfaceType("Instance"),
                            INSTANCE_NAME + moduleInstantiationNumber++,
                            generateModuleInstantiation(cmd, wasmFilesFolder));
                    break;
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                    method = createTestMethod(testClass, testNumber++, excludedMethods, ordered);

                    var baseVarName = escapedCamelCase(cmd.getAction().getField());
                    var varNum = fallbackVarNumber++;
                    var varName = "var" + ((baseVarName.length() == 0) ? varNum : baseVarName);
                    var fieldExport =
                            generateFieldExport(varName, cmd, (moduleInstantiationNumber - 1));
                    if (fieldExport.isPresent()) {
                        method.getBody().get().addStatement(fieldExport.get());
                    }

                    for (var expr : generateAssert(varName, cmd)) {
                        method.getBody().get().addStatement(expr);
                    }
                    break;
                case ASSERT_INVALID:
                case ASSERT_MALFORMED:
                    testNumber++;
                    //                    method = createTestMethod(testClass, testNumber++,
                    // excludedMethods);
                    //
                    //                    for (var expr : generateAssertThrows(cmd,
                    // wasmFilesFolder)) {
                    //                        method.getBody().get().addStatement(expr);
                    //                    }
                    break;
                default:
                    log.info("command type not yet supported " + cmd.getType());
                    //                    throw new IllegalArgumentException(
                    //                            "command type not yet supported " +
                    // cmd.getType());
            }
        }

        return cu;
    }

    private MethodDeclaration createTestMethod(
            ClassOrInterfaceDeclaration testClass,
            int testNumber,
            List<String> excludedTests,
            boolean ordered) {
        var methodName = "test" + testNumber;
        var method = testClass.addMethod("test" + testNumber, Modifier.Keyword.PUBLIC);
        if (excludedTests.contains(methodName)) {
            method.addAnnotation("Disabled");
        }
        method.addAnnotation("Test");
        if (ordered) {
            method.addSingleMemberAnnotation("Order", new IntegerLiteralExpr(testNumber));
        }

        return method;
    }

    private Optional<Expression> generateFieldExport(
            String varName, Command cmd, int instanceNumber) {
        if (cmd.getAction() != null && cmd.getAction().getField() != null) {
            var declarator =
                    new VariableDeclarator()
                            .setName(varName)
                            .setType(parseClassOrInterfaceType("ExportFunction"))
                            .setInitializer(
                                    new NameExpr(
                                            INSTANCE_NAME
                                                    + instanceNumber
                                                    + ".getExport(\""
                                                    + StringEscapeUtils.escapeJava(
                                                            cmd.getAction().getField())
                                                    + "\")"));
            Expression varDecl = new VariableDeclarationExpr(declarator);
            return Optional.of(varDecl);
        } else {
            return Optional.empty();
        }
    }

    private List<Expression> generateAssert(String varName, Command cmd) {
        assert (cmd.getType() == CommandType.ASSERT_RETURN
                || cmd.getType() == CommandType.ASSERT_TRAP);

        var returnVar = "null";
        var typeConversion = "";
        var deltaParam = "";
        if (cmd.getExpected() != null && cmd.getExpected().length > 0) {
            if (cmd.getExpected().length == 1) {
                var expected = cmd.getExpected()[0];
                returnVar = expected.toJavaValue();
                typeConversion = expected.extractType();
                deltaParam = expected.getDelta();
            } else {
                throw new RuntimeException("Multiple expected return, implement me!");
            }
        }

        String invocationMethod;
        if (cmd.getAction().getType() == INVOKE) {
            var args =
                    Arrays.stream(cmd.getAction().getArgs())
                            .map(WasmValue::toWasmValue)
                            .collect(Collectors.joining(", "));
            invocationMethod = ".apply(" + args + ")";
        } else {
            throw new IllegalArgumentException(
                    "Unhandled action type " + cmd.getAction().getType());
        }

        switch (cmd.getType()) {
            case ASSERT_RETURN:
                return List.of(
                        new NameExpr(
                                "assertEquals("
                                        + returnVar
                                        + ", "
                                        + varName
                                        + invocationMethod
                                        + typeConversion
                                        + deltaParam
                                        + ")"));
            case ASSERT_TRAP:
                var assertDecl =
                        new NameExpr(
                                "var exception = assertThrows(WASMRuntimeException.class, () -> "
                                        + varName
                                        + invocationMethod
                                        + typeConversion
                                        + ")");
                if (cmd.getText() != null) {
                    return List.of(assertDecl, exceptionMessageMatch(cmd.getText()));
                } else {
                    return List.of(assertDecl);
                }
        }

        throw new RuntimeException("Unreachable");
    }

    private Expression generateModuleInstantiation(Command cmd, File folder) {
        assert (cmd.getType() == CommandType.MODULE);

        var relativeFile =
                folder.toPath()
                        .resolve(cmd.getFilename())
                        .toFile()
                        .getAbsolutePath()
                        .replace(baseDir.getAbsolutePath() + File.separator, "")
                        .replace("\\", "\\\\"); // Win compat

        var additionalParam = "";
        if (cmd.getModuleType() != null) {
            additionalParam = ", ModuleType." + cmd.getModuleType().toUpperCase();
        }
        return new NameExpr(
                "Module.build(\"" + relativeFile + "\"" + additionalParam + ").instantiate()");
    }

    private List<Expression> generateAssertThrows(Command cmd, File wasmFilesFolder) {
        assert (cmd.getType() == CommandType.ASSERT_INVALID
                || cmd.getType() == CommandType.ASSERT_MALFORMED);

        var exceptionType = "";
        if (cmd.getType() == CommandType.ASSERT_INVALID) {
            exceptionType = "InvalidException";
        } else if (cmd.getType() == CommandType.ASSERT_MALFORMED) {
            exceptionType = "MalformedException";
        }

        var assignementStmt = (cmd.getText() != null) ? "var exception = " : "";

        var assertThrows =
                new NameExpr(
                        assignementStmt
                                + "assertThrows("
                                + exceptionType
                                + ".class, () -> "
                                + generateModuleInstantiation(cmd, wasmFilesFolder)
                                + ")");

        if (cmd.getText() != null) {
            return List.of(assertThrows, exceptionMessageMatch(cmd.getText()));
        } else {
            return List.of(assertThrows);
        }
    }

    private Expression exceptionMessageMatch(String text) {
        return new NameExpr(
                "assertTrue(exception.getMessage().contains(\""
                        + text
                        + "\"), \"'\" + exception.getMessage() + \"' doesn't contains: '"
                        + text
                        + "\")");
    }
}
