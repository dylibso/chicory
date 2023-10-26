package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;

public class Wast {

    @JsonProperty("source_filename")
    private File sourceFilename;

    @JsonProperty("commands")
    private Command[] commands;

    public File getSourceFilename() {
        return sourceFilename;
    }

    public Command[] getCommands() {
        return commands;
    }
}
