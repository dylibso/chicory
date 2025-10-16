package com.dylibso.chicory.compiler;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public interface Cache {

    /**
     * The temp dir is deleted once closed.
     */
    interface TempDir extends Closeable {
        Path path();
    }

    /**
     * Return the directory for the given key if it exists else null.
     *
     * @param key    "algo:digest"
     */
    Path get(String key) throws IOException;

    /**
     * Create a unique temporary directory suitable for writing the computation output.
     * The directory will be on the same filesystem as the final target so that ATOMIC_MOVE works.
     */
    TempDir createTempDir() throws IOException;

    /**
     * Atomically publish a completed temp directory into the cache location for the key.
     * If another thread/process already published for this key then this is a no-op.
     *
     * @param key    "algo:digest"
     * @param tmpDir a directory containing fully written results (created via createTempDir())
     */
    void put(String key, TempDir tmpDir) throws IOException;
}
