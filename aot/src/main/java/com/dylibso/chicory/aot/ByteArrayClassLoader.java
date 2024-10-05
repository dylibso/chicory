package com.dylibso.chicory.aot;

import org.objectweb.asm.ClassReader;

final class ByteArrayClassLoader extends ClassLoader {

    public ByteArrayClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> loadFromBytes(byte[] bytes) {
        var name = new ClassReader(bytes).getClassName().replace('/', '.');
        return defineClass(name, bytes, 0, bytes.length);
    }
}
