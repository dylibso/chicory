package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.Map;

public enum MutabilityType {
    Const(0x00),
    Var(0x01);

    private final long id;

    MutabilityType(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    private static final Map<Long, MutabilityType> byId = new HashMap<>(4);

    static {
        for (MutabilityType e : MutabilityType.values()) byId.put(e.id(), e);
    }

    public static MutabilityType byId(long id) {
        return byId.get(id);
    }
}
