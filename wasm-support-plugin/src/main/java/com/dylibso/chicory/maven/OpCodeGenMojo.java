package com.dylibso.chicory.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin should generate the OpCodes.java file from a tsv
 */
@Mojo(name = "opcode-gen", defaultPhase = GENERATE_SOURCES, threadSafe = true)
public class OpCodeGenMojo extends AbstractMojo {

    /**
     * The source file
     */
    @Parameter(defaultValue = "src/main/resources/instructions.tsv")
    private File instructionsFile;

    /**
     * Location for the OpCode generated source.
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/opcode")
    private File sourceDestinationFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        sourceDestinationFolder.mkdirs();
        try {
            OpCodeGen.generate(instructionsFile, sourceDestinationFolder);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
        project.addCompileSourceRoot(sourceDestinationFolder.getPath());
    }
}
