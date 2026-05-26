package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.component.types.PrimitiveType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for Phase 3: Binary deserialization of Component Model metadata.
 *
 * <p>These tests verify that we can deserialize component metadata from binary format
 * and produce correct ComponentDefinition objects.
 *
 * <p>MVP Scope: Support basic function exports/imports with primitive types and strings.
 */
@DisplayName("Phase 3: Binary Deserialization")
public class Phase3BinaryDeserializationTest {

    /**
     * Create a minimal valid component binary with:
     * - package name: "test"
     * - interface name: "component"
     * - 0 types
     * - 1 export: add(i32, i32) -> i32
     * - 0 imports
     */
    private byte[] createMinimalComponentBinary() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(256);

        // Write package name: "test"
        writeString(buf, "test");

        // Write interface name: "component"
        writeString(buf, "component");

        // Write type count: 0
        writeULeb128(buf, 0);

        // Write export count: 1
        writeULeb128(buf, 1);

        // Write export: add(i32, i32) -> i32
        writeString(buf, "add");
        writeULeb128(buf, 2); // param count

        // First parameter: a: i32
        writeString(buf, "a");
        buf.put((byte) 0x7F); // i32

        // Second parameter: b: i32
        writeString(buf, "b");
        buf.put((byte) 0x7F); // i32

        // Return type count: 1
        writeULeb128(buf, 1);
        buf.put((byte) 0x7F); // i32 return type

        // Write import count: 0
        writeULeb128(buf, 0);

        // Trim buffer
        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    private void writeString(ByteBuffer buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeULeb128(buf, bytes.length);
        buf.put(bytes);
    }

    private void writeULeb128(ByteBuffer buf, long value) {
        while (true) {
            byte b = (byte) (value & 0x7F);
            value >>= 7;
            if (value != 0) {
                b |= 0x80;
            }
            buf.put(b);
            if (value == 0) break;
        }
    }

    @Test
    @DisplayName("Deserialize minimal component binary with one export")
    public void testDeserializeMinimalComponent() throws Exception {
        byte[] binary = createMinimalComponentBinary();

        ComponentDefinition def = ComponentExtractor.deserializeComponentBinary(binary);

        assertNotNull(def);
        assertEquals("test", def.packageName());
        assertEquals("component", def.interfaceName());
        assertEquals(1, def.exports().size());
        assertEquals(0, def.imports().size());

        ComponentDefinition.FunctionSignature add = def.exports().get(0);
        assertEquals("add", add.name());
        assertEquals(2, add.parameters().size());
        assertEquals(1, add.returns().size());

        // Check parameter types
        assertEquals("a", add.parameters().get(0).name);
        assertEquals(PrimitiveType.I32, add.parameters().get(0).type);

        assertEquals("b", add.parameters().get(1).name);
        assertEquals(PrimitiveType.I32, add.parameters().get(1).type);

        // Check return type
        assertEquals(PrimitiveType.I32, add.returns().get(0));

        System.out.println("✅ Minimal component deserialization works!");
    }

    @Test
    @DisplayName("BinaryComponentReader reads LEB128 correctly")
    public void testBinaryComponentReaderLeb128() throws Exception {
        // Create buffer with: 0x7F (127), 0x80 0x01 (128)
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put((byte) 0x7F);
        buf.put((byte) 0x80);
        buf.put((byte) 0x01);
        buf.flip();

        BinaryComponentReader reader = new BinaryComponentReader(buf.array());

        long val1 = reader.readUnsigned();
        assertEquals(127, val1);

        long val2 = reader.readUnsigned();
        assertEquals(128, val2);

        System.out.println("✅ LEB128 reading works!");
    }

    @Test
    @DisplayName("BinaryComponentReader reads strings correctly")
    public void testBinaryComponentReaderString() throws Exception {
        String testStr = "hello";
        ByteBuffer buf = ByteBuffer.allocate(20);

        // Write string with length prefix
        byte[] strBytes = testStr.getBytes(StandardCharsets.UTF_8);
        buf.put((byte) strBytes.length);
        buf.put(strBytes);
        buf.flip();

        BinaryComponentReader reader = new BinaryComponentReader(buf.array());
        String result = reader.readString();

        assertEquals(testStr, result);

        System.out.println("✅ String reading works!");
    }

    @Test
    @DisplayName("Empty component binary throws error")
    public void testEmptyComponentBinaryThrows() {
        byte[] emptyBinary = new byte[0];

        assertThrows(
                ComponentModelException.class,
                () -> {
                    ComponentExtractor.deserializeComponentBinary(emptyBinary);
                });

        System.out.println("✅ Empty binary error handling works!");
    }

    @Test
    @DisplayName("Malformed binary throws error")
    public void testMalformedBinaryThrows() {
        // Create invalid binary (just junk data)
        byte[] malformed = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        // Should not crash, should throw ComponentModelException
        assertThrows(
                ComponentModelException.class,
                () -> {
                    ComponentExtractor.deserializeComponentBinary(malformed);
                });

        System.out.println("✅ Malformed binary error handling works!");
    }
}
