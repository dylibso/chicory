package com.dylibso.chicory.experimental.aot;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import java.util.Map;
import java.util.function.Function;

public final class CompilerResult {

    private final Function<Instance, Machine> machineFactory;
    private final Map<String, byte[]> classBytes;

    public CompilerResult(
            Function<Instance, Machine> machineFactory, Map<String, byte[]> classBytes) {
        this.machineFactory = machineFactory;
        this.classBytes = classBytes;
    }

    public Function<Instance, Machine> machineFactory() {
        return machineFactory;
    }

    public Map<String, byte[]> classBytes() {
        return classBytes;
    }
}
