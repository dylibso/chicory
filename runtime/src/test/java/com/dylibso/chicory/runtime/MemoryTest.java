package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MemoryTest {

    private static Stream<Arguments> growableMemoryImplementations() {
        return Stream.of(
                Arguments.of(
                        "ByteArrayMemory",
                        (Supplier<Memory>)
                                () -> new ByteArrayMemory(new MemoryLimits(1, 1000, true))),
                Arguments.of(
                        "ByteBufferMemory",
                        (Supplier<Memory>)
                                () -> new ByteBufferMemory(new MemoryLimits(1, 1000, true))));
    }

    /**
     * Test that concurrent grow and write operations don't lose writes.
     *
     * <p>This test has one thread incrementing a counter sequentially (read i, write i+1), while
     * another thread grows memory. If a write is lost due to a racy grow, the counter will have an
     * inconsistent value.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("growableMemoryImplementations")
    public void concurrentGrowAndAccessStressTest(String name, Supplier<Memory> memorySupplier)
            throws Exception {
        final int counterAddr = 0;
        final int growIterations = 200;

        var memory = memorySupplier.get();
        memory.writeI32(counterAddr, 0);

        // Grower thread: grows memory a fixed number of times
        var grower =
                CompletableFuture.runAsync(
                        () -> {
                            for (int i = 0; i < growIterations; i++) {
                                if (memory.grow(1) < 0) {
                                    throw new AssertionError("failed to grow memory");
                                }
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                        });

        // Counter thread: sequentially increment counter until grower is done
        var counter =
                CompletableFuture.runAsync(
                        () -> {
                            int i = 0;
                            while (!grower.isDone()) {
                                int read = (int) memory.readI32(counterAddr);
                                if (read != i) {
                                    throw new AssertionError(
                                            "inconsistent count at i=" + i + ", read=" + read);
                                }
                                i++;
                                memory.writeI32(counterAddr, i);
                            }
                        });

        CompletableFuture.allOf(counter, grower).get(5, TimeUnit.SECONDS);
    }
}
