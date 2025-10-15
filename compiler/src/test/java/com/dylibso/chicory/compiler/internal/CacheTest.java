package com.dylibso.chicory.compiler.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.Cache;
import com.dylibso.chicory.compiler.DirectoryCache;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class CacheTest {

    static class MockCacheImpl implements Cache {
        public boolean cacheHit;

        FileSystem fs = ZeroFs.newFileSystem(Configuration.unix());
        DirectoryCache cache = new DirectoryCache(fs.getPath("/cache"));

        @Override
        public Path get(String key) {
            Path result = cache.get(key);
            if (result != null) {
                cacheHit = true;
            }
            return result;
        }

        @Override
        public TempDir createTempDir() throws IOException {
            return cache.createTempDir();
        }

        @Override
        public Path put(String key, TempDir tmpDir) throws IOException {
            return cache.put(key, tmpDir);
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
}
