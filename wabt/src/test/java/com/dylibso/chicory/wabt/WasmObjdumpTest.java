package com.dylibso.chicory.wabt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

public class WasmObjdumpTest {

    @Test
    public void shouldRunWasmObjdump() throws Exception {
        // Arrange
        var wasmObjdump =
                WasmObjdump.builder()
                        .withFile(
                                new File("../wasm-corpus/src/test/resources/compiled/basic.c.wasm"))
                        .withDisassemble(true)
                        .build();

        // Act
        var result = wasmObjdump.dump();

        // Assert
        assertTrue(result.length() > 0);
        assertTrue(new String(result).contains("basic.c.wasm:\tfile format wasm 0x1"));
        assertTrue(new String(result).contains("Code Disassembly:"));
    }
}
