package com.dylibso.chicory.wasm;

/**
 * Exception thrown when the binary format of a WebAssembly module
 * does not conform to the expected structure or encoding rules.
 * This often indicates a parsing error due to corrupted or incorrectly encoded data.
 */
public class MalformedException extends ChicoryException {
    /**
     * Constructs a new MalformedException with the specified detail message.
     *
     * @param msg the detail message.
     */
    public MalformedException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new MalformedException with the specified cause.
     *
     * @param cause the cause.
     */
    public MalformedException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new MalformedException with the specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause.
     */
    public MalformedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
