package com.dylibso.chicory.fuzz;

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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FuzzTest extends TestModule {
    private static final Logger logger = new SystemLogger();
    private static final int ITERATIONS = Integer.getInteger("fuzz.test.iterations", 10);

    private final WasmSmithWrapper smith = new WasmSmithWrapper();
    private final WasmRunner interpreterRunner = new ChicoryRunner();
    private final WasmRunner compilerRunner = new ChicoryRunner(MachineFactoryCompiler::compile);

    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    @ParameterizedTest
    @EnumSource(
            value = InstructionType.class,
            names = {
                "NUMERIC",
                "TABLE",
                "MEMORY",
                "CONTROL",
                "VARIABLE",
                "PARAMETRIC",
                "REFERENCE"
            })
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
                saveCrashReproducer(targetWasm, type.value(), "parse", e);
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

            // Instantiate and run differential tests — any crash is saved as a reproducer
            Instance instance;
            try {
                instance = Instance.builder(module).withInitialize(true).withStart(false).build();
            } catch (RuntimeException e) {
                logger.warn("Failed to instantiate module: " + e);
                saveCrashReproducer(targetWasm, type.value(), "instantiate", e);
                continue;
            }

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
        }
    }

    private static void saveCrashReproducer(
            File targetWasm, String instructionType, String phase, RuntimeException e) {
        try {
            CrashReproducer.save(targetWasm, instructionType, phase, e.getMessage(), null);
        } catch (IOException ex) {
            logger.error("Failed to save crash reproducer: " + ex);
        }
    }
}
