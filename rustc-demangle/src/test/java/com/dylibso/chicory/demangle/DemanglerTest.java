package com.dylibso.chicory.demangle;

import static com.dylibso.chicory.demangle.Demangler.demangle;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DemanglerTest {

    @Test
    public void basicDemanglingExamples() {
        // https://docs.rs/rustc-demangle/latest/rustc_demangle/#examples
        assertEquals("test", demangle("_ZN4testE"));
        assertEquals("foo::bar", demangle("_ZN3foo3barE"));
        assertEquals("foo", demangle("foo"));
    }
}
