package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;

public class ExtismPlugin {
    Instance instance;
    ExtismKernel kernel;

    public ExtismPlugin(Module module) {
        this(module, new HostFunction[] {});
    }

    public ExtismPlugin(Module module, HostFunction[] hostFunctions) {
        kernel = new ExtismKernel();

        // concat list of host functions
        var kernelFuncs = kernel.toHostFunctions();
        var hostFuncList = new HostFunction[hostFunctions.length + kernelFuncs.length];
        System.arraycopy(kernelFuncs, 0, hostFuncList, 0, kernelFuncs.length);
        System.arraycopy(hostFunctions, 0, hostFuncList, kernelFuncs.length, hostFunctions.length);

        instance = module.instantiate(hostFuncList);
    }

    public byte[] call(String funcName, byte[] input) {
        var func = instance.getExport(funcName);
        kernel.setInput(input);
        var result = func.apply()[0].asInt();
        if (result == 0) {
            return kernel.getOutput();
        } else {
            throw new WASMRuntimeException("Failed");
        }
    }
}
