package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ImmutableSortedMap2025Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.empty();
        assertNotNull(map);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEmpty_sameInstance() {
        ImmutableSortedMap<String, Integer> map1 = ImmutableSortedMap.empty();
        ImmutableSortedMap<String, Integer> map2 = ImmutableSortedMap.empty();
        assertSame(map1, map2);
    }

    @Test
    public void testOf_oneEntry() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("count", 42);
        assertEquals(1, map.size());
        assertEquals(42, map.get("count"));
        assertEquals("count", map.firstKey());
        assertEquals("count", map.lastKey());
    }

    @Test
    public void testOf_twoEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(2, "two", 1, "one");
        assertEquals(2, map.size());
        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertEquals(1, map.firstKey());
        assertEquals(2, map.lastKey());
    }

    @Test
    public void testOf_twoEntries_sorted() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("b", 2, "a", 1);
        assertEquals(2, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("b", map.lastKey());
    }

    @Test
    public void testOf_threeEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("c", 3, "a", 1, "b", 2);
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
    }

    @Test
    public void testOf_fourEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(4, "four", 1, "one", 3, "three", 2, "two");
        assertEquals(4, map.size());
        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertEquals("three", map.get(3));
        assertEquals("four", map.get(4));
        assertEquals(1, map.firstKey());
        assertEquals(4, map.lastKey());
    }

    @Test
    public void testOf_fiveEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("e", 5, "c", 3, "a", 1, "d", 4, "b", 2);
        assertEquals(5, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(5, map.get("e"));
        assertEquals("a", map.firstKey());
        assertEquals("e", map.lastKey());
    }

    @Test
    public void testOf_sixEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(6, "six", 3, "three", 1, "one", 5, "five", 2, "two", 4, "four");
        assertEquals(6, map.size());
        assertEquals("one", map.get(1));
        assertEquals("six", map.get(6));
        assertEquals(1, map.firstKey());
        assertEquals(6, map.lastKey());
    }

    @Test
    public void testOf_sevenEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("g", 7, "d", 4, "a", 1, "f", 6, "c", 3, "e", 5, "b", 2);
        assertEquals(7, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(7, map.get("g"));
        assertEquals("a", map.firstKey());
        assertEquals("g", map.lastKey());
    }

    @Test
    public void testOf_eightEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(8, "eight", 4, "four", 1, "one", 6, "six", 3, "three", 7, "seven", 2, "two", 5, "five");
        assertEquals(8, map.size());
        assertEquals("one", map.get(1));
        assertEquals("eight", map.get(8));
        assertEquals(1, map.firstKey());
        assertEquals(8, map.lastKey());
    }

    @Test
    public void testOf_nineEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("i", 9, "e", 5, "a", 1, "h", 8, "d", 4, "g", 7, "c", 3, "f", 6, "b", 2);
        assertEquals(9, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(9, map.get("i"));
        assertEquals("a", map.firstKey());
        assertEquals("i", map.lastKey());
    }

    @Test
    public void testOf_tenEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(10, "ten", 5, "five", 1, "one", 8, "eight", 3, "three", 7, "seven", 2, "two", 9, "nine",
                4, "four", 6, "six");
        assertEquals(10, map.size());
        assertEquals("one", map.get(1));
        assertEquals("ten", map.get(10));
        assertEquals(1, map.firstKey());
        assertEquals(10, map.lastKey());
    }

    @Test
    public void testOf_duplicateKeys() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "a", 2);
        assertEquals(1, map.size());
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testCopyOf_fromImmutableSortedMap() {
        ImmutableSortedMap<String, Integer> original = ImmutableSortedMap.of("a", 1, "b", 2);
        ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOf(original);
        assertSame(original, copy);
    }

    @Test
    public void testCopyOf_fromNull() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.copyOf(null);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_fromEmptyMap() {
        Map<String, Integer> emptyMap = new HashMap<>();
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.copyOf(emptyMap);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_fromHashMap() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("b", 2);
        mutable.put("a", 1);
        mutable.put("c", 3);

        ImmutableSortedMap<String, Integer> immutable = ImmutableSortedMap.copyOf(mutable);
        assertEquals(3, immutable.size());
        assertEquals(1, immutable.get("a"));
        assertEquals(2, immutable.get("b"));
        assertEquals(3, immutable.get("c"));
        assertEquals("a", immutable.firstKey());
        assertEquals("c", immutable.lastKey());

        mutable.put("d", 4);
        assertEquals(3, immutable.size());
        assertFalse(immutable.containsKey("d"));
    }

    @Test
    public void testCopyOf_fromSortedMap() {
        SortedMap<String, Integer> sortedMap = new TreeMap<>();
        sortedMap.put("z", 26);
        sortedMap.put("a", 1);
        sortedMap.put("m", 13);

        ImmutableSortedMap<String, Integer> immutable = ImmutableSortedMap.copyOf(sortedMap);
        assertEquals(3, immutable.size());
        assertEquals("a", immutable.firstKey());
        assertEquals("z", immutable.lastKey());

        sortedMap.put("b", 2);
        assertEquals(3, immutable.size());
        assertFalse(immutable.containsKey("b"));
    }

    @Test
    public void testCopyOf_fromSortedMapWithComparator() {
        SortedMap<String, Integer> sortedMap = new TreeMap<>(Comparator.reverseOrder());
        sortedMap.put("a", 1);
        sortedMap.put("b", 2);
        sortedMap.put("c", 3);

        ImmutableSortedMap<String, Integer> immutable = ImmutableSortedMap.copyOf(sortedMap);
        assertEquals(3, immutable.size());
        assertNotNull(immutable.comparator());
        assertEquals("c", immutable.firstKey());
        assertEquals("a", immutable.lastKey());
    }

    @Test
    public void testWrap_fromImmutableSortedMap() {
        ImmutableSortedMap<String, Integer> original = ImmutableSortedMap.of("a", 1);
        ImmutableSortedMap<String, Integer> wrapped = ImmutableSortedMap.wrap(original);
        assertSame(original, wrapped);
    }

    @Test
    public void testWrap_fromNull() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.wrap((SortedMap<String, Integer>) null);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testWrap_fromSortedMap() {
        SortedMap<Integer, String> mutable = new TreeMap<>();
        mutable.put(1, "one");
        mutable.put(2, "two");

        ImmutableSortedMap<Integer, String> wrapped = ImmutableSortedMap.wrap(mutable);
        assertEquals(2, wrapped.size());

        mutable.put(3, "three");
        assertEquals(3, wrapped.size());
        assertTrue(wrapped.containsKey(3));
    }

    @Test
    public void testWrap_mapThrowsException() {
        Map<String, Integer> regularMap = new HashMap<>();
        assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableSortedMap.wrap(regularMap);
        });
    }

    @Test
    public void testComparator_naturalOrder() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        assertNull(map.comparator());
    }

    @Test
    public void testComparator_customOrder() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        SortedMap<String, Integer> customMap = new TreeMap<>(reverseOrder);
        customMap.put("a", 1);
        customMap.put("b", 2);

        ImmutableSortedMap<String, Integer> immutable = ImmutableSortedMap.wrap(customMap);
        assertNotNull(immutable.comparator());
        assertSame(reverseOrder, immutable.comparator());
    }

    @Test
    public void testSubMap() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two", 3, "three", 4, "four", 5, "five");

        ImmutableSortedMap<Integer, String> subMap = map.subMap(2, 4);
        assertEquals(2, subMap.size());
        assertTrue(subMap.containsKey(2));
        assertTrue(subMap.containsKey(3));
        assertFalse(subMap.containsKey(1));
        assertFalse(subMap.containsKey(4));
        assertEquals("two", subMap.get(2));
        assertEquals("three", subMap.get(3));
    }

    @Test
    public void testSubMap_emptyRange() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        ImmutableSortedMap<String, Integer> subMap = map.subMap("b", "b");
        assertTrue(subMap.isEmpty());
    }

    @Test
    public void testSubMap_invalidRange() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two", 3, "three");
        assertThrows(IllegalArgumentException.class, () -> {
            map.subMap(3, 1);
        });
    }

    @Test
    public void testSubMap_isImmutable() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3, "d", 4);
        ImmutableSortedMap<String, Integer> subMap = map.subMap("b", "d");

        assertThrows(UnsupportedOperationException.class, () -> {
            subMap.put("x", 99);
        });
    }

    @Test
    public void testHeadMap() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3, "d", 4);

        ImmutableSortedMap<String, Integer> headMap = map.headMap("c");
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey("a"));
        assertTrue(headMap.containsKey("b"));
        assertFalse(headMap.containsKey("c"));
        assertFalse(headMap.containsKey("d"));
        assertEquals(1, headMap.get("a"));
        assertEquals(2, headMap.get("b"));
    }

    @Test
    public void testHeadMap_empty() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(5, "five", 10, "ten", 15, "fifteen");
        ImmutableSortedMap<Integer, String> headMap = map.headMap(1);
        assertTrue(headMap.isEmpty());
    }

    @Test
    public void testHeadMap_isImmutable() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        ImmutableSortedMap<String, Integer> headMap = map.headMap("c");

        assertThrows(UnsupportedOperationException.class, () -> {
            headMap.put("x", 99);
        });
    }

    @Test
    public void testTailMap() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(10, "ten", 20, "twenty", 30, "thirty", 40, "forty");

        ImmutableSortedMap<Integer, String> tailMap = map.tailMap(25);
        assertEquals(2, tailMap.size());
        assertTrue(tailMap.containsKey(30));
        assertTrue(tailMap.containsKey(40));
        assertFalse(tailMap.containsKey(10));
        assertFalse(tailMap.containsKey(20));
        assertEquals("thirty", tailMap.get(30));
        assertEquals("forty", tailMap.get(40));
    }

    @Test
    public void testTailMap_fromFirstKey() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        ImmutableSortedMap<String, Integer> tailMap = map.tailMap("a");
        assertEquals(3, tailMap.size());
        assertEquals(map.size(), tailMap.size());
    }

    @Test
    public void testTailMap_empty() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two", 3, "three");
        ImmutableSortedMap<Integer, String> tailMap = map.tailMap(100);
        assertTrue(tailMap.isEmpty());
    }

    @Test
    public void testTailMap_isImmutable() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        ImmutableSortedMap<String, Integer> tailMap = map.tailMap("b");

        assertThrows(UnsupportedOperationException.class, () -> {
            tailMap.put("x", 99);
        });
    }

    @Test
    public void testFirstKey() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("banana", 2, "apple", 1, "cherry", 3);
        assertEquals("apple", map.firstKey());
    }

    @Test
    public void testFirstKey_singleElement() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(42, "answer");
        assertEquals(42, map.firstKey());
    }

    @Test
    public void testFirstKey_emptyMap() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.empty();
        assertThrows(NoSuchElementException.class, () -> {
            map.firstKey();
        });
    }

    @Test
    public void testLastKey() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(3, "three", 1, "one", 4, "four", 1, "uno", 5, "five");
        assertEquals(5, map.lastKey());
    }

    @Test
    public void testLastKey_singleElement() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("only", 1);
        assertEquals("only", map.lastKey());
    }

    @Test
    public void testLastKey_emptyMap() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.empty();
        assertThrows(NoSuchElementException.class, () -> {
            map.lastKey();
        });
    }

    @Test
    public void testImmutability_put() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        assertThrows(UnsupportedOperationException.class, () -> {
            map.put("c", 3);
        });
    }

    @Test
    public void testImmutability_remove() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        assertThrows(UnsupportedOperationException.class, () -> {
            map.remove("a");
        });
    }

    @Test
    public void testImmutability_clear() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        assertThrows(UnsupportedOperationException.class, () -> {
            map.clear();
        });
    }

    @Test
    public void testImmutability_putAll() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1);
        Map<String, Integer> other = new HashMap<>();
        other.put("b", 2);

        assertThrows(UnsupportedOperationException.class, () -> {
            map.putAll(other);
        });
    }

    @Test
    public void testSorting_naturalOrder() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(5, "five", 2, "two", 8, "eight", 1, "one", 3, "three");

        Integer[] expectedKeys = { 1, 2, 3, 5, 8 };
        int index = 0;
        for (Integer key : map.keySet()) {
            assertEquals(expectedKeys[index++], key);
        }
    }

    @Test
    public void testSorting_stringNaturalOrder() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("zebra", 1, "apple", 2, "mango", 3, "banana", 4);

        String[] expectedKeys = { "apple", "banana", "mango", "zebra" };
        int index = 0;
        for (String key : map.keySet()) {
            assertEquals(expectedKeys[index++], key);
        }
    }

    @Test
    public void testWithNullValues() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", null, "b", 2);
        assertEquals(2, map.size());
        assertNull(map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testEntrySet() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.entrySet().size());

        String[] expectedKeys = { "a", "b", "c" };
        int index = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            assertEquals(expectedKeys[index++], entry.getKey());
        }
    }

    @Test
    public void testKeySet() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(3, "three", 1, "one", 2, "two");
        assertEquals(3, map.keySet().size());
        assertTrue(map.keySet().contains(1));
        assertTrue(map.keySet().contains(2));
        assertTrue(map.keySet().contains(3));
    }

    @Test
    public void testValues() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.values().size());
        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
        assertTrue(map.values().contains(3));
    }

    @Test
    public void testContainsKey() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertFalse(map.containsKey("c"));
    }

    @Test
    public void testContainsValue() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertFalse(map.containsValue(3));
    }

    @Test
    public void testGet() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("key", 42);
        assertEquals(42, map.get("key"));
        assertNull(map.get("nonexistent"));
    }

    @Test
    public void testSize() {
        ImmutableSortedMap<String, Integer> empty = ImmutableSortedMap.empty();
        assertEquals(0, empty.size());

        ImmutableSortedMap<String, Integer> one = ImmutableSortedMap.of("a", 1);
        assertEquals(1, one.size());

        ImmutableSortedMap<String, Integer> three = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, three.size());
    }

    @Test
    public void testIsEmpty() {
        ImmutableSortedMap<String, Integer> empty = ImmutableSortedMap.empty();
        assertTrue(empty.isEmpty());

        ImmutableSortedMap<String, Integer> notEmpty = ImmutableSortedMap.of("a", 1);
        assertFalse(notEmpty.isEmpty());
    }

    @Test
    public void testEquals() {
        ImmutableSortedMap<String, Integer> map1 = ImmutableSortedMap.of("a", 1, "b", 2);
        ImmutableSortedMap<String, Integer> map2 = ImmutableSortedMap.of("a", 1, "b", 2);
        ImmutableSortedMap<String, Integer> map3 = ImmutableSortedMap.of("a", 1, "c", 3);

        assertEquals(map1, map2);
        assertNotSame(map1, map2);
        assertFalse(map1.equals(map3));
    }

    @Test
    public void testHashCode() {
        ImmutableSortedMap<String, Integer> map1 = ImmutableSortedMap.of("a", 1, "b", 2);
        ImmutableSortedMap<String, Integer> map2 = ImmutableSortedMap.of("a", 1, "b", 2);

        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testToString() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);
        String str = map.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("1"));
    }
}
