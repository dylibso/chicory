package com.dylibso.chicory.experimental.aot.cli;

import com.dylibso.chicory.experimental.aot.InterpreterFallback;
import com.dylibso.chicory.experimental.build.time.aot.Config;
import com.dylibso.chicory.experimental.build.time.aot.Generator;
import com.dylibso.chicory.wasm.Version;
import java.io.IOException;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
        name = "chicory-aot",
        versionProvider = Cli.VersionProvider.class,
        mixinStandardHelpOptions = true,
        helpCommand = true,
        header = "A CLI to generate resources using the Chicory AOT compiler")
public class Cli implements Runnable {
    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {Version.version()};
        }
    }

    @CommandLine.Parameters(arity = "1", description = "The wasm file to be used")
    Path wasmFile;

    @CommandLine.Option(
            order = 1,
            names = "--prefix",
            description = "The prefix to be used to generate resources",
            defaultValue = "com.dylibso.chicory.Wasm")
    String prefix;

    @CommandLine.Option(
            order = 2,
            names = "--source-dir",
            description = "The target folder to use for source files",
            defaultValue = ".")
    Path targetSourceFolder;

    @CommandLine.Option(
            order = 3,
            names = "--class-dir",
            description = "The target folder to use for class files",
            defaultValue = ".")
    Path targetClassFolder;

    @CommandLine.Option(
            order = 4,
            names = "--wasm-dir",
            description = "The target folder to use for the wasm meta file",
            defaultValue = ".")
    Path targetWasmFolder;

    @CommandLine.Option(
            order = 4,
            names = "--interpreter-fallback",
            description =
                    "Action to take if the compiler needs to use the interpreter because a function"
                            + " is too big",
            defaultValue = "FAIL")
    InterpreterFallback interpreterFallback;

    @Override
    public void run() {
        var config =
                Config.builder()
                        .withWasmFile(wasmFile)
                        .withName(prefix)
                        .withTargetClassFolder(targetClassFolder)
                        .withTargetSourceFolder(targetSourceFolder)
                        .withTargetWasmFolder(targetWasmFolder)
                        .withInterpreterFallback(interpreterFallback)
                        .build();

        var generator = new Generator(config);

        try {
            generator.generateResources();
            generator.generateMetaWasm();
            generator.generateSources();
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Failed to execute the command", e);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Cli()).execute(args);
        System.exit(exitCode);
    }
}
