package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
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
import java.util.function.Function;

public final class Wat2Wasm {
    private static final Logger logger = new SystemLogger();
    private static final WasmModule MODULE = Wat2WasmModule.load();

    private Wat2Wasm() {}

    public static byte[] parse(InputStream is, Options... options) {
        return parse(is, "temp.wast", options);
    }

    public static byte[] parse(File file, Options... options) {
        try (InputStream is = new FileInputStream(file)) {
            return parse(is, file.getName(), options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] parse(String wat, Options... options) {
        try (InputStream is = new ByteArrayInputStream(wat.getBytes(StandardCharsets.UTF_8))) {
            return parse(is, "temp.wast", options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] parse(InputStream is, String fileName, Options... options) {
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
                        WasiPreview1.builder().withLogger(logger).withOptions(wasiOpts).build()) {
                    ImportValues imports =
                            ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

                    Options b = options.length > 0 ? options[0] : options();
                    b.build(imports);
                }

                return stdoutStream.toByteArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Options options() {
        return new Options();
    }

    public enum ExecutionType {
        INTERPRETED,
        AOT,
    }

    public static final class Options {

        ExecutionType type = ExecutionType.AOT;
        private Function<MemoryLimits, Memory> memoryFactory;

        private Options() {}

        public Options withExecutionType(ExecutionType type) {
            this.type = type;
            return this;
        }

        public Options withMemoryFactory(Function<MemoryLimits, Memory> memoryFactory) {
            this.memoryFactory = memoryFactory;
            return this;
        }

        Instance build(ImportValues imports) {
            Instance.Builder builder;
            switch (type) {
                case INTERPRETED:
                    {
                        var module = Parser.parse(Wat2Wasm.class.getResourceAsStream("/wat2wasm"));
                        builder = Instance.builder(module);
                        break;
                    }
                case AOT:
                    {
                        builder =
                                Instance.builder(MODULE).withMachineFactory(Wat2WasmModule::create);
                        break;
                    }
                default:
                    throw new IllegalArgumentException("Unknown execution type: " + type);
            }
            if (memoryFactory != null) {
                builder.withMemoryFactory(memoryFactory);
            }
            return builder.withImportValues(imports).build();
        }
    }
}
