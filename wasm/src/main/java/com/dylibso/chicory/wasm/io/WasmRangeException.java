package com.dylibso.chicory.wasm.io;

/**
 * An exception indicating that the maximum numerical range of a value was exceeded.
 */
public class WasmRangeException extends WasmIOException {
    private static final long serialVersionUID = 3428910081536825291L;

    /**
     * Constructs a new {@code WasmRangeException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public WasmRangeException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code WasmRangeException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code WasmRangeException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public WasmRangeException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code WasmRangeException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public WasmRangeException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
