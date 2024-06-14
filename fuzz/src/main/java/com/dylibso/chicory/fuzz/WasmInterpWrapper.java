package com.dylibso.chicory.fuzz;

public class WasmInterpWrapper extends RuntimeCli {

    public static final String BINARY_NAME = "wasm-interp";

    public WasmInterpWrapper() {
        super(BINARY_NAME, "wasm-interp");
    }
}
