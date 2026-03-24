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
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WasmSmith {

    private WasmSmith() {}

    private static final Logger logger =
            new SystemLogger() {
                @Override
                public boolean isLoggable(Logger.Level level) {
                    return false;
                }
            };
    private static final WasmModule MODULE = WasmToolsModule.load();

    public static byte[] run(
            byte[] seed, Map<String, String> properties, String allowedInstructions)
            throws WasmSmithException {

        try (var stdinStream = new ByteArrayInputStream(seed);
                var stdoutStream = new ByteArrayOutputStream();
                var stderrStream = new ByteArrayOutputStream();
                FileSystem fs =
                        ZeroFs.newFileSystem(
                                Configuration.unix().toBuilder()
                                        .setAttributeViews("unix")
                                        .build())) {

            var outputDir = fs.getPath("output");
            java.nio.file.Files.createDirectory(outputDir);
            var outputPath = outputDir.resolve("generated.wasm");

            var wasiOpts = WasiOptions.builder();
            wasiOpts.withStdin(stdinStream, false);
            wasiOpts.withStdout(stdoutStream, false);
            wasiOpts.withStderr(stderrStream, false);
            wasiOpts.withDirectory(outputDir.toString(), outputDir);

            List<String> args = new ArrayList<>();
            args.add("wasm-tools");
            args.add("smith");

            for (var entry : properties.entrySet()) {
                args.add("--" + entry.getKey());
                args.add(entry.getValue());
            }

            args.add("--allowed-instructions");
            args.add(allowedInstructions);

            args.add("-o");
            args.add(outputPath.toString());

            wasiOpts.withArguments(args);

            try (var wasi =
                    WasiPreview1.builder()
                            .withLogger(logger)
                            .withOptions(wasiOpts.build())
                            .build()) {
                var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

                Instance.builder(MODULE)
                        .withMachineFactory(WasmToolsModule::create)
                        .withMemoryFactory(ByteArrayMemory::new)
                        .withImportValues(imports)
                        .build();
            } catch (WasiExitException e) {
                if (e.exitCode() != 0) {
                    throw new WasmSmithException(stderrStream.toString(StandardCharsets.UTF_8), e);
                }
            }

            if (!java.nio.file.Files.exists(outputPath)) {
                throw new WasmSmithException(
                        "wasm-smith produced no output file: "
                                + stderrStream.toString(StandardCharsets.UTF_8));
            }

            return java.nio.file.Files.readAllBytes(outputPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
