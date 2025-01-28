package com.dylibso.chicory.testgen.wast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

@JsonIgnoreProperties
public class Action {

    @JsonProperty("type")
    private ActionType type;

    @JsonProperty("module")
    private String module;

    @JsonProperty("field")
    private String field;

    @JsonProperty("args")
    private WasmValue[] args;

    public ActionType type() {
        return type;
    }

    public String module() {
        return module;
    }

    public String field() {
        return field;
    }

    public WasmValue[] args() {
        return args;
    }

    @Override
    public String toString() {
        return "Action{"
                + "type="
                + type
                + ", module='"
                + module
                + '\''
                + ", field='"
                + field
                + '\''
                + ", args="
                + Arrays.toString(args)
                + '}';
    }
}
