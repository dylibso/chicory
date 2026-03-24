package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FuzzTest extends TestModule {
    private static final Logger logger = new SystemLogger();
    private static final int ITERATIONS = Integer.getInteger("fuzz.test.iterations", 10);

    private final WasmSmithWrapper smith = new WasmSmithWrapper();
    private final WasmRunner interpreterRunner = new ChicoryRunner();
    private final WasmRunner compilerRunner = new ChicoryRunner(MachineFactoryCompiler::compile);

    @ParameterizedTest
    @EnumSource(
            value = InstructionType.class,
            names = {"NUMERIC", "TABLE"})
    void differentialFuzz(InstructionType type) throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            logger.info(
                    String.format("Iteration %d of %d for %s", i + 1, ITERATIONS, type.value()));

            File targetWasm;
            try {
                targetWasm =
                        smith.run(type.value() + "-" + i, "test.wasm", new InstructionTypes(type));
            } catch (IOException e) {
                logger.warn("wasm-smith failed to generate module, skipping iteration: " + e);
                continue;
            }

            // Try to parse — if Chicory can't parse a valid wasm-smith module,
            // save it as a crash reproducer for investigation
            WasmModule module;
            try {
                module = Parser.parse(targetWasm);
            } catch (RuntimeException e) {
                logger.warn("Generated WASM failed to parse: " + e);
                try {
                    CrashReproducer.save(targetWasm, type.value(), "parse", e.getMessage(), null);
                } catch (IOException ex) {
                    logger.error("Failed to save parse crash reproducer: " + ex);
                }
                continue;
            }

            // Skip modules with no exported functions
            boolean hasExportedFunction = false;
            for (int j = 0; j < module.exportSection().exportCount(); j++) {
                if (module.exportSection().getExport(j).exportType() == ExternalType.FUNCTION) {
                    hasExportedFunction = true;
                    break;
                }
            }
            if (!hasExportedFunction) {
                continue;
            }

            var instance = Instance.builder(module).withInitialize(true).withStart(false).build();

            var results =
                    testModule(
                            targetWasm,
                            module,
                            instance,
                            interpreterRunner,
                            compilerRunner,
                            type.value(),
                            true);

            for (var res : results) {
                assertEquals(res.getOracleResult(), res.getChicoryResult());
            }

            assertDoesNotThrow(() -> Instance.builder(module).build());
        }
    }
}
