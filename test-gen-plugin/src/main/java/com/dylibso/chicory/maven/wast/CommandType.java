package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CommandType {
    @JsonProperty("module")
    MODULE("module"),
    @JsonProperty("assert_return")
    ASSERT_RETURN("assert_return"),

    @JsonProperty("assert_invalid")
    ASSERT_INVALID("assert_invalid");

    private String value;
    CommandType(String value) {
        this.value = value;
    }

    @JsonValue()
    public String getValue() {
        return value;
    }
}
