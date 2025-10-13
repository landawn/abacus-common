package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ShortArrayType100Test extends TestBase {

    private ShortArrayType shortArrayType;

    @BeforeEach
    public void setUp() {
        shortArrayType = (ShortArrayType) createType("Short[]");
    }

    @Test
    public void testStringOf() {
        Short[] array = { 1, null, 3 };
        String result = shortArrayType.stringOf(array);
        assertEquals("[1, null, 3]", result);

        assertEquals("[]", shortArrayType.stringOf(new Short[0]));

        assertNull(shortArrayType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        Short[] result = shortArrayType.valueOf("[1, null, 3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(Short.valueOf((short) 1), result[0]);
        assertNull(result[1]);
        assertEquals(Short.valueOf((short) 3), result[2]);

        result = shortArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);

        assertNull(shortArrayType.valueOf(null));
        assertNull(shortArrayType.valueOf(""));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        Short[] array = { 1, null, 3 };
        shortArrayType.appendTo(writer, array);
        assertEquals("[1, null, 3]", writer.toString());

        writer = new StringWriter();
        shortArrayType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        Short[] array = { 1, null, 3 };
        shortArrayType.writeCharacter(writer, array, config);

        shortArrayType.writeCharacter(writer, null, config);
    }
}
