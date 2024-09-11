package com.dylibso.chicory.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.lingala.zip4j.ZipFile;
import org.apache.maven.plugin.logging.Log;

class WasiTestSuiteDownloader {
    private final Log log;

    public WasiTestSuiteDownloader(Log log) {
        this.log = log;
    }

    public void downloadTestsuite(
            String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder)
            throws IOException {
        if (testSuiteFolder.exists()) {
            log.debug("Testsuite detected, using the cached version.");
            return;
        }

        log.warn("Cloning the testsuite at ref: " + testSuiteRepoRef);
        String zipName = testSuiteRepoRef + ".zip";
        URL url = new URL(testSuiteRepo + "/archive/refs/heads/" + zipName);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (InputStream in = con.getInputStream()) {
            var zipFile = Paths.get("target/" + zipName.replace('/', '-'));
            Files.write(zipFile, in.readAllBytes());
            try (var zip = new ZipFile(zipFile.toFile())) {
                zip.renameFile(
                        "wasi-testsuite-" + testSuiteRepoRef.replace('/', '-') + "/",
                        "wasi-testsuite");
                zip.extractAll(".");
            }
        } finally {
            con.disconnect();
        }
        log.warn("Cloned the testsuite at ref: " + testSuiteRepoRef);
    }
}
