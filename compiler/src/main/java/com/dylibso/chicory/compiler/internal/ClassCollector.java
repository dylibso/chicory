package com.dylibso.chicory.compiler.internal;

import java.util.Map;

public interface ClassCollector {
    void putMainClass(String className, byte[] bytes);

    String mainClass();

    void put(String name, byte[] data);

    void putAll(ClassCollector collector);

    Map<String, byte[]> classBytes();

    byte[] resolve(Class<?> clazz);
}
