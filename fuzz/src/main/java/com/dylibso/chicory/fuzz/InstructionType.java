package com.dylibso.chicory.fuzz;

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

    public String value() {
        return value;
    }
}
