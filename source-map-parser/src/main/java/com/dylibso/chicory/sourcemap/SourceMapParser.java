package com.dylibso.chicory.sourcemap;

import com.dylibso.chicory.experimental.aot.SourceMap;
import com.dylibso.chicory.experimental.aot.SourceMap.Language;
import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.WasmModule;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SourceMapParser {
    private static final Logger logger = new SystemLogger();
    private static final WasmModule MODULE = SourceMapParserModule.load();

    private static final class SourceResult {
        String error;
        ArrayList<SourceUnit> units;
    }

    private static final class SourceUnit {
        @SuppressWarnings("unused")
        String name;

        @SuppressWarnings("unused")
        String directory;

        ArrayList<SourceFile> files;
    }

    private static final class SourceFile {
        String directory;
        String file;
        int language;
        ArrayList<long[]> lines;
    }

    private SourceMapParser() {}

    public static SourceMap parse(File file) throws SourceMapException {
        try (InputStream is = new FileInputStream(file)) {
            return parse(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SourceMap parse(byte[] wasm) throws SourceMapException {
        try (InputStream is = new ByteArrayInputStream(wasm)) {
            return parse(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SourceMap parse(InputStream is) throws SourceMapException {
        ArrayList<SourceMap.Entry> entries = new ArrayList<>();
        try (var stdoutStream = new ByteArrayOutputStream();
                var stderrStream = new ByteArrayOutputStream()) {

            WasiOptions wasiOpts =
                    WasiOptions.builder()
                            .withStdin(is)
                            .withStdout(stdoutStream)
                            .withStderr(stderrStream)
                            .withArguments(List.of("wasm-source-map.wasm"))
                            .build();

            try (var wasi =
                    WasiPreview1.builder().withLogger(logger).withOptions(wasiOpts).build()) {
                ImportValues imports =
                        ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
                Instance.builder(MODULE)
                        .withMachineFactory(SourceMapParserModule::create)
                        .withImportValues(imports)
                        .build();
            }

            var stdoutString = new String(stdoutStream.toByteArray(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            SourceResult result = gson.fromJson(stdoutString, SourceResult.class);

            if (result.error != null) {
                throw new SourceMapException(result.error);
            }
            for (var unit : result.units) {
                for (var file : unit.files) {
                    Language language = toLanguage(file.language);

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

                    for (var location : file.lines) {
                        long address = location[0];
                        int line = (int) location[1];
                        entries.add(new SourceMap.Entry(path, file.file, language, address, line));
                    }
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new SourceMap(entries);
    }

    private static Language toLanguage(int language) {
        switch (language) {
            case 0x0001:
                return Language.C89;
            case 0x0002:
                return Language.C;
            case 0x0003:
                return Language.Ada83;
            case 0x0004:
                return Language.CPP;
            case 0x0005:
                return Language.Cobol74;
            case 0x0006:
                return Language.Cobol85;
            case 0x0007:
                return Language.Fortran77;
            case 0x0008:
                return Language.Fortran90;
            case 0x0009:
                return Language.Pascal83;
            case 0x000a:
                return Language.Modula2;
            case 0x000b:
                return Language.Java;
            case 0x000c:
                return Language.C99;
            case 0x000d:
                return Language.Ada95;
            case 0x000e:
                return Language.Fortran95;
            case 0x000f:
                return Language.PLI;
            case 0x0010:
                return Language.ObjC;
            case 0x0011:
                return Language.ObjCPP;
            case 0x0012:
                return Language.UPC;
            case 0x0013:
                return Language.D;
            case 0x0014:
                return Language.Python;
            case 0x0015:
                return Language.OpenCL;
            case 0x0016:
                return Language.Go;
            case 0x0017:
                return Language.Modula3;
            case 0x0018:
                return Language.Haskell;
            case 0x0019:
                return Language.CPP03;
            case 0x001a:
                return Language.CPP11;
            case 0x001b:
                return Language.OCaml;
            case 0x001c:
                return Language.Rust;
            case 0x001d:
                return Language.C11;
            case 0x001e:
                return Language.Swift;
            case 0x001f:
                return Language.Julia;
            case 0x0020:
                return Language.Dylan;
            case 0x0021:
                return Language.CPP14;
            case 0x0022:
                return Language.Fortran03;
            case 0x0023:
                return Language.Fortran08;
            case 0x0024:
                return Language.RenderScript;
            case 0x0025:
                return Language.BLISS;
            case 0x8001:
                return Language.MipsAssembler;
            case 0x8e57:
                return Language.GoogleRenderScript;
            case 0x9001:
                return Language.SunAssembler;
            case 0x9101:
                return Language.AltiumAssembler;
            case 0xb000:
                return Language.BorlandDelphi;
            default:
                return Language.UNKNOWN;
        }
    }
}
