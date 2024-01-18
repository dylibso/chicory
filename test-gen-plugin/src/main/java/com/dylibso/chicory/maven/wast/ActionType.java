package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
    @JsonProperty("invoke")
    INVOKE("invoke"),
    @JsonProperty("get")
    GET("get");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }

    @JsonValue()
    public String getValue() {
        return value;
    }
}
