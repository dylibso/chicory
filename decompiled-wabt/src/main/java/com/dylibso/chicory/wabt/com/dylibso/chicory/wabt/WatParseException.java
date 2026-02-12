package com.dylibso.chicory.wabt;

public class WatParseException extends RuntimeException {

    public WatParseException() {}

    public WatParseException(String message) {
        super(message);
    }

    public WatParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
