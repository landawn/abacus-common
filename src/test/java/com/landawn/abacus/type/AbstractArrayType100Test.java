package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractArrayType100Test extends TestBase {
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
        assertEquals(SerializationType.ARRAY, type.getSerializationType());
    }

    @Test
    public void testArray2Collection_NullArray() {
        Collection<Object> result = type.array2Collection(null, ArrayList.class);
        assertNull(result);
    }

    @Test
    public void testArray2Collection_ArrayList() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.array2Collection(array, ArrayList.class);
        assertNotNull(result);
        assertTrue(result instanceof ArrayList);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_HashSet() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.array2Collection(array, HashSet.class);
        assertNotNull(result);
        assertTrue(result instanceof HashSet);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_LinkedList() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.array2Collection(array, LinkedList.class);
        assertNotNull(result);
        assertTrue(result instanceof LinkedList);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_EmptyArray() {
        Object[] array = new Object[0];
        Collection<Object> result = type.array2Collection(array, ArrayList.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
