package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

public interface Machine {

    long[] call(int funcId, long[] args) throws ChicoryException;
}
