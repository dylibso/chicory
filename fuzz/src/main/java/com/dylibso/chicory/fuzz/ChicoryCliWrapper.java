package com.dylibso.chicory.fuzz;

import java.nio.file.Paths;

public class ChicoryCliWrapper extends RuntimeCli {

    // Assumes that the `cli` subproject have been packaged
    public static final String BINARY_NAME =
            Paths.get("..").resolve("cli").resolve("target").resolve("chicory").toString();

    public ChicoryCliWrapper() {
        super(BINARY_NAME, "chicory");
    }
}
