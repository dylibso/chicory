package com.dylibso.chicory.tools.wasm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WasmToolsTest {

    @Test
    public void shouldRunWast2Json(@TempDir Path tempDir) throws Exception {
        // Arrange
        var outputFile = tempDir.resolve("fac").toFile();
        var wast2Json =
                Wast2Json.builder()
                        .withFile(new File("src/test/resources/fac.wast"))
                        .withOutput(outputFile)
                        .build();

        // Act
        wast2Json.process();

        // Assert
        assertTrue(outputFile.exists());
        assertTrue(outputFile.toPath().resolve("spec.0.wasm").toFile().exists());
    }
}
