package com.dylibso.chicory.runtime;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.Parser;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class InterruptionTest {
    @Test
    public void shouldInterruptLoop() throws InterruptedException {
        var instance =
                Instance.builder(
                                Parser.parse(
                                        CorpusResources.getResource(
                                                "compiled/infinite-loop.c.wasm")))
                        .build();
        var function = instance.export("run");
        assertInterruption(function::apply);
    }

    @Test
    public void shouldInterruptCall() throws InterruptedException {
        var instance =
                Instance.builder(Parser.parse(CorpusResources.getResource("compiled/power.c.wasm")))
                        .build();
        var function = instance.export("run");
        assertInterruption(() -> function.apply(100));
    }

    private static void assertInterruption(Runnable function) throws InterruptedException {
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
        waitForWasmExecution(thread);

        // interrupt thread and verify it aborts
        thread.interrupt();
        SECONDS.timedJoin(thread, 10);
        assertTrue(interrupted.get());
    }

    private static void waitForWasmExecution(Thread thread) throws InterruptedException {
        long start = System.nanoTime();
        while (true) {
            if ((System.nanoTime() - start) >= SECONDS.toNanos(10)) {
                throw new AssertionError("Timed out waiting for execution to start");
            }

            for (StackTraceElement element : thread.getStackTrace()) {
                if (element.getClassName().equals(InterpreterMachine.class.getName())
                        && element.getMethodName().equals("eval")) {
                    return;
                }
            }

            MILLISECONDS.sleep(10);
        }
    }
}
