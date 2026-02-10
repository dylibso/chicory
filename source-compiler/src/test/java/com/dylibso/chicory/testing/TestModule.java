package com.dylibso.chicory.testing;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.source.compiler.MachineFactorySourceCompiler;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;

/**
 * TestModule for source-compiler tests. Uses MachineFactorySourceCompiler instead of
 * MachineFactoryCompiler.
 */
public class TestModule {

    private final WasmModule module;

    /** Logical module name (e.g. "i32") derived from the classpath, used for source dumps. */
    private final String moduleName;

    /** Discriminator (e.g. "spec_0") derived from the classpath, used for unique class names. */
    private final String discriminator;

    private static final String HACK_MATCH_ALL_MALFORMED_EXCEPTION_TEXT =
            "Matching keywords to get the WebAssembly testsuite to pass: "
                    + "malformed UTF-8 encoding "
                    + "import after function "
                    + "inline function type "
                    + "constant out of range"
                    + "unknown operator "
                    + "unexpected token "
                    + "unexpected mismatching "
                    + "mismatching label "
                    + "unknown type "
                    + "duplicate func "
                    + "duplicate local "
                    + "duplicate global "
                    + "duplicate memory "
                    + "duplicate table "
                    + "mismatching label "
                    + "import after global "
                    + "import after table "
                    + "import after memory "
                    + "i32 constant out of range "
                    + "unknown label "
                    + "alignment "
                    + "multiple start sections";

    public static TestModule of(String classpath) {
        try (var is = CorpusResources.getResource(classpath.substring(1))) {
            // Extract module name from classpath (e.g., "/i32/spec.0.wasm" -> "i32")
            String moduleName = extractModuleName(classpath);
            // Extract discriminator from classpath (e.g., "/i32/spec.0.wasm" -> "spec_0")
            String discriminator = extractDiscriminator(classpath);

            WasmModule module;
            if (classpath.endsWith(".wat")) {
                byte[] parsed;
                try {
                    parsed = Wat2Wasm.parse(is);
                } catch (RuntimeException e) {
                    throw new MalformedException(
                            e.getMessage() + HACK_MATCH_ALL_MALFORMED_EXCEPTION_TEXT);
                }
                module = Parser.parse(parsed);
            } else {
                module = Parser.parse(is);
            }
            return new TestModule(module, moduleName, discriminator);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestModule of(WasmModule module) {
        return new TestModule(module, null, null);
    }

    private static String extractModuleName(String classpath) {
        // Extract module name from classpath like "/i32/spec.0.wasm" -> "i32"
        if (classpath.startsWith("/") && classpath.length() > 1) {
            String withoutLeadingSlash = classpath.substring(1);
            int firstSlash = withoutLeadingSlash.indexOf('/');
            if (firstSlash > 0) {
                return withoutLeadingSlash.substring(0, firstSlash);
            }
            // If no slash, use filename without extension
            int lastSlash = withoutLeadingSlash.lastIndexOf('/');
            int lastDot = withoutLeadingSlash.lastIndexOf('.');
            if (lastDot > lastSlash) {
                return withoutLeadingSlash.substring(lastSlash + 1, lastDot);
            }
        }
        return "unknown";
    }

    /**
     * Extract a discriminator from the classpath for use in class names.
     * Example: "/i32/spec.0.wasm" -> "spec_0"
     */
    private static String extractDiscriminator(String classpath) {
        if (classpath.startsWith("/") && classpath.length() > 1) {
            String withoutLeadingSlash = classpath.substring(1);
            int lastSlash = withoutLeadingSlash.lastIndexOf('/');
            int lastDot = withoutLeadingSlash.lastIndexOf('.');
            if (lastDot > lastSlash && lastDot > 0) {
                String filename = withoutLeadingSlash.substring(lastSlash + 1, lastDot);
                // Replace dots and other invalid characters with underscores
                return filename.replace('.', '_').replace('-', '_');
            }
        }
        return "unknown";
    }

    public TestModule(WasmModule module, String moduleName, String discriminator) {
        this.module = module;
        this.moduleName = moduleName;
        this.discriminator = discriminator;
    }

    public Instance instantiate(Store s) {
        ImportValues importValues = s.toImportValues();

        // Enable source dumping by default in tests (can be disabled via system property)
        //   -Dchicory.source.dumpSources=false
        boolean dumpSources = !Boolean.getBoolean("chicory.source.dumpSources.disable");

        return Instance.builder(module)
                .withImportValues(importValues)
                .withMachineFactory(
                        instance -> {
                            // Mangle class name with discriminator in test scope only
                            String mangledClassName = null;
                            if (discriminator != null) {
                                mangledClassName =
                                        "com.dylibso.chicory.gen.CompiledMachine_" + discriminator;
                            }
                            return MachineFactorySourceCompiler.builder(instance.module())
                                    .withModuleName(moduleName)
                                    .withClassName(mangledClassName)
                                    .withDumpSources(dumpSources)
                                    .compile()
                                    .apply(instance);
                        })
                .build();
    }
}
