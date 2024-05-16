package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.getFileAttributeView;
import static java.nio.file.Files.isSymbolicLink;
import static java.nio.file.Files.walkFileTree;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

// TODO: refactor the little customizations done here in the wasi module
public final class Files {
    private Files() {}

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

                            try {
                                createDirectory(directory, attributes);
                            } catch (FileAlreadyExistsException e) {
                                // already exists ignore
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        String relative = source.relativize(file).toString().replace("\\", "/");
                        Path path = target.resolve(relative);
                        copy(file, path, StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
