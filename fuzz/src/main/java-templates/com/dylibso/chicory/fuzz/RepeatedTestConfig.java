package com.dylibso.chicory.fuzz;

// Using a java-template to be able to inject those values through java properties
public final class RepeatedTestConfig {
    public static final int FUZZ_TEST_NUMERIC = ${fuzz.test.numeric};
    public static final int FUZZ_TEST_TABLE = ${fuzz.test.table};
}
