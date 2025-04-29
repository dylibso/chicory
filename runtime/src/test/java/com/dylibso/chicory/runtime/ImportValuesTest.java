package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImportValuesTest {

    @Nested
    class Builder {
        @Test
        void empty() {
            final ImportValues result = ImportValues.builder().build();
            assertEquals(0, result.functionCount());
            assertEquals(0, result.globalCount());
            assertEquals(0, result.memoryCount());
            assertEquals(0, result.tableCount());
        }

        @Nested
        class Function {

            @Test
            void withFunctions() {
                final ImportValues result =
                        ImportValues.builder()
                                .withFunctions(
                                        Arrays.asList(
                                                new HostFunction(
                                                        "module_1", "", FunctionType.empty(), null),
                                                new HostFunction(
                                                        "module_2",
                                                        "",
                                                        FunctionType.empty(),
                                                        null)))
                                .build();
                assertEquals(2, result.functionCount());
            }

            @Test
            void addFunction() {
                final ImportValues result =
                        ImportValues.builder()
                                .addFunction(
                                        new HostFunction(
                                                "module_1", "", FunctionType.empty(), null))
                                .addFunction(
                                        new HostFunction(
                                                "module_2", "", FunctionType.empty(), null))
                                .build();
                assertEquals(2, result.functionCount());
            }
        }

        @Nested
        class Global {

            @Test
            void withGlobals() {
                final ImportValues result =
                        ImportValues.builder()
                                .withGlobals(
                                        Arrays.asList(
                                                new ImportGlobal(
                                                        "spectest",
                                                        "global_i32",
                                                        new GlobalInstance(Value.i32(666))),
                                                new ImportGlobal(
                                                        "spectest",
                                                        "global_i64",
                                                        new GlobalInstance(Value.i64(666)))))
                                .build();
                assertEquals(2, result.globalCount());
            }

            @Test
            void addGlobal() {
                final ImportValues result =
                        ImportValues.builder()
                                .addGlobal(
                                        new ImportGlobal(
                                                "spectest",
                                                "global_i32",
                                                new GlobalInstance(Value.i32(666))))
                                .addGlobal(
                                        new ImportGlobal(
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
                final ImportValues result =
                        ImportValues.builder()
                                .withMemories(
                                        Arrays.asList(
                                                new ImportMemory("spectest", "memory", null),
                                                new ImportMemory("spectest", "memory_2", null)))
                                .build();
                assertEquals(2, result.memoryCount());
            }

            @Test
            void addMemory() {
                final ImportValues result =
                        ImportValues.builder()
                                .addMemory(new ImportMemory("spectest", "memory", null))
                                .addMemory(new ImportMemory("spectest", "memory_2", null))
                                .build();
                assertEquals(2, result.memoryCount());
            }
        }

        @Nested
        class Table {

            @Test
            void withTables() {
                final ImportValues result =
                        ImportValues.builder()
                                .withTables(
                                        Arrays.asList(
                                                new ImportTable(
                                                        "spectest",
                                                        "table",
                                                        Collections.emptyMap()),
                                                new ImportTable(
                                                        "spectest",
                                                        "table_2",
                                                        Collections.emptyMap())))
                                .build();
                assertEquals(2, result.tableCount());
            }

            @Test
            void addMemory() {
                final ImportValues result =
                        ImportValues.builder()
                                .addTable(
                                        new ImportTable(
                                                "spectest", "table", Collections.emptyMap()))
                                .addTable(
                                        new ImportTable(
                                                "spectest", "table_2", Collections.emptyMap()))
                                .build();
                assertEquals(2, result.tableCount());
            }
        }
    }
}
