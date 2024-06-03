package com.dylibso.chicory.aot;

final class ByteArrayClassLoader extends ClassLoader {

    public ByteArrayClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> loadFromBytes(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
