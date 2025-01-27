package com.dylibso.chicory.testgen;

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

public class TestSuiteDownloader {

    TestSuiteDownloader() {}

    public void downloadTestsuite(
            String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder)
            throws IOException {
        if (testSuiteFolder.exists()
                && testSuiteFolder.list((dir, name) -> name.endsWith(".wast")).length == 0) {
            try (Stream<Path> files =
                    Files.walk(testSuiteFolder.toPath()).sorted(Comparator.reverseOrder())) {
                files.map(Path::toFile).forEach(File::delete);
            }
        }

        if (!testSuiteFolder.exists()) {
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
        }
    }
}
