package com.dylibso.chicory.wabt;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
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
    private static final com.dylibso.chicory.wasm.Module wasmModule = Module.builder(
                    Wat2Wasm.class.getResourceAsStream("/wat2wasm"))
            .withInitialize(false)
            .withStart(false)
            .build()
            .wasmModule();

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

            try (FileSystem fs = Jimfs.newFileSystem(
                    Configuration.unix().toBuilder().setAttributeViews("unix").build())) {

                Path target = fs.getPath("tmp");
                java.nio.file.Files.createDirectory(target);
                Path path = target.resolve(fileName);
                copy(is, path, StandardCopyOption.REPLACE_EXISTING);

                WasiOptions wasiOpts = WasiOptions.builder()
                        .withStdout(stdoutStream)
                        .withStderr(stdoutStream)
                        .withDirectory(target.toString(), target)
                        .withArguments(List.of("wat2wasm", path.toString(), "--output=-"))
                        .build();

                try (var wasi = new WasiPreview1(logger, wasiOpts)) {
                    HostImports imports = new HostImports(wasi.toHostFunctions());
                    Module module =
                            Module.builder(wasmModule).withHostImports(imports).build();
                    module.instantiate();
                }

                return stdoutStream.toByteArray();
            } catch (WASMMachineException e) {
                if (!(e.getCause() instanceof WasiExitException)) {
                    throw e;
                }
                var stdout = stdoutStream.toString(UTF_8);
                var stderr = stderrStream.toString(UTF_8);
                throw new MalformedException(stdout + "\n" + stderr);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
