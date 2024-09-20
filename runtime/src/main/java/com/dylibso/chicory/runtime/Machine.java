package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import java.util.Map;

public interface Machine {

    Value[] call(int funcId, Value[] args) throws ChicoryException;

    Map<OpCode, OpImpl> additionalOpCodes();

    @FunctionalInterface
    public interface OpImpl {
        void invoke(MStack stack, Instance instance, InterpreterMachine.Operands operands);
    }
}
