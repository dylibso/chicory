package com.dylibso.chicory.runtime.exceptions;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

public class WASMRuntimeException extends ChicoryException {
    public WASMRuntimeException(String msg) {
        super(msg);
    }
    public WASMRuntimeException(Throwable cause) {
        super(cause);
    }
    public WASMRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
