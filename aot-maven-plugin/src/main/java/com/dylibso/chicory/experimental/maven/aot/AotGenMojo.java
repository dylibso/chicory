package com.dylibso.chicory.experimental.maven.aot;

import com.dylibso.chicory.experimental.build.time.aot.Config;
import com.dylibso.chicory.experimental.build.time.aot.Generator;
import java.io.File;
import java.io.IOException;
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
     *
     * @deprecated use moduleClass instead
     */
    @Parameter @Deprecated private String name;

    /**
     * the module class name that will be generated.
     */
    @Parameter private String moduleClass;

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
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating AOT classes for " + name + " from " + wasmFile);

        Config.Builder builder = Config.builder();
        if (moduleClass != null) {
            builder.withModuleClass(moduleClass);
        } else {
            builder.withModuleClass(name + "Module");
        }

        var config =
                builder.withWasmFile(wasmFile.toPath())
                        .withTargetClassFolder(targetClassFolder.toPath())
                        .withTargetSourceFolder(targetSourceFolder.toPath())
                        .withTargetWasmFolder(targetWasmFolder.toPath())
                        .build();

        var generator = new Generator(config);

        try {
            generator.generateMetaWasm();
            generator.generateResources();
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
