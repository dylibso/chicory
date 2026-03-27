package com.dylibso.chicory.build.time.maven;

import com.dylibso.chicory.build.time.compiler.Config;
import com.dylibso.chicory.build.time.compiler.Generator;
import com.dylibso.chicory.compiler.InterpreterFallback;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
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
@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ChicoryCompilerGenMojo extends AbstractMojo {

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
    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
    private File targetClassFolder;

    /**
     * the target source folder to generate the Machine implementation
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/chicory-compiler")
    private File targetSourceFolder;

    /**
     * the target wasm folder to generate the stripped meta wasm module
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-resources/chicory-compiler")
    private File targetWasmFolder;

    /**
     * the action to take if the compiler needs to use the interpreter because a function is too big
     */
    @Parameter(required = true, defaultValue = "FAIL")
    InterpreterFallback interpreterFallback;

    /**
     * The indexes of functions that should be interpreted, separated by commas
     */
    @Parameter(required = false, defaultValue = "")
    Set<Integer> interpretedFunctions;

    /**
     * Fully qualified name of the user's class that will use the compiled module.
     * When set, the plugin generates _ModuleExports and _ModuleImports wrapper classes,
     * eliminating the need for @WasmModuleInterface annotation and the annotation processor.
     */
    @Parameter(required = false)
    String moduleInterface;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Compiling classes for " + name + " from " + wasmFile);

        var config =
                Config.builder()
                        .withWasmFile(wasmFile.toPath())
                        .withName(name)
                        .withTargetClassFolder(targetClassFolder.toPath())
                        .withTargetSourceFolder(targetSourceFolder.toPath())
                        .withTargetWasmFolder(targetWasmFolder.toPath())
                        .withInterpreterFallback(interpreterFallback)
                        .withInterpretedFunctions(interpretedFunctions)
                        .withModuleInterface(moduleInterface)
                        .build();

        var generator = new Generator(config);

        try {
            var finalInterpretedFunctions = generator.generateResources();
            generator.generateMetaWasm(finalInterpretedFunctions);
            generator.generateSources();

            if (moduleInterface != null && !moduleInterface.isEmpty()) {
                generator.generateModuleInterface(moduleInterface);
            }

            if (interpreterFallback == InterpreterFallback.WARN
                    && !finalInterpretedFunctions.isEmpty()) {
                var sorted = new TreeSet<>(finalInterpretedFunctions);
                StringBuilder sb = new StringBuilder();
                sb.append("<interpretedFunctions>\n");
                for (Integer funcId : sorted) {
                    sb.append("  <function>").append(funcId).append("</function>\n");
                }
                sb.append("</interpretedFunctions>");
                getLog().warn(
                                "Copy-paste the following to pre-declare interpreted functions"
                                        + " in your pom.xml:\n"
                                        + sb);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate resources", e);
        }

        Resource resource = new Resource();
        resource.setDirectory(targetWasmFolder.getPath());
        project.addResource(resource);
        project.addCompileSourceRoot(targetSourceFolder.getPath());
    }
}
