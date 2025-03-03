package com.dylibso.chicory.tools.wasm;

import static java.nio.file.Files.createDirectories;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Wast2Json {
    private static final Logger logger =
            new SystemLogger() {
                @Override
                public boolean isLoggable(Logger.Level level) {
                    return false;
                }
            };
    private static final WasmModule MODULE =
            Parser.parse(Wast2Json.class.getResourceAsStream("/wasm-tools.wasm"));

    private final File input;
    private final File output;
    private final String[] options;

    private Wast2Json(File input, File output, String[] options) {
        this.input = input;
        this.output = output;
        this.options = options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        try (FileInputStream fis = new FileInputStream(input);
                FileSystem fs =
                        Jimfs.newFileSystem(
                                Configuration.unix().toBuilder()
                                        .setAttributeViews("unix")
                                        .build())) {

            var wasiOpts = WasiOptions.builder();

            wasiOpts.inheritSystem();

            Path inputFolder = fs.getPath("input");
            java.nio.file.Files.createDirectory(inputFolder);
            Path inputPath = inputFolder.resolve("spec.wast");
            java.nio.file.Files.copy(fis, inputPath, StandardCopyOption.REPLACE_EXISTING);
            //                copy(fis, inputPath, StandardCopyOption.REPLACE_EXISTING);
            wasiOpts.withDirectory(inputFolder.toString(), inputFolder);

            Path outputFolder = fs.getPath("output");
            java.nio.file.Files.createDirectory(outputFolder);
            wasiOpts.withDirectory(outputFolder.toString(), outputFolder);
            java.nio.file.Files.createDirectory(outputFolder.resolve(output.getName()));

            List<String> args = new ArrayList<>();
            args.add("wasm-tools");
            args.add("json-from-wast");
            args.add(inputPath.toString());
            args.add("--wasm-dir");
            args.add(outputFolder.resolve(output.getName()).toString());
            args.add("--output");
            args.add(outputFolder.resolve(output.getName()).resolve("spec.json").toString());
            args.addAll(List.of(options));
            logger.info("Running command: " + args.stream().collect(Collectors.joining(" ")));
            wasiOpts.withArguments(args);

            try (var wasi =
                    WasiPreview1.builder()
                            .withLogger(logger)
                            .withOptions(wasiOpts.build())
                            .build()) {
                ImportValues imports =
                        ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

                Instance.builder(MODULE)
                        .withMemoryFactory(ByteArrayMemory::new)
                        .withImportValues(imports)
                        .build();
            }

            createDirectories(output.toPath());
            Files.copyDirectory(outputFolder.resolve(output.getName()), output.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static final class Builder {
        private File input;
        private File output;
        private String[] options = new String[0];

        private Builder() {}

        public Builder withFile(File f) {
            this.input = f;
            return this;
        }

        public Builder withOutput(File f) {
            this.output = f;
            return this;
        }

        public Builder withOptions(String[] opts) {
            this.options = opts;
            return this;
        }

        public Wast2Json build() {
            return new Wast2Json(input, output, options);
        }
    }
}
