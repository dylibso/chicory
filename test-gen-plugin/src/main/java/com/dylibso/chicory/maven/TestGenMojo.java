package com.dylibso.chicory.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_SOURCES;

import com.dylibso.chicory.testgen.TestGen;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin should generate the testsuite out of wast files
 */
@Mojo(name = "wasm-test-gen", defaultPhase = GENERATE_TEST_SOURCES, threadSafe = true)
public class TestGenMojo extends AbstractMojo {

    /**
     * Repository of the testsuite.
     */
    @Parameter(required = true, defaultValue = "https://github.com/WebAssembly/testsuite")
    private String testSuiteRepo;

    /**
     * Repository of the testsuite.
     */
    // TODO: restore `main`
    @Parameter(required = true, defaultValue = "88e97b0f742f4c3ee01fea683da130f344dd7b02")
    private String testSuiteRepoRef;

    /**
     * Location for the source wast files.
     */
    @Parameter(required = true, defaultValue = "${project.directory}/../../testsuite")
    private File testsuiteFolder;

    /**
     * Location for the junit generated sources.
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-test-sources/test-gen")
    private File sourceDestinationFolder;

    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-resources/compiled-wast")
    private File compiledWastTargetFolder;

    /**
     * Include list for the wast files that should generate an ordered spec.
     */
    @Parameter(required = true)
    private List<String> includedWasts;

    /**
     * Exclude list for tests that are still failing.
     */
    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedTests;

    /**
     * Exclude list for wast files that are still failing.
     */
    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedMalformedWasts;

    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedInvalidWasts;

    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedUninstantiableWasts;

    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedUnlinkableWasts;

    /**
     * Exclude list for wast files that are entirely skipped.
     */
    @Parameter(defaultValue = "[]")
    private List<String> excludedWasts;

    /**
     * Skip execution of this Mojo.
     */
    @Parameter(property = "wasm-test-gen.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            return;
        }

        JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());

        var excludedTests =
                this.excludedTests.stream().map(String::trim).collect(Collectors.toList());

        try {
            TestGen.execute(
                    testSuiteRepo,
                    testSuiteRepoRef,
                    testsuiteFolder,
                    sourceDestinationFolder,
                    compiledWastTargetFolder,
                    includedWasts,
                    excludedTests,
                    excludedMalformedWasts,
                    excludedInvalidWasts,
                    excludedUninstantiableWasts,
                    excludedUnlinkableWasts,
                    excludedWasts);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Failed executing TestGen", e);
        }

        // Add the generated tests to the source root
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
        // Add compiled-wast to the resources
        Resource resource = new Resource();
        resource.setDirectory(compiledWastTargetFolder.getPath());
        project.addTestResource(resource);
    }
}
