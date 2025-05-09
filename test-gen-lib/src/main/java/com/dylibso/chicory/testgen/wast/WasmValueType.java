package com.dylibso.chicory.testgen.wast;

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
    F64("f64"),
    @JsonProperty("v128")
    V128("v128"),
    @JsonProperty("externref")
    EXTERN_REF("externref"),
    @JsonProperty("funcref")
    FUNC_REF("funcref"),
    @JsonProperty("refnull")
    REF_NULL("refnull"),
    @JsonProperty("exnref")
    EXN_REF("exnref");

    private final String value;

    WasmValueType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
