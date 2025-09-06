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
    EXN_REF("exnref"),
    @JsonProperty("structref")
    STRUCT_REF("structref"),
    @JsonProperty("anyref")
    ANY_REF("anyref"),
    @JsonProperty("nullref")
    NULL_REF("nullref"),
    @JsonProperty("nullfuncref")
    NULL_FUNC_REF("nullfuncref"),
    @JsonProperty("nullexternref")
    NULL_EXTERN_REF("nullexternref"),
    @JsonProperty("arrayref")
    ARRAY_REF("arrayref"),
    @JsonProperty("eqref")
    EQ_REF("eqref"),
    @JsonProperty("i31ref")
    I31_REF("i31ref");

    private final String value;

    WasmValueType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
