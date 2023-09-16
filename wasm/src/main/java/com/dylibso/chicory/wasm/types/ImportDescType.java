package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.Map;

public enum ImportDescType {
    FuncIdx(0x00),
    TableIdx(0x01),
    MemIdx(0x02),
    GlobalIdx(0x03);

    private final long id;
    ImportDescType(long id) { this.id = id; }

    public long id() { return id; }

    private static final Map<Long, ImportDescType> byId = new HashMap<Long, ImportDescType>(4);

    static {
        for (ImportDescType e : ImportDescType.values())
            byId.put(e.id(), e);
    }
    public static ImportDescType byId(long id) { return byId.get(id); }
}
