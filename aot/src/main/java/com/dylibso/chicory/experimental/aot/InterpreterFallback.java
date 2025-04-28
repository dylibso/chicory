package com.dylibso.chicory.experimental.aot;

/**
 * Enum representing the fallback behavior for when the Aot compiler needs to fallback to using
 * the interpreter.
 */
public enum InterpreterFallback {

    /**
     * The Aot compiler will silently use the interpreter as a fallback without any notification.
     */
    SILENT,

    /**
     * The Aot compiler will log a warning message to stderr when it falls back to using the interpreter.
     */
    WARN,

    /**
     * The Aot compiler will throw an exception if it needs to fall back to using the interpreter.
     */
    FAIL
}
