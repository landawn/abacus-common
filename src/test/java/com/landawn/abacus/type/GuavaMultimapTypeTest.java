package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import com.landawn.abacus.TestBase;

public class GuavaMultimapTypeTest extends TestBase {

    private GuavaMultimapType<String, Integer, Multimap<String, Integer>> multimapType;

    @BeforeEach
    public void setUp() {
        multimapType = (GuavaMultimapType<String, Integer, Multimap<String, Integer>>) createType("com.google.common.collect.Multimap<String, Integer>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = multimapType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Multimap"));
        assertTrue(declaringName.contains("String"));
        assertTrue(declaringName.contains("Integer"));
    }

    @Test
    public void testClazz() {
        assertEquals(Multimap.class, multimapType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = multimapType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(multimapType.isParameterizedType());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(multimapType.isSerializable());
    }

    @Test
    public void testStringOf() {
        assertNull(multimapType.stringOf(null));
    }

    @Test
    public void testStringOf_NonNull() {
        ArrayListMultimap<String, Integer> mm = ArrayListMultimap.create();
        mm.put("x", 1);
        mm.put("x", 2);
        mm.put("y", 3);
        String json = multimapType.stringOf((Multimap<String, Integer>) mm);
        assertNotNull(json);
        assertTrue(json.contains("x"));
    }

    @Test
    public void testValueOf() {
        assertNull(multimapType.valueOf(null));
        assertNull(multimapType.valueOf(""));
        assertTrue(multimapType.valueOf("null").isEmpty());
    }

    @Test
    public void testValueOf_WithData() {
        Multimap<String, Integer> result = multimapType.valueOf("{\"a\":[1,2],\"b\":[3]}");
        assertNotNull(result);
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testValueOf_ArrayListMultimap() {
        GuavaMultimapType<String, Integer, ArrayListMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, ArrayListMultimap<String, Integer>>) createType(
                "com.google.common.collect.ArrayListMultimap<String, Integer>");
        ArrayListMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertNotNull(result);
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testValueOf_HashMultimap() {
        GuavaMultimapType<String, Integer, HashMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, HashMultimap<String, Integer>>) createType(
                "com.google.common.collect.HashMultimap<String, Integer>");
        HashMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertNotNull(result);
        assertTrue(result.containsKey("a"));
    }

    @Test
    public void testValueOf_LinkedHashMultimap() {
        GuavaMultimapType<String, Integer, LinkedHashMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, LinkedHashMultimap<String, Integer>>) createType(
                "com.google.common.collect.LinkedHashMultimap<String, Integer>");
        LinkedHashMultimap<String, Integer> result = type.valueOf("{\"a\":[1]}");
        assertNotNull(result);
        assertTrue(result.containsKey("a"));
    }

    @Test
    public void testValueOf_LinkedListMultimap() {
        GuavaMultimapType<String, Integer, LinkedListMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, LinkedListMultimap<String, Integer>>) createType(
                "com.google.common.collect.LinkedListMultimap<String, Integer>");
        LinkedListMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertNotNull(result);
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testValueOf_TreeMultimap() {
        GuavaMultimapType<String, Integer, TreeMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, TreeMultimap<String, Integer>>) createType(
                "com.google.common.collect.TreeMultimap<String, Integer>");
        TreeMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertNotNull(result);
        assertTrue(result.containsKey("a"));
    }

    @Test
    public void testValueOf_ListMultimap() {
        GuavaMultimapType<String, Integer, ListMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, ListMultimap<String, Integer>>) createType(
                "com.google.common.collect.ListMultimap<String, Integer>");
        ListMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertNotNull(result);
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testValueOf_SetMultimap() {
        GuavaMultimapType<String, Integer, SetMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, SetMultimap<String, Integer>>) createType(
                "com.google.common.collect.SetMultimap<String, Integer>");
        SetMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertNotNull(result);
        assertTrue(result.containsKey("a"));
    }

    @Test
    public void testValueOf_ImmutableListMultimap() {
        GuavaMultimapType<String, Integer, ImmutableListMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, ImmutableListMultimap<String, Integer>>) createType(
                "com.google.common.collect.ImmutableListMultimap<String, Integer>");
        ImmutableListMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");

        assertNotNull(result);
        assertEquals(2, result.get("a").size());
        assertTrue(result instanceof ImmutableListMultimap);
    }

    @Test
    public void testValueOf_ImmutableSetMultimap() {
        GuavaMultimapType<String, Integer, ImmutableSetMultimap<String, Integer>> type = (GuavaMultimapType<String, Integer, ImmutableSetMultimap<String, Integer>>) createType(
                "com.google.common.collect.ImmutableSetMultimap<String, Integer>");
        ImmutableSetMultimap<String, Integer> result = type.valueOf("{\"a\":[1,2,2]}");

        assertNotNull(result);
        assertEquals(2, result.get("a").size());
        assertTrue(result instanceof ImmutableSetMultimap);
    }

    @Test
    public void testGetTypeName() {
        String typeName = GuavaMultimapType.getTypeName(Multimap.class, "String", "Integer", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multimap"));

        typeName = GuavaMultimapType.getTypeName(Multimap.class, "String", "Integer", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multimap"));
    }
}
