package com.dylibso.chicory.runtime.internal.smap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class SmapParserTest {

    @Test
    public void testParseSimpleExample() throws ParserException {
        // Example from JSR-045 specification
        String smap =
                "SMAP\n"
                        + "HelloServlet.java\n"
                        + "JSP\n"
                        + "*S JSP\n"
                        + "*F\n"
                        + "1 Hello.jsp\n"
                        + "2 greeting.jsp\n"
                        + "*L\n"
                        + "1#1,5:10,2\n"
                        + "1#2,2:20,2\n"
                        + "7#1,2:24,2\n"
                        + "*E\n";

        Smap generator = SmapParser.parse(smap);
        assertNotNull(generator);

        assertEquals(
                "SMAP\n"
                        + "HelloServlet.java\n"
                        + "JSP\n"
                        + "*S JSP\n"
                        + "*F\n"
                        + "0 Hello.jsp\n"
                        + "1 greeting.jsp\n"
                        + "*L\n"
                        + "1,5:10,2\n"
                        + "1#1,2:20,2\n"
                        + "7#0,2:24,2\n"
                        + "*E\n",
                generator.toString());
    }

    @Test
    public void testParseExampleSmap() throws ParserException, IOException {
        // Read the example.smap file from test resources
        try (var inputStream = getClass().getResourceAsStream("/example.smap")) {
            assertNotNull(inputStream, "example.smap resource not found");
            String actual = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            Smap generator = SmapParser.parse(actual);
            assertNotNull(generator);

            String regenerated = generator.toString();
            assertNotNull(regenerated);
            assertEquals(actual, regenerated);
        }
    }
}
