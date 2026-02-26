package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MemoryTest {

    private static Stream<Arguments> memoryImplementations() {
        return Stream.of(
                Arguments.of(
                        "ByteArrayMemory",
                        (Supplier<Memory>) () -> new ByteArrayMemory(new MemoryLimits(2, 2))),
                Arguments.of(
                        "ByteBufferMemory",
                        (Supplier<Memory>) () -> new ByteBufferMemory(new MemoryLimits(2, 2))));
    }

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

    /**
     * Test that overlapping copy with dest > src preserves data correctly.
     * This is the memmove semantics required by Wasm memory.copy.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("memoryImplementations")
    public void overlappingCopyDestAfterSrc(String name, Supplier<Memory> memorySupplier) {
        var memory = memorySupplier.get();

        // Write known pattern [1, 2, 3, 4, 5, 6, 7, 8] at addr 100
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        memory.write(100, data, 0, data.length);

        // Overlapping copy: src=100, dest=104, size=8
        // Expected: addr 104..111 = [1, 2, 3, 4, 5, 6, 7, 8]
        memory.copy(104, 100, 8);

        assertArrayEquals(data, memory.readBytes(104, 8));
    }

    /**
     * Test overlapping copy with dest < src (forward copy path).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("memoryImplementations")
    public void overlappingCopyDestBeforeSrc(String name, Supplier<Memory> memorySupplier) {
        var memory = memorySupplier.get();

        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        memory.write(104, data, 0, data.length);

        // Overlapping copy: src=104, dest=100, size=8
        memory.copy(100, 104, 8);

        assertArrayEquals(data, memory.readBytes(100, 8));
    }

    /**
     * Test overlapping copy across page boundaries (PAGE_SIZE = 65536).
     * This exercises the chunked backward copy logic when overlap spans pages.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("memoryImplementations")
    public void overlappingCopyCrossPageBoundary(String name, Supplier<Memory> memorySupplier) {
        var memory = memorySupplier.get();

        int pageSize = 65536;
        // Place data straddling the page boundary
        int src = pageSize - 4;
        byte[] data = {10, 20, 30, 40, 50, 60, 70, 80};
        memory.write(src, data, 0, data.length);

        // Overlapping copy with dest > src, crossing page boundary
        int dest = src + 4;
        memory.copy(dest, src, data.length);

        assertArrayEquals(data, memory.readBytes(dest, data.length));
    }

    /**
     * Test non-overlapping copy across pages.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("memoryImplementations")
    public void nonOverlappingCopyCrossPage(String name, Supplier<Memory> memorySupplier) {
        var memory = memorySupplier.get();

        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        memory.write(100, data, 0, data.length);

        // Copy to a completely different location on a different page
        int dest = 65536 + 200;
        memory.copy(dest, 100, data.length);

        assertArrayEquals(data, memory.readBytes(dest, data.length));
        // Source should be unchanged
        assertArrayEquals(data, memory.readBytes(100, data.length));
    }
}
