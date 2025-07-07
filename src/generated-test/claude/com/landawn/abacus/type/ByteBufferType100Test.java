package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

public class ByteBufferType100Test extends TestBase {

    private ByteBufferType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ByteBufferType) createType("ByteBuffer");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<ByteBuffer> result = type.clazz();
        assertEquals(ByteBuffer.class, result);
    }

    @Test
    public void testIsByteBuffer() {
        boolean result = type.isByteBuffer();
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_EmptyBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        String result = type.stringOf(buffer);
        Assertions.assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testStringOf_BufferWithData() {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);

        String result = type.stringOf(buffer);
        Assertions.assertNotNull(result);
        // Base64 encoding of [1, 2, 3]
        assertEquals("AQID", result);
    }

    @Test
    public void testValueOf_Null() {
        ByteBuffer result = type.valueOf((String) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        ByteBuffer result = type.valueOf("");
        Assertions.assertNotNull(result);
        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, result.capacity());
    }

    @Test
    public void testValueOf_ValidBase64() {
        String base64 = "AQID"; // Base64 encoding of [1, 2, 3]
        ByteBuffer result = type.valueOf(base64);

        Assertions.assertNotNull(result);
        assertEquals(3, result.position());
        assertEquals(3, result.limit());
        assertEquals(3, result.capacity());

        // Verify content
        result.position(0);
        assertEquals((byte) 1, result.get());
        assertEquals((byte) 2, result.get());
        assertEquals((byte) 3, result.get());
    }

    @Test
    public void testByteArrayOf() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put((byte) 10);
        buffer.put((byte) 20);
        buffer.put((byte) 30);
        buffer.put((byte) 40);

        byte[] result = ByteBufferType.byteArrayOf(buffer);

        Assertions.assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals((byte) 10, result[0]);
        assertEquals((byte) 20, result[1]);
        assertEquals((byte) 30, result[2]);
        assertEquals((byte) 40, result[3]);

        // Verify position is restored
        assertEquals(4, buffer.position());
    }

    @Test
    public void testByteArrayOf_PartialBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);
        // Position is now 3, capacity is 5

        byte[] result = ByteBufferType.byteArrayOf(buffer);

        Assertions.assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals((byte) 1, result[0]);
        assertEquals((byte) 2, result[1]);
        assertEquals((byte) 3, result[2]);

        // Verify position is restored
        assertEquals(3, buffer.position());
    }

    @Test
    public void testValueOf_ByteArray() {
        byte[] bytes = new byte[] { (byte) -1, (byte) 0, (byte) 127 };
        ByteBuffer result = ByteBufferType.valueOf(bytes);

        Assertions.assertNotNull(result);
        assertEquals(3, result.position());
        assertEquals(3, result.limit());
        assertEquals(3, result.capacity());

        // Verify content
        result.position(0);
        assertEquals((byte) -1, result.get());
        assertEquals((byte) 0, result.get());
        assertEquals((byte) 127, result.get());
    }

    @Test
    public void testValueOf_EmptyByteArray() {
        byte[] bytes = new byte[0];
        ByteBuffer result = ByteBufferType.valueOf(bytes);

        Assertions.assertNotNull(result);
        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, result.capacity());
    }

    @Test
    public void testRoundTrip() {
        // Create original buffer
        ByteBuffer original = ByteBuffer.allocate(4);
        original.put((byte) 10);
        original.put((byte) 20);
        original.put((byte) 30);
        original.put((byte) 40);

        // Convert to string
        String base64 = type.stringOf(original);
        Assertions.assertNotNull(base64);

        // Convert back to ByteBuffer
        ByteBuffer restored = type.valueOf(base64);
        Assertions.assertNotNull(restored);

        // Verify content matches
        assertEquals(original.position(), restored.position());
        assertEquals(original.limit(), restored.limit());

        original.position(0);
        restored.position(0);
        while (original.hasRemaining()) {
            assertEquals(original.get(), restored.get());
        }
    }

    @Test
    public void testComplexData() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        for (int i = 0; i < 256; i++) {
            buffer.put((byte) i);
        }

        String base64 = type.stringOf(buffer);
        ByteBuffer restored = type.valueOf(base64);

        assertEquals(256, restored.position());
        restored.position(0);
        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, restored.get());
        }
    }
}
