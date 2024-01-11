package com.dylibso.chicory.maven;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.util.SystemReader;

public class TestSuiteDownloader {

    private final Log log;

    TestSuiteDownloader(Log log) {
        this.log = log;
    }

    public void downloadTestsuite(
            String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder) throws Exception {
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
            SystemReader.getInstance().getUserConfig().clear();
            try (Git git =
                    Git.cloneRepository()
                            .setURI(testSuiteRepo)
                            .setDirectory(testSuiteFolder)
                            .call()) {
                git.checkout().setName(testSuiteRepoRef).call();
                log.warn("Cloned the testsuite at ref: " + testSuiteRepoRef);
            }
        }
    }
}
