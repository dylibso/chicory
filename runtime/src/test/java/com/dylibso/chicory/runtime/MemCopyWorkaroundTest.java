package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MemCopyWorkaroundTest {

    @Test
    public void shouldReturnOnJavaVersion() {
        var result = MemCopyWorkaround.shouldUseMemWorkaround("1.8");
        assertTrue(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("11");
        assertTrue(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("17");
        assertTrue(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("21");
        assertFalse(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("25");
        assertFalse(result);
    }

    @Test
    public void shouldReturnOnEdgeCases() {
        var result = MemCopyWorkaround.shouldUseMemWorkaround(null);
        assertFalse(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("");
        assertFalse(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("0");
        assertFalse(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("000");
        assertTrue(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("00000");
        assertTrue(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("11-ea");
        assertTrue(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("25-ea");
        assertFalse(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("123-$%^&");
        assertFalse(result);
        result = MemCopyWorkaround.shouldUseMemWorkaround("$%^&-123");
        assertFalse(result);
    }
}
