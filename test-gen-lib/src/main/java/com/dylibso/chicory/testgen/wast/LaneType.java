package com.dylibso.chicory.testgen.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LaneType {
    @JsonProperty("i8")
    I8("i8"),
    @JsonProperty("i16")
    I16("i16"),
    @JsonProperty("i32")
    I32("i32"),
    @JsonProperty("i64")
    I64("i64"),
    @JsonProperty("f32")
    F32("f32"),
    @JsonProperty("f64")
    F64("f64");

    private final String value;

    LaneType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
