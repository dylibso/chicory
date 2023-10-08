package com.dylibso.chicory.maven;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

/**
 * This plugin should generate the testsuite out of wast files
 */
@Mojo(name = "wasm-test-gen", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class TestGenMojo extends AbstractMojo {
    private final Log log = new SystemStreamLog();

    private static final String WAST2JSON = "wast2json";
    private static final String SPEC_JSON = "spec.json";

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
     * Wabt version
     */
    @Parameter(required = true, defaultValue = "1.0.33")
    private String wabtVersion;

    /**
     * Location for the junit generated sources.
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-test-sources/test-gen")
    private File sourceDestinationFolder;

    @Parameter(required = true, defaultValue = "${project.build.directory}/compiled-wast")
    private File compiledWastTargetFolder;

    @Parameter(required = true, defaultValue = "${project.build.directory}/wabt")
    private File downloadTargetFolder;

    @Parameter(required = true)
    private List<String> wastToProcess;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void execute() throws MojoExecutionException {
        JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());
        try {
            downloadTestsuite(testSuiteRepo, testSuiteRepoRef, testsuiteFolder);
            generateTests(testsuiteFolder, sourceDestinationFolder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadTestsuite(String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder) throws Exception {
        if (testSuiteFolder.exists() && testSuiteFolder.list((dir, name) -> name.endsWith(".wast")).length == 0) {
            log.warn("Testsuite folder exists but looks corrupted, replacing.");
            Files.walk(testSuiteFolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(x -> x.toFile())
                    .forEach(File::delete);
        }
        if (!testSuiteFolder.exists()) {
            log.warn("Cloning the testsuite.");
            Git.cloneRepository()
                    .setURI(testSuiteRepo)
                    .setDirectory(testSuiteFolder)
                    .setBranchesToClone(singleton("refs/heads/" + testSuiteRepoRef))
                    .setBranch("refs/heads/" + testSuiteRepoRef)
                    .call();
        }
    }

    public String wabtArchitectureName() {
        var osName = this.osName.toLowerCase(Locale.ROOT);
        if (osName.startsWith("mac") || osName.startsWith("osx")) {
            return "macos-12";
        } else if (osName.startsWith("windows")) {
            return "windows";
        } else if (osName.startsWith("linux")) {
            return "ubuntu";
        } else {
            throw new IllegalArgumentException("Detected OS is not supported: " + osName);
        }
    }

    private File download(URL url) {
        downloadTargetFolder.mkdirs();
        final File finalDestination = new File(downloadTargetFolder, new File(url.getFile()).getName());

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(finalDestination)) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return finalDestination;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error downloading : " + url, e);
        }
    }

    private String getWast2Json() {
        ProcessBuilder pb = new ProcessBuilder(WAST2JSON);
        pb.directory(new File("."));
        pb.inheritIO();
        Process ps = null;
        try {
            ps = pb.start();
            ps.waitFor(1, TimeUnit.SECONDS);
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }

        if (ps != null && ps.exitValue() == 0) {
            log.info(WAST2JSON + " binary detected available, using the system one");
            return WAST2JSON;
        }

        // Downloading locally WABT
        var binary = downloadTargetFolder.toPath().resolve("wabt-" + wabtVersion).resolve("bin").resolve(WAST2JSON);

        if (binary.toFile().exists()) {
            log.warn("cached `wast2json` exists trying to use it, please run `mvn clean` if this doesn't succeed");
            return binary.toFile().getAbsolutePath();
        }

        log.info("Cannot locate " + WAST2JSON + " binary, downloading");

        var fileName = "wabt-" + wabtVersion + "-" + wabtArchitectureName() + ".tar.gz";
        var wabtRelease = "https://github.com/WebAssembly/wabt/releases/download/" + wabtVersion + "/" + fileName;
        try {
            download(new URL(wabtRelease));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream fis = new FileInputStream(downloadTargetFolder.toPath().resolve(fileName).toFile().getAbsolutePath())) {
            new TarExtractor(fis, downloadTargetFolder.toPath()).untar();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set executable
        binary.toFile().setExecutable(true, false);
        return binary.toFile().getAbsolutePath();
    }

    private File executeWast2Json(String wast2jsonBinary, File wastFile) {
        var plainName = wastFile.getName().replace(".wast", "");
        var targetFolder = compiledWastTargetFolder.toPath().resolve(plainName).toFile();
        var destFile = targetFolder.toPath().resolve(SPEC_JSON).toFile();

        targetFolder.mkdirs();

        var command = List.of(
                wast2jsonBinary,
                wastFile.getAbsolutePath(),
                "-o",
                destFile.getAbsolutePath());
        log.info("Going to execute command: " + command.stream().collect(Collectors.joining(" ")));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File("."));
        pb.inheritIO();
        Process ps = null;
        try {
            ps = pb.start();
            ps.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (ps.exitValue() != 0) {
            System.err.println("wast2json exiting with:"+ ps.exitValue());
            System.err.println(ps.getErrorStream().toString());
            throw new RuntimeException("Failed to execute wast2json program.");
        }

        return targetFolder;
    }

    private String INSTANCE_NAME = "instance";

    private void generateJava(File specFile, File targetFolder) {
        Wast wast;
        try {
            wast = mapper.readValue(specFile, Wast.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());
        var cu = new CompilationUnit("com.dylibso.chicory.test.gen");
        var name = specFile.toPath().getParent().toFile().getName();
        var testName = "SpecV1" + capitalize(escapedCamelCase(name)) + "Test";
        cu.setStorage(sourceDestinationFolder.toPath().resolve(testName + ".java"));

        // all the imports
        cu.addImport("org.junit.Test");

        cu.addImport("com.dylibso.chicory.runtime.exceptions.InvalidException");
        cu.addImport("com.dylibso.chicory.runtime.exceptions.MalformedException");
        cu.addImport("com.dylibso.chicory.runtime.exceptions.WASMRuntimeException");
        cu.addImport("com.dylibso.chicory.runtime.ExportFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Module");
        cu.addImport("com.dylibso.chicory.runtime.ModuleType");

        cu.addImport("com.dylibso.chicory.wasm.types.Value");

        cu.addImport("org.junit.Assert.assertEquals", true, false);
        cu.addImport("org.junit.Assert.assertThrows", true, false);

        var testClass = cu.addClass(testName);

        MethodDeclaration method = null;
        int testNumber = 0;
        int moduleInstantiationNumber = 0;
        for (var cmd: wast.getCommands()) {
            switch (cmd.getType()) {
                case MODULE:
                    testClass.addFieldWithInitializer(
                            new ClassOrInterfaceType("Instance"),
                            INSTANCE_NAME + moduleInstantiationNumber++,
                            generateModuleInstantiation(cmd, targetFolder));
                    break;
                case ASSERT_RETURN:
                case ASSERT_TRAP:
                    method = testClass.addMethod("test" + testNumber++, Modifier.Keyword.PUBLIC);
                    method.addAnnotation("Test");

                    var varName = escapedCamelCase(cmd.getAction().getField());
                    var fieldExport = generateFieldExport(varName, cmd, (moduleInstantiationNumber - 1));
                    if (fieldExport.isPresent()) {
                        method.getBody().get().addStatement(fieldExport.get());
                    }

                    for (var expr: generateAssert(varName, cmd)) {
                        method.getBody().get().addStatement(expr);
                    }
                    break;
                case ASSERT_INVALID:
                case ASSERT_MALFORMED:
                    method = testClass.addMethod("test" + testNumber++, Modifier.Keyword.PUBLIC);
                    method.addAnnotation("Test");

                    for (var expr: generateAssertThrows(cmd, targetFolder)) {
                        method.getBody().get().addStatement(expr);
                    }
                    break;
            }
        }

        dest.add(cu);
        dest.saveAll();
    }

    private String capitalize(String in) {
        return in.substring(0, 1).toUpperCase() + in.substring(1);
    }

    private String escapedCamelCase(String in) {
        var escaped = StringEscapeUtils.escapeJava(in);
        var sb = new StringBuffer();
        var capitalize = false;
        for (var i = 0; i < escaped.length(); i++) {
            var character = escaped.charAt(i);

            if (Character.isDigit(character)) {
                sb.append(character);
            } else if (Character.isAlphabetic(character)) {
                if (capitalize) {
                    sb.append(Character.toUpperCase(character));
                    capitalize = false;
                } else {
                    sb.append(character);
                }
            } else {
                capitalize = true;
            }
        }

        return sb.toString();
    }

    private Optional<Expression> generateFieldExport(String varName, Command cmd, int instanceNumber) {
        if (cmd.getAction() != null && cmd.getAction().getField() != null) {
            var declarator = new VariableDeclarator()
                    .setName(varName)
                    .setType(new ClassOrInterfaceType("ExportFunction"))
                    .setInitializer(new NameExpr(INSTANCE_NAME + instanceNumber + ".getExport(\"" + cmd.getAction().getField() + "\")"));
            Expression varDecl = new VariableDeclarationExpr(declarator);
            return Optional.of(varDecl);
        } else {
            return Optional.empty();
        }
    }

    private List<Expression> generateAssert(String varName, Command cmd) {
        assert(cmd.getType() == CommandType.ASSERT_RETURN || cmd.getType() == CommandType.ASSERT_TRAP);

        var returnVar = "null";
        var typeConversion = "";
        var deltaParam = "";
        if (cmd.getExpected() != null && cmd.getExpected().length > 0) {
            if (cmd.getExpected().length == 1) {
                var expected = cmd.getExpected()[0];
                returnVar = expected.toJavaValue();
                typeConversion = expected.extractType();
                deltaParam = expected.getDelta();
            } else {
                throw new RuntimeException("Multiple expected return, implement me!");
            }
        }

        String invocationMethod = null;
        switch (cmd.getAction().getType()) {
            case INVOKE:
                var args = Arrays.stream(cmd.getAction().getArgs()).map(arg -> arg.toWasmValue()).collect(Collectors.joining(", "));
                invocationMethod = ".apply(" + args + ")";
                break;
        }

        switch (cmd.getType()) {
            case ASSERT_RETURN:
                return List.of(new NameExpr("assertEquals(" + returnVar + ", "+ varName + invocationMethod + typeConversion + deltaParam + ")"));
            case ASSERT_TRAP:
                var assertDecl = new NameExpr("var exception = assertThrows(WASMRuntimeException.class, () -> "+ varName + invocationMethod + typeConversion + ")");
                if (cmd.getText() != null) {
                    var messageMatch = new NameExpr("assertEquals(\"" + cmd.getText() + "\", exception.getMessage())");
                    return List.of(assertDecl, messageMatch);
                } else {
                    return List.of(assertDecl);
                }
        }

        throw new RuntimeException("Unreachable");
    }

    private Expression generateModuleInstantiation(Command cmd, File folder) {
        assert(cmd.getType() == CommandType.MODULE);

        var relativeFile = folder.toPath().resolve(cmd.getFilename()).toFile().getAbsolutePath()
                .replaceFirst(project.getBasedir().getAbsolutePath() + "/", "");

        var additionalParam = "";
        if (cmd.getModuleType() != null) {
            additionalParam = ", ModuleType." + cmd.getModuleType().toUpperCase();
        }
        return new NameExpr("Module.build(\"" + relativeFile + "\"" + additionalParam + ").instantiate()");
    }

    private List<Expression> generateAssertThrows(Command cmd, File targetFolder) {
        assert(cmd.getType() == CommandType.ASSERT_INVALID || cmd.getType() == CommandType.ASSERT_MALFORMED);

        var exceptionType = "";
        if (cmd.getType() == CommandType.ASSERT_INVALID) {
            exceptionType = "InvalidException";
        } else if (cmd.getType() == CommandType.ASSERT_MALFORMED) {
            exceptionType = "MalformedException";
        }

        var assignementStmt = (cmd.getText() != null) ? "var exception = " : "";

        var assertThrows = new NameExpr(assignementStmt + "assertThrows(" + exceptionType + ".class, () -> " + generateModuleInstantiation(cmd, targetFolder) + ")");

        if (cmd.getText() != null) {
            var messageMatch = new NameExpr("assertEquals(\"" + cmd.getText() + "\", exception.getMessage())");
            return List.of(assertThrows, messageMatch);
        } else {
            return List.of(assertThrows);
        }
    }

    private void generateTests(File testsuiteFolder, File sourceDestinationFolder) throws Exception {
        var wast2JsonBinary = getWast2Json();

        compiledWastTargetFolder.mkdirs();
        sourceDestinationFolder.mkdirs();

        for (var spec: wastToProcess) {
            var wastFile = testsuiteFolder.toPath().resolve(spec).toFile();
            if (!wastFile.exists()) {
                throw new IllegalArgumentException("Wast file " + wastFile.getAbsolutePath() + " not found");
            }
            var destFolder = executeWast2Json(wast2JsonBinary, testsuiteFolder.toPath().resolve(spec).toFile());
            generateJava(destFolder.toPath().resolve(SPEC_JSON).toFile(), destFolder);
        }

        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }
}
