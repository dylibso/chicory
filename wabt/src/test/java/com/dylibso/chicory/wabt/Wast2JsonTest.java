package com.dylibso.chicory.wabt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Wast2JsonTest {

    @Test
    public void shouldRunWast2Json(@TempDir Path tempDir) throws Exception {
        // Arrange
        var outputFile = tempDir.resolve("fac").resolve("spec.json");
        var wast2Json =
                Wast2Json.builder()
                        .withFile(Path.of("src/test/resources/fac.wast"))
                        .withOutput(outputFile)
                        .build();

        // Act
        wast2Json.process();

        // Assert
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.exists(outputFile.getParent().resolve("spec.0.wasm")));
    }
}
