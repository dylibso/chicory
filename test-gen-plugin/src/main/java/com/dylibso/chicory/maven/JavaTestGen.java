package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.StringUtils.capitalize;
import static com.dylibso.chicory.maven.StringUtils.escapedCamelCase;
import static com.dylibso.chicory.maven.wast.ActionType.INVOKE;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.WasmValue;
import com.dylibso.chicory.maven.wast.Wast;
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
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.StringEscapeUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final List<String> excludedMalformedWasts;

    private final List<String> excludedInvalidWasts;

    public JavaTestGen(
            Log log,
            File baseDir,
            File sourceTargetFolder,
            List<String> excludedTests,
            List<String> excludedMalformedWasts,
            List<String> excludedInvalidWasts) {
        this.log = log;
        this.baseDir = baseDir;
        this.sourceTargetFolder = sourceTargetFolder;
        this.excludedTests = excludedTests;
        this.excludedMalformedWasts = excludedMalformedWasts;
        this.excludedInvalidWasts = excludedInvalidWasts;
    }

    public CompilationUnit generate(
            String name, Wast wast, File wasmFilesFolder, SourceRoot importsSourceRoot) {
        var cu = new CompilationUnit("com.dylibso.chicory.test.gen");
        var testName = "SpecV1" + capitalize(escapedCamelCase(name)) + "Test";
        var importsName = "SpecV1" + capitalize(escapedCamelCase(name)) + "HostFuncs";
        cu.setStorage(sourceTargetFolder.toPath().resolve(testName + ".java"));

        // all the imports
        // junit imports
        cu.addImport("java.io.File");
        cu.addImport("org.junit.jupiter.api.Disabled");
        cu.addImport("org.junit.jupiter.api.Test");
        cu.addImport("org.junit.jupiter.api.MethodOrderer");
        cu.addImport("org.junit.jupiter.api.TestMethodOrder");
        cu.addImport("org.junit.jupiter.api.Order");
        cu.addImport("org.junit.jupiter.api.TestInstance");
        cu.addImport("org.junit.jupiter.api.Assertions.assertEquals", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertTrue", true, false);
        cu.addImport("org.junit.jupiter.api.Assertions.assertDoesNotThrow", true, false);

        // testing imports
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

        boolean excludeMalformed = excludedMalformedWasts.contains(name + ".wast");
        boolean excludeInvalid = excludedInvalidWasts.contains(name + ".wast");

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

                    instantiateMethod.setBody(
                            new BlockStmt()
                                    .addStatement(
                                            new AssignExpr(
                                                    new NameExpr(lastInstanceVarName),
                                                    generateModuleInstantiation(
                                                            cmd,
                                                            currentWasmFile,
                                                            importsName,
                                                            hostFuncs,
                                                            excludeInvalid),
                                                    AssignExpr.Operator.ASSIGN)));
                    break;
                case ACTION:
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                    method = createTestMethod(testClass, testNumber++, excludedMethods);

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
                case REGISTER:
                    // should be irrelevant
                    break;
                case ASSERT_MALFORMED:
                    method = createTestMethod(testClass, testNumber++, excludedMethods);
                    generateAssertThrows(wasmFilesFolder, cmd, method, excludeMalformed);
                    break;
                case ASSERT_INVALID:
                    method = createTestMethod(testClass, testNumber++, excludedMethods);
                    generateAssertThrows(wasmFilesFolder, cmd, method, excludeInvalid);
                    break;
                case ASSERT_UNINSTANTIABLE:
                case ASSERT_EXHAUSTION:
                case ASSERT_UNLINKABLE:
                    testNumber++;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "command type not yet supported " + cmd.type());
            }
        }

        return cu;
    }

    private MethodDeclaration createTestMethod(
            ClassOrInterfaceDeclaration testClass, int testNumber, List<String> excludedTests) {
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

    private static final String TAB = "  ";
    private static final String INDENT = TAB + TAB + TAB + TAB + TAB;

    private static NameExpr generateModuleInstantiation(
            Command cmd,
            String wasmFile,
            String importsName,
            String hostFuncs,
            boolean excludeInvalid) {
        var additionalParam =
                cmd.moduleType() == null ? "" : ", ModuleType." + cmd.moduleType().toUpperCase();
        return new NameExpr(
                "TestModule.of(\n"
                        + INDENT
                        + TAB
                        + "new File(\""
                        + wasmFile
                        + "\")"
                        + additionalParam
                        + ")\n"
                        + INDENT
                        + ".build()\n"
                        + ((excludeInvalid) ? "" : INDENT + ".withTypeValidation(true)\n")
                        + ((hostFuncs != null)
                                ? INDENT
                                        + ".withHostImports("
                                        + importsName
                                        + "."
                                        + hostFuncs
                                        + "())"
                                : "")
                        + INDENT
                        + ".instantiate()\n"
                        + INDENT
                        + ".instance()\n"
                        + INDENT
                        + ".initialize(true)"); // TODO: verify start = true
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
                                + generateModuleInstantiation(cmd, wasmFile, null, null, false)
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
                        + "\"), \"'\" + exception.getMessage() + \"' doesn't contains: '"
                        + text
                        + "\")");
    }
}
