package com.dylibso.chicory.fuzz;

public class WasmTimeWrapper extends RuntimeCli {

    public static final String BINARY_NAME = "wasmtime";

    public WasmTimeWrapper() {
        super(BINARY_NAME, "wasmtime");
    }
}
