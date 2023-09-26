package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WasmValueType {
    @JsonProperty("i32")
    I32("i32"),
    @JsonProperty("i64")
    I64("i64"),
    @JsonProperty("f32")
    F32("f32"),
    @JsonProperty("f64")
    F64("f64");

    private String value;
    WasmValueType(String value) {
        this.value = value;
    }

    @JsonValue()
    public String getValue() {
        return value;
    }
}
