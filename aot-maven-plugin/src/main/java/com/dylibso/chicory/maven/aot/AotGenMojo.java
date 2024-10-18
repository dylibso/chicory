package com.dylibso.chicory.maven.aot;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.aot.AotCompiler;
import com.dylibso.chicory.aot.CompilerResult;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
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
    private File wasmFile;

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
    private File targetClassFolder;

    /**
     * the target source folder to generate the Machine implementation
     */
    @Parameter(
            required = true,
            defaultValue = "${project.basedir}/target/generated-sources/chicory-aot")
    private File targetSourceFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    @SuppressWarnings("deprecation")
    public void execute() throws MojoExecutionException {
        var module = Parser.parse(wasmFile);
        var result = AotCompiler.compileModule(module, name);

        var finalFolder = targetClassFolder.toPath();
        var finalSourceFolder = targetSourceFolder.toPath();

        String[] splitName = name.split("\\.", -1);
        finalFolder = targetClassFolder.toPath();
        finalSourceFolder = targetSourceFolder.toPath();
        String packageName = createPackageName(splitName);

        finalFolder = resolvePath(finalFolder, splitName);
        finalSourceFolder = resolvePath(finalSourceFolder, splitName);
        try {
            Files.createDirectories(finalFolder);
            Files.createDirectories(finalSourceFolder);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to create either "
                            + finalFolder.toString()
                            + " or "
                            + finalFolder.toString()
                            + "folders.",
                    e);
        }

        String machineName = splitName[splitName.length - 1] + "MachineFactory";
        generateClass(finalSourceFolder, packageName, machineName);
        writeCompiledClasses(result, finalFolder);
        addProjectResources(targetClassFolder.getPath(), targetSourceFolder.getPath());
    }

    private void addProjectResources(String classFolder, String sourceFolder) {
        Resource resource = new Resource();
        resource.setDirectory(classFolder);
        project.addResource(resource);
        project.addCompileSourceRoot(sourceFolder);
    }

    private void writeCompiledClasses(CompilerResult result, Path finalFolder)
            throws MojoExecutionException {
        for (Map.Entry<String, byte[]> entry : result.classBytes().entrySet()) {
            String binaryName = entry.getKey().replace('.', '/') + ".class";
            Path targetFile = finalFolder.resolve(binaryName);
            try {
                Files.write(targetFile, entry.getValue());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write " + targetFile, e);
            }
        }
    }

    private void generateClass(Path finalSourceFolder, String packageName, String machineName) {
        SourceRoot dest = new SourceRoot(finalSourceFolder);

        CompilationUnit cu = new CompilationUnit(packageName);
        cu.setPackageDeclaration(packageName);
        cu.setStorage(finalSourceFolder.resolve(machineName + ".java"));

        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Machine");

        var clazz = cu.addClass(machineName, Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
        clazz.addConstructor(Modifier.Keyword.PRIVATE).createBody();

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

        dest.add(cu);
        dest.saveAll();
    }

    private String createPackageName(String[] splitName) {
        StringJoiner packageName = new StringJoiner(".");
        for (int i = 0; i < splitName.length - 1; i++) {
            packageName.add(splitName[i]);
        }
        return packageName.toString();
    }

    private Path resolvePath(Path baseFolder, String[] splitName) {
        for (int i = 0; i < splitName.length - 1; i++) {
            baseFolder = baseFolder.resolve(splitName[i]);
        }
        return baseFolder;
    }
}
