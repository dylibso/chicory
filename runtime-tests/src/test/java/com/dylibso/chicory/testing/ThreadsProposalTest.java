package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ThreadsProposalTest {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(ThreadsProposalTest.class.getResourceAsStream("/" + fileName));
    }

    private static Stream<Arguments> memoryImplementations() {
        return Stream.of(
                Arguments.of(new ByteArrayMemory(new MemoryLimits(1, 1, true))),
                Arguments.of(new ByteBufferMemory(new MemoryLimits(1, 1, true))));
    }

    @ParameterizedTest
    @MethodSource("memoryImplementations")
    public void threadsExample(Memory memory) throws Exception {
        // attempting to validate the threads implementation:
        // https://github.com/WebAssembly/threads/blob/b2567bff61ee6fbe731934f0ed17a5d48dc9ab01/proposals/threads/Overview.md#example
        var module = loadModule("compiled/threads-example.wat.wasm");
        var mutexAddr = 0;

        var mainInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        var workerInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        // Lock on main
        var mainLocked = mainInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(1, mainLocked);

        // the worker instance cannot acquire the lock
        var workerLocked = workerInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(0, workerLocked);

        // unlock main
        mainInstance.exports().function("unlockMutex").apply(mutexAddr);

        // now lock from worker
        workerLocked = workerInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(1, workerLocked);

        // main cannot lock
        mainLocked = mainInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(0, mainLocked);

        workerInstance.exports().function("unlockMutex").apply(mutexAddr);

        // now more interesting
        // main gets the lock
        mainLocked = mainInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(1, mainLocked);

        var workerAcquiredLock = new AtomicBoolean(false);
        Thread t =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            workerInstance.exports().function("lockMutex").apply(mutexAddr);
                            workerAcquiredLock.set(true);
                            workerInstance.exports().function("unlockMutex").apply(mutexAddr);
                        });
        t.start();

        // unlock the mutex to let the worker acquire the lock
        mainInstance.exports().function("unlockMutex").apply(mutexAddr);

        t.join();

        assertTrue(workerAcquiredLock.get());
    }

    @ParameterizedTest
    @MethodSource("memoryImplementations")
    public void threadsExampleWake(Memory memory) throws Exception {
        var module = loadModule("compiled/threads-example.wat.wasm");
        var mutexAddr = 0;

        var mainInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        var workerInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        // Lock on main
        var mainLocked = mainInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(1, mainLocked);

        var workerAcquireLock = new AtomicInteger(-1);
        Thread workerT =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            var result =
                                    workerInstance
                                            .exports()
                                            .function("lockMutexWithTimeout")
                                            .apply(mutexAddr, 1)[0];
                            workerAcquireLock.set((int) result);
                        });
        Thread mainT =
                new Thread(
                        () -> {
                            // unlock the mutex
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            mainInstance.exports().function("unlockMutex").apply(mutexAddr);
                        });
        workerT.start();
        mainT.start();

        mainT.join();
        workerT.join();

        // 0 == ok -> unlocked and notified
        assertEquals(0, workerAcquireLock.get());
    }

    @ParameterizedTest
    @MethodSource("memoryImplementations")
    public void threadsExampleNotEqual(Memory memory) throws Exception {
        var module = loadModule("compiled/threads-example.wat.wasm");
        var mutexAddr = 0;

        var mainInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        var workerInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        // Lock on main
        var mainLocked = mainInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(1, mainLocked);

        var workerAcquireLock = new AtomicInteger(-1);
        Thread workerT =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            var result =
                                    workerInstance
                                            .exports()
                                            .function("lockMutexWithTimeout")
                                            .apply(mutexAddr, 2)[0];
                            workerAcquireLock.set((int) result);
                        });
        Thread mainT =
                new Thread(
                        () -> {
                            // unlock the mutex
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            mainInstance.exports().function("unlockMutex").apply(mutexAddr);
                        });
        workerT.start();
        mainT.start();

        mainT.join();
        workerT.join();

        // 1 == not equal
        assertEquals(1, workerAcquireLock.get());
    }

    @ParameterizedTest
    @MethodSource("memoryImplementations")
    public void threadsExampleTimeout(Memory memory) throws Exception {
        var module = loadModule("compiled/threads-example.wat.wasm");
        var mutexAddr = 0;

        var mainInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        var workerInstance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build())
                        .build();

        // Lock on main
        var mainLocked = mainInstance.exports().function("tryLockMutex").apply(mutexAddr)[0];
        assertEquals(1, mainLocked);

        var workerAcquireLock = new AtomicInteger(-1);
        Thread workerT =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            var result =
                                    workerInstance
                                            .exports()
                                            .function("lockMutexWithTimeout")
                                            .apply(mutexAddr, 1)[0];
                            workerAcquireLock.set((int) result);
                        });
        workerT.start();
        workerT.join();

        // 2 == timeout
        assertEquals(2, workerAcquireLock.get());
    }
}
