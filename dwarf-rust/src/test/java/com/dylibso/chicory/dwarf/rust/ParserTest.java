package com.dylibso.chicory.dwarf.rust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.junit.jupiter.api.Test;

public class ParserTest {

    @Test
    public void shouldParseCountVowels() throws Exception {
        var result =
                RustParser.parse(
                        ParserTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));
        assertNotNull(result);
        assertEquals(21813, result.entries().size());

        // Just want peek at an entry and make sure it values look like
        // what you would be normal for a source line...
        var entry = result.entries().get(200);
        assertEquals(
                "/rustc/05f9846f893b09a1be1fc8560e33fc3c815cfecb/library/alloc/src/raw_vec.rs",
                entry.path());
        assertEquals(5085, entry.address());
        assertEquals(512, entry.line());
    }

    @Test
    public void shouldParseWasmSourceInfo() throws Exception {
        // this file does not contain debug info
        var result = RustParser.parse(new File("./src/main/wasm/wasm-source-map.wasm"));
        assertNotNull(result);
        assertEquals(0, result.entries().size());
    }
}
