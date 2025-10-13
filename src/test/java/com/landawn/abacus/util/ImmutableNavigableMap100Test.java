package com.landawn.abacus.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableNavigableMap100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableNavigableMap<String, Integer> emptyMap = ImmutableNavigableMap.empty();
        Assertions.assertTrue(emptyMap.isEmpty());
        Assertions.assertEquals(0, emptyMap.size());
        Assertions.assertNull(emptyMap.lowerEntry("any"));
        Assertions.assertNull(emptyMap.higherEntry("any"));
    }

    @Test
    public void testOf_SingleEntry() {
        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.of("one", 1);
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals("one", map.firstKey());
        Assertions.assertEquals("one", map.lastKey());
    }

    @Test
    public void testOf_TwoEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(2, "two", 1, "one");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.firstKey());
        Assertions.assertEquals(2, map.lastKey());
        Assertions.assertEquals("one", map.get(1));
        Assertions.assertEquals("two", map.get(2));
    }

    @Test
    public void testOf_ThreeEntries() {
        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.of("b", 2, "a", 1, "c", 3);
        Assertions.assertEquals(3, map.size());
        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("a", keys.next());
        Assertions.assertEquals("b", keys.next());
        Assertions.assertEquals("c", keys.next());
    }

    @Test
    public void testOf_FourEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(4, "four", 2, "two", 3, "three", 1, "one");
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(1, map.firstKey());
        Assertions.assertEquals(4, map.lastKey());
    }

    @Test
    public void testOf_FiveEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(5, "five", 3, "three", 1, "one", 4, "four", 2, "two");
        Assertions.assertEquals(5, map.size());
        Assertions.assertTrue(map.containsKey(3));
    }

    @Test
    public void testOf_SixEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(6, "six", 5, "five", 4, "four", 3, "three", 2, "two", 1, "one");
        Assertions.assertEquals(6, map.size());
    }

    @Test
    public void testOf_SevenEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(7, map.size());
    }

    @Test
    public void testOf_EightEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(8, "8", 7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(8, map.size());
    }

    @Test
    public void testOf_NineEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(9, "9", 8, "8", 7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(9, map.size());
    }

    @Test
    public void testOf_TenEntries() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(10, "10", 9, "9", 8, "8", 7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(10, map.size());
        Assertions.assertEquals(1, map.firstKey());
        Assertions.assertEquals(10, map.lastKey());
    }

    @Test
    public void testCopyOf() {
        SortedMap<String, Integer> source = new TreeMap<>();
        source.put("c", 3);
        source.put("a", 1);
        source.put("b", 2);

        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.copyOf(source);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("a", map.firstKey());
        Assertions.assertEquals("c", map.lastKey());
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableNavigableMap<String, Integer> original = ImmutableNavigableMap.of("a", 1);
        ImmutableNavigableMap<String, Integer> copy = ImmutableNavigableMap.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.copyOf(new TreeMap<>());
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.copyOf(null);
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testWrap() {
        NavigableMap<String, Integer> mutable = new TreeMap<>();
        mutable.put("b", 2);
        mutable.put("a", 1);

        ImmutableNavigableMap<String, Integer> wrapped = ImmutableNavigableMap.wrap(mutable);
        Assertions.assertEquals(2, wrapped.size());

        mutable.put("c", 3);
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.containsKey("c"));
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableNavigableMap<String, Integer> original = ImmutableNavigableMap.of("a", 1);
        ImmutableNavigableMap<String, Integer> wrapped = ImmutableNavigableMap.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableNavigableMap<String, Integer> wrapped = ImmutableNavigableMap.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_SortedMap_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableNavigableMap.wrap((SortedMap<String, Integer>) new TreeMap<String, Integer>());
        });
    }

    @Test
    public void testLowerEntry() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");

        Assertions.assertNull(map.lowerEntry(1));
        ImmutableEntry<Integer, String> entry = map.lowerEntry(3);
        Assertions.assertEquals(1, entry.getKey());
        Assertions.assertEquals("one", entry.getValue());

        entry = map.lowerEntry(6);
        Assertions.assertEquals(5, entry.getKey());
        Assertions.assertEquals("five", entry.getValue());
    }

    @Test
    public void testLowerKey() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        Assertions.assertNull(map.lowerKey(1));
        Assertions.assertEquals(1, map.lowerKey(2));
        Assertions.assertEquals(1, map.lowerKey(3));
        Assertions.assertEquals(3, map.lowerKey(4));
        Assertions.assertEquals(5, map.lowerKey(10));
    }

    @Test
    public void testFloorEntry() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        Assertions.assertNull(map.floorEntry(0));
        ImmutableEntry<Integer, String> entry = map.floorEntry(3);
        Assertions.assertEquals(3, entry.getKey());
        Assertions.assertEquals("three", entry.getValue());

        entry = map.floorEntry(4);
        Assertions.assertEquals(3, entry.getKey());
    }

    @Test
    public void testFloorKey() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        Assertions.assertNull(map.floorKey(0));
        Assertions.assertEquals(1, map.floorKey(1));
        Assertions.assertEquals(1, map.floorKey(2));
        Assertions.assertEquals(3, map.floorKey(3));
        Assertions.assertEquals(5, map.floorKey(10));
    }

    @Test
    public void testCeilingEntry() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        ImmutableEntry<Integer, String> entry = map.ceilingEntry(0);
        Assertions.assertEquals(1, entry.getKey());

        entry = map.ceilingEntry(3);
        Assertions.assertEquals(3, entry.getKey());

        entry = map.ceilingEntry(4);
        Assertions.assertEquals(5, entry.getKey());

        Assertions.assertNull(map.ceilingEntry(6));
    }

    @Test
    public void testCeilingKey() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        Assertions.assertEquals(1, map.ceilingKey(0));
        Assertions.assertEquals(1, map.ceilingKey(1));
        Assertions.assertEquals(3, map.ceilingKey(3));
        Assertions.assertNull(map.ceilingKey(6));
    }

    @Test
    public void testHigherEntry() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        ImmutableEntry<Integer, String> entry = map.higherEntry(0);
        Assertions.assertEquals(1, entry.getKey());

        entry = map.higherEntry(1);
        Assertions.assertEquals(3, entry.getKey());

        entry = map.higherEntry(3);
        Assertions.assertEquals(5, entry.getKey());

        Assertions.assertNull(map.higherEntry(5));
    }

    @Test
    public void testHigherKey() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");

        Assertions.assertEquals(1, map.higherKey(0));
        Assertions.assertEquals(3, map.higherKey(1));
        Assertions.assertEquals(3, map.higherKey(2));
        Assertions.assertEquals(5, map.higherKey(3));
        Assertions.assertNull(map.higherKey(5));
    }

    @Test
    public void testFirstEntry() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(3, "three", 1, "one", 2, "two");
        ImmutableEntry<Integer, String> entry = map.firstEntry();
        Assertions.assertEquals(1, entry.getKey());
        Assertions.assertEquals("one", entry.getValue());
    }

    @Test
    public void testFirstEntry_Empty() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.empty();
        Assertions.assertNull(map.firstEntry());
    }

    @Test
    public void testLastEntry() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(3, "three", 1, "one", 2, "two");
        ImmutableEntry<Integer, String> entry = map.lastEntry();
        Assertions.assertEquals(3, entry.getKey());
        Assertions.assertEquals("three", entry.getValue());
    }

    @Test
    public void testLastEntry_Empty() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.empty();
        Assertions.assertNull(map.lastEntry());
    }

    @Test
    public void testPollFirstEntry_ThrowsUnsupported() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 2, "two");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.pollFirstEntry());
    }

    @Test
    public void testPollLastEntry_ThrowsUnsupported() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 2, "two");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.pollLastEntry());
    }

    @Test
    public void testDescendingMap() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 2, "two", 3, "three");
        ImmutableNavigableMap<Integer, String> descending = map.descendingMap();

        Assertions.assertEquals(3, descending.size());
        Assertions.assertEquals(3, descending.firstKey());
        Assertions.assertEquals(1, descending.lastKey());

        Iterator<Integer> keys = descending.keySet().iterator();
        Assertions.assertEquals(3, keys.next());
        Assertions.assertEquals(2, keys.next());
        Assertions.assertEquals(1, keys.next());
    }

    @Test
    public void testNavigableKeySet() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");
        ImmutableNavigableSet<Integer> keySet = map.navigableKeySet();

        Assertions.assertEquals(3, keySet.size());
        Assertions.assertEquals(1, keySet.first());
        Assertions.assertEquals(5, keySet.last());
        Assertions.assertEquals(3, keySet.lower(5));
        Assertions.assertEquals(3, keySet.ceiling(2));
    }

    @Test
    public void testDescendingKeySet() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five");
        ImmutableNavigableSet<Integer> descKeys = map.descendingKeySet();

        Assertions.assertEquals(3, descKeys.size());
        Assertions.assertEquals(5, descKeys.first());
        Assertions.assertEquals(1, descKeys.last());

        Iterator<Integer> iter = descKeys.iterator();
        Assertions.assertEquals(5, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(1, iter.next());
    }

    @Test
    public void testSubMap_Inclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven", 9, "nine");
        ImmutableNavigableMap<Integer, String> sub = map.subMap(3, true, 7, true);

        Assertions.assertEquals(3, sub.size());
        Assertions.assertTrue(sub.containsKey(3));
        Assertions.assertTrue(sub.containsKey(5));
        Assertions.assertTrue(sub.containsKey(7));
        Assertions.assertFalse(sub.containsKey(1));
        Assertions.assertFalse(sub.containsKey(9));
    }

    @Test
    public void testSubMap_Exclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");
        ImmutableNavigableMap<Integer, String> sub = map.subMap(3, false, 7, false);

        Assertions.assertEquals(1, sub.size());
        Assertions.assertTrue(sub.containsKey(5));
        Assertions.assertFalse(sub.containsKey(3));
        Assertions.assertFalse(sub.containsKey(7));
    }

    @Test
    public void testSubMap_MixedInclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");
        ImmutableNavigableMap<Integer, String> sub = map.subMap(3, true, 7, false);

        Assertions.assertEquals(2, sub.size());
        Assertions.assertTrue(sub.containsKey(3));
        Assertions.assertTrue(sub.containsKey(5));
        Assertions.assertFalse(sub.containsKey(7));
    }

    @Test
    public void testHeadMap_Inclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");
        ImmutableNavigableMap<Integer, String> head = map.headMap(5, true);

        Assertions.assertEquals(3, head.size());
        Assertions.assertTrue(head.containsKey(1));
        Assertions.assertTrue(head.containsKey(3));
        Assertions.assertTrue(head.containsKey(5));
        Assertions.assertFalse(head.containsKey(7));
    }

    @Test
    public void testHeadMap_Exclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");
        ImmutableNavigableMap<Integer, String> head = map.headMap(5, false);

        Assertions.assertEquals(2, head.size());
        Assertions.assertTrue(head.containsKey(1));
        Assertions.assertTrue(head.containsKey(3));
        Assertions.assertFalse(head.containsKey(5));
    }

    @Test
    public void testTailMap_Inclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");
        ImmutableNavigableMap<Integer, String> tail = map.tailMap(3, true);

        Assertions.assertEquals(3, tail.size());
        Assertions.assertTrue(tail.containsKey(3));
        Assertions.assertTrue(tail.containsKey(5));
        Assertions.assertTrue(tail.containsKey(7));
        Assertions.assertFalse(tail.containsKey(1));
    }

    @Test
    public void testTailMap_Exclusive() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 3, "three", 5, "five", 7, "seven");
        ImmutableNavigableMap<Integer, String> tail = map.tailMap(3, false);

        Assertions.assertEquals(2, tail.size());
        Assertions.assertFalse(tail.containsKey(3));
        Assertions.assertTrue(tail.containsKey(5));
        Assertions.assertTrue(tail.containsKey(7));
    }

    @Test
    public void testNavigationWithStrings() {
        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.of("apple", 1, "banana", 2, "cherry", 3, "date", 4);

        Assertions.assertEquals("banana", map.higherKey("apple"));
        Assertions.assertEquals("cherry", map.ceilingKey("cherry"));
        Assertions.assertEquals("banana", map.floorKey("banana"));
        Assertions.assertEquals("apple", map.lowerKey("banana"));

        Assertions.assertNull(map.lowerKey("apple"));
        Assertions.assertNull(map.higherKey("date"));
    }

    @Test
    public void testWithCustomComparator() {
        NavigableMap<String, Integer> source = new TreeMap<>(Comparator.reverseOrder());
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.copyOf(source);
        Assertions.assertEquals("c", map.firstKey());
        Assertions.assertEquals("a", map.lastKey());

        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("c", keys.next());
        Assertions.assertEquals("b", keys.next());
        Assertions.assertEquals("a", keys.next());
    }

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 2, "two");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put(3, "three"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove(1));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.pollFirstEntry());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.pollLastEntry());
    }
}
