package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    public String run(File file, String functionName, List<String> params) throws Exception {
        var command = new ArrayList<String>();
        command.addAll(List.of(binaryName, file.getAbsolutePath(), "--invoke", functionName));
        command.addAll(params);
        logger.info("Going to execute command:\n" + String.join(" ", command));
        // write the command to a file to make it reproducible
        // TODO: centralize the management of folders/files it's too scattered
        try (var outputStream =
                new FileOutputStream(file.getParentFile() + "/" + cmdName + "-command.txt")) {
            outputStream.write(String.join(" ", command).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        ProcessBuilder pb = new ProcessBuilder(command);

        // execute
        pb.directory(new File("."));
        Process ps;
        try {
            ps = pb.start();
            ps.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check
        if (ps.exitValue() != 0) {
            System.err.println(cmdName + " exiting with:" + ps.exitValue());
            System.err.println(new String(ps.getErrorStream().readAllBytes()));
            throw new RuntimeException("Failed to execute " + cmdName + " program.");
        }

        // get output
        BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line + "\n");
        String result = sb.toString();
        logger.info("Returned output is:\n" + result);
        return result;
    }
}
