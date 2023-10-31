package com.dylibso.chicory.testing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class WasmObjDumpTool {

    private static final String TOOL_BINARY = "wasm-objdump";

    private static final String TOOL_BINARY_WIN = TOOL_BINARY + ".exe";

    private static final String TOOL_BINARY_PATH = tryResolveToolBinaryPath().orElse(null);

    private WasmObjDumpTool() {
        // prevent instantiation
    }

    public static void dump(String wasmFilePath, String symbolFilter) {

        if (TOOL_BINARY_PATH == null) {
            // no tool binary found, skip object dump.
            return;
        }

        if (wasmFilePath == null) {
            System.err.printf(
                    "WasmFilePath missing. Wasm file must be present to run %s.%n", TOOL_BINARY);
            return;
        }

        System.out.printf("Module: %s%n", wasmFilePath);
        System.out.printf("Filter: %s%n", symbolFilter == null ? "NONE" : symbolFilter);
        var pb =
                new ProcessBuilder(
                        TOOL_BINARY_PATH, "-d", Path.of(wasmFilePath).toFile().getAbsolutePath());
        pb.directory(new File("."));

        Process process;
        try {
            if (symbolFilter == null) {
                pb.inheritIO();
                process = pb.start();
            } else {
                process = pb.start();
                scrapeProcessOutputForSymbolAndPrintUntilEndOfBlock(process, symbolFilter);
            }
            process.waitFor(10, TimeUnit.SECONDS);

            if (process.exitValue() != 0) {
                System.err.printf("%s exiting with: %s%n", TOOL_BINARY, process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute wasm-objdump program.", e);
        }
    }

    private static void scrapeProcessOutputForSymbolAndPrintUntilEndOfBlock(
            Process process, String symbolFilter) {

        String filterPattern = "<" + symbolFilter + ">";
        String blockEndMarker = "| end";

        boolean inMatchedSymbol = false;
        try (var scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(filterPattern)) {
                    inMatchedSymbol = true;
                    System.out.println("--------------------------------------------");
                }
                if (inMatchedSymbol) {
                    System.out.println(line);
                    if (line.endsWith(blockEndMarker)) {
                        System.out.println("--------------------------------------------");
                        break;
                    }
                }
            }
        }
    }

    private static Optional<String> tryResolveToolBinaryPath() {

        // This requires the wabt binary tools to be downloaded to target/wabt
        Path toolPath = null;
        try (var paths =
                Files.find(
                        Path.of("target/wabt"),
                        4,
                        (path, attrs) -> {
                            String name = path.toFile().getName();
                            return !attrs.isDirectory()
                                    && (name.equals(TOOL_BINARY) || name.equals(TOOL_BINARY_WIN));
                        })) {
            toolPath = paths.findFirst().orElse(null);
        } catch (IOException ex) {
            System.err.printf(
                    "Error while searching for %s. error=%s%n", TOOL_BINARY, ex.getMessage());
        }

        if (toolPath == null) {
            System.err.printf("Could not find %s tool. Skipping dump generation.%n", TOOL_BINARY);
            return Optional.empty();
        }

        File toolFile = toolPath.toFile();
        System.out.printf("Found %s in %s%n", TOOL_BINARY, toolPath);
        if (!toolFile.setExecutable(true, false)) {
            System.err.printf("Couldn't change file to be executable: %s%n", toolPath);
        }

        return Optional.of(toolFile.getAbsolutePath());
    }
}
