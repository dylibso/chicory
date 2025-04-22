package com.dylibso.chicory.wasm;

/**
 * Exception thrown when a WebAssembly module cannot be linked,
 * typically due to missing or mismatched imports.
 */
public class UnlinkableException extends ChicoryException {
    /**
     * Constructs a new UnlinkableException with the specified detail message.
     *
     * @param msg the detail message.
     */
    public UnlinkableException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new UnlinkableException with the specified cause.
     *
     * @param cause the cause.
     */
    public UnlinkableException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new UnlinkableException with the specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause.
     */
    public UnlinkableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
