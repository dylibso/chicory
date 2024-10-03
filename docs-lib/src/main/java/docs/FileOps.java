package docs;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class FileOps {

    private FileOps() {}

    public static void copyFromWasmCorpus(String sourceName, String destName) throws Exception {
        var dest = new File(".").toPath().resolve(destName);
        if (dest.toFile().exists()) {
            dest.toFile().delete();
        }
        Files.copy(
                new File(".")
                        .toPath()
                        .resolve("wasm-corpus")
                        .resolve("src")
                        .resolve("main")
                        .resolve("resources")
                        .resolve("compiled")
                        .resolve(sourceName),
                dest,
                StandardCopyOption.REPLACE_EXISTING);
    }

    public static void writeResult(String folder, String name, String content) throws Exception {
        var dir = new File(".").toPath().resolve(folder);
        dir.toFile().mkdirs();
        FileWriter fileWriter = new FileWriter(dir.resolve(name).toFile(), StandardCharsets.UTF_8);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(content);
        printWriter.flush();
        printWriter.close();
    }
}
