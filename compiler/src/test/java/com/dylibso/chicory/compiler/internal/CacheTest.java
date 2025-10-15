package com.dylibso.chicory.compiler.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.Cache;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CacheTest {

    static class MockCacheImpl implements Cache {
        private final Map<String, CompilerResult> cache = new HashMap<>();
        public boolean cacheHit;

        @Override
        public void put(String key, CompilerResult content) {
            cache.put(key, content);
        }

        @Override
        public CompilerResult get(String key) {
            var result = cache.get(key);
            if (result != null) {
                cacheHit = true;
            }
            return result;
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
    public void shouldCacheCompiledResult() {
        var cache = new MockCacheImpl();
        var module =
                Parser.parse(CacheTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));

        var instance1 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();

        exerciseCountVowels(instance1);
        assertFalse(cache.cacheHit);
        var instance2 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();
        exerciseCountVowels(instance2);
        assertTrue(cache.cacheHit);
    }

    static class MockEhCacheImpl implements Cache, AutoCloseable {
        private final CacheManager cacheManager;
        private final org.ehcache.Cache<String, CompilerResult> cache;
        public boolean cacheHit;

        public MockEhCacheImpl() {
            cacheManager =
                    CacheManagerBuilder.newCacheManagerBuilder()
                            .withCache(
                                    "myCache",
                                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                            String.class,
                                            CompilerResult.class,
                                            ResourcePoolsBuilder.heap(10)))
                            .build(true);

            cache = cacheManager.getCache("myCache", String.class, CompilerResult.class);
        }

        @Override
        public void put(String key, CompilerResult content) {
            cache.put(key, content);
        }

        @Override
        public CompilerResult get(String key) {
            var result = cache.get(key);
            if (result != null) {
                cacheHit = true;
            }
            return result;
        }

        @Override
        public void close() {
            cacheManager.removeCache("myCache");
            cacheManager.close();
        }
    }

    @Test
    public void shouldUseEhCache() {
        var cache = new MockEhCacheImpl();
        var module =
                Parser.parse(CacheTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));

        var instance1 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();

        exerciseCountVowels(instance1);
        assertFalse(cache.cacheHit);

        var instance2 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache).compile())
                        .build();
        exerciseCountVowels(instance2);
        assertTrue(cache.cacheHit);
    }

    static class MockPersistentEhCacheImpl implements Cache, AutoCloseable {
        private final PersistentCacheManager cacheManager;
        private final org.ehcache.Cache<String, CompilerResult> cache;
        public boolean cacheHit;

        public MockPersistentEhCacheImpl(Path tempDir) {
            cacheManager =
                    CacheManagerBuilder.newCacheManagerBuilder()
                            .with(CacheManagerBuilder.persistence(tempDir.toFile()))
                            .withCache(
                                    "myCache",
                                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                            String.class,
                                            CompilerResult.class,
                                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                    .heap(10, EntryUnit.ENTRIES)
                                                    .disk(10, MemoryUnit.MB, true)))
                            .build(true);

            cache = cacheManager.getCache("myCache", String.class, CompilerResult.class);
        }

        @Override
        public void put(String key, CompilerResult content) {
            cache.put(key, content);
        }

        @Override
        public CompilerResult get(String key) {
            var result = cache.get(key);
            if (result != null) {
                cacheHit = true;
            }
            return result;
        }

        @Override
        public void close() {
            cacheManager.removeCache("myCache");
            cacheManager.close();
        }
    }

    @Test
    public void shouldUsePeristentEhCache(@TempDir Path tempDir) {
        var cache1 = new MockPersistentEhCacheImpl(tempDir);
        var module =
                Parser.parse(CacheTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));

        var instance1 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache1).compile())
                        .build();

        exerciseCountVowels(instance1);
        assertFalse(cache1.cacheHit);

        cache1.close();

        var cache2 = new MockPersistentEhCacheImpl(tempDir);
        var instance2 =
                Instance.builder(module)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module).withCache(cache2).compile())
                        .build();
        exerciseCountVowels(instance2);
        assertTrue(cache2.cacheHit);
    }
}
