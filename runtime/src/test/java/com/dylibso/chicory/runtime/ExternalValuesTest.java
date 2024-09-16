package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.wasm.types.Value;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExternalValuesTest {

    @Nested
    class Builder {
        @Test
        void empty() {
            final ExternalValues result = ExternalValues.builder().build();
            assertEquals(0, result.functionCount());
            assertEquals(0, result.globalCount());
            assertEquals(0, result.memoryCount());
            assertEquals(0, result.tableCount());
        }

        @Nested
        class Function {

            @Test
            void withFunctions() {
                final ExternalValues result =
                        ExternalValues.builder()
                                .withFunctions(
                                        Arrays.asList(
                                                new HostFunction(null, "module_1", "", null, null),
                                                new HostFunction(null, "module_2", "", null, null)))
                                .build();
                assertEquals(2, result.functionCount());
            }

            @Test
            void addFunction() {
                final ExternalValues result =
                        ExternalValues.builder()
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
                final ExternalValues result =
                        ExternalValues.builder()
                                .withGlobals(
                                        Arrays.asList(
                                                new ExternalGlobal(
                                                        "spectest",
                                                        "global_i32",
                                                        new GlobalInstance(Value.i32(666))),
                                                new ExternalGlobal(
                                                        "spectest",
                                                        "global_i64",
                                                        new GlobalInstance(Value.i64(666)))))
                                .build();
                assertEquals(2, result.globalCount());
            }

            @Test
            void addGlobal() {
                final ExternalValues result =
                        ExternalValues.builder()
                                .addGlobal(
                                        new ExternalGlobal(
                                                "spectest",
                                                "global_i32",
                                                new GlobalInstance(Value.i32(666))))
                                .addGlobal(
                                        new ExternalGlobal(
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
                final ExternalValues result =
                        ExternalValues.builder()
                                .withMemories(
                                        Arrays.asList(
                                                new ExternalMemory("spectest", "memory", null),
                                                new ExternalMemory("spectest", "memory_2", null)))
                                .build();
                assertEquals(2, result.memoryCount());
            }

            @Test
            void addMemory() {
                final ExternalValues result =
                        ExternalValues.builder()
                                .addMemory(new ExternalMemory("spectest", "memory", null))
                                .addMemory(new ExternalMemory("spectest", "memory_2", null))
                                .build();
                assertEquals(2, result.memoryCount());
            }
        }

        @Nested
        class Table {

            @Test
            void withTables() {
                final ExternalValues result =
                        ExternalValues.builder()
                                .withTables(
                                        Arrays.asList(
                                                new ExternalTable(
                                                        "spectest",
                                                        "table",
                                                        Collections.emptyMap()),
                                                new ExternalTable(
                                                        "spectest",
                                                        "table_2",
                                                        Collections.emptyMap())))
                                .build();
                assertEquals(2, result.tableCount());
            }

            @Test
            void addMemory() {
                final ExternalValues result =
                        ExternalValues.builder()
                                .addTable(
                                        new ExternalTable(
                                                "spectest", "table", Collections.emptyMap()))
                                .addTable(
                                        new ExternalTable(
                                                "spectest", "table_2", Collections.emptyMap()))
                                .build();
                assertEquals(2, result.tableCount());
            }
        }
    }
}
