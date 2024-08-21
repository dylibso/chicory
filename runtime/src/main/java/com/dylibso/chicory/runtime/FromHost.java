package com.dylibso.chicory.runtime;

public interface FromHost {
    enum FromHostType {
        FUNCTION,
        GLOBAL,
        MEMORY,
        TABLE
    }

    String moduleName();

    String fieldName();

    FromHostType type();
}
