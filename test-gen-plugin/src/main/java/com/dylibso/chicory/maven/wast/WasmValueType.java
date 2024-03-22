package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WasmValueType {
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
    F64("f64"),
    @JsonProperty("externref")
    EXTERN_REF("externref"),
    @JsonProperty("funcref")
    FUNC_REF("funcref"),
    @JsonProperty("v128")
    VEC_REF("v128");

    private final String value;

    WasmValueType(String value) {
        this.value = value;
    }

    @JsonValue()
    public String value() {
        return value;
    }
}
