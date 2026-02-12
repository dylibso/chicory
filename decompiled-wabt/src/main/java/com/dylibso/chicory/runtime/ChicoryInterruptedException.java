package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

public class ChicoryInterruptedException extends ChicoryException {
    public ChicoryInterruptedException(String msg) {
        super(msg);
    }

    public ChicoryInterruptedException(Throwable cause) {
        super(cause);
    }

    public ChicoryInterruptedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
