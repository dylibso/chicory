package com.dylibso.chicory.testing;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit {@link TestWatcher} extension for analyzing test outcomes of Web Assembly module tests.
 */
public class ChicoryTestWatcher implements TestWatcher {

    @Override
    public void testAborted(ExtensionContext context, Throwable throwable) {
        // do something
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> optional) {
        // do something
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable throwable) {
        createWasmObjectDumpForFailedTest(context);
    }

    private void createWasmObjectDumpForFailedTest(ExtensionContext context) {

        Method testMethod = context.getRequiredTestMethod();
        if (!isWasmObjectDumpEnabledFor(testMethod)) {
            return;
        }

        Class<?> clazz = context.getRequiredTestClass();
        String reference = clazz.getSimpleName() + "." + testMethod.getName();
        System.err.printf("wasm-objdump for test: %s%n", reference);

        String wasmFile = null;
        String symbolFilter = null;
        Tag[] tagsAnno = testMethod.getAnnotationsByType(Tag.class);
        for (var tag : tagsAnno) {
            String tagValue = tag.value();
            if (tagValue.startsWith("export=")) {
                symbolFilter =
                        new String(
                                Base64.getDecoder().decode(tagValue.substring("export=".length())));
            }
            if (tagValue.startsWith("wasm=")) {
                wasmFile = tagValue.substring("wasm=".length());
            }
        }

        WasmObjDumpTool.dump(wasmFile, symbolFilter);
    }

    private boolean isWasmObjectDumpEnabledFor(Method testMethod) {

        var methodAnnotation = testMethod.getAnnotation(ChicoryTest.class);
        if (methodAnnotation != null) {
            return methodAnnotation.dumpOnFail();
        }

        var typeAnnotation = testMethod.getDeclaringClass().getAnnotation(ChicoryTest.class);
        if (typeAnnotation != null) {
            return typeAnnotation.dumpOnFail();
        }

        return false;
    }

    @Override
    public void testSuccessful(ExtensionContext extensionContext) {
        // do something
    }
}
