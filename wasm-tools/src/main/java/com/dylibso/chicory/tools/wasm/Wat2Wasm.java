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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public final class Wat2Wasm {
    private Wat2Wasm() {}

    private static final Logger logger =
            new SystemLogger() {
                @Override
                public boolean isLoggable(Logger.Level level) {
                    return false;
                }
            };
    private static final WasmModule MODULE = WasmToolsModule.load();

    public static byte[] parse(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return parse(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] parse(String wat) {
        try (InputStream is = new ByteArrayInputStream(wat.getBytes(StandardCharsets.UTF_8))) {
            return parse(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] parse(InputStream is) {
        byte[] input = null;
        try {
            input = is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (ByteArrayInputStream stdinStream1 = new ByteArrayInputStream(input);
                ByteArrayInputStream stdinStream2 = new ByteArrayInputStream(input);
                ByteArrayOutputStream stdoutStream1 = new ByteArrayOutputStream();
                ByteArrayOutputStream stdoutStream2 = new ByteArrayOutputStream();
                ByteArrayOutputStream stderrStream = new ByteArrayOutputStream()) {

            WasiOptions validateWasiOpts =
                    WasiOptions.builder()
                            .withStdin(stdinStream1, false)
                            .withStdout(stdoutStream1, false)
                            .withStderr(stderrStream, false)
                            .withArguments(List.of("wasm-tools", "validate", "-"))
                            .build();

            WasiOptions parseWasiOpts =
                    WasiOptions.builder()
                            .withStdin(stdinStream2, false)
                            .withStdout(stdoutStream2, false)
                            .withStderr(stderrStream, false)
                            .withArguments(List.of("wasm-tools", "parse", "-"))
                            .build();

            logger.info(
                    "Running command: "
                            + parseWasiOpts.arguments().stream().collect(Collectors.joining(" ")));

            try (var validateWasi =
                            WasiPreview1.builder()
                                    .withLogger(logger)
                                    .withOptions(validateWasiOpts)
                                    .build();
                    var parseWasi =
                            WasiPreview1.builder()
                                    .withLogger(logger)
                                    .withOptions(parseWasiOpts)
                                    .build()) {
                ImportValues validateImports =
                        ImportValues.builder().addFunction(validateWasi.toHostFunctions()).build();

                try {
                    Instance.builder(MODULE)
                            .withMachineFactory(WasmToolsModule::create)
                            .withMemoryFactory(ByteArrayMemory::new)
                            .withImportValues(validateImports)
                            .build();
                } catch (WasiExitException e) {
                    if (e.exitCode() != 0) {
                        throw new WatParseException(
                                stdoutStream1.toString(StandardCharsets.UTF_8)
                                        + stderrStream.toString(StandardCharsets.UTF_8),
                                e);
                    }
                }

                ImportValues parseImports =
                        ImportValues.builder().addFunction(parseWasi.toHostFunctions()).build();

                Instance.builder(MODULE)
                        .withMachineFactory(WasmToolsModule::create)
                        .withMemoryFactory(ByteArrayMemory::new)
                        .withImportValues(parseImports)
                        .build();
            } catch (WasiExitException e) {
                if (e.exitCode() != 0 || stdoutStream2.size() <= 0) {
                    throw new WatParseException(
                            stdoutStream2.toString(StandardCharsets.UTF_8)
                                    + stderrStream.toString(StandardCharsets.UTF_8),
                            e);
                }
            }
            return stdoutStream2.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
