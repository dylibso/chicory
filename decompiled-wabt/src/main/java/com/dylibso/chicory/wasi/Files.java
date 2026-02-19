package com.dylibso.chicory.wasi;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.getFileAttributeView;
import static java.nio.file.Files.isSymbolicLink;
import static java.nio.file.Files.walkFileTree;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

public final class Files {
    private Files() {}

    /**
     * Copy directory recursively, including POSIX file permissions.
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        walkFileTree(
                source,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        if (isSymbolicLink(dir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        Path directory = target.resolve(source.relativize(dir).toString());

                        if (!directory.toString().equals("/")) {
                            FileAttribute<?>[] attributes = new FileAttribute[0];
                            var attributeView =
                                    getFileAttributeView(dir, PosixFileAttributeView.class);
                            if (attributeView != null) {
                                var permissions = attributeView.readAttributes().permissions();
                                var attribute = PosixFilePermissions.asFileAttribute(permissions);
                                attributes = new FileAttribute[] {attribute};
                            }

                            createDirectory(directory, attributes);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        String relative = source.relativize(file).toString().replace("\\", "/");
                        Path path = target.resolve(relative);
                        copy(file, path, StandardCopyOption.COPY_ATTRIBUTES);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
