package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.StringUtils.capitalize;
import static com.dylibso.chicory.maven.StringUtils.escapedCamelCase;
import static com.github.javaparser.utils.StringEscapeUtils.escapeJava;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_SOURCES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;

/**
 * This plugin generates test classes for the WASI test suite.
 */
@Mojo(name = "wasi-test-gen", defaultPhase = GENERATE_TEST_SOURCES, threadSafe = true)
public class WasiTestGenMojo extends AbstractMojo {

    private static final String JUNIT_TEST = "org.junit.jupiter.api.Test";
    private static final String WASI_TEST_RUNNER = "com.dylibso.chicory.wasi.WasiTestRunner";

    private final Log log = new SystemStreamLog();

    /**
     * Repository of the test suite.
     */
    @Parameter(required = true, defaultValue = "https://github.com/WebAssembly/wasi-testsuite")
    private String testSuiteRepo;

    /**
     * Repository of the test suite.
     */
    @Parameter(required = true, defaultValue = "prod/testsuite-base")
    private String testSuiteRepoRef;

    /**
     * Location for the test suite.
     */
    @Parameter(required = true)
    private File testSuiteFolder;

    /**
     * Test suite files to process.
     */
    @Parameter(required = true)
    private FileSet testSuiteFiles;

    /**
     * Location for the junit generated sources.
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-test-sources/test-gen")
    private File sourceDestinationFolder;

    /**
     * Skip execution of this Mojo.
     */
    @Parameter(property = "wasi-test-gen.skip", defaultValue = "false")
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

        try {
            new WasiTestSuiteDownloader(log)
                    .downloadTestsuite(testSuiteRepo, testSuiteRepoRef, testSuiteFolder);
        } catch (GitAPIException | ConfigInvalidException | IOException e) {
            throw new MojoExecutionException("Failed to download testsuite: " + e.getMessage(), e);
        }

        if (testSuiteFiles.getDirectory() == null) {
            testSuiteFiles.setDirectory(testSuiteFolder.getAbsolutePath());
        }

        // find all *.wasm test cases
        FileSetManager fileSetManager = new FileSetManager();
        String[] includedFiles = fileSetManager.getIncludedFiles(testSuiteFiles);
        List<File> allFiles =
                Stream.of(includedFiles)
                        .map(file -> new File(testSuiteFiles.getDirectory(), file))
                        .sorted()
                        .collect(toList());
        if (allFiles.isEmpty()) {
            throw new MojoExecutionException("No files found in the test suite");
        }

        // validate and group files by test suite
        PathMatcher pathMatcher =
                FileSystems.getDefault().getPathMatcher("glob:**/tests/*/testsuite/*.wasm");

        Map<String, List<File>> filesBySuite = new LinkedHashMap<>();
        for (File file : allFiles) {
            Path path = file.toPath();
            if (!pathMatcher.matches(path)) {
                throw new MojoExecutionException("Invalid test suite file path: " + path);
            }
            String suiteName = path.getParent().getParent().getFileName().toString();
            filesBySuite.computeIfAbsent(suiteName, ignored -> new ArrayList<>()).add(file);
        }

        // create source root
        try {
            Files.createDirectories(sourceDestinationFolder.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to create destination folder: " + sourceDestinationFolder, e);
        }
        SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());

        // generate test classes
        for (var entry : filesBySuite.entrySet()) {
            String testSuite = entry.getKey();
            List<File> files = entry.getValue();

            String packageName = "com.dylibso.chicory.wasi.test";
            var cu = new CompilationUnit(packageName);

            var destFile =
                    Path.of(sourceDestinationFolder.getAbsolutePath(), packageName.split("\\."))
                            .resolve("Suite" + capitalize(testSuite) + "Test.java");
            cu.setStorage(destFile);

            cu.addImport(WASI_TEST_RUNNER);
            cu.addImport("java.io.File");
            cu.addImport("java.util.List");
            cu.addImport("java.util.Map");
            cu.addImport(JUNIT_TEST);

            // generate test methods
            var testClass = cu.addClass("Suite" + capitalize(testSuite) + "Test");
            for (File file : files) {
                String baseName =
                        file.getName().substring(0, file.getName().length() - ".wasm".length());

                Specification specification =
                        readSpecification(new File(file.getParentFile(), baseName + ".json"));

                var method =
                        testClass.addMethod(
                                "test" + escapedCamelCase(baseName), Modifier.Keyword.PUBLIC);
                method.addAnnotation("Test");

                method.getBody()
                        .orElseThrow()
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("var test"),
                                        new NameExpr("new File(\"" + relativePath(file) + "\")"),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("List<String> args"),
                                        new NameExpr(listOf(specification.args())),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("List<String> dirs"),
                                        new NameExpr(listOf(specification.dirs())),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("Map<String, String> env"),
                                        new NameExpr(mapOf(specification.env())),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("var exitCode"),
                                        new NameExpr(String.valueOf(specification.exitCode())),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("var stderr"),
                                        new NameExpr(
                                                javaString(
                                                        requireNonNullElse(
                                                                specification.stderr(), ""))),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new AssignExpr(
                                        new NameExpr("var stdout"),
                                        new NameExpr(
                                                javaString(
                                                        requireNonNullElse(
                                                                specification.stdout(), ""))),
                                        AssignExpr.Operator.ASSIGN))
                        .addStatement(
                                new NameExpr(
                                        "WasiTestRunner.execute(test, args, dirs, env, exitCode,"
                                                + " stderr, stdout)"));
            }

            dest.add(cu);
        }
        // write the test classes
        dest.saveAll();

        // add generated sources to the project
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }

    private static Specification readSpecification(File json) throws MojoExecutionException {
        if (!json.isFile()) {
            return Specification.createDefault();
        }
        try {
            return new ObjectMapper().readValue(json, Specification.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read specification file: " + json, e);
        }
    }

    private static String listOf(List<String> list) {
        return "List.of("
                + list.stream().map(WasiTestGenMojo::javaString).collect(Collectors.joining(", "))
                + ")";
    }

    private static String mapOf(Map<String, String> map) {
        return "Map.of("
                + map.entrySet().stream()
                        .map(
                                entry ->
                                        javaString(entry.getKey())
                                                + ", "
                                                + javaString(entry.getValue()))
                        .collect(Collectors.joining(", "))
                + ")";
    }

    private static String javaString(String value) {
        return "\"" + escapeJava(value) + "\"";
    }

    private String relativePath(File file) {
        return file.getAbsolutePath()
                .replace(project.getBasedir().getAbsolutePath() + File.separator, "")
                .replace("\\", "\\\\"); // Win compat
    }
}
