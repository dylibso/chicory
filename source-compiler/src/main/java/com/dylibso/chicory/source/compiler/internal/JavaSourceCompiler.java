package com.dylibso.chicory.source.compiler.internal;

import com.dylibso.chicory.wasm.ChicoryException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Compiles Java source files to bytecode using the standard JavaCompiler API.
 */
public final class JavaSourceCompiler {

    private JavaSourceCompiler() {}

    /**
     * Compile Java source files to a target directory.
     *
     * @param sourceFiles map of fully qualified class names to source code
     * @param targetDir directory where .class files will be written
     * @param classpath classpath for compilation (can include runtime dependencies)
     * @throws ChicoryException if compilation fails
     */
    public static void compile(Map<String, String> sourceFiles, Path targetDir, String classpath) {
        try {
            // Create target directory
            Files.createDirectories(targetDir);

            // Write source files to temporary directory
            Path tempSourceDir = Files.createTempDirectory("chicory-source-");
            try {
                List<File> sourceFileList = new ArrayList<>();
                for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
                    String className = entry.getKey();
                    String source = entry.getValue();

                    // Convert package name to directory structure
                    String packagePath = className.substring(0, className.lastIndexOf('.'));
                    Path packageDir =
                            tempSourceDir.resolve(packagePath.replace('.', File.separatorChar));
                    Files.createDirectories(packageDir);

                    // Write source file
                    String simpleName = className.substring(className.lastIndexOf('.') + 1);
                    Path sourceFile = packageDir.resolve(simpleName + ".java");
                    Files.writeString(sourceFile, source);
                    sourceFileList.add(sourceFile.toFile());
                }

                // Get Java compiler
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) {
                    throw new ChicoryException(
                            "No Java compiler available. Make sure you're running with a JDK, not a"
                                    + " JRE.");
                }

                StandardJavaFileManager fileManager =
                        compiler.getStandardFileManager(null, null, null);

                // Set output directory
                fileManager.setLocation(
                        StandardLocation.CLASS_OUTPUT, Arrays.asList(targetDir.toFile()));

                // Set classpath if provided
                if (classpath != null && !classpath.isEmpty()) {
                    List<File> classpathFiles = new ArrayList<>();
                    @SuppressWarnings("StringSplitter")
                    String[] entries = classpath.split(File.pathSeparator);
                    for (String cpEntry : entries) {
                        if (!cpEntry.isEmpty()) {
                            File cpFile = new File(cpEntry);
                            if (cpFile.exists()) {
                                classpathFiles.add(cpFile);
                            }
                        }
                    }
                    if (!classpathFiles.isEmpty()) {
                        fileManager.setLocation(StandardLocation.CLASS_PATH, classpathFiles);
                    }
                }

                // Get compilation units
                Iterable<? extends JavaFileObject> compilationUnits =
                        fileManager.getJavaFileObjectsFromFiles(sourceFileList);

                // Compile with diagnostics
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                JavaCompiler.CompilationTask task =
                        compiler.getTask(
                                null, fileManager, diagnostics, null, null, compilationUnits);

                boolean success = task.call();
                fileManager.close();

                if (!success) {
                    StringBuilder errorMsg = new StringBuilder("Compilation failed:\n");
                    for (Diagnostic<? extends JavaFileObject> diagnostic :
                            diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            errorMsg.append(diagnostic.getKind())
                                    .append(": ")
                                    .append(diagnostic.getMessage(null))
                                    .append(" at ")
                                    .append(diagnostic.getLineNumber())
                                    .append(":")
                                    .append(diagnostic.getColumnNumber())
                                    .append("\n");
                        }
                    }
                    throw new ChicoryException(errorMsg.toString());
                }
            } finally {
                // Clean up temp directory
                deleteRecursively(tempSourceDir);
            }
        } catch (IOException e) {
            throw new ChicoryException("Failed to compile Java sources", e);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .forEach(
                                p -> {
                                    try {
                                        Files.delete(p);
                                    } catch (IOException e) {
                                        // Ignore cleanup errors
                                    }
                                });
            }
        }
    }
}
