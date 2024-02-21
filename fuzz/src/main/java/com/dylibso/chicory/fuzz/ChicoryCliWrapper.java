package com.dylibso.chicory.fuzz;

public class ChicoryCliWrapper extends RuntimeCli {

    // Assumes that the `cli` subproject have been packaged
    public static final String BINARY_NAME = "../cli/target/chicory";

    public ChicoryCliWrapper() {
        super(BINARY_NAME, "chicory");
    }
}
