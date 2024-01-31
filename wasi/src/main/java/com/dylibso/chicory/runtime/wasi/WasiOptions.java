package com.dylibso.chicory.runtime.wasi;

import java.io.InputStream;
import java.io.PrintStream;

public class WasiOptions {
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final InputStream stdin;

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {
        private PrintStream stdout;
        private PrintStream stderr;
        private InputStream stdin;

        private Builder() {}

        public Builder withStdout(PrintStream stdout) {
            this.stdout = stdout;
            return this;
        }

        public Builder withStderr(PrintStream stderr) {
            this.stderr = stderr;
            return this;
        }

        public Builder withStdin(InputStream stdin) {
            this.stdin = stdin;
            return this;
        }

        public Builder inheritSystem() {
            this.stdout = System.out;
            this.stdin = System.in;
            this.stderr = System.err;
            return this;
        }

        public WasiOptions build() {
            return new WasiOptions(stdout, stderr, stdin);
        }
    }
}
