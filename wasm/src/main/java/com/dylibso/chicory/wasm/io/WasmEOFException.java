package com.dylibso.chicory.wasm.io;

/**
 * An exception indicating that an EOF was hit while parsing.
 */
public class WasmEOFException extends WasmParseException {
    private static final long serialVersionUID = 5975076159794215675L;

    /**
     * Constructs a new {@code WasmEOFException} instance with a default message.  No
     * cause is specified.
     */
    public WasmEOFException() {
        this("unexpected end-of-file/length out of bounds");
    }

    /**
     * Constructs a new {@code WasmEOFException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public WasmEOFException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code WasmEOFException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code WasmEOFException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public WasmEOFException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code WasmEOFException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public WasmEOFException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
