package com.dylibso.chicory.experimental.build.time.aot;

import static com.dylibso.chicory.wasm.WasmWriter.writeVarUInt32;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmWriter;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.RawSection;
import com.dylibso.chicory.wasm.types.SectionId;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Generator {

    private final Config config;

    public Generator(Config config) {
        this.config = config;
    }

    public void generateResources() throws IOException {
        var module = Parser.parse(config.wasmFile());
        var machineName = config.name() + "Machine";
        var compiler = AotCompiler.builder(module).withClassName(machineName).build();
        var result = compiler.compile();

        var finalFolder = config.targetClassFolder();

        createFolders(finalFolder, config.name().split("\\."));

        for (Map.Entry<String, byte[]> entry : result.classBytes().entrySet()) {
            var binaryName = entry.getKey().replace('.', '/') + ".class";
            var targetFile = config.targetClassFolder().resolve(binaryName);
            Files.write(targetFile, entry.getValue());
        }
    }

    public void generateSources() throws IOException {
        var machineName = config.name() + "Machine";
        var split = config.name().split("\\.");

        var finalSourceFolder = config.targetSourceFolder();

        createFolders(finalSourceFolder, split);

        String packageName = config.getPackageName();

        SourceRoot dest = new SourceRoot(finalSourceFolder);

        var moduleName = config.getBaseName();

        var cu = StaticJavaParser.parse(getClass().getResourceAsStream("Template.java"));
        var clazz = cu.getClassByName("Template").get();
        clazz.setName(moduleName);
        clazz.getConstructors().get(0).setName(moduleName);

        generateCreateMethod(clazz.getMethodsByName("create").get(0).createBody(), machineName);
        generateGetClass(clazz.getMethodsByName("moduleClass").get(0).createBody(), moduleName);

        cu.setPackageDeclaration(packageName);
        dest.add(packageName, moduleName + ".java", cu);
        dest.saveAll();
    }

    private static void generateCreateMethod(BlockStmt method, String machineName) {
        var constructorInvocation =
                new ObjectCreationExpr(
                        null,
                        parseClassOrInterfaceType(machineName),
                        NodeList.nodeList(new NameExpr("instance")));
        method.addStatement(new ReturnStmt(constructorInvocation));
    }

    private static void generateGetClass(BlockStmt method, String moduleName) {
        method.addStatement(new ReturnStmt(new ClassExpr(parseType(moduleName))));
    }

    public void generateMetaWasm() throws IOException {
        byte[] wasmBytes = Files.readAllBytes(config.wasmFile());
        var module = Parser.builder().includeSectionId(SectionId.CODE).build().parse(wasmBytes);

        var writer = new WasmWriter();
        Parser.parseWithoutDecoding(
                wasmBytes,
                section -> {
                    if (section.sectionId() == SectionId.CODE) {
                        var out = new ByteArrayOutputStream();
                        int count = module.codeSection().functionBodyCount();
                        writeVarUInt32(out, count);
                        for (int i = 0; i < count; i++) {
                            writeVarUInt32(out, 3); // function size in bytes
                            writeVarUInt32(out, 0); // locals count
                            out.write(OpCode.UNREACHABLE.opcode());
                            out.write(OpCode.END.opcode());
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
}
