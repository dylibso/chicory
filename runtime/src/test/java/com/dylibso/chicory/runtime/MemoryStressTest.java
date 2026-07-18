package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MemoryStressTest {

    static final class RwLock {
        private static final int WRITER_BIT = 0x80000000;
        private static final int READER_MASK = 0x7FFFFFFF;

        private final Memory memory;
        private final int addr;

        public RwLock(Memory memory, int addr) {
            this.memory = memory;
            this.addr = addr;
        }

        public void lock() {
            while (true) {
                int s = memory.atomicReadInt(addr);
                if ((s & WRITER_BIT) != 0) {
                    memory.atomicWait(addr, s, -1L);
                    continue;
                }
                if (memory.atomicCmpxchgInt(addr, s, s | WRITER_BIT) == s) {
                    break;
                }
            }
            while (true) {
                int s = memory.atomicReadInt(addr);
                if ((s & READER_MASK) == 0) {
                    return;
                }
                memory.atomicWait(addr, s, -1L);
            }
        }

        public void unlock() {
            memory.atomicWriteInt(addr, 0);
            memory.atomicNotify(addr, -1);
        }

        public void lockShared() {
            while (true) {
                int s = memory.atomicReadInt(addr);
                if ((s & WRITER_BIT) != 0) {
                    memory.atomicWait(addr, s, -1L);
                    continue;
                }
                if (memory.atomicCmpxchgInt(addr, s, s + 1) == s) {
                    return;
                }
            }
        }

        public void unlockShared() {
            int prev = memory.atomicAddInt(addr, -1);
            if ((prev & READER_MASK) == 1 && (prev & WRITER_BIT) != 0) {
                memory.atomicNotify(addr, -1);
            }
        }
    }

    private static Stream<Arguments> sharedMemoryImplementations() {
        return Stream.of(
                Arguments.of(
                        "ByteArrayMemory",
                        (Supplier<Memory>) () -> new ByteArrayMemory(new MemoryLimits(1, 1, true))),
                Arguments.of(
                        "ByteBufferMemory",
                        (Supplier<Memory>)
                                () -> new ByteBufferMemory(new MemoryLimits(1, 1, true))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sharedMemoryImplementations")
    public void rwLockSurvivesContention(String name, Supplier<Memory> memorySupplier)
            throws InterruptedException {
        final RwLock lock = new RwLock(memorySupplier.get(), 0);
        final long deadline = System.currentTimeMillis() + 3_000;

        final List<Thread> threads = new ArrayList<Thread>();
        for (int r = 0; r < 6; r++) {
            threads.add(
                    new Thread(
                            () -> {
                                while (System.currentTimeMillis() < deadline) {
                                    lock.lockShared();
                                    lock.unlockShared();
                                }
                            },
                            "reader-" + r));
        }
        threads.add(
                new Thread(
                        () -> {
                            while (System.currentTimeMillis() < deadline) {
                                lock.lock();
                                lock.unlock();
                            }
                        },
                        "writer"));

        for (Thread t : threads) {
            t.setDaemon(true);
            t.start();
        }
        final long joinDeadline = deadline + 1000;
        for (Thread t : threads) {
            final long remaining = Long.max(10, joinDeadline - System.currentTimeMillis());
            t.join(remaining);
        }

        final boolean anyThreadBlocked = threads.stream().anyMatch(Thread::isAlive);
        assertFalse(anyThreadBlocked);
    }
}
