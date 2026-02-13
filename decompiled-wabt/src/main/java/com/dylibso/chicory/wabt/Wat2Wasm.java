package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
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
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class Wat2Wasm {
    private static final Logger logger = new SystemLogger();
    private static final WasmModule MODULE = Wat2WasmModule.load();

    private Wat2Wasm() {}

    public static byte[] parse(InputStream is) {
        return parse(is, "temp.wast");
    }

    public static byte[] parse(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return parse(is, file.getName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] parse(String wat) {
        try (InputStream is = new ByteArrayInputStream(wat.getBytes(StandardCharsets.UTF_8))) {
            return parse(is, "temp.wast");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] parse(InputStream is, String fileName) {
        try (ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
             ByteArrayOutputStream stderrStream = new ByteArrayOutputStream()) {
            WasiOptions wasiOpts =
                    WasiOptions.builder()
                            .withStdin(is)
                            .withStdout(stdoutStream)
                            .withStderr(stderrStream)
                            .withArguments(List.of("wat2wasm", "-", "--output=-"))
                            .build();

            try (var wasi =
                         WasiPreview1.builder().withLogger(logger).withOptions(wasiOpts).build()) {
                ImportValues imports =
                        ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
                Instance.builder(MODULE)
                        .withMachineFactory(Wat2WasmModule::create)
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

            return stdoutStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
