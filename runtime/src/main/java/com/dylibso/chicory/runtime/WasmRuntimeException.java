package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

public class WasmRuntimeException extends ChicoryException {
    public WasmRuntimeException(String msg) {
        super(msg);
    }

    public WasmRuntimeException(Throwable cause) {
        super(cause);
    }

    public WasmRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
