package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class TypeSection extends Section {
    private final ArrayList<FunctionType> types;

    public TypeSection(FunctionType[] types) {
        super(SectionId.TYPE);
        this.types = new ArrayList<>(List.of(types));
    }

    public FunctionType[] types() {
        return types.toArray(FunctionType[]::new);
    }

    public int typeCount() {
        return types.size();
    }

    public FunctionType getType(int idx) {
        return types.get(idx);
    }
}
