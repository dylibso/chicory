package com.dylibso.chicory.testgen.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;

public class Wast {

    @JsonProperty("source_filename")
    private File sourceFilename;

    @JsonProperty("commands")
    private Command[] commands;

    public File sourceFilename() {
        return sourceFilename;
    }

    public Command[] commands() {
        return commands;
    }
}
