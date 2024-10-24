package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

@FunctionalInterface
public interface Machine {

    long[] call(int funcId, long[] args) throws ChicoryException;
}
