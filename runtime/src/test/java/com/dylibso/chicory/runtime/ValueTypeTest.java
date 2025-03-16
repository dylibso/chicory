package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import org.junit.jupiter.api.Test;

public class ValueTypeTest {

    @Test
    public void roundtripTest() {
        ValueType[] all =
                new ValueType[] {
                    ValueType.F64,
                    ValueType.F32,
                    ValueType.I64,
                    ValueType.I32,
                    ValueType.V128,
                    ValueType.FuncRef,
                    ValueType.ExternRef
                };

        for (ValueType v : all) {
            assert ValueType.forId(v.id()).equals(v) : "cannot roundtrip: " + v;
        }
    }
}
