package com.dylibso.chicory.compiler.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dylibso.chicory.compiler.InterpreterFallback;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CacheTest {

    static class Bytecode implements Serializable {
        private Map<String, byte[]> entries;

        Bytecode(Map<String, byte[]> entries) {
            this.entries = entries;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.writeObject(entries);
        }

        private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
            this.entries = (Map<String, byte[]>) ois.readObject();
        }
    }

    static class MyCache implements AutoCloseable {
        private final PersistentCacheManager cacheManager;
        private final Cache<Integer, Bytecode> cache;
        public boolean cacheHit;

        public MyCache(Path tempDir) {
            cacheManager =
                    CacheManagerBuilder.newCacheManagerBuilder()
                            .with(CacheManagerBuilder.persistence(tempDir.toFile()))
                            .withCache(
                                    "myCache",
                                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                            Integer.class,
                                            Bytecode.class,
                                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                    .heap(10, EntryUnit.ENTRIES)
                                                    .disk(10, MemoryUnit.MB, true)))
                            .build(true);
            cache = cacheManager.getCache("myCache", Integer.class, Bytecode.class);
        }

        public Bytecode get(Integer key) {
            return cache.get(key);
        }

        public void put(Integer key, Bytecode value) {
            cache.put(key, value);
        }

        @Override
        public void close() {
            cacheManager.removeCache("myCache");
            cacheManager.close();
        }
    }

    @Test
    public void cacheCompiledCode(@TempDir Path tempDir) {
        var wasm = CacheTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm");

        var module = Parser.parse(wasm);
        var machineName = "Machine";

        final var cache = new AtomicReference<MyCache>();
        cache.set(new MyCache(tempDir));

        Function<Instance, Machine> factory =
                (inst) -> {
                    var cached = cache.get().get(module.hashCode());
                    Bytecode bytecode;
                    if (cached != null) {
                        bytecode = cached;
                    } else {
                        var compiler =
                                Compiler.builder(module)
                                        .withClassName(machineName)
                                        .withClassCollectorFactory(ByteClassCollector::new)
                                        .withInterpreterFallback(InterpreterFallback.WARN)
                                        .build();
                        var result = compiler.compile();

                        bytecode = new Bytecode(result.classBytes());
                        cache.get().put(module.hashCode(), bytecode);
                    }

                    var classLoadingCollector = new ClassLoadingCollector();
                    for (var e : bytecode.entries.entrySet()) {
                        if (e.getKey().equals(machineName)) {
                            classLoadingCollector.putMainClass(machineName, e.getValue());
                        } else {
                            classLoadingCollector.put(e.getKey(), e.getValue());
                        }
                    }
                    return classLoadingCollector.machineFactory().apply(inst);
                };

        var instance1 = Instance.builder(module).withMachineFactory(factory).build();

        exerciseCountVowels(instance1);
        assertFalse(cache.get().cacheHit);

        cache.get().close();
        cache.set(new MyCache(tempDir));

        var instance2 = Instance.builder(module).withMachineFactory(factory).build();

        exerciseCountVowels(instance2);
        assertFalse(cache.get().cacheHit);
    }

    private static void exerciseCountVowels(Instance instance) {
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
}
