package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.Constants.SPEC_JSON;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.apache.maven.plugin.logging.Log;

public class Wast2JsonWrapper {

    private static final String WAST2JSON = "wast2json";

    private final Log log;
    private final File wabtDownloadTargetFolder;
    private final String wabtReleasesURL;
    private final String wabtVersion;
    private final String osName;
    private final File compiledWastTargetFolder;

    private String wast2JsonCmd = WAST2JSON;

    public Wast2JsonWrapper(
            Log log,
            File wabtDownloadTargetFolder,
            String wabtReleasesURL,
            String wabtVersion,
            String osName,
            File compiledWastTargetFolder) {
        this.log = log;
        this.wabtDownloadTargetFolder = wabtDownloadTargetFolder;
        this.wabtReleasesURL = wabtReleasesURL;
        this.wabtVersion = wabtVersion;
        this.osName = osName;
        this.compiledWastTargetFolder = compiledWastTargetFolder;
    }

    public void fetch() {
        wast2JsonCmd = resolveOrInstallWast2Json();
    }

    public File execute(File wastFile) {
        return executeWast2Json(wastFile);
    }

    private File executeWast2Json(File wastFile) {
        var plainName = wastFile.getName().replace(".wast", "");
        var targetFolder = compiledWastTargetFolder.toPath().resolve(plainName).toFile();
        var destFile = targetFolder.toPath().resolve(SPEC_JSON).toFile();

        if (!targetFolder.mkdirs()) {
            log.warn("Could not create folder: " + targetFolder);
        }

        var command =
                List.of(wast2JsonCmd, wastFile.getAbsolutePath(), "-o", destFile.getAbsolutePath());
        log.info("Going to execute command: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File("."));
        pb.inheritIO();
        Process ps;
        try {
            ps = pb.start();
            ps.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (ps.exitValue() != 0) {
            System.err.println("wast2json exiting with:" + ps.exitValue());
            System.err.println(ps.getErrorStream().toString());
        }

        return targetFolder;
    }

    private void downloadAndExtract(URL url) {
        if (!wabtDownloadTargetFolder.mkdirs()) {
            log.warn("Could not create folder: " + wabtDownloadTargetFolder);
        }
        final File finalDestination =
                new File(wabtDownloadTargetFolder, new File(url.getFile()).getName());

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(finalDestination)) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error downloading : " + url, e);
        }
    }

    private String resolveOrInstallWast2Json() {
        ProcessBuilder pb = new ProcessBuilder(WAST2JSON);
        pb.directory(new File("."));
        pb.inheritIO();
        Process ps = null;
        try {
            ps = pb.start();
            ps.waitFor(1, TimeUnit.SECONDS);
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }

        if (ps != null && ps.exitValue() == 0) {
            log.info(WAST2JSON + " binary detected available, using the system one.");
            return WAST2JSON;
        }

        // Downloading locally WABT
        var binary =
                wabtDownloadTargetFolder
                        .toPath()
                        .resolve("wabt-" + wabtVersion)
                        .resolve("bin")
                        .resolve(WAST2JSON);

        if (binary.toFile().exists()) {
            log.warn(
                    "cached `wast2json` exists trying to use it, please run `mvn clean` if this"
                            + " doesn't succeed");
            return binary.toFile().getAbsolutePath();
        }

        log.info("Cannot locate " + WAST2JSON + " binary, downloading");

        var fileName = "wabt-" + wabtVersion + "-" + wabtArchitectureName() + ".tar.gz";
        var wabtRelease = wabtReleasesURL + wabtVersion + "/" + fileName;
        try {
            downloadAndExtract(URI.create(wabtRelease).toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream fis =
                new FileInputStream(
                        wabtDownloadTargetFolder
                                .toPath()
                                .resolve(fileName)
                                .toFile()
                                .getAbsolutePath())) {
            new TarExtractor(fis, wabtDownloadTargetFolder.toPath()).untar();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set executable
        if (!binary.toFile().setExecutable(true, false)) {
            log.warn("Couldn't change file to be executable: " + binary);
        }
        return binary.toFile().getAbsolutePath();
    }

    public String wabtArchitectureName() {
        var osName = this.osName.toLowerCase(Locale.ROOT);
        if (osName.startsWith("mac") || osName.startsWith("osx")) {
            return "macos-12";
        } else if (osName.startsWith("windows")) {
            return "windows";
        } else if (osName.startsWith("linux")) {
            return "ubuntu";
        } else {
            throw new IllegalArgumentException("Detected OS is not supported: " + osName);
        }
    }
}
