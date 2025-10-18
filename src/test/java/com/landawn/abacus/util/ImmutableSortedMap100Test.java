package com.landawn.abacus.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableSortedMap100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableSortedMap<String, Integer> emptyMap = ImmutableSortedMap.empty();
        Assertions.assertTrue(emptyMap.isEmpty());
        Assertions.assertEquals(0, emptyMap.size());
        Assertions.assertThrows(NoSuchElementException.class, () -> emptyMap.firstKey());
        Assertions.assertThrows(NoSuchElementException.class, () -> emptyMap.lastKey());
    }

    @Test
    public void testOf_SingleEntry() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("one", 1);
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals("one", map.firstKey());
        Assertions.assertEquals("one", map.lastKey());
    }

    @Test
    public void testOf_TwoEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("beta", 2, "alpha", 1);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("alpha", map.firstKey());
        Assertions.assertEquals("beta", map.lastKey());
        Assertions.assertEquals(1, map.get("alpha"));
        Assertions.assertEquals(2, map.get("beta"));
    }

    @Test
    public void testOf_ThreeEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(3, "three", 1, "one", 2, "two");
        Assertions.assertEquals(3, map.size());
        Iterator<Integer> keys = map.keySet().iterator();
        Assertions.assertEquals(1, keys.next());
        Assertions.assertEquals(2, keys.next());
        Assertions.assertEquals(3, keys.next());
    }

    @Test
    public void testOf_FourEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("d", 4, "b", 2, "c", 3, "a", 1);
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals("a", map.firstKey());
        Assertions.assertEquals("d", map.lastKey());
    }

    @Test
    public void testOf_FiveEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(5, "five", 3, "three", 1, "one", 4, "four", 2, "two");
        Assertions.assertEquals(5, map.size());
        Assertions.assertEquals(1, map.firstKey());
        Assertions.assertEquals(5, map.lastKey());
    }

    @Test
    public void testOf_SixEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(6, map.size());
    }

    @Test
    public void testOf_SevenEntries() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("g", 7, "f", 6, "e", 5, "d", 4, "c", 3, "b", 2, "a", 1);
        Assertions.assertEquals(7, map.size());
        Assertions.assertEquals("a", map.firstKey());
        Assertions.assertEquals("g", map.lastKey());
    }

    @Test
    public void testOf_EightEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(8, "8", 7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(8, map.size());
    }

    @Test
    public void testOf_NineEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(9, "9", 8, "8", 7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(9, map.size());
    }

    @Test
    public void testOf_TenEntries() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(10, "10", 9, "9", 8, "8", 7, "7", 6, "6", 5, "5", 4, "4", 3, "3", 2, "2", 1, "1");
        Assertions.assertEquals(10, map.size());
        Assertions.assertEquals(1, map.firstKey());
        Assertions.assertEquals(10, map.lastKey());
    }

    @Test
    public void testCopyOf() {
        SortedMap<String, Integer> source = new TreeMap<>();
        source.put("charlie", 3);
        source.put("alpha", 1);
        source.put("beta", 2);

        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.copyOf(source);
        Assertions.assertEquals(3, map.size());
        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("alpha", keys.next());
        Assertions.assertEquals("beta", keys.next());
        Assertions.assertEquals("charlie", keys.next());
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableSortedMap<String, Integer> original = ImmutableSortedMap.of("a", 1, "b", 2);
        ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.copyOf(new TreeMap<>());
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.copyOf(null);
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testWrap() {
        SortedMap<String, Integer> mutable = new TreeMap<>();
        mutable.put("b", 2);
        mutable.put("a", 1);

        ImmutableSortedMap<String, Integer> wrapped = ImmutableSortedMap.wrap(mutable);
        Assertions.assertEquals(2, wrapped.size());

        mutable.put("c", 3);
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.containsKey("c"));
        Assertions.assertEquals("c", wrapped.lastKey());
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableSortedMap<String, Integer> original = ImmutableSortedMap.of("a", 1);
        ImmutableSortedMap<String, Integer> wrapped = ImmutableSortedMap.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableSortedMap<String, Integer> wrapped = ImmutableSortedMap.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_Map_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableSortedMap.wrap(new HashMap<String, Integer>());
        });
    }

    @Test
    public void testComparator() {
        ImmutableSortedMap<String, Integer> naturalOrder = ImmutableSortedMap.of("a", 1, "b", 2);
        Assertions.assertNull(naturalOrder.comparator());

        SortedMap<String, Integer> customOrder = new TreeMap<>(Comparator.reverseOrder());
        customOrder.put("a", 1);
        customOrder.put("b", 2);
        ImmutableSortedMap<String, Integer> withComparator = ImmutableSortedMap.wrap(customOrder);
        Assertions.assertNotNull(withComparator.comparator());
    }

    @Test
    public void testSubMap() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
        ImmutableSortedMap<Integer, String> sub = map.subMap(2, 4);

        Assertions.assertEquals(2, sub.size());
        Assertions.assertTrue(sub.containsKey(2));
        Assertions.assertTrue(sub.containsKey(3));
        Assertions.assertFalse(sub.containsKey(1));
        Assertions.assertFalse(sub.containsKey(4));
        Assertions.assertFalse(sub.containsKey(5));
    }

    @Test
    public void testSubMap_Empty() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two", 3, "three");
        ImmutableSortedMap<Integer, String> sub = map.subMap(2, 2);
        Assertions.assertTrue(sub.isEmpty());
    }

    @Test
    public void testSubMap_IllegalArgument() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two");
        Assertions.assertThrows(IllegalArgumentException.class, () -> map.subMap(2, 1));
    }

    @Test
    public void testHeadMap() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3, "d", 4);
        ImmutableSortedMap<String, Integer> head = map.headMap("c");

        Assertions.assertEquals(2, head.size());
        Assertions.assertTrue(head.containsKey("a"));
        Assertions.assertTrue(head.containsKey("b"));
        Assertions.assertFalse(head.containsKey("c"));
        Assertions.assertFalse(head.containsKey("d"));
    }

    @Test
    public void testHeadMap_Empty() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("b", 2, "c", 3, "d", 4);
        ImmutableSortedMap<String, Integer> head = map.headMap("a");
        Assertions.assertTrue(head.isEmpty());
    }

    @Test
    public void testTailMap() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(10, "ten", 20, "twenty", 30, "thirty", 40, "forty");
        ImmutableSortedMap<Integer, String> tail = map.tailMap(25);

        Assertions.assertEquals(2, tail.size());
        Assertions.assertTrue(tail.containsKey(30));
        Assertions.assertTrue(tail.containsKey(40));
        Assertions.assertFalse(tail.containsKey(10));
        Assertions.assertFalse(tail.containsKey(20));
    }

    @Test
    public void testTailMap_All() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(10, "ten", 20, "twenty", 30, "thirty");
        ImmutableSortedMap<Integer, String> tail = map.tailMap(5);
        Assertions.assertEquals(3, tail.size());
    }

    @Test
    public void testFirstKey() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("banana", 2, "apple", 1, "cherry", 3);
        Assertions.assertEquals("apple", map.firstKey());
    }

    @Test
    public void testLastKey() {
        ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(3, "three", 1, "one", 4, "four", 1, "uno", 5, "five");
        Assertions.assertEquals(5, map.lastKey());
    }

    @Test
    public void testIteratorOrder() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("dog", 4, "cat", 3, "bird", 2, "ant", 1);
        Iterator<Map.Entry<String, Integer>> iter = map.entrySet().iterator();

        Map.Entry<String, Integer> entry = iter.next();
        Assertions.assertEquals("ant", entry.getKey());
        Assertions.assertEquals(1, entry.getValue());

        entry = iter.next();
        Assertions.assertEquals("bird", entry.getKey());

        entry = iter.next();
        Assertions.assertEquals("cat", entry.getKey());

        entry = iter.next();
        Assertions.assertEquals("dog", entry.getKey());

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.putAll(CommonUtil.asMap("d", 4)));
    }
}
