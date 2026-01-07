package com.dylibso.chicory.experimental.dircache.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class PathUtils {

    private PathUtils() {}

    public static void recursiveDelete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(
                path,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            // ignore
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        try {
                            Files.delete(dir);
                        } catch (IOException e) {
                            // ignore
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
