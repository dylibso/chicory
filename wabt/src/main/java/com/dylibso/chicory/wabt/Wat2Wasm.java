package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ExternalValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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
    private static final Module MODULE =
            Parser.parse(Wat2Wasm.class.getResourceAsStream("/wat2wasm"));

    private Wat2Wasm() {}

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

            try (FileSystem fs =
                    Jimfs.newFileSystem(
                            Configuration.unix().toBuilder().setAttributeViews("unix").build())) {

                Path target = fs.getPath("tmp");
                java.nio.file.Files.createDirectory(target);
                Path path = target.resolve(fileName);
                copy(is, path, StandardCopyOption.REPLACE_EXISTING);

                WasiOptions wasiOpts =
                        WasiOptions.builder()
                                .withStdout(stdoutStream)
                                .withStderr(stdoutStream)
                                .withDirectory(target.toString(), target)
                                .withArguments(List.of("wat2wasm", path.toString(), "--output=-"))
                                .build();

                try (var wasi =
                        WasiPreview1.builder().withLogger(logger).withOpts(wasiOpts).build()) {
                    ExternalValues imports = new ExternalValues(wasi.toHostFunctions());
                    Instance.builder(MODULE)
                            .withMachineFactory(Wat2WasmModuleMachineFactory::create)
                            .withExternalValues(imports)
                            .build();
                }

                return stdoutStream.toByteArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
