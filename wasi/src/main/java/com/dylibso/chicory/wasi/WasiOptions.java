package com.dylibso.chicory.wasi;

import java.io.InputStream;
import java.io.PrintStream;

public class WasiOptions {
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final InputStream stdin;

    public static WasiOptionsBuilder builder() {
        return new WasiOptionsBuilder();
    }

    public WasiOptions(PrintStream stdout, PrintStream stderr, InputStream stdin) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.stdin = stdin;
    }

    public PrintStream stdout() {
        return stdout;
    }

    public PrintStream stderr() {
        return stderr;
    }

    public InputStream stdin() {
        return stdin;
    }
}
