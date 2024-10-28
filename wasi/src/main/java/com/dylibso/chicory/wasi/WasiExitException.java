package com.dylibso.chicory.wasi;

import com.dylibso.chicory.wasm.ChicoryException;

public class WasiExitException extends ChicoryException {
    private final int exitCode;

    public WasiExitException(int exitCode) {
        super("Process exit code: " + exitCode);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }

    // no need to capture the Stack Trace
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
