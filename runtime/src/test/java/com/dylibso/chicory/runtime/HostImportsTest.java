package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.wasm.types.Value;
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
    }
}
