package com.dylibso.chicory.compiler.internal;

public interface ClassFileResolver {
    byte[] getBytecode(Class<?> className);
}
