package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

/*
 * Signal a successful stop of execution
 */
public class ExecutionCompletedException extends ChicoryException {

    public ExecutionCompletedException(String msg) {
        super(msg);
    }

    public ExecutionCompletedException(Throwable cause) {
        super(cause);
    }

    public ExecutionCompletedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
