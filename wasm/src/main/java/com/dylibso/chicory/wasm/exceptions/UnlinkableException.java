package com.dylibso.chicory.wasm.exceptions;

public class UnlinkableException extends ChicoryException {
    public UnlinkableException(String msg) {
        super(msg);
    }

    public UnlinkableException(Throwable cause) {
        super(cause);
    }

    public UnlinkableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
