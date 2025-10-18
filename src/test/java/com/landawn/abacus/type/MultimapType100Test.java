package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SetMultimap;

@Tag("new-test")
public class MultimapType100Test extends TestBase {

    private MultimapType<String, Integer, List<Integer>, ListMultimap<String, Integer>> listMultimapType;
    private MultimapType<String, Integer, Set<Integer>, SetMultimap<String, Integer>> setMultimapType;

    @BeforeEach
    public void setUp() {
        listMultimapType = (MultimapType<String, Integer, List<Integer>, ListMultimap<String, Integer>>) createType("ListMultimap<String, Integer>");
        setMultimapType = (MultimapType<String, Integer, Set<Integer>, SetMultimap<String, Integer>>) createType("SetMultimap<String, Integer>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = listMultimapType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Multimap"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = listMultimapType.clazz();
        Assertions.assertNotNull(clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = listMultimapType.getParameterTypes();
        Assertions.assertNotNull(paramTypes);
        Assertions.assertTrue(paramTypes.length >= 2);
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = listMultimapType.isGenericType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testIsSerializable() {
        boolean isSerializable = listMultimapType.isSerializable();
        Assertions.assertTrue(isSerializable);
    }

    @Test
    public void testStringOfNull() {
        String result = listMultimapType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key2", 3);

        String result = listMultimapType.stringOf(multimap);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key1"));
        Assertions.assertTrue(result.contains("key2"));
    }

    @Test
    public void testValueOfNull() {
        ListMultimap<String, Integer> result = listMultimapType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        ListMultimap<String, Integer> result = listMultimapType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidJsonForListMultimap() {
        ListMultimap<String, Integer> result = listMultimapType.valueOf("{\"key1\":[1,2],\"key2\":[3]}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.containsKey("key1"));
        Assertions.assertTrue(result.containsKey("key2"));
        assertEquals(2, result.get("key1").size());
        assertEquals(1, result.get("key2").size());
    }

    @Test
    public void testValueOfValidJsonForSetMultimap() {
        SetMultimap<String, Integer> result = setMultimapType.valueOf("{\"key1\":[1,2],\"key2\":[3]}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.containsKey("key1"));
        Assertions.assertTrue(result.containsKey("key2"));
        assertEquals(2, result.get("key1").size());
        assertEquals(1, result.get("key2").size());
    }

}
