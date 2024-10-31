package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.TagType;

public class ImportTag implements ImportValue {
    private final String module;
    private final String name;
    private final TagType tag;

    public ImportTag(String module, String name, TagType tag) {
        this.module = module;
        this.name = name;
        this.tag = tag;
    }

    @Override
    public String module() {
        return module;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type() {
        return Type.TAG;
    }

    public TagType tag() {
        return tag;
    }
}
