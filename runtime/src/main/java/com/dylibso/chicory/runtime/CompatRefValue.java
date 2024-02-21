package com.dylibso.chicory.runtime;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Temporary class to allow testing with integer-based external refs to pass.
 * To be replaced with a test script-specific ref class.
 */
public final class CompatRefValue {
    public static final List<CompatRefValue> values =
            IntStream.range(0, 256).mapToObj(CompatRefValue::new).collect(Collectors.toList());

    private final int value;

    private CompatRefValue(final int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
