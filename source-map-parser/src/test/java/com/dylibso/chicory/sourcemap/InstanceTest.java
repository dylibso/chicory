package com.dylibso.chicory.sourcemap;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ValueType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstanceTest {

    private InputStream getResource(String wasmResource) {
        return getClass().getResourceAsStream(wasmResource);
    }

    @Test
    public void shouldCountVowels() throws SourceMapException {

        var wasmResource = "/compiled/log.go.tiny.wasm";
        var sourceMap = SourceMapParser.parse(getResource(wasmResource));
        var module = Parser.parse(getResource(wasmResource));
        var machineFactory = AotCompiler.builder(module).withSourceMap(sourceMap).build().compile().machineFactory();

        var log =
                new HostFunction(
                        "env",
                        "log",
                        List.of(ValueType.I32),
                        List.of(ValueType.I32),
                        (inst, args) -> {
                            // set your debugger break point here.
                            return args;
                        });

        var wasi = WasiPreview1.builder().withOptions(WasiOptions.builder().build()).build();
        var imports = ImportValues.builder().addFunction(log).addFunction(wasi.toHostFunctions()).build();

        var instance = Instance.builder(module).withMachineFactory(machineFactory).withImportValues(imports).build();
        var result =  instance.export("add").apply(10,5);
        assertEquals(15L, result[0]);
    }



}
