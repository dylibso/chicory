package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

@JsonIgnoreProperties
public class Action {
    @JsonProperty("type")
    private ActionType type;

    @JsonProperty("field")
    String field;

    @JsonProperty("args")
    WasmValue[] args;

    public Action() {}

    public ActionType getType() {

        return type;
    }

    public String getField() {
        return field;
    }

    public WasmValue[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "Action{"
                + "type="
                + type
                + ", field='"
                + field
                + '\''
                + ", args="
                + Arrays.toString(args)
                + '}';
    }
}
