package com.dylibso.chicory.cli;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.wasi.WasiOptions;
import com.dylibso.chicory.runtime.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.types.Value;
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
        var module = Module.builder(file).withLogger(logger).build();
        var imports =
                wasi
                        ? new HostImports(
                                new WasiPreview1(
                                                module.logger(),
                                                WasiOptions.builder().inheritSystem().build())
                                        .toHostFunctions())
                        : new HostImports();
        var instance = module.instantiate(imports, false);

        if (functionName != null) {
            var exportSig = module.export(functionName);
            var export = instance.export(functionName);

            var typeId = instance.functionType(exportSig.index());
            var type = instance.type(typeId);

            if (arguments.length != type.params().length) {
                throw new RuntimeException(
                        "The function needs "
                                + type.params().length
                                + " parameters, but found: "
                                + arguments.length);
            }
            var params = new Value[type.params().length];
            for (var i = 0; i < type.params().length; i++) {
                params[i] = new Value(type.params()[i], Long.valueOf(arguments[i]));
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
