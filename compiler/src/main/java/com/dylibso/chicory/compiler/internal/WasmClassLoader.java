package com.dylibso.chicory.compiler.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;

final class WasmClassLoader extends ClassLoader {

    private final Map<String, byte[]> resources = new HashMap<>();

    public WasmClassLoader() {
        super(WasmClassLoader.class.getClassLoader());
    }

    public void addResource(String name, byte[] data) {
        resources.put(name, data);
    }

    public Class<?> loadFromBytes(byte[] bytes) {
        var name = new ClassReader(bytes).getClassName().replace('/', '.');
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] data = resources.get(name);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }
        return super.getResourceAsStream(name);
    }
}
