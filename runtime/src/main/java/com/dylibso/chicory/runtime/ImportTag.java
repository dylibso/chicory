package com.dylibso.chicory.runtime;

public class ImportTag implements ImportValue {
    private final String module;
    private final String name;
    private final TagInstance tag;

    public ImportTag(String module, String name, TagInstance tag) {
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

    public TagInstance tag() {
        return tag;
    }
}
