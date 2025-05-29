package com.dylibso.chicory.build.time.maven;

import com.dylibso.chicory.build.time.compiler.Config;
import com.dylibso.chicory.build.time.compiler.Generator;
import com.dylibso.chicory.compiler.InterpreterFallback;
import java.io.File;
import java.io.IOException;
import java.util.Set;
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
@Mojo(
        name = "chicory-compiler-gen",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true)
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
     * the target wasm folder to generate the stripped meta wasm module
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-resources/chicory-aot")
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
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating AOT classes for " + name + " from " + wasmFile);

        var config =
                Config.builder()
                        .withWasmFile(wasmFile.toPath())
                        .withName(name)
                        .withTargetClassFolder(targetClassFolder.toPath())
                        .withTargetSourceFolder(targetSourceFolder.toPath())
                        .withTargetWasmFolder(targetWasmFolder.toPath())
                        .withInterpreterFallback(interpreterFallback)
                        .withInterpretedFunctions(interpretedFunctions)
                        .build();

        var generator = new Generator(config);

        try {
            var finalInterpretedFunctions = generator.generateResources();
            generator.generateMetaWasm(finalInterpretedFunctions);
            generator.generateSources();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate resources", e);
        }

        Resource resource = new Resource();
        resource.setDirectory(targetClassFolder.getPath());
        project.addResource(resource);
        project.addCompileSourceRoot(targetSourceFolder.getPath());
    }
}
