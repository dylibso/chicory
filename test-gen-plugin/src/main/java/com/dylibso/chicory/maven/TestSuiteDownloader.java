package com.dylibso.chicory.maven;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static java.util.Collections.singleton;

public class TestSuiteDownloader {

    private final Log log;

    TestSuiteDownloader(Log log) {
        this.log = log;
    }

    public void downloadTestsuite(String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder) throws Exception {
        if (testSuiteFolder.exists() && testSuiteFolder.list((dir, name) -> name.endsWith(".wast")).length == 0) {
            log.warn("Testsuite folder exists but looks corrupted, replacing.");
            Files.walk(testSuiteFolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } else {
            log.debug("Testsuite detected, using the cached version.");
        }
        if (!testSuiteFolder.exists()) {
            log.warn("Cloning the testsuite at ref: " + testSuiteRepoRef);
            Git.cloneRepository()
                    .setURI(testSuiteRepo)
                    .setDirectory(testSuiteFolder)
                    .setDepth(1)
                    .setBranchesToClone(singleton("refs/heads/" + testSuiteRepoRef))
                    .setBranch("refs/heads/" + testSuiteRepoRef)
                    .call();
        }
    }

}
