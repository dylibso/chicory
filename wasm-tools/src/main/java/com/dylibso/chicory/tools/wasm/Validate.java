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

public final class Validate {

    private Validate() {}

    private static final Logger logger =
            new SystemLogger() {
                @Override
                public boolean isLoggable(Logger.Level level) {
                    return false;
                }
            };
    private static final WasmModule MODULE = WasmToolsModule.load();

    public static void validate(File file) {
        try (var is = new FileInputStream(file)) {
            validate(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void validate(String wat) {
        try (var is = new ByteArrayInputStream(wat.getBytes(StandardCharsets.UTF_8))) {
            validate(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void validate(InputStream is) {
        try (var stdinStream = new ByteArrayInputStream(is.readAllBytes());
                var stdoutStream = new ByteArrayOutputStream();
                var stderrStream = new ByteArrayOutputStream()) {

            var options =
                    WasiOptions.builder()
                            .withStdin(stdinStream, false)
                            .withStdout(stdoutStream, false)
                            .withStderr(stderrStream, false)
                            .withArguments(List.of("wasm-tools", "validate", "-"))
                            .build();

            logger.info("Running command: " + String.join(" ", options.arguments()));

            try (var wasi =
                    WasiPreview1.builder().withLogger(logger).withOptions(options).build()) {
                var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

                try {
                    Instance.builder(MODULE)
                            .withMachineFactory(WasmToolsModule::create)
                            .withMemoryFactory(ByteArrayMemory::new)
                            .withImportValues(imports)
                            .build();
                } catch (WasiExitException e) {
                    if (e.exitCode() != 0) {
                        throw new WatParseException(
                                stdoutStream.toString(StandardCharsets.UTF_8)
                                        + stderrStream.toString(StandardCharsets.UTF_8),
                                e);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
