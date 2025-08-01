package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DoubleList;

public class PrimitiveDoubleListType100Test extends TestBase {

    private PrimitiveDoubleListType type;
    private Type<DoubleList> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(DoubleList.class);
        type = (PrimitiveDoubleListType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(DoubleList.class, type.clazz());
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
        DoubleList list = DoubleList.of(new double[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        DoubleList list = DoubleList.of(new double[] { 1.5, 2.7, 3.14 });
        assertEquals("[1.5, 2.7, 3.14]", type.stringOf(list));
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
        DoubleList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        DoubleList result = type.valueOf("[1.5, 2.7, 3.14]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0));
        assertEquals(2.7, result.get(1));
        assertEquals(3.14, result.get(2));
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
        DoubleList list = DoubleList.of(new double[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        DoubleList list = DoubleList.of(new double[] { 1.5, 2.7, 3.14 });
        type.appendTo(sb, list);
        assertEquals("[1.5, 2.7, 3.14]", sb.toString());
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
        DoubleList list = DoubleList.of(new double[0]);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        DoubleList list = DoubleList.of(new double[] { 1.5, 2.7 });
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(1.5);
        verify(writer).write(", ");
        verify(writer).write(2.7);
        verify(writer).write(']');
    }
}
