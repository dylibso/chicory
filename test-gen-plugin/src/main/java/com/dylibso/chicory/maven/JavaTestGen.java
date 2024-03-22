package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.StringUtils.*;
import static com.dylibso.chicory.maven.wast.ActionType.INVOKE;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.WasmValue;
import com.dylibso.chicory.maven.wast.WasmValueType;
import com.dylibso.chicory.maven.wast.Wast;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.StringEscapeUtils;
import java.io.File;
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

    private final Log log;

    private final File baseDir;

    private final File sourceTargetFolder;

    private final List<String> excludedTests;

    private final List<String> excludedValidationTests;

    public JavaTestGen(
            Log log,
            File baseDir,
            File sourceTargetFolder,
            List<String> excludedTests,
            List<String> excludedValidationTests) {
        this.log = log;
        this.baseDir = baseDir;
        this.sourceTargetFolder = sourceTargetFolder;
        this.excludedTests = excludedTests;
        this.excludedValidationTests = excludedValidationTests;
    }

    public CompilationUnit generate(
            String name,
            Wast wast,
            File wasmFilesFolder,
            boolean ordered,
            SourceRoot importsSourceRoot) {
        var cu = new CompilationUnit("com.dylibso.chicory.test.gen");
        var testName = "SpecV1" + capitalize(escapedCamelCase(name)) + "Test";
        var importsName = "SpecV1" + capitalize(escapedCamelCase(name)) + "HostFuncs";
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
        cu.addImport("org.junit.jupiter.api.Assertions.assertArrayEquals", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertTrue", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertDoesNotThrow", true, false);

        // testing imports
        cu.addImport("com.dylibso.chicory.testing.ChicoryTest");
        cu.addImport("com.dylibso.chicory.testing.TestModule");

        // runtime imports
        cu.addImport("com.dylibso.chicory.wasm.exceptions.ChicoryException");
        cu.addImport("com.dylibso.chicory.runtime.ExportFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Module");
        cu.addImport("com.dylibso.chicory.runtime.ModuleType");

        // base imports
        cu.addImport("com.dylibso.chicory.wasm.exceptions.InvalidException");
        cu.addImport("com.dylibso.chicory.wasm.exceptions.MalformedException");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");

        // import for host functions
        cu.addImport("com.dylibso.chicory.imports.*");

        var testClass = cu.addClass(testName);
        if (ordered) {
            testClass.addSingleMemberAnnotation(
                    "TestMethodOrder", new NameExpr("MethodOrderer.OrderAnnotation.class"));
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
        String lastModuleVarName = null;
        int fallbackVarNumber = 0;

        var excludedMethods =
                excludedTests.stream()
                        .filter(t -> t.startsWith(testName))
                        .map(t -> t.substring(testName.length() + 1))
                        .collect(Collectors.toList());

        boolean excludeValidation = excludedValidationTests.contains(name + ".wast");

        String currentWasmFile = null;
        for (var cmd : wast.commands()) {

            switch (cmd.type()) {
                case MODULE:
                    currentWasmFile = getWasmFile(cmd, wasmFilesFolder);
                    lastModuleVarName = TEST_MODULE_NAME + moduleInstantiationNumber;
                    String lastInstanceVarName = lastModuleVarName + "Instance";
                    moduleInstantiationNumber++;
                    if (cmd.name() != null) {
                        lastModuleVarName = cmd.name().replace("$", "");
                        lastInstanceVarName = cmd.name().replace("$", "") + "Instance";
                    }
                    String hostFuncs =
                            detectImports(importsName, lastModuleVarName, importsSourceRoot);
                    testClass.addFieldWithInitializer(
                            parseClassOrInterfaceType("TestModule"),
                            lastModuleVarName,
                            generateModuleInstantiation(
                                    cmd, currentWasmFile, importsName, hostFuncs));
                    testClass.addFieldWithInitializer(
                            "Instance",
                            lastInstanceVarName,
                            new NameExpr(lastModuleVarName + ".instance()"));
                    break;
                case ACTION:
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                    method =
                            createTestMethod(
                                    testClass,
                                    testNumber++,
                                    excludedMethods,
                                    ordered,
                                    cmd,
                                    currentWasmFile);

                    var baseVarName = escapedCamelCase(cmd.action().field());
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
                case ASSERT_MALFORMED:
                    method =
                            createTestMethod(
                                    testClass,
                                    testNumber++,
                                    excludedMethods,
                                    ordered,
                                    cmd,
                                    currentWasmFile);
                    generateAssertThrows(wasmFilesFolder, cmd, method, excludeValidation);
                    break;
                case ASSERT_INVALID:
                case ASSERT_UNINSTANTIABLE:
                    testNumber++;
                    break;
                default:
                    // TODO we need to implement all of these
                    log.info("TODO: command type not yet supported " + cmd.type());
                    //                    throw new IllegalArgumentException(
                    //                            "command type not yet supported " +
                    // cmd.getType());
            }
        }

        if (testClass.getMethods().size() == 0) {
            var methodName = "instantiationTest";
            var instantiationMethod = testClass.addMethod(methodName, Modifier.Keyword.PUBLIC);
            instantiationMethod.addAnnotation("Test");
            instantiationMethod.addOrphanComment(
                    new LineComment("Empty test to trigger the class instances creation"));
            instantiationMethod.setBody(
                    new BlockStmt().addStatement(new NameExpr("assertTrue(true)")));
        }

        return cu;
    }

    private MethodDeclaration createTestMethod(
            ClassOrInterfaceDeclaration testClass,
            int testNumber,
            List<String> excludedTests,
            boolean ordered,
            Command cmd,
            String currentWasmFile) {
        var methodName = "test" + testNumber;
        var method = testClass.addMethod(methodName, Modifier.Keyword.PUBLIC);
        if (excludedTests.contains(methodName)) {
            method.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("Disabled"), new StringLiteralExpr("Test excluded")));
        }
        method.addAnnotation("Test");
        if (ordered) {
            method.addSingleMemberAnnotation(
                    "Order", new IntegerLiteralExpr(Integer.toString(testNumber)));
        }

        // generate Tag annotation with exported symbol as reference
        switch (cmd.type()) {
            case ACTION:
            case ASSERT_RETURN:
            case ASSERT_TRAP:
                {
                    // some characters that are allowed in wasm symbol names are not allowed in the
                    // Tag annotation, thus we use base64 encoding.
                    String export = cmd.action().field();
                    String base64EncodedExport =
                            Base64.getEncoder()
                                    .encodeToString(export.getBytes(StandardCharsets.UTF_8));
                    method.addSingleMemberAnnotation(
                            "Tag", new StringLiteralExpr("export=" + base64EncodedExport));
                    method.addSingleMemberAnnotation(
                            "Tag", new StringLiteralExpr("wasm=" + currentWasmFile));
                }

                break;
        }

        return method;
    }

    private Optional<Expression> generateFieldExport(
            String varName, Command cmd, String moduleName) {
        if (cmd.action() != null && cmd.action().field() != null) {
            var declarator =
                    new VariableDeclarator()
                            .setName(varName)
                            .setType(parseClassOrInterfaceType("ExportFunction"))
                            .setInitializer(
                                    new NameExpr(
                                            moduleName
                                                    + "Instance.export(\""
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
        assert (cmd.type() == CommandType.ASSERT_RETURN || cmd.type() == CommandType.ASSERT_TRAP);
        assert (cmd.expected() != null);
        assert (cmd.expected().length > 0);
        assert (cmd.action().type() == INVOKE);

        var args =
                (cmd.action().args() != null)
                        ? Arrays.stream(cmd.action().args())
                                .map(WasmValue::toWasmValue)
                                .collect(Collectors.joining(", "))
                        : "";

        var invocationMethod = ".apply(" + args + ")";
        if (cmd.type() == CommandType.ASSERT_TRAP) {
            var assertDecl =
                    new NameExpr(
                            "var exception ="
                                    + " assertThrows(ChicoryException.class, () -> "
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
            exprs.add(new NameExpr("var results = " + varName + ".apply(" + args + ")"));

            for (int i = 0; i < cmd.expected().length; i++) {
                var expected = cmd.expected()[i];
                var returnVar = expected.toJavaValue();

                if (expected.type() == WasmValueType.VEC_REF) {
                    exprs.add(new NameExpr("assertArrayEquals(" + returnVar + ", results" + ")"));
                } else {
                    var typeConversion = expected.extractType();
                    var deltaParam = expected.delta();
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
            }

            return exprs;
        } else {
            throw new IllegalArgumentException("Unhandled command type " + cmd.type());
        }
    }

    private List<Expression> generateInvoke(String varName, Command cmd) {
        assert cmd.type() == CommandType.ACTION;

        String invocationMethod;
        if (cmd.action().type() == INVOKE) {
            var args =
                    Arrays.stream(cmd.action().args())
                            .map(WasmValue::toWasmValue)
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

    private static NameExpr generateModuleInstantiation(
            Command cmd, String wasmFile, String importsName, String hostFuncs) {
        var additionalParam =
                cmd.moduleType() == null ? "" : ", ModuleType." + cmd.moduleType().toUpperCase();
        return new NameExpr(
                "TestModule.of(new File(\""
                        + wasmFile
                        + "\")"
                        + additionalParam
                        + ").build().instantiate("
                        + ((hostFuncs != null) ? importsName + "." + hostFuncs + "()" : "")
                        + ")");
    }

    private String detectImports(String importsName, String varName, SourceRoot testSourcesRoot) {
        String hostFuncs = null;
        try {
            var parsed =
                    testSourcesRoot.tryToParse(
                            "com.dylibso.chicory.imports", importsName + ".java");
            if (parsed.isSuccessful()) {
                var methods =
                        parsed.getResult().get().getClassByName(importsName).get().getMethods();

                for (int i = 0; i < methods.size(); i++) {
                    if (methods.get(i).getName().asString().equals(varName)) {
                        hostFuncs = varName;
                    }
                }
                if (hostFuncs == null) {
                    hostFuncs = "fallback";
                }
            }

            if (hostFuncs != null) {
                log.info("Found binding HostFunctions " + importsName + "#" + varName);
            }
        } catch (Exception e) {
            // ignore
        }
        return hostFuncs;
    }

    private String getWasmFile(Command cmd, File folder) {
        return folder.toPath()
                .resolve(cmd.filename())
                .toFile()
                .getAbsolutePath()
                .replace(baseDir.getAbsolutePath() + File.separator, "")
                .replace("\\", "\\\\"); // Win compat
    }

    private void generateAssertThrows(
            File wasmFilesFolder, Command cmd, MethodDeclaration method, boolean excluded) {
        assert (cmd.type() == CommandType.ASSERT_INVALID
                || cmd.type() == CommandType.ASSERT_MALFORMED);

        String wasmFile = getWasmFile(cmd, wasmFilesFolder);

        var exceptionType = "";
        if (cmd.type() == CommandType.ASSERT_INVALID) {
            exceptionType = "InvalidException";
        } else if (cmd.type() == CommandType.ASSERT_MALFORMED) {
            exceptionType = "MalformedException";
        }

        var assignementStmt = (cmd.text() != null) ? "var exception = " : "";

        var assertThrows =
                new NameExpr(
                        assignementStmt
                                + "assertThrows("
                                + exceptionType
                                + ".class, () -> "
                                + generateModuleInstantiation(cmd, wasmFile, null, null)
                                + ")");

        method.getBody().get().addStatement(assertThrows);
        if (cmd.text() != null) {
            method.getBody().get().addStatement(exceptionMessageMatch(cmd.text()));
        }

        if (excluded) {
            method.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("Disabled"),
                            new StringLiteralExpr("Validation test excluded")));
        } else if (cmd.moduleType() != null && cmd.moduleType().equalsIgnoreCase("text")) {
            method.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("Disabled"),
                            new StringLiteralExpr(
                                    "Parsing of textual WASM sources is not implemented yet")));
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
