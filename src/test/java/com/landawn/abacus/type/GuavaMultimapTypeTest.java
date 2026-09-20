package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
        List<Type<?>> paramTypes = multimapType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
        assertEquals(String.class, paramTypes.get(0).javaType());
        assertEquals(Integer.class, paramTypes.get(1).javaType());
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
        String json = multimapType.stringOf(mm);
        assertNotNull(json);
        assertTrue(json.contains("x"));
    }

    @Test
    public void testValueOf() {
        assertNull(multimapType.valueOf(null));
        assertNull(multimapType.valueOf(""));
        // P2-07: an unquoted scalar root is no longer silently read as an empty map; the bean reader has always
        // rejected "null"/"abc" this way, and the multimap types delegate to the same map reader.
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> multimapType.valueOf("null"));
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
        assertEquals(Integer.class, type.parameterTypes().get(1).javaType());
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

    // T8-01: the ordered targets (linked / immutable) were fed from an unordered HashMap intermediate.
    @Test
    public void reviewFixes20260906_orderedTargetsKeepDocumentOrder() {
        final String json = "{\"z\": [1], \"a\": [2], \"m\": [3], \"b\": [4], \"q\": [5]}";
        final List<String> expected = java.util.Arrays.asList("z", "a", "m", "b", "q");

        for (final String typeName : new String[] { "com.google.common.collect.LinkedHashMultimap<String, Integer>",
                "com.google.common.collect.LinkedListMultimap<String, Integer>", "com.google.common.collect.ImmutableListMultimap<String, Integer>",
                "com.google.common.collect.ImmutableSetMultimap<String, Integer>", "com.google.common.collect.ImmutableMultimap<String, Integer>" }) {
            final Type<Multimap<String, Integer>> t = TypeFactory.getType(typeName);
            final Multimap<String, Integer> mm = t.valueOf(json);

            assertEquals(expected, new java.util.ArrayList<>(mm.keySet()), typeName);
            assertEquals(json, t.stringOf(mm), typeName);
            assertEquals(json, t.stringOf(t.valueOf(t.stringOf(mm))), typeName);
        }

        // immutable runtime classes are still produced
        assertTrue(TypeFactory.getType("com.google.common.collect.ImmutableListMultimap<String, Integer>").valueOf(json) instanceof ImmutableListMultimap);
        assertTrue(TypeFactory.getType("com.google.common.collect.ImmutableSetMultimap<String, Integer>").valueOf(json) instanceof ImmutableSetMultimap);
        assertTrue(TypeFactory.getType("com.google.common.collect.ImmutableMultimap<String, Integer>")
                .valueOf(json) instanceof com.google.common.collect.ImmutableMultimap);

        // Set-valued ordered targets keep the array order of the values as well (duplicates collapsed)
        final String values = "{\"k\": [\"z\", \"a\", \"m\", \"b\", \"q\", \"z\"]}";
        final Type<SetMultimap<String, String>> linkedSet = TypeFactory.getType("com.google.common.collect.LinkedHashMultimap<String, String>");
        assertEquals(expected, new java.util.ArrayList<>(linkedSet.valueOf(values).get("k")));
        final Type<SetMultimap<String, String>> immutableSet = TypeFactory.getType("com.google.common.collect.ImmutableSetMultimap<String, String>");
        assertEquals(expected, new java.util.ArrayList<>(immutableSet.valueOf(values).get("k")));

        // Unicode
        final String unicode = "{\"é\": [\"中\"], \"à\": [\"x\"], \"😀\": [\"y\"]}";
        final Type<Multimap<String, String>> tu = TypeFactory.getType("com.google.common.collect.ImmutableListMultimap<String, String>");
        assertEquals(unicode, tu.stringOf(tu.valueOf(unicode)));
    }

    // Negative test: the unordered interface targets keep their runtime classes (no re-routing to Linked* classes).
    @Test
    public void reviewFixes20260906_unorderedTargetsKeepTheirRuntimeClasses() {
        final String json = "{\"z\": [1], \"a\": [2], \"m\": [3]}";

        assertEquals(ArrayListMultimap.class, multimapType.valueOf(json).getClass());
        assertEquals(ArrayListMultimap.class, TypeFactory.getType("com.google.common.collect.ListMultimap<String, Integer>").valueOf(json).getClass());
        assertEquals(HashMultimap.class, TypeFactory.getType("com.google.common.collect.SetMultimap<String, Integer>").valueOf(json).getClass());
        assertEquals(HashMultimap.class, TypeFactory.getType("com.google.common.collect.HashMultimap<String, Integer>").valueOf(json).getClass());
        assertEquals(ArrayListMultimap.class, TypeFactory.getType("com.google.common.collect.ArrayListMultimap<String, Integer>").valueOf(json).getClass());

        final Multimap<String, Integer> sorted = TypeFactory.<Multimap<String, Integer>> getType("com.google.common.collect.TreeMultimap<String, Integer>")
                .valueOf(json);
        assertEquals(TreeMultimap.class, sorted.getClass());
        assertEquals(java.util.Arrays.asList("a", "m", "z"), new java.util.ArrayList<>(sorted.keySet()));

        // same content regardless of order
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList("z", "a", "m")), multimapType.valueOf(json).keySet());
    }

    // T8-05: {"k": null} threw a raw NullPointerException (Collection::size); now the key is dropped like the abacus twins do.
    @Test
    public void reviewFixes20260906_nullValueDropsTheKey() {
        for (final String typeName : new String[] { "com.google.common.collect.Multimap<String, Integer>",
                "com.google.common.collect.ListMultimap<String, Integer>", "com.google.common.collect.SetMultimap<String, Integer>",
                "com.google.common.collect.LinkedHashMultimap<String, Integer>", "com.google.common.collect.ImmutableListMultimap<String, Integer>",
                "com.google.common.collect.ImmutableSetMultimap<String, Integer>", "com.google.common.collect.TreeMultimap<String, Integer>" }) {
            final Type<Multimap<String, Integer>> t = TypeFactory.getType(typeName);

            final Multimap<String, Integer> mm = t.valueOf("{\"a\": null, \"b\": [1], \"c\": []}");
            assertEquals(java.util.Collections.singleton("b"), new java.util.HashSet<>(mm.keySet()), typeName);
            assertEquals(java.util.Arrays.asList(1), new java.util.ArrayList<>(mm.get("b")), typeName);

            assertTrue(t.valueOf("{\"a\": null}").isEmpty(), typeName);
            assertTrue(t.valueOf("{}").isEmpty(), typeName);

            // duplicate key: position of the first occurrence, values of the last one
            final Multimap<String, Integer> dup = t.valueOf("{\"a\": [1], \"b\": [2], \"a\": [3]}");
            assertEquals(java.util.Arrays.asList(3), new java.util.ArrayList<>(dup.get("a")), typeName);

            org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> t.valueOf("{\"a\": [1]"), typeName);
        }

        // a null element is accepted by the mutable targets and rejected by the immutable ones (documented)
        assertEquals(java.util.Arrays.asList((Integer) null), new java.util.ArrayList<>(multimapType.valueOf("{\"a\": [null]}").get("a")));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> TypeFactory.getType("com.google.common.collect.ImmutableListMultimap<String, Integer>").valueOf("{\"a\": [null]}"));
    }
}
