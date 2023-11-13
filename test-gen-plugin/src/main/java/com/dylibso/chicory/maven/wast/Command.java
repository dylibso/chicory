package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

@JsonIgnoreProperties
public class Command {

    @JsonProperty("type")
    private CommandType type;

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

    public CommandType getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public String getFilename() {
        return filename;
    }

    public Action getAction() {
        return action;
    }

    public WasmValue[] getExpected() {
        return expected;
    }

    public String getText() {
        return text;
    }

    public String getModuleType() {
        return moduleType;
    }
}
