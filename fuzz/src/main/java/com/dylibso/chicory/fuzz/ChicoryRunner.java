package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.runtime.ChicoryInterruptedException;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.util.List;
import java.util.function.Function;

public class ChicoryRunner implements WasmRunner {

    private final Function<Instance, Machine> machineFactory;

    public ChicoryRunner() {
        this(null);
    }

    public ChicoryRunner(Function<Instance, Machine> machineFactory) {
        this.machineFactory = machineFactory;
    }

    @Override
    public String run(File wasmFile, String functionName, List<String> params) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            throw new ChicoryInterruptedException("Thread interrupted");
        }
        var module = Parser.parse(wasmFile);
        var builder = Instance.builder(module).withInitialize(true).withStart(false);
        if (machineFactory != null) {
            builder.withMachineFactory(machineFactory);
        }
        var instance = builder.build();

        var type = instance.exportType(functionName);
        var export = instance.export(functionName);
        var longParams = new long[type.params().size()];
        for (var i = 0; i < type.params().size(); i++) {
            longParams[i] = Long.parseLong(params.get(i));
        }

        var result = export.apply(longParams);
        var sb = new StringBuilder();
        if (result != null) {
            for (var r : result) {
                sb.append(r).append("\n");
            }
        }
        return sb.toString();
    }
}
