package com.dylibso.chicory.testgen;

import static com.dylibso.chicory.testgen.Constants.SPEC_JSON;

import com.dylibso.chicory.testgen.wast.Wast;
import com.dylibso.chicory.tools.wasm.Wast2Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This plugin should generate the testsuite out of wast files
 */
public final class TestGen {

    private TestGen() {}

    public static void execute(
            String testSuiteRepo,
            String testSuiteRepoRef,
            File testsuiteFolder,
            File sourceDestinationFolder,
            File compiledWastTargetFolder,
            List<String> includedWasts,
            List<String> excludedTests,
            List<String> excludedMalformedWasts,
            List<String> excludedInvalidWasts,
            List<String> excludedUninstantiableWasts,
            List<String> excludedUnlinkableWasts,
            List<String> excludedWasts) {
        // Validate config
        validate(includedWasts, "includedWasts", true);
        validate(excludedTests, "excludedTests", false);
        validate(excludedWasts, "excludedWasts", true);
        validate(excludedMalformedWasts, "excludedMalformedWasts", true);
        validate(excludedInvalidWasts, "excludedInvalidWasts", true);
        validate(excludedUninstantiableWasts, "excludedUninstantiableWasts", true);
        validate(excludedUnlinkableWasts, "excludedUnlinkableWasts", true);

        // Instantiate the utilities
        var testSuiteDownloader = new TestSuiteDownloader();
        var testGen =
                new JavaTestGen(
                        excludedTests,
                        excludedMalformedWasts,
                        excludedInvalidWasts,
                        excludedUninstantiableWasts,
                        excludedUnlinkableWasts);

        // Create destination folders
        if (!compiledWastTargetFolder.mkdirs()) {
            throw new RuntimeException("Failed to create folder: " + compiledWastTargetFolder);
        }

        if (!sourceDestinationFolder.mkdirs()) {
            throw new RuntimeException("Failed to create folder: " + sourceDestinationFolder);
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
                throw new RuntimeException("Failed to list wast files in " + testsuiteFolder, e);
            }
            includedWasts.forEach(allWastFiles::remove);
            excludedMalformedWasts.forEach(allWastFiles::remove);
            excludedInvalidWasts.forEach(allWastFiles::remove);
            excludedUninstantiableWasts.forEach(allWastFiles::remove);
            excludedUnlinkableWasts.forEach(allWastFiles::remove);
            excludedWasts.forEach(allWastFiles::remove);
            if (!allWastFiles.isEmpty()) {
                throw new RuntimeException(
                        "Some wast files are not included or excluded: " + allWastFiles);
            }

            // generate the tests
            final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());

            TestGenerator testGenerator =
                    new TestGenerator(testGen, dest, testsuiteFolder, compiledWastTargetFolder);

            includedWasts.parallelStream().forEach(testGenerator::generateTests);

            dest.saveAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void validate(List<String> items, String name, boolean requireSorted) {
        Set<String> set = new HashSet<>();
        for (String item : items) {
            if (!set.add(item)) {
                throw new RuntimeException(name + " contains duplicate: " + item);
            }
        }
        if (requireSorted) {
            List<String> sorted = items.stream().sorted().collect(Collectors.toList());
            if (!sorted.equals(items)) {
                throw new RuntimeException(name + " is not sorted. Expected: " + sorted);
            }
        }
    }

    private static final class Proposal {
        final String remapping;

        private Proposal(String remapping) {
            this.remapping = remapping;
        }
    }

    private static Map<String, Proposal> proposals =
            Map.of(
                    "gc",
                    new Proposal("gc"),
                    "tail-call",
                    new Proposal("tc"),
                    "exception-handling",
                    new Proposal("eh"),
                    "function-references",
                    new Proposal("function-references"));

    private static final class TestGenerator {

        private final JavaTestGen testGen;
        private final SourceRoot dest;
        private final File testsuiteFolder;
        private final File compiledWastTargetFolder;

        private TestGenerator(
                JavaTestGen testGen,
                SourceRoot dest,
                File testsuiteFolder,
                File compiledWastTargetFolder) {
            this.testGen = testGen;
            this.dest = dest;
            this.testsuiteFolder = testsuiteFolder;
            this.compiledWastTargetFolder = compiledWastTargetFolder;
        }

        private void generateTests(String spec) {
            var wastFile = testsuiteFolder.toPath().resolve(spec).toFile();
            if (!wastFile.exists()) {
                throw new IllegalArgumentException(
                        "Wast file " + wastFile.getAbsolutePath() + " not found");
            }

            var plainName = wastFile.getName().replace(".wast", "");
            if (wastFile.getParentFile().getParentFile().getName().equalsIgnoreCase("proposals")) {
                var proposal = proposals.get(wastFile.getParentFile().getName());
                plainName =
                        proposal.remapping
                                + plainName.substring(0, 1).toUpperCase(Locale.ROOT)
                                + plainName.substring(1);
            }
            File wasmFilesFolder = compiledWastTargetFolder.toPath().resolve(plainName).toFile();
            File specFile = wasmFilesFolder.toPath().resolve(SPEC_JSON).toFile();
            if (!wasmFilesFolder.mkdirs()) {
                throw new RuntimeException("Could not create folder: " + wasmFilesFolder);
            }

            Wast2Json.builder()
                    .withFile(wastFile)
                    .withOutput(specFile.toPath().getParent().toFile())
                    .build()
                    .process();

            var name = specFile.toPath().getParent().toFile().getName();
            var cu = testGen.generate(name, readWast(specFile), "/" + plainName);
            dest.add(
                    cu.getPackageDeclaration().orElseThrow().getName().toString(),
                    cu.getType(0).getNameAsString() + ".java",
                    cu);
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
