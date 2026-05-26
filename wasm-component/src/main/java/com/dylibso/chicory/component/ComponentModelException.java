package com.dylibso.chicory.component;

/**
 * Exception thrown when Component Model operations fail.
 *
 * <p>This can include:
 * <ul>
 *   <li>WIT parsing failures
 *   <li>Missing component metadata in WASM binary
 *   <li>Invalid binary format or encoding
 *   <li>Export/import binding failures
 * </ul>
 */
public class ComponentModelException extends RuntimeException {

    public ComponentModelException(String message) {
        super(message);
    }

    public ComponentModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentModelException(Throwable cause) {
        super(cause);
    }
}
