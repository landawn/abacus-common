package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

public class FloatArrayType100Test extends TestBase {

    private FloatArrayType floatArrayType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        floatArrayType = (FloatArrayType) createType("Float[]");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(floatArrayType.stringOf(null));

        // Test with empty array
        Float[] emptyArray = new Float[0];
        assertEquals("[]", floatArrayType.stringOf(emptyArray));

        // Test with single element
        Float[] singleElement = { 1.5f };
        String result = floatArrayType.stringOf(singleElement);
        assertTrue(result.contains("1.5"));

        // Test with multiple elements
        Float[] multipleElements = { 1.5f, 2.5f, 3.5f };
        result = floatArrayType.stringOf(multipleElements);
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("2.5"));
        assertTrue(result.contains("3.5"));

        // Test with null element
        Float[] withNull = { 1.5f, null, 3.5f };
        result = floatArrayType.stringOf(withNull);
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("3.5"));
    }

    @Test
    public void testValueOf() {
        // Test with null
        assertNull(floatArrayType.valueOf(null));

        // Test with empty string
        Float[] result = floatArrayType.valueOf("");
        assertNull(result);

        // Test with empty array string
        result = floatArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);

        // Test with single element
        result = floatArrayType.valueOf("[1.5]");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1.5f, result[0]);

        // Test with multiple elements
        result = floatArrayType.valueOf("[1.5, 2.5, 3.5]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1.5f, result[0]);
        assertEquals(2.5f, result[1]);
        assertEquals(3.5f, result[2]);

        // Test with null element
        result = floatArrayType.valueOf("[1.5, null, 3.5]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1.5f, result[0]);
        assertNull(result[1]);
        assertEquals(3.5f, result[2]);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        // Test with null
        floatArrayType.appendTo(writer, null);
        assertEquals("null", writer.toString());

        // Test with empty array
        writer = new StringWriter();
        floatArrayType.appendTo(writer, new Float[0]);
        assertEquals("[]", writer.toString());

        // Test with elements
        writer = new StringWriter();
        Float[] array = { 1.5f, null, 3.5f };
        floatArrayType.appendTo(writer, array);
        String result = writer.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("3.5"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        // Test with null
        floatArrayType.writeCharacter(characterWriter, null, null);

        // Test with empty array
        floatArrayType.writeCharacter(characterWriter, new Float[0], null);

        // Test with elements
        Float[] array = { 1.5f, null, 3.5f };
        floatArrayType.writeCharacter(characterWriter, array, null);
    }
}
