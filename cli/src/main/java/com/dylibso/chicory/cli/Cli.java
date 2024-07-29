package com.dylibso.chicory.cli;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;
import picocli.CommandLine;

@CommandLine.Command(
        name = "chicory",
        mixinStandardHelpOptions = true,
        helpCommand = true,
        header = "A pure Java WASM runtime available as a CLI.")
public class Cli implements Runnable {

    @CommandLine.Parameters(arity = "1", description = "a wasm file to be executed")
    File file;

    @CommandLine.Parameters(
            arity = "0..*",
            description = "values to be passed to the wasm function")
    int[] arguments;

    @CommandLine.Option(
            names = {"--invoke"},
            description = "The exported WASM function to be invoked")
    String functionName;

    @CommandLine.Option(
            names = {"--wasi"},
            description = "Enable the experimental WASI V1 support",
            defaultValue = "false")
    boolean wasi;

    @CommandLine.Option(
            names = {"--log-level"},
            description = "The log level to be used",
            defaultValue = "INFO")
    String logLevel; // this should be an enum

    @Override
    public void run() {
        // TODO: improve the handling of the logLevel
        try (var is =
                new ByteArrayInputStream(
                        (".level = " + logLevel).getBytes(StandardCharsets.UTF_8))) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var logger = new SystemLogger();
        var module = WasmModule.builder(file).withLogger(logger).build();
        var imports =
                wasi
                        ? new HostImports(
                                new WasiPreview1(
                                                logger,
                                                WasiOptions.builder().inheritSystem().build())
                                        .toHostFunctions())
                        : new HostImports();
        var instance =
                Instance.builder(module)
                        .withInitialize(true)
                        .withStart(false)
                        .withHostImports(imports)
                        .build();

        if (functionName != null) {
            var export = instance.export(functionName);
            var params = new Value[arguments.length];
            for (var i = 0; i < arguments.length; i++) {
                // TODO: FIXME - it's hard to extract the exported function signature
                // -> should be easy
                params[i] = new Value(ValueType.I32, Long.valueOf(arguments[i]));
            }

            var result = export.apply(params);
            if (result != null) {
                for (var r : result) {
                    if (result == null) {
                        System.out.println(0);
                    } else {
                        System.out.println(r.asLong()); // Check floating point results
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Cli()).execute(args);
        System.exit(exitCode);
    }
}
