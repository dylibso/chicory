package com.dylibso.chicory.fuzz;

import java.util.HashMap;
import java.util.Map;

public enum InstructionType {
    NUMERIC("numeric"),
    VECTOR("vector"),
    CONTROL("control"),
    MEMORY("memory"),
    REFERENCE("reference"),
    PARAMETRIC("parametric"),
    VARIABLE("variable"),
    TABLE("table");

    private final String value;

    InstructionType(String value) {
        this.value = value;
    }

    public static InstructionType byValue(String value) {
        return byValue.get(value);
    }

    private static final Map<String, InstructionType> byValue =
            new HashMap<>(InstructionType.values().length);

    static {
        for (InstructionType i : InstructionType.values()) byValue.put(i.value(), i);
    }

    public String value() {
        return value;
    }
}
