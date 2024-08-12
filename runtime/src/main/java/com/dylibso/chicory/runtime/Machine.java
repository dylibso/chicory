package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Value;

public interface Machine {

    Value[] call(int funcId, Value[] args) throws ChicoryException;
}
