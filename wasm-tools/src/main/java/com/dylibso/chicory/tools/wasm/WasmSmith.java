package com.dylibso.chicory.tools.wasm;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.WasmModule;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class WasmSmith {

    private WasmSmith() {}

    private static final long TIMEOUT_SECONDS = 30;

    private static final Logger logger =
            new SystemLogger() {
                @Override
                public boolean isLoggable(Logger.Level level) {
                    return false;
                }
            };
    private static final WasmModule MODULE = WasmToolsModule.load();

    private static final ThreadFactory DAEMON_THREAD_FACTORY =
            r -> {
                var t = new Thread(r, "wasm-smith");
                t.setDaemon(true);
                return t;
            };

    public static byte[] run(
            byte[] seed, Map<String, String> properties, String allowedInstructions)
            throws WasmSmithException {

        Callable<byte[]> task = () -> runInternal(seed, properties, allowedInstructions);

        // Use daemon threads so leaked threads (from timeouts where the interpreter
        // doesn't respond to interrupts) don't prevent JVM exit.
        ExecutorService executor = Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY);
        try {
            var future = executor.submit(task);
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new WasmSmithException("wasm-smith timed out after " + TIMEOUT_SECONDS + "s", e);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof WasmSmithException) {
                throw (WasmSmithException) cause;
            }
            throw new WasmSmithException("wasm-smith failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WasmSmithException("wasm-smith interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private static byte[] runInternal(
            byte[] seed, Map<String, String> properties, String allowedInstructions)
            throws IOException {

        try (var stdinStream = new ByteArrayInputStream(seed);
                var stdoutStream = new ByteArrayOutputStream();
                var stderrStream = new ByteArrayOutputStream();
                FileSystem fs =
                        ZeroFs.newFileSystem(
                                Configuration.unix().toBuilder()
                                        .setAttributeViews("unix")
                                        .build())) {

            var outputDir = fs.getPath("output");
            java.nio.file.Files.createDirectory(outputDir);
            var outputPath = outputDir.resolve("generated.wasm");

            var wasiOpts = WasiOptions.builder();
            wasiOpts.withStdin(stdinStream, false);
            wasiOpts.withStdout(stdoutStream, false);
            wasiOpts.withStderr(stderrStream, false);
            wasiOpts.withDirectory(outputDir.toString(), outputDir);

            List<String> args = new ArrayList<>();
            args.add("wasm-tools");
            args.add("smith");

            for (var entry : properties.entrySet()) {
                args.add("--" + entry.getKey());
                args.add(entry.getValue());
            }

            args.add("--allowed-instructions");
            args.add(allowedInstructions);

            args.add("-o");
            args.add(outputPath.toString());

            wasiOpts.withArguments(args);

            try (var wasi =
                    WasiPreview1.builder()
                            .withLogger(logger)
                            .withOptions(wasiOpts.build())
                            .build()) {
                var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

                Instance.builder(MODULE)
                        .withMachineFactory(WasmToolsModule::create)
                        .withMemoryFactory(ByteArrayMemory::new)
                        .withImportValues(imports)
                        .build();
            } catch (WasiExitException e) {
                if (e.exitCode() != 0) {
                    throw new WasmSmithException(stderrStream.toString(StandardCharsets.UTF_8), e);
                }
            }

            if (!java.nio.file.Files.exists(outputPath)) {
                throw new WasmSmithException(
                        "wasm-smith produced no output file: "
                                + stderrStream.toString(StandardCharsets.UTF_8));
            }

            return java.nio.file.Files.readAllBytes(outputPath);
        }
    }
}
