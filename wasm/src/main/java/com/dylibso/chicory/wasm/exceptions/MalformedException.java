package com.dylibso.chicory.wasm.exceptions;

public class MalformedException extends ChicoryException {
    public MalformedException(String msg) {
        super(msg);
    }
    public MalformedException(Throwable cause) {
        super(cause);
    }
    public MalformedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
