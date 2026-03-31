package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FuzzTest extends TestModule {
    private static final Logger logger = new SystemLogger();
    private static final int ITERATIONS = Integer.getInteger("fuzz.test.iterations", 10);

    private final WasmSmithWrapper smith = new WasmSmithWrapper();
    private final WasmRunner interpreterRunner = new ChicoryRunner();
    private final WasmRunner compilerRunner = new ChicoryRunner(MachineFactoryCompiler::compile);

    @AfterEach
    void tearDown() {
        shutdown();
    }

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
        var failures = new ArrayList<String>();
        var smithFailures = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            logger.info(
                    String.format("Iteration %d of %d for %s", i + 1, ITERATIONS, type.value()));

            File targetWasm;
            try {
                targetWasm =
                        smith.run(type.value() + "-" + i, "test.wasm", new InstructionTypes(type));
            } catch (IOException e) {
                logger.warn("wasm-smith failed to generate module: " + e);
                smithFailures++;
                continue;
            }

            // Try to parse — save reproducer on failure but continue to next iteration
            WasmModule module;
            try {
                module = Parser.parse(targetWasm);
            } catch (RuntimeException e) {
                logger.warn("Generated WASM failed to parse: " + e);
                saveCrashReproducer(targetWasm, type.value(), "parse", e);
                failures.add("parse: " + e.getMessage());
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

            // Instantiate — random modules may trap during init (e.g. unreachable in
            // start function or init expression), which is expected behavior, not a bug
            Instance instance;
            try {
                instance = Instance.builder(module).withInitialize(true).withStart(false).build();
            } catch (RuntimeException e) {
                logger.info("Module trapped during instantiation (expected for random wasm): " + e);
                continue;
            }

            // Run differential test
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
                if (res.getChicoryResult() == null) {
                    // Compiler crashed — reproducer already saved by testModule
                    failures.add("compiler crash (subject returned null)");
                } else if (!res.getOracleResult().equals(res.getChicoryResult())) {
                    failures.add(
                            "mismatch: oracle="
                                    + res.getOracleResult()
                                    + " subject="
                                    + res.getChicoryResult());
                }
            }
        }

        // Fail if wasm-smith couldn't generate any modules at all
        assertTrue(
                smithFailures < ITERATIONS,
                "wasm-smith failed on all "
                        + ITERATIONS
                        + " iterations for "
                        + type.value()
                        + " — check smith.default.properties for unsupported flags");

        // Fail after all iterations so all reproducers are saved
        assertTrue(
                failures.isEmpty(),
                failures.size()
                        + " failure(s) found for "
                        + type.value()
                        + ". Reproducers saved to target/crash-reproducers/.\n"
                        + String.join("\n", failures));
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
