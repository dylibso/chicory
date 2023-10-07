package com.dylibso.chicory.runtime.exceptions;

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
