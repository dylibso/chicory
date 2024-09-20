package com.dylibso.chicory.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import net.lingala.zip4j.ZipFile;
import org.apache.maven.plugin.logging.Log;

public class TestSuiteDownloader {

    private final Log log;

    TestSuiteDownloader(Log log) {
        this.log = log;
    }

    public void downloadTestsuite(
            String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder)
            throws IOException {
        if (testSuiteFolder.exists()
                && testSuiteFolder.list((dir, name) -> name.endsWith(".wast")).length == 0) {
            log.warn("Testsuite folder exists but looks corrupted, replacing.");
            try (Stream<Path> files =
                    Files.walk(testSuiteFolder.toPath()).sorted(Comparator.reverseOrder())) {
                files.map(Path::toFile).forEach(File::delete);
            }
        } else {
            log.debug("Testsuite detected, using the cached version.");
        }
        if (!testSuiteFolder.exists()) {
            log.warn("Cloning the testsuite at ref: " + testSuiteRepoRef);
            // target URL:
            // https://github.com/WebAssembly/testsuite/archive/c2a67a575ddc815ff2212f68301d333e5e30a923.tar.gz
            // https://docs.github.com/en/repositories/working-with-files/using-files/downloading-source-code-archives#source-code-archive-urls
            String zipName = testSuiteRepoRef + ".zip";
            URL url = new URL(testSuiteRepo + "/archive/" + zipName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            try (InputStream in = con.getInputStream()) {
                var zipFile = Paths.get("target/" + zipName);
                Files.write(zipFile, in.readAllBytes());
                try (var zip = new ZipFile(zipFile.toFile())) {
                    zip.renameFile("testsuite-" + testSuiteRepoRef + "/", "testsuite");
                    zip.extractAll(".");
                }
            } finally {
                con.disconnect();
            }
            log.warn("Cloned the testsuite at ref: " + testSuiteRepoRef);
        }
    }
}
