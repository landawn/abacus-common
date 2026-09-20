package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class BiMapTest extends AbstractTest {

    @Test
    public void testOf() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
    }

    @Test
    public void testOfManyPairs() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10);
        assertEquals(10, biMap.size());
        assertEquals(10, biMap.get("ten"));
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

    @Test
    public void testOfMultiple() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2, "three", 3);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
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
    public void testInverse_PutWithNullKey() {
        BiMap<String, Integer> biMap = BiMap.of("a", 1);
        BiMap<Integer, String> inverse = biMap.inverse();
        assertThrows(IllegalArgumentException.class, () -> inverse.put(null, "b"));
    }

    @Test
    public void testInverse_PutWithNullValue() {
        BiMap<String, Integer> biMap = BiMap.of("a", 1);
        BiMap<Integer, String> inverse = biMap.inverse();
        assertThrows(IllegalArgumentException.class, () -> inverse.put(2, null));
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
    public void testCopyOf_EmptyMap() {
        BiMap<String, Integer> biMap = BiMap.copyOf(new HashMap<>());
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
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
    public void test_01() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("a", 1);
        biMap.put("b", 2);
        assertEquals(1, biMap.get("a").intValue());
        assertEquals("a", biMap.getByValue(1));

        biMap.put("a", 11);
        assertNull(biMap.getByValue(1));

        biMap.forcePut("c", 2);
        assertNull(biMap.get("b"));

        assertThrows(IllegalArgumentException.class, () -> biMap.put(null, 1));
        assertThrows(IllegalArgumentException.class, () -> biMap.put("d", null));

        Map<String, Integer> map = BiMap.of("e", 5);
        biMap.putAll(map);
        assertEquals(5, biMap.remove("e").intValue());
        assertEquals("c", biMap.removeByValue(2));

        assertTrue(biMap.containsKey("a"));
        assertTrue(biMap.containsValue(11));
        biMap.clear();

        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());

        assertNull(biMap.remove("a"));
        assertNull(biMap.removeByValue(1));
        BiMap<String, Integer> biMap2 = new BiMap<>(map.size() * 2);
        biMap2.putAll(map);
        assertEquals(1, biMap2.size());
        assertEquals(5, biMap2.get("e"));
    }

    @Test
    public void testGetByValueOrDefault() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        assertEquals("one", biMap.getByValueOrDefault(1, "default"));
        assertEquals("two", biMap.getByValueOrDefault(2, "default"));
        assertEquals("default", biMap.getByValueOrDefault(3, "default"));
        assertNull(biMap.getByValueOrDefault(99, null));
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
    public void testPut_ReplaceValueAndInverse() {
        BiMap<String, Integer> biMap = BiMap.of("a", 1, "b", 2);
        biMap.put("a", 10);
        assertEquals(10, biMap.get("a"));
        assertNull(biMap.getByValue(1));
        assertEquals("a", biMap.getByValue(10));
        assertEquals(2, biMap.size());
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
    public void testPutIfAbsentRejectsNullValueForExistingKey() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);

        assertThrows(IllegalArgumentException.class, () -> biMap.putIfAbsent("one", null));
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals("one", biMap.getByValue(1));
        assertFalse(biMap.containsValue(null));
    }

    @Test
    public void testForcePutReplaceValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        Integer oldValue = biMap.forcePut("one", 2);
        assertEquals(1, oldValue);
        assertEquals(2, biMap.get("one"));
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
    public void testForcePut_SameKeyAndValue() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        Integer old = biMap.forcePut("one", 1);
        assertEquals(1, old);
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals("one", biMap.getByValue(1));
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
    public void testRemoveByValue_ClearsInverse() {
        BiMap<String, Integer> biMap = BiMap.of("a", 1, "b", 2);
        String removed = biMap.removeByValue(2);
        assertEquals("b", removed);
        assertNull(biMap.get("b"));
        assertNull(biMap.getByValue(2));
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
    public void testEntrySet_Iteration() {
        BiMap<String, Integer> biMap = BiMap.of("a", 1, "b", 2, "c", 3);
        ImmutableSet<Map.Entry<String, Integer>> entries = biMap.entrySet();
        assertEquals(3, entries.size());

        int count = 0;
        for (Map.Entry<String, Integer> entry : entries) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            count++;
        }
        assertEquals(3, count);
    }

    // inverse()
    @Test
    public void testInverse() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1, "two", 2);
        BiMap<Integer, String> inverse = biMap.inverse();
        assertNotNull(inverse);
        assertEquals(2, inverse.size());
        assertEquals("one", inverse.get(1));
        assertEquals("two", inverse.get(2));
    }

    @Test
    public void testInverse_modifications() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        BiMap<Integer, String> inverse = biMap.inverse();

        // Modifications through inverse view reflect in original
        inverse.put(2, "two");
        assertEquals(2, biMap.size());
        assertEquals(2, biMap.get("two"));

        // Same instance returned on repeated calls
        assertSame(inverse, biMap.inverse());
    }

    @Test
    public void testInversedTwice() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);

        BiMap<Integer, String> inverse = biMap.inverse();
        BiMap<String, Integer> inverseInverse = inverse.inverse();

        assertSame(biMap, inverseInverse);
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
    public void testConstructorRejectsNullSuppliersAndNullResults() {
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, Integer>((Supplier<Map<String, Integer>>) null, HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, Integer>(HashMap::new, (Supplier<Map<Integer, String>>) null));
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, Integer>(() -> null, HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, Integer>(HashMap::new, () -> null));
    }

    @Test
    public void testConstructorRejectsNonemptyAndAliasedSupplierResults() {
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, Integer>(() -> new HashMap<>(Map.of("one", 1)), HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, Integer>(HashMap::new, () -> new HashMap<>(Map.of(1, "one"))));

        final Map<String, String> sharedMap = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> new BiMap<String, String>(() -> sharedMap, () -> sharedMap));
        assertTrue(sharedMap.isEmpty());
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
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = new BiMap<>(100, 0.8f);
        assertTrue(biMap.isEmpty());
        assertEquals(0, biMap.size());
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
    public void testEqualsWithDifferentMaps() {
        BiMap<String, Integer> biMap1 = BiMap.of("one", 1);
        BiMap<String, Integer> biMap2 = BiMap.of("two", 2);

        assertNotEquals(biMap1, biMap2);
    }

    @Test
    public void testEqualsWithDifferentType() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNotEquals(biMap, new HashMap<>());
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
    public void testEqualsSameInstance() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertEquals(biMap, biMap);
    }

    @Test
    public void testEqualsWithNull() {
        BiMap<String, Integer> biMap = BiMap.of("one", 1);
        assertNotEquals(biMap, null);
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
    public void testBuilderBuild() {
        BiMap<String, Integer> biMap = BiMap.<String, Integer> builder().put("one", 1).build();

        assertNotNull(biMap);
        assertEquals(1, biMap.size());
    }

    @Test
    public void testBuilderWithNullMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            BiMap.builder(null);
        });
    }

    @Test
    public void testInverse_supplierBuiltBiMap_withNonInstantiableMapClass() {
        final BiMap<String, Integer> biMap = new BiMap<>(() -> Collections.synchronizedMap(new HashMap<>()),
                () -> Collections.synchronizedMap(new HashMap<>()));
        biMap.put("a", 1);
        biMap.put("b", 2);

        final BiMap<Integer, String> inverse = biMap.inverse();
        assertEquals("a", inverse.get(1));
        assertEquals("b", inverse.get(2));

        final BiMap<Integer, String> copy = inverse.copy();
        assertEquals("a", copy.get(1));
        assertEquals("b", copy.get(2));
    }

    @Test
    public void testInverseCopy_preservesValueMapComparator() {
        final BiMap<String, Integer> biMap = new BiMap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), HashMap::new);
        biMap.put("Apple", 1);

        final BiMap<Integer, String> copyOfInverse = biMap.inverse().copy();
        assertEquals("Apple", copyOfInverse.get(1));

        // the copied inverse keeps the case-insensitive value map, so a value differing only by case is a duplicate
        assertThrows(IllegalArgumentException.class, () -> copyOfInverse.put(2, "APPLE"));
    }

    @Test
    public void testPutSameMappingUsesBackingMapKeyEquivalence() {
        final BiMap<String, Integer> map = new BiMap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), HashMap::new);
        map.put("Alpha", 1);

        assertDoesNotThrow(() -> map.put("ALPHA", 1));
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(1), map.get("alpha"));
        assertEquals("Alpha", map.getByValue(1));
    }

    @Test
    public void testPutEquivalentKeyWithNewValueKeepsCanonicalKeyInInverse() {
        final BiMap<String, Integer> map = new BiMap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), HashMap::new);
        final String canonicalKey = new String("Alpha");
        final String equivalentKey = new String("ALPHA");
        map.put(canonicalKey, 1);

        map.put(equivalentKey, 2);

        assertEquals(1, map.size());
        assertSame(canonicalKey, map.keySet().iterator().next());
        assertSame(canonicalKey, map.getByValue(2));
        assertSame(canonicalKey, map.inverse().get(2));
        assertTrue(map.inverse().containsEntry(2, canonicalKey));
    }

    @Test
    public void testPutSameMappingUsesBackingMapValueEquivalence() {
        final BiMap<Integer, String> map = new BiMap<>(HashMap::new, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        map.put(1, "Alpha");

        assertDoesNotThrow(() -> map.put(1, "ALPHA"));
        assertEquals(1, map.size());
        assertEquals("Alpha", map.get(1));
        assertEquals(Integer.valueOf(1), map.getByValue("alpha"));
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testCopyOfComparatorBackedSortedMapThenCopy() {
        // regression: copyOf derived suppliers from the runtime map class, losing the comparator;
        // copy() then threw ClassCastException for non-Comparable keys
        final TreeMap<Object, String> tm = new TreeMap<>(java.util.Comparator.comparingInt(System::identityHashCode));
        final Object k = new Object();
        tm.put(k, "one");

        final BiMap<Object, String> bm = BiMap.copyOf(tm);
        assertEquals("one", bm.copy().get(k));
        assertEquals(k, bm.inverse().copy().get("one"));
    }

    @Test
    public void testPutKeepsLinkedHashMapOrderAndForcePutExactMappingIsNoOp() {
        // regression: put() removed-then-reinserted the key, moving it to the end of
        // LinkedHashMap-backed BiMaps; forcePut of the exact existing mapping was documented as a
        // no-op but also moved the entry
        final BiMap<String, Integer> m = new BiMap<>(java.util.LinkedHashMap::new, java.util.LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);

        m.forcePut("a", 1);
        assertEquals(CommonUtil.asList("a", "b", "c"), new java.util.ArrayList<>(m.keySet()));

        m.put("a", 9);
        assertEquals(CommonUtil.asList("a", "b", "c"), new java.util.ArrayList<>(m.keySet()));
        assertEquals(Integer.valueOf(9), m.get("a"));
    }

    /**
     * Map.replaceAll must work even though entrySet() yields immutable entry snapshots.
     * Pre-fix: JDK default replaceAll called Entry.setValue → UnsupportedOperationException.
     */
    @Test
    public void testReplaceAll_updatesValuesAndInverse() {
        final BiMap<String, Integer> m = BiMap.of("a", 1, "b", 2);
        m.replaceAll((k, v) -> v + 10);

        assertEquals(11, m.get("a"));
        assertEquals(12, m.get("b"));
        assertEquals("a", m.getByValue(11));
        assertEquals("b", m.getByValue(12));
        assertNull(m.getByValue(1));
        assertNull(m.getByValue(2));
    }

    @Test
    public void testReplaceAll_rejectsDuplicateValuesWithoutMutating() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);

        assertThrows(IllegalArgumentException.class, () -> m.replaceAll((k, v) -> 99));

        // Atomic: the collision is detected before any entry is modified.
        assertEquals(1, m.get("a"));
        assertEquals(2, m.get("b"));
        assertEquals(2, m.size());
        assertEquals("a", m.getByValue(1));
        assertEquals("b", m.getByValue(2));
    }

    @Test
    public void testReplaceAll_valueCollisionUsesValueMapEquivalence() {
        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        m.put("a", "first");
        m.put("b", "second");

        // "SAME" collides with "same" under the case-insensitive value map.
        assertThrows(IllegalArgumentException.class, () -> m.replaceAll((k, v) -> k.equals("a") ? "same" : "SAME"));

        // Atomic: both entries are unchanged after the failed replaceAll.
        assertEquals(2, m.size());
        assertEquals("first", m.get("a"));
        assertEquals("second", m.get("b"));
        assertEquals("a", m.getByValue("FIRST"));
        assertEquals("b", m.getByValue("SECOND"));
    }

    @Test
    public void testReplaceAll_isAtomicWhenFunctionFails() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);

        assertThrows(IllegalArgumentException.class, () -> m.replaceAll((k, v) -> k.equals("a") ? v + 10 : null));

        // Atomic: the null replacement is detected before any entry is modified.
        assertEquals(1, m.get("a"));
        assertEquals(2, m.get("b"));
        assertEquals("a", m.getByValue(1));
        assertEquals("b", m.getByValue(2));
    }

    @Test
    public void testReplaceAll_swappingValuesSucceeds() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);

        // Each replacement value is still held by another key until that key is processed.
        m.replaceAll((k, v) -> k.equals("a") ? 2 : (k.equals("b") ? 1 : 3));

        assertEquals(3, m.size());
        assertEquals(2, m.get("a"));
        assertEquals(1, m.get("b"));
        assertEquals(3, m.get("c"));
        assertEquals("a", m.getByValue(2));
        assertEquals("b", m.getByValue(1));
        assertEquals("c", m.getByValue(3));
        assertEquals(CommonUtil.asList("a", "b", "c"), new java.util.ArrayList<>(m.keySet()));
    }

    @Test
    public void testReplaceAll_storesExactComparatorEquivalentResult() {
        final BiMap<Integer, String> m = new BiMap<>(LinkedHashMap::new, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        final String replacement = new String("ALPHA");
        m.put(1, "Alpha");

        m.replaceAll((k, v) -> replacement);

        assertSame(replacement, m.get(1));
        assertSame(replacement, m.values().iterator().next());
        assertEquals(Integer.valueOf(1), m.getByValue("alpha"));
    }

    @Test
    public void testReplaceAll_onInverseUpdatesOriginal() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);

        m.inverse().replaceAll((value, key) -> key.toUpperCase());

        assertNull(m.get("a"));
        assertNull(m.get("b"));
        assertEquals(Integer.valueOf(1), m.get("A"));
        assertEquals(Integer.valueOf(2), m.get("B"));
        assertEquals("A", m.inverse().get(1));
        assertEquals("B", m.inverse().get(2));
    }

    @Test
    public void testReplaceAll_functionExceptionDoesNotMutate() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);

        assertThrows(IllegalStateException.class, () -> m.replaceAll((k, v) -> {
            if (k.equals("b")) {
                throw new IllegalStateException("boom");
            }

            return v + 10;
        }));

        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals(Integer.valueOf(2), m.get("b"));
        assertEquals("a", m.getByValue(1));
        assertEquals("b", m.getByValue(2));
    }

    @Test
    public void testReplaceAll_invokesFunctionOncePerOriginalEntry() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        final java.util.List<String> invocations = new java.util.ArrayList<>();
        m.put("a", 1);
        m.put("b", 2);

        m.replaceAll((k, v) -> {
            invocations.add(k + "=" + v);
            return v + 10;
        });

        assertEquals(CommonUtil.asList("a=1", "b=2"), invocations);
    }

    @Test
    public void testReplaceAll_rejectsReusedBackingMapSupplierWithoutMutating() {
        final Map<Integer, String> sharedValueMap = new LinkedHashMap<>();
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, () -> sharedValueMap);
        m.put("a", 1);
        m.put("b", 2);

        assertThrows(IllegalArgumentException.class, () -> m.replaceAll((k, v) -> v + 10));

        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals(Integer.valueOf(2), m.get("b"));
        assertEquals("a", m.getByValue(1));
        assertEquals("b", m.getByValue(2));
        assertEquals(2, sharedValueMap.size());
    }

    @Test
    public void testReplaceAll_doesNotStructurallyModifyForwardKeyMap() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        final java.util.Iterator<String> keyIterator = m.keySet().iterator();

        assertEquals("a", keyIterator.next());
        m.replaceAll((k, v) -> v + 10);

        assertEquals("b", keyIterator.next());
        assertEquals("c", keyIterator.next());
        assertFalse(keyIterator.hasNext());
    }

    // ------------------------------------------------------------------------------------------
    // values() must present the FORWARD map's order, so keySet()/values()/entrySet() line up.
    // ------------------------------------------------------------------------------------------

    private static <K, V> void assertViewsAligned(final BiMap<K, V> m) {
        final java.util.List<K> keys = new java.util.ArrayList<>(m.keySet());
        final java.util.List<V> values = new java.util.ArrayList<>(m.values());
        final java.util.List<Map.Entry<K, V>> entries = new java.util.ArrayList<>(m.entrySet());

        assertEquals(m.size(), keys.size());
        assertEquals(m.size(), values.size());
        assertEquals(m.size(), entries.size());

        for (int i = 0; i < keys.size(); i++) {
            assertEquals(m.get(keys.get(i)), values.get(i), "values()[" + i + "] does not pair with keySet()[" + i + "]");
            assertEquals(keys.get(i), entries.get(i).getKey());
            assertEquals(values.get(i), entries.get(i).getValue());
        }
    }

    @Test
    public void testValues_alignedWithKeySet_hashBacked() {
        final BiMap<String, Integer> m = new BiMap<>();

        for (int i = 0; i < 16; i++) {
            m.put("k" + i, i * 7);
        }

        assertViewsAligned(m);
    }

    @Test
    public void testValues_alignedWithKeySet_afterValueReplacement() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.put("a", 3); // "a" keeps its position; the reverse map's order now differs

        assertEquals(CommonUtil.asList("a", "b"), new java.util.ArrayList<>(m.keySet()));
        assertEquals(CommonUtil.asList(3, 2), new java.util.ArrayList<>(m.values()));
        assertViewsAligned(m);
    }

    @Test
    public void testValues_alignedWithKeySet_afterForcePut() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        m.forcePut("a", 3); // removes ("c", 3)

        assertEquals(CommonUtil.asList("a", "b"), new java.util.ArrayList<>(m.keySet()));
        assertEquals(CommonUtil.asList(3, 2), new java.util.ArrayList<>(m.values()));
        assertViewsAligned(m);
    }

    @Test
    public void testValues_alignedWithKeySet_mismatchedBackingMapTypes() {
        // A LinkedHashMap forward map with a TreeMap reverse map: values() must follow insertion order,
        // not the reverse map's sorted order.
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, TreeMap::new);
        m.put("c", 3);
        m.put("a", 1);
        m.put("b", 2);

        assertEquals(CommonUtil.asList("c", "a", "b"), new java.util.ArrayList<>(m.keySet()));
        assertEquals(CommonUtil.asList(3, 1, 2), new java.util.ArrayList<>(m.values()));
        assertViewsAligned(m);
    }

    @Test
    public void testValues_alignedWithKeySet_afterRemoval() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        m.removeByValue(2);

        assertEquals(CommonUtil.asList("a", "c"), new java.util.ArrayList<>(m.keySet()));
        assertEquals(CommonUtil.asList(1, 3), new java.util.ArrayList<>(m.values()));
        assertViewsAligned(m);
    }

    @Test
    public void testValues_alignedWithKeySet_afterInverseMutation() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);
        m.inverse().put(3, "c");

        assertViewsAligned(m);
        assertViewsAligned(m.inverse());
    }

    @Test
    public void testValues_containsIsCorrectAndSetSemantics() {
        final BiMap<String, Integer> m = BiMap.of("a", 1, "b", 2);

        assertTrue(m.values().contains(1));
        assertTrue(m.values().contains(2));
        assertFalse(m.values().contains(3));
        assertFalse(m.values().contains(null));
        assertFalse(m.values().contains("not an Integer"));
        assertTrue(m.values().containsAll(CommonUtil.asList(1, 2)));
        assertEquals(CommonUtil.asSet(1, 2), new java.util.HashSet<>(m.values()));
        // values() is a Set, so it must obey Set equality
        assertEquals(CommonUtil.asSet(1, 2), m.values());
    }

    @Test
    public void testValues_isLiveViewAndReadOnly() {
        final BiMap<String, Integer> m = BiMap.of("a", 1);
        final ImmutableSet<Integer> values = m.values();

        m.put("b", 2);

        assertEquals(2, values.size());
        assertTrue(values.contains(2));
        assertThrows(UnsupportedOperationException.class, () -> values.add(3));
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
        assertThrows(UnsupportedOperationException.class, () -> values.iterator().remove());
    }

    @Test
    public void testValues_emptyBiMap() {
        final BiMap<String, Integer> m = new BiMap<>();

        assertTrue(m.values().isEmpty());
        assertEquals(0, m.values().size());
        assertFalse(m.values().iterator().hasNext());
        assertViewsAligned(m);
    }

    @Test
    public void testCollectionViewsAreCached() {
        final BiMap<String, Integer> m = BiMap.of("a", 1);

        assertSame(m.keySet(), m.keySet());
        assertSame(m.values(), m.values());
        assertSame(m.entrySet(), m.entrySet());
    }

    @Test
    public void testEntrySetContains() {
        final BiMap<String, Integer> m = BiMap.of("a", 1, "b", 2);

        assertTrue(m.entrySet().contains(Map.entry("a", 1)));
        assertFalse(m.entrySet().contains(Map.entry("a", 2)));
        assertFalse(m.entrySet().contains(Map.entry("z", 1)));
        assertFalse(m.entrySet().contains("not an entry"));
    }

    @Test
    public void testPutDuplicateValueMessageNamesConflictingKey() {
        final BiMap<String, Integer> m = BiMap.of("one", 1);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> m.put("two", 1));
        assertTrue(ex.getMessage().contains("one"), "message should name the conflicting key: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("1"), ex.getMessage());
        // the BiMap is left untouched
        assertEquals(1, m.size());
        assertEquals("one", m.getByValue(1));
    }

    @Test
    public void testInheritedMapDefaultsEnforceValueUniqueness() {
        final BiMap<String, Integer> m = BiMap.of("a", 1, "b", 2);

        assertThrows(IllegalArgumentException.class, () -> m.replace("a", 2));
        assertThrows(IllegalArgumentException.class, () -> m.replace("a", 1, 2));
        assertThrows(IllegalArgumentException.class, () -> m.computeIfAbsent("z", k -> 2));
        assertThrows(IllegalArgumentException.class, () -> m.computeIfPresent("a", (k, v) -> 2));
        assertThrows(IllegalArgumentException.class, () -> m.compute("a", (k, v) -> 2));
        assertThrows(IllegalArgumentException.class, () -> m.merge("a", 2, (x, y) -> y));
        // Map.merge's own contract requires a non-null value -> NullPointerException, not IAE
        assertThrows(NullPointerException.class, () -> m.merge("a", null, (x, y) -> y));
        // unchanged throughout
        assertEquals(BiMap.of("a", 1, "b", 2), m);
    }

    @Test
    public void testCopyOfSortedSourceIsIndependentAndKeepsTheComparator() {
        // The key-map supplier captures only the comparator, not the source map itself. That the source is
        // no longer *retained* is not observable from a unit test (it is a heap-liveness property); what is
        // observable, and asserted here, is that the copy, its copy() and its inverse() all keep working
        // from the captured comparator after the source map is emptied.
        final SortedMap<String, Integer> src = new TreeMap<>(Comparator.reverseOrder());
        src.put("a", 1);
        src.put("b", 2);

        final BiMap<String, Integer> bm = BiMap.copyOf(src);
        src.clear(); // must not affect bm, its copies, or its inverse

        assertEquals(CommonUtil.asList("b", "a"), new java.util.ArrayList<>(bm.keySet()));
        assertEquals(CommonUtil.asList("b", "a"), new java.util.ArrayList<>(bm.copy().keySet()));
        assertEquals(2, bm.inverse().size());
        assertEquals(2, bm.inverse().copy().size());
        assertViewsAligned(bm);
    }

    @Test
    public void testViewsStayAlignedUnderRandomMutation() {
        // The three views must correspond entry for entry no matter which backing map types are used or
        // which mutation path got the BiMap into its current state.
        final java.util.Random rnd = new java.util.Random(20260831L);

        for (int trial = 0; trial < 200; trial++) {
            final BiMap<String, Integer> m = switch (trial % 4) {
                case 0 -> new BiMap<>();
                case 1 -> new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
                case 2 -> new BiMap<>(LinkedHashMap::new, TreeMap::new);
                default -> new BiMap<>(TreeMap::new, LinkedHashMap::new);
            };

            for (int i = 0; i < 40; i++) {
                final String k = "k" + rnd.nextInt(12);
                final int v = rnd.nextInt(12);

                try {
                    switch (rnd.nextInt(5)) {
                        case 0 -> m.put(k, v);
                        case 1 -> m.forcePut(k, v);
                        case 2 -> m.remove(k);
                        case 3 -> m.removeByValue(v);
                        default -> m.putIfAbsent(k, v);
                    }
                } catch (final IllegalArgumentException expected) {
                    // put/putIfAbsent reject a value already bound to another key
                }
            }

            assertViewsAligned(m);
            assertViewsAligned(m.inverse());

            final java.util.List<Integer> values = new java.util.ArrayList<>(m.values());
            assertEquals(values.size(), new java.util.HashSet<>(values).size(), "values() must be distinct");

            for (final Integer v : values) {
                assertTrue(m.values().contains(v));
            }

            assertFalse(m.values().contains(-1));
        }
    }

    @Test
    public void testCollectionViewsObeyTheirHashCodeContracts() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, TreeMap::new);
        m.put("c", 3);
        m.put("a", 1);
        m.put("b", 2);

        assertEquals(CommonUtil.asSet(1, 2, 3), m.values());
        assertEquals(CommonUtil.asSet(1, 2, 3).hashCode(), m.values().hashCode());
        assertEquals(CommonUtil.asSet("a", "b", "c").hashCode(), m.keySet().hashCode());
        // Map.hashCode() is the sum of its entries' hash codes, so entrySet() must match
        assertEquals(m.hashCode(), m.entrySet().hashCode());
        assertEquals(Map.of("c", 3, "a", 1, "b", 2).entrySet().hashCode(), m.entrySet().hashCode());
    }

    @Test
    public void testCachedCollectionViewsStayLive() {
        final BiMap<String, Integer> m = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        m.put("a", 1);
        m.put("b", 2);

        final ImmutableSet<String> keys = m.keySet();
        final ImmutableSet<Integer> values = m.values();
        final ImmutableSet<Map.Entry<String, Integer>> entries = m.entrySet();

        m.put("c", 3);
        m.remove("a");

        assertEquals(2, keys.size());
        assertEquals(2, values.size());
        assertEquals(2, entries.size());
        assertViewsAligned(m);
        assertEquals(new java.util.ArrayList<>(m.values()), new java.util.ArrayList<>(values));

        m.clear();

        assertTrue(keys.isEmpty());
        assertTrue(values.isEmpty());
        assertTrue(entries.isEmpty());
    }

    // ------------------------------------------------------------------------------------------
    // put() must keep the forward and reverse maps in lockstep on the null and failure paths.
    // ------------------------------------------------------------------------------------------

    /** A HashMap whose {@code put} rejects the next {@code rejectCount} writes. */
    private static final class RejectingHashMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

        private int rejectCount;

        @Override
        public V put(final K key, final V value) {
            if (rejectCount > 0) {
                rejectCount--;
                throw new IllegalStateException("reverse put rejected");
            }

            return super.put(key, value);
        }
    }

    /**
     * An insertion-ordered map whose {@code put} rejects the next {@code rejectCount} writes ({@code -1}
     * rejects every write). When {@code cached} is set, that one instance is thrown every time, the way a
     * map that pre-allocates its failure would.
     */
    private static final class RejectingLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;

        private int rejectCount;
        private RuntimeException cached;

        @Override
        public V put(final K key, final V value) {
            if (rejectCount != 0) {
                if (rejectCount > 0) {
                    rejectCount--;
                }

                throw cached == null ? new IllegalStateException("reverse put rejected") : cached;
            }

            return super.put(key, value);
        }
    }

    @Test
    public void testPutWithStaleReverseEntryKeepsBothMapsBijective() {
        // A BiMap's values are the keys of its reverse map. When a stored value's hashCode changes while it
        // is in the map, the reverse lookup misses; put() must not then write a null key into the reverse
        // map, nor leave a stale entry behind that defeats the value-uniqueness check.
        // The two key objects are deliberately equal but NOT identical. Reusing one interned literal for
        // both puts would let a repair that matches the stale reverse entry against the INCOMING key by
        // identity pass this test while silently abandoning this, the ordinary HashMap case.
        final BiMap<String, java.util.List<Integer>> m = new BiMap<>();
        final String storedKey = new String("a");
        final java.util.List<Integer> stored = new java.util.ArrayList<>(java.util.List.of(1));
        m.put(storedKey, stored);
        stored.add(2);

        final java.util.List<Integer> replacement = new java.util.ArrayList<>(java.util.List.of(9));
        final String equivalentKey = new String("a");
        assertNotSame(storedKey, equivalentKey);
        assertSame(stored, m.put(equivalentKey, replacement));

        assertEquals(1, m.size());
        assertEquals(1, m.inverse().size());
        assertEquals("a", m.getByValue(replacement));
        assertSame(storedKey, m.getByValue(replacement), "both directions must expose the key object keyMap kept");
        assertTrue(m.containsValue(replacement));

        for (final String reverseStoredKey : m.inverse().values()) {
            assertNotNull(reverseStoredKey);
        }

        assertEquals(1, m.inverse().copy().size());
        assertEquals("a", m.inverse().copy().get(replacement));

        // the value-uniqueness guard must still see the stored value
        assertThrows(IllegalArgumentException.class, () -> m.put("b", new java.util.ArrayList<>(java.util.List.of(9))));
        assertEquals(1, m.size());
    }

    @Test
    public void testPutWithSelfEvictingReverseMapStoresNoNullKey() {
        // Same missed reverse lookup, reached with immutable values and no mutation at all: a backing map
        // that drops entries of its own accord.
        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, () -> new LinkedHashMap<String, String>(16, 0.75f, false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, String> eldest) {
                return size() > 2;
            }
        });

        m.put("k1", "v1");
        m.put("k2", "v2");
        m.put("k3", "v3"); // the reverse map has evicted v1 -> k1 by now

        assertEquals("v1", m.put("k1", "v9"));
        assertEquals("k1", m.getByValue("v9"));
        assertTrue(m.inverse().containsKey("v9"));

        for (final String storedKey : m.inverse().values()) {
            assertNotNull(storedKey);
        }
    }

    @Test
    public void testPutRollsBackWhenReverseMapRejectsTheWrite() {
        // TreeMap.get never calls the comparator while the tree is empty, but TreeMap.put type-checks with
        // compare(key, key) - so the rejection lands after the forward map has already been written.
        final BiMap<String, Object> m = new BiMap<>(HashMap::new, () -> new TreeMap<>(Comparator.comparing(o -> (String) o)));

        assertThrows(ClassCastException.class, () -> m.put("a", 42));

        assertEquals(0, m.size());
        assertEquals(0, m.inverse().size());
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("a"));
        assertNull(m.get("a"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testForcePutRollbackRestoresTheDisplacedEntry() {
        final RejectingHashMap<String, String>[] reverse = new RejectingHashMap[1];
        final BiMap<String, String> m = new BiMap<>(HashMap::new, () -> {
            final RejectingHashMap<String, String> map = new RejectingHashMap<>();
            reverse[0] = map;
            return map;
        });

        m.forcePut("k1", "V");
        reverse[0].rejectCount = 1; // reject only the commit, so the rollback itself can still write

        assertThrows(IllegalStateException.class, () -> m.forcePut("k2", "V"));

        // the entry displaced by the failed forcePut must survive, not just the bijection
        assertEquals(1, m.size());
        assertEquals(1, m.inverse().size());
        assertEquals("V", m.get("k1"));
        assertEquals("k1", m.getByValue("V"));
        assertFalse(m.containsKey("k2"));
        assertViewsAligned(m);
    }

    @Test
    public void testForEachFollowsForwardBackingMapOrder() {
        final BiMap<String, Integer> ordered = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        ordered.put("one", 1);
        ordered.put("two", 2);

        final java.util.List<String> seen = new java.util.ArrayList<>();
        ordered.forEach((k, v) -> seen.add(k + "=" + v));
        assertEquals(CommonUtil.asList("one=1", "two=2"), seen);

        // BiMap.of(..) is HashMap-backed, so forEach follows the table's order, which is not the argument
        // order in general. Asserting that it differs for one specific pair of keys would only pin those
        // two strings' bucket positions, so assert the property that does hold: forEach and keySet() agree.
        final BiMap<String, Integer> hashBacked = BiMap.of("one", 1, "two", 2);
        final java.util.List<String> hashSeen = new java.util.ArrayList<>();
        hashBacked.forEach((k, v) -> hashSeen.add(k));

        assertEquals(new java.util.ArrayList<>(hashBacked.keySet()), hashSeen);
    }

    @Test
    public void testOfKeepsOnlyTheLastValueOfARepeatedKey() {
        final BiMap<String, Integer> m = BiMap.of("a", 1, "a", 2);

        assertEquals(1, m.size());
        assertEquals(Integer.valueOf(2), m.get("a"));
        assertEquals("a", m.getByValue(2));
        assertFalse(m.containsValue(1));

        // the mirror-image mistake - a value still bound when it is supplied again - is rejected instead of
        // collapsed
        assertThrows(IllegalArgumentException.class, () -> BiMap.of("a", 1, "b", 1));

        // but a repeated value whose earlier binding a repeated key has already displaced is accepted, so
        // "a repeated value is rejected" would be too strong
        final BiMap<String, Integer> reused = BiMap.of("a", 1, "a", 2, "b", 1);
        assertEquals(2, reused.size());
        assertEquals(Integer.valueOf(2), reused.get("a"));
        assertEquals(Integer.valueOf(1), reused.get("b"));
        assertEquals("b", reused.getByValue(1));
    }

    @Test
    public void testCopyThrowsWhenSuppliersHandOutOneSharedInstance() {
        final Map<String, Integer> sharedForward = new HashMap<>();
        final Map<Integer, String> sharedReverse = new HashMap<>();
        final BiMap<String, Integer> m = new BiMap<>(() -> sharedForward, () -> sharedReverse);

        assertThrows(IllegalArgumentException.class, m::copy);

        m.put("a", 1);

        assertThrows(IllegalArgumentException.class, m::copy);
        assertThrows(IllegalArgumentException.class, () -> BiMap.copyOf(m));
        // replaceAll stages the replacements in a temporary BiMap built from the same suppliers, so it
        // reaches the same construction check
        assertThrows(IllegalArgumentException.class, () -> m.replaceAll((k, v) -> v + 1));
        assertEquals(Integer.valueOf(1), m.get("a"));
        assertThrows(IllegalArgumentException.class, () -> m.inverse().copy());
    }

    // ------------------------------------------------------------------------------------------
    // The stale-reverse-entry repair must decide "the same key" the way the FORWARD map does, and the
    // rollback must not destroy state it did not write.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testStaleEntryRepairDoesNotDropAnEqualButDistinctKeysEntry() {
        // An IdentityHashMap forward map keeps two equal-but-distinct keys apart, so repairing one key's
        // stale reverse entry must not delete the other key's live one.
        final String k1 = new String("k");
        final String k2 = new String("k");
        final java.util.List<Integer> a = new java.util.ArrayList<>(java.util.List.of(1));
        final java.util.List<Integer> b = new java.util.ArrayList<>(java.util.List.of(2));
        final BiMap<String, java.util.List<Integer>> m = new BiMap<>(java.util.IdentityHashMap::new, HashMap::new);
        m.put(k1, a);
        m.put(k2, b);

        a.add(99); // the reverse lookup for a now misses

        m.put(k1, new java.util.ArrayList<>(java.util.List.of(7)));

        assertEquals(2, m.size());
        assertEquals(m.size(), m.inverse().size());
        assertSame(k2, m.getByValue(b)); // k2's reverse binding is untouched
        assertTrue(m.containsValue(b));
        assertSame(k1, m.getByValue(java.util.List.of(7)));
    }

    @Test
    public void testStaleEntryRepairWorksForAComparatorKeyedForwardMap() {
        // A comparator-keyed forward map decides key identity by its comparator, not by equals, so an
        // equals-based repair scan would never match and the stale entry would survive.
        final BiMap<String, java.util.List<Integer>> m = new BiMap<>(() -> new TreeMap<String, java.util.List<Integer>>(String.CASE_INSENSITIVE_ORDER),
                HashMap::new);
        final java.util.List<Integer> stored = new java.util.ArrayList<>(java.util.List.of(1));
        m.put("Key", stored);

        stored.add(9);

        assertSame(stored, m.put("key", new java.util.ArrayList<>(java.util.List.of(5))));

        assertEquals(1, m.size());
        assertEquals(m.size(), m.inverse().size());
        assertEquals("Key", m.getByValue(java.util.List.of(5))); // the key object keyMap still holds

        for (final String storedKey : m.inverse().values()) {
            assertNotNull(storedKey);
        }
    }

    @Test
    public void testStaleEntryRepairWorksForAnEquivalentButDistinctIncomingKey() {
        // The ordinary HashMap case the repair exists for, with an equivalent-but-not-identical incoming key.
        final BiMap<String, java.util.List<Integer>> m = new BiMap<>();
        final java.util.List<Integer> stored = new java.util.ArrayList<>(java.util.List.of(1));
        m.put(new String("a"), stored);

        stored.add(9);

        m.put(new String("a"), new java.util.ArrayList<>(java.util.List.of(5)));

        assertEquals(1, m.size());
        assertEquals(m.size(), m.inverse().size());
        assertEquals("a", m.getByValue(java.util.List.of(5)));
    }

    @Test
    public void testOverwritingRepairsAHalfFailedRemoveByValue() {
        // Class javadoc: overwriting the affected key makes the two directions consistent again - here after
        // the documented "a backing map rejected a write inside removeByValue" state, with an identity-keyed
        // forward map. No value is mutated and no map drops entries of its own accord.
        final class FlakyIdentity<K, V> extends java.util.IdentityHashMap<K, V> {
            private static final long serialVersionUID = 1L;

            private boolean rejectRemove;

            @Override
            public V remove(final Object k) {
                if (rejectRemove) {
                    rejectRemove = false;
                    throw new IllegalStateException("forward map rejected remove");
                }

                return super.remove(k);
            }
        }

        final FlakyIdentity<String, String> forward = new FlakyIdentity<>();
        final BiMap<String, String> m = new BiMap<>(() -> forward, HashMap::new);
        final String k1 = new String("k");
        final String k2 = new String("k");
        m.put(k1, "A");
        m.put(k2, "B");

        forward.rejectRemove = true;
        assertThrows(IllegalStateException.class, () -> m.removeByValue("A"));

        m.put(k1, "C"); // overwrite the affected key

        assertEquals(2, m.size());
        assertEquals(m.size(), m.inverse().size());
        assertSame(k2, m.getByValue("B"));
        assertSame(k1, m.getByValue("C"));
    }

    @Test
    public void testAbandonedRollbackIsReportedAsSuppressed() {
        // An abandoned undo must report why it was abandoned and must not commit the new value.
        final RejectingLinkedHashMap<String, String> reverse = new RejectingLinkedHashMap<>();
        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, () -> reverse);
        m.put("k1", "v1");

        reverse.rejectCount = -1;

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> m.put("k1", "v9"));
        assertEquals(1, e.getSuppressed().length, "the abandoned restore must be attached");

        assertEquals("v1", m.get("k1"), "the rejected new value must not be committed");
        assertEquals(1, m.size());
        assertEquals(0, m.inverse().size());
    }

    @Test
    public void testPutOverwriteRollbackRestoresTheOldValueAndBothMapsKeepTheirOrder() {
        // The oldValue != null half of the rollback: a plain overwrite whose reverse commit write is
        // rejected. Both the forward value and the old reverse binding must come back.
        final RejectingLinkedHashMap<String, String> reverse = new RejectingLinkedHashMap<>();
        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, () -> reverse);
        m.put("k1", "v1");
        m.put("k2", "v2");
        m.put("k3", "v3");

        reverse.rejectCount = 1; // reject only the commit, so every restoring write can still succeed

        assertThrows(IllegalStateException.class, () -> m.put("k2", "v9"));

        assertEquals("v1", m.get("k1"));
        assertEquals("v2", m.get("k2"));
        assertEquals("v3", m.get("k3"));
        assertEquals("k2", m.getByValue("v2"));
        assertFalse(m.containsValue("v9"));
        assertEquals(3, m.size());
        assertEquals(3, m.inverse().size());
        assertViewsAligned(m);

        // a plain put never re-inserts the forward key, so the forward order survives untouched; the undo
        // does re-insert into the reverse map, so the restored reverse entry moves to the end
        assertEquals(CommonUtil.asList("k1", "k2", "k3"), new java.util.ArrayList<>(m.keySet()));
        assertEquals(CommonUtil.asList("v1", "v3", "v2"), new java.util.ArrayList<>(m.inverse().keySet()));
    }

    @Test
    public void testEachRollbackStepIsGuardedSoAnEarlierFailureCannotDropTheDisplacedEntry() {
        // The displaced entry is the only piece of state that is destroyed outright if it is not restored,
        // so a restoring write that the backing map rejects must not abandon the remaining restores.
        final RejectingLinkedHashMap<String, String> reverse = new RejectingLinkedHashMap<>();
        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, () -> reverse);
        m.put("k1", "v1"); // the entry the forcePut below displaces
        m.put("k2", "v2"); // the key it overwrites, so an earlier restore step exists

        reverse.rejectCount = -1; // the reverse map stays down, so its restoring writes fail as well

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> m.forcePut("k2", "v1"));

        assertEquals(2, e.getSuppressed().length, "every abandoned restore must be reported");
        assertEquals("v1", m.get("k1"), "the displaced entry must not be dropped by an earlier failed restore");
        assertEquals("v2", m.get("k2"));
        assertEquals(2, m.size());
    }

    @Test
    public void testFailedPutDoesNotInsertAReverseBindingThatWasNeverThere() {
        // The reverse map below has already evicted v1 -> k1 of its own accord. A failed put must not
        // "restore" that binding: inserting it evicts a live one, so the failed put would destroy an
        // unrelated entry that was working before the call.
        final int[] reject = { 0 };

        final class BoundedRejecting extends LinkedHashMap<String, String> {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, String> eldest) {
                return size() > 2;
            }

            @Override
            public String put(final String key, final String value) {
                if (reject[0] > 0) {
                    reject[0]--;
                    throw new IllegalStateException("reverse put rejected");
                }

                return super.put(key, value);
            }
        }

        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, BoundedRejecting::new);
        m.put("k1", "v1");
        m.put("k2", "v2");
        m.put("k3", "v3"); // the reverse map has evicted v1 -> k1 by now
        assertEquals("k2", m.getByValue("v2"));

        reject[0] = 1;
        assertThrows(IllegalStateException.class, () -> m.put("k1", "v9"));

        // the forward map is untouched ...
        assertEquals("v1", m.get("k1"));
        assertEquals("v2", m.get("k2"));
        assertEquals("v3", m.get("k3"));
        // ... and so is the reverse map: nothing was removed from it, so nothing may be re-inserted
        assertEquals("k2", m.getByValue("v2"));
        assertEquals("k3", m.getByValue("v3"));
        assertNull(m.getByValue("v9"));
        assertEquals(2, m.inverse().size());
    }

    @Test
    public void testARestoreThrowingTheSameInstanceDoesNotMaskTheBackingMapFailure() {
        // A map that pre-allocates its failure throws one instance every time. addSuppressed(itself) raises
        // IllegalArgumentException "Self-suppression not permitted" - exactly the type put() documents for
        // its own contract violations - which would replace the backing map's own exception.
        final RejectingLinkedHashMap<String, String> reverse = new RejectingLinkedHashMap<>();
        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, () -> reverse);
        m.put("k1", "v1");

        reverse.cached = new IllegalStateException("cached singleton failure");
        reverse.rejectCount = -1;

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> m.put("k1", "v9"));
        assertSame(reverse.cached, thrown);
        assertEquals(0, thrown.getSuppressed().length);
        assertEquals("v1", m.get("k1"));
    }

    @Test
    public void testRemoveCannotRepairAnUnreachableReverseEntryButOverwritingCan() {
        // Class javadoc: a bare remove() does not repair a stale reverse entry - it cannot be found by value
        // either - so it makes the two sizes disagree instead. Overwriting the key, or clear(), repairs it.
        final BiMap<String, java.util.List<Integer>> damaged = new BiMap<>();
        final java.util.List<Integer> stale = new java.util.ArrayList<>(java.util.List.of(1));
        damaged.put("a", stale);
        damaged.put("b", new java.util.ArrayList<>(java.util.List.of(2)));
        stale.add(99); // the reverse lookup for this value now misses

        assertNull(damaged.getByValue(stale));
        assertSame(stale, damaged.remove("a"));
        assertEquals(1, damaged.size());
        assertEquals(2, damaged.inverse().size());
        assertNull(damaged.remove("a"));
        assertNull(damaged.removeByValue(stale));
        assertEquals(2, damaged.inverse().size());

        damaged.clear();
        assertEquals(0, damaged.size());
        assertEquals(0, damaged.inverse().size());

        final BiMap<String, java.util.List<Integer>> repaired = new BiMap<>();
        final java.util.List<Integer> stale2 = new java.util.ArrayList<>(java.util.List.of(1));
        repaired.put("a", stale2);
        stale2.add(99);

        assertSame(stale2, repaired.put("a", new java.util.ArrayList<>(java.util.List.of(5))));
        assertEquals(1, repaired.size());
        assertEquals(1, repaired.inverse().size());
        assertEquals("a", repaired.getByValue(java.util.List.of(5)));
    }

    @Test
    public void testForcePutNotPutRepairsTheDesyncLeftByAFailedRemove() {
        // Class javadoc: after a mutator that does not undo, re-binding the affected key to its value with
        // forcePut (or clear) restores consistency, while a plain put is rejected because the orphaned
        // reverse entry still binds that value.
        final boolean[] rejectRemove = { false };

        final class RemoveRejecting extends LinkedHashMap<String, String> {
            private static final long serialVersionUID = 1L;

            @Override
            public String remove(final Object key) {
                if (rejectRemove[0]) {
                    rejectRemove[0] = false;
                    throw new IllegalStateException("reverse remove rejected");
                }

                return super.remove(key);
            }
        }

        final BiMap<String, String> m = new BiMap<>(LinkedHashMap::new, RemoveRejecting::new);
        m.put("k1", "v1");
        m.put("k2", "v2");

        rejectRemove[0] = true;
        assertThrows(IllegalStateException.class, () -> m.remove("k1"));
        assertEquals(1, m.size());
        assertEquals(2, m.inverse().size());

        assertNull(m.remove("k1"));
        assertEquals(2, m.inverse().size(), "remove() cannot repair it");
        assertThrows(IllegalArgumentException.class, () -> m.put("k1", "v1"));
        assertEquals(2, m.inverse().size(), "put() cannot repair it either");

        assertNull(m.forcePut("k1", "v1"));
        assertEquals(2, m.size());
        assertEquals(2, m.inverse().size());
        assertEquals("k1", m.getByValue("v1"));
    }

    @Test
    public void testForcePutRestoresReverseEntryWhenForwardRemovalFails() {
        final boolean[] reject = { false };
        final Map<String, String> forward = new LinkedHashMap<>() {
            @Override
            public String remove(final Object key) {
                if (reject[0]) {
                    reject[0] = false;
                    throw new IllegalStateException("forward remove rejected");
                }
                return super.remove(key);
            }
        };
        final BiMap<String, String> map = new BiMap<>(() -> forward, LinkedHashMap::new);
        map.put("first", "one");
        map.put("second", "two");

        for (final String target : new String[] { "new", "second" }) {
            reject[0] = true;
            assertThrows(IllegalStateException.class, () -> map.forcePut(target, "one"));
            assertEquals(Map.of("first", "one", "second", "two"), map);
            assertEquals(Map.of("one", "first", "two", "second"), map.inverse());
            assertViewsAligned(map);
        }
    }

    @Test
    public void testForcePutRollbackPreservesTheDisplacedValueObject() {
        final RejectingHashMap<String, String> reverse = new RejectingHashMap<>();
        final BiMap<String, String> map = new BiMap<>(LinkedHashMap::new, () -> reverse);
        final String stored = new String("value");
        final String incoming = new String("value");
        map.put("first", stored);
        map.put("second", "other");

        for (final String target : new String[] { "new", "second" }) {
            reverse.rejectCount = 1;
            assertThrows(IllegalStateException.class, () -> map.forcePut(target, incoming));
            assertSame(stored, map.get("first"), "rollback must restore the original value, not the equal incoming object");
            assertSame(stored, map.inverse().keySet().stream().filter(stored::equals).findFirst().orElseThrow());
            assertEquals("other", map.get("second"));
            assertEquals(2, map.size());
            assertViewsAligned(map);
        }
    }

    @Test
    public void testFailedDisplacedForwardRestoreDoesNotSkipReverseRestore() {
        final boolean[] rejectRestore = { false };
        final Map<String, String> forward = new LinkedHashMap<>() {
            @Override
            public String put(final String key, final String value) {
                if (rejectRestore[0] && key.equals("first")) {
                    throw new IllegalArgumentException("forward restore rejected");
                }
                return super.put(key, value);
            }
        };
        final RejectingHashMap<String, String> reverse = new RejectingHashMap<>();
        final BiMap<String, String> map = new BiMap<>(() -> forward, () -> reverse);
        map.put("first", "one");
        reverse.rejectCount = 1;
        rejectRestore[0] = true;

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> map.forcePut("new", "one"));
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("forward restore rejected", thrown.getSuppressed()[0].getMessage());
        assertEquals("first", map.getByValue("one"), "each undo must be attempted even if an earlier one fails");
        assertFalse(map.containsKey("new"));
    }

    @Test
    public void testCopyRejectsSuppliersThatSwapTheOriginalBackingMaps() {
        final Map<String, String> forward = new HashMap<>();
        final Map<String, String> reverse = new HashMap<>();
        final int[] forwardCalls = { 0 };
        final int[] reverseCalls = { 0 };
        final BiMap<String, String> map = new BiMap<>(() -> forwardCalls[0]++ == 0 ? forward : reverse, () -> reverseCalls[0]++ == 0 ? reverse : forward);

        assertThrows(IllegalArgumentException.class, map::copy);
        assertThrows(IllegalArgumentException.class, () -> BiMap.copyOf(map));
        assertThrows(IllegalArgumentException.class, () -> map.inverse().copy());
        assertTrue(map.isEmpty());
        assertTrue(map.inverse().isEmpty());
    }

    @Test
    public void testEveryOfOverloadAcceptsRepeatingTheSameMapping() {
        // A duplicate value is a conflict only when it belongs to a different key.
        final java.util.List<BiMap<String, Integer>> maps = java.util.List.of(BiMap.of("a", 1, "a", 1), BiMap.of("a", 1, "a", 1, "a", 1),
                BiMap.of("a", 1, "a", 1, "a", 1, "a", 1), BiMap.of("a", 1, "a", 1, "a", 1, "a", 1, "a", 1),
                BiMap.of("a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1), BiMap.of("a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1),
                BiMap.of("a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1),
                BiMap.of("a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1),
                BiMap.of("a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1, "a", 1));

        for (final BiMap<String, Integer> map : maps) {
            assertEquals(Map.of("a", 1), map);
            assertEquals(Map.of(1, "a"), map.inverse());
        }
    }

    /** Fails exactly one selected write, either before or after changing its mapping. */
    private static final class FailingWriteMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private int failAt;
        private int writes;
        private boolean afterWrite;
        private Throwable failure;

        private void fail() {
            if (failure instanceof Error error) {
                throw error;
            }
            throw (RuntimeException) failure;
        }

        @Override
        public V put(final K key, final V value) {
            final boolean reject = ++writes == failAt;
            if (reject && !afterWrite) {
                fail();
            }
            final V previous = super.put(key, value);
            if (reject && afterWrite) {
                fail();
            }
            return previous;
        }

        @Override
        public V remove(final Object key) {
            final boolean reject = ++writes == failAt;
            if (reject && !afterWrite) {
                fail();
            }
            final V previous = super.remove(key);
            if (reject && afterWrite) {
                fail();
            }
            return previous;
        }
    }

    @Test
    public void testRollbackAtEveryForwardAndReverseWriteBoundary() {
        // All normal mutation shapes, both maps, before/after-write failures, and RuntimeException/Error.
        // Failure is one-shot so rollback itself succeeds; the original objects must all survive.
        for (int scenario = 0; scenario < 4; scenario++) {
            for (final boolean failForward : new boolean[] { false, true }) {
                for (final boolean afterWrite : new boolean[] { false, true }) {
                    for (final boolean error : new boolean[] { false, true }) {
                        for (int write = 1; write <= 3; write++) {
                            final FailingWriteMap<String, String> forward = new FailingWriteMap<>();
                            final FailingWriteMap<String, String> reverse = new FailingWriteMap<>();
                            final BiMap<String, String> map = new BiMap<>(() -> forward, () -> reverse);
                            map.put(new String("first"), new String("one"));
                            map.put(new String("second"), new String("two"));
                            map.put(new String("third"), new String("three"));
                            final Map<String, String> beforeForward = new LinkedHashMap<>(forward);
                            final Map<String, String> beforeReverse = new LinkedHashMap<>(reverse);
                            final FailingWriteMap<String, String> failing = failForward ? forward : reverse;
                            failing.failAt = failing.writes + write;
                            failing.afterWrite = afterWrite;
                            failing.failure = error ? new AssertionError("write rejected") : new IllegalStateException("write rejected");
                            final String target = new String(scenario % 2 == 0 ? "new" : "second");
                            final String value = new String(scenario < 2 ? "new value" : "one");
                            final String context = "scenario=" + scenario + ", forward=" + failForward + ", after=" + afterWrite + ", write=" + write;

                            try {
                                if (scenario < 2) {
                                    map.put(target, value);
                                } else {
                                    map.forcePut(target, value);
                                }
                                assertTrue(failing.writes < failing.failAt, context + ": selected write should have failed");
                            } catch (final RuntimeException | Error thrown) {
                                assertSame(failing.failure, thrown, context);
                                assertEquals(0, thrown.getSuppressed().length, context);
                                assertEquals(beforeForward, forward, context);
                                assertEquals(beforeReverse, reverse, context);
                                beforeForward.forEach((k, v) -> assertSame(v, map.get(k), context));
                                beforeReverse.forEach((v, k) -> assertSame(k, map.getByValue(v), context));
                                for (final String k : map.keySet()) {
                                    assertSame(k, map.getByValue(map.get(k)), context);
                                }
                                for (final String v : map.inverse().keySet()) {
                                    assertSame(v, map.get(map.getByValue(v)), context);
                                }
                            }
                            assertViewsAligned(map);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testFailedRepairRestoresAnOrphanWithoutCreatingAForwardEntry() {
        final FailingWriteMap<String, String> reverse = new FailingWriteMap<>();
        final BiMap<String, String> map = new BiMap<>(LinkedHashMap::new, () -> reverse);
        final String stored = new String("one");
        map.put("first", stored);
        map.put("second", "two");
        reverse.failure = new IllegalStateException("reverse write rejected");
        reverse.failAt = reverse.writes + 1;

        // remove() has no rollback: rejecting its reverse removal leaves only the reverse binding.
        assertThrows(IllegalStateException.class, () -> map.remove("first"));
        assertFalse(map.containsKey("first"));
        assertEquals("first", map.getByValue(stored));

        for (final String target : new String[] { "first", "new" }) {
            reverse.failAt = reverse.writes + 2; // allow removing the orphan; reject the replacement put
            assertThrows(IllegalStateException.class, () -> map.forcePut(target, new String("one")));
            assertEquals(Map.of("second", "two"), map);
            assertEquals(Map.of("one", "first", "two", "second"), map.inverse());
            assertSame(stored, map.inverse().keySet().stream().filter(stored::equals).findFirst().orElseThrow());
        }
    }
}
