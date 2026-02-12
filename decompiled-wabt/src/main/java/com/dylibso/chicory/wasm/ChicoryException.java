package com.dylibso.chicory.wasm;

public class ChicoryException extends RuntimeException {
    public ChicoryException(String msg) {
        super(msg);
    }

    public ChicoryException(Throwable cause) {
        super(cause);
    }

    public ChicoryException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
