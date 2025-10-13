package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class PrimitiveBooleanListType100Test extends TestBase {

    private PrimitiveBooleanListType booleanListType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        booleanListType = (PrimitiveBooleanListType) createType("BooleanList");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(BooleanList.class, booleanListType.clazz());
    }

    @Test
    public void testGetElementType() {
        assertNotNull(booleanListType.getElementType());
        assertEquals("boolean", booleanListType.getElementType().name());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(booleanListType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmptyList() {
        BooleanList emptyList = BooleanList.of(new boolean[0]);
        assertEquals("[]", booleanListType.stringOf(emptyList));
    }

    @Test
    public void testStringOfWithSingleElement() {
        BooleanList list = BooleanList.of(true);
        assertEquals("[true]", booleanListType.stringOf(list));
    }

    @Test
    public void testStringOfWithMultipleElements() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals("[true, false, true, false]", booleanListType.stringOf(list));
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(booleanListType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertNull(booleanListType.valueOf(""));
    }

    @Test
    public void testValueOfWithEmptyArrayString() {
        BooleanList result = booleanListType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfWithSingleElement() {
        BooleanList result = booleanListType.valueOf("[false]");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0));
    }

    @Test
    public void testValueOfWithMultipleElements() {
        BooleanList result = booleanListType.valueOf("[true, false, true]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
        assertTrue(result.get(2));
    }

    @Test
    public void testValueOfWithVariousFormats() {
        BooleanList result = booleanListType.valueOf("[TRUE, FALSE, 1, 0, yes, no]");
        assertNotNull(result);
        assertEquals(6, result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
        assertTrue(result.get(2));
        assertFalse(result.get(3));
        assertFalse(result.get(4));
        assertFalse(result.get(5));
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        booleanListType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        BooleanList emptyList = BooleanList.of(new boolean[0]);
        booleanListType.appendTo(sb, emptyList);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToWithList() throws IOException {
        StringBuilder sb = new StringBuilder();
        BooleanList list = BooleanList.of(true, false, true);
        booleanListType.appendTo(sb, list);
        assertEquals("[true, false, true]", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        booleanListType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmptyList() throws IOException {
        BooleanList emptyList = BooleanList.of(new boolean[0]);
        booleanListType.writeCharacter(writer, emptyList, config);
        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterWithList() throws IOException {
        BooleanList list = BooleanList.of(true, false);
        booleanListType.writeCharacter(writer, list, config);
        verify(writer, atLeastOnce()).write(anyChar());
        verify(writer, atLeastOnce()).write(any(char[].class));
    }

    @Test
    public void testIsPrimitiveList() {
        assertTrue(booleanListType.isPrimitiveList());
    }

    @Test
    public void testIsList() {
        assertFalse(booleanListType.isList());
    }

    @Test
    public void testIsCollection() {
        assertFalse(booleanListType.isCollection());
    }

    @Test
    public void testCreateFromArray() {
        boolean[] array = { true, false, true, false, true };
        BooleanList list = BooleanList.of(array);

        String stringRep = booleanListType.stringOf(list);
        assertEquals("[true, false, true, false, true]", stringRep);

        BooleanList parsed = booleanListType.valueOf(stringRep);
        assertNotNull(parsed);
        assertEquals(list.size(), parsed.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), parsed.get(i));
        }
    }
}
