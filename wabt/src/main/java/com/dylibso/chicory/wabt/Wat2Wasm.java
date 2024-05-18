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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class Wat2Wasm {
    private static final Logger logger = new SystemLogger();

    private Wat2Wasm() {}

    public static byte[] parse(File file) {
        Module module = Module.builder(Wat2Wasm.class.getResourceAsStream("/wat2wasm")).build();
        try (ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stderrStream = new ByteArrayOutputStream()) {
            try (FileInputStream fis = new FileInputStream(file);
                    FileSystem fs =
                            Jimfs.newFileSystem(
                                    Configuration.unix().toBuilder()
                                            .setAttributeViews("unix")
                                            .build())) {

                var wasiOpts = WasiOptions.builder();

                wasiOpts.withStdout(stdoutStream);
                wasiOpts.withStderr(stdoutStream);

                Path target = fs.getPath("tmp");
                java.nio.file.Files.createDirectory(target);
                Path path = target.resolve(file.getName());
                copy(fis, path, StandardCopyOption.REPLACE_EXISTING);
                wasiOpts.withDirectory(target.toString(), target);

                wasiOpts.withArguments(List.of("wat2wasm", path.toString(), "--output=-"));

                try (var wasi = new WasiPreview1(logger, wasiOpts.build())) {
                    HostImports imports = new HostImports(wasi.toHostFunctions());
                    module.withHostImports(imports).instantiate();
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
