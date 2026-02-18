package com.dylibso.chicory.wasi;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class WasiOptions {
    private final Random random;
    private final Clock clock;
    private final OutputStream stdout;
    private final OutputStream stderr;
    private final InputStream stdin;
    private final boolean stdinIsTty;
    private final boolean stdoutIsTty;
    private final boolean stderrIsTty;
    private final List<String> arguments;
    private final Map<String, String> environment;
    private final Map<String, Path> directories;
    private final boolean throwOnExit0;

    public static Builder builder() {
        return new Builder();
    }

    private WasiOptions(
            Random random,
            Clock clock,
            OutputStream stdout,
            OutputStream stderr,
            InputStream stdin,
            boolean stdinIsTty,
            boolean stdoutIsTty,
            boolean stderrIsTty,
            List<String> arguments,
            Map<String, String> environment,
            Map<String, Path> directories,
            boolean throwOnExit0) {
        this.random = requireNonNull(random);
        this.clock = requireNonNull(clock);
        this.stdout = requireNonNull(stdout);
        this.stderr = requireNonNull(stderr);
        this.stdin = requireNonNull(stdin);
        this.stdinIsTty = stdinIsTty;
        this.stdoutIsTty = stdoutIsTty;
        this.stderrIsTty = stderrIsTty;
        this.arguments = List.copyOf(arguments);
        this.environment = unmodifiableMap(new LinkedHashMap<>(environment));
        this.directories = unmodifiableMap(new LinkedHashMap<>(directories));
        this.throwOnExit0 = throwOnExit0;
    }

    public Random random() {
        return random;
    }

    public Clock clock() {
        return clock;
    }

    public OutputStream stdout() {
        return stdout;
    }

    public OutputStream stderr() {
        return stderr;
    }

    public InputStream stdin() {
        return stdin;
    }

    public boolean stdinIsTty() {
        return stdinIsTty;
    }

    public boolean stdoutIsTty() {
        return stdoutIsTty;
    }

    public boolean stderrIsTty() {
        return stderrIsTty;
    }

    public List<String> arguments() {
        return arguments;
    }

    public Map<String, String> environment() {
        return environment;
    }

    public Map<String, Path> directories() {
        return directories;
    }

    public boolean throwOnExit0() {
        return throwOnExit0;
    }

    public static final class Builder {
        // ThreadLocalRandom is correctly substituted in graal native-image:
        // https://github.com/oracle/graal/blob/f63ba1767a34d9a4e9d747d077d684f20f4d934d/substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/jdk/ThreadLocalRandomAccessors.java#L38
        private Random random = ThreadLocalRandom.current();
        private Clock clock = Clock.systemUTC();
        private OutputStream stdout = IO.nullOutputStream();
        private OutputStream stderr = IO.nullOutputStream();
        private InputStream stdin = IO.nullInputStream();
        private boolean stdinIsTty = true;
        private boolean stdoutIsTty = true;
        private boolean stderrIsTty = true;
        private List<String> arguments = List.of();
        private final Map<String, String> environment = new LinkedHashMap<>();
        private final Map<String, Path> directories = new LinkedHashMap<>();
        private boolean throwOnExit0 = true;

        private Builder() {}

        public Builder withRandom(Random random) {
            this.random = random;
            return this;
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withStdout(OutputStream stdout) {
            this.stdout = stdout;
            return this;
        }

        public Builder withStdout(OutputStream stdout, boolean isTty) {
            this.stdout = stdout;
            this.stdoutIsTty = isTty;
            return this;
        }

        public Builder withStderr(OutputStream stderr) {
            this.stderr = stderr;
            return this;
        }

        public Builder withStderr(OutputStream stderr, boolean isTty) {
            this.stderr = stderr;
            this.stderrIsTty = isTty;
            return this;
        }

        public Builder withStdin(InputStream stdin) {
            this.stdin = stdin;
            return this;
        }

        public Builder withStdin(InputStream stdin, boolean isTty) {
            this.stdin = stdin;
            this.stdinIsTty = isTty;
            return this;
        }

        public Builder inheritSystem() {
            this.stdout = System.out;
            this.stdin = System.in;
            this.stderr = System.err;
            return this;
        }

        public Builder withArguments(List<String> arguments) {
            this.arguments = List.copyOf(arguments);
            return this;
        }

        public Builder withEnvironment(String name, String value) {
            this.environment.put(name, value);
            return this;
        }

        public Builder withDirectory(String guest, Path host) {
            this.directories.put(guest, host);
            return this;
        }

        public Builder withThrowOnExit0(boolean throwOnExit0) {
            this.throwOnExit0 = throwOnExit0;
            return this;
        }

        public WasiOptions build() {
            return new WasiOptions(
                    random,
                    clock,
                    stdout,
                    stderr,
                    stdin,
                    stdinIsTty,
                    stdoutIsTty,
                    stderrIsTty,
                    arguments,
                    environment,
                    directories,
                    throwOnExit0);
        }
    }
}
