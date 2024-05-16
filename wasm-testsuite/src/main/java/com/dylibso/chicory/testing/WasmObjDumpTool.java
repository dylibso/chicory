package com.dylibso.chicory.testing;

import com.dylibso.chicory.wabt.WasmObjdump;
import java.nio.file.Path;
import java.util.Scanner;

public class WasmObjDumpTool {

    private WasmObjDumpTool() {
        // prevent instantiation
    }

    public static void dump(String wasmFilePath, String symbolFilter) {
        System.out.printf("Module: %s%n", wasmFilePath);
        System.out.printf("Filter: %s%n", symbolFilter == null ? "NONE" : symbolFilter);

        var wasmObjdump =
                WasmObjdump.builder()
                        .withDisassemble(true)
                        .withFile(Path.of(wasmFilePath).toFile())
                        .build();
        try {
            var output = wasmObjdump.dump();

            scrapeProcessOutputForSymbolAndPrintUntilEndOfBlock(output, symbolFilter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute wasm-objdump program.", e);
        }
    }

    private static void scrapeProcessOutputForSymbolAndPrintUntilEndOfBlock(
            String output, String symbolFilter) {

        String filterPattern = "<" + symbolFilter + ">";
        String blockEndMarker = "| end";

        boolean inMatchedSymbol = false;
        try (var scanner = new Scanner(output)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(filterPattern)) {
                    inMatchedSymbol = true;
                    System.out.println("--------------------------------------------");
                }
                if (inMatchedSymbol) {
                    System.out.println(line);
                    if (line.endsWith(blockEndMarker)) {
                        System.out.println("--------------------------------------------");
                        break;
                    }
                }
            }
        }
    }
}
