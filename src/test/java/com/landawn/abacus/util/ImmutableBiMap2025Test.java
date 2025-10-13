package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ImmutableBiMap2025Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableBiMap<String, Integer> empty = ImmutableBiMap.empty();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
    }

    @Test
    public void testEmptyReturnsSameInstance() {
        ImmutableBiMap<String, Integer> empty1 = ImmutableBiMap.empty();
        ImmutableBiMap<String, Integer> empty2 = ImmutableBiMap.empty();
        assertSame(empty1, empty2);
    }

    @Test
    public void testOf1Entry() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);
        assertNotNull(biMap);
        assertEquals(1, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals("one", biMap.getByValue(1));
    }

    @Test
    public void testOf1EntryWithNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            ImmutableBiMap.of(null, 1);
        });
    }

    @Test
    public void testOf1EntryWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ImmutableBiMap.of("one", null);
        });
    }

    @Test
    public void testOf2Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);
        assertNotNull(biMap);
        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals("one", biMap.getByValue(1));
        assertEquals("two", biMap.getByValue(2));
    }

    @Test
    public void testOf2EntriesWithDuplicateValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ImmutableBiMap.of("one", 1, "two", 1);
        });
    }

    @Test
    public void testOf3Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3);
        assertNotNull(biMap);
        assertEquals(3, biMap.size());
        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));
    }

    @Test
    public void testOf3EntriesWithDuplicateValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ImmutableBiMap.of("one", 1, "two", 2, "three", 1);
        });
    }

    @Test
    public void testOf4Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4);
        assertNotNull(biMap);
        assertEquals(4, biMap.size());
        assertEquals(4, biMap.get("four"));
    }

    @Test
    public void testOf5Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
        assertNotNull(biMap);
        assertEquals(5, biMap.size());
        assertEquals(5, biMap.get("five"));
    }

    @Test
    public void testOf6Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6);
        assertNotNull(biMap);
        assertEquals(6, biMap.size());
        assertEquals(6, biMap.get("six"));
    }

    @Test
    public void testOf7Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7);
        assertNotNull(biMap);
        assertEquals(7, biMap.size());
        assertEquals(7, biMap.get("seven"));
    }

    @Test
    public void testOf8Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8);
        assertNotNull(biMap);
        assertEquals(8, biMap.size());
        assertEquals(8, biMap.get("eight"));
    }

    @Test
    public void testOf9Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8,
                "nine", 9);
        assertNotNull(biMap);
        assertEquals(9, biMap.size());
        assertEquals(9, biMap.get("nine"));
    }

    @Test
    public void testOf10Entries() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8,
                "nine", 9, "ten", 10);
        assertNotNull(biMap);
        assertEquals(10, biMap.size());
        assertEquals(10, biMap.get("ten"));
    }

    @Test
    public void testCopyOf() {
        BiMap<String, Integer> mutableBiMap = new BiMap<>();
        mutableBiMap.put("one", 1);
        mutableBiMap.put("two", 2);
        mutableBiMap.put("three", 3);

        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(mutableBiMap);
        assertNotNull(immutableBiMap);
        assertEquals(3, immutableBiMap.size());
        assertEquals(1, immutableBiMap.get("one"));
        assertEquals(2, immutableBiMap.get("two"));
        assertEquals(3, immutableBiMap.get("three"));
    }

    @Test
    public void testCopyOfIsDefensiveCopy() {
        BiMap<String, Integer> mutableBiMap = new BiMap<>();
        mutableBiMap.put("one", 1);
        mutableBiMap.put("two", 2);

        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(mutableBiMap);

        mutableBiMap.put("three", 3);

        assertEquals(2, immutableBiMap.size());
        assertNull(immutableBiMap.get("three"));
    }

    @Test
    public void testCopyOfEmptyBiMap() {
        BiMap<String, Integer> emptyBiMap = new BiMap<>();
        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(emptyBiMap);

        assertNotNull(immutableBiMap);
        assertTrue(immutableBiMap.isEmpty());
        assertSame(ImmutableBiMap.empty(), immutableBiMap);
    }

    @Test
    public void testCopyOfNullBiMap() {
        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(null);

        assertNotNull(immutableBiMap);
        assertTrue(immutableBiMap.isEmpty());
        assertSame(ImmutableBiMap.empty(), immutableBiMap);
    }

    @Test
    public void testWrap() {
        BiMap<String, Integer> mutableBiMap = new BiMap<>();
        mutableBiMap.put("one", 1);
        mutableBiMap.put("two", 2);

        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.wrap(mutableBiMap);
        assertNotNull(immutableBiMap);
        assertEquals(2, immutableBiMap.size());
        assertEquals(1, immutableBiMap.get("one"));
        assertEquals(2, immutableBiMap.get("two"));
    }

    @Test
    public void testWrapIsBackedByOriginal() {
        BiMap<String, Integer> mutableBiMap = new BiMap<>();
        mutableBiMap.put("one", 1);

        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.wrap(mutableBiMap);

        mutableBiMap.put("two", 2);

        assertEquals(2, immutableBiMap.size());
        assertEquals(2, immutableBiMap.get("two"));
    }

    @Test
    public void testWrapNull() {
        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.wrap(null);

        assertNotNull(immutableBiMap);
        assertTrue(immutableBiMap.isEmpty());
        assertSame(ImmutableBiMap.empty(), immutableBiMap);
    }

    @Test
    public void testGetByValue() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3);

        assertEquals("one", biMap.getByValue(1));
        assertEquals("two", biMap.getByValue(2));
        assertEquals("three", biMap.getByValue(3));
    }

    @Test
    public void testGetByValueNotFound() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        assertNull(biMap.getByValue(3));
        assertNull(biMap.getByValue(100));
    }

    @Test
    public void testGetByValueWithNull() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);

        assertNull(biMap.getByValue(null));
    }

    @Test
    public void testImmutabilityPut() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);

        assertThrows(UnsupportedOperationException.class, () -> {
            biMap.put("two", 2);
        });
    }

    @Test
    public void testImmutabilityRemove() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);

        assertThrows(UnsupportedOperationException.class, () -> {
            biMap.remove("one");
        });
    }

    @Test
    public void testImmutabilityClear() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);

        assertThrows(UnsupportedOperationException.class, () -> {
            biMap.clear();
        });
    }

    @Test
    public void testGet() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertNull(biMap.get("three"));
    }

    @Test
    public void testContainsKey() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        assertTrue(biMap.containsKey("one"));
        assertTrue(biMap.containsKey("two"));
        assertFalse(biMap.containsKey("three"));
    }

    @Test
    public void testContainsValue() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        assertTrue(biMap.containsValue(1));
        assertTrue(biMap.containsValue(2));
        assertFalse(biMap.containsValue(3));
    }

    @Test
    public void testKeySet() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        Set<String> keys = biMap.keySet();
        assertNotNull(keys);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
    }

    @Test
    public void testValues() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        Collection<Integer> values = biMap.values();
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEntrySet() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

        Set<java.util.Map.Entry<String, Integer>> entries = biMap.entrySet();
        assertNotNull(entries);
        assertEquals(2, entries.size());
    }

    @Test
    public void testIsEmpty() {
        ImmutableBiMap<String, Integer> empty = ImmutableBiMap.empty();
        assertTrue(empty.isEmpty());

        ImmutableBiMap<String, Integer> notEmpty = ImmutableBiMap.of("one", 1);
        assertFalse(notEmpty.isEmpty());
    }

    @Test
    public void testSize() {
        ImmutableBiMap<String, Integer> empty = ImmutableBiMap.empty();
        assertEquals(0, empty.size());

        ImmutableBiMap<String, Integer> one = ImmutableBiMap.of("one", 1);
        assertEquals(1, one.size());

        ImmutableBiMap<String, Integer> two = ImmutableBiMap.of("one", 1, "two", 2);
        assertEquals(2, two.size());
    }

    @Test
    public void testBidirectionalLookup() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3);

        assertEquals(1, biMap.get("one"));
        assertEquals(2, biMap.get("two"));
        assertEquals(3, biMap.get("three"));

        assertEquals("one", biMap.getByValue(1));
        assertEquals("two", biMap.getByValue(2));
        assertEquals("three", biMap.getByValue(3));
    }

    @Test
    public void testEqualsAndHashCode() {
        ImmutableBiMap<String, Integer> biMap1 = ImmutableBiMap.of("one", 1, "two", 2);
        ImmutableBiMap<String, Integer> biMap2 = ImmutableBiMap.of("one", 1, "two", 2);

        assertEquals(biMap1, biMap2);
        assertEquals(biMap1.hashCode(), biMap2.hashCode());
    }

    @Test
    public void testToString() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);
        String str = biMap.toString();

        assertNotNull(str);
        assertTrue(str.contains("one"));
        assertTrue(str.contains("1"));
    }

    @Test
    public void testCopyOfPreservesOrder() {
        BiMap<String, Integer> mutableBiMap = new BiMap<>();
        mutableBiMap.put("one", 1);
        mutableBiMap.put("two", 2);
        mutableBiMap.put("three", 3);

        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(mutableBiMap);

        assertEquals(mutableBiMap.size(), immutableBiMap.size());
        for (String key : mutableBiMap.keySet()) {
            assertEquals(mutableBiMap.get(key), immutableBiMap.get(key));
        }
    }

    @Test
    public void testMultipleGetByValue() {
        ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);

        assertEquals("one", biMap.getByValue(1));
        assertEquals("two", biMap.getByValue(2));
        assertEquals("three", biMap.getByValue(3));
        assertEquals("four", biMap.getByValue(4));
        assertEquals("five", biMap.getByValue(5));
        assertNull(biMap.getByValue(6));
    }
}
