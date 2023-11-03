package com.dylibso.chicory.wasm.types;

public class GlobalType implements Type {
    private ValueType returns;
    private MutabilityType mutable;

    public GlobalType(ValueType returns, MutabilityType mutable) {
        this.returns = returns;
        this.mutable = mutable;
    }

    @Override
    public ValueType[] getParams() {
        return new ValueType[0];
    }

    @Override
    public ValueType[] getReturns() {
        return new ValueType[] {returns};
    }
}
