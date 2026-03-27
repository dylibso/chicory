package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class ChicoryRunner implements WasmRunner {

    private static final long TIMEOUT_SECONDS = 10;

    private static final ThreadFactory DAEMON_THREAD_FACTORY =
            r -> {
                var t = new Thread(r, "chicory-fuzz-runner");
                t.setDaemon(true);
                return t;
            };

    private final Function<Instance, Machine> machineFactory;

    public ChicoryRunner() {
        this(null);
    }

    public ChicoryRunner(Function<Instance, Machine> machineFactory) {
        this.machineFactory = machineFactory;
    }

    @Override
    public String run(File wasmFile, String functionName, List<String> params) throws Exception {
        Callable<String> task =
                () -> {
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
                };

        // Use daemon threads so leaked threads (from timeouts where the WASM
        // execution doesn't respond to interrupts) don't prevent JVM exit.
        ExecutorService executor = Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY);
        try {
            var future = executor.submit(task);
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Execution timed out after " + TIMEOUT_SECONDS + "s", e);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } finally {
            executor.shutdownNow();
        }
    }
}
