package com.dylibso.chicory.wasm.types;

import org.junit.jupiter.api.Test;

public class ValueTypeTest {
    @Test
    public void roundtrip() {
        var cases =
                new ValueType[] {
                    ValueType.F64,
                    ValueType.F32,
                    ValueType.I64,
                    ValueType.I32,
                    ValueType.V128,
                    ValueType.FuncRef,
                    ValueType.ExternRef,
                    new ValueType(ValueTypeOpCode.RefNull, ValueType.OperandCode.FUNC.code()),
                    new ValueType(ValueTypeOpCode.Ref, ValueType.OperandCode.EXTERN.code()),
                    new ValueType(ValueTypeOpCode.Ref, 16),
                };

        for (var vt : cases) {
            assert vt.equals(ValueType.forId(vt.id())) : "Failed to roundtrip: " + vt;
        }
    }
}
