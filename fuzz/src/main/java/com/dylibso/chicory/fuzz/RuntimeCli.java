package com.dylibso.chicory.fuzz;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class RuntimeCli {

    protected static final Logger logger = new SystemLogger();

    protected final String binaryName;
    protected final String cmdName;

    RuntimeCli(String binaryName, String cmdName) {
        this.binaryName = binaryName;
        this.cmdName = cmdName;
    }

    public String run(Path path, String functionName, List<String> params) throws IOException {
        var command = new ArrayList<String>();
        command.addAll(
                List.of(binaryName, path.toAbsolutePath().toString(), "--invoke", functionName));
        command.addAll(params);
        logger.info("Going to execute command:\n" + String.join(" ", command));
        // write the command to a file to make it reproducible
        // TODO: centralize the management of folders/files it's too scattered
        Files.writeString(
                path.getParent().resolve(cmdName + "-command.txt"), String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);

        // execute
        pb.directory(Path.of(".").toFile());
        Process ps;
        try {
            ps = pb.start();
            ps.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        // check
        if (ps.exitValue() != 0) {
            System.err.println(cmdName + " exiting with:" + ps.exitValue());
            System.err.println(new String(ps.getErrorStream().readAllBytes(), UTF_8));
            throw new RuntimeException("Failed to execute " + cmdName + " program.");
        }

        // get output
        BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream(), UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        String result = sb.toString();
        logger.info("Returned output is:\n" + result);
        return result;
    }
}
