package com.dylibso.chicory.experimental.maven.aot;

import static com.dylibso.chicory.wasm.WasmWriter.writeVarUInt32;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.WasmWriter;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.RawSection;
import com.dylibso.chicory.wasm.types.SectionId;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
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
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.utils.SourceRoot;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin generates an invokable library from the compiled Wasm
 */
@Mojo(name = "wasm-aot-gen", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class AotGenMojo extends AbstractMojo {

    /**
     * the wasm module to be used
     */
    @Parameter(required = true)
    private File wasmFile;

    /**
     * the base name to be used for the generated classes
     */
    @Parameter(required = true)
    private String name;

    /**
     * the target folder to generate classes
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-resources/chicory-aot")
    private File targetClassFolder;

    /**
     * the target source folder to generate the Machine implementation
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/chicory-aot")
    private File targetSourceFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating AOT classes for " + name + " from " + wasmFile);

        byte[] wasmBytes;
        try {
            wasmBytes = Files.readAllBytes(wasmFile.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read WASM file: " + wasmFile, e);
        }

        var module = Parser.parse(wasmFile);
        var machineName = name + "Machine";
        var result = AotCompiler.compileModule(module, machineName);
        var split = name.split("\\.");

        var finalFolder = targetClassFolder.toPath();
        var finalSourceFolder = targetSourceFolder.toPath();

        createFolders(finalFolder, finalSourceFolder, split);

        String packageName = getPackageName(split);

        SourceRoot dest = new SourceRoot(finalSourceFolder);

        var baseName = split[split.length - 1];
        var moduleName = baseName + "Module";
        var wasmName = baseName + ".meta";

        var cu = new CompilationUnit(packageName);

        var type = cu.addClass(moduleName, Keyword.PUBLIC, Keyword.FINAL);

        type.addConstructor(Keyword.PRIVATE).createBody();

        generateCreateMethod(cu, type, machineName);
        generateLoadMethod(cu, type, moduleName, wasmName);

        dest.add(packageName, moduleName + ".java", cu);
        dest.saveAll();

        for (Map.Entry<String, byte[]> entry : result.classBytes().entrySet()) {
            var binaryName = entry.getKey().replace('.', '/') + ".class";
            var targetFile = targetClassFolder.toPath().resolve(binaryName);
            try {
                Files.write(targetFile, entry.getValue());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write " + targetFile, e);
            }
        }

        var rewrittenWasm = rewriteWasm(wasmBytes, module);
        var newWasmFile =
                targetClassFolder.toPath().resolve(packageName.replace('.', '/')).resolve(wasmName);
        try {
            Files.write(newWasmFile, rewrittenWasm);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write " + newWasmFile, e);
        }

        Resource resource = new Resource();
        resource.setDirectory(targetClassFolder.getPath());
        project.addResource(resource);
        project.addCompileSourceRoot(targetSourceFolder.getPath());
    }

    private static void generateCreateMethod(
            CompilationUnit cu, ClassOrInterfaceDeclaration type, String machineName) {

        cu.addImport(Instance.class);
        cu.addImport(Machine.class);

        var method =
                type.addMethod("create", Keyword.PUBLIC, Keyword.STATIC)
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

        var method =
                type.addMethod("load", Keyword.PUBLIC, Keyword.STATIC)
                        .setType(WasmModule.class)
                        .createBody();

        var getResource =
                new MethodCallExpr(
                        new ClassExpr(parseType(moduleName)),
                        "getResourceAsStream",
                        new NodeList<>(new StringLiteralExpr(wasmName)));
        var resourceVar =
                new VariableDeclarationExpr(
                        new VariableDeclarator(new VarType(), "in", getResource));

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

    private static byte[] rewriteWasm(byte[] wasmBytes, WasmModule module) {
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
        return writer.bytes();
    }

    static void createFolders(
            Path classFilesBaseFolder, Path generatedSourceBaseFolder, String[] split) {
        for (int i = 0; i < (split.length - 1); i++) {
            classFilesBaseFolder = classFilesBaseFolder.resolve(split[i]);
            generatedSourceBaseFolder = generatedSourceBaseFolder.resolve(split[i]);
        }
        classFilesBaseFolder.toFile().mkdirs();
        generatedSourceBaseFolder.toFile().mkdirs();
    }

    static String getPackageName(String[] split) {
        StringJoiner packageName = new StringJoiner(".");
        for (int i = 0; i < split.length - 1; i++) {
            packageName.add(split[i]);
        }
        return packageName.toString();
    }
}
