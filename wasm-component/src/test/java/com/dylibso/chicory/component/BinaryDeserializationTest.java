package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.component.types.PrimitiveType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Binary Deserialization")
class BinaryDeserializationTest {

    @Nested
    @DisplayName("Component Metadata")
    class ComponentMetadataTests {
        @Test
        @DisplayName("deserializes a minimal component definition")
        void deserializesAMinimalComponentDefinition() throws Exception {
            ComponentDefinition definition =
                    ComponentExtractor.deserializeComponentBinary(createMinimalComponentBinary());

            assertNotNull(definition);
            assertEquals("test", definition.packageName());
            assertEquals("component", definition.interfaceName());
            assertEquals(1, definition.exports().size());
            assertEquals(0, definition.imports().size());

            ComponentDefinition.FunctionSignature add = definition.exports().get(0);
            assertEquals("add", add.name());
            assertEquals(2, add.parameters().size());
            assertEquals(1, add.returns().size());
            assertEquals("a", add.parameters().get(0).name);
            assertEquals(PrimitiveType.I32, add.parameters().get(0).type);
            assertEquals("b", add.parameters().get(1).name);
            assertEquals(PrimitiveType.I32, add.parameters().get(1).type);
            assertEquals(PrimitiveType.I32, add.returns().get(0));
        }
    }

    @Nested
    @DisplayName("Binary Reader")
    class BinaryReaderTests {
        @Test
        @DisplayName("reads unsigned LEB128 values")
        void readsUnsignedLeb128Values() {
            ByteBuffer buffer = ByteBuffer.allocate(10);
            buffer.put((byte) 0x7F);
            buffer.put((byte) 0x80);
            buffer.put((byte) 0x01);

            BinaryComponentReader reader = new BinaryComponentReader(buffer.array());
            assertEquals(127, reader.readUnsigned());
            assertEquals(128, reader.readUnsigned());
        }

        @Test
        @DisplayName("reads length-prefixed strings")
        void readsLengthPrefixedStrings() {
            String value = "hello";
            ByteBuffer buffer = ByteBuffer.allocate(20);
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            buffer.put((byte) bytes.length);
            buffer.put(bytes);

            BinaryComponentReader reader = new BinaryComponentReader(buffer.array());
            assertEquals(value, reader.readString());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        @Test
        @DisplayName("rejects empty binaries")
        void rejectsEmptyBinaries() {
            assertThrows(
                    ComponentModelException.class,
                    () -> ComponentExtractor.deserializeComponentBinary(new byte[0]));
        }

        @Test
        @DisplayName("rejects malformed binaries")
        void rejectsMalformedBinaries() {
            byte[] malformed = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            assertThrows(
                    ComponentModelException.class,
                    () -> ComponentExtractor.deserializeComponentBinary(malformed));
        }
    }

    private static byte[] createMinimalComponentBinary() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        writeString(buffer, "test");
        writeString(buffer, "component");
        writeUnsignedLeb128(buffer, 0);
        writeUnsignedLeb128(buffer, 1);
        writeString(buffer, "add");
        writeUnsignedLeb128(buffer, 2);
        writeString(buffer, "a");
        buffer.put((byte) 0x7F);
        writeString(buffer, "b");
        buffer.put((byte) 0x7F);
        writeUnsignedLeb128(buffer, 1);
        buffer.put((byte) 0x7F);
        writeUnsignedLeb128(buffer, 0);

        byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);
        return result;
    }

    private static void writeString(ByteBuffer buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeUnsignedLeb128(buffer, bytes.length);
        buffer.put(bytes);
    }

    private static void writeUnsignedLeb128(ByteBuffer buffer, long value) {
        while (true) {
            byte current = (byte) (value & 0x7F);
            value >>= 7;
            if (value != 0) {
                current |= 0x80;
            }
            buffer.put(current);
            if (value == 0) {
                return;
            }
        }
    }
}
