package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.Map;

public interface Machine {

    long[] call(int funcId, long[] args) throws ChicoryException;

    Map<OpCode, OpImpl> additionalOpCodes();

    @FunctionalInterface
    interface Operands {
        long get(int index);
    }

    @FunctionalInterface
    interface OpImpl {
        void invoke(MStack stack, Instance instance, Operands operands);
    }

    static int readMemPtr(MStack stack, Operands operands) {
        int offset = (int) stack.pop();
        if (operands.get(1) < 0 || operands.get(1) >= Integer.MAX_VALUE || offset < 0) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
        return (int) (operands.get(1) + offset);
    }
}
