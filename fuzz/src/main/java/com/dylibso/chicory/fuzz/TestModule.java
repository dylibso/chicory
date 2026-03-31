package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;

public class TestModule {
    private static final Logger logger = new SystemLogger();
    private static final int PER_CALL_TIMEOUT_SECONDS = 30;

    // Shared single-thread executor for per-call timeouts.
    // Reused across all calls to avoid thread exhaustion.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void shutdown() {
        executor.shutdownNow();
    }

    public List<String> paramsList(FunctionType type) {
        return type.params().stream().map(p -> randomNumber()).collect(Collectors.toList());
    }

    @SuppressWarnings("deprecation")
    public String randomNumber() {
        return RandomStringUtils.randomNumeric(2);
    }

    public List<TestResult> testModule(
            File targetWasm,
            WasmModule module,
            Instance instance,
            WasmRunner oracle,
            WasmRunner subject,
            String instructionType,
            boolean commitOnFailure)
            throws Exception {

        var results = new ArrayList<TestResult>();

        for (var i = 0; i < module.exportSection().exportCount(); i++) {
            var export = module.exportSection().getExport(i);
            if (export.exportType() != ExternalType.FUNCTION) {
                logger.info("Skipping export " + export.name());
                continue;
            }

            logger.info("Going to test export " + export.name());
            var type = instance.exportType(export.name());
            var params = paramsList(type);

            String oracleResult;
            try {
                logger.info("  Running oracle (interpreter) ...");
                long start = System.currentTimeMillis();
                oracleResult = runWithTimeout(oracle, targetWasm, export.name(), params);
                logger.info("  Oracle finished in " + (System.currentTimeMillis() - start) + " ms");
            } catch (TimeoutException e) {
                logger.warn("  Oracle timed out after " + PER_CALL_TIMEOUT_SECONDS + "s");
                if (commitOnFailure) {
                    saveCrashReproducer(
                            targetWasm, instructionType, export.name(), "timeout (oracle)");
                }
                continue;
            } catch (RuntimeException e) {
                logger.error("Failed to run oracle, skip the check: " + e);
                continue;
            }

            String subjectResult;
            try {
                logger.info("  Running subject (compiler) ...");
                long start = System.currentTimeMillis();
                subjectResult = runWithTimeout(subject, targetWasm, export.name(), params);
                logger.info(
                        "  Subject finished in " + (System.currentTimeMillis() - start) + " ms");
            } catch (TimeoutException e) {
                logger.warn("  Subject timed out after " + PER_CALL_TIMEOUT_SECONDS + "s");
                if (commitOnFailure) {
                    saveCrashReproducer(
                            targetWasm, instructionType, export.name(), "timeout (subject)");
                }
                subjectResult = null;
            } catch (RuntimeException e) {
                logger.warn("Failed to run subject, but oracle succeeded: " + e);
                subjectResult = null;
            }

            if (commitOnFailure && (subjectResult == null || !oracleResult.equals(subjectResult))) {
                try {
                    CrashReproducer.save(
                            targetWasm,
                            instructionType,
                            export.name(),
                            oracleResult,
                            subjectResult);
                } catch (IOException ex) {
                    logger.error("Failed to save crash reproducer: " + ex);
                }
            }

            results.add(new TestResult(oracleResult, subjectResult));
        }

        return results;
    }

    private String runWithTimeout(
            WasmRunner runner, File wasmFile, String functionName, List<String> params)
            throws TimeoutException, Exception {
        Future<String> future = executor.submit(() -> runner.run(wasmFile, functionName, params));
        try {
            return future.get(PER_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // interrupts the executor thread
            throw e;
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static void saveCrashReproducer(
            File targetWasm, String instructionType, String functionName, String reason) {
        try {
            CrashReproducer.save(targetWasm, instructionType, functionName, reason, null);
        } catch (IOException ex) {
            logger.error("Failed to save crash reproducer: " + ex);
        }
    }
}
