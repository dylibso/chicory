package com.dylibso.chicory.compiler.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.compiler.Cache;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class CacheTest {

    static class MockCache implements Cache {
        ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

        @Override
        public byte[] get(String key) throws IOException {
            return cache.get(key);
        }

        @Override
        public void putIfAbsent(String key, byte[] data) throws IOException {
            cache.putIfAbsent(key, data);
        }
    }

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
    public void shouldCacheCompiledResultInMem() {

        var cache = new CacheWithHitCounter(new MockCache());
        var module =
                Parser.parse(CacheTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));

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
}
