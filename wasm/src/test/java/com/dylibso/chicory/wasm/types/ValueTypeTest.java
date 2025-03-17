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
                    new ValueType(ValueType.ID.RefNull, ValueType.TypeIdxCode.FUNC.code()),
                    new ValueType(ValueType.ID.Ref, ValueType.TypeIdxCode.EXTERN.code()),
                    new ValueType(ValueType.ID.Ref, 16),
                };

        for (var vt : cases) {
            assert vt.equals(ValueType.forId(vt.id())) : "Failed to roundtrip: " + vt;
        }
    }
}
