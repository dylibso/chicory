package com.dylibso.chicory.testing;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;
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
        System.err.printf("Generating WASM ObjectDump for failed test: %s%n", reference);

        TestModule testModule = getTestModule(context, clazz);

        String symbolFilter = null;
        Tag[] tagsAnno = testMethod.getAnnotationsByType(Tag.class);
        for (var tag : tagsAnno) {
            String tagValue = tag.value();
            if (tagValue.startsWith("export=")) {
                String exportName =
                        new String(
                                Base64.getDecoder().decode(tagValue.substring("export=".length())));
                symbolFilter = exportName;
            }
        }

        WasmDumper.objectDump(reference, testModule.getFile().toURI(), symbolFilter);
    }

    private static TestModule getTestModule(ExtensionContext context, Class<?> clazz) {
        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.getType().isAssignableFrom(TestModule.class))
                .peek(field -> field.setAccessible(true))
                .map(
                        field -> {
                            try {
                                return (TestModule) field.get(context.getRequiredTestInstance());
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .findFirst()
                .get();
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
