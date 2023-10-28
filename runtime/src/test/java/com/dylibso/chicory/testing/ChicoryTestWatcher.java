package com.dylibso.chicory.testing;

import java.lang.reflect.Method;
import java.util.Optional;
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
        System.err.printf("Generating WASM ObjectDump for failed test: %s%n", reference);

        for (var field : clazz.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(TestModule.class)) {
                field.setAccessible(true);
                try {
                    TestModule testModule =
                            (TestModule) field.get(context.getRequiredTestInstance());
                    WasmDumper.objectDump(reference, testModule.getFile().toURI());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
