package com.dylibso.chicory.wasm;

public class UninstantiableException extends ChicoryException {
    public UninstantiableException(String msg) {
        super(msg);
    }

    public UninstantiableException(Throwable cause) {
        super(cause);
    }

    public UninstantiableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
