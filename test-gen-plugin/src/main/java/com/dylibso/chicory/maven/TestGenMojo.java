package com.dylibso.chicory.maven;

import com.dylibso.chicory.maven.wast.Command;
import com.dylibso.chicory.maven.wast.CommandType;
import com.dylibso.chicory.maven.wast.Wast;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.utils.SourceRoot;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Parameter(required = true, defaultValue = "return.wast")
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
            generateTests(testsuiteFolder, sourceDestinationFolder);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException("Failed to execute wast2json program.");
        }

        return targetFolder;
    }

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
        var testName = name.substring(0, 1).toUpperCase() + name.substring(1) + "Test";
        cu.setStorage(sourceDestinationFolder.toPath().resolve(testName + ".java"));

        // default imports
        cu.addImport("org.junit.Test");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.Module");
        cu.addImport("org.junit.Assert.assertEquals", true, false);

        var testClass = cu.addClass(testName);

        Expression moduleInstantiation = null;
        MethodDeclaration method = null;
        int testNumber = 0;
        for (var cmd: wast.getCommands()) {
            log.error("****");
            log.error(cmd.toString());
            switch (cmd.getType()) {
                case MODULE:
                    moduleInstantiation = generateModuleInstantiation(cmd, targetFolder);
                    method = testClass.addMethod("test" + testNumber++, Modifier.Keyword.PUBLIC);
                    method.addAnnotation("Test");
                    break;
                case ASSERT_RETURN:
                    if (moduleInstantiation != null) {
                        // initialize
                        method.setBody(new BlockStmt().addStatement(
                                new AssignExpr(
                                        new VariableDeclarationExpr(
                                                new ClassOrInterfaceType("Instance"), "instance"), moduleInstantiation, AssignExpr.Operator.ASSIGN)));
                        moduleInstantiation = null;
                    }

                    method.getBody().get().addStatement(generateAssert(cmd));
                    break;
                case ASSERT_INVALID:
                    // TODO: generate assertThrows
                    break;
            }
        }

        dest.add(cu);
        dest.saveAll();
    }

    private Statement generateAssert(Command cmd) {
        assert(cmd.getType() == CommandType.ASSERT_RETURN);
        // TODO: implement me

        var returnVar = "null";
        if (cmd.getExpected() != null && cmd.getExpected().length > 0) {
            // TODO: emit the correct value
            if (cmd.getExpected().length == 1) {
                var value = cmd.getExpected()[0];
                // TODO: go on from here unnwrapping primitive types
            } else {
                throw new RuntimeException("Not implemented yet");
            }
        }

        String invocationMethod = null;
        switch (cmd.getAction().getType()) {
            case INVOKE:
                invocationMethod = ".apply()";
                break;
        }
        return new ExpressionStmt(new NameExpr("assertEquals(" + returnVar + ", instance.getExport(\"" + cmd.getAction().getField() +"\")" + invocationMethod + ")"));
    }

    private Expression generateModuleInstantiation(Command cmd, File folder) {
        assert(cmd.getType() == CommandType.MODULE);

        var relativeFile = folder.toPath().resolve(cmd.getFilename()).toFile().getAbsolutePath()
                .replaceFirst(project.getBasedir().getAbsolutePath() + "/", "");

        return new NameExpr("Module.build(\"" + relativeFile + "\").instantiate()");
    }

    private void generateTests(File testsuiteFolder, File sourceDestinationFolder) throws Exception {
        var wast2JsonBinary = getWast2Json();

        compiledWastTargetFolder.mkdirs();
        sourceDestinationFolder.mkdirs();

        for (var spec: wastToProcess) {
            var destFolder = executeWast2Json(wast2JsonBinary, testsuiteFolder.toPath().resolve(spec).toFile());
            generateJava(destFolder.toPath().resolve(SPEC_JSON).toFile(), destFolder);
        }

        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }
}
