package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class DoubleArrayType100Test extends TestBase {

    private DoubleArrayType doubleArrayType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        doubleArrayType = (DoubleArrayType) createType("Double[]");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf() {
        assertNull(doubleArrayType.stringOf(null));

        Double[] emptyArray = new Double[0];
        assertEquals("[]", doubleArrayType.stringOf(emptyArray));

        Double[] singleElement = { 1.5 };
        String result = doubleArrayType.stringOf(singleElement);
        assertTrue(result.contains("1.5"));

        Double[] multipleElements = { 1.5, 2.5, 3.5 };
        result = doubleArrayType.stringOf(multipleElements);
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("2.5"));
        assertTrue(result.contains("3.5"));

        Double[] withNull = { 1.5, null, 3.5 };
        result = doubleArrayType.stringOf(withNull);
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("3.5"));
    }

    @Test
    public void testValueOf() {
        assertNull(doubleArrayType.valueOf(null));

        Double[] result = doubleArrayType.valueOf("");
        assertNull(result);

        result = doubleArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);

        result = doubleArrayType.valueOf("[1.5]");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1.5, result[0]);

        result = doubleArrayType.valueOf("[1.5, 2.5, 3.5]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1.5, result[0]);
        assertEquals(2.5, result[1]);
        assertEquals(3.5, result[2]);

        result = doubleArrayType.valueOf("[1.5, null, 3.5]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1.5, result[0]);
        assertNull(result[1]);
        assertEquals(3.5, result[2]);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        doubleArrayType.appendTo(writer, null);
        assertEquals("null", writer.toString());

        writer = new StringWriter();
        doubleArrayType.appendTo(writer, new Double[0]);
        assertEquals("[]", writer.toString());

        writer = new StringWriter();
        Double[] array = { 1.5, null, 3.5 };
        doubleArrayType.appendTo(writer, array);
        String result = writer.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("3.5"));
    }

    @Test
    public void testWriteCharacter() throws IOException {

        doubleArrayType.writeCharacter(characterWriter, null, null);

        doubleArrayType.writeCharacter(characterWriter, new Double[0], null);

        Double[] array = { 1.5, null, 3.5 };
        doubleArrayType.writeCharacter(characterWriter, array, null);
    }
}
