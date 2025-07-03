package com.dylibso.chicory.dwarf.rust;

import com.dylibso.chicory.dwarf.rust.internal.Wasm;
import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.ParserException;
import com.dylibso.chicory.runtime.Stratum;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.WasmWriter;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DebugParser implements com.dylibso.chicory.runtime.DebugParser {
    private static final Logger logger = new SystemLogger();
    private static final WasmModule MODULE = Wasm.load();

    @Override
    public Stratum apply(WasmModule wasmModule) {
        return parse(wasmModule);
    }

    static final class SourceResult {
        public String error;
        public List<SourceUnit> units;
        public List<long[]> lines;
        public Map<String, long[]> functions;
    }

    static final class SourceUnit {
        @SuppressWarnings("unused")
        public String name;

        @SuppressWarnings("unused")
        public String directory;

        public List<SourceFile> files;
    }

    static final class SourceFile {
        public int id;
        public String directory;
        public String file;

        @SuppressWarnings("unused")
        public int language;
    }

    private static final class FileInfo {
        final String file;
        final String path;

        public FileInfo(String path, SourceFile file) {
            this.file = file.file;
            this.path = path;
        }
    }

    private static byte[] toBytes(WasmModule module) {
        var writer = new WasmWriter();
        var keepers =
                Set.of(
                        ".debug_info",
                        ".debug_line",
                        ".debug_str",
                        ".debug_aranges",
                        ".debug_pubnames",
                        ".debug_loc",
                        ".debug_ranges",
                        ".debug_abbrev",
                        ".debug_pubtypes");
        for (var section : module.customSections()) {
            if (section instanceof UnknownCustomSection) {
                if (keepers.contains(section.name())) {
                    writer.writeSection((UnknownCustomSection) section);
                }
            }
        }
        writer.writeEmptyCodeSection();
        return writer.bytes();
    }

    public static Stratum parse(WasmModule module) throws ParserException {
        return parse(toBytes(module));
    }

    private static Stratum parse(byte[] wasm) throws ParserException {
        try (InputStream is = new ByteArrayInputStream(wasm)) {
            return parse(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stratum parse(InputStream is) throws ParserException {
        SourceResult result = getSourceResult(is);
        var stratum = Stratum.builder("WASM");
        HashMap<Integer, FileInfo> sourceFilesByID = new HashMap<>();
        for (var unit : result.units) {
            for (var file : unit.files) {

                String path = file.file;
                if (path != null) {
                    path = path.replace('\\', '/');
                }
                if (file.directory != null) {
                    var dir = file.directory.replace('\\', '/');
                    if (dir.endsWith("/")) {
                        path = file.directory + path;
                    } else {
                        path = file.directory + "/" + path;
                    }
                }
                sourceFilesByID.put(file.id, new FileInfo(path, file));
            }
        }

        int size = result.lines.size();
        for (int i = 0; i < size; i++) {
            var location = result.lines.get(i);
            long address = location[0];

            // we treat each address as a line.
            var outputLineCount = 1;
            if (i + 1 < size) {
                var nextAddress = result.lines.get(i + 1)[0];
                outputLineCount = (int) (nextAddress - address);
            }

            int fileID = (int) location[1];
            int line = (int) location[2];

            var file = sourceFilesByID.get(fileID);
            stratum.withLineMapping(file.file, file.path, line, 1, address, outputLineCount);
        }

        if (result.functions != null) {
            for (var entry : result.functions.entrySet()) {
                String functionName = entry.getKey();
                long[] locations = entry.getValue();
                if (locations.length < 2) {
                    continue; // Invalid function data
                }
                long startAddress = locations[0];
                long endAddress = locations[1];
                stratum.withFunctionMapping(functionName, startAddress, endAddress);
            }
        }

        return stratum.build();
    }

    static SourceResult getSourceResult(InputStream is) {
        SourceResult result = null;
        try (var stdoutStream = new ByteArrayOutputStream();
                var stderrStream = new ByteArrayOutputStream()) {

            WasiOptions wasiOpts =
                    WasiOptions.builder()
                            .withStdin(is)
                            .withStdout(stdoutStream)
                            .withStderr(stderrStream)
                            .build();

            try (var wasi =
                    WasiPreview1.builder().withLogger(logger).withOptions(wasiOpts).build()) {
                ImportValues imports =
                        ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
                Instance.builder(MODULE)
                        .withMachineFactory(Wasm::create)
                        .withImportValues(imports)
                        .build();
            }

            var stdoutString = new String(stdoutStream.toByteArray(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.readValue(stdoutString, SourceResult.class);

            if (result.error != null) {
                throw new ParserException(result.error);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }
}
