package io.github.andreatp.wasmdemo.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StringContent {

    private String content;

    public StringContent() {
        this.content = null;
    }

    public StringContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
