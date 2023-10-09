package com.dylibso.chicory.wasm.exceptions;

public class InvalidException extends ChicoryException {
    public InvalidException(String msg) {
        super(msg);
    }

    public InvalidException(Throwable cause) {
        super(cause);
    }

    public InvalidException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
