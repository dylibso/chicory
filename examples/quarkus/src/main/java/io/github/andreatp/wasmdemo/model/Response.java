package io.github.andreatp.wasmdemo.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Response {

    private String message;

    public Response() {
        this.message = null;
    }

    public Response(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
