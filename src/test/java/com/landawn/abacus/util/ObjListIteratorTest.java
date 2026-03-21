package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public class ObjListIteratorTest extends TestBase {

    // --- empty() ---

    @Test
    public void testEmpty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertEquals(0, iter.nextIndex());
        Assertions.assertEquals(-1, iter.previousIndex());
    }

    @Test
    public void testEmpty_ToList() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertTrue(iter.toList().isEmpty());
    }

    @Test
    public void testEmpty_ToArray() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertEquals(0, iter.toArray().length);
    }

    @Test
    public void testEmpty_Count() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertEquals(0, iter.count());
    }

    @Test
    public void testToSet_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Set<String> set = iter.toSet();
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testToImmutableList_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        ImmutableList<String> immutable = iter.toImmutableList();
        Assertions.assertTrue(immutable.isEmpty());
    }

    @Test
    public void testToImmutableSet_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        ImmutableSet<String> immutable = iter.toImmutableSet();
        Assertions.assertTrue(immutable.isEmpty());
    }

    @Test
    public void testCount_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertEquals(0, iter.count());
    }

    @Test
    public void testEmpty_NextThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmpty_PreviousThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.previous());
    }

    @Test
    public void testEmpty_SetThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.set("x"));
    }

    @Test
    public void testEmpty_AddThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.add("x"));
    }

    @Test
    public void testEmpty_RemoveThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    // --- just() ---

    @Test
    public void testJust() {
        ObjListIterator<String> iter = ObjListIterator.just("Hello");
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertEquals("Hello", iter.next());
        Assertions.assertTrue(iter.hasPrevious());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testJust_PreviousAfterNext() {
        ObjListIterator<String> iter = ObjListIterator.just("val");
        iter.next();
        Assertions.assertEquals("val", iter.previous());
    }

    @Test
    public void testJust_Null() {
        ObjListIterator<String> iter = ObjListIterator.just(null);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertNull(iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // --- of(T...) ---

    @Test
    public void testOfArray() {
        String[] array = { "one", "two", "three" };
        ObjListIterator<String> iter = ObjListIterator.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("one", iter.next());
        Assertions.assertEquals("two", iter.next());
        Assertions.assertEquals("three", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // --- of(T[], int, int) ---

    @Test
    public void testOfArrayRange() {
        Integer[] numbers = { 1, 2, 3, 4, 5 };
        ObjListIterator<Integer> iter = ObjListIterator.of(numbers, 1, 4);

        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(4, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayRange_FullRange() {
        Integer[] numbers = { 1, 2, 3 };
        ObjListIterator<Integer> iter = ObjListIterator.of(numbers, 0, 3);
        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // --- of(List) ---

    @Test
    public void testOfList() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjListIterator<String> iter = ObjListIterator.of(list);

        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // --- of(ListIterator) ---

    @Test
    public void testOfListIterator() {
        List<String> list = Arrays.asList("x", "y", "z");
        ListIterator<String> listIter = list.listIterator();
        ObjListIterator<String> iter = ObjListIterator.of(listIter);

        Assertions.assertEquals("x", iter.next());
        Assertions.assertEquals("y", iter.next());
        Assertions.assertEquals("z", iter.next());
    }

    // --- Bidirectional iteration (hasNext, next, hasPrevious, previous, nextIndex, previousIndex) ---

    @Test
    public void testBidirectionalIteration() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        ObjListIterator<String> iter = ObjListIterator.of(list);

        iter.next();
        iter.next();
        Assertions.assertEquals("b", iter.previous());
        Assertions.assertEquals("a", iter.previous());
        Assertions.assertFalse(iter.hasPrevious());
    }

    @Test
    public void testNextIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        ObjListIterator<Integer> iter = ObjListIterator.of(list);

        Assertions.assertEquals(0, iter.nextIndex());
        iter.next();
        Assertions.assertEquals(1, iter.nextIndex());
        iter.next();
        Assertions.assertEquals(2, iter.nextIndex());
    }

    @Test
    public void testPreviousIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        ObjListIterator<Integer> iter = ObjListIterator.of(list);

        Assertions.assertEquals(-1, iter.previousIndex());
        iter.next();
        Assertions.assertEquals(0, iter.previousIndex());
        iter.next();
        Assertions.assertEquals(1, iter.previousIndex());
    }

    // --- toSet() (inherited from ImmutableIterator) ---

    @Test
    public void testToSet() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "a", "c"));
        Set<String> set = iter.toSet();
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("a"));
        Assertions.assertTrue(set.contains("b"));
        Assertions.assertTrue(set.contains("c"));
    }

    // --- toCollection() (inherited from ImmutableIterator) ---

    @Test
    public void testToCollection() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        LinkedList<String> linked = iter.toCollection(LinkedList::new);
        Assertions.assertEquals(3, linked.size());
        Assertions.assertEquals("a", linked.getFirst());
        Assertions.assertEquals("c", linked.getLast());
    }

    @Test
    public void testToCollection_HashSet() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 2, 3));
        HashSet<Integer> set = iter.toCollection(HashSet::new);
        Assertions.assertEquals(3, set.size());
    }

    // --- toImmutableList() (inherited from ImmutableIterator) ---

    @Test
    public void testToImmutableList() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ImmutableList<String> immutable = iter.toImmutableList();
        Assertions.assertEquals(3, immutable.size());
        Assertions.assertEquals("a", immutable.get(0));
        Assertions.assertEquals("c", immutable.get(2));
    }

    // --- toImmutableSet() (inherited from ImmutableIterator) ---

    @Test
    public void testToImmutableSet() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "a"));
        ImmutableSet<String> immutable = iter.toImmutableSet();
        Assertions.assertEquals(2, immutable.size());
        Assertions.assertTrue(immutable.contains("a"));
        Assertions.assertTrue(immutable.contains("b"));
    }

    // --- count() (inherited from ImmutableIterator) ---

    @Test
    public void testCount() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCount_AfterPartialIteration() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        iter.next();
        Assertions.assertEquals(2, iter.count());
    }

    @Test
    public void testOfArray_Empty() {
        String[] array = {};
        ObjListIterator<String> iter = ObjListIterator.of(array);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArray_Null() {
        ObjListIterator<String> iter = ObjListIterator.of((String[]) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArray_Single() {
        ObjListIterator<Integer> iter = ObjListIterator.of(42);
        Assertions.assertEquals(42, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayRange_SameIndex() {
        Integer[] numbers = { 1, 2, 3 };
        ObjListIterator<Integer> iter = ObjListIterator.of(numbers, 1, 1);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfList_Null() {
        List<String> list = null;
        ObjListIterator<String> iter = ObjListIterator.of(list);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfList_Empty() {
        ObjListIterator<String> iter = ObjListIterator.of(new ArrayList<>());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfListIterator_Null() {
        ListIterator<String> listIter = null;
        ObjListIterator<String> iter = ObjListIterator.of(listIter);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfListIterator_WrapsObjListIterator() {
        ObjListIterator<String> original = ObjListIterator.of(Arrays.asList("a", "b"));
        ObjListIterator<String> wrapped = ObjListIterator.of((ListIterator<String>) original);
        // Should return the same instance
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testOfArrayRange_Invalid() {
        Integer[] numbers = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, 1, 5));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, 2, 1));
    }

    @Test
    public void testOfArrayRange_NullArray() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of((Integer[]) null, 0, 1));
    }

    @Test
    public void testOfListIterator_SetThrows() {
        List<String> list = Arrays.asList("a", "b");
        ObjListIterator<String> iter = ObjListIterator.of(list.listIterator());
        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.set("x"));
    }

    @Test
    public void testOfListIterator_AddThrows() {
        List<String> list = Arrays.asList("a", "b");
        ObjListIterator<String> iter = ObjListIterator.of(list.listIterator());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.add("x"));
    }

    // --- set() and add() (UnsupportedOperationException) ---

    @Test
    public void testSetUnsupported() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.set("x"));
    }

    @Test
    public void testAddUnsupported() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.add("x"));
    }

    // --- remove() (inherited from ImmutableIterator) ---

    @Test
    public void testRemoveUnsupported() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    // --- skip() ---

    @Test
    public void testSkip() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> skipped = iter.skip(2);

        Assertions.assertEquals(3, skipped.next());
        Assertions.assertEquals(4, skipped.next());
        Assertions.assertEquals(5, skipped.next());
        Assertions.assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_MoreThanSize() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        ObjListIterator<Integer> skipped = iter.skip(5);

        Assertions.assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_WithPrevious() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> skipped = iter.skip(2);

        skipped.next();
        skipped.next();
        Assertions.assertEquals(4, skipped.previous());
        Assertions.assertEquals(3, skipped.previous());
    }

    @Test
    public void testSkip_HasPrevious() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> skipped = iter.skip(2);

        // After skip, hasPrevious should reflect the underlying iterator
        Assertions.assertTrue(skipped.hasNext()); // trigger skip
        Assertions.assertTrue(skipped.hasPrevious()); // skipped elements are behind
    }

    @Test
    public void testSkip_NextIndex() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> skipped = iter.skip(2);
        skipped.hasNext(); // trigger skip
        Assertions.assertEquals(2, skipped.nextIndex());
    }

    @Test
    public void testSkip_PreviousIndex() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> skipped = iter.skip(2);
        skipped.hasNext(); // trigger skip
        Assertions.assertEquals(1, skipped.previousIndex());
    }

    @Test
    public void testSkip_Zero() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        ObjListIterator<Integer> skipped = iter.skip(0);

        Assertions.assertSame(iter, skipped);
        Assertions.assertEquals(1, skipped.next());
        Assertions.assertEquals(2, skipped.next());
        Assertions.assertEquals(3, skipped.next());
    }

    @Test
    public void testSkip_Negative() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testSkip_NextThrowsWhenExhausted() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2));
        ObjListIterator<Integer> skipped = iter.skip(5);
        Assertions.assertThrows(NoSuchElementException.class, () -> skipped.next());
    }

    @Test
    public void testSkip_SetThrows() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        ObjListIterator<Integer> skipped = iter.skip(1);
        skipped.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> skipped.set(99));
    }

    @Test
    public void testSkip_AddThrows() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        ObjListIterator<Integer> skipped = iter.skip(1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> skipped.add(99));
    }

    // --- limit() ---

    @Test
    public void testLimit() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e"));
        ObjListIterator<String> limited = iter.limit(3);

        Assertions.assertEquals("a", limited.next());
        Assertions.assertEquals("b", limited.next());
        Assertions.assertEquals("c", limited.next());
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_MoreThanSize() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        ObjListIterator<String> limited = iter.limit(5);

        Assertions.assertEquals("a", limited.next());
        Assertions.assertEquals("b", limited.next());
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_WithPrevious() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e"));
        ObjListIterator<String> limited = iter.limit(3);

        limited.next();
        limited.next();
        limited.next();
        Assertions.assertFalse(limited.hasNext());
        Assertions.assertEquals("c", limited.previous());
        Assertions.assertEquals("b", limited.previous());
    }

    @Test
    public void testLimit_HasPrevious() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(2);
        limited.next();
        Assertions.assertTrue(limited.hasPrevious());
    }

    @Test
    public void testLimit_NextIndex() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(2);
        Assertions.assertEquals(0, limited.nextIndex());
        limited.next();
        Assertions.assertEquals(1, limited.nextIndex());
    }

    @Test
    public void testLimit_PreviousIndex() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(2);
        Assertions.assertEquals(-1, limited.previousIndex());
        limited.next();
        Assertions.assertEquals(0, limited.previousIndex());
    }

    // --- skip + limit combined ---

    @Test
    public void testSkipAndLimit() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> sliced = iter.skip(1).limit(2);

        Assertions.assertEquals(2, sliced.next());
        Assertions.assertEquals(3, sliced.next());
        Assertions.assertFalse(sliced.hasNext());
    }

    @Test
    public void testLimit_Zero() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(0);

        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_Negative() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testLimit_NextThrowsWhenExhausted() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(1);
        limited.next();
        Assertions.assertThrows(NoSuchElementException.class, () -> limited.next());
    }

    @Test
    public void testLimit_SetThrows() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(2);
        limited.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> limited.set("x"));
    }

    @Test
    public void testLimit_AddThrows() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(2);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> limited.add("x"));
    }

    // --- firstNonNull() ---

    @Test
    public void testFirstNonNull() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList(null, null, "found", "next"));
        Optional<String> first = iter.firstNonNull();

        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("found", first.get());
    }

    @Test
    public void testFirstNonNull_AllNull() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList(null, null, null));
        Optional<String> first = iter.firstNonNull();

        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testFirstNonNull_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Optional<String> first = iter.firstNonNull();
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testFirstNonNull_FirstIsNonNull() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Optional<String> first = iter.firstNonNull();
        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("a", first.get());
    }

    // --- toArray() ---

    @Test
    public void testToArray() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        Object[] array = iter.toArray();

        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    // --- toArray(A[]) ---

    @Test
    public void testToArrayWithType() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        String[] array = iter.toArray(new String[0]);

        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testToArrayWithType_LargerArray() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        String[] array = iter.toArray(new String[5]);
        Assertions.assertEquals(5, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
    }

    @Test
    public void testToArray_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Object[] array = iter.toArray();
        Assertions.assertEquals(0, array.length);
    }

    // --- toList() ---

    @Test
    public void testToList() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        List<Integer> list = iter.toList();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

    @Test
    public void testToList_Empty() {
        ObjListIterator<Integer> iter = ObjListIterator.empty();
        List<Integer> list = iter.toList();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testToList_Mutable() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a"));
        List<String> list = iter.toList();
        list.add("b"); // should not throw
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testStream_Filter() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        long count = iter.stream().filter(x -> x > 3).count();
        Assertions.assertEquals(2, count);
    }

    // --- stream() ---

    @Test
    public void testStream() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        Stream<String> stream = iter.stream();

        Assertions.assertNotNull(stream);
        long count = stream.count();
        Assertions.assertEquals(3, count);
    }

    @Test
    public void testStream_Empty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertEquals(0, iter.stream().count());
    }

    // --- foreachRemaining() ---

    @Test
    public void testForeachRemaining() throws Exception {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        List<String> result = new ArrayList<>();

        iter.foreachRemaining(result::add);

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    public void testForeachRemaining_NullAction() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    @Test
    public void testForeachRemaining_Empty() throws Exception {
        ObjListIterator<String> iter = ObjListIterator.empty();
        List<String> result = new ArrayList<>();
        iter.foreachRemaining(result::add);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testForeachRemaining_AfterPartialIteration() throws Exception {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        iter.next(); // consume first
        List<String> result = new ArrayList<>();
        iter.foreachRemaining(result::add);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("b", result.get(0));
    }

    // --- foreachIndexed() ---

    @Test
    public void testForeachIndexed() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjListIterator<String> iter = ObjListIterator.of(list);
        iter.next();

        List<String> results = new ArrayList<>();
        iter.foreachIndexed((index, value) -> results.add(index + ": " + value));

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("0: b", results.get(0));
        Assertions.assertEquals("1: c", results.get(1));
    }

    @Test
    public void testForeachIndexed_NullAction() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    @Test
    public void testForeachIndexed_Empty() throws Exception {
        ObjListIterator<String> iter = ObjListIterator.empty();
        List<String> results = new ArrayList<>();
        iter.foreachIndexed((index, value) -> results.add(index + ": " + value));
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testForeachIndexed_FromStart() throws Exception {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("x", "y"));
        List<String> results = new ArrayList<>();
        iter.foreachIndexed((index, value) -> results.add(index + "=" + value));
        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("0=x", results.get(0));
        Assertions.assertEquals("1=y", results.get(1));
    }
}
