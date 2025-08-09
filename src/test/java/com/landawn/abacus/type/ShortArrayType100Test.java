package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class ShortArrayType100Test extends TestBase {

    private ShortArrayType shortArrayType;

    @BeforeEach
    public void setUp() {
        shortArrayType = (ShortArrayType) createType("Short[]");
    }

    @Test
    public void testStringOf() {
        // Test with array containing values
        Short[] array = { 1, null, 3 };
        String result = shortArrayType.stringOf(array);
        assertEquals("[1, null, 3]", result);

        // Test with empty array
        assertEquals("[]", shortArrayType.stringOf(new Short[0]));

        // Test with null
        assertNull(shortArrayType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        // Test parsing array with values
        Short[] result = shortArrayType.valueOf("[1, null, 3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(Short.valueOf((short) 1), result[0]);
        assertNull(result[1]);
        assertEquals(Short.valueOf((short) 3), result[2]);

        // Test empty array
        result = shortArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);

        // Test null/empty string
        assertNull(shortArrayType.valueOf(null));
        assertNull(shortArrayType.valueOf(""));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        // Test with array
        Short[] array = { 1, null, 3 };
        shortArrayType.appendTo(writer, array);
        assertEquals("[1, null, 3]", writer.toString());

        // Test with null
        writer = new StringWriter();
        shortArrayType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        // Test with array
        Short[] array = { 1, null, 3 };
        shortArrayType.writeCharacter(writer, array, config);

        // Test with null
        shortArrayType.writeCharacter(writer, null, config);
    }
}
