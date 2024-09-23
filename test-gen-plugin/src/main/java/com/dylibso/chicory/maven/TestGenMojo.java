package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.Constants.SPEC_JSON;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_SOURCES;

import com.dylibso.chicory.aot.wabt.Wast2Json;
import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin should generate the testsuite out of wast files
 */
@Mojo(name = "wasm-test-gen", defaultPhase = GENERATE_TEST_SOURCES, threadSafe = true)
public class TestGenMojo extends AbstractMojo {

    private final Log log = new SystemStreamLog();

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

    @Parameter(required = true, defaultValue = "${project.build.directory}/compiled-wast")
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

        // Validate config
        validate(includedWasts, "includedWasts", true);
        validate(excludedTests, "excludedTests", false);
        validate(excludedWasts, "excludedWasts", true);
        validate(excludedMalformedWasts, "excludedMalformedWasts", true);
        validate(excludedInvalidWasts, "excludedInvalidWasts", true);
        validate(excludedUninstantiableWasts, "excludedUninstantiableWasts", true);
        validate(excludedUnlinkableWasts, "excludedUnlinkableWasts", true);

        // Instantiate the utilities
        var testSuiteDownloader = new TestSuiteDownloader(log);
        var testGen =
                new JavaTestGen(
                        project.getBasedir(),
                        sourceDestinationFolder,
                        excludedTests,
                        excludedMalformedWasts,
                        excludedInvalidWasts,
                        excludedUninstantiableWasts,
                        excludedUnlinkableWasts);

        JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());

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

            // Ensure that all wast files are included or excluded
            Set<String> allWastFiles = new HashSet<>();
            try (DirectoryStream<Path> stream =
                    Files.newDirectoryStream(testsuiteFolder.toPath(), "*.wast")) {
                stream.forEach(path -> allWastFiles.add(path.getFileName().toString()));
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to list wast files in " + testsuiteFolder, e);
            }
            includedWasts.forEach(allWastFiles::remove);
            excludedMalformedWasts.forEach(allWastFiles::remove);
            excludedInvalidWasts.forEach(allWastFiles::remove);
            excludedUninstantiableWasts.forEach(allWastFiles::remove);
            excludedUnlinkableWasts.forEach(allWastFiles::remove);
            excludedWasts.forEach(allWastFiles::remove);
            if (!allWastFiles.isEmpty()) {
                throw new MojoExecutionException(
                        "Some wast files are not included or excluded: " + allWastFiles);
            }
            List<String> includedExcludedWasts = new ArrayList<>();
            for (String includedWast : includedWasts) {
                if (excludedWasts.contains(includedWast)) {
                    includedExcludedWasts.add(includedWast);
                }
            }
            if (!includedExcludedWasts.isEmpty()) {
                throw new MojoExecutionException(
                        "Some wast files are both included and excluded: " + includedExcludedWasts);
            }

            // generate the tests
            final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());

            TestGenerator testGenerator = new TestGenerator(testGen, dest);

            includedWasts.parallelStream().forEach(testGenerator::generateTests);

            dest.saveAll();
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }

        // Add the generated tests to the source root
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }

    private static void validate(List<String> items, String name, boolean requireSorted)
            throws MojoExecutionException {
        Set<String> set = new HashSet<>();
        for (String item : items) {
            if (!set.add(item)) {
                throw new MojoExecutionException(name + " contains duplicate: " + item);
            }
        }
        if (requireSorted) {
            List<String> sorted = items.stream().sorted().collect(Collectors.toList());
            if (!sorted.equals(items)) {
                throw new MojoExecutionException(name + " is not sorted. Expected: " + sorted);
            }
        }
    }

    private final class TestGenerator {

        private final JavaTestGen testGen;
        private final SourceRoot dest;

        private TestGenerator(JavaTestGen testGen, SourceRoot dest) {
            this.testGen = testGen;
            this.dest = dest;
        }

        private void generateTests(String spec) {
            log.debug("TestGen processing " + spec);
            var wastFile = testsuiteFolder.toPath().resolve(spec).toFile();
            if (!wastFile.exists()) {
                throw new IllegalArgumentException(
                        "Wast file " + wastFile.getAbsolutePath() + " not found");
            }

            var plainName = wastFile.getName().replace(".wast", "");
            File wasmFilesFolder = compiledWastTargetFolder.toPath().resolve(plainName).toFile();
            File specFile = wasmFilesFolder.toPath().resolve(SPEC_JSON).toFile();
            if (!wasmFilesFolder.mkdirs()) {
                log.warn("Could not create folder: " + wasmFilesFolder);
            }

            Wast2Json.builder().withFile(wastFile).withOutput(specFile).build().process();

            var name = specFile.toPath().getParent().toFile().getName();
            var cu = testGen.generate(name, readWast(specFile), wasmFilesFolder);
            dest.add(cu);
        }

        private Wast readWast(File file) {
            try {
                return new ObjectMapper().readValue(file, Wast.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
