package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ShortList;

@Tag("new-test")
public class PrimitiveShortListType100Test extends TestBase {

    private PrimitiveShortListType type;
    private Type<ShortList> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(ShortList.class);
        type = (PrimitiveShortListType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(ShortList.class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyList() {
        ShortList list = ShortList.of(new short[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        ShortList list = ShortList.of(new short[] { 1, 2, 3 });
        assertEquals("[1, 2, 3]", type.stringOf(list));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
    }

    @Test
    public void testValueOfEmptyString() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void testValueOfEmptyArray() {
        ShortList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        ShortList result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 2, result.get(1));
        assertEquals((short) 3, result.get(2));
    }

    @Test
    public void testAppendToNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        ShortList list = ShortList.of(new short[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        ShortList list = ShortList.of(new short[] { 1, 2, 3 });
        type.appendTo(sb, list);
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ShortList list = ShortList.of(new short[0]);
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ShortList list = ShortList.of(new short[] { 1, 2 });
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write((short) 1);
        verify(writer).write(", ");
        verify(writer).write((short) 2);
        verify(writer).write(']');
    }
}
