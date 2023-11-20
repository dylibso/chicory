package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.Constants.SPEC_JSON;

import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin should generate the testsuite out of wast files
 */
@Mojo(name = "wasm-test-gen", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class TestGenMojo extends AbstractMojo {

    private final Log log = new SystemStreamLog();

    /**
     * OS name
     */
    @Parameter(defaultValue = "${os.name}")
    private String osName;

    /**
     * Repository of the testsuite.
     */
    @Parameter(required = true, defaultValue = "https://github.com/WebAssembly/testsuite")
    private String testSuiteRepo;

    /**
     * Repository of the testsuite.
     */
    @Parameter(required = true, defaultValue = "main")
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

    @Parameter(required = true, defaultValue = "${project.build.directory}/compiled-wast")
    private File compiledWastTargetFolder;

    /**
     * Wabt version
     */
    @Parameter(required = true, property = "wabtVersion", defaultValue = "1.0.33")
    private String wabtVersion;

    @Parameter(
            required = true,
            defaultValue = "https://github.com/WebAssembly/wabt/releases/download/")
    private String wabtReleasesURL;

    @Parameter(required = true, defaultValue = "${project.build.directory}/wabt")
    private File wabtDownloadTargetFolder;

    /**
     * Include list for the wast files to be processed.
     */
    @Parameter(required = true)
    private List<String> wastToProcess;

    /**
     * Include list for the wast files that should generate an ordered spec.
     */
    @Parameter(required = true)
    private List<String> orderedWastToProcess;

    /**
     * Exclude list for tests that are still failing.
     */
    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedTests;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    private List<String> clean(List<String> in) {
        return in.stream()
                .map(t -> (t != null) ? t.replace("\n", "").replace("\r", "").trim() : "")
                .collect(Collectors.toList());
    }

    @Override
    public void execute() throws MojoExecutionException {
        JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());
        // Instantiate the utilities
        var testSuiteDownloader = new TestSuiteDownloader(log);
        var wast2Json =
                new Wast2JsonWrapper(
                        log,
                        wabtDownloadTargetFolder,
                        wabtReleasesURL,
                        wabtVersion,
                        osName,
                        compiledWastTargetFolder);
        var testGen =
                new JavaTestGen(
                        log, project.getBasedir(), sourceDestinationFolder, clean(excludedTests));

        // Create destination folders
        if (!compiledWastTargetFolder.mkdirs()) {
            log.warn("Failed to create folder: " + compiledWastTargetFolder);
        }

        if (!sourceDestinationFolder.mkdirs()) {
            log.warn("Failed to create folder: " + sourceDestinationFolder);
        }

        try {
            // create a reproducible environment
            testSuiteDownloader.downloadTestsuite(testSuiteRepo, testSuiteRepoRef, testsuiteFolder);
            wast2Json.fetch();

            var allWasts = new ArrayList<String>();
            allWasts.addAll(clean(wastToProcess));
            var cleanedOrderedWasts = clean(orderedWastToProcess);
            allWasts.addAll(cleanedOrderedWasts);

            // generate the tests
            final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());
            clean(allWasts).stream()
                    .parallel()
                    .forEach(
                            spec -> {
                                log.debug(
                                        "TestGen processing "
                                                + spec
                                                + " ordered: "
                                                + cleanedOrderedWasts.contains(spec));
                                var wastFile = testsuiteFolder.toPath().resolve(spec).toFile();
                                if (!wastFile.exists()) {
                                    throw new IllegalArgumentException(
                                            "Wast file "
                                                    + wastFile.getAbsolutePath()
                                                    + " not found");
                                }
                                var wasmFilesFolder =
                                        wast2Json.execute(
                                                testsuiteFolder.toPath().resolve(spec).toFile());
                                var cu =
                                        testGen.generate(
                                                dest,
                                                wasmFilesFolder
                                                        .toPath()
                                                        .resolve(SPEC_JSON)
                                                        .toFile(),
                                                wasmFilesFolder,
                                                cleanedOrderedWasts.contains(spec));
                                dest.add(cu);
                            });
            dest.saveAll();
        } catch (Exception e) {
            System.err.println("Generation of the testsuite failed - ignoring -");
            e.printStackTrace();
        }

        // Add the generated tests to the source root
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }
}
