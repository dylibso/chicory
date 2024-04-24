package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.Constants.SPEC_JSON;

import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
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
    // TODO: wabt doesn't handle correctly the syntax of if.wast here:
    // https://github.com/WebAssembly/testsuite/blame/dc27dad3e34e466bdbfea32fe3c73f5e31f88560/if.wast#L528
    // restore 'main'
    @Parameter(required = true, defaultValue = "c2a67a575ddc815ff2212f68301d333e5e30a923")
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

    /**
     * Location of the imports sources.
     */
    @Parameter(required = true, defaultValue = "${project.basedir}/src/test/java")
    private File importsSourcesFolder;

    @Parameter(required = true, defaultValue = "${project.build.directory}/compiled-wast")
    private File compiledWastTargetFolder;

    /**
     * Wabt version
     * Accepts a specific version or "latest" to query the GH API
     */
    @Parameter(required = true, property = "wabtVersion", defaultValue = "1.0.34")
    private String wabtVersion;

    @Parameter(
            required = true,
            defaultValue = "https://github.com/WebAssembly/wabt/releases/download/")
    private String wabtReleasesURL;

    @Parameter(required = true, defaultValue = "${project.build.directory}/wabt")
    private File wabtDownloadTargetFolder;

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

    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedMalformedTests;

    @Parameter(required = false, defaultValue = "[]")
    private List<String> excludedInvalidTests;

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
                        log,
                        project.getBasedir(),
                        sourceDestinationFolder,
                        clean(excludedTests),
                        clean(excludedMalformedTests),
                        clean(excludedInvalidTests));

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

            // generate the tests
            final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());
            final SourceRoot importSourceRoot = new SourceRoot(importsSourcesFolder.toPath());

            TestGenerator testGenerator =
                    new TestGenerator(wast2Json, testGen, importSourceRoot, dest);

            clean(orderedWastToProcess).stream()
                    .parallel()
                    .forEach(spec -> testGenerator.generateTests(spec));

            dest.saveAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Add the generated tests to the source root
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }

    private class TestGenerator {

        private final Wast2JsonWrapper wast2Json;
        private final JavaTestGen testGen;
        private final SourceRoot importSourceRoot;
        private final SourceRoot dest;

        private TestGenerator(
                Wast2JsonWrapper wast2Json,
                JavaTestGen testGen,
                SourceRoot importSourceRoot,
                SourceRoot dest) {
            this.wast2Json = wast2Json;
            this.testGen = testGen;
            this.importSourceRoot = importSourceRoot;
            this.dest = dest;
        }

        private void generateTests(String spec) {
            log.debug("TestGen processing " + spec);
            var wastFile = testsuiteFolder.toPath().resolve(spec).toFile();
            if (!wastFile.exists()) {
                throw new IllegalArgumentException(
                        "Wast file " + wastFile.getAbsolutePath() + " not found");
            }
            var retries = 3;
            File specFile = null;
            File wasmFilesFolder = null;
            Wast wast = null;

            while (true) {
                try {
                    wasmFilesFolder =
                            wast2Json.execute(testsuiteFolder.toPath().resolve(spec).toFile());
                    specFile = wasmFilesFolder.toPath().resolve(SPEC_JSON).toFile();

                    assert (specFile.exists());

                    wast = new ObjectMapper().readValue(specFile, Wast.class);
                    break;
                } catch (Exception e) {
                    if (retries > 0) {
                        --retries;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
            var name = specFile.toPath().getParent().toFile().getName();
            var cu = testGen.generate(name, wast, wasmFilesFolder, importSourceRoot);
            dest.add(cu);
        }
    }
}
