package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class PrimitiveCharListType100Test extends TestBase {

    private PrimitiveCharListType type;
    private Type<CharList> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(CharList.class);
        type = (PrimitiveCharListType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(CharList.class, type.clazz());
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
        CharList list = CharList.of(new char[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        CharList list = CharList.of(new char[] { 'a', 'b', 'c' });
        assertEquals("['a', 'b', 'c']", type.stringOf(list));
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
        CharList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        CharList result = type.valueOf("['a', 'b', 'c']");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals('a', result.get(0));
        assertEquals('b', result.get(1));
        assertEquals('c', result.get(2));
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
        CharList list = CharList.of(new char[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        CharList list = CharList.of(new char[] { 'a', 'b', 'c' });
        type.appendTo(sb, list);
        assertEquals("[a, b, c]", sb.toString());
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
        CharList list = CharList.of(new char[0]);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        CharList list = CharList.of(new char[] { 'a', 'b' });
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).writeCharacter('a');
        verify(writer).write(", ");
        verify(writer).writeCharacter('b');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        CharList list = CharList.of(new char[] { 'a' });
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer, times(2)).write('\'');
        verify(writer).writeCharacter('a');
        verify(writer).write(']');
    }
}
