package com.dylibso.chicory.compiler.internal;

import org.objectweb.asm.ClassReader;

final class WasmClassLoader extends ClassLoader {

    public WasmClassLoader() {
        super(WasmClassLoader.class.getClassLoader());
    }

    public Class<?> loadFromBytes(byte[] bytes) {
        var name = new ClassReader(bytes).getClassName().replace('/', '.');
        return defineClass(name, bytes, 0, bytes.length);
    }
}
