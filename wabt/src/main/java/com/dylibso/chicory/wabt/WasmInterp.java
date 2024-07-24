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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class WasmInterp {
    private static final Logger logger = new SystemLogger();

    public static final String WASMINTERP = "wasm-interp";

    private final File file;
    private final String inline;

    private WasmInterp(File file, String inline) {
        this.file = file;
        this.inline = inline;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private File file;
        private String inline;

        private Builder() {}

        public Builder withFile(File file) {
            this.file = file;
            return this;
        }

        public Builder withInline(String inline) {
            this.inline = inline;
            return this;
        }

        public WasmInterp build() {
            if (!(file == null || inline == null)) {
                throw new IllegalArgumentException(
                        "WasmInterp can be invoked either by targeting a file or passing an inline"
                                + " wasm module, not both.");
            }

            return new WasmInterp(file, inline);
        }
    }

    private InputStream getIs() {
        if (inline != null) {
            return new ByteArrayInputStream(inline.getBytes(StandardCharsets.UTF_8));
        } else {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String invoke(String funName, String... args) {
        try (InputStream is = getIs()) {
            return invoke(is, funName, args);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String invoke(InputStream is, String funName, String... args) {
        Module module =
                Module.builder(Wat2Wasm.class.getResourceAsStream("/" + WASMINTERP)).build();

        try (ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stderrStream = new ByteArrayOutputStream()) {
            try (FileSystem fs =
                    Jimfs.newFileSystem(
                            Configuration.unix().toBuilder().setAttributeViews("unix").build())) {

                Path target = fs.getPath("tmp");
                java.nio.file.Files.createDirectory(target);
                Path path = target.resolve("tmp.wasm");
                copy(is, path, StandardCopyOption.REPLACE_EXISTING);

                var command = new ArrayList<String>();
                command.add(WASMINTERP);
                command.add(path.toString());
                if (funName != null) {
                    command.add("-r");
                    command.add(funName);
                }
                for (var arg : args) {
                    command.add("-a");
                    command.add(arg);
                }

                WasiOptions wasiOpts =
                        WasiOptions.builder()
                                .withStdout(stdoutStream)
                                .withStderr(stdoutStream)
                                .withDirectory(target.toString(), target)
                                .withArguments(command)
                                .build();

                try (var wasi = new WasiPreview1(logger, wasiOpts)) {
                    HostImports imports = new HostImports(wasi.toHostFunctions());
                    module.withHostImports(imports).instantiate();
                }

                return stdoutStream.toString(UTF_8);
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
