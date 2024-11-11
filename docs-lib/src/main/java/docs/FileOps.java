package docs;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileOps {

    private FileOps() {}

    public static void copyFromWasmCorpus(String sourceName, String destName) throws Exception {
        var dest = Path.of(".").resolve(destName);
        if (dest.exists()) {
            dest.delete();
        }
        Files.copy(
                Path.of("..")
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
        var dir = Path.of(".").resolve(folder);
        Files.createDirectories(dir);
        Writer fileWriter = Files.newBufferedWriter(dir.resolve(name), StandardCharsets.UTF_8);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(content);
        printWriter.flush();
        printWriter.close();
    }
}
