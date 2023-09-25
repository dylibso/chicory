package com.dylibso.chicory.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.SourceRoot;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This plugin should generate the testsuite out of wast files
 */
@Mojo(name = "wasm-test-gen", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class TestGenMojo extends AbstractMojo {
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

    @Parameter(required = true, defaultValue = "${project.build.directory}/json")
    private File jsonTargetFolder;

    @Parameter(required = true, defaultValue = "${project.build.directory}/wabt")
    private File downloadTargetFolder;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void execute() throws MojoExecutionException {
        JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());

        try {
            generateTests(testsuiteFolder, sourceDestinationFolder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File downloadWabt(URL url) {
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

    private void generateTests(File testsuiteFolder, File sourceDestinationFolder) throws Exception {
        // First download wast2json if not already available
        // add an option to use a system-provided wasm2json binary

        // download the binary
        var wabtVersion = "1.0.33";
        var fileName = "wabt-" + wabtVersion + "-macos-12.tar.gz";
        var wabtRelease = "https://github.com/WebAssembly/wabt/releases/download/" + wabtVersion + "/" + fileName;
        downloadWabt(new URL(wabtRelease));

        var extractor = new TarExtractor(new FileInputStream(downloadTargetFolder.toPath().resolve(fileName).toFile().getAbsolutePath()), downloadTargetFolder.toPath());
        var binaryName = "wast2json";
        extractor.untar();
        var binary = downloadTargetFolder.toPath().resolve("wabt-" + wabtVersion).resolve("bin").resolve(binaryName);
        // Set executable
        binary.toFile().setExecutable(true, false);

        jsonTargetFolder.mkdirs();

        var exampleTargetJson = jsonTargetFolder.toPath().resolve("binary.json").toAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(List.of(
                binary.toAbsolutePath().toString(),
                testsuiteFolder.toPath().resolve("binary.wast").toAbsolutePath().toString(),
                "-o",
                exampleTargetJson.toString()
                ));
        pb.directory(new File("."));
        pb.inheritIO();
        var ps = pb.start();
        ps.waitFor(10, TimeUnit.SECONDS);

        // Extract the Json
        var jsonTree = mapper.readTree(exampleTargetJson.toFile());
        var exampleCommand = jsonTree.path("commands").get(0).path("filename").asText();

        // Generate the Java code
        sourceDestinationFolder.mkdirs();
        final SourceRoot dest = new SourceRoot(sourceDestinationFolder.toPath());
        var cu = new CompilationUnit("com.dylibso.chicory.gen");
        cu.setStorage(sourceDestinationFolder.toPath().resolve("ExampleTest.java"));

        cu.addImport("org.junit.Test");

        var testClass = cu.addClass("ExampleTest");
        var test1 = testClass.addMethod("exampleTest", Modifier.Keyword.PUBLIC);

        test1.addAnnotation("Test");
        test1.setBody(new BlockStmt().addStatement("System.out.println(\"Go on: " + exampleCommand + "\");"));

        dest.add(cu);
        dest.saveAll();
        project.addTestCompileSourceRoot(sourceDestinationFolder.getAbsolutePath());
    }
}
