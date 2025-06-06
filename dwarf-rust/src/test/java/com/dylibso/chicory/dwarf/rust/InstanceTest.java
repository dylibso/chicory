package com.dylibso.chicory.dwarf.rust;

import com.dylibso.chicory.dwarf.ParserException;
import org.junit.jupiter.api.Test;

public class InstanceTest {

    //    private InputStream getResource(String wasmResource) {
    //        return getClass().getResourceAsStream(wasmResource);
    //    }

    @Test
    public void shouldCountVowels() throws ParserException {
        // TODO:
        //        var wasmResource = "/compiled/log.go.tiny.wasm";
        //        var sourceMap = Parser.parse(getResource(wasmResource));
        //        var module = com.dylibso.chicory.wasm.Parser.parse(getResource(wasmResource));
        //        var machineFactory =
        //                MachineFactoryCompiler.builder(module)
        //                        .withSourceMap(sourceMap)
        //                        .build()
        //                        .compile()
        //                        .machineFactory();
        //
        //        var log =
        //                new HostFunction(
        //                        "env",
        //                        "log",
        //                        List.of(ValueType.I32),
        //                        List.of(ValueType.I32),
        //                        (inst, args) -> {
        //                            // set your debugger break point here.
        //                            return args;
        //                        });
        //
        //        var wasi =
        // WasiPreview1.builder().withOptions(WasiOptions.builder().build()).build();
        //        var imports =
        //
        // ImportValues.builder().addFunction(log).addFunction(wasi.toHostFunctions()).build();
        //
        //        var instance =
        //                Instance.builder(module)
        //                        .withMachineFactory(machineFactory)
        //                        .withImportValues(imports)
        //                        .build();
        //        var result = instance.export("add").apply(10, 5);
        //        assertEquals(15L, result[0]);
    }
}
