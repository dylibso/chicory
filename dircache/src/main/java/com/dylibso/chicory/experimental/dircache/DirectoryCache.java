package com.dylibso.chicory.experimental.dircache;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import com.dylibso.chicory.compiler.Cache;
import com.dylibso.chicory.experimental.dircache.internal.PathUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Disk-backed sharded file cache.
 */
public class DirectoryCache implements Cache {

    private static final String ALLOWED_DIGEST_CHARS = "^[A-Za-z0-9+_\\-/]+=$";
    private static final Pattern ALLOWED_DIGEST_CHARS_REGEX = Pattern.compile(ALLOWED_DIGEST_CHARS);

    private final Path baseDir;
    private final Path tmpRoot;

    /**
     * Construct a DirectoryCache at the given Path
     *
     * @param baseDir     the root cache directory (e.g., Paths.get("cache"))
     */
    public DirectoryCache(Path baseDir) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
        this.tmpRoot = baseDir.resolve(".tmp");
    }

    /**
     * Return the cached data for the given key if it exists, else null.
     * Does not create anything.
     */
    @Override
    public byte[] get(String key) throws IOException {
        try {
            Path target = toFilePath(key);
            return Files.isRegularFile(target) ? Files.readAllBytes(target) : null;
        } catch (IOException e) {
            // if we can't read it, then treat it like it not being in the cache.
            return null;
        }
    }

    /**
     * Atomically publish data into the cache location for the key.
     * If another thread/process already published for this key then this is a no-op.
     *
     * @param key    "algo:digest"
     * @param data   the data to cache
     */
    @Override
    public void putIfAbsent(String key, byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");

        Path finalPath = toFilePath(key);
        Path parent = finalPath.getParent(); // .../<algo>/<shard>
        if (parent == null) {
            throw new IOException("Cannot determine parent for " + finalPath);
        }

        Files.createDirectories(this.tmpRoot);
        var tmpFile = Files.createTempFile(tmpRoot, "f-", ".tmp");
        try {
            // Write the data to temp file
            Files.write(tmpFile, data);

            // Ensure parent exists before atomic move.
            Files.createDirectories(parent);

            // Move it
            Files.move(tmpFile, finalPath, ATOMIC_MOVE);
        } catch (FileSystemException e) {
            // did another process beat us to creating the cache entry?
            if (Files.isRegularFile(finalPath)) {
                return;
            }
            throw e;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            PathUtils.recursiveDelete(tmpFile);
        }
    }

    // ---------- internals ----------

    /*
     * baseDir / algo / first 2 chars of digest / remainder of digest.jar
     * Validates the digest.
     */
    private Path toFilePath(String key) {
        Objects.requireNonNull(key, "key");
        int colon = key.indexOf(':');
        if (colon <= 0 || colon == key.length() - 1) {
            throw new IllegalArgumentException("Key must be in form '<algo>:<hex>'");
        }
        String algo = key.substring(0, colon).toLowerCase(Locale.ROOT);
        String digest = key.substring(colon + 1);

        if (!ALLOWED_DIGEST_CHARS_REGEX.matcher(digest).matches()) {
            throw new IllegalArgumentException("Digest must match " + ALLOWED_DIGEST_CHARS);
        }

        // digest will be base64 chars, convert so they are safe to use for file names
        digest = digest.replace('+', '-').replace('/', '_').replace("=", "");

        if (digest.length() < 2) {
            throw new IllegalArgumentException(
                    "Digest must be at least 2 hex chars (got " + digest.length() + ")");
        }

        String lower = digest.toLowerCase(Locale.ROOT);
        String shard = lower.substring(0, 2);
        String remainder = lower.substring(2);

        Path algoDir = baseDir.resolve(algo);
        Path shardDir = algoDir.resolve(shard);
        Path basePath = remainder.isEmpty() ? shardDir : shardDir.resolve(remainder);
        return basePath.resolveSibling(basePath.getFileName() + ".jar");
    }
}
