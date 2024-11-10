package com.dylibso.chicory.experimental.maven.aot;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.IOException;
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
    private Path wasmFile;

    /**
     * the name to be used by the generated class
     */
    @Parameter(required = true)
    private String name;

    /**
     * the target folder to generate classes
     */
    @Parameter(
            required = true,
            defaultValue = "${project.basedir}/target/generated-resources/chicory-aot")
    private Path targetClassFolder;

    /**
     * the target source folder to generate the Machine implementation
     */
    @Parameter(
            required = true,
            defaultValue = "${project.basedir}/target/generated-sources/chicory-aot")
    private Path targetSourceFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            var module = Parser.parse(wasmFile);
            var result = AotCompiler.compileModule(module, name);
            var split = name.split("\\.");

            var finalFolder = targetClassFolder;
            var finalSourceFolder = targetSourceFolder;

            createFolders(finalFolder, finalSourceFolder, split);

            String packageName = getPackageName(split);

            // Generate static Machine implementation
            final SourceRoot dest = new SourceRoot(finalSourceFolder);

            var machineName = split[split.length - 1] + "MachineFactory";

            var cu = new CompilationUnit(packageName);

            cu.addImport("com.dylibso.chicory.runtime.Instance");
            cu.addImport("com.dylibso.chicory.runtime.Machine");

            var clazz = cu.addClass(machineName, Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);

            var constr = clazz.addConstructor(Modifier.Keyword.PRIVATE);
            constr.createBody();

            var method = clazz.addMethod("create", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
            method.addParameter(parseType("Instance"), "instance");
            method.setType(Machine.class);
            var methodBody = method.createBody();

            var constructorInvocation =
                    new ObjectCreationExpr(
                            null,
                            parseClassOrInterfaceType(name),
                            NodeList.nodeList(new NameExpr("instance")));
            methodBody.addStatement(new ReturnStmt(constructorInvocation));

            dest.add(
                    cu.getPackageDeclaration().orElseThrow().getName().toString(),
                    cu.getType(0).getNameAsString() + ".java",
                    cu);
            dest.saveAll();

            for (Map.Entry<String, byte[]> entry : result.classBytes().entrySet()) {
                var binaryName = entry.getKey().replace('.', '/') + ".class";
                var targetFile = targetClassFolder.resolve(binaryName);
                try {
                    Files.write(targetFile, entry.getValue());
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to write " + targetFile, e);
                }
            }

            Resource resource = new Resource();
            resource.setDirectory(targetClassFolder.toString());
            project.addResource(resource);
            project.addCompileSourceRoot(targetSourceFolder.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    static void createFolders(
            Path classFilesBaseFolder, Path generatedSourceBaseFolder, String[] split) throws IOException {
        for (int i = 0; i < (split.length - 1); i++) {
            classFilesBaseFolder = classFilesBaseFolder.resolve(split[i]);
            generatedSourceBaseFolder = generatedSourceBaseFolder.resolve(split[i]);
        }
        Files.createDirectories(classFilesBaseFolder);
        Files.createDirectories(generatedSourceBaseFolder);
    }

    static String getPackageName(String[] split) {
        StringJoiner packageName = new StringJoiner(".");
        for (int i = 0; i < split.length - 1; i++) {
            packageName.add(split[i]);
        }
        return packageName.toString();
    }
}
