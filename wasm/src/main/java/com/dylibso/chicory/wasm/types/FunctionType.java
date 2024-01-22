package com.dylibso.chicory.wasm.types;

import java.util.Arrays;

public class FunctionType {
    private ValueType[] params;
    private ValueType[] returns;

    public FunctionType(ValueType[] params, ValueType[] returns) {
        this.params = params;
        this.returns = returns;
    }

    public ValueType[] getParams() {
        return params;
    }

    public ValueType[] getReturns() {
        return returns;
    }

    public boolean paramsMatch(FunctionType other) {
        return Arrays.equals(params, other.params);
    }

    public boolean returnsMatch(FunctionType other) {
        return Arrays.equals(returns, other.returns);
    }

    public boolean typesMatch(FunctionType other) {
        return paramsMatch(other) && returnsMatch(other);
    }

    public String toString() {
        var builder = new StringBuilder();
        builder.append('(');
        var nParams = this.params.length;
        for (var i = 0; i < nParams; i++) {
            builder.append(this.params[i].toString());
            if (i < nParams - 1) {
                builder.append(',');
            }
        }
        builder.append(") -> ");
        var nReturns = this.returns.length;
        if (nReturns == 0) {
            builder.append("nil");
        } else {
            for (var i = 0; i < nParams; i++) {
                builder.append(this.params[i].toString());
                if (i < nReturns - 1) {
                    builder.append(',');
                }
            }
        }
        return builder.toString();
    }
}
