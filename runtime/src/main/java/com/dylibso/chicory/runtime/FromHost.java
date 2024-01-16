package com.dylibso.chicory.runtime;

public interface FromHost {
    enum FromHostType {
        FUNCTION,
        GLOBAL,
        MEMORY,
        TABLE;
    }

    String getModuleName();

    String getFieldName();

    FromHostType getType();

    boolean override();
}
