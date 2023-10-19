package io.github.andreatp.wasmdemo.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WasmBinary {

    private String content;

    public WasmBinary() {
        this.content = null;
    }

    public WasmBinary(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
