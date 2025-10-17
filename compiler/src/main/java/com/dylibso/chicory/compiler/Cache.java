package com.dylibso.chicory.compiler;

import java.io.IOException;

public interface Cache {

    /**
     * Return the cached data for the given key if it exists else null.
     *
     * @param key    "algo:digest"
     */
    byte[] get(String key) throws IOException;

    /**
     * Atomically publish data into the cache location for the key.
     * If another thread/process already published for this key then this is a no-op.
     *
     * @param key    "algo:digest"
     * @param data   the data to cache
     */
    void put(String key, byte[] data) throws IOException;
}
