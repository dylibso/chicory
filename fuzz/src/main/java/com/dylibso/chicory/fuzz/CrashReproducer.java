package com.dylibso.chicory.fuzz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public final class CrashReproducer {

    private CrashReproducer() {}

    private static final Path CRASH_DIR = Path.of("target/crash-reproducers");

    public static void save(
            File sourceWasm,
            String instructionType,
            String functionName,
            String oracleResult,
            String subjectResult)
            throws IOException {

        var hash = shortHash(sourceWasm.getAbsolutePath() + functionName);
        var folderName = "crash-" + instructionType + "-" + hash;
        var crashDir = CRASH_DIR.resolve(folderName);
        Files.createDirectories(crashDir);

        // Copy the wasm binary
        Files.copy(
                sourceWasm.toPath(),
                crashDir.resolve("test.wasm"),
                StandardCopyOption.REPLACE_EXISTING);

        // Copy seed file if present
        var seedFile = sourceWasm.toPath().getParent().resolve("seed.txt");
        if (Files.exists(seedFile)) {
            Files.copy(seedFile, crashDir.resolve("seed.txt"), StandardCopyOption.REPLACE_EXISTING);
        }

        // Write metadata
        var props = new Properties();
        props.setProperty("instructionType", instructionType);
        props.setProperty("functionName", functionName);
        props.setProperty("oracleResult", oracleResult != null ? oracleResult : "");
        props.setProperty("subjectResult", subjectResult != null ? subjectResult : "");
        try (var out = new FileOutputStream(crashDir.resolve("crash-info.properties").toFile())) {
            props.store(out, "Fuzz crash reproducer");
        }
    }

    private static String shortHash(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hashBytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
