package com.dylibso.chicory.maven;

import static com.dylibso.chicory.maven.Constants.SPEC_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private String wabtVersion;
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
            throw new RuntimeException("Failed to execute wast2json program.");
        }

        return targetFolder;
    }

    private void downloadAndExtract(URL url) {
        if (!wabtDownloadTargetFolder.mkdirs()) {
            log.warn("Could not create folder: " + wabtDownloadTargetFolder);
        }
        final File finalDestination =
                new File(wabtDownloadTargetFolder, new File(url.getFile()).getName());

        var retries = 3;
        while (retries > 0) {
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(finalDestination)) {
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                break;
            } catch (IOException e) {
                --retries;
            }
        }

        if (retries == 0) {
            throw new IllegalArgumentException("Error downloading : " + url);
        }
    }

    private String resolveLatestVersion() {
        try {
            URI releaseURI = new URI(wabtReleasesURL);
            URI latestVersionURI =
                    new URI(
                            releaseURI.getScheme(),
                            "api." + releaseURI.getHost(),
                            "/repos" + releaseURI.getPath().replace("download/", "latest"),
                            releaseURI.getFragment());

            HttpRequest request = HttpRequest.newBuilder().uri(latestVersionURI).GET().build();

            HttpResponse<String> response =
                    HttpClient.newBuilder()
                            .build()
                            .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to resolve latest version of WABT");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode latestVersionJson = mapper.readTree(response.body());
            String latestVersion = latestVersionJson.get("name").asText();
            if (latestVersion.startsWith("v")) {
                latestVersion = latestVersion.substring(1);
            }

            return latestVersion;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveOrInstallWast2Json() {
        ProcessBuilder pb = new ProcessBuilder(WAST2JSON, "--version");
        pb.directory(new File("."));
        Process ps = null;
        String systemVersion = null;
        try {
            ps = pb.start();
            ps.waitFor(1, TimeUnit.SECONDS);
            if (ps.exitValue() != 0) {
                System.err.println(ps.getErrorStream().toString());
            }
            systemVersion = new String(ps.getInputStream().readAllBytes());
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }

        if (wabtVersion.equals("latest")) {
            wabtVersion = resolveLatestVersion();
        }

        if (ps != null && ps.exitValue() == 0) {
            if (systemVersion != null && versionMatches(systemVersion, wabtVersion)) {
                log.info(WAST2JSON + " binary detected available, using the system one.");
                return WAST2JSON;
            }
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

    /**
     * Compares the available wast2json version with the minimum required version. It
     * first checks for an exact match on the version strings, and if they do not
     * exactly match it tries to parse and compare them using 3 part semantic version numbers.
     * @param actual the version of wast2json already installed
     * @param required the version of wast2json that's required
     * @return true if the actual version meets the requirements, false otherwise
     */
    private boolean versionMatches(String actual, String required) {
        actual = actual.trim();
        required = required.trim();

        if (actual.equals(required)) {
            return true;
        }

        var actualComponents = splitVersion(actual);
        var requiredComponents = splitVersion(required);

        if (actualComponents.length != 3 || requiredComponents.length != 3) {
            return false;
        }

        return componentMatches(actualComponents, requiredComponents, 0)
                && componentMatches(actualComponents, requiredComponents, 1)
                && componentMatches(actualComponents, requiredComponents, 2);
    }

    private boolean componentMatches(String[] actual, String[] required, int component) {
        var actualField = Integer.parseInt(actual[component]);
        var requiredField = Integer.parseInt(required[component]);

        return actualField >= requiredField;
    }

    private String[] splitVersion(String v) {
        return v.split("\\.");
    }
}
