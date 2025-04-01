package com.dylibso.chicory.runtime.alloc;

import static com.dylibso.chicory.runtime.Memory.PAGE_SIZE;
import static com.dylibso.chicory.runtime.Memory.RUNTIME_MAX_PAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MemAllocStrategyTest {

    @Test
    public void basicFunctionality() {
        // Arrange
        var memAlloc = new DefaultMemAllocStrategy(10);

        // Act
        var result = memAlloc.next(2, 3);

        // Assert
        assertTrue(result >= 3);
    }

    @Test
    public void avoidOverflows() {
        // Arrange
        var memAlloc = new DefaultMemAllocStrategy(2147418112);

        // Act
        var result = memAlloc.next(1610612736, 1610678272);

        // Assert
        assertEquals(2147418112, result);
    }

    @Test
    public void exhaustiveGrowing() {
        var maxBytes = RUNTIME_MAX_PAGES * PAGE_SIZE;
        var memAlloc = new DefaultMemAllocStrategy(maxBytes);
        var steps =
                new int[] {
                    1,
                    maxBytes / 10000,
                    maxBytes / 1000,
                    maxBytes / 100,
                    maxBytes / 10,
                    maxBytes / 3,
                    maxBytes / 2,
                    maxBytes,
                    maxBytes + 1
                };
        var current = 0;

        for (int i = 0; i < steps.length; i++) {
            var target = steps[i];
            var result = memAlloc.next(current, target);
            current = target;

            assertTrue(result > 0);
            assertTrue(
                    result >= Math.min(target, maxBytes),
                    "Result is: " + result + " but target is: " + target);
        }
    }
}
