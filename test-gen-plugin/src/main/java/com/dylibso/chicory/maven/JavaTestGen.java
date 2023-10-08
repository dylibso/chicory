package com.dylibso.chicory.maven;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.SourceRoot;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dylibso.chicory.maven.StringUtils.*;

public class JavaTestGen {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Log log;
    private final File baseDir;
    private final File sourceTargetFolder;

    public JavaTestGen(Log log, File baseDir, File sourceTargetFolder) {
        this.log = log;
        this.baseDir = baseDir;
        this.sourceTargetFolder = sourceTargetFolder;
    }

    private String INSTANCE_NAME = "instance";

    public void generate(File specFile, File wasmFilesFolder) {
        Wast wast;
        try {
            wast = mapper.readValue(specFile, Wast.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final SourceRoot dest = new SourceRoot(sourceTargetFolder.toPath());
        var cu = new CompilationUnit("com.dylibso.chicory.test.gen");
        var name = specFile.toPath().getParent().toFile().getName();
        var testName = "SpecV1" + capitalize(escapedCamelCase(name)) + "Test";
        cu.setStorage(sourceTargetFolder.toPath().resolve(testName + ".java"));

        // all the imports
        cu.addImport("org.junit.Test");

        cu.addImport("com.dylibso.chicory.runtime.exceptions.InvalidException");
        cu.addImport("com.dylibso.chicory.runtime.exceptions.MalformedException");
        cu.addImport("com.dylibso.chicory.runtime.exceptions.WASMRuntimeException");
        cu.addImport("com.dylibso.chicory.runtime.ExportFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Module");
        cu.addImport("com.dylibso.chicory.runtime.ModuleType");

        cu.addImport("com.dylibso.chicory.wasm.types.Value");

        cu.addImport("org.junit.Assert.assertEquals", true, false);
        cu.addImport("org.junit.Assert.assertThrows", true, false);

        var testClass = cu.addClass(testName);

        MethodDeclaration method = null;
        int testNumber = 0;
        int moduleInstantiationNumber = 0;
        for (var cmd: wast.getCommands()) {
            switch (cmd.getType()) {
                case MODULE:
                    testClass.addFieldWithInitializer(
                            new ClassOrInterfaceType("Instance"),
                            INSTANCE_NAME + moduleInstantiationNumber++,
                            generateModuleInstantiation(cmd, wasmFilesFolder));
                    break;
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                    method = testClass.addMethod("test" + testNumber++, Modifier.Keyword.PUBLIC);
                    method.addAnnotation("Test");

                    var varName = escapedCamelCase(cmd.getAction().getField());
                    var fieldExport = generateFieldExport(varName, cmd, (moduleInstantiationNumber - 1));
                    if (fieldExport.isPresent()) {
                        method.getBody().get().addStatement(fieldExport.get());
                    }

                    for (var expr: generateAssert(varName, cmd)) {
                        method.getBody().get().addStatement(expr);
                    }
                    break;
                case ASSERT_INVALID:
                case ASSERT_MALFORMED:
                    method = testClass.addMethod("test" + testNumber++, Modifier.Keyword.PUBLIC);
                    method.addAnnotation("Test");

                    for (var expr: generateAssertThrows(cmd, wasmFilesFolder)) {
                        method.getBody().get().addStatement(expr);
                    }
                    break;
            }
        }

        dest.add(cu);
        dest.saveAll();
    }

    private Optional<Expression> generateFieldExport(String varName, Command cmd, int instanceNumber) {
        if (cmd.getAction() != null && cmd.getAction().getField() != null) {
            var declarator = new VariableDeclarator()
                    .setName(varName)
                    .setType(new ClassOrInterfaceType("ExportFunction"))
                    .setInitializer(new NameExpr(INSTANCE_NAME + instanceNumber + ".getExport(\"" + cmd.getAction().getField() + "\")"));
            Expression varDecl = new VariableDeclarationExpr(declarator);
            return Optional.of(varDecl);
        } else {
            return Optional.empty();
        }
    }

    private List<Expression> generateAssert(String varName, Command cmd) {
        assert(cmd.getType() == CommandType.ASSERT_RETURN || cmd.getType() == CommandType.ASSERT_TRAP);

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

        String invocationMethod = null;
        switch (cmd.getAction().getType()) {
            case INVOKE:
                var args = Arrays.stream(cmd.getAction().getArgs()).map(arg -> arg.toWasmValue()).collect(Collectors.joining(", "));
                invocationMethod = ".apply(" + args + ")";
                break;
        }

        switch (cmd.getType()) {
            case ASSERT_RETURN:
                return List.of(new NameExpr("assertEquals(" + returnVar + ", "+ varName + invocationMethod + typeConversion + deltaParam + ")"));
            case ASSERT_TRAP:
                var assertDecl = new NameExpr("var exception = assertThrows(WASMRuntimeException.class, () -> "+ varName + invocationMethod + typeConversion + ")");
                if (cmd.getText() != null) {
                    var messageMatch = new NameExpr("assertEquals(\"" + cmd.getText() + "\", exception.getMessage())");
                    return List.of(assertDecl, messageMatch);
                } else {
                    return List.of(assertDecl);
                }
        }

        throw new RuntimeException("Unreachable");
    }

    private Expression generateModuleInstantiation(Command cmd, File folder) {
        assert(cmd.getType() == CommandType.MODULE);

        var relativeFile = folder.toPath().resolve(cmd.getFilename()).toFile().getAbsolutePath()
                .replaceFirst(baseDir.getAbsolutePath() + "/", "");

        var additionalParam = "";
        if (cmd.getModuleType() != null) {
            additionalParam = ", ModuleType." + cmd.getModuleType().toUpperCase();
        }
        return new NameExpr("Module.build(\"" + relativeFile + "\"" + additionalParam + ").instantiate()");
    }

    private List<Expression> generateAssertThrows(Command cmd, File wasmFilesFolder) {
        assert(cmd.getType() == CommandType.ASSERT_INVALID || cmd.getType() == CommandType.ASSERT_MALFORMED);

        var exceptionType = "";
        if (cmd.getType() == CommandType.ASSERT_INVALID) {
            exceptionType = "InvalidException";
        } else if (cmd.getType() == CommandType.ASSERT_MALFORMED) {
            exceptionType = "MalformedException";
        }

        var assignementStmt = (cmd.getText() != null) ? "var exception = " : "";

        var assertThrows = new NameExpr(assignementStmt + "assertThrows(" + exceptionType + ".class, () -> " + generateModuleInstantiation(cmd, wasmFilesFolder) + ")");

        if (cmd.getText() != null) {
            var messageMatch = new NameExpr("assertEquals(\"" + cmd.getText() + "\", exception.getMessage())");
            return List.of(assertThrows, messageMatch);
        } else {
            return List.of(assertThrows);
        }
    }
}
