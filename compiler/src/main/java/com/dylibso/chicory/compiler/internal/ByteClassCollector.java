package com.dylibso.chicory.compiler.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link ClassCollector} that stores all the classes
 * in a map. It resolves a given class to bytes by looking into classpath.
 *
 */
public class ByteClassCollector implements ClassCollector {
    private final Map<String, byte[]> classBytes = new HashMap<>();
    private String mainClass;

    public ByteClassCollector() {}

    @Override
    public String mainClassName() {
        return mainClass;
    }

    @Override
    public void putMainClass(String className, byte[] bytes) {
        this.mainClass = className;
        classBytes.put(className, bytes);
    }

    @Override
    public void put(String className, byte[] bytes) {
        classBytes.put(className, bytes);
    }

    @Override
    public void putAll(ClassCollector collector) {
        var classBytes = collector.classBytes();
        this.classBytes.putAll(classBytes);
    }

    @Override
    public Map<String, byte[]> classBytes() {
        return Collections.unmodifiableMap(classBytes);
    }

    @Override
    public byte[] resolve(Class<?> clazz) {
        return Shader.getBytecode(clazz);
    }
}
