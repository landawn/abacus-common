package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BaseEncodedType100Test extends TestBase {

    private Type<byte[]> base64Type;

    @BeforeEach
    public void setUp() {
        base64Type = createType("Base64Encoded");
    }

    @Test
    public void testClazz() {
        assertEquals(byte[].class, base64Type.clazz());
    }

    @Test
    public void testStringOf() {
        assertEquals("", base64Type.stringOf(null));

        byte[] empty = new byte[0];
        String emptyEncoded = base64Type.stringOf(empty);
        assertNotNull(emptyEncoded);
        assertEquals("", emptyEncoded);

        byte[] data = "Hello World".getBytes();
        String encoded = base64Type.stringOf(data);
        assertNotNull(encoded);
        assertEquals("SGVsbG8gV29ybGQ=", encoded);

        byte[] binary = { 0, 1, 2, 3, 4, 5 };
        String binaryEncoded = base64Type.stringOf(binary);
        assertNotNull(binaryEncoded);
        assertEquals("AAECAwQF", binaryEncoded);
    }

    @Test
    public void testValueOf() {
        assertArrayEquals(new byte[0], base64Type.valueOf((String) null));

        byte[] empty = base64Type.valueOf("");
        assertNotNull(empty);
        assertEquals(0, empty.length);

        byte[] decoded = base64Type.valueOf("SGVsbG8gV29ybGQ=");
        assertNotNull(decoded);
        assertArrayEquals("Hello World".getBytes(), decoded);

        byte[] binary = base64Type.valueOf("AAECAwQF");
        assertNotNull(binary);
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5 }, binary);

        // Test invalid Base64
        assertThrows(IllegalArgumentException.class, () -> base64Type.valueOf("!@#$%"));
    }

    @Test
    public void testRoundTrip() {
        // Test various data patterns
        byte[][] testData = { "".getBytes(), "a".getBytes(), "ab".getBytes(), "abc".getBytes(), "Hello, World!".getBytes(), { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { -128, -1, 0, 1, 127 }, new byte[100] // Large array
        };

        for (byte[] original : testData) {
            String encoded = base64Type.stringOf(original);
            byte[] decoded = base64Type.valueOf(encoded);
            assertArrayEquals(original, decoded);
        }
    }
}
