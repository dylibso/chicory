package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

@FunctionalInterface
public interface Machine {

    interface CallCtx {}
    ;

    long[] call(int funcId, long[] args, CallCtx ctx) throws ChicoryException;
}
