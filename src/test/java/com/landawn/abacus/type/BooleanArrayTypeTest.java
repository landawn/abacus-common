package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class BooleanArrayTypeTest extends TestBase {

    private final BooleanArrayType type = new BooleanArrayType();

    @Test
    public void test_stringOf() {
        Boolean[] arr = { true, false, null, true };
        String result = type.stringOf(arr);
        assertEquals("[true, false, null, true]", result);

        assertNull(type.stringOf(null));
        assertEquals("[]", type.stringOf(new Boolean[0]));
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
    public void test_valueOf_String() {
        Boolean[] result = type.valueOf("[true, false, null]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertNull(result[2]);

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
        assertNotNull(type.valueOf("[]"));
        assertEquals(0, type.valueOf("[]").length);
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
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Boolean[] arr = { true, false, null };
        type.appendTo(sw, arr);
        assertEquals("[true, false, null]", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, new Boolean[0]);
        assertEquals("[]", sw.toString());
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
    public void test_serializeTo() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        Boolean[] arr = { true, false };
        type.serializeTo(writer, arr, config);
        verify(writer, atLeast(2)).write(anyChar());

        reset(writer);
        type.serializeTo(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        type.serializeTo(mockWriter, null, null);
        verify(mockWriter).write("null".toCharArray());
    }

    @Test
    public void testSerializeTo_Empty() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Boolean[] array = new Boolean[0];
        type.serializeTo(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
    }

    @Test
    public void testSerializeTo_SingleElement() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Boolean[] array = new Boolean[] { true };
        type.serializeTo(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write("true".toCharArray());
        verify(mockWriter).write(']');
    }

    @Test
    public void testSerializeTo_MultipleElements() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Boolean[] array = new Boolean[] { false, null, true };
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.serializeTo(mockWriter, array, config);

        verify(mockWriter).write('[');
        verify(mockWriter).write("false".toCharArray());
        verify(mockWriter, times(2)).write(", ");
        verify(mockWriter).write("null".toCharArray());
        verify(mockWriter).write("true".toCharArray());
        verify(mockWriter).write(']');
    }

    @Test
    public void testSerializeTo_NullElementHonorsWriteNullBooleanAsFalse() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.isWriteNullBooleanAsFalse()).thenReturn(true);

        type.serializeTo(mockWriter, new Boolean[] { null }, config);

        verify(mockWriter).write(FALSE_CHAR_ARRAY);
    }

    @Test
    public void test_clazz() {
        assertEquals(Boolean[].class, type.javaType());
    }

    @Test
    public void test_name() {
        assertEquals("Boolean[]", type.name());
    }

    @Test
    public void test_isObjectArray() {
        assertTrue(type.isObjectArray());
    }

}
