package io.github.andreatp.wasmdemo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StringContent {

    private String content;

    @JsonCreator
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
