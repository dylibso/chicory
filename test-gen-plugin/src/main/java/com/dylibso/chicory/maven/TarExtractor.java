package com.dylibso.chicory.maven;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class TarExtractor {

    private final InputStream tarStream;

    private final Path destination;

    public TarExtractor(InputStream in, Path destination) {
        this.tarStream = in;
        this.destination = destination;
    }

    public void untar() throws IOException {

        Files.createDirectories(destination);

        try (BufferedInputStream inputStream = new BufferedInputStream(this.tarStream);
                TarArchiveInputStream tar =
                        new TarArchiveInputStream(new GzipCompressorInputStream(inputStream))) {
            ArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                Path extractTo = this.destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(extractTo);
                } else {
                    Files.copy(tar, extractTo);
                }
            }
        }
    }
}
