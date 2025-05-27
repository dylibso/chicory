package com.dylibso.chicory.testing;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ZigTestsuiteTest {

    @Test
    public void shouldRunZigStdlibTestsuite() throws Exception {
        try (FileSystem fs =
                ZeroFs.newFileSystem(
                        Configuration.unix().toBuilder().setAttributeViews("unix").build())) {
            Path target = fs.getPath(".");

            var wasiOpts =
                    WasiOptions.builder()
                            .inheritSystem()
                            .withArguments(List.of("test.wasm"))
                            .withDirectory(target.toString(), target)
                            .build();
            var wasi =
                    WasiPreview1.builder()
                            .withLogger(new SystemLogger())
                            .withOptions(wasiOpts)
                            .build();

            var instance =
                    Instance.builder(ZigModule.load())
                            .withImportValues(
                                    ImportValues.builder()
                                            .addFunction(wasi.toHostFunctions())
                                            .build())
                            .withMachineFactory(ZigModule::create);

            try {
                instance.build(); // call start
            } catch (WasiExitException e) {
                if (e.exitCode() != 0) {
                    throw new RuntimeException("exit with errors: " + e.exitCode());
                }
                System.out.println("Success!!!");
            }
        }
    }
}
