package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.WasmModule;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class Wast2Json {
    private static final Logger logger =
            new SystemLogger() {
                @Override
                public boolean isLoggable(Logger.Level level) {
                    return false;
                }
            };
    private static final WasmModule MODULE = Wast2JsonModule.load();

    private final Path input;
    private final Path output;
    private final String[] options;

    private Wast2Json(Path input, Path output, String[] options) {
        this.input = input;
        this.output = output;
        this.options = options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        try (ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream()) {
            try (InputStream fis = Files.newInputStream(input);
                    FileSystem fs =
                            Jimfs.newFileSystem(
                                    Configuration.unix().toBuilder()
                                            .setAttributeViews("unix")
                                            .build())) {

                var wasiOpts = WasiOptions.builder();

                wasiOpts.withStdout(stdoutStream);
                wasiOpts.withStderr(stdoutStream);

                Path inputFolder = fs.getPath("input");
                Files.createDirectory(inputFolder);
                Path inputPath = inputFolder.resolve(input.getFileName());
                copy(fis, inputPath, StandardCopyOption.REPLACE_EXISTING);
                wasiOpts.withDirectory(inputFolder.toString(), inputFolder);

                Path outputFolder = fs.getPath("output");
                Files.createDirectory(outputFolder);
                wasiOpts.withDirectory(outputFolder.toString(), outputFolder);

                List<String> args = new ArrayList<>();
                args.add("wast2json");
                args.add(inputPath.toString());
                args.add("-o");
                args.add(outputFolder.resolve(output.getFileName()).toString());
                args.addAll(List.of(options));
                wasiOpts.withArguments(args);

                try (var wasi =
                        WasiPreview1.builder()
                                .withLogger(logger)
                                .withOptions(wasiOpts.build())
                                .build()) {
                    ImportValues imports =
                            ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

                    Instance.builder(MODULE)
                            .withImportValues(imports)
                            .withMachineFactory(Wast2JsonModule::create)
                            .build();
                }

                createDirectories(output.getParent());
                com.dylibso.chicory.wabt.Files.copyDirectory(outputFolder, output.getParent());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Implementing only the needed options for now
    //  --version                                Print version information
    // -v, --verbose                              Use multiple times for more info
    //  --debug-parser                           Turn on debugging the parser of wast files
    //  --enable-exceptions                      Enable Experimental exception handling
    //  --disable-mutable-globals                Disable Import/export mutable globals
    //  --disable-saturating-float-to-int        Disable Saturating float-to-int operators
    //  --disable-sign-extension                 Disable Sign-extension operators
    //  --disable-simd                           Disable SIMD support
    //  --enable-threads                         Enable Threading support
    //  --enable-function-references             Enable Typed function references
    //  --disable-multi-value                    Disable Multi-value
    //  --enable-tail-call                       Enable Tail-call support
    //  --disable-bulk-memory                    Disable Bulk-memory operations
    //  --disable-reference-types                Disable Reference types (externref)
    //  --enable-annotations                     Enable Custom annotation syntax
    //  --enable-code-metadata                   Enable Code metadata
    //  --enable-gc                              Enable Garbage collection
    //  --enable-memory64                        Enable 64-bit memory
    //  --enable-multi-memory                    Enable Multi-memory
    //  --enable-extended-const                  Enable Extended constant expressions
    //  --enable-relaxed-simd                    Enable Relaxed SIMD
    //  --enable-all                             Enable all features
    // -o, --output=FILE                          output JSON file
    // -r, --relocatable                          Create a relocatable wasm binary (suitable for
    // linking with e.g. lld)
    //  --no-canonicalize-leb128s                Write all LEB128 sizes as 5-bytes instead of their
    // minimal size
    //  --debug-names                            Write debug names to the generated binary file
    //  --no-check                               Don't check for invalid modules
    public static final class Builder {
        private Path input;
        private Path output;
        private String[] options = new String[0];

        private Builder() {}

        public Builder withFile(Path p) {
            this.input = p;
            return this;
        }

        public Builder withOutput(Path p) {
            this.output = p;
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
