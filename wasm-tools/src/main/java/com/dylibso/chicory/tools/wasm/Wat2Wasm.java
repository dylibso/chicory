package com.dylibso.chicory.tools.wasm;

import static java.nio.file.Files.copy;

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
            return parse(is, file.getName());
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
        return parse(is, "temp.wat");
    }

    private static byte[] parse(InputStream is, String fileName) {
        try (ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
                FileSystem fs =
                        ZeroFs.newFileSystem(
                                Configuration.unix().toBuilder()
                                        .setAttributeViews("unix")
                                        .build())) {

            Path target = fs.getPath("tmp");
            java.nio.file.Files.createDirectory(target);
            Path path = target.resolve(fileName);
            copy(is, path, StandardCopyOption.REPLACE_EXISTING);

            String resultFileName = fileName + ".wasm";
            Path result = target.resolve(resultFileName);

            WasiOptions validateWasiOpts =
                    WasiOptions.builder()
                            .withStdout(stdoutStream)
                            .withStderr(stderrStream)
                            .withDirectory(target.toString(), target)
                            .withArguments(List.of("wasm-tools", "validate", path.toString()))
                            .build();

            WasiOptions parseWasiOpts =
                    WasiOptions.builder()
                            .withStdout(stdoutStream)
                            .withStderr(stderrStream)
                            .withDirectory(target.toString(), target)
                            .withArguments(
                                    List.of(
                                            "wasm-tools",
                                            "parse",
                                            path.toString(),
                                            "-o",
                                            result.toString()))
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
                                stdoutStream.toString(StandardCharsets.UTF_8)
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
                if (e.exitCode() != 0 || !java.nio.file.Files.exists(result)) {
                    throw new WatParseException(
                            stdoutStream.toString(StandardCharsets.UTF_8)
                                    + stderrStream.toString(StandardCharsets.UTF_8),
                            e);
                }
            }

            return java.nio.file.Files.readAllBytes(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
