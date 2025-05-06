package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CompilerResult {

    private final Function<Instance, Machine> machineFactory;
    private final Map<String, byte[]> classBytes;
    private final Set<Integer> interpretedFunctions;

    public CompilerResult(
            Function<Instance, Machine> machineFactory,
            Map<String, byte[]> classBytes,
            Set<Integer> interpretedFunctions) {
        this.machineFactory = machineFactory;
        this.classBytes = classBytes;
        this.interpretedFunctions = interpretedFunctions;
    }

    public Function<Instance, Machine> machineFactory() {
        return machineFactory;
    }

    public Map<String, byte[]> classBytes() {
        return classBytes;
    }

    public Set<Integer> interpretedFunctions() {
        return interpretedFunctions;
    }
}
