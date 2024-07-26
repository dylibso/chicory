package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HostImportsTest {

    @Nested
    class Builder {
        @Test
        void empty() {
            final HostImports result = HostImports.builder().build();
            assertEquals(0, result.functionCount());
            assertEquals(0, result.globalCount());
            assertEquals(0, result.memoryCount());
            assertEquals(0, result.tableCount());
            assertNull(result.index());
        }

        @Nested
        class Function {

            @Test
            void withFunctions() {
                final HostImports result =
                        HostImports.builder()
                                .withFunctions(
                                        Arrays.asList(
                                                new HostFunction(null, "module_1", "", null, null),
                                                new HostFunction(null, "module_2", "", null, null)))
                                .build();
                assertEquals(2, result.functionCount());
            }

            @Test
            void addFunction() {
                final HostImports result =
                        HostImports.builder()
                                .addFunction(new HostFunction(null, "module_1", "", null, null))
                                .addFunction(new HostFunction(null, "module_2", "", null, null))
                                .build();
                assertEquals(2, result.functionCount());
            }
        }

        @Nested
        class Global {

            @Test
            void withGlobals() {
                final HostImports result =
                        HostImports.builder()
                                .withGlobals(
                                        Arrays.asList(
                                                new HostGlobal(
                                                        "spectest",
                                                        "global_i32",
                                                        new GlobalInstance(Value.i32(666))),
                                                new HostGlobal(
                                                        "spectest",
                                                        "global_i64",
                                                        new GlobalInstance(Value.i64(666)))))
                                .build();
                assertEquals(2, result.globalCount());
            }

            @Test
            void addGlobal() {
                final HostImports result =
                        HostImports.builder()
                                .addGlobal(
                                        new HostGlobal(
                                                "spectest",
                                                "global_i32",
                                                new GlobalInstance(Value.i32(666))))
                                .addGlobal(
                                        new HostGlobal(
                                                "spectest",
                                                "global_i64",
                                                new GlobalInstance(Value.i64(666))))
                                .build();
                assertEquals(2, result.globalCount());
            }
        }

        @Nested
        class Memory {

            @Test
            void withMemories() {
                final HostImports result =
                        HostImports.builder()
                                .withMemories(
                                        Arrays.asList(
                                                new HostMemory("spectest", "memory", null),
                                                new HostMemory("spectest", "memory_2", null)))
                                .build();
                assertEquals(2, result.memoryCount());
            }

            @Test
            void addMemory() {
                final HostImports result =
                        HostImports.builder()
                                .addMemory(new HostMemory("spectest", "memory", null))
                                .addMemory(new HostMemory("spectest", "memory_2", null))
                                .build();
                assertEquals(2, result.memoryCount());
            }
        }

        @Nested
        class Table {

            @Test
            void withTables() {
                final HostImports result =
                        HostImports.builder()
                                .withTables(
                                        Arrays.asList(
                                                new HostTable(
                                                        "spectest",
                                                        "table",
                                                        Collections.emptyMap()),
                                                new HostTable(
                                                        "spectest",
                                                        "table_2",
                                                        Collections.emptyMap())))
                                .build();
                assertEquals(2, result.tableCount());
            }

            @Test
            void addMemory() {
                final HostImports result =
                        HostImports.builder()
                                .addTable(
                                        new HostTable("spectest", "table", Collections.emptyMap()))
                                .addTable(
                                        new HostTable(
                                                "spectest", "table_2", Collections.emptyMap()))
                                .build();
                assertEquals(2, result.tableCount());
            }
        }

        @Nested
        class Index {

            @Test
            void withIndex() {
                final HostImports result =
                        HostImports.builder()
                                .withIndex(
                                        Arrays.asList(
                                                new HostFunction(null, "", "", null, null),
                                                new HostGlobal(
                                                        "spectest",
                                                        "global_i32",
                                                        new GlobalInstance(Value.i32(666)))))
                                .build();
                assertEquals(2, result.index().length);
            }

            @Test
            void addIndex() {
                final HostImports result =
                        HostImports.builder()
                                .addIndex(new HostFunction(null, "", "", null, null))
                                .build();
                assertEquals(1, result.index().length);
            }
        }

        @Nested
        class DSL {

            @Test
            void withIndex() {
                var moduleName = "module";
                var fieldName = "filed";
                HostImports.builder()
                        .withNewImport(moduleName, fieldName)
                        .withGlobal(Value.i32(1))
                        .withNewImport(moduleName, fieldName)
                        .withGlobal(MutabilityType.Var, Value.i32(1))
                        .withNewImport(moduleName, fieldName)
                        .withMutableGlobal(Value.i32(1))
                        .withNewImport(moduleName, fieldName)
                        .withGlobal(Value.i32(1))
                        .withNewImport(moduleName, fieldName)
                        .withMemory()
                        .withNewImport(moduleName, fieldName)
                        .withMemory(new MemoryLimits(1))
                        .withNewImport(moduleName, fieldName)
                        .withMemory(1)
                        .withNewImport(moduleName, fieldName)
                        .withMemory(1, 2)
                        .withNewImport(moduleName, fieldName)
                        .withTable()
                        .withNewImport(moduleName, fieldName)
                        .withTable(ValueType.ExternRef)
                        .withNewImport(moduleName, fieldName)
                        .withTable(new Limits(1))
                        .withNewImport(moduleName, fieldName)
                        .withTable(1)
                        .withNewImport(moduleName, fieldName)
                        .withTable(1, 2)
                        .withNewImport(moduleName, fieldName)
                        .withProcedure(() -> System.out.println("hello world"))
                        .withNewImport(moduleName, fieldName)
                        .withProcedure((Instance inst) -> () -> System.out.println("hello world"))
                        .withNewImport(moduleName, fieldName)
                        .withSupplier(() -> 1)
                        .withNewImport(moduleName, fieldName)
                        .withSupplier((Instance inst) -> () -> 1)
                        .withNewImport(moduleName, fieldName)
                        .withSupplier(() -> 2L)
                        // this doesn't compile now as expected, since we do not handle Double just
                        // yet
                        // .withSupplier(() -> { return 0.0 })
                        .withNewImport(moduleName, fieldName)
                        .withConsumer(
                                (int i) -> {
                                    var x = i + 1;
                                })
                        .withNewImport(moduleName, fieldName)
                        .withConsumer(
                                (long l) -> {
                                    var x = l + 2L;
                                })
                        // this doesn't compile now as expected, since we do not handle Double just
                        // yet
                        // .withConsumer((double d) -> { var x = d + 2; })
                        .withNewImport(moduleName, fieldName)
                        .withFunction((int i) -> i + 1)
                        .withNewImport(moduleName, fieldName)
                        .withFunction((long l) -> l + 2L)
                        // this doesn't compile now as expected, since we do not handle Double just
                        // yet
                        // .withFunction((double d) -> d + 2.0)
                        .withNewImport(moduleName, fieldName)
                        .withFunction(
                                (Instance inst) ->
                                        (Value[] args) -> {
                                            return new Value[] {};
                                        })
                        .build();
            }
        }
    }
}
