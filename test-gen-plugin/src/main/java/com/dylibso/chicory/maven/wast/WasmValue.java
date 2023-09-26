package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WasmValue {

    @JsonProperty("type")
    WasmValueType type;

    @JsonProperty("value")
    String value;

    public WasmValueType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
