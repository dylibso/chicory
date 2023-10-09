package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.Map;

public enum ElementType {
    FuncRef(0x70);

    private final long id;

    ElementType(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    private static final Map<Long, ElementType> byId = new HashMap<>(1);

    static {
        for (ElementType e : ElementType.values()) byId.put(e.id(), e);
    }

    public static ElementType byId(long id) {
        return byId.get(id);
    }
}
