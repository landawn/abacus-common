package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableNavigableSet100Test extends TestBase {

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.add(4));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.remove(2));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollFirst());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollLast());
    }

    @Test
    public void testDescendingSet() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
        ImmutableNavigableSet<Integer> descending = set.descendingSet();

        Assertions.assertEquals(5, descending.size());
        Iterator<Integer> iter = descending.iterator();
        Assertions.assertEquals(9, iter.next());
        Assertions.assertEquals(7, iter.next());
        Assertions.assertEquals(5, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(1, iter.next());
    }

    @Test
    public void testDescendingIterator() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("a", "b", "c");
        ObjIterator<String> iter = set.descendingIterator();

        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSubSet_Inclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> sub = set.subSet(2, true, 4, false);

        Assertions.assertEquals(2, sub.size());
        Assertions.assertTrue(sub.contains(2));
        Assertions.assertTrue(sub.contains(3));
        Assertions.assertFalse(sub.contains(4));
    }

    @Test
    public void testSubSet_Exclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> sub = set.subSet(2, false, 4, false);

        Assertions.assertEquals(1, sub.size());
        Assertions.assertTrue(sub.contains(3));
    }

    @Test
    public void testSubSet_BothInclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> sub = set.subSet(2, true, 4, true);

        Assertions.assertEquals(3, sub.size());
        Assertions.assertTrue(sub.contains(2));
        Assertions.assertTrue(sub.contains(3));
        Assertions.assertTrue(sub.contains(4));
    }

    @Test
    public void testHeadSet_Inclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> head = set.headSet(3, true);

        Assertions.assertEquals(3, head.size());
        Assertions.assertTrue(head.contains(1));
        Assertions.assertTrue(head.contains(2));
        Assertions.assertTrue(head.contains(3));
        Assertions.assertFalse(head.contains(4));
    }

    @Test
    public void testHeadSet_Exclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> head = set.headSet(3, false);

        Assertions.assertEquals(2, head.size());
        Assertions.assertTrue(head.contains(1));
        Assertions.assertTrue(head.contains(2));
        Assertions.assertFalse(head.contains(3));
    }

    @Test
    public void testTailSet_Inclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> tail = set.tailSet(3, true);

        Assertions.assertEquals(3, tail.size());
        Assertions.assertTrue(tail.contains(3));
        Assertions.assertTrue(tail.contains(4));
        Assertions.assertTrue(tail.contains(5));
        Assertions.assertFalse(tail.contains(2));
    }

    @Test
    public void testTailSet_Exclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> tail = set.tailSet(3, false);

        Assertions.assertEquals(2, tail.size());
        Assertions.assertTrue(tail.contains(4));
        Assertions.assertTrue(tail.contains(5));
        Assertions.assertFalse(tail.contains(3));
    }

    @Test
    public void testNavigationWithStrings() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("apple", "banana", "cherry", "date", "elderberry");

        Assertions.assertEquals("banana", set.higher("apple"));
        Assertions.assertEquals("cherry", set.ceiling("cherry"));
        Assertions.assertEquals("banana", set.floor("banana"));
        Assertions.assertEquals("apple", set.lower("banana"));

        Assertions.assertNull(set.lower("apple"));
        Assertions.assertNull(set.higher("elderberry"));
    }

    @Test
    public void testWithCustomComparator() {
        NavigableSet<String> source = new TreeSet<>(Comparator.reverseOrder());
        source.addAll(Arrays.asList("a", "b", "c"));

        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(source);
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testEmpty() {
        ImmutableNavigableSet<String> emptySet = ImmutableNavigableSet.empty();
        Assertions.assertTrue(emptySet.isEmpty());
        Assertions.assertEquals(0, emptySet.size());
        Assertions.assertNull(emptySet.lower("any"));
        Assertions.assertNull(emptySet.higher("any"));
    }

    @Test
    public void testJust() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.just(42);
        Assertions.assertEquals(1, set.size());
        Assertions.assertEquals(42, set.first());
        Assertions.assertEquals(42, set.last());
    }

    @Test
    public void testOf_SingleElement() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("single");
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains("single"));
    }

    @Test
    public void testOf_TwoElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(2, 1);
        Assertions.assertEquals(2, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(2, set.last());
    }

    @Test
    public void testOf_ThreeElements() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("beta", "alpha", "gamma");
        Assertions.assertEquals(3, set.size());
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("alpha", iter.next());
        Assertions.assertEquals("beta", iter.next());
        Assertions.assertEquals("gamma", iter.next());
    }

    @Test
    public void testOf_FourElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(4, 2, 3, 1);
        Assertions.assertEquals(4, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(4, set.last());
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(5, 3, 1, 4, 2);
        Assertions.assertEquals(5, set.size());
        Assertions.assertTrue(set.contains(3));
    }

    @Test
    public void testOf_SixElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(6, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(6, set.last());
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(7, set.size());
    }

    @Test
    public void testOf_EightElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(8, set.size());
    }

    @Test
    public void testOf_NineElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(9, 8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(9, set.size());
    }

    @Test
    public void testOf_TenElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(10, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(10, set.last());
    }

    @Test
    public void testCopyOf() {
        List<String> list = Arrays.asList("charlie", "alpha", "beta");
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(list);

        Assertions.assertEquals(3, set.size());
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("alpha", iter.next());
        Assertions.assertEquals("beta", iter.next());
        Assertions.assertEquals("charlie", iter.next());
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableNavigableSet<String> original = ImmutableNavigableSet.of("a", "b");
        ImmutableNavigableSet<String> copy = ImmutableNavigableSet.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(new ArrayList<>());
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(null);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testWrap() {
        NavigableSet<String> mutable = new TreeSet<>();
        mutable.add("b");
        mutable.add("a");

        ImmutableNavigableSet<String> wrapped = ImmutableNavigableSet.wrap(mutable);
        Assertions.assertEquals(2, wrapped.size());

        mutable.add("c");
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("c"));
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableNavigableSet<String> original = ImmutableNavigableSet.of("a");
        ImmutableNavigableSet<String> wrapped = ImmutableNavigableSet.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableNavigableSet<String> wrapped = ImmutableNavigableSet.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_SortedSet_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableNavigableSet.wrap((SortedSet<String>) new TreeSet<String>());
        });
    }

    @Test
    public void testLower() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertNull(set.lower(1));
        Assertions.assertEquals(1, set.lower(2));
        Assertions.assertEquals(1, set.lower(3));
        Assertions.assertEquals(3, set.lower(4));
        Assertions.assertEquals(3, set.lower(5));
        Assertions.assertEquals(5, set.lower(6));
        Assertions.assertEquals(9, set.lower(10));
    }

    @Test
    public void testFloor() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertNull(set.floor(0));
        Assertions.assertEquals(1, set.floor(1));
        Assertions.assertEquals(1, set.floor(2));
        Assertions.assertEquals(3, set.floor(3));
        Assertions.assertEquals(3, set.floor(4));
        Assertions.assertEquals(5, set.floor(5));
        Assertions.assertEquals(9, set.floor(10));
    }

    @Test
    public void testCeiling() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertEquals(1, set.ceiling(0));
        Assertions.assertEquals(1, set.ceiling(1));
        Assertions.assertEquals(3, set.ceiling(2));
        Assertions.assertEquals(3, set.ceiling(3));
        Assertions.assertEquals(5, set.ceiling(4));
        Assertions.assertEquals(5, set.ceiling(5));
        Assertions.assertNull(set.ceiling(10));
    }

    @Test
    public void testHigher() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertEquals(1, set.higher(0));
        Assertions.assertEquals(3, set.higher(1));
        Assertions.assertEquals(3, set.higher(2));
        Assertions.assertEquals(5, set.higher(3));
        Assertions.assertEquals(5, set.higher(4));
        Assertions.assertEquals(7, set.higher(5));
        Assertions.assertNull(set.higher(9));
        Assertions.assertNull(set.higher(10));
    }

    @Test
    public void testPollFirst_ThrowsUnsupported() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollFirst());
    }

    @Test
    public void testPollLast_ThrowsUnsupported() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollLast());
    }
}
