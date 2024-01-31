package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.Map;

public enum ValueType {
    F64(0x7c),
    F32(0x7d),
    I64(0x7e),
    I32(0x7f),
    V128(0x7b),
    FuncRef(0x70),
    ExternRef(0x6f);

    private final long id;

    ValueType(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    private static final Map<Long, ValueType> byId = new HashMap<>(4);

    static {
        for (ValueType e : ValueType.values()) byId.put(e.id(), e);
    }

    public static ValueType byId(long id) {
        return byId.get(id);
    }
}
