package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractArrayTypeTest extends TestBase {
    private Type<Object[]> type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = createType(Object[].class);
        writer = createCharacterWriter();
    }

    @Test
    public void testIsArray() {
        assertTrue(type.isArray());
    }

    @Test
    public void testGetSerializationType_Serializable() {
        assertEquals(SerializationType.ARRAY, type.serializationType());
    }

    @Test
    public void testArray2Collection_NullArray() {
        Collection<Object> result = type.arrayToCollection(null, ArrayList.class);
        assertNull(result);
    }

    @Test
    public void testArray2Collection_ArrayList() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.arrayToCollection(array, ArrayList.class);
        assertNotNull(result);
        assertTrue(result instanceof ArrayList);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_HashSet() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.arrayToCollection(array, HashSet.class);
        assertNotNull(result);
        assertTrue(result instanceof HashSet);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_LinkedList() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.arrayToCollection(array, LinkedList.class);
        assertNotNull(result);
        assertTrue(result instanceof LinkedList);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_EmptyArray() {
        Object[] array = new Object[0];
        Collection<Object> result = type.arrayToCollection(array, ArrayList.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 2026-10-05: the shared compact-JSON element normalization behind the array codecs' valueOf(String).
    @Test
    public void jsonElementContent20261005() {
        assertNull(AbstractArrayType.jsonElementContent("null"));
        assertEquals("NULL", AbstractArrayType.jsonElementContent("NULL"));
        assertEquals("null", AbstractArrayType.jsonElementContent("\"null\""));
        assertEquals("1", AbstractArrayType.jsonElementContent("1"));
        assertEquals("1", AbstractArrayType.jsonElementContent("\"1\""));
        assertEquals("-2", AbstractArrayType.jsonElementContent("'-2'"));
        assertEquals("abc", AbstractArrayType.jsonElementContent("\"abc\""));
        assertEquals("1", AbstractArrayType.jsonElementContent("\"\\u0031\""));
        assertEquals("\t1", AbstractArrayType.jsonElementContent("'\\t1'"));
        // decoded exactly once: a double-escaped sequence is the escaped text
        assertEquals("\\u0031", AbstractArrayType.jsonElementContent("\"\\\\u0031\""));
        // a malformed escape is rejected
        assertThrows(IllegalArgumentException.class, () -> AbstractArrayType.jsonElementContent("\"\\u00\""));
        // mismatched or lone quotes are not a quoted element
        assertEquals("\"1'", AbstractArrayType.jsonElementContent("\"1'"));
        assertEquals("\"1", AbstractArrayType.jsonElementContent("\"1"));
        assertEquals("\"", AbstractArrayType.jsonElementContent("\""));
        // an empty quoted element stays quoted for the numeric codecs and is its empty content for the boolean ones
        assertEquals("\"\"", AbstractArrayType.jsonElementContent("\"\""));
        assertEquals("''", AbstractArrayType.jsonElementContent("''"));
        assertEquals("\"\"", AbstractArrayType.jsonElementContent("\"\"", false));
        assertEquals("", AbstractArrayType.jsonElementContent("\"\"", true));
        assertEquals("", AbstractArrayType.jsonElementContent("''", true));
        assertEquals("Y", AbstractArrayType.jsonElementContent("'Y'", true));
        assertEquals("1", AbstractArrayType.jsonElementContent("\"\\u0031\"", true));
        assertNull(AbstractArrayType.jsonElementContent("null", true));
        assertEquals("\"", AbstractArrayType.jsonElementContent("\"", true));
    }
}
