package com.dylibso.chicory.wasm;

/**
 * Base exception class for all exceptions thrown by the Chicory Wasm runtime and parser.
 */
public class ChicoryException extends RuntimeException {
    /**
     * Constructs a new ChicoryException with the specified detail message.
     *
     * @param msg the detail message.
     */
    public ChicoryException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new ChicoryException with the specified cause.
     *
     * @param cause the cause.
     */
    public ChicoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new ChicoryException with the specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause.
     */
    public ChicoryException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
