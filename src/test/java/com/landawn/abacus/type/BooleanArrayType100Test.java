package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BooleanArrayType100Test extends TestBase {

    private BooleanArrayType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BooleanArrayType) createType("Boolean[]");
        writer = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_Empty() {
        Boolean[] array = new Boolean[0];
        String result = type.stringOf(array);
        assertEquals("[]", result);
    }

    @Test
    public void testStringOf_SingleElement() {
        Boolean[] array = new Boolean[] { true };
        String result = type.stringOf(array);
        assertEquals("[true]", result);
    }

    @Test
    public void testStringOf_MultipleElements() {
        Boolean[] array = new Boolean[] { true, false, null };
        String result = type.stringOf(array);
        assertEquals("[true, false, null]", result);
    }

    @Test
    public void testValueOf_Null() {
        Boolean[] result = type.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        Boolean[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_EmptyArray() {
        Boolean[] result = type.valueOf("[]");
        Assertions.assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOf_SingleElement() {
        Boolean[] result = type.valueOf("[true]");
        Assertions.assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(Boolean.TRUE, result[0]);
    }

    @Test
    public void testValueOf_MultipleElements() {
        Boolean[] result = type.valueOf("[true, false, null]");
        Assertions.assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(Boolean.TRUE, result[0]);
        assertEquals(Boolean.FALSE, result[1]);
        Assertions.assertNull(result[2]);
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
        Boolean[] array = new Boolean[0];
        type.appendTo(sw, array);
        assertEquals("[]", sw.toString());
    }

    @Test
    public void testAppendTo_SingleElement() throws IOException {
        StringWriter sw = new StringWriter();
        Boolean[] array = new Boolean[] { false };
        type.appendTo(sw, array);
        assertEquals("[false]", sw.toString());
    }

    @Test
    public void testAppendTo_MultipleElements() throws IOException {
        StringWriter sw = new StringWriter();
        Boolean[] array = new Boolean[] { true, false, null };
        type.appendTo(sw, array);
        assertEquals("[true, false, null]", sw.toString());
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
        Boolean[] array = new Boolean[0];
        type.writeCharacter(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_SingleElement() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Boolean[] array = new Boolean[] { true };
        type.writeCharacter(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write("true".toCharArray());
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_MultipleElements() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Boolean[] array = new Boolean[] { false, null, true };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(mockWriter, array, config);

        verify(mockWriter).write('[');
        verify(mockWriter).write("false".toCharArray());
        verify(mockWriter, times(2)).write(", ");
        verify(mockWriter).write("null".toCharArray());
        verify(mockWriter).write("true".toCharArray());
        verify(mockWriter).write(']');
    }
}
