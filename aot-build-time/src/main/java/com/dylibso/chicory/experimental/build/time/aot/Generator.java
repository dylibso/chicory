package com.dylibso.chicory.experimental.build.time.aot;

import static com.dylibso.chicory.wasm.Encoding.readVarUInt32;
import static com.dylibso.chicory.wasm.WasmWriter.writeVarUInt32;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.compiler.internal.Compiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.WasmWriter;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.RawSection;
import com.dylibso.chicory.wasm.types.SectionId;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Generator {

    private final Config config;

    public Generator(Config config) {
        this.config = config;
    }

    public Set<Integer> generateResources() throws IOException {
        var module = Parser.parse(config.wasmFile());
        var machineName = config.name() + "Machine";
        var compiler =
                Compiler.builder(module)
                        .withClassName(machineName)
                        .withInterpreterFallback(config.interpreterFallback())
                        .withInterpretedFunctions(config.interpretedFunctions())
                        .build();
        var result = compiler.compile();

        var finalFolder = config.targetClassFolder();

        createFolders(finalFolder, config.name().split("\\."));

        for (Map.Entry<String, byte[]> entry : result.classBytes().entrySet()) {
            var binaryName = entry.getKey().replace('.', '/') + ".class";
            var targetFile = config.targetClassFolder().resolve(binaryName);
            Files.write(targetFile, entry.getValue());
        }

        return result.interpretedFunctions();
    }

    public void generateSources() throws IOException {
        var machineName = config.name() + "Machine";
        var split = config.name().split("\\.");

        var finalSourceFolder = config.targetSourceFolder();

        createFolders(finalSourceFolder, split);

        String packageName = config.getPackageName();

        SourceRoot dest = new SourceRoot(finalSourceFolder);

        var baseName = config.getBaseName();
        var moduleName = baseName + "Module";
        var wasmName = baseName + ".meta";

        var cu = new CompilationUnit(packageName);

        var type = cu.addClass(moduleName, Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);

        type.addConstructor(Modifier.Keyword.PRIVATE).createBody();

        generateCreateMethod(cu, type, machineName);
        generateLoadMethod(cu, type, moduleName, wasmName);

        dest.add(packageName, moduleName + ".java", cu);
        dest.saveAll();
    }

    public void generateMetaWasm(Set<Integer> interpretedFunctions) throws IOException {
        byte[] wasmBytes = Files.readAllBytes(config.wasmFile());
        var module = Parser.builder().includeSectionId(SectionId.CODE).build().parse(wasmBytes);

        var writer = new WasmWriter();
        Parser.parseWithoutDecoding(
                wasmBytes,
                section -> {
                    if (section.sectionId() == SectionId.CODE) {

                        var source = ByteBuffer.wrap(((RawSection) section).contents());

                        var out = new ByteArrayOutputStream();
                        var importFuncs = module.importSection().importCount();
                        int count = module.codeSection().functionBodyCount();
                        writeVarUInt32(out, count);
                        var actual = readVarUInt32(source);
                        assert count == actual;
                        for (int i = 0; i < count; i++) {
                            var funcId = importFuncs + i;
                            if (interpretedFunctions.contains(funcId)) {

                                // Copy over the original function body from the source
                                var bodySize = (int) readVarUInt32(source);
                                writeVarUInt32(out, bodySize);
                                var bodyBytes = new byte[bodySize];
                                source.get(bodyBytes);
                                try {
                                    out.write(bodyBytes);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }

                            } else {

                                // Move the source position past the function body
                                var bodySize = (int) readVarUInt32(source);
                                source.position(source.position() + bodySize - 1);
                                var end_op = source.get();
                                assert end_op == OpCode.END.opcode();

                                // Write an empty function body
                                writeVarUInt32(out, 3); // function size in bytes
                                writeVarUInt32(out, 0); // locals count
                                out.write(OpCode.UNREACHABLE.opcode());
                                out.write(OpCode.END.opcode());
                            }
                        }
                        writer.writeSection(SectionId.CODE, out.toByteArray());
                    } else if (section.sectionId() != SectionId.CUSTOM) {
                        writer.writeSection((RawSection) section);
                    }
                });

        var newWasmFile =
                config.targetWasmFolder()
                        .resolve(config.getPackageName().replace('.', '/'))
                        .resolve(config.getBaseName() + ".meta");
        Files.createDirectories(newWasmFile.getParent());
        Files.write(newWasmFile, writer.bytes());
    }

    private static void createFolders(Path filesFolder, String[] split) throws IOException {
        for (int i = 0; i < (split.length - 1); i++) {
            filesFolder = filesFolder.resolve(split[i]);
        }
        Files.createDirectories(filesFolder);
    }

    private static void generateCreateMethod(
            CompilationUnit cu, ClassOrInterfaceDeclaration type, String machineName) {

        cu.addImport(Instance.class);
        cu.addImport(Machine.class);

        var method =
                type.addMethod("create", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
                        .addParameter(parseType("Instance"), "instance")
                        .setType(Machine.class)
                        .createBody();

        var constructorInvocation =
                new ObjectCreationExpr(
                        null,
                        parseClassOrInterfaceType(machineName),
                        NodeList.nodeList(new NameExpr("instance")));
        method.addStatement(new ReturnStmt(constructorInvocation));
    }

    private static void generateLoadMethod(
            CompilationUnit cu,
            ClassOrInterfaceDeclaration type,
            String moduleName,
            String wasmName) {

        cu.addImport(IOException.class);
        cu.addImport(UncheckedIOException.class);
        cu.addImport(Parser.class);
        cu.addImport(WasmModule.class);
        cu.addImport(InputStream.class);

        var method =
                type.addMethod("load", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
                        .setType(WasmModule.class)
                        .createBody();

        var getResource =
                new MethodCallExpr(
                        new ClassExpr(parseType(moduleName)),
                        "getResourceAsStream",
                        new NodeList<>(new StringLiteralExpr(wasmName)));
        var resourceVar =
                new VariableDeclarationExpr(
                        new VariableDeclarator(parseType("InputStream"), "in", getResource));

        var returnStmt =
                new ReturnStmt(
                        new MethodCallExpr()
                                .setScope(new NameExpr("Parser"))
                                .setName("parse")
                                .addArgument(new NameExpr("in")));

        var newException =
                new ObjectCreationExpr()
                        .setType(parseClassOrInterfaceType("UncheckedIOException"))
                        .addArgument(new StringLiteralExpr("Failed to load AOT WASM module"))
                        .addArgument(new NameExpr("e"));
        var catchIoException =
                new CatchClause()
                        .setParameter(
                                new com.github.javaparser.ast.body.Parameter(
                                        parseClassOrInterfaceType("IOException"), "e"))
                        .setBody(new BlockStmt(new NodeList<>(new ThrowStmt(newException))));

        method.addStatement(
                new TryStmt()
                        .setResources(new NodeList<>(resourceVar))
                        .setTryBlock(new BlockStmt(new NodeList<>(returnStmt)))
                        .setCatchClauses(new NodeList<>(catchIoException)));
    }
}
