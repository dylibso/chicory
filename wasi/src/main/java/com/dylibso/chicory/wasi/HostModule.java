package com.dylibso.chicory.wasi;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.List;

public interface HostModule {

    public static class Builder {
        private final String moduleName;
        private final List<FunctionSignature> signatures;

        public Builder(String moduleName) {
            this.moduleName = moduleName;
            this.signatures = new ArrayList<>();
        }

        public Builder withFunctionSignature(
                String name, List<ValueType> paramTypes, List<ValueType> returnTypes) {
            signatures.add(new FunctionSignature(this.moduleName, name, paramTypes, returnTypes));
            return this;
        }

        public HostModule build() {
            return new FunctionSignatureBundle(
                    moduleName, signatures.toArray(new FunctionSignature[signatures.size()]));
        }
    }

    static Builder builder(String moduleName) {
        return new HostModule.Builder(moduleName);
    }

    FunctionSignature[] signatures();

    String name();
}
