package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ImmutableSortedSetTest extends TestBase {

    @Test
    public void test_empty() {
        ImmutableSortedSet<String> emptySet = ImmutableSortedSet.empty();
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void test_empty_returnsSameInstance() {
        ImmutableSortedSet<String> empty1 = ImmutableSortedSet.empty();
        ImmutableSortedSet<Integer> empty2 = ImmutableSortedSet.empty();
        assertSame(empty1, empty2);
    }

    //
    //
    //    @Test
    //    public void test_just_withNullableComparator() {
    //        ImmutableSortedSet<Integer> set = ImmutableSortedSet.just(10);
    //        assertNotNull(set);
    //        assertEquals(1, set.size());
    //        assertTrue(set.contains(10));
    //    }

    //
    //
    //

    @Test
    public void test_of_oneElement() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(42);
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test
    public void test_of_twoElements() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("b", "a");
        assertEquals(2, set.size());
        assertEquals("a", set.first());
        assertEquals("b", set.last());
    }

    @Test
    public void test_of_twoElements_withDuplicate() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(5, 5);
        assertEquals(1, set.size());
        assertTrue(set.contains(5));
    }

    @Test
    public void test_of_threeElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(1, set.first());
        assertEquals(3, set.last());
    }

    @Test
    public void test_of_threeElements_withDuplicates() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "a");
        assertEquals(2, set.size());
    }

    @Test
    public void test_of_fourElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(4, 2, 3, 1);
        assertEquals(4, set.size());
        List<Integer> expected = Arrays.asList(1, 2, 3, 4);
        assertEquals(expected, new ArrayList<>(set));
    }

    @Test
    public void test_of_fiveElements() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("e", "c", "a", "d", "b");
        assertEquals(5, set.size());
        assertEquals("a", set.first());
        assertEquals("e", set.last());
    }

    @Test
    public void test_of_sixElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(6, 4, 2, 5, 3, 1);
        assertEquals(6, set.size());
        assertEquals(1, set.first());
        assertEquals(6, set.last());
    }

    @Test
    public void test_of_sevenElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(7, 5, 3, 1, 2, 4, 6);
        assertEquals(7, set.size());
        assertEquals(1, set.first());
        assertEquals(7, set.last());
    }

    @Test
    public void test_of_eightElements() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("h", "f", "d", "b", "a", "c", "e", "g");
        assertEquals(8, set.size());
        assertEquals("a", set.first());
        assertEquals("h", set.last());
    }

    @Test
    public void test_of_nineElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(9, 7, 5, 3, 1, 2, 4, 6, 8);
        assertEquals(9, set.size());
        assertEquals(1, set.first());
        assertEquals(9, set.last());
    }

    @Test
    public void test_of_tenElements() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(10, 8, 6, 4, 2, 1, 3, 5, 7, 9);
        assertEquals(10, set.size());
        assertEquals(1, set.first());
        assertEquals(10, set.last());
    }

    @Test
    public void test_copyOf_withList() {
        List<String> list = Arrays.asList("c", "a", "b");
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(list);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());
    }

    @Test
    public void test_copyOf_withEmptyCollection() {
        List<String> emptyList = new ArrayList<>();
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(emptyList);
        assertSame(ImmutableSortedSet.empty(), set);
    }

    @Test
    public void test_copyOf_withNull() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf((Collection<String>) null);
        assertSame(ImmutableSortedSet.empty(), set);
    }

    @Test
    public void test_copyOf_withImmutableSortedSet() {
        ImmutableSortedSet<Integer> original = ImmutableSortedSet.of(1, 2, 3);
        ImmutableSortedSet<Integer> copy = ImmutableSortedSet.copyOf(original);
        assertSame(original, copy);
    }

    @Test
    public void test_copyOf_withSortedSet() {
        SortedSet<String> sortedSet = new TreeSet<>(Arrays.asList("c", "a", "b"));
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(sortedSet);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());
    }

    @Test
    public void test_copyOf_withDuplicates() {
        List<Integer> listWithDuplicates = Arrays.asList(1, 2, 2, 3, 3, 3);
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.copyOf(listWithDuplicates);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void test_wrap_withSortedSet() {
        SortedSet<String> sortedSet = new TreeSet<>(Arrays.asList("b", "a", "c"));
        ImmutableSortedSet<String> wrapped = ImmutableSortedSet.wrap(sortedSet);
        assertNotNull(wrapped);
        assertEquals(3, wrapped.size());
    }

    @Test
    public void test_wrap_withNull() {
        ImmutableSortedSet<String> wrapped = ImmutableSortedSet.wrap((SortedSet<String>) null);
        assertSame(ImmutableSortedSet.empty(), wrapped);
    }

    @Test
    public void test_wrap_withImmutableSortedSet() {
        ImmutableSortedSet<Integer> original = ImmutableSortedSet.of(1, 2, 3);
        ImmutableSortedSet<Integer> wrapped = ImmutableSortedSet.wrap(original);
        assertSame(original, wrapped);
    }

    @Test
    public void test_wrap_reflectsChanges() {
        TreeSet<Integer> mutableSet = new TreeSet<>(Arrays.asList(1, 2, 3));
        ImmutableSortedSet<Integer> wrapped = ImmutableSortedSet.wrap(mutableSet);
        assertEquals(3, wrapped.size());

        mutableSet.add(4);
        assertEquals(4, wrapped.size());
        assertTrue(wrapped.contains(4));
    }

    @Test
    public void test_wrap_Set_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableSortedSet.wrap(new HashSet<>());
        });
    }

    @Test
    public void test_comparator_naturalOrdering() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c");
        assertNull(set.comparator());
    }

    @Test
    public void test_comparator_customOrdering() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        TreeSet<String> treeSet = new TreeSet<>(reverseOrder);
        treeSet.addAll(Arrays.asList("a", "b", "c"));
        ImmutableSortedSet<String> set = ImmutableSortedSet.wrap(treeSet);
        assertSame(reverseOrder, set.comparator());
    }

    @Test
    public void test_subSet_validRange() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> subset = set.subSet(2, 4);
        assertEquals(2, subset.size());
        assertTrue(subset.contains(2));
        assertTrue(subset.contains(3));
        assertFalse(subset.contains(4));
    }

    @Test
    public void test_subSet_emptyRange() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c", "d");
        ImmutableSortedSet<String> subset = set.subSet("b", "b");
        assertTrue(subset.isEmpty());
    }

    @Test
    public void test_subSet_invalidRange_throwsException() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        assertThrows(IllegalArgumentException.class, () -> {
            set.subSet(4, 2);
        });
    }

    @Test
    public void test_subSet_immutability() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> subset = set.subSet(2, 4);
        assertThrows(UnsupportedOperationException.class, () -> {
            java.util.Iterator<Integer> iter = subset.iterator();
            iter.next();
            iter.remove();
        });
    }

    @Test
    public void test_headSet_validElement() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c", "d");
        ImmutableSortedSet<String> headSet = set.headSet("c");
        assertEquals(2, headSet.size());
        assertTrue(headSet.contains("a"));
        assertTrue(headSet.contains("b"));
        assertFalse(headSet.contains("c"));
    }

    @Test
    public void test_headSet_firstElement() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> headSet = set.headSet(1);
        assertTrue(headSet.isEmpty());
    }

    @Test
    public void test_headSet_immutability() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c");
        ImmutableSortedSet<String> headSet = set.headSet("b");
        assertThrows(UnsupportedOperationException.class, () -> {
            java.util.Iterator<String> iter = headSet.iterator();
            iter.next();
            iter.remove();
        });
    }

    @Test
    public void test_tailSet_validElement() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> tailSet = set.tailSet(3);
        assertEquals(3, tailSet.size());
        assertTrue(tailSet.contains(3));
        assertTrue(tailSet.contains(4));
        assertTrue(tailSet.contains(5));
    }

    @Test
    public void test_tailSet_lastElement() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c");
        ImmutableSortedSet<String> tailSet = set.tailSet("c");
        assertEquals(1, tailSet.size());
        assertTrue(tailSet.contains("c"));
    }

    @Test
    public void test_tailSet_immutability() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        ImmutableSortedSet<Integer> tailSet = set.tailSet(3);
        assertThrows(UnsupportedOperationException.class, () -> {
            java.util.Iterator<Integer> iter = tailSet.iterator();
            iter.next();
            iter.remove();
        });
    }

    @Test
    public void test_first_nonEmptySet() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(5, 3, 1, 4, 2);
        assertEquals(1, set.first());
    }

    @Test
    public void test_first_emptySet_throwsException() {
        ImmutableSortedSet<String> emptySet = ImmutableSortedSet.empty();
        assertThrows(NoSuchElementException.class, () -> {
            emptySet.first();
        });
    }

    @Test
    public void test_last_nonEmptySet() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "c", "b", "e", "d");
        assertEquals("e", set.last());
    }

    @Test
    public void test_last_emptySet_throwsException() {
        ImmutableSortedSet<Integer> emptySet = ImmutableSortedSet.empty();
        assertThrows(NoSuchElementException.class, () -> {
            emptySet.last();
        });
    }

    @Test
    public void test_last_singleElement() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(42);
        assertEquals(42, set.last());
    }

    @Test
    public void test_sorted_order_preserved() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(5, 2, 8, 1, 9, 3);
        List<Integer> sortedList = new ArrayList<>(set);
        List<Integer> expected = Arrays.asList(1, 2, 3, 5, 8, 9);
        assertEquals(expected, sortedList);
    }

    @Test
    public void test_immutability_add() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c");
        assertThrows(UnsupportedOperationException.class, () -> {
            set.add("d");
        });
    }

    @Test
    public void test_immutability_remove() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3);
        assertThrows(UnsupportedOperationException.class, () -> {
            set.remove(2);
        });
    }

    @Test
    public void test_immutability_clear() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("x", "y", "z");
        assertThrows(UnsupportedOperationException.class, () -> {
            set.clear();
        });
    }

    @Test
    public void test_contains_operations() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("apple", "banana", "cherry");
        assertTrue(set.contains("banana"));
        assertFalse(set.contains("orange"));
    }

    @Test
    public void test_size_operations() {
        ImmutableSortedSet<Integer> empty = ImmutableSortedSet.empty();
        assertEquals(0, empty.size());

        ImmutableSortedSet<Integer> withElements = ImmutableSortedSet.of(1, 2, 3, 4, 5);
        assertEquals(5, withElements.size());
    }

    @Test
    public void test_isEmpty_operations() {
        ImmutableSortedSet<String> empty = ImmutableSortedSet.empty();
        assertTrue(empty.isEmpty());

        ImmutableSortedSet<String> nonEmpty = ImmutableSortedSet.of("test");
        assertFalse(nonEmpty.isEmpty());
    }

    @Test
    public void test_iterator_order() {
        ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 4, 1, 5, 9, 2, 6);
        List<Integer> iteratedElements = new ArrayList<>();
        for (Integer element : set) {
            iteratedElements.add(element);
        }

        for (int i = 0; i < iteratedElements.size() - 1; i++) {
            assertTrue(iteratedElements.get(i) < iteratedElements.get(i + 1));
        }
    }

    @Test
    public void test_toArray() {
        ImmutableSortedSet<String> set = ImmutableSortedSet.of("c", "a", "b");
        Object[] array = set.toArray();
        assertEquals(3, array.length);
        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
        assertEquals("c", array[2]);
    }

    @Test
    public void testEmpty() {
        ImmutableSortedSet<String> emptySet = ImmutableSortedSet.empty();
        Assertions.assertTrue(emptySet.isEmpty());
        Assertions.assertEquals(0, emptySet.size());
        Assertions.assertThrows(NoSuchElementException.class, () -> emptySet.first());
        Assertions.assertThrows(NoSuchElementException.class, () -> emptySet.last());
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

        Assertions.assertEquals(3, set.size());
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
        ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf((Collection<String>) null);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testWrap() {
        SortedSet<String> mutable = new TreeSet<>();
        mutable.add("b");
        mutable.add("a");

        ImmutableSortedSet<String> wrapped = ImmutableSortedSet.wrap(mutable);
        Assertions.assertEquals(2, wrapped.size());

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
