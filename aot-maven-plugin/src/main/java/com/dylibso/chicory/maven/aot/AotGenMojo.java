package com.dylibso.chicory.maven.aot;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.Instance;
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
@Mojo(name = "wasm-aot-gen", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = false)
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
    @SuppressWarnings({"StringSplitter", "deprecation"})
    public void execute() throws MojoExecutionException {
        var instance =
                Instance.builder(Parser.parse(wasmFile))
                        .withMachineFactory(inst -> new AotMachine(name, inst))
                        .withStart(false)
                        .withInitialize(false)
                        .withSkipImportMapping(true)
                        .build();
        var compiled = ((AotMachine) instance.getMachine()).compiledClass();
        var split = name.split("\\.");
        var finalFolder = targetClassFolder.toPath();
        var finalSourceFolder = targetSourceFolder.toPath();
        for (int i = 0; i < (split.length - 1); i++) {
            finalFolder = finalFolder.resolve(split[i]);
            finalSourceFolder = finalSourceFolder.resolve(split[i]);
        }
        finalFolder.toFile().mkdirs();
        var packageName = split[0];
        for (int i = 1; i < (split.length - 1); i++) {
            packageName += "." + split[i];
        }

        // Generate static Machine implementation
        finalSourceFolder.toFile().mkdirs();
        final SourceRoot dest = new SourceRoot(finalSourceFolder);

        var machineName = split[split.length - 1] + "MachineFactory";

        var cu = new CompilationUnit(packageName);
        cu.setPackageDeclaration(packageName);
        cu.setStorage(finalSourceFolder.resolve(machineName + ".java"));

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

        dest.add(cu);
        dest.saveAll();

        var targetFile = finalFolder.resolve(split[split.length - 1] + ".class");
        try {
            Files.write(targetFile, compiled);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write " + name, e);
        }

        Resource resource = new Resource();
        resource.setDirectory(targetClassFolder.getPath());
        project.addResource(resource);
        project.addCompileSourceRoot(targetSourceFolder.getPath());
    }
}
