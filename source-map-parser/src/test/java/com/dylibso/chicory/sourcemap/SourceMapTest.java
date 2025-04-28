package com.dylibso.chicory.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.junit.jupiter.api.Test;

public class SourceMapTest {

    @Test
    public void shouldParseCountVowels() throws Exception {
        var result =
                SourceMapParser.parse(
                        getClass().getResourceAsStream("/compiled/count_vowels.rs.wasm"));
        assertNotNull(result);
        assertEquals(21227, result.entries().size());
        var entry = result.entries().get(200);
        assertEquals("/rust/deps/dlmalloc-0.2.7/src/dlmalloc.rs", entry.path());
        assertEquals(10953, entry.address());
        assertEquals(673, entry.line());
    }

    @Test
    public void shouldParseWasmSourceMap() throws Exception {
        // this file does not contain debug info
        var result = SourceMapParser.parse(new File("./src/main/wasm/wasm-source-map.wasm"));
        assertNotNull(result);
        assertEquals(0, result.entries().size());
    }
}
