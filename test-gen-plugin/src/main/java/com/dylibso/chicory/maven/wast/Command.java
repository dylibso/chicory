package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

@JsonIgnoreProperties
public class Command {

    @JsonProperty("type")
    private CommandType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("line")
    private int line;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("module_type")
    private String moduleType;

    @JsonProperty("action")
    private Action action;

    @JsonProperty("expected")
    private WasmValue[] expected;

    @JsonProperty("text")
    private String text;

    @JsonProperty("as")
    private String as;

    @Override
    public String toString() {
        return "Command{"
                + "type="
                + type
                + ", name="
                + name
                + ", line="
                + line
                + ", filename='"
                + filename
                + '\''
                + ", action="
                + action
                + ", expected="
                + Arrays.toString(expected)
                + ", text='"
                + text
                + '\''
                + ", moduleType='"
                + moduleType
                + '\''
                + '}';
    }

    public CommandType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public int line() {
        return line;
    }

    public String filename() {
        return filename;
    }

    public Action action() {
        return action;
    }

    public WasmValue[] expected() {
        return expected;
    }

    public String text() {
        return text;
    }

    public String moduleType() {
        return moduleType;
    }
}
