package com.dylibso.chicory.maven;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.util.SystemReader;

class WasiTestSuiteDownloader {
    private final Log log;

    public WasiTestSuiteDownloader(Log log) {
        this.log = log;
    }

    public void downloadTestsuite(
            String testSuiteRepo, String testSuiteRepoRef, File testSuiteFolder)
            throws ConfigInvalidException, GitAPIException, IOException {
        if (testSuiteFolder.exists()) {
            log.debug("Testsuite detected, using the cached version.");
            return;
        }

        log.warn("Cloning the testsuite at ref: " + testSuiteRepoRef);
        SystemReader.getInstance().getUserConfig().clear();
        Git.cloneRepository()
                .setURI(testSuiteRepo)
                .setDirectory(testSuiteFolder)
                .setBranch(testSuiteRepoRef)
                .setDepth(1)
                .call()
                .close();
        log.warn("Cloned the testsuite at ref: " + testSuiteRepoRef);
    }
}
