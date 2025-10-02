package com.dylibso.chicory.compiler;

import com.dylibso.chicory.compiler.internal.CompilerResult;

public interface Cache {

    // TODO: this way CompilerResult becomes public API
    void put(int key, CompilerResult content);

    CompilerResult get(int key);
}
