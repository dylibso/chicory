package com.dylibso.chicory.compiler;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Disk-backed sharded directory cache.
 */
public class DirectoryCache {

    private static final String ALLOWED_DIGEST_CHARS = "^[A-Za-z0-9+_\\-/]+=$";
    private static final Pattern ALLOWED_DIGEST_CHARS_REGEX = Pattern.compile(ALLOWED_DIGEST_CHARS);

    private final Path baseDir;
    private final Path tmpRoot;
    private final ReentrantLock[] stripes;

    /**
     * Construct with a sensible default (64 stripes).
     */
    public DirectoryCache(Path baseDir) {
        this(baseDir, 64);
    }

    /**
     * @param baseDir     the root cache directory (e.g., Paths.get("cache"))
     * @param stripeCount number of lock stripes (power of two recommended; e.g., 64 or 128)
     */
    public DirectoryCache(Path baseDir, int stripeCount) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
        if (stripeCount <= 0) {
            throw new IllegalArgumentException("stripeCount must be > 0");
        }
        this.tmpRoot = baseDir.resolve(".tmp");
        this.stripes = new ReentrantLock[stripeCount];
        for (int i = 0; i < stripeCount; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    /**
     * Return the directory for the given key if it exists (and is a directory), else null.
     * Does not create anything.
     */
    public Path get(String key) {
        Path target = toDirectoryPath(key);
        return Files.isDirectory(target) ? target : null;
    }

    private static final class TempDirImpl implements Cache.TempDir {

        private final Path path;

        private TempDirImpl(Path path) {
            this.path = path;
        }

        @Override
        public Path path() {
            return path;
        }

        @Override
        public void close() throws IOException {
            if (!Files.exists(path)) {
                return;
            }
            Files.walkFileTree(
                    path,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                                throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        }
    }

    /**
     * Create a unique temporary directory under baseDir/.tmp, suitable for writing the computation output.
     * The directory will be on the same filesystem as the final target so that ATOMIC_MOVE works.
     */
    public Cache.TempDir createTempDir() throws IOException {
        Files.createDirectories(tmpRoot);
        return new TempDirImpl(Files.createTempDirectory(tmpRoot, "d-"));
    }

    /**
     * Atomically publish a completed temp directory into the cache location for the key.
     * If another thread/process already published for this key, the temp directory is deleted and the
     * existing path is returned.
     *
     * @param key    "algo:digest"
     * @param tmpDir a directory containing fully written results (created via createTempDir())
     * @return the final cache directory path
     */
    public Path put(String key, Cache.TempDir tmpDir) throws IOException {
        Objects.requireNonNull(tmpDir, "tmpDir");
        if (!Files.isDirectory(tmpDir.path())) {
            throw new IllegalArgumentException("tmpDir must be an existing directory: " + tmpDir);
        }

        Path finalPath = toDirectoryPath(key);
        Path parent = finalPath.getParent(); // .../<algo>/<shard>
        if (parent == null) {
            throw new IOException("Cannot determine parent for " + finalPath);
        }

        ReentrantLock lock = stripeFor(key);
        lock.lock();
        try {
            // Ensure parent exists before atomic move.
            Files.createDirectories(parent);

            // If already present, return existing.
            if (Files.isDirectory(finalPath)) {
                return finalPath;
            }

            // Try atomic move; if target appeared between our checks, handle gracefully.
            try {
                Files.move(tmpDir.path(), finalPath, ATOMIC_MOVE);
            } catch (FileAlreadyExistsException ignore) {
                // SUPPRESS CHECKSTYLE EmptyCatchBlock
            }
            return finalPath;
        } finally {
            lock.unlock();
        }
    }

    // ---------- internals ----------
    private ReentrantLock stripeFor(String key) {
        int h = smear(key.hashCode());
        int idx = (h & 0x7fffffff) % stripes.length;
        return stripes[idx];
    }

    private static int smear(int h) {
        h ^= (h >>> 16);
        return h;
    }

    /**
     * baseDir / <algo> / <first2> / <remainder>
     * Validates algo and digest.
     */
    private Path toDirectoryPath(String key) {
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
        return remainder.isEmpty() ? shardDir : shardDir.resolve(remainder);
    }
}
