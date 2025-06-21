package com.dylibso.chicory.dwarf.rust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dylibso.chicory.runtime.internal.smap.SmapStratum;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.io.StringWriter;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class RustParserTest {

    @Test
    public void shouldParseCountVowels() throws Exception {
        var module =
                Parser.parse(
                        RustParserTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));
        var result = (SmapStratum) DebugParser.parse(module);
        assertNotNull(result);

        // Just want peek at an entry and make sure it values look like
        // what you would be normal for a source line...
        var entry = result.lineData().get(200);

        var writer = new StringWriter();
        writer.append("input file=").append(result.getPath(entry.lineFileID())).append("\n");
        writer.append("input line=").append(String.valueOf(entry.inputStartLine())).append("\n");
        writer.append("output line=").append(String.valueOf(entry.outputStartLine())).append("\n");

        Approvals.verify(writer.toString());
    }

    @Test
    public void shouldGetSourceResult() throws Exception {
        var result =
                DebugParser.getSourceResult(
                        RustParserTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));
        assertNotNull(result);
        assertNull(result.error);
        assertFalse(result.units.isEmpty());
        assertFalse(result.lines.isEmpty());
        assertFalse(result.functions.isEmpty());
    }

    @Test
    public void shouldParseWasmSourceInfo() throws Exception {

        // this file does not contain debug info
        var module = Parser.parse(new File("./rust/target/wasm32-wasip1/release/dwarf-rust.wasm"));
        var result = (SmapStratum) DebugParser.parse(module);
        assertNotNull(result);
        assertEquals(0, result.lineData().size());
    }
}
