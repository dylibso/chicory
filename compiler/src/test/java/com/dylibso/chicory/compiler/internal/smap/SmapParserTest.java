package com.dylibso.chicory.compiler.internal.smap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SmapParser to validate round-trip SMAP generation and parsing.
 */
public class SmapParserTest {

    @Test
    public void testBasicSmapRoundTrip() {
        // Create a basic SMAP
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("test.java");

        SmapStratum stratum = new SmapStratum("Java");
        stratum.addFile("test.wasm");
        stratum.addLineData(1, "test.wasm", 1, 1, 1);
        stratum.addLineData(2, "test.wasm", 1, 6, 1);

        originalGenerator.addStratum(stratum, true);

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testComplexSmapRoundTrip() {
        // Create a more complex SMAP with multiple files and line mappings
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("complex.java");

        SmapStratum stratum = new SmapStratum("JSP");
        stratum.addFile("foo.jsp");
        stratum.addFile("bar.jsp", "/foo/foo/bar.jsp");
        stratum.addLineData(1, "foo.jsp", 1, 1, 1);
        stratum.addLineData(2, "foo.jsp", 1, 6, 1);
        stratum.addLineData(3, "foo.jsp", 2, 10, 5);
        stratum.addLineData(20, "/foo/foo/bar.jsp", 1, 30, 1);

        originalGenerator.addStratum(stratum, true);

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testSmapWithEmbeddedContent() {
        // Create SMAP with embedded content
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("embedded.java");

        SmapStratum mainStratum = new SmapStratum("JSP");
        mainStratum.addFile("main.jsp");
        mainStratum.addLineData(1, "main.jsp", 1, 1, 1);
        originalGenerator.addStratum(mainStratum, true);

        // Add embedded SMAP
        SmapGenerator embeddedGenerator = new SmapGenerator();
        embeddedGenerator.setOutputFileName("embedded.tier2");
        SmapStratum embeddedStratum = new SmapStratum("Tier2");
        embeddedStratum.addFile("1.tier2");
        embeddedStratum.addLineData(1, "1.tier2", 1, 1, 1);
        embeddedGenerator.addStratum(embeddedStratum, true);

        originalGenerator.addSmap(embeddedGenerator.toString(), "JSP");

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testSmapWithOptimizedLineData() {
        // Create SMAP that would trigger line optimization
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("optimized.java");

        SmapStratum stratum = new SmapStratum("Java");
        stratum.addFile("test.wasm");

        // Add line data that could be optimized
        stratum.addLineData(1, "test.wasm", 1, 1, 1);
        stratum.addLineData(2, "test.wasm", 1, 2, 1);
        stratum.addLineData(3, "test.wasm", 1, 3, 1);
        stratum.addLineData(4, "test.wasm", 2, 4, 1);

        // Optimize the line section
        stratum.optimizeLineSection();

        originalGenerator.addStratum(stratum, true);

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testSmapWithMultipleStrata() {
        // Create SMAP with multiple strata
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("multistrata.java");

        // First stratum
        SmapStratum stratum1 = new SmapStratum("Java");
        stratum1.addFile("source1.wasm");
        stratum1.addLineData(1, "source1.wasm", 1, 1, 1);
        originalGenerator.addStratum(stratum1, true);

        // Second stratum
        SmapStratum stratum2 = new SmapStratum("Custom");
        stratum2.addFile("source2.custom");
        stratum2.addLineData(10, "source2.custom", 2, 20, 1);
        originalGenerator.addStratum(stratum2, false);

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testInvalidSmapParsing() {
        // Test various invalid SMAP strings
        assertThrows(IllegalArgumentException.class, () -> SmapParser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> SmapParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> SmapParser.parse("INVALID"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SmapParser.parse("NotSMAP\nfile.java\nJava\n*E\n"));
    }

    @Test
    public void testExampleFromSmapGenerator() {
        // Test the exact example from SmapGenerator.main() method
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("foo.java");
        SmapStratum s = new SmapStratum("JSP");
        s.addFile("foo.jsp");
        s.addFile("bar.jsp", "/foo/foo/bar.jsp");
        s.addLineData(1, "foo.jsp", 1, 1, 1);
        s.addLineData(2, "foo.jsp", 1, 6, 1);
        s.addLineData(3, "foo.jsp", 2, 10, 5);
        s.addLineData(20, "/foo/foo/bar.jsp", 1, 30, 1);
        originalGenerator.addStratum(s, true);

        // Add embedded SMAP
        SmapGenerator embedded = new SmapGenerator();
        embedded.setOutputFileName("blargh.tier2");
        s = new SmapStratum("Tier2");
        s.addFile("1.tier2");
        s.addLineData(1, "1.tier2", 1, 1, 1);
        embedded.addStratum(s, true);
        originalGenerator.addSmap(embedded.toString(), "JSP");

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testSmapParsingWithNoEmbedded() {
        // Test SMAP with embedded content disabled
        SmapGenerator originalGenerator = new SmapGenerator();
        originalGenerator.setOutputFileName("noembedded.java");
        originalGenerator.setDoEmbedded(false);

        SmapStratum stratum = new SmapStratum("Java");
        stratum.addFile("test.wasm");
        stratum.addLineData(1, "test.wasm", 1, 1, 1);
        originalGenerator.addStratum(stratum, true);

        // Add embedded SMAP (should be ignored)
        SmapGenerator embeddedGenerator = new SmapGenerator();
        embeddedGenerator.setOutputFileName("ignored.tier2");
        SmapStratum embeddedStratum = new SmapStratum("Tier2");
        embeddedStratum.addFile("ignored.tier2");
        embeddedStratum.addLineData(1, "ignored.tier2", 1, 1, 1);
        embeddedGenerator.addStratum(embeddedStratum, true);
        originalGenerator.addSmap(embeddedGenerator.toString(), "Java");

        // Generate SMAP string
        String smapString = originalGenerator.getString();
        assertNotNull(smapString);

        // Parse it back
        SmapGenerator parsedGenerator = SmapParser.parse(smapString);

        // Verify round-trip
        String reparsedSmapString = parsedGenerator.getString();
        assertEquals(smapString, reparsedSmapString);
    }

    @Test
    public void testRealWorldSmapExample() throws IOException {
        // Load the real SMAP example from resources
        try (InputStream is = getClass().getResourceAsStream("/example.smap")) {
            byte[] bytes = is.readAllBytes();
            String smapContent = new String(bytes, StandardCharsets.UTF_8);

            // Parse the SMAP
            SmapGenerator parsedGenerator = SmapParser.parse(smapContent);
            assertNotNull(parsedGenerator);

            assertEquals(smapContent, parsedGenerator.getString());
        }
    }
}
