package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Value;
import java.util.List;

public interface Machine {

    Value[] call(int funcId, Value[] args) throws ChicoryException;

    List<StackFrame> getStackTrace();
}
