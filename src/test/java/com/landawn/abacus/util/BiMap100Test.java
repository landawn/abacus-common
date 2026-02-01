package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BiMap100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        BiMap<String, Integer> biMap = new BiMap<>(100);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = new BiMap<>(100, 0.8f);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithMapTypes() {
        BiMap<String, Integer> biMap = new BiMap<>(HashMap.class, HashMap.class);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testPut() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertNull(biMap.put("one", 1));
        assertEquals(1, biMap.get("one"));
        assertEquals("one", biMap.getByValue(1));
        assertEquals(1, biMap.size());
    }

    @Test
    public void testPutWithDuplicateValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        assertThrows(IllegalArgumentException.class, () -> biMap.put("two", 1));
    }

    @Test
    public void testPutWithNullKey() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> biMap.put(null, 1));
    }

    @Test
    public void testPutWithNullValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> biMap.put("one", null));
    }

    @Test
    public void testForcePut() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        assertNull(biMap.forcePut("two", 1));
        assertNull(biMap.get("one"));
        assertEquals(1, biMap.get("two"));
        assertEquals("two", biMap.getByValue(1));
        assertEquals(1, biMap.size());
    }

    @Test
    public void testForcePutReplacingKey() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);
        assertEquals(1, biMap.forcePut("one", 2));
        assertNull(biMap.get("two"));
        assertEquals(2, biMap.get("one"));
        assertEquals(1, biMap.size());
    }

    @Test
    public void testGet() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertNull(biMap.get("one"));
        biMap.put("one", 1);
        assertEquals(1, biMap.get("one"));
    }

    @Test
    public void testGetByValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertNull(biMap.getByValue(1));
        biMap.put("one", 1);
        assertEquals("one", biMap.getByValue(1));
    }

    @Test
    public void testGetByValueOrDefault() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertEquals("default", biMap.getByValueOrDefault(1, "default"));
        biMap.put("one", 1);
        assertEquals("one", biMap.getByValueOrDefault(1, "default"));
    }

    @Test
    public void testRemove() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        assertEquals(1, biMap.remove("one"));
        assertNull(biMap.get("one"));
        assertNull(biMap.getByValue(1));
        assertEquals(0, biMap.size());
    }

    @Test
    public void testRemoveByValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        assertEquals("one", biMap.removeByValue(1));
        assertNull(biMap.get("one"));
        assertNull(biMap.getByValue(1));
        assertEquals(0, biMap.size());
    }

    @Test
    public void testContainsKey() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertFalse(biMap.containsKey("one"));
        biMap.put("one", 1);
        assertTrue(biMap.containsKey("one"));
    }

    @Test
    public void testContainsValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertFalse(biMap.containsValue(1));
        biMap.put("one", 1);
        assertTrue(biMap.containsValue(1));
    }

    @Test
    public void testPutAll() {
        BiMap<String, Integer> biMap = new BiMap<>();
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        biMap.putAll(map);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testClear() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);
        assertEquals(2, biMap.size());

        biMap.clear();
        assertEquals(0, biMap.size());
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testKeySet() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        ImmutableSet<String> keys = biMap.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
    }

    @Test
    public void testValues() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        ImmutableSet<Integer> values = biMap.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEntrySet() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        ImmutableSet<Map.Entry<String, Integer>> entries = biMap.entrySet();
        assertEquals(2, entries.size());
    }

    @Test
    public void testInversed() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        BiMap<Integer, String> inverse = biMap.inverted();
        assertEquals(2, inverse.size());
        assertEquals("one", inverse.get(1));
        assertEquals("two", inverse.get(2));
        assertEquals(1, inverse.getByValue("one"));
        assertEquals(2, inverse.getByValue("two"));

        biMap.put("three", 3);
        assertEquals("three", inverse.get(3));
    }

    @Test
    public void testInversedTwice() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);

        BiMap<Integer, String> inverse = biMap.inverted();
        BiMap<String, Integer> inverseInverse = inverse.inverted();

        assertSame(biMap, inverseInverse);
    }

    @Test
    public void testCopy() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        BiMap<String, Integer> copy = biMap.copy();
        assertEquals(biMap.size(), copy.size());
        assertEquals(biMap.get("one"), copy.get("one"));
        assertEquals(biMap.get("two"), copy.get("two"));

        copy.put("three", 3);
        assertFalse(biMap.containsKey("three"));
    }

    @Test
    public void testOf() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
    }

    @Test
    public void testOfMultiple() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testOfManyPairs() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10);
        assertEquals(10, biMap.size());
        assertEquals(10, biMap.get("ten"));
    }

    @Test
    public void testCopyOf() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        BiMap<String, Integer> biMap = BiMap.copyOf(map);
        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
    }

    @Test
    public void testBuilder() {
        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().put("one", 1).put("two", 2).put("three", 3).build();

        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testBuilderWithMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        BiMap<String, Integer> biMap = BiMap.builder(map).put("three", 3).build();

        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testBuilderForcePut() {
        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().put("one", 1).forcePut("two", 1).build();

        assertEquals(1, biMap.size());
        assertNull(biMap.get("one"));
        assertEquals(1, biMap.get("two"));
    }

    @Test
    public void testBuilderPutAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().putAll(map).build();

        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
    }

    @Test
    public void testEqualsAndHashCode() {
        BiMap<String, Integer> biMap1 = new BiMap<>();
        biMap1.put("one", 1);
        biMap1.put("two", 2);

        BiMap<String, Integer> biMap2 = new BiMap<>();
        biMap2.put("one", 1);
        biMap2.put("two", 2);

        assertEquals(biMap1, biMap2);
        assertEquals(biMap1.hashCode(), biMap2.hashCode());

        biMap2.put("three", 3);
        assertNotEquals(biMap1, biMap2);
    }

    @Test
    public void testToString() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);

        String str = biMap.toString();
        assertTrue(str.contains("one"));
        assertTrue(str.contains("1"));
    }

    @Test
    public void testIsEmpty() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertTrue(biMap.isEmpty());

        biMap.put("one", 1);
        assertFalse(biMap.isEmpty());

        biMap.remove("one");
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testSize() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertEquals(0, biMap.size());

        biMap.put("one", 1);
        assertEquals(1, biMap.size());

        biMap.put("two", 2);
        assertEquals(2, biMap.size());

        biMap.remove("one");
        assertEquals(1, biMap.size());
    }
}
