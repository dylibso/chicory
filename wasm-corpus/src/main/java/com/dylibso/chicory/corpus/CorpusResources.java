package com.dylibso.chicory.corpus;

import java.io.InputStream;

public final class CorpusResources {

    private CorpusResources() {}

    public static InputStream getResource(String name) {
        InputStream stream = CorpusResources.class.getResourceAsStream("/" + name);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: /" + name);
        }
        return stream;
    }
}
