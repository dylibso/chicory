package com.dylibso.chicory.wasm.types;

import org.junit.jupiter.api.Test;

public class ValTypeTest {
    @Test
    public void roundtrip() {
        var cases =
                new ValType[] {
                    ValType.F64,
                    ValType.F32,
                    ValType.I64,
                    ValType.I32,
                    ValType.V128,
                    ValType.FuncRef,
                    ValType.ExternRef,
                    new ValType(ValType.ID.RefNull, ValType.TypeIdxCode.FUNC.code()),
                    new ValType(ValType.ID.Ref, ValType.TypeIdxCode.EXTERN.code()),
                    new ValType(ValType.ID.Ref, 16),
                };

        for (var vt : cases) {
            assert vt.equals(ValType.forId(vt.id())) : "Failed to roundtrip: " + vt;
        }
    }
}
