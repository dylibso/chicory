package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ThreadsProposalTest {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    @FunctionalInterface
    interface LockWithTimeout {
        int lock(Instance instance, int mutexAddr, long expected);
    }

    private static MemoryLimits memoryLimits = new MemoryLimits(1, 1, true);
    private static List<Supplier<Memory>> memories =
            List.of(
                    () -> new ByteArrayMemory(memoryLimits),
                    () -> new ByteBufferMemory(memoryLimits));
    private static List<Function<Instance.Builder, Instance.Builder>> machines =
            List.of(
                    // interpreter
                    (instBuilder) -> instBuilder.withMachineFactory(InterpreterMachine::new),
                    // runtime compiler
                    (instBuilder) ->
                            instBuilder.withMachineFactory(MachineFactoryCompiler::compile),
                    // build time compiler
                    (instBuilder) -> instBuilder.withMachineFactory(ThreadsExampleModule::create));
    private static List<LockWithTimeout> locks =
            List.of(
                    ThreadsProposalTest::lockMutexWithTimeout,
                    ThreadsProposalTest::lock64MutexWithTimeout);

    private static Stream<Arguments> memoryAndMachinesImplementations() {
        List<Arguments> args = new ArrayList<>();
        for (var mem : memories) {
            for (var machine : machines) {
                args.add(Arguments.of(mem.get(), machine));
            }
        }
        return args.stream();
    }

    private static Stream<Arguments> memoryMachinesAndLocksImplementations() {
        List<Arguments> args = new ArrayList<>();
        for (var mem : memories) {
            for (var machine : machines) {
                for (var lock : locks) {
                    args.add(Arguments.of(mem.get(), machine, lock));
                }
            }
        }
        return args.stream();
    }

    // originally from:
    // https://github.com/WebAssembly/threads/blob/b2567bff61ee6fbe731934f0ed17a5d48dc9ab01/proposals/threads/Overview.md#example
    private static WasmModule module = loadModule("compiled/threads-example.wat.wasm");

    private static Instance newInstance(
            Memory memory, Function<Instance.Builder, Instance.Builder> machineInject) {
        var builder =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build());

        return machineInject.apply(builder).build();
    }

    private static int tryLockMutex(Instance instance, int mutexAddr) {
        return (int) instance.exports().function("tryLockMutex").apply(mutexAddr)[0];
    }

    private static int lockMutexWithTimeout(Instance instance, int mutexAddr, long expected) {
        return (int)
                instance.exports().function("lockMutexWithTimeout").apply(mutexAddr, expected)[0];
    }

    private static int lock64MutexWithTimeout(Instance instance, int mutexAddr, long expected) {
        return (int)
                instance.exports().function("lock64MutexWithTimeout").apply(mutexAddr, expected)[0];
    }

    private static void lockMutex(Instance instance, int mutexAddr) {
        instance.exports().function("lockMutex").apply(mutexAddr);
    }

    private static void unlockMutex(Instance instance, int mutexAddr) {
        instance.exports().function("unlockMutex").apply(mutexAddr);
    }

    @ParameterizedTest
    @MethodSource("memoryAndMachinesImplementations")
    public void threadsExample(
            Memory memory, Function<Instance.Builder, Instance.Builder> machineInject)
            throws Exception {
        var mutexAddr = 0;
        var mainInstance = newInstance(memory, machineInject);
        var workerInstance = newInstance(memory, machineInject);

        // Lock on main
        var mainLocked = tryLockMutex(mainInstance, mutexAddr);
        assertEquals(1, mainLocked);

        // the worker instance cannot acquire the lock
        var workerLocked = tryLockMutex(workerInstance, mutexAddr);
        assertEquals(0, workerLocked);

        // unlock main
        unlockMutex(mainInstance, mutexAddr);

        // now lock from worker
        workerLocked = tryLockMutex(workerInstance, mutexAddr);
        assertEquals(1, workerLocked);

        // main cannot lock
        mainLocked = tryLockMutex(mainInstance, mutexAddr);
        assertEquals(0, mainLocked);

        workerInstance.exports().function("unlockMutex").apply(mutexAddr);

        // now more interesting
        // main gets the lock
        mainLocked = tryLockMutex(mainInstance, mutexAddr);
        assertEquals(1, mainLocked);

        var workerAcquiredLock = new AtomicBoolean(false);
        Thread t =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            lockMutex(workerInstance, mutexAddr);
                            workerAcquiredLock.set(true);
                            unlockMutex(workerInstance, mutexAddr);
                        });
        t.start();

        // unlock the mutex to let the worker acquire the lock
        unlockMutex(mainInstance, mutexAddr);

        t.join();

        assertTrue(workerAcquiredLock.get());
    }

    @ParameterizedTest
    @MethodSource("memoryMachinesAndLocksImplementations")
    public void threadsExampleWake(
            Memory memory,
            Function<Instance.Builder, Instance.Builder> machineInject,
            LockWithTimeout lockWithTimeout)
            throws Exception {
        var mutexAddr = 0;
        var mainInstance = newInstance(memory, machineInject);
        var workerInstance = newInstance(memory, machineInject);

        // Lock on main
        var mainLocked = tryLockMutex(mainInstance, mutexAddr);
        assertEquals(1, mainLocked);

        var workerAcquireLock = new AtomicInteger(-1);
        Thread workerT =
                new Thread(
                        () -> {
                            var result = lockWithTimeout.lock(workerInstance, mutexAddr, 1);
                            workerAcquireLock.set(result);
                        });
        workerT.start();
        Thread.sleep(200);

        unlockMutex(mainInstance, mutexAddr);
        workerT.join();

        // 0 == ok -> unlocked and notified
        assertEquals(0, workerAcquireLock.get());
    }

    @ParameterizedTest
    @MethodSource("memoryMachinesAndLocksImplementations")
    public void threadsExampleNotEqual(
            Memory memory,
            Function<Instance.Builder, Instance.Builder> machineInject,
            LockWithTimeout lockWithTimeout)
            throws Exception {
        var mutexAddr = 0;
        var mainInstance = newInstance(memory, machineInject);
        var workerInstance = newInstance(memory, machineInject);

        // Lock on main
        var mainLocked = tryLockMutex(mainInstance, mutexAddr);
        assertEquals(1, mainLocked);

        var workerAcquireLock = new AtomicInteger(-1);
        Thread workerT =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            var result = lockWithTimeout.lock(workerInstance, mutexAddr, 2);
                            workerAcquireLock.set(result);
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
                            unlockMutex(mainInstance, mutexAddr);
                        });
        workerT.start();
        mainT.start();

        mainT.join();
        workerT.join();

        // 1 == not equal
        assertEquals(1, workerAcquireLock.get());
    }

    @ParameterizedTest
    @MethodSource("memoryMachinesAndLocksImplementations")
    public void threadsExampleTimeout(
            Memory memory,
            Function<Instance.Builder, Instance.Builder> machineInject,
            LockWithTimeout lockWithTimeout)
            throws Exception {
        var mutexAddr = 0;
        var mainInstance = newInstance(memory, machineInject);
        var workerInstance = newInstance(memory, machineInject);

        // Lock on main
        var mainLocked = tryLockMutex(mainInstance, mutexAddr);
        assertEquals(1, mainLocked);

        var workerAcquireLock = new AtomicInteger(-1);
        Thread workerT =
                new Thread(
                        () -> {
                            // worker remains ready for locking
                            var result = lockWithTimeout.lock(workerInstance, mutexAddr, 1);
                            workerAcquireLock.set(result);
                        });
        workerT.start();
        workerT.join();

        // 2 == timeout
        assertEquals(2, workerAcquireLock.get());
    }
}
