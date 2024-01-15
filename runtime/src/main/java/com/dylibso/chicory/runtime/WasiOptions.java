package com.dylibso.chicory.runtime;

import java.io.InputStream;
import java.io.PrintStream;

public class WasiOptions {
    private PrintStream stdout;
    private PrintStream stderr;
    private InputStream stdin;

    public static WasiOptions build() {
        return new WasiOptions();
    }

    protected WasiOptions() { }

    public WasiOptions inheritSystem() {
        this.stdout = System.out;
        this.stdin = System.in;
        this.stderr = System.err;
        return this;
    }

    public PrintStream getStdout() {
        return stdout;
    }

    public WasiOptions setStdout(PrintStream stdout) {
        this.stdout = stdout;
        return this;
    }

    public PrintStream getStderr() {
        return stderr;
    }

    public WasiOptions setStderr(PrintStream stderr) {
        this.stderr = stderr;
        return this;
    }

    public InputStream getStdin() {
        return stdin;
    }

    public WasiOptions setStdin(InputStream stdin) {
        this.stdin = stdin;
        return this;
    }
}
