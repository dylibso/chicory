package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.StringUtils.*;
import static com.dylibso.chicory.maven.wast.ActionType.INVOKE;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.WasmValue;
import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.StringEscapeUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.plugin.logging.Log;

public class JavaTestGen {

    private static final String TEST_MODULE_NAME = "testModule";

    private static final JavaParser JAVA_PARSER = new JavaParser();

    private final ObjectMapper mapper;

    private final Log log;

    private final File baseDir;

    private final File sourceTargetFolder;

    private final List<String> excludedTests;

    public JavaTestGen(Log log, File baseDir, File sourceTargetFolder, List<String> excludedTests) {
        this.log = log;
        this.baseDir = baseDir;
        this.sourceTargetFolder = sourceTargetFolder;
        this.excludedTests = excludedTests;
        this.mapper = new ObjectMapper();
    }

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
        cu.addImport("java.io.File");
        cu.addImport("org.junit.jupiter.api.Disabled");
        cu.addImport("org.junit.jupiter.api.Tag");
        cu.addImport("org.junit.jupiter.api.Test");
        if (ordered) {
            cu.addImport("org.junit.jupiter.api.MethodOrderer");
            cu.addImport("org.junit.jupiter.api.TestMethodOrder");
            cu.addImport("org.junit.jupiter.api.Order");
            cu.addImport("org.junit.jupiter.api.TestInstance");
        }
        cu.addImport("org.junit.jupiter.api.Assertions.assertEquals", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertTrue", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertDoesNotThrow", true, false);

        // testing imports
        cu.addImport("com.dylibso.chicory.testing.ChicoryTest");
        cu.addImport("com.dylibso.chicory.testing.TestModule");

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
                    new ClassExpr(
                            JAVA_PARSER
                                    .parseClassOrInterfaceType("MethodOrderer.OrderAnnotation")
                                    .getResult()
                                    .get()));
            testClass.addSingleMemberAnnotation(
                    "TestInstance",
                    new FieldAccessExpr(
                            new FieldAccessExpr(new NameExpr("TestInstance"), "Lifecycle"),
                            "PER_CLASS"));
        }

        testClass.addAnnotation("ChicoryTest");

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
                            parseClassOrInterfaceType("TestModule"),
                            TEST_MODULE_NAME + moduleInstantiationNumber++,
                            generateModuleInstantiation(cmd, wasmFilesFolder));
                    break;
                case ACTION:
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                    method =
                            createTestMethod(
                                    testClass, testNumber++, excludedMethods, ordered, cmd);

                    var baseVarName = escapedCamelCase(cmd.getAction().getField());
                    var varNum = fallbackVarNumber++;
                    var varName = "var" + (baseVarName.isEmpty() ? varNum : baseVarName);
                    var fieldExport =
                            generateFieldExport(varName, cmd, (moduleInstantiationNumber - 1));
                    if (fieldExport.isPresent()) {
                        method.getBody().get().addStatement(fieldExport.get());
                    }

                    if (cmd.getType() == CommandType.ACTION) {
                        for (var expr : generateInvoke(varName, cmd)) {
                            method.getBody().get().addStatement(expr);
                        }
                    } else {
                        for (var expr : generateAssert(varName, cmd)) {
                            method.getBody().get().addStatement(expr);
                        }
                    }
                    break;
                case ASSERT_INVALID:
                case ASSERT_MALFORMED:
                case ASSERT_UNINSTANTIABLE:
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
                    // TODO we need to implement all of these
                    log.info("TODO: command type not yet supported " + cmd.getType());
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
            boolean ordered,
            Command cmd) {
        var methodName = "test" + testNumber;
        var method = testClass.addMethod("test" + testNumber, Modifier.Keyword.PUBLIC);
        if (excludedTests.contains(methodName)) {
            method.addAnnotation("Disabled");
        }
        method.addAnnotation("Test");
        if (ordered) {
            method.addSingleMemberAnnotation(
                    "Order", new IntegerLiteralExpr(Integer.toString(testNumber)));
        }

        // generate Tag annotation with exported symbol as reference
        switch (cmd.getType()) {
            case ACTION:
            case ASSERT_RETURN:
            case ASSERT_TRAP:
                {
                    // some characters that are allowed in wasm symbol names are not allowed in the
                    // Tag annotation, thus we use base64 encoding.
                    String export = cmd.getAction().getField();
                    String base64EncodedExport =
                            Base64.getEncoder()
                                    .encodeToString(export.getBytes(StandardCharsets.UTF_8));
                    method.addSingleMemberAnnotation(
                            "Tag", new StringLiteralExpr("export=" + base64EncodedExport));
                }

                break;
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
                                            TEST_MODULE_NAME
                                                    + instanceNumber
                                                    + ".getInstance().getExport(\""
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
        assert (cmd.getExpected() != null);
        assert (cmd.getExpected().length > 0);
        assert (cmd.getAction().getType() == INVOKE);

        var args =
                Arrays.stream(cmd.getAction().getArgs())
                        .map(WasmValue::toWasmValue)
                        .collect(Collectors.joining(", "));

        var invocationMethod = ".apply(" + args + ")";
        if (cmd.getType() == CommandType.ASSERT_TRAP) {
            var assertDecl =
                    new NameExpr(
                            "var exception ="
                                    + " assertThrows(WASMRuntimeException.class, () -> "
                                    + varName
                                    + invocationMethod
                                    + ")");
            if (cmd.getText() != null) {
                return List.of(assertDecl, exceptionMessageMatch(cmd.getText()));
            } else {
                return List.of(assertDecl);
            }
        } else if (cmd.getType() == CommandType.ASSERT_RETURN) {
            List<Expression> exprs = new ArrayList<>();
            exprs.add(new NameExpr("var results = " + varName + ".apply(" + args + ")"));

            for (int i = 0; i < cmd.getExpected().length; i++) {
                var expected = cmd.getExpected()[i];
                var returnVar = expected.toJavaValue();
                var typeConversion = expected.extractType();
                var deltaParam = expected.getDelta();
                exprs.add(
                        new NameExpr(
                                "assertEquals("
                                        + returnVar
                                        + ", results["
                                        + i
                                        + "]"
                                        + typeConversion
                                        + deltaParam
                                        + ")"));
            }

            return exprs;
        } else {
            throw new IllegalArgumentException("Unhandled command type " + cmd.getType());
        }
    }

    private List<Expression> generateInvoke(String varName, Command cmd) {
        assert cmd.getType() == CommandType.ACTION;

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

        var assertDecl =
                new NameExpr(
                        "var exception = assertDoesNotThrow(() -> "
                                + varName
                                + invocationMethod
                                + ")");
        return List.of(assertDecl);
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
                "TestModule.of(new File(\""
                        + relativeFile
                        + "\")"
                        + additionalParam
                        + ").build().instantiate()");
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
