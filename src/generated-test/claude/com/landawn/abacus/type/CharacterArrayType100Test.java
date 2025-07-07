package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class CharacterArrayType100Test extends TestBase {

    private CharacterArrayType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (CharacterArrayType) createType("Character[]");
        writer = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_Empty() {
        Character[] array = new Character[0];
        String result = type.stringOf(array);
        assertEquals("[]", result);
    }

    @Test
    public void testStringOf_SingleElement() {
        Character[] array = new Character[] { 'a' };
        String result = type.stringOf(array);
        assertEquals("['a']", result);
    }

    @Test
    public void testStringOf_MultipleElements() {
        Character[] array = new Character[] { 'x', 'y', null, 'z' };
        String result = type.stringOf(array);
        assertEquals("['x', 'y', null, 'z']", result);
    }

    @Test
    public void testStringOf_SpecialCharacters() {
        Character[] array = new Character[] { '\'', '"', '\n', '\t' };
        String result = type.stringOf(array);
        assertEquals("[''', '\"', '\n', '\t']", result);
    }

    @Test
    public void testValueOf_Null() {
        Character[] result = type.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        Character[] result = type.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyArray() {
        Character[] result = type.valueOf("[]");
        Assertions.assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOf_SingleElement_Quoted() {
        Character[] result = type.valueOf("['a']");
        Assertions.assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(Character.valueOf('a'), result[0]);
    }

    @Test
    public void testValueOf_SingleElement_Unquoted() {
        Character[] result = type.valueOf("[a]");
        Assertions.assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(Character.valueOf('a'), result[0]);
    }

    @Test
    public void testValueOf_MultipleElements_Quoted() {
        Character[] result = type.valueOf("['x', 'y', null, 'z']");
        Assertions.assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(Character.valueOf('x'), result[0]);
        assertEquals(Character.valueOf('y'), result[1]);
        Assertions.assertNull(result[2]);
        assertEquals(Character.valueOf('z'), result[3]);
    }

    @Test
    public void testValueOf_MultipleElements_Unquoted() {
        Character[] result = type.valueOf("[a, b, null, c]");
        Assertions.assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(Character.valueOf('a'), result[0]);
        assertEquals(Character.valueOf('b'), result[1]);
        Assertions.assertNull(result[2]);
        assertEquals(Character.valueOf('c'), result[3]);
    }

    @Test
    public void testValueOf_MixedQuotes() {
        Character[] result = type.valueOf("[\"a\", \"b\", null, \"c\"]");
        Assertions.assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(Character.valueOf('a'), result[0]);
        assertEquals(Character.valueOf('b'), result[1]);
        Assertions.assertNull(result[2]);
        assertEquals(Character.valueOf('c'), result[3]);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringWriter sw = new StringWriter();
        Character[] array = new Character[0];
        type.appendTo(sw, array);
        assertEquals("[]", sw.toString());
    }

    @Test
    public void testAppendTo_SingleElement() throws IOException {
        StringWriter sw = new StringWriter();
        Character[] array = new Character[] { 'X' };
        type.appendTo(sw, array);
        assertEquals("[X]", sw.toString());
    }

    @Test
    public void testAppendTo_MultipleElements() throws IOException {
        StringWriter sw = new StringWriter();
        Character[] array = new Character[] { '1', null, '3' };
        type.appendTo(sw, array);
        assertEquals("[1, null, 3]", sw.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        type.writeCharacter(mockWriter, null, null);
        verify(mockWriter).write("null".toCharArray());
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Character[] array = new Character[0];
        type.writeCharacter(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_NoQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Character[] array = new Character[] { 'a', null, 'b' };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn((char) 0);

        type.writeCharacter(mockWriter, array, config);

        verify(mockWriter).write('[');
        verify(mockWriter).writeCharacter('a');
        verify(mockWriter, times(2)).write(", ");
        verify(mockWriter).write("null".toCharArray());
        verify(mockWriter).writeCharacter('b');
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_WithSingleQuote() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Character[] array = new Character[] { 'x' };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(mockWriter, array, config);

        verify(mockWriter).write('[');
        verify(mockWriter, times(2)).write('\'');
        verify(mockWriter).writeCharacter('x');
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_EscapeSingleQuote() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Character[] array = new Character[] { '\'' };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(mockWriter, array, config);

        verify(mockWriter).write('[');
        verify(mockWriter, times(2)).write('\'');
        verify(mockWriter).write('\\');
        verify(mockWriter).writeCharacter('\'');
        verify(mockWriter).write(']');
    }

    @Test
    public void testToString_Null() {
        String result = type.toString(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testToString_Empty() {
        Character[] array = new Character[0];
        String result = type.toString(array);
        assertEquals("[]", result);
    }

    @Test
    public void testToString_Elements() {
        Character[] array = new Character[] { 'A', 'B', null, 'D' };
        String result = type.toString(array);
        assertEquals("[A, B, null, D]", result);
    }
}
