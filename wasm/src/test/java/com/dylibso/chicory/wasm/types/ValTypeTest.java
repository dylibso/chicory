package com.dylibso.chicory.wasm.types;

import org.junit.jupiter.api.Test;

public class ValTypeTest {
    private FunctionType context(int idx) {
        return FunctionType.returning(ValType.FuncRef);
    }

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
                    new ValType.Builder(ValType.ID.RefNull, ValType.TypeIdxCode.FUNC.code())
                            .build(this::context),
                    new ValType.Builder(ValType.ID.Ref, ValType.TypeIdxCode.EXTERN.code())
                            .build(this::context),
                    new ValType.Builder(ValType.ID.Ref, 16).build(this::context),
                };

        for (var vt : cases) {
            long id = vt.id();
            ValType roundTrip = new ValType.Builder(id).build(this::context);
            assert vt.equals(roundTrip) : "Failed to roundtrip: " + vt;
        }
    }
}
