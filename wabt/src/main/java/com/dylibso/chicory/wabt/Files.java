package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.walkFileTree;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

// TODO: refactor the little customizations done here in the wasi module
public final class Files {
    private Files() {}

    public static void copyDirectory(Path source, Path target) throws IOException {
        walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = source.relativize(file).toString().replace("\\", "/");
                Path path = target.resolve(relative);
                copy(file, path, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
