package com.landawn.abacus.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableNavigableMapTest extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableNavigableMap<String, Integer> emptyMap = ImmutableNavigableMap.empty();
        Assertions.assertTrue(emptyMap.isEmpty());
        Assertions.assertEquals(0, emptyMap.size());
        Assertions.assertNull(emptyMap.lowerEntry("any"));
        Assertions.assertNull(emptyMap.higherEntry("any"));
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
    public void testOf_SingleEntry() {
        ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.of("one", 1);
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals("one", map.firstKey());
        Assertions.assertEquals("one", map.lastKey());
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
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "one", 2, "two");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put(3, "three"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove(1));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.pollFirstEntry());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.pollLastEntry());
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
    public void testCopyOf_EmptySortedMapRetainsComparator() {
        Comparator<String> comparator = Comparator.reverseOrder();
        SortedMap<String, Integer> source = new TreeMap<>(comparator);

        Assertions.assertSame(comparator, ImmutableNavigableMap.copyOf(source).comparator());
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
    public void testEquals() {
        ImmutableNavigableMap<Integer, String> map1 = ImmutableNavigableMap.of(1, "one", 2, "two", 3, "three");
        ImmutableNavigableMap<Integer, String> map2 = ImmutableNavigableMap.of(1, "one", 2, "two", 3, "three");
        ImmutableNavigableMap<Integer, String> map3 = ImmutableNavigableMap.of(1, "one", 2, "two");

        Assertions.assertEquals(map1, map2);
        Assertions.assertNotEquals(map1, map3);

        // Equality with a regular TreeMap
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        treeMap.put(2, "two");
        treeMap.put(3, "three");
        Assertions.assertEquals(map1, treeMap);
        Assertions.assertEquals(treeMap, map1);
    }

    @Test
    public void testHashCode() {
        ImmutableNavigableMap<Integer, String> map1 = ImmutableNavigableMap.of(1, "one", 2, "two");
        ImmutableNavigableMap<Integer, String> map2 = ImmutableNavigableMap.of(1, "one", 2, "two");

        Assertions.assertEquals(map1.hashCode(), map2.hashCode());

        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        treeMap.put(2, "two");
        Assertions.assertEquals(map1.hashCode(), treeMap.hashCode());
    }

    /** A key type that is deliberately NOT Comparable. */
    private static final class NotComparableNM {
        private final String s;

        NotComparableNM(final String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    @Test
    public void testOf_nonComparableKeySelectsInheritedUnsortedFactory() {
        // of(...) requires K extends Comparable. A non-Comparable key is not applicable to
        // ImmutableNavigableMap.of, so the call binds to ImmutableMap.of and returns an unsorted map.
        final ImmutableMap<NotComparableNM, Integer> one = ImmutableNavigableMap.of(new NotComparableNM("z"), 1);
        final ImmutableMap<NotComparableNM, Integer> two = ImmutableNavigableMap.of(new NotComparableNM("z"), 1, new NotComparableNM("a"), 2);
        Assertions.assertFalse(one instanceof SortedMap);
        Assertions.assertFalse(two instanceof SortedMap);
        Assertions.assertEquals(1, one.size());
        Assertions.assertEquals(2, two.size());
    }

    @Test
    public void testOf_isStillSortedForComparableKeys() {
        final ImmutableNavigableMap<String, Integer> m = ImmutableNavigableMap.of("c", 3, "a", 1, "b", 2);

        Assertions.assertEquals(java.util.Arrays.asList("a", "b", "c"), new java.util.ArrayList<>(m.keySet()));
        Assertions.assertEquals("a", m.firstKey());
        Assertions.assertEquals("c", m.lastKey());
    }

    @Test
    public void testBuilderIsBlocked() {
        Assertions.assertThrows(UnsupportedOperationException.class, ImmutableNavigableMap::builder);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> ImmutableSortedMap.builder(new java.util.TreeMap<String, Integer>()));
    }

    @Test
    public void testCopyOf_copiesAWrappedView() {
        final java.util.TreeMap<String, Integer> live = new java.util.TreeMap<>();
        live.put("a", 1);

        final ImmutableNavigableMap<String, Integer> view = ImmutableNavigableMap.wrap(live);
        final ImmutableNavigableMap<String, Integer> copy = ImmutableNavigableMap.copyOf(view);

        Assertions.assertNotSame(view, copy);

        live.put("b", 2);

        Assertions.assertEquals(2, view.size());
        Assertions.assertEquals(1, copy.size());
    }

    @Test
    public void testDescendingMapTraversesInDescendingOrder() {
        // Same reason as ImmutableNavigableSet.descendingSet(): AbstractImmutableMap.forEach() delegates to
        // the backing map, which for this view is already the descending one.
        final ImmutableNavigableMap<String, Integer> descending = ImmutableNavigableMap.of("a", 1, "b", 2, "c", 3).descendingMap();
        final java.util.List<String> expected = java.util.Arrays.asList("c", "b", "a");

        Assertions.assertEquals(expected, new java.util.ArrayList<>(descending.keySet()));
        Assertions.assertEquals(java.util.Arrays.asList(3, 2, 1), new java.util.ArrayList<>(descending.values()));

        final java.util.List<String> seen = new java.util.ArrayList<>();
        descending.forEach((k, v) -> seen.add(k));
        Assertions.assertEquals(expected, seen);

        Assertions.assertEquals("c", descending.firstKey());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> descending.put("z", 9));
    }

    @Test
    public void testRangeViewsInheritOwnership() {
        final ImmutableNavigableMap<String, Integer> owned = ImmutableNavigableMap.of("a", 1, "b", 2, "c", 3);
        final ImmutableNavigableMap<String, Integer> ownedHead = owned.headMap("c", false);
        Assertions.assertSame(ownedHead, ImmutableNavigableMap.copyOf(ownedHead));

        final java.util.TreeMap<String, Integer> live = new java.util.TreeMap<>();
        live.put("a", 1);
        live.put("c", 3);

        final ImmutableNavigableMap<String, Integer> viewHead = ImmutableNavigableMap.wrap(live).headMap("d", false);
        final ImmutableNavigableMap<String, Integer> copy = ImmutableNavigableMap.copyOf(viewHead);
        Assertions.assertNotSame(viewHead, copy);

        live.put("b", 2);
        Assertions.assertEquals(3, viewHead.size());
        Assertions.assertEquals(2, copy.size());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> viewHead.put("z", 9));
    }

    @Test
    public void inclusiveRangeViews_throwWhenEndpointOutsideRestrictedParent() {
        final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 3, "c", 5, "e");
        Assertions.assertThrows(IllegalArgumentException.class, () -> map.headMap(3, true).subMap(1, true, 5, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> map.headMap(3, true).tailMap(5, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> map.tailMap(3, true).headMap(1, true));
    }

    @Test
    public void reversed_isTheNarrowedDescendingMapView() {
        final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

        // covariant re-override of ImmutableSortedMap.reversed(); its behaviour is that of descendingMap()
        final ImmutableNavigableMap<Integer, String> reversed = map.reversed();
        Assertions.assertEquals("{3=c, 2=b, 1=a}", reversed.toString());
        Assertions.assertEquals(map.descendingMap().toString(), reversed.toString());
        Assertions.assertTrue(reversed instanceof Immutable);
        Assertions.assertEquals(Integer.valueOf(3), reversed.firstKey());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> reversed.put(9, "z"));
    }

    @Test
    public void copyOf_returnsEveryDerivedViewOfAnOwningSourceUnchanged() {
        // pins the copyOf(Map) memory note: every derived view of an owning map owns its backing storage
        // too, so copyOf hands it straight back and the whole parent map stays reachable.
        final ImmutableNavigableMap<Integer, String> owning = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

        final ImmutableNavigableMap<Integer, String> descending = owning.descendingMap();
        Assertions.assertSame(descending, ImmutableNavigableMap.copyOf(descending));
        Assertions.assertSame(descending, ImmutableSortedMap.copyOf(descending));

        final ImmutableNavigableMap<Integer, String> head = owning.headMap(3);
        Assertions.assertSame(head, ImmutableNavigableMap.copyOf(head));

        // the remaining three views the same paragraph enumerates - subMap/tailMap/reversed
        final ImmutableNavigableMap<Integer, String> sub = owning.subMap(1, 3);
        Assertions.assertSame(sub, ImmutableNavigableMap.copyOf(sub));

        final ImmutableNavigableMap<Integer, String> tail = owning.tailMap(2);
        Assertions.assertSame(tail, ImmutableNavigableMap.copyOf(tail));

        final ImmutableNavigableMap<Integer, String> reversed = owning.reversed();
        Assertions.assertSame(reversed, ImmutableNavigableMap.copyOf(reversed));
        Assertions.assertSame(reversed, ImmutableSortedMap.copyOf(reversed));

        // and the inclusive navigable form
        final ImmutableNavigableMap<Integer, String> subInclusive = owning.subMap(1, true, 3, true);
        Assertions.assertSame(subInclusive, ImmutableNavigableMap.copyOf(subInclusive));
    }
}
