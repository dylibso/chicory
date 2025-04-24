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
    private final List<String> arguments;
    private final Map<String, String> environment;
    private final Map<String, Path> directories;

    public static Builder builder() {
        return new Builder();
    }

    private WasiOptions(
            Random random,
            Clock clock,
            OutputStream stdout,
            OutputStream stderr,
            InputStream stdin,
            List<String> arguments,
            Map<String, String> environment,
            Map<String, Path> directories) {
        this.random = requireNonNull(random);
        this.clock = requireNonNull(clock);
        this.stdout = requireNonNull(stdout);
        this.stderr = requireNonNull(stderr);
        this.stdin = requireNonNull(stdin);
        this.arguments = List.copyOf(arguments);
        this.environment = unmodifiableMap(new LinkedHashMap<>(environment));
        this.directories = unmodifiableMap(new LinkedHashMap<>(directories));
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

    public List<String> arguments() {
        return arguments;
    }

    public Map<String, String> environment() {
        return environment;
    }

    public Map<String, Path> directories() {
        return directories;
    }

    public static final class Builder {
        // ThreadLocalRandom is correctly substituted in graal native-image:
        // https://github.com/oracle/graal/blob/f63ba1767a34d9a4e9d747d077d684f20f4d934d/substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/jdk/ThreadLocalRandomAccessors.java#L38
        private Random random = ThreadLocalRandom.current();
        private Clock clock = Clock.systemUTC();
        private OutputStream stdout = OutputStream.nullOutputStream();
        private OutputStream stderr = OutputStream.nullOutputStream();
        private InputStream stdin = InputStream.nullInputStream();
        private List<String> arguments = List.of();
        private final Map<String, String> environment = new LinkedHashMap<>();
        private final Map<String, Path> directories = new LinkedHashMap<>();

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

        public Builder withStderr(OutputStream stderr) {
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

        public WasiOptions build() {
            return new WasiOptions(
                    random, clock, stdout, stderr, stdin, arguments, environment, directories);
        }
    }
}
