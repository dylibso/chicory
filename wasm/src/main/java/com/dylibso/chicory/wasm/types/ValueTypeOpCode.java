package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ValueTypeOpCode {
    BOT(-1),
    F64(ID.F64),
    F32(ID.F32),
    I64(ID.I64),
    I32(ID.I32),
    V128(ID.V128),
    FuncRef(ID.FuncRef),
    ExternRef(ID.ExternRef),
    RefNull(ID.RefNull, List.of(WasmEncoding.VARSINT32)),
    Ref(ID.Ref, List.of(WasmEncoding.VARSINT32));

    private final int opcode;

    // trick: the enum constructor cannot access its own static fields
    // but can access another class
    private static final class ValueTypeOpCodes {
        private ValueTypeOpCodes() {}

        private static final Map<Integer, ValueTypeOpCode> byOpCode = new HashMap<>();
        private static final Map<Integer, List<WasmEncoding>> signatures = new HashMap<>();
    }

    ValueTypeOpCode(int opcode) {
        this(opcode, List.of());
    }

    ValueTypeOpCode(int opcode, List<WasmEncoding> signature) {
        this.opcode = opcode;
        ValueTypeOpCodes.byOpCode.put(opcode, this);
        ValueTypeOpCodes.signatures.put(opcode, signature);
    }

    public int opcode() {
        return this.opcode;
    }

    public static boolean isValidOpCode(int opcode) {
        return ValueTypeOpCodes.byOpCode.containsKey(opcode);
    }

    public static ValueTypeOpCode byOpCode(int opcode) {
        if (ValueTypeOpCodes.byOpCode.containsKey(opcode)) {
            return ValueTypeOpCodes.byOpCode.get(opcode);
        }

        throw new IllegalArgumentException("invalid ValueTypeOpCode: " + opcode);
    }

    public static List<WasmEncoding> signature(ValueTypeOpCode opcode) {
        if (ValueTypeOpCodes.signatures.containsKey(opcode.opcode())) {
            return ValueTypeOpCodes.signatures.get(opcode.opcode());
        }

        throw new IllegalArgumentException("invalid ValueTypeOpCode: " + opcode);
    }

    /**
     * A separate holder class for ID constants.
     * This is necessary because enum constants are initialized before normal fields, so any reference to an ID constant
     * in the same class would be considered an invalid forward reference.
     */
    static final class ID {
        private ID() {}

        static final int RefNull = 0x63;
        static final int Ref = 0x64;
        static final int ExternRef = 0x6f;
        static final int FuncRef = 0x70;
        static final int V128 = 0x7b;
        static final int F64 = 0x7c;
        static final int F32 = 0x7d;
        static final int I64 = 0x7e;
        static final int I32 = 0x7f;
    }
}
