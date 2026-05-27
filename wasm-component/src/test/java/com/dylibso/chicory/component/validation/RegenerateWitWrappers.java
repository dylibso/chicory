package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.WitParser;
import com.dylibso.chicory.component.codegen.WitToJavaGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility to manually regenerate Java wrapper classes from WIT files.
 * <p>
 * This tool scans for .wit files and generates corresponding Java classes
 * (Person.java, Color.java, OperationResult.java, ExampleComponent.java, etc).
 *
 * <p>
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.dylibso.chicory.component.validation.RegenerateWitWrappers"
 * <p>
 * Or run directly from IDE (right-click → Run main() on this class).
 * <p>
 * Generated files are written to: wasm-component/src/test/java/com/example/generated/
 */
public class RegenerateWitWrappers {

    private static final String WIT_DIRECTORY = "example";
    private static final String OUTPUT_ROOT = "wasm-component/src/test/java";
    private static final String PACKAGE_NAME = "com.example.generated";

    public static void main(String[] args) {
        System.out.println("=== WIT Wrapper Regenerator ===\n");
        System.out.println("Scanning for WIT files in: " + WIT_DIRECTORY);
        System.out.println("Output root: " + OUTPUT_ROOT);
        System.out.println("Package name: " + PACKAGE_NAME);
        System.out.println();

        try {
            List<Path> witFiles = findWitFiles(WIT_DIRECTORY);

            if (witFiles.isEmpty()) {
                System.out.println("❌ No .wit files found in " + WIT_DIRECTORY);
                return;
            }

            System.out.println("Found " + witFiles.size() + " WIT file(s):");
            for (Path witFile : witFiles) {
                System.out.println("  - " + witFile);
            }
            System.out.println();

            int successCount = 0;
            int failureCount = 0;

            for (Path witFile : witFiles) {
                try {
                    System.out.println("Regenerating from: " + witFile.getFileName());
                    regenerateFromWit(witFile);
                    System.out.println("  ✅ Success\n");
                    successCount++;
                } catch (Exception e) {
                    System.out.println("  ❌ Failed: " + e.getMessage());
                    e.printStackTrace();
                    System.out.println();
                    failureCount++;
                }
            }

            System.out.println("=== Summary ===");
            System.out.println("Successful: " + successCount);
            System.out.println("Failed: " + failureCount);

            if (failureCount == 0) {
                System.out.println("\n✅ All wrapper classes regenerated successfully!");
                String outputPath = OUTPUT_ROOT + "/" + PACKAGE_NAME.replace(".", "/");
                System.out.println("Generated files are in: " + outputPath);
            }

        } catch (Exception e) {
            System.out.println("❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Find all .wit files in the given directory.
     */
    private static List<Path> findWitFiles(String directory) throws IOException {
        List<Path> witFiles = new ArrayList<>();
        Path dir = Paths.get(directory);

        if (!Files.exists(dir)) {
            throw new IOException("Directory not found: " + dir.toAbsolutePath());
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> p.toString().endsWith(".wit")).forEach(witFiles::add);
        }

        return witFiles;
    }

    /**
     * Regenerate Java wrapper classes from a single WIT file.
     */
    private static void regenerateFromWit(Path witFile) throws IOException {
        String witSource = Files.readString(witFile);

        WitParser parser = new WitParser();
        ComponentDefinition componentDef = parser.parse(witSource);

        // Note: WitToJavaGenerator creates the package directory structure,
        // so we pass the output ROOT, not the package directory
        Path outputRoot = Paths.get(OUTPUT_ROOT);
        Files.createDirectories(outputRoot);

        WitToJavaGenerator generator =
                new WitToJavaGenerator(componentDef, outputRoot, PACKAGE_NAME);
        generator.generate();
    }
}
