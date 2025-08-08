package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.TagType;

public class TagInstance {

    private final TagType tag;
    private FunctionType type;

    public TagInstance(TagType tag) {
        this.tag = tag;
    }

    public TagInstance(TagType tag, FunctionType type) {
        this.tag = tag;
        this.type = type;
    }

    public TagType tagType() {
        return tag;
    }

    public void setType(FunctionType type) {
        this.type = type;
    }

    public FunctionType type() {
        return type;
    }

    public TagInstance copy() {
        return new TagInstance(this.tag, this.type);
    }
}
