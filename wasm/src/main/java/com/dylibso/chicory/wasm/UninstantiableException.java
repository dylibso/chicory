package com.dylibso.chicory.wasm;

/**
 * Exception thrown when a WebAssembly module cannot be instantiated.
 * This typically occurs during the linking phase if imports cannot be resolved
 * or if initialization fails (e.g., start function trap, memory/table limits exceeded).
 */
public class UninstantiableException extends ChicoryException {
    /**
     * Constructs a new UninstantiableException with the specified detail message.
     *
     * @param msg the detail message.
     */
    public UninstantiableException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new UninstantiableException with the specified cause.
     *
     * @param cause the cause.
     */
    public UninstantiableException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new UninstantiableException with the specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause.
     */
    public UninstantiableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
