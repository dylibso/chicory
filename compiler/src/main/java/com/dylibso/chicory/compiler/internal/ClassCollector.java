package com.dylibso.chicory.compiler.internal;

import java.util.Map;

/**
 * A class collector exposes methods to resolve class files
 * from the classpath and collecting bytes representing classes.
 */
public interface ClassCollector {
    /**
     * Main entry point
     */
    String mainClassName();

    void putMainClass(String className, byte[] bytes);

    void put(String name, byte[] data);

    void putAll(ClassCollector collector);

    Map<String, byte[]> classBytes();

    byte[] resolve(Class<?> clazz);
}
