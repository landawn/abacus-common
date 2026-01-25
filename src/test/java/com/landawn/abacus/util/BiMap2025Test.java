package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BiMap2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        BiMap<String, Integer> biMap = new BiMap<>(20);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = new BiMap<>(20, 0.8f);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithMapTypes() {
        BiMap<String, Integer> biMap = new BiMap<>(LinkedHashMap.class, TreeMap.class);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testConstructorWithSuppliers() {
        BiMap<String, Integer> biMap = new BiMap<>(HashMap::new, HashMap::new);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
    }

    @Test
    public void testOf1Entry() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNotNull(biMap);
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals("one", biMap.getByValue(1));
    }

    @Test
    public void testOf2Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertNotNull(biMap);
        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals("one", biMap.getByValue(1));
        assertEquals("two", biMap.getByValue(2));
    }

    @Test
    public void testOf3Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3);
        assertNotNull(biMap);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testOf4Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4);
        assertNotNull(biMap);
        assertEquals(4, biMap.size());
        assertEquals(4, biMap.get("four"));
    }

    @Test
    public void testOf5Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
        assertNotNull(biMap);
        assertEquals(5, biMap.size());
        assertEquals(5, biMap.get("five"));
    }

    @Test
    public void testOf6Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6);
        assertNotNull(biMap);
        assertEquals(6, biMap.size());
        assertEquals(6, biMap.get("six"));
    }

    @Test
    public void testOf7Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7);
        assertNotNull(biMap);
        assertEquals(7, biMap.size());
        assertEquals(7, biMap.get("seven"));
    }

    @Test
    public void testOf8Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8);
        assertNotNull(biMap);
        assertEquals(8, biMap.size());
        assertEquals(8, biMap.get("eight"));
    }

    @Test
    public void testOf9Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8, "nine", 9);
        assertNotNull(biMap);
        assertEquals(9, biMap.size());
        assertEquals(9, biMap.get("nine"));
    }

    @Test
    public void testOf10Entries() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10);
        assertNotNull(biMap);
        assertEquals(10, biMap.size());
        assertEquals(10, biMap.get("ten"));
    }

    @Test
    public void testOfWithDuplicateValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            BiMap.of("one", 1, "two", 1);
        });
    }

    @Test
    public void testOfWithNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            BiMap.of(null, 1);
        });
    }

    @Test
    public void testOfWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            BiMap.of("one", null);
        });
    }

    @Test
    public void testCopyOf() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        BiMap<String, Integer> biMap = BiMap.copyOf(map);
        assertNotNull(biMap);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testCopyOfWithDuplicateValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 1);

        assertThrows(IllegalArgumentException.class, () -> {
            BiMap.copyOf(map);
        });
    }

    @Test
    public void testGet() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertNull(biMap.get("three"));
    }

    @Test
    public void testGetByValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertEquals("one", biMap.getByValue(1));
        assertEquals("two", biMap.getByValue(2));
        assertNull(biMap.getByValue(3));
    }

    @Test
    public void testGetByValueOrDefault() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertEquals("one", biMap.getByValueOrDefault(1, "default"));
        assertEquals("two", biMap.getByValueOrDefault(2, "default"));
        assertEquals("default", biMap.getByValueOrDefault(3, "default"));
    }

    @Test
    public void testPut() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertNull(biMap.put("one", 1));
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals("one", biMap.getByValue(1));
    }

    @Test
    public void testPutReplaceValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        Integer oldValue = biMap.put("one", 11);
        assertEquals(1, oldValue);
        assertEquals(11, biMap.get("one"));
        assertNull(biMap.getByValue(1));
        assertEquals("one", biMap.getByValue(11));
    }

    @Test
    public void testPutSameKeyValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        Integer oldValue = biMap.put("one", 1);
        assertEquals(1, oldValue);
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
    }

    @Test
    public void testPutWithNullKey() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            biMap.put(null, 1);
        });
    }

    @Test
    public void testPutWithNullValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            biMap.put("one", null);
        });
    }

    @Test
    public void testPutWithDuplicateValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        assertThrows(IllegalArgumentException.class, () -> {
            biMap.put("two", 1);
        });
    }

    @Test
    public void testForcePut() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertNull(biMap.forcePut("one", 1));
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
    }

    @Test
    public void testForcePutRemovesPreviousMapping() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNull(biMap.forcePut("two", 1));
        assertEquals(1, biMap.size());
        assertNull(biMap.get("one"));
        assertEquals(1, biMap.get("two"));
        assertEquals("two", biMap.getByValue(1));
    }

    @Test
    public void testForcePutReplaceValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        Integer oldValue = biMap.forcePut("one", 2);
        assertEquals(1, oldValue);
        assertEquals(2, biMap.get("one"));
    }

    @Test
    public void testForcePutWithNullKey() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            biMap.forcePut(null, 1);
        });
    }

    @Test
    public void testForcePutWithNullValue() {
        BiMap<String, Integer> biMap = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            biMap.forcePut("one", null);
        });
    }

    @Test
    public void testPutAll() {
        BiMap<String, Integer> biMap = new BiMap<>();
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        biMap.putAll(map);
        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
    }

    @Test
    public void testPutAllWithDuplicateValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        Map<String, Integer> map = new HashMap<>();
        map.put("two", 1);

        assertThrows(IllegalArgumentException.class, () -> {
            biMap.putAll(map);
        });
    }

    @Test
    public void testRemove() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        Integer removed = biMap.remove("one");
        assertEquals(1, removed);
        assertEquals(1, biMap.size());
        assertNull(biMap.get("one"));
        assertNull(biMap.getByValue(1));
    }

    @Test
    public void testRemoveNonExistent() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNull(biMap.remove("two"));
        assertEquals(1, biMap.size());
    }

    @Test
    public void testRemoveByValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        String removed = biMap.removeByValue(1);
        assertEquals("one", removed);
        assertEquals(1, biMap.size());
        assertNull(biMap.get("one"));
        assertNull(biMap.getByValue(1));
    }

    @Test
    public void testRemoveByValueNonExistent() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNull(biMap.removeByValue(2));
        assertEquals(1, biMap.size());
    }

    @Test
    public void testContainsKey() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertTrue(biMap.containsKey("one"));
        assertTrue(biMap.containsKey("two"));
        assertFalse(biMap.containsKey("three"));
    }

    @Test
    public void testContainsValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertTrue(biMap.containsValue(1));
        assertTrue(biMap.containsValue(2));
        assertFalse(biMap.containsValue(3));
    }

    @Test
    public void testKeySet() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        ImmutableSet<String> keys = biMap.keySet();
        assertNotNull(keys);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
    }

    @Test
    public void testValues() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        ImmutableSet<Integer> values = biMap.values();
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEntrySet() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        ImmutableSet<Map.Entry<String, Integer>> entries = biMap.entrySet();
        assertNotNull(entries);
        assertEquals(2, entries.size());

        boolean foundOne = false;
        boolean foundTwo = false;
        for (Map.Entry<String, Integer> entry : entries) {
            if ("one".equals(entry.getKey()) && Integer.valueOf(1).equals(entry.getValue())) {
                foundOne = true;
            }
            if ("two".equals(entry.getKey()) && Integer.valueOf(2).equals(entry.getValue())) {
                foundTwo = true;
            }
        }
        assertTrue(foundOne);
        assertTrue(foundTwo);
    }

    @Test
    public void testInversed() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        BiMap<Integer, String> inverse = biMap.inverse();

        assertNotNull(inverse);
        assertEquals(2, inverse.size());
        assertEquals("one", inverse.get(1));
        assertEquals("two", inverse.get(2));
        assertEquals(1, inverse.getByValue("one"));
        assertEquals(2, inverse.getByValue("two"));
    }

    @Test
    public void testInversedBackedBySameData() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        BiMap<Integer, String> inverse = biMap.inverse();

        biMap.put("two", 2);
        assertEquals(2, inverse.size());
        assertEquals("two", inverse.get(2));

        inverse.put(3, "three");
        assertEquals(3, biMap.size());
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testInversedReturnsSameInstance() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        BiMap<Integer, String> inverse1 = biMap.inverse();
        BiMap<Integer, String> inverse2 = biMap.inverse();

        assertSame(inverse1, inverse2);
    }

    @Test
    public void testCopy() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        BiMap<String, Integer> copy = biMap.copy();

        assertNotNull(copy);
        assertNotSame(biMap, copy);
        assertEquals(biMap.size(), copy.size());
        assertEquals(biMap.get("one"), copy.get("one"));
        assertEquals(biMap.get("two"), copy.get("two"));
    }

    @Test
    public void testCopyIsIndependent() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        BiMap<String, Integer> copy = biMap.copy();

        biMap.put("two", 2);
        assertEquals(1, copy.size());
        assertNull(copy.get("two"));
    }

    @Test
    public void testClear() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        biMap.clear();

        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
        assertNull(biMap.get("one"));
        assertNull(biMap.getByValue(1));
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

        biMap.clear();
        assertEquals(0, biMap.size());
    }

    @Test
    public void testHashCode() {
        BiMap<String, Integer> biMap1 = BiMap.of("one", 1, "two", 2);
        BiMap<String, Integer> biMap2 = BiMap.of("one", 1, "two", 2);

        assertEquals(biMap1.hashCode(), biMap2.hashCode());
    }

    @Test
    public void testHashCodeDifferentMaps() {
        BiMap<String, Integer> biMap1 = BiMap.of("one", 1);
        BiMap<String, Integer> biMap2 = BiMap.of("two", 2);

        assertNotEquals(biMap1.hashCode(), biMap2.hashCode());
    }

    @Test
    public void testEquals() {
        BiMap<String, Integer> biMap1 = BiMap.of("one", 1, "two", 2);
        BiMap<String, Integer> biMap2 = BiMap.of("one", 1, "two", 2);

        assertEquals(biMap1, biMap2);
        assertEquals(biMap2, biMap1);
    }

    @Test
    public void testEqualsSameInstance() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertEquals(biMap, biMap);
    }

    @Test
    public void testEqualsWithDifferentMaps() {
        BiMap<String, Integer> biMap1 = BiMap.of("one", 1);
        BiMap<String, Integer> biMap2 = BiMap.of("two", 2);

        assertNotEquals(biMap1, biMap2);
    }

    @Test
    public void testEqualsWithNull() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNotEquals(biMap, null);
    }

    @Test
    public void testEqualsWithDifferentType() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNotEquals(biMap, new HashMap<>());
    }

    @Test
    public void testToString() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        String str = biMap.toString();

        assertNotNull(str);
        assertTrue(str.contains("one"));
        assertTrue(str.contains("1"));
    }

    @Test
    public void testToStringEmpty() {
        BiMap<String, Integer> biMap = new BiMap<>();
        String str = biMap.toString();

        assertNotNull(str);
        assertEquals("{}", str);
    }

    @Test
    public void testBuilder() {
        BiMap.Builder<String, Integer> builder = BiMap.builder();
        assertNotNull(builder);
    }

    @Test
    public void testBuilderPut() {
        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().put("one", 1).put("two", 2).build();

        assertNotNull(biMap);
        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
    }

    @Test
    public void testBuilderForcePut() {
        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().put("one", 1).forcePut("two", 1).build();

        assertNotNull(biMap);
        assertEquals(1, biMap.size());
        assertNull(biMap.get("one"));
        assertEquals(1, biMap.get("two"));
    }

    @Test
    public void testBuilderPutAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().putAll(map).put("three", 3).build();

        assertNotNull(biMap);
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

        assertNotNull(biMap);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testBuilderWithNullMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            BiMap.builder(null);
        });
    }

    @Test
    public void testBuilderBuild() {
        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().put("one", 1).build();

        assertNotNull(biMap);
        assertEquals(1, biMap.size());
    }

    @Test
    public void testMultipleOperations() {
        BiMap<String, Integer> biMap = new BiMap<>();

        biMap.put("one", 1);
        biMap.put("two", 2);
        biMap.put("three", 3);
        assertEquals(3, biMap.size());

        biMap.put("one", 11);
        assertEquals(11, biMap.get("one"));
        assertNull(biMap.getByValue(1));

        biMap.forcePut("four", 2);
        assertNull(biMap.get("two"));
        assertEquals("four", biMap.getByValue(2));
        assertEquals(3, biMap.size());

        biMap.remove("three");
        assertEquals(2, biMap.size());

        biMap.removeByValue(11);
        assertEquals(1, biMap.size());

        biMap.clear();
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testInverseOperations() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        BiMap<Integer, String> inverse = biMap.inverse();

        inverse.put(3, "three");
        assertEquals(3, biMap.size());
        assertEquals(3, biMap.get("three"));

        inverse.remove(1);
        assertEquals(2, biMap.size());
        assertNull(biMap.get("one"));

        inverse.forcePut(2, "new-two");
        assertEquals("new-two", biMap.getByValue(2));
        assertEquals("new-two", inverse.get(2));
    }
}
