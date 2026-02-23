package com.dylibso.chicory.experimental.dircache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.Cache;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryCacheTest {

    public static class CacheWithHitCounter implements Cache {
        private final Cache cache;
        public AtomicInteger hits = new AtomicInteger(0);

        public CacheWithHitCounter(Cache cache) {
            this.cache = cache;
        }

        @Override
        public byte[] get(String key) throws IOException {
            byte[] result = cache.get(key);
            if (result != null) {
                hits.incrementAndGet();
            }
            return result;
        }

        @Override
        public void putIfAbsent(String key, byte[] data) throws IOException {
            cache.putIfAbsent(key, data);
        }
    }

    public static class NotFailingDirCache extends DirectoryCache {
        public NotFailingDirCache(Path baseDir) {
            super(baseDir);
        }

        @Override
        public byte[] get(String key) throws IOException {
            Path target = toFilePath(key);
            try {
                return Files.isRegularFile(target) ? Files.readAllBytes(target) : null;
            } catch (IOException e) {
                e.printStackTrace();
                // retry
                return get(key);
            }
        }
    }

    private void exerciseCountVowels(Instance instance) {
        var alloc = instance.export("alloc");
        var dealloc = instance.export("dealloc");
        var countVowels = instance.export("count_vowels");
        var memory = instance.memory();
        var message = "Hello, World!";
        var len = message.getBytes(UTF_8).length;
        int ptr = (int) alloc.apply(len)[0];
        memory.writeString(ptr, message);
        var result = countVowels.apply(ptr, len);
        dealloc.apply(ptr, len);
        assertEquals(3L, result[0]);
    }

    @Test
    public void shouldCacheCompiledResultInMemFS() {

        FileSystem fs = ZeroFs.newFileSystem(Configuration.unix());
        var cache = new CacheWithHitCounter(new DirectoryCache(fs.getPath("/cache")));
        var module =
                Parser.parse(
                        DirectoryCacheTest.class.getResourceAsStream(
                                "/compiled/count_vowels.rs.wasm"));

        var instance1 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();

        exerciseCountVowels(instance1);
        assertEquals(0, cache.hits.get());
        var instance2 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();
        exerciseCountVowels(instance2);
        assertEquals(1, cache.hits.get());
    }

    @Test
    public void shouldCacheCompiledResultNativeFS(@TempDir Path cacheDir) throws IOException {

        var module =
                Parser.parse(
                        DirectoryCacheTest.class.getResourceAsStream(
                                "/compiled/count_vowels.rs.wasm"));

        var cache = new CacheWithHitCounter(new DirectoryCache(cacheDir));

        var instance1 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();

        exerciseCountVowels(instance1);
        assertEquals(0, cache.hits.get());
        var instance2 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();
        exerciseCountVowels(instance2);
        assertEquals(1, cache.hits.get());
    }

    @Test
    public void testConcurrentAccessNativeFS(@TempDir Path cacheDir) throws IOException {
        var module =
                Parser.parse(
                        DirectoryCacheTest.class.getResourceAsStream(
                                "/compiled/count_vowels.rs.wasm"));

        // Execute the section concurrently 10 times

        var concurrency = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CompletableFuture<Void>[] futures = new CompletableFuture[concurrency];

        AtomicInteger hits = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            futures[i] =
                    CompletableFuture.runAsync(
                            () -> {
                                // each thread gets its own DirectoryCache so it simulates
                                // multiple
                                // processes accessing the disk cache concurrently
                                var cache =
                                        new CacheWithHitCounter(new NotFailingDirCache(cacheDir));

                                var instance1 =
                                        Instance.builder(module)
                                                .withMachineFactory(
                                                        MachineFactoryCompiler.builder(module)
                                                                .withCache(cache)
                                                                .compile())
                                                .build();
                                exerciseCountVowels(instance1);

                                var instance2 =
                                        Instance.builder(module)
                                                .withMachineFactory(
                                                        MachineFactoryCompiler.builder(module)
                                                                .withCache(cache)
                                                                .compile())
                                                .build();
                                exerciseCountVowels(instance2);

                                hits.addAndGet(cache.hits.get());
                            },
                            executor);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        // Some of the first 100 instance creates may result in a cache hit but ALL the 2nd
        // instance
        // creates should result in
        // cache hits.
        assertTrue(
                hits.get() >= concurrency,
                "Expected at least " + concurrency + " hits, but only got: " + hits.get());
    }
}
