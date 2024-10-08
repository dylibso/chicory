package com.dylibso.chicory.aot;

import org.objectweb.asm.ClassReader;

final class AotClassLoader extends ClassLoader {

    public AotClassLoader() {
        super(AotClassLoader.class.getClassLoader());
    }

    public Class<?> loadFromBytes(byte[] bytes) {
        var name = new ClassReader(bytes).getClassName().replace('/', '.');
        return defineClass(name, bytes, 0, bytes.length);
    }
}
