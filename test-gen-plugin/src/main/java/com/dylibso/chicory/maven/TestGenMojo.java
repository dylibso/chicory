package com.dylibso.chicory.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.dylibso.chicory.maven.Constants.SPEC_JSON;

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
    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-test-sources/test-gen")
    private File sourceDestinationFolder;

    @Parameter(required = true, defaultValue = "${project.build.directory}/compiled-wast")
    private File compiledWastTargetFolder;

    /**
     * Wabt version
     */
    @Parameter(required = true, defaultValue = "1.0.33")
    private String wabtVersion;

    @Parameter(required = true, defaultValue = "https://github.com/WebAssembly/wabt/releases/download/")
    private String wabtReleasesURL;
    @Parameter(required = true, defaultValue = "${project.build.directory}/wabt")
    private File wabtDownloadTargetFolder;

    /**
     * Include list for the wast files to be processed.
     */
    @Parameter(required = true)
    private List<String> wastToProcess;

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
        return in.stream().map(t -> t.replace("\n", "").replace("\r", "").trim()).collect(Collectors.toList());
    }

    @Override
    public void execute() throws MojoExecutionException {
        JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());
        // Instantiate the utilities
        var testSuiteDownloader = new TestSuiteDownloader(log);
        var wast2Json = new Wast2JsonWrapper(log, wabtDownloadTargetFolder, wabtReleasesURL, wabtVersion, osName, compiledWastTargetFolder);
        var testGen = new JavaTestGen(log, project.getBasedir(), sourceDestinationFolder);

        // Create destination folders
        compiledWastTargetFolder.mkdirs();
        sourceDestinationFolder.mkdirs();

        try {
            // create a reproducible environment
            testSuiteDownloader.downloadTestsuite(testSuiteRepo, testSuiteRepoRef, testsuiteFolder);
            wast2Json.fetch();

            // generate the tests
            for (var spec: clean(wastToProcess)) {
                log.debug("TestGen processing " + spec);
                var wastFile = testsuiteFolder.toPath().resolve(spec).toFile();
                if (!wastFile.exists()) {
                    throw new IllegalArgumentException("Wast file " + wastFile.getAbsolutePath() + " not found");
                }
                var wasmFilesFolder = wast2Json.execute(testsuiteFolder.toPath().resolve(spec).toFile());
                testGen.generate(wasmFilesFolder.toPath().resolve(SPEC_JSON).toFile(), wasmFilesFolder, clean(excludedTests));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Add the generated tests to the source root
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }

}
