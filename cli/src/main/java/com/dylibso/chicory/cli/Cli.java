package com.dylibso.chicory.cli;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ExternalValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
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
            names = "--invoke",
            description = "The exported WASM function to be invoked")
    String functionName;

    @CommandLine.Option(
            names = "--wasi",
            description = "Enable the experimental WASI V1 support",
            defaultValue = "false")
    boolean wasi;

    @CommandLine.Option(
            names = "--log-level",
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
        var module = Parser.parse(file);
        var imports =
                wasi
                        ? new ExternalValues(
                                new WasiPreview1(
                                                logger,
                                                WasiOptions.builder().inheritSystem().build())
                                        .toHostFunctions())
                        : new ExternalValues();
        var instance =
                Instance.builder(module)
                        .withInitialize(true)
                        .withStart(false)
                        .withExternalValues(imports)
                        .build();

        if (functionName != null) {
            var type = instance.exportType(functionName);
            var export = instance.export(functionName);
            var params = new long[type.params().size()];
            for (var i = 0; i < type.params().size(); i++) {
                params[i] = Long.valueOf(arguments[i]);
            }

            var result = export.apply(params);
            if (result != null) {
                for (var r : result) {
                    if (result == null) {
                        System.out.println(0);
                    } else {
                        System.out.println(r); // Check floating point results
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
