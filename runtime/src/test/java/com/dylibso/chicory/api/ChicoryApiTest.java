package com.dylibso.chicory.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ChicoryApiTest {

    @WasmModule("wasm/count_vowels.rs.wasm")
    public interface HelloWorld {

        @WasmFunction("count_vowels")
        int countVowels(String input);
    }

    @Test
    public void shouldCreateProxy() {

        HelloWorld hw = Chicory.proxy(HelloWorld.class);

        assertNotNull(hw);
        assertEquals(3, hw.countVowels("AAAx"));

        assertEquals(hw, hw);
        assertEquals(hw.hashCode(), hw.hashCode());
        assertNotNull(hw.toString());
    }
}
