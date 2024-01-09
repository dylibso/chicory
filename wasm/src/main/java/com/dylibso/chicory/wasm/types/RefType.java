package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.Map;

public enum RefType {
    FuncRef(0x70),
    ExternRef(0x6f);

    private final long id;

    RefType(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    private static final Map<Long, RefType> byId = new HashMap<>(2);

    static {
        for (RefType e : RefType.values()) byId.put(e.id(), e);
    }

    public static RefType byId(long id) {
        return byId.get(id);
    }
}
