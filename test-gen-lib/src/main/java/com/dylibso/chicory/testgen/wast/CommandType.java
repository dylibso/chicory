package com.dylibso.chicory.testgen.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CommandType {
    @JsonProperty("module")
    MODULE("module"),
    @JsonProperty("assert_return")
    ASSERT_RETURN("assert_return"),
    @JsonProperty("assert_trap")
    ASSERT_TRAP("assert_trap"),
    @JsonProperty("assert_invalid")
    ASSERT_INVALID("assert_invalid"),
    @JsonProperty("assert_malformed")
    ASSERT_MALFORMED("assert_malformed"),
    @JsonProperty("assert_uninstantiable")
    ASSERT_UNINSTANTIABLE("assert_uninstantiable"),
    @JsonProperty("assert_exhaustion")
    ASSERT_EXHAUSTION("assert_exhaustion"),
    @JsonProperty("assert_unlinkable")
    ASSERT_UNLINKABLE("assert_unlinkable"),
    @JsonProperty("action")
    ACTION("action"),
    @JsonProperty("register")
    REGISTER("register");

    private final String value;

    CommandType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
