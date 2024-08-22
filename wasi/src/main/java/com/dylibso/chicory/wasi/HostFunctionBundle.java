package com.dylibso.chicory.wasi;

import com.dylibso.chicory.runtime.HostFunction;

public class HostFunctionBundle implements HostModuleInstance {
    private final HostModule module;
    private final HostFunction[] hostFunctions;
    private final Runnable onClose;

    public HostFunctionBundle(HostModule module, HostFunction[] hostFunctions, Runnable onClose) {
        this.module = module;
        this.hostFunctions = hostFunctions;
        this.onClose = onClose;
    }

    @Override
    public HostModule module() {
        return module;
    }

    @Override
    public HostFunction[] hostFunctions() {
        return hostFunctions;
    }

    @Override
    public void close() {
        onClose.run();
    }
}
