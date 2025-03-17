package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ValueTypeOpCode {
    UNKNOWN(-1),
    F64(ID.F64),
    F32(ID.F32),
    I64(ID.I64),
    I32(ID.I32),
    V128(ID.V128),
    FuncRef(ID.FuncRef),
    ExternRef(ID.ExternRef);

    private final int id;

    // trick: the enum constructor cannot access its own static fields
    // but can access another class
    private static final class ValueTypeOpCodes {
        private ValueTypeOpCodes() {}

        private static final Map<Integer, List<WasmEncoding>> byOpCode = new HashMap<>();
    }

    ValueTypeOpCode(int id) {
        this(id, List.of());
    }

    ValueTypeOpCode(int id, List<WasmEncoding> signature) {
        this.id = id;
        ValueTypeOpCodes.byOpCode.put(id, signature);
    }

    public int id() {
        return this.id;
    }

    /**
     * A separate holder class for ID constants.
     * This is necessary because enum constants are initialized before normal fields, so any reference to an ID constant
     * in the same class would be considered an invalid forward reference.
     */
    static final class ID {
        private ID() {}

        static final int ExternRef = 0x6f;
        static final int FuncRef = 0x70;
        static final int V128 = 0x7b;
        static final int F64 = 0x7c;
        static final int F32 = 0x7d;
        static final int I64 = 0x7e;
        static final int I32 = 0x7f;
    }
}
