package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.FunctionType;

public class WasmException extends RuntimeException {
    private final FunctionType type;
    private final long[] args;

    public WasmException(FunctionType type, long[] args) {
        this.type = type;
        this.args = args.clone();
    }
}
