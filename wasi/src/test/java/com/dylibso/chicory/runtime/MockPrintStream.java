package com.dylibso.chicory.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class MockPrintStream extends PrintStream {
    private final ByteArrayOutputStream baos;

    public MockPrintStream() {
        super(new ByteArrayOutputStream());
        this.baos = (ByteArrayOutputStream) this.out;
    }

    @Override
    public void println(String s) {
        super.println(s);
    }

    public String output() {
        return baos.toString();
    }
}
