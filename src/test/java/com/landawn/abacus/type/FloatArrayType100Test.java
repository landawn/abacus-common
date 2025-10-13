package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
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
        assertNull(floatArrayType.stringOf(null));

        Float[] emptyArray = new Float[0];
        assertEquals("[]", floatArrayType.stringOf(emptyArray));

        Float[] singleElement = { 1.5f };
        String result = floatArrayType.stringOf(singleElement);
        assertTrue(result.contains("1.5"));

        Float[] multipleElements = { 1.5f, 2.5f, 3.5f };
        result = floatArrayType.stringOf(multipleElements);
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("2.5"));
        assertTrue(result.contains("3.5"));

        Float[] withNull = { 1.5f, null, 3.5f };
        result = floatArrayType.stringOf(withNull);
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("3.5"));
    }

    @Test
    public void testValueOf() {
        assertNull(floatArrayType.valueOf(null));

        Float[] result = floatArrayType.valueOf("");
        assertNull(result);

        result = floatArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);

        result = floatArrayType.valueOf("[1.5]");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1.5f, result[0]);

        result = floatArrayType.valueOf("[1.5, 2.5, 3.5]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1.5f, result[0]);
        assertEquals(2.5f, result[1]);
        assertEquals(3.5f, result[2]);

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

        floatArrayType.appendTo(writer, null);
        assertEquals("null", writer.toString());

        writer = new StringWriter();
        floatArrayType.appendTo(writer, new Float[0]);
        assertEquals("[]", writer.toString());

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
        floatArrayType.writeCharacter(characterWriter, null, null);

        floatArrayType.writeCharacter(characterWriter, new Float[0], null);

        Float[] array = { 1.5f, null, 3.5f };
        floatArrayType.writeCharacter(characterWriter, array, null);
    }
}
