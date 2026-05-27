package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.wasm.WasmModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ComponentModel Embedded WIT API")
class EmbeddedWitApiTest {

    @Nested
    @DisplayName("Load Overloads")
    class LoadOverloadTests {
        @Test
        @DisplayName("provides embedded WIT load overloads")
        void providesEmbeddedWitLoadOverloads() throws Exception {
            assertNotNull(ComponentModel.class.getMethod("load", WasmModule.class));
            assertNotNull(ComponentModel.class.getMethod("load", WasmModule.class, String.class));
            assertNotNull(
                    ComponentModel.class.getMethod(
                            "load", WasmModule.class, HostFunctionProvider.class));
            assertNotNull(
                    ComponentModel.class.getMethod(
                            "load", WasmModule.class, String.class, HostFunctionProvider.class));
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandlingTests {
        @Test
        @DisplayName("ComponentModelException preserves its message")
        void componentModelExceptionPreservesItsMessage() {
            ComponentModelException exception =
                    assertThrows(
                            ComponentModelException.class,
                            () -> {
                                throw new ComponentModelException("Test error");
                            });

            assertEquals("Test error", exception.getMessage());
        }
    }
}
