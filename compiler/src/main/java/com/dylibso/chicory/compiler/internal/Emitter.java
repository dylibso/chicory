package com.dylibso.chicory.compiler.internal;

@FunctionalInterface
interface Emitter {
    void emit(Context context);
}
