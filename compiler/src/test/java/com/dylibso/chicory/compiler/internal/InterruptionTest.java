package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.methodNameForFunc;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class InterruptionTest {
    @Test
    public void shouldInterruptLoop() throws InterruptedException {
        var module = Parser.parse(CorpusResources.getResource("compiled/infinite-loop.c.wasm"));
        var instance =
                Instance.builder(module)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();

        var function = instance.export("run");
        assertInterruption(function::apply, functionIdx(module, "run"));
    }

    @Test
    public void shouldInterruptCall() throws InterruptedException {
        var module = Parser.parse(CorpusResources.getResource("compiled/power.c.wasm"));
        var instance =
                Instance.builder(module)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();
        var function = instance.export("run");
        assertInterruption(() -> function.apply(100), functionIdx(module, "run"));
    }

    private static int functionIdx(WasmModule module, String name) {
        for (int i = 0; i < module.exportSection().exportCount(); i++) {
            var export = module.exportSection().getExport(i);
            if (export.name().equals(name)) {
                return export.index();
            }
        }
        throw new IllegalArgumentException("Function not found");
    }

    private static void assertInterruption(Runnable function, int funcIdx)
            throws InterruptedException {
        AtomicBoolean interrupted = new AtomicBoolean();
        Runnable runnable =
                () -> {
                    var e = assertThrows(ChicoryException.class, function::run);
                    assertEquals("Thread interrupted", e.getMessage());
                    interrupted.set(true);
                };

        // start the thread and wait for WASM execution
        Thread thread = new Thread(runnable);
        thread.start();
        waitForWasmExecution(thread, funcIdx);

        // interrupt thread and verify it aborts
        thread.interrupt();
        SECONDS.timedJoin(thread, 10);
        assertTrue(interrupted.get());
    }

    private static void waitForWasmExecution(Thread thread, int funcIdx)
            throws InterruptedException {
        long start = System.nanoTime();
        while (true) {
            if ((System.nanoTime() - start) >= SECONDS.toNanos(10)) {
                throw new AssertionError("Timed out waiting for execution to start");
            }

            for (StackTraceElement element : thread.getStackTrace()) {
                var className = element.getClassName();
                var methodName = element.getMethodName();
                if (className.startsWith(Compiler.DEFAULT_CLASS_NAME + "FuncGroup_")
                        && methodName.equals(methodNameForFunc(funcIdx))) {
                    return;
                }
            }

            MILLISECONDS.sleep(10);
        }
    }
}
