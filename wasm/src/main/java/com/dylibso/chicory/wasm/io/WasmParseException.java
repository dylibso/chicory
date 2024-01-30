package com.dylibso.chicory.wasm.io;

/**
 * An exception representing some type of parsing problem.
 */
public class WasmParseException extends WasmIOException {
    private static final long serialVersionUID = 2721902382204037015L;

    /**
     * Constructs a new {@code WasmParseException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public WasmParseException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code WasmParseException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code WasmParseException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public WasmParseException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code WasmParseException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public WasmParseException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
