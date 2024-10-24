package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Machine;

public class InterpreterMachineFactory {

    public static InterpreterMachine create(Instance instance) {
        return new InterpreterMachine(instance);
    }

}
