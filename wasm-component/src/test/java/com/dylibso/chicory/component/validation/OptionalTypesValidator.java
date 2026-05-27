package com.dylibso.chicory.component.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.types.OptionalType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Optional Types")
class OptionalTypesValidator {

    @Nested
    @DisplayName("Optional Type Metadata")
    class OptionalTypeMetadataTests {
        @Test
        @DisplayName("wraps primitive types")
        void wrapsPrimitiveTypes() {
            assertEquals("option<i32>", new OptionalType(PrimitiveType.I32).displayName());
            assertEquals("option<string>", new OptionalType(PrimitiveType.STRING).displayName());
        }

        @Test
        @DisplayName("supports nested optionals")
        void supportsNestedOptionals() {
            OptionalType nestedOptional = new OptionalType(new OptionalType(PrimitiveType.I64));
            assertEquals("option<option<i64>>", nestedOptional.displayName());
        }

        @Test
        @DisplayName("supports record payloads")
        void supportsRecordPayloads() {
            RecordType personRecord = new RecordType("person");
            assertEquals("option<person>", new OptionalType(personRecord).displayName());
        }
    }

    @Nested
    @DisplayName("Java Optional Usage")
    class JavaOptionalUsageTests {
        @Test
        @DisplayName("tracks present and empty values")
        void tracksPresentAndEmptyValues() {
            Optional<Integer> timeout = Optional.of(5000);
            Optional<String> description = Optional.empty();

            assertTrue(timeout.isPresent());
            assertEquals(5000, timeout.get());
            assertFalse(description.isPresent());
        }

        @Test
        @DisplayName("ifPresent only runs for present values")
        void ifPresentOnlyRunsForPresentValues() {
            StringBuilder present = new StringBuilder();
            Optional.of(3000).ifPresent(timeout -> present.append("Timeout: ").append(timeout));

            StringBuilder absent = new StringBuilder();
            Optional.<String>empty().ifPresent(value -> absent.append(value));

            assertEquals("Timeout: 3000", present.toString());
            assertEquals(0, absent.length());
        }

        @Test
        @DisplayName("orElse returns defaults for empty values")
        void orElseReturnsDefaultsForEmptyValues() {
            assertEquals(3, Optional.<Integer>empty().orElse(3));
            assertEquals("Alice", Optional.of("Alice").orElse("Guest"));
        }

        @Test
        @DisplayName("map transforms present values")
        void mapTransformsPresentValues() {
            Optional<String> duration = Optional.of(5000).map(value -> value + "ms");
            assertTrue(duration.isPresent());
            assertEquals("5000ms", duration.get());
        }

        @Test
        @DisplayName("flatMap composes optional lookups")
        void flatMapComposesOptionalLookups() {
            Optional<String> result =
                    Optional.of(123)
                            .flatMap(id -> id > 100 ? Optional.of("Valid ID") : Optional.empty());

            assertTrue(result.isPresent());
            assertEquals("Valid ID", result.get());
        }

        @Test
        @DisplayName("optional fields can model nullable record properties")
        void optionalFieldsCanModelNullableRecordProperties() {
            UserConfig enabledConfig =
                    new UserConfig(Optional.of(5000), Optional.of("secret"), true);
            UserConfig defaultedConfig =
                    new UserConfig(Optional.empty(), Optional.of("public"), false);

            assertEquals(5000, enabledConfig.timeout().orElse(-1));
            assertEquals("secret", enabledConfig.apiKey().orElse("none"));
            assertTrue(enabledConfig.enabled());
            assertEquals(-1, defaultedConfig.timeout().orElse(-1));
            assertEquals("public", defaultedConfig.apiKey().orElse("none"));
            assertFalse(defaultedConfig.enabled());
        }
    }

    @Nested
    @DisplayName("Code Generation")
    class CodeGenerationTests {
        @Test
        @DisplayName("record fields preserve optional type information")
        void recordFieldsPreserveOptionalTypeInformation() {
            RecordType configRecord = new RecordType("config");
            configRecord.addField("timeout", new OptionalType(PrimitiveType.I32));
            configRecord.addField("name", PrimitiveType.STRING);

            assertEquals("config", configRecord.displayName());
            assertEquals(2, configRecord.fields().size());
            assertEquals("timeout", configRecord.fields().get(0).name);
            assertEquals("option<i32>", configRecord.fields().get(0).type.displayName());
            assertEquals("name", configRecord.fields().get(1).name);
            assertEquals("string", configRecord.fields().get(1).type.displayName());
        }
    }

    private static final class UserConfig {
        private final Optional<Integer> timeout;
        private final Optional<String> apiKey;
        private final boolean enabled;

        private UserConfig(Optional<Integer> timeout, Optional<String> apiKey, boolean enabled) {
            this.timeout = timeout;
            this.apiKey = apiKey;
            this.enabled = enabled;
        }

        private Optional<Integer> timeout() {
            return timeout;
        }

        private Optional<String> apiKey() {
            return apiKey;
        }

        private boolean enabled() {
            return enabled;
        }
    }
}
