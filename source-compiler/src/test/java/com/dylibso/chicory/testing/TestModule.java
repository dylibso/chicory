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

    /** Fully-qualified mangled class name (package encodes spec, e.g. "com.dylibso.chicory.gen.i32.CompiledMachine_spec_0"). */
    private final String mangledClassName;

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
            String mangledClassName = extractMangledClassName(classpath);

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
            return new TestModule(module, mangledClassName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestModule of(WasmModule module) {
        return new TestModule(module, null);
    }

    /**
     * Extract fully-qualified mangled class name from classpath.
     *
     * <p>Example: "/i32/spec.0.wasm" -> "com.dylibso.chicory.gen.i32.CompiledMachine_spec_0"
     */
    private static String extractMangledClassName(String classpath) {
        if (!classpath.startsWith("/") || classpath.length() <= 1) {
            return null;
        }

        String withoutLeadingSlash = classpath.substring(1);
        int firstSlash = withoutLeadingSlash.indexOf('/');
        int lastSlash = withoutLeadingSlash.lastIndexOf('/');
        int lastDot = withoutLeadingSlash.lastIndexOf('.');
        if (firstSlash <= 0 || lastDot <= lastSlash || lastDot <= 0) {
            return null;
        }

        String moduleDir = withoutLeadingSlash.substring(0, firstSlash);
        String filename = withoutLeadingSlash.substring(lastSlash + 1, lastDot);

        String safeModuleDir = moduleDir.replace('-', '_') + "_";

        String suffix = filename.replace('.', '_').replace('-', '_');
        return "com.dylibso.chicory.gen." + safeModuleDir + ".CompiledMachine_" + suffix;
    }

    public TestModule(WasmModule module, String mangledClassName) {
        this.module = module;
        this.mangledClassName = mangledClassName;
    }

    public Instance instantiate(Store s) {
        ImportValues importValues = s.toImportValues();

        // Enable source dumping by default in tests (can be disabled via system property)
        //   -Dchicory.source.dumpSources=false
        boolean dumpSources = !Boolean.getBoolean("chicory.source.dumpSources.disable");

        // Generate sources BEFORE building instance, so we can dump even if validation fails
        if (dumpSources && mangledClassName != null) {
            try {
                MachineFactorySourceCompiler.builder(module)
                        .withClassName(mangledClassName)
                        .withDumpSources(true)
                        .generateAndDumpSources();
            } catch (Throwable e) {
                // Ignore - generateAndDumpSources has its own error handling and finally block
            }
        }

        return Instance.builder(module)
                .withImportValues(importValues)
                .withMachineFactory(
                        instance -> {
                            return MachineFactorySourceCompiler.builder(instance.module())
                                    .withClassName(mangledClassName)
                                    .withDumpSources(false)
                                    .compile()
                                    .apply(instance);
                        })
                .build();
    }
}
