package com.dylibso.chicory.wasi;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

public class WasiExitException extends ChicoryException {
    private final int exitCode;

    public WasiExitException(int exitCode) {
        super("Process exit code: " + exitCode);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
