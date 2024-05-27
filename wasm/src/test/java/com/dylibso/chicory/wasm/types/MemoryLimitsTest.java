package com.dylibso.chicory.wasm.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.wasm.exceptions.InvalidException;
import org.junit.jupiter.api.Test;

class MemoryLimitsTest {

    @Test
    public void shouldCreateDefaultMemoryLimits() {
        MemoryLimits defaults = MemoryLimits.defaultLimits();
        assertNotNull(defaults);
        assertEquals(0, defaults.initialPages());
        assertEquals(MemoryLimits.MAX_PAGES, defaults.maximumPages());
    }

    @Test
    public void shouldThrowOnInvalidMemoryLimits() {
        assertThrows(InvalidException.class, () -> new MemoryLimits(-1, -1));
        assertThrows(InvalidException.class, () -> new MemoryLimits(0, -1));
        assertThrows(InvalidException.class, () -> new MemoryLimits(2, 1));
        assertThrows(InvalidException.class, () -> new MemoryLimits(2, Integer.MAX_VALUE));
    }
}
