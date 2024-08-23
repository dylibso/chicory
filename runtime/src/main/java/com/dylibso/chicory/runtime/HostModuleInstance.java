package com.dylibso.chicory.runtime;

import java.util.HashMap;
import java.util.Map;

public interface HostModuleInstance extends AutoCloseable {

    class Builder {
        private HostModule hostModule;
        private Runnable onClose;
        private Map<String, WasmFunctionHandle> bindings;

        private Builder(HostModule hostModule) {
            this.hostModule = hostModule;
            this.bindings = new HashMap<>(hostModule.signatures().length);
        }

        public Builder onClose(Runnable onClose) {
            this.onClose = onClose;
            return this;
        }

        public Builder bind(String fname, WasmFunctionHandle handle) {
            this.bindings.put(fname, handle);
            return this;
        }

        public HostModuleInstance build() {
            var onClose = this.onClose;
            if (onClose == null) {
                onClose = () -> {}; // NOP
            }

            FunctionSignature[] signatures = this.hostModule.signatures();
            if (this.bindings.size() != signatures.length) {
                throw new IllegalArgumentException(
                        "Must supply a binding for each function signature");
            }

            HostFunction[] hostFunctions = new HostFunction[signatures.length];
            for (int i = 0; i < signatures.length; i++) {
                FunctionSignature signature = signatures[i];
                String name = signature.name();
                WasmFunctionHandle handle = this.bindings.get(name);
                if (handle == null) {
                    throw new IllegalArgumentException(
                            String.format("Cannot find a binding for function signature %s", name));
                }
                hostFunctions[i] =
                        new HostFunction(
                                handle,
                                signature.moduleName(),
                                name,
                                signature.paramTypes(),
                                signature.returnTypes());
            }

            return new HostFunctionBundle(hostModule, hostFunctions, onClose);
        }
    }

    static Builder builder(HostModule hostModule) {
        return new Builder(hostModule);
    }

    HostModule module();

    HostFunction[] hostFunctions();

    void close();
}
