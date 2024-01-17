package com.dylibso.chicory.runtime.wasi;

import java.io.InputStream;
import java.io.PrintStream;

public class WasiOptionsBuilder {
    private PrintStream stdout;
    private PrintStream stderr;
    private InputStream stdin;

    public WasiOptionsBuilder() {}

    public WasiOptionsBuilder withStdout(PrintStream stdout) {
        this.stdout = stdout;
        return this;
    }

    public WasiOptionsBuilder withStderr(PrintStream stderr) {
        this.stderr = stderr;
        return this;
    }

    public WasiOptionsBuilder withStdin(InputStream stdin) {
        this.stdin = stdin;
        return this;
    }

    public WasiOptionsBuilder inheritSystem() {
        this.stdout = System.out;
        this.stdin = System.in;
        this.stderr = System.err;
        return this;
    }

    public WasiOptions build() {
        return new WasiOptions(stdout, stderr, stdin);
    }
}
