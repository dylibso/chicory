package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.TagType;

public class TagInstance {

    private final TagType tag;
    private final Instance instance;

    public TagInstance(TagType tag, Instance instance) {
        this.tag = tag;
        this.instance = instance;
    }

    public TagType tagType() {
        return tag;
    }

    public Instance instance() {
        return instance;
    }
}
