package com.dylibso.chicory.wasm.io;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

/**
 * An exception indicating a problem with I/O.
 */
public class WasmIOException extends ChicoryException {

    private static final long serialVersionUID = 7143984209392368993L;

    /**
     * Constructs a new {@code WasmIOException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     */
    public WasmIOException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code WasmIOException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code WasmIOException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public WasmIOException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code WasmIOException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public WasmIOException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
