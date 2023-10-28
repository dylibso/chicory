package com.dylibso.chicory.testing;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public enum WasmDumper {
    ;

    /**
     * Keeps track of which modules were already dumped. This helps to avoid repeated work.
     * Keys: wasm module uri
     * Values: "Reference key" -> TestClass.testMethod where the first full dump was collected.
     */
    private static final ConcurrentMap<String, String> ALREADY_DUMPED_MODULES =
            new ConcurrentHashMap<>();

    private static final String WASM_OBJECTDUMP = "wasm-objdump";

    public static void objectDump(String reference, URI uri) {

        String uriString = uri.toString();

        if (ALREADY_DUMPED_MODULES.containsKey(uriString)) {
            // skipping dump of already printed module
            System.err.printf(
                    "Skipping wasm-objdump as it was already generated. reference: %s module: %s%n",
                    reference, uriString);
            return;
        }

        System.err.println("wasm-objdump for reference: " + reference + " module: " + uri);
        var pb = new ProcessBuilder(WASM_OBJECTDUMP, "-d", Path.of(uri).toFile().getAbsolutePath());
        pb.directory(new File("."));
        pb.inheritIO();

        try {
            pb.start().waitFor();
            ALREADY_DUMPED_MODULES.put(uriString, reference);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
