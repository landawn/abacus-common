package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public class ObjListIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertEquals(0, iter.nextIndex());
        Assertions.assertEquals(-1, iter.previousIndex());
    }

    @Test
    public void testEmptyNextThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmptyPreviousThrows() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.previous());
    }

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
    public void testOfArray() {
        String[] array = {"one", "two", "three"};
        ObjListIterator<String> iter = ObjListIterator.of(array);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("one", iter.next());
        Assertions.assertEquals("two", iter.next());
        Assertions.assertEquals("three", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        String[] array = {};
        ObjListIterator<String> iter = ObjListIterator.of(array);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayRange() {
        Integer[] numbers = {1, 2, 3, 4, 5};
        ObjListIterator<Integer> iter = ObjListIterator.of(numbers, 1, 4);
        
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(4, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayRangeInvalid() {
        Integer[] numbers = {1, 2, 3};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, 1, 5));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, 2, 1));
    }

    @Test
    public void testOfList() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjListIterator<String> iter = ObjListIterator.of(list);
        
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfNullList() {
        List<String> list = null;
        ObjListIterator<String> iter = ObjListIterator.of(list);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfListIterator() {
        List<String> list = Arrays.asList("x", "y", "z");
        ListIterator<String> listIter = list.listIterator();
        ObjListIterator<String> iter = ObjListIterator.of(listIter);
        
        Assertions.assertEquals("x", iter.next());
        Assertions.assertEquals("y", iter.next());
        Assertions.assertEquals("z", iter.next());
    }

    @Test
    public void testOfNullListIterator() {
        ListIterator<String> listIter = null;
        ObjListIterator<String> iter = ObjListIterator.of(listIter);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testBidirectionalIteration() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        ObjListIterator<String> iter = ObjListIterator.of(list);
        
        iter.next(); // "a"
        iter.next(); // "b"
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
    public void testSkipZero() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        ObjListIterator<Integer> skipped = iter.skip(0);
        
        Assertions.assertEquals(1, skipped.next());
        Assertions.assertEquals(2, skipped.next());
        Assertions.assertEquals(3, skipped.next());
    }

    @Test
    public void testSkipMoreThanSize() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        ObjListIterator<Integer> skipped = iter.skip(5);
        
        Assertions.assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipNegative() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

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
    public void testLimitZero() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        ObjListIterator<String> limited = iter.limit(0);
        
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitMoreThanSize() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        ObjListIterator<String> limited = iter.limit(5);
        
        Assertions.assertEquals("a", limited.next());
        Assertions.assertEquals("b", limited.next());
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitNegative() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testFirst() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("first", "second"));
        Nullable<String> first = iter.first();
        
        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("first", first.get());
    }

    @Test
    public void testFirstEmpty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        Nullable<String> first = iter.first();
        
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testFirstNonNull() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList(null, null, "found", "next"));
        Optional<String> first = iter.firstNonNull();
        
        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("found", first.get());
    }

    @Test
    public void testFirstNonNullAllNull() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList(null, null, null));
        Optional<String> first = iter.firstNonNull();
        
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4));
        Nullable<Integer> last = iter.last();
        
        Assertions.assertTrue(last.isPresent());
        Assertions.assertEquals(4, last.get());
    }

    @Test
    public void testLastEmpty() {
        ObjListIterator<Integer> iter = ObjListIterator.empty();
        Nullable<Integer> last = iter.last();
        
        Assertions.assertFalse(last.isPresent());
    }

    @Test
    public void testToArray() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        Object[] array = iter.toArray();
        
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

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
    public void testToList() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
        List<Integer> list = iter.toList();
        
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

    @Test
    public void testStream() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        Stream<String> stream = iter.stream();
        
        Assertions.assertNotNull(stream);
        long count = stream.count();
        Assertions.assertEquals(3, count);
    }

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
    public void testForeachRemainingNullAction() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    @Test
    public void testForeachIndexed() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjListIterator<String> iter = ObjListIterator.of(list);
        iter.next(); // Skip first element
        
        List<String> results = new ArrayList<>();
        iter.foreachIndexed((index, value) -> results.add(index + ": " + value));
        
        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("0: b", results.get(0));
        Assertions.assertEquals("1: c", results.get(1));
    }

    @Test
    public void testForeachIndexedNullAction() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

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

    @Test
    public void testSkipWithPrevious() {
        ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
        ObjListIterator<Integer> skipped = iter.skip(2);
        
        skipped.next(); // 3
        skipped.next(); // 4
        Assertions.assertEquals(4, skipped.previous());
        Assertions.assertEquals(3, skipped.previous());
    }

    @Test
    public void testLimitWithPrevious() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e"));
        ObjListIterator<String> limited = iter.limit(3);
        
        limited.next(); // "a"
        limited.next(); // "b"
        limited.next(); // "c"
        Assertions.assertFalse(limited.hasNext());
        Assertions.assertEquals("c", limited.previous());
        Assertions.assertEquals("b", limited.previous());
    }
}