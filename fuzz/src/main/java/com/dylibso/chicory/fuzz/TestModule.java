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
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;

public class TestModule {
    private static final Logger logger = new SystemLogger();

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
                oracleResult = oracle.run(targetWasm, export.name(), params);
            } catch (RuntimeException e) {
                logger.error("Failed to run oracle, skip the check: " + e);
                continue;
            }

            String subjectResult;
            try {
                subjectResult = subject.run(targetWasm, export.name(), params);
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
}
