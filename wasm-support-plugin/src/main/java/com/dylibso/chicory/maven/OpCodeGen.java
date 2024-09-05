package com.dylibso.chicory.maven;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generate the OpCodes.java file from a tsv
 */
public class OpCodeGen {

    @SuppressWarnings("StringSplitter")
    public static void generate(
        File instructionsFile,
        File sourceDestinationFolder
    ) throws IOException {
        sourceDestinationFolder.mkdirs();
        List<String[]> lines;
        try (Stream<String> stream = Files.lines(instructionsFile.toPath())) {
            lines = stream.map(line -> line.split("\t")).collect(Collectors.toList());
        } catch (IOException e) {
            throw e;
        }

        var cu = new CompilationUnit("com.dylibso.chicory.wasm.types");
        var destFile =
                Path.of(
                        sourceDestinationFolder.getAbsolutePath(),
                        "com",
                        "dylibso",
                        "chicory",
                        "wasm",
                        "types",
                        "OpCode.java");
        cu.setStorage(destFile);

        cu.addImport("java.util.HashMap");
        cu.addImport("java.util.Map");

        var enumDef = cu.addEnum("OpCode", Modifier.Keyword.PUBLIC);

        List<NameExpr> staticAssignement = new ArrayList<>();

        for (var line : lines) {
            var enumConstantDecl = new EnumConstantDeclaration();
            var originalName = line[0].split(" ")[0];
            var enumName = originalName.toUpperCase(Locale.ROOT).replace('.', '_');
            var value = line[1].trim().split(" ")[0];
            var hexValue = value.replace("$", "0x");

            enumConstantDecl.setName(enumName);
            enumConstantDecl.setArguments(new NodeList<>(new IntegerLiteralExpr(hexValue)));

            enumDef.addOrphanComment(new LineComment(line[0]));
            enumDef.addEntry(enumConstantDecl);

            var staticAssignmentParams =
                    Arrays.stream(line[0].split(" "))
                            .skip(1)
                            .map(a -> getType(a))
                            .collect(Collectors.joining(", "));
            staticAssignement.add(
                    new NameExpr(
                            "signature.put("
                                    + enumName
                                    + ", new WasmEncoding[] {"
                                    + staticAssignmentParams
                                    + "})"));
        }

        var opcodeField =
                enumDef.addField("int", "opcode", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        var constructor = enumDef.addConstructor();
        constructor.addParameter("int", "opcode");
        constructor.setBody(
                new BlockStmt()
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("this.opcode"),
                                        new NameExpr("opcode"),
                                        AssignExpr.Operator.ASSIGN)));
        opcodeField.createGetter().setName("opcode");

        enumDef.addFieldWithInitializer(
                "Map<Integer, OpCode>",
                "byOpCode",
                new NameExpr("new HashMap<>()"),
                Modifier.Keyword.PRIVATE,
                Modifier.Keyword.STATIC,
                Modifier.Keyword.FINAL);

        var byOpCode =
                enumDef.addMethod("byOpCode", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        byOpCode.setType("OpCode");
        byOpCode.addParameter("int", "opcode");
        byOpCode.setBody(
                new BlockStmt().addStatement(new ReturnStmt(new NameExpr("byOpCode.get(opcode)"))));

        enumDef.addFieldWithInitializer(
                "Map<OpCode, WasmEncoding[]>",
                "signature",
                new NameExpr("new HashMap<>()"),
                Modifier.Keyword.PRIVATE,
                Modifier.Keyword.STATIC,
                Modifier.Keyword.FINAL);

        var getSignature =
                enumDef.addMethod("getSignature", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        getSignature.setType("WasmEncoding[]");
        getSignature.addParameter("OpCode", "o");
        getSignature.setBody(
                new BlockStmt().addStatement(new ReturnStmt(new NameExpr("signature.get(o)"))));

        var staticBlock = enumDef.addStaticInitializer();

        // byOpCode initialization
        staticBlock.addStatement(
                new NameExpr(
                        "for (OpCode e: OpCode.values()) {" + " byOpCode.put(e.opcode(), e); }"));
        for (var assign : staticAssignement) {
            staticBlock.addStatement(assign);
        }

        final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());
        dest.add(cu);
        dest.saveAll();
    }

    private static String getType(String in) {
        switch (in) {
            case "<varuint>":
                return "WasmEncoding.VARUINT";
            case "<varsint32>":
                return "WasmEncoding.VARSINT32";
            case "<varsint64>":
                return "WasmEncoding.VARSINT64";
            case "<float32>":
                return "WasmEncoding.FLOAT32";
            case "<float64>":
                return "WasmEncoding.FLOAT64";
            case "vec(<varuint>)":
                return "WasmEncoding.VEC_VARUINT";
            default:
                throw new IllegalArgumentException("Unknown param: " + in);
        }
    }
}
