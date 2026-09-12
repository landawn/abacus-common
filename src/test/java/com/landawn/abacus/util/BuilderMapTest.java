package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.MapBuilder;

public class BuilderMapTest extends BuilderTestSupport {
    @Test
    public void testMapBuilderRemoveAllWithLiveMapViews() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "b");
        map.put("b", "a");
        final var mapBuilder = Builder.of(map);
        assertSame(mapBuilder, mapBuilder.removeAll(map.keySet()));
        assertTrue(map.isEmpty());

        map.put("a", "b");
        map.put("b", "a");
        assertSame(mapBuilder, mapBuilder.removeAll(map.values()));
        assertTrue(map.isEmpty());

        final TreeMap<String, Integer> sorted = new TreeMap<>();
        sorted.put("a", 1);
        sorted.put("b", 2);
        sorted.put("c", 3);
        Builder.of(sorted).removeAll(sorted.headMap("c").keySet());
        assertEquals(Map.of("c", 3), sorted);
    }

    @Test
    public void testMapBuilder_put_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.put("a", 1));
    }

    @Test
    public void testMapBuilder_put() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.put("a", 1).put("b", 2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testMapBuilder_putAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("b", 2);
        toAdd.put("c", 3);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testMapBuilder_putIfAbsent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("a", 2);
        builder.putIfAbsent("b", 2);
        assertEquals(1, builder.val().get("a"));
        assertEquals(2, builder.val().get("b"));
    }

    @Test
    public void testMapBuilder_putIfAbsentSupplier() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        Supplier<Integer> supplier = () -> 5;
        builder.putIfAbsentBySupplier("a", supplier);
        assertEquals(5, builder.val().get("a"));
    }

    @Test
    public void testMapBuilder_remove() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.remove("a");
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMapBuilder_removeAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.removeAll(Arrays.asList("a", "b"));
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMapBuilderPut() {
        Map<String, Integer> map = new HashMap<>();
        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);

        builder.put("one", 1).put("two", 2);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testMapBuilderPutAll() {
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("three", 3);
        toAdd.put("four", 4);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putAll(toAdd);

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Integer.valueOf(3), map.get("three"));
    }

    @Test
    public void testMapBuilderPutIfAbsent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("one", 10);
        builder.putIfAbsent("two", 2);

        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testMapBuilderPutIfAbsentWithSupplier() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsentBySupplier("one", () -> 10);
        builder.putIfAbsentBySupplier("two", () -> 2);

        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testMapBuilderRemove() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.remove("one");

        Assertions.assertEquals(1, map.size());
        Assertions.assertFalse(map.containsKey("one"));
    }

    @Test
    public void testMapBuilderRemoveAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.removeAll(Arrays.asList("one", "two"));

        Assertions.assertEquals(1, map.size());
        Assertions.assertTrue(map.containsKey("three"));
    }

    @Test
    public void testMapBuilder_putIfAbsent_existingKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("key", 99);
        assertEquals(1, (int) builder.val().get("key"));
    }

    @Test
    public void testMapBuilder_putIfAbsentSupplier_existingKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsentBySupplier("key", () -> 99);
        assertEquals(1, (int) builder.val().get("key"));
    }

    @Test
    public void testMapBuilder_of() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        assertNotNull(builder);
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMapBuilder() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);

        builder.put("b", 2);
        assertEquals(2, map.size());

        Map<String, Integer> anotherMap = new HashMap<>();
        anotherMap.put("c", 3);
        builder.putAll(anotherMap);
        assertEquals(3, map.size());

        builder.putIfAbsent("a", 10);
        assertEquals(1, (int) map.get("a"));

        builder.putIfAbsent("d", 4);
        assertEquals(4, (int) map.get("d"));

        builder.putIfAbsentBySupplier("e", () -> 5);
        assertEquals(5, (int) map.get("e"));

        builder.remove("a");
        assertFalse(map.containsKey("a"));

        builder.removeAll(Arrays.asList("b", "c"));
        assertFalse(map.isEmpty());
    }

    @Test
    public void testMapBuilderPutIfAbsentEdgeCases() {
        Map<String, String> map = new HashMap<>();
        map.put("null-value", null);

        MapBuilder<String, String, Map<String, String>> builder = Builder.of(map);

        builder.putIfAbsent("null-value", "replacement");
        assertEquals("replacement", map.get("null-value"));

        map.put("null-value2", null);
        builder.putIfAbsentBySupplier("null-value2", () -> "generated");
        assertEquals("generated", map.get("null-value2"));
    }

    @Test
    public void testMapBuilder_putAll_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.putAll(new HashMap<>()));
    }

    @Test
    public void testMapBuilder_putIfAbsent_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.putIfAbsent("a", 1));
    }

    @Test
    public void testMapBuilder_putIfAbsentSupplier_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.putIfAbsentBySupplier("a", () -> 1));
    }

    @Test
    public void testMapBuilder_remove_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testMapBuilder_removeAll_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    @Test
    public void testMapBuilder_putAll_nullMap() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        b.putAll(null);
        assertEquals(0, map.size());
    }

    @Test
    public void testMapBuilder_removeAll_nullCollection() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        b.removeAll(null);
        assertEquals(1, map.size());
    }

    @Test
    public void testMapBuilder_removeAll_emptyCollection() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        b.removeAll(Collections.emptyList());
        assertEquals(1, map.size());
    }
}
