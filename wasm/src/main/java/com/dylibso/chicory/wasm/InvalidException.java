package com.dylibso.chicory.wasm;

/**
 * Exception thrown when a WebAssembly module is determined to be invalid
 * according to the specification's validation rules.
 */
public class InvalidException extends ChicoryException {
    /**
     * Constructs a new InvalidException with the specified detail message.
     *
     * @param msg the detail message.
     */
    public InvalidException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new InvalidException with the specified cause.
     *
     * @param cause the cause.
     */
    public InvalidException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new InvalidException with the specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause.
     */
    public InvalidException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
