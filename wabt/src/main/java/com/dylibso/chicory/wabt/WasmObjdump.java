package com.dylibso.chicory.wabt;

import static java.nio.file.Files.copy;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class WasmObjdump {
    private static final Logger logger = new SystemLogger();
    private static final Module module = Module.builder("wasm-objdump").build();

    private final File file;
    private final String[] options;

    private WasmObjdump(File file, String[] options) {
        this.file = file;
        this.options = options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String dump() {
        try (ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stderrStream = new ByteArrayOutputStream()) {
            try (FileInputStream fis = new FileInputStream(file);
                    FileSystem fs =
                            Jimfs.newFileSystem(
                                    Configuration.unix().toBuilder()
                                            .setAttributeViews("unix")
                                            .build())) {

                var wasiOpts = WasiOptions.builder();

                wasiOpts.withStdout(stdoutStream);
                wasiOpts.withStderr(stdoutStream);

                Path target = fs.getPath("tmp");
                java.nio.file.Files.createDirectory(target);
                Path path = target.resolve(file.getName());
                copy(fis, path, StandardCopyOption.REPLACE_EXISTING);
                wasiOpts.withDirectory(target.toString(), target);

                List<String> args = new ArrayList<>();
                args.add("wasm-objdump");
                args.addAll(List.of(options));
                args.add(path.toString());
                wasiOpts.withArguments(args);

                var wasi = new WasiPreview1(logger, wasiOpts.build());
                var imports = new HostImports(wasi.toHostFunctions());

                module.withHostImports(imports).instantiate();

                return new String(stdoutStream.toByteArray());
            } catch (WASMMachineException e) {
                assert (e.getCause() instanceof WasiExitException);
                var stdout = new String(stdoutStream.toByteArray());
                var stderr = new String(stderrStream.toByteArray());
                throw new RuntimeException(stdout + "\n" + stderr);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Implementing only the needed options for now
    //  -h, --headers                Print headers
    //  -j, --section=SECTION        Select just one section
    //  -s, --full-contents          Print raw section contents
    //  -d, --disassemble            Disassemble function bodies
    //      --debug                  Print extra debug information
    //  -x, --details                Show section details
    //  -r, --reloc                  Show relocations inline with disassembly
    //      --section-offsets        Print section offsets instead of file offsets in code
    // disassembly
    public static class Builder {
        private File file;
        private boolean disassemble;
        private boolean details;

        private Builder() {}

        public Builder withFile(File f) {
            this.file = f;
            return this;
        }

        public Builder withDisassemble(boolean d) {
            this.disassemble = d;
            return this;
        }

        public Builder withDetails(boolean d) {
            this.details = d;
            return this;
        }

        public WasmObjdump build() {
            List<String> options = new ArrayList<>();
            if (disassemble) {
                options.add("-d");
            }
            if (details) {
                options.add("-x");
            }
            return new WasmObjdump(file, options.toArray(new String[0]));
        }
    }
}
