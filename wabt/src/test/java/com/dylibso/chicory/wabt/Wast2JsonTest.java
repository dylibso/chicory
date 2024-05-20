package com.dylibso.chicory.wabt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Wast2JsonTest {

    @Test
    public void shouldRunWast2Json(@TempDir Path tempDir) throws Exception {
        // Arrange
        var outputFile = tempDir.resolve("fac").resolve("spec.json").toFile();
        var wast2Json =
                Wast2Json.builder()
                        .withFile(new File("src/test/resources/fac.wast"))
                        .withOutput(outputFile)
                        .build();

        // Act
        wast2Json.process();

        System.out.println(outputFile.getAbsolutePath());
        // Assert
        assertTrue(outputFile.exists());
        assertTrue(outputFile.toPath().getParent().resolve("spec.0.wasm").toFile().exists());
    }
}
