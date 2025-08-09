package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class LongArrayType100Test extends TestBase {

    private LongArrayType longArrayType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        longArrayType = (LongArrayType) createType("Long[]");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        assertNull(longArrayType.stringOf(null));
    }

    @Test
    public void testStringOf_EmptyArray() {
        Long[] arr = new Long[0];
        assertEquals("[]", longArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_SingleElement() {
        Long[] arr = { 42L };
        assertEquals("[42]", longArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_MultipleElements() {
        Long[] arr = { 1L, 2L, 3L };
        assertEquals("[1, 2, 3]", longArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_WithNullElements() {
        Long[] arr = { 1L, null, 3L };
        assertEquals("[1, null, 3]", longArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_AllNullElements() {
        Long[] arr = { null, null, null };
        assertEquals("[null, null, null]", longArrayType.stringOf(arr));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(longArrayType.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        Long[] result = longArrayType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_EmptyArray() {
        Long[] result = longArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOf_SingleElement() {
        Long[] result = longArrayType.valueOf("[42]");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(42L, result[0]);
    }

    @Test
    public void testValueOf_MultipleElements() {
        Long[] result = longArrayType.valueOf("[1,2,3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1L, result[0]);
        assertEquals(2L, result[1]);
        assertEquals(3L, result[2]);
    }

    @Test
    public void testValueOf_WithNullElements() {
        Long[] result = longArrayType.valueOf("[1,null,3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1L, result[0]);
        assertNull(result[1]);
        assertEquals(3L, result[2]);
    }

    @Test
    public void testValueOf_WithSpaces() {
        Long[] result = longArrayType.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1L, result[0]);
        assertEquals(2L, result[1]);
        assertEquals(3L, result[2]);
    }

    @Test
    public void testValueOf_NegativeNumbers() {
        Long[] result = longArrayType.valueOf("[-1,-2,-3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(-1L, result[0]);
        assertEquals(-2L, result[1]);
        assertEquals(-3L, result[2]);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        longArrayType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_EmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        Long[] arr = new Long[0];
        longArrayType.appendTo(sb, arr);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendTo_SingleElement() throws IOException {
        StringBuilder sb = new StringBuilder();
        Long[] arr = { 42L };
        longArrayType.appendTo(sb, arr);
        assertEquals("[42]", sb.toString());
    }

    @Test
    public void testAppendTo_MultipleElements() throws IOException {
        StringBuilder sb = new StringBuilder();
        Long[] arr = { 1L, 2L, 3L };
        longArrayType.appendTo(sb, arr);
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testAppendTo_WithNullElements() throws IOException {
        StringBuilder sb = new StringBuilder();
        Long[] arr = { 1L, null, 3L };
        longArrayType.appendTo(sb, arr);
        assertEquals("[1, null, 3]", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        longArrayType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_EmptyArray() throws IOException {
        Long[] arr = new Long[0];
        longArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_SingleElement() throws IOException {
        Long[] arr = { 42L };
        longArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).write(42L);
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_MultipleElements() throws IOException {
        Long[] arr = { 1L, 2L, 3L };
        longArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).write(1L);
        verify(characterWriter, times(2)).write(", ");
        verify(characterWriter).write(2L);
        verify(characterWriter).write(3L);
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_WithNullElements() throws IOException {
        Long[] arr = { 1L, null, 3L };
        longArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).write(1L);
        verify(characterWriter, times(2)).write(", ");
        verify(characterWriter).write(any(char[].class)); // for null
        verify(characterWriter).write(3L);
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_WithConfig() throws IOException {
        Long[] arr = { 1L, 2L, 3L };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        longArrayType.writeCharacter(characterWriter, arr, config);
        verify(characterWriter).write('[');
        verify(characterWriter).write(1L);
        verify(characterWriter, times(2)).write(", ");
        verify(characterWriter).write(2L);
        verify(characterWriter).write(3L);
        verify(characterWriter).write(']');
    }
}
