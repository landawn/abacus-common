package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableSortedSet100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableSortedSet<String> emptySet = ImmutableSortedSet.empty();
        Assertions.assertTrue(emptySet.isEmpty());
        Assertions.assertEquals(0, emptySet.size());
        Assertions.assertThrows(NoSuchElementException.class, () -> emptySet.first());
        Assertions.assertThrows(NoSuchElementException.class, () -> emptySet.last());
    }

    @Test
    public void testJust() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.just(42);
        Assertions.assertEquals(1, set.size());
        Assertions.assertEquals(42, set.first());
        Assertions.assertEquals(42, set.last());
    }

    @Test
    public void testOf_SingleElement() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("single");
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains("single"));
        Assertions.assertEquals("single", set.first());
        Assertions.assertEquals("single", set.last());
    }

    @Test
    public void testOf_TwoElements() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("beta", "alpha");
        Assertions.assertEquals(2, set.size());
        Assertions.assertEquals("alpha", set.first());
        Assertions.assertEquals("beta", set.last());
    }

    @Test
    public void testOf_ThreeElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 2);
        Assertions.assertEquals(3, set.size());
        Iterator<Integer> iter = set.iterator();
        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
    }

    @Test
    public void testOf_FourElements() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("d", "b", "c", "a");
        Assertions.assertEquals(4, set.size());
        Assertions.assertEquals("a", set.first());
        Assertions.assertEquals("d", set.last());
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(5, 3, 1, 4, 2);
        Assertions.assertEquals(5, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(5, set.last());
    }

    @Test
    public void testOf_SixElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(6, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(6, set.last());
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("g", "f", "e", "d", "c", "b", "a");
        Assertions.assertEquals(7, set.size());
        Assertions.assertEquals("a", set.first());
        Assertions.assertEquals("g", set.last());
    }

    @Test
    public void testOf_EightElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(8, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(8, set.last());
    }

    @Test
    public void testOf_NineElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(9, 8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(9, set.size());
        Assertions.assertTrue(set.contains(5));
    }

    @Test
    public void testOf_TenElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(10, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(10, set.last());
    }

    @Test
    public void testCopyOf() {
        List<String> list = Arrays.asList("charlie", "alpha", "beta", "alpha");
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(list);
        
        Assertions.assertEquals(3, set.size()); // Duplicates removed
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("alpha", iter.next());
        Assertions.assertEquals("beta", iter.next());
        Assertions.assertEquals("charlie", iter.next());
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableSortedSet<String> original = ImmutableSortedSet.of("a", "b");
        ImmutableSortedSet<String> copy = ImmutableSortedSet.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(new ArrayList<>());
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(null);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testWrap() {
        SortedSet<String> mutable = new TreeSet<>();
        mutable.add("b");
        mutable.add("a");
        
        ImmutableSortedSet<String> wrapped = ImmutableSortedSet.wrap(mutable);
        Assertions.assertEquals(2, wrapped.size());
        
        // Changes are reflected
        mutable.add("c");
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("c"));
        Assertions.assertEquals("c", wrapped.last());
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableSortedSet<String> original = ImmutableSortedSet.of("a");
        ImmutableSortedSet<String> wrapped = ImmutableSortedSet.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableSortedSet<String> wrapped = ImmutableSortedSet.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_Set_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableSortedSet.wrap(new HashSet<String>());
        });
    }

    @Test
    public void testComparator() {
        ImmutableSortedSet<String> naturalOrder = ImmutableSortedSet.of("a", "b", "c");
        Assertions.assertNull(naturalOrder.comparator());
        
        SortedSet<String> customOrder = new TreeSet<>(Comparator.reverseOrder());
        customOrder.add("a");
        customOrder.add("b");
        ImmutableSortedSet<String> withComparator = ImmutableSortedSet.wrap(customOrder);
        Assertions.assertNotNull(withComparator.comparator());
    }

    @Test
    public void testSubSet() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> sub = set.subSet(2, 4);
        
        Assertions.assertEquals(2, sub.size());
        Assertions.assertTrue(sub.contains(2));
        Assertions.assertTrue(sub.contains(3));
        Assertions.assertFalse(sub.contains(1));
        Assertions.assertFalse(sub.contains(4));
        Assertions.assertFalse(sub.contains(5));
    }

    @Test
    public void testSubSet_Empty() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> sub = set.subSet(2, 2);
        Assertions.assertTrue(sub.isEmpty());
    }

    @Test
    public void testSubSet_IllegalArgument() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> set.subSet(3, 1));
    }

    @Test
    public void testHeadSet() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c", "d");
        ImmutableSortedSet<String> head = set.headSet("c");
        
        Assertions.assertEquals(2, head.size());
        Assertions.assertTrue(head.contains("a"));
        Assertions.assertTrue(head.contains("b"));
        Assertions.assertFalse(head.contains("c"));
        Assertions.assertFalse(head.contains("d"));
    }

    @Test
    public void testHeadSet_Empty() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("b", "c", "d");
        ImmutableSortedSet<String> head = set.headSet("a");
        Assertions.assertTrue(head.isEmpty());
    }

    @Test
    public void testTailSet() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(10, 20, 30, 40);
        ImmutableSortedSet<Integer> tail = set.tailSet(25);
        
        Assertions.assertEquals(2, tail.size());
        Assertions.assertTrue(tail.contains(30));
        Assertions.assertTrue(tail.contains(40));
        Assertions.assertFalse(tail.contains(10));
        Assertions.assertFalse(tail.contains(20));
    }

    @Test
    public void testTailSet_All() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(10, 20, 30);
        ImmutableSortedSet<Integer> tail = set.tailSet(5);
        Assertions.assertEquals(3, tail.size());
    }

    @Test
    public void testFirst() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("banana", "apple", "cherry");
        Assertions.assertEquals("apple", set.first());
    }

    @Test
    public void testLast() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 4, 1, 5);
        Assertions.assertEquals(5, set.last());
    }

    @Test
    public void testWithDuplicates() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 2, 3, 3, 3);
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains(1));
        Assertions.assertTrue(set.contains(2));
        Assertions.assertTrue(set.contains(3));
    }

    @Test
    public void testIteratorOrder() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("dog", "cat", "bird", "ant");
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("ant", iter.next());
        Assertions.assertEquals("bird", iter.next());
        Assertions.assertEquals("cat", iter.next());
        Assertions.assertEquals("dog", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b");
        
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.add("c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.addAll(Arrays.asList("d", "e")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.removeAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.retainAll(Arrays.asList("a")));
    }
}