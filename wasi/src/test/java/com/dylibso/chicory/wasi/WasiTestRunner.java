package com.dylibso.chicory.wasi;

import static com.dylibso.chicory.wasi.Files.copyDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasm.WasmModule;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WasiTestRunner {
    private static final SystemLogger LOGGER = new SystemLogger();

    private WasiTestRunner() {}

    public static void execute(
            File test,
            List<String> args,
            List<String> dirs,
            Map<String, String> env,
            int exitCode,
            String stderr,
            String stdout) {

        try (FileSystem fs =
                Jimfs.newFileSystem(
                        Configuration.unix().toBuilder().setAttributeViews("unix").build())) {

            var stdoutStream = new MockPrintStream();
            var stderrStream = new MockPrintStream();

            List<String> allArgs = new ArrayList<>();
            allArgs.add("test");
            allArgs.addAll(args);

            WasiOptions.Builder options =
                    WasiOptions.builder()
                            .withStdout(stdoutStream)
                            .withStderr(stderrStream)
                            .withArguments(allArgs);

            env.forEach(options::withEnvironment);

            for (String dir : dirs) {
                Path source = test.getParentFile().toPath().resolve(dir);
                Path target = fs.getPath(dir);
                copyDirectory(source, target);
                options.withDirectory(target.toString(), target);
            }

            int actualExitCode;
            try {
                actualExitCode = execute(test, options.build());
            } catch (RuntimeException e) {
                String message = "Failed to execute test: " + test;
                if (!stdoutStream.output().isEmpty() || !stderrStream.output().isEmpty()) {
                    message += "\n<<<<<\n";
                    message += (stdoutStream.output() + stderrStream.output()).strip();
                    message += "\n>>>>>";
                }
                throw new RuntimeException(message, e);
            }

            assertEquals(exitCode, actualExitCode, "exit code");
            assertEquals(stdout, stdoutStream.output(), "stdout");
            assertEquals(stderr, stderrStream.output(), "stderr");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int execute(File test, WasiOptions wasiOptions) {
        try (var wasi = new WasiPreview1(LOGGER, wasiOptions)) {
            Instance.builder(WasmModule.builder(test).build())
                    .withHostImports(new HostImports(wasi.toHostFunctions()))
                    .build();
        } catch (WASMMachineException e) {
            if (e.getCause() instanceof WasiExitException) {
                return ((WasiExitException) e.getCause()).exitCode();
            }
            throw e;
        }
        return 0;
    }
}
