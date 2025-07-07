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

public class IntegerArrayType100Test extends TestBase {

    private IntegerArrayType integerArrayType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        integerArrayType = (IntegerArrayType) createType("Integer[]");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        assertNull(integerArrayType.stringOf(null));
    }

    @Test
    public void testStringOf_EmptyArray() {
        Integer[] arr = new Integer[0];
        assertEquals("[]", integerArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_SingleElement() {
        Integer[] arr = { 42 };
        assertEquals("[42]", integerArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_MultipleElements() {
        Integer[] arr = { 1, 2, 3 };
        assertEquals("[1, 2, 3]", integerArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_WithNullElements() {
        Integer[] arr = { 1, null, 3 };
        assertEquals("[1, null, 3]", integerArrayType.stringOf(arr));
    }

    @Test
    public void testStringOf_AllNullElements() {
        Integer[] arr = { null, null, null };
        assertEquals("[null, null, null]", integerArrayType.stringOf(arr));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(integerArrayType.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        Integer[] result = integerArrayType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_EmptyArray() {
        Integer[] result = integerArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOf_SingleElement() {
        Integer[] result = integerArrayType.valueOf("[42]");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(42, result[0]);
    }

    @Test
    public void testValueOf_MultipleElements() {
        Integer[] result = integerArrayType.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
    }

    @Test
    public void testValueOf_WithNullElements() {
        Integer[] result = integerArrayType.valueOf("[1, null, 3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertNull(result[1]);
        assertEquals(3, result[2]);
    }

    @Test
    public void testValueOf_WithSpaces() {
        Integer[] result = integerArrayType.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
    }

    @Test
    public void testValueOf_NegativeNumbers() {
        Integer[] result = integerArrayType.valueOf("[-1,-2,-3]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(-1, result[0]);
        assertEquals(-2, result[1]);
        assertEquals(-3, result[2]);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        integerArrayType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_EmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        Integer[] arr = new Integer[0];
        integerArrayType.appendTo(sb, arr);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendTo_SingleElement() throws IOException {
        StringBuilder sb = new StringBuilder();
        Integer[] arr = { 42 };
        integerArrayType.appendTo(sb, arr);
        assertEquals("[42]", sb.toString());
    }

    @Test
    public void testAppendTo_MultipleElements() throws IOException {
        StringBuilder sb = new StringBuilder();
        Integer[] arr = { 1, 2, 3 };
        integerArrayType.appendTo(sb, arr);
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testAppendTo_WithNullElements() throws IOException {
        StringBuilder sb = new StringBuilder();
        Integer[] arr = { 1, null, 3 };
        integerArrayType.appendTo(sb, arr);
        assertEquals("[1, null, 3]", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        integerArrayType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_EmptyArray() throws IOException {
        Integer[] arr = new Integer[0];
        integerArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_SingleElement() throws IOException {
        Integer[] arr = { 42 };
        integerArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).writeInt(42);
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_MultipleElements() throws IOException {
        Integer[] arr = { 1, 2, 3 };
        integerArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).writeInt(1);
        verify(characterWriter, times(2)).write(", ");
        verify(characterWriter).writeInt(2);
        verify(characterWriter).writeInt(3);
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_WithNullElements() throws IOException {
        Integer[] arr = { 1, null, 3 };
        integerArrayType.writeCharacter(characterWriter, arr, null);
        verify(characterWriter).write('[');
        verify(characterWriter).writeInt(1);
        verify(characterWriter, times(2)).write(", ");
        verify(characterWriter).write(any(char[].class)); // for null
        verify(characterWriter).writeInt(3);
        verify(characterWriter).write(']');
    }

    @Test
    public void testWriteCharacter_WithConfig() throws IOException {
        Integer[] arr = { 1, 2, 3 };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        integerArrayType.writeCharacter(characterWriter, arr, config);
        verify(characterWriter).write('[');
        verify(characterWriter).writeInt(1);
        verify(characterWriter, times(2)).write(", ");
        verify(characterWriter).writeInt(2);
        verify(characterWriter).writeInt(3);
        verify(characterWriter).write(']');
    }
}
