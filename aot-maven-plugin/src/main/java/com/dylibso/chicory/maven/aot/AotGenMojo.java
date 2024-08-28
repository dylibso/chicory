package com.dylibso.chicory.maven.aot;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This plugin generates an invokable library from the compiled Wasm
 */
@Mojo(name = "wasm-aot-gen", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class AotGenMojo extends AbstractMojo {

    // private final Log log = new SystemStreamLog();

    /**
     * the wasm module to be used
     */
    @Parameter(required = true)
    private File wasmFile;

    /**
     * the target folder to generate classes
     */
    @Parameter(required = true, defaultValue = "${project.basedir}/target/classes")
    private File targetFolder;

    // private String adaptParams(int funcId, FunctionType t) {
    //     var base = "CompiledModule.func_" + funcId + "(";
    //     for (var i = 0; i < t.params().size(); i++) {
    //         base += "args[" + i + "].asInt(),";
    //     }
    //     base += " instance.memory(), instance)";
    //     return base;
    // }

    // private String generateReturn(int funcId, FunctionType t) {
    //     if (t.returns().size() > 0) {
    //         return "return new Value[] { Value.i32(" + adaptParams(funcId, t) + ")};";
    //     } else {
    //         return adaptParams(funcId, t) + ";\nreturn new Value[] {};";
    //     }
    // }

    @Override
    @SuppressWarnings("StringSplitter")
    public void execute() throws MojoExecutionException {
        try (var wasi = WasiPreview1.builder().withOpts(WasiOptions.builder().build()).build()) {
            var instance =
                    Instance.builder(Parser.parse(wasmFile))
                            .withMachineFactory(AotMachine::new)
                            .withHostImports(
                                    HostImports.builder()
                                            .addFunction(wasi.toHostFunctions())
                                            .build())
                            .withStart(false)
                            .build();
            var compiled = ((AotMachine) instance.getMachine()).compiledClass();
            var name = AotMachine.DEFAULT_CLASS_NAME;

            // debug
            // TODO: remove me!
            // for (var i = 0; i < instance.functionCount(); i++) {
            //     System.out.println(
            //             "funcs.put( "
            //                     + i
            //                     + ", (instance, args) -> { \n"
            //                     + generateReturn(i, instance.type(instance.functionType(i)))
            //                     + "});");
            // }

            var finalFolder = targetFolder.toPath();
            var split = name.split("\\.");
            for (int i = 0; i < (split.length - 1); i++) {
                finalFolder = finalFolder.resolve(split[i]);
            }
            finalFolder.toFile().mkdirs();
            try {
                Files.write(finalFolder.resolve(split[split.length - 1] + ".class"), compiled);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write " + name, e);
            }
        }
    }
}
