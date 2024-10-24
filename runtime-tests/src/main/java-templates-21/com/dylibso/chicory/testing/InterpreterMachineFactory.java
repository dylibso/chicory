package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.simd.SimdInterpreterMachine;

public class InterpreterMachineFactory {

    public static Machine create(Instance instance) {
        return new SimdInterpreterMachine(instance);
    }

}
