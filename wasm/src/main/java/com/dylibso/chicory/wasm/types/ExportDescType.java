package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.Map;

public enum ExportDescType {
    FuncIdx(0x00),
    TableIdx(0x01),
    MemIdx(0x02),
    GlobalIdx(0x03);

    private final long id;
    ExportDescType(long id) { this.id = id; }

    public long id() { return id; }

    private static final Map<Long, ExportDescType> byId = new HashMap<Long, ExportDescType>(4);

    static {
        for (ExportDescType e : ExportDescType.values())
            byId.put(e.id(), e);
    }
    public static ExportDescType byId(long id) { return byId.get(id); }
}
