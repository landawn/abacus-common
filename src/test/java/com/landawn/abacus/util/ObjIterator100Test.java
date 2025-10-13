package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;

@Tag("new-test")
public class ObjIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        ObjIterator<String> iter = ObjIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testJust() {
        ObjIterator<String> iter = ObjIterator.just("test");
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("test", iter.next());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOf_Array() {
        String[] array = { "a", "b", "c" };
        ObjIterator<String> iter = ObjIterator.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_EmptyArray() {
        String[] array = {};
        ObjIterator<String> iter = ObjIterator.of(array);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_NullArray() {
        ObjIterator<String> iter = ObjIterator.of((String[]) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_ArrayRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        ObjIterator<Integer> iter = ObjIterator.of(array, 1, 4);

        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(4, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_ArrayRange_InvalidIndices() {
        Integer[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, 0, 4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, 2, 1));
    }

    @Test
    public void testOf_Iterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIterator<String> iter = ObjIterator.of(list.iterator());

        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_NullIterator() {
        ObjIterator<String> iter = ObjIterator.of((Iterator<String>) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_Collection() {
        List<String> list = Arrays.asList("x", "y", "z");
        ObjIterator<String> iter = ObjIterator.of(list);

        Assertions.assertEquals("x", iter.next());
        Assertions.assertEquals("y", iter.next());
        Assertions.assertEquals("z", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_Iterable() {
        Iterable<Integer> iterable = Arrays.asList(1, 2, 3);
        ObjIterator<Integer> iter = ObjIterator.of(iterable);

        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer() {
        boolean[] supplierCalled = { false };
        Supplier<Iterator<String>> supplier = () -> {
            supplierCalled[0] = true;
            return Arrays.asList("a", "b").iterator();
        };

        ObjIterator<String> iter = ObjIterator.defer(supplier);
        Assertions.assertFalse(supplierCalled[0]);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(supplierCalled[0]);

        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_InfiniteSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.generate(() -> counter.incrementAndGet());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_WithHasNextAndSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.generate(() -> counter.get() < 3, () -> counter.incrementAndGet());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_WithInitAndPredicate() {
        ObjIterator<Integer> iter = ObjIterator.generate(new int[] { 0 }, state -> state[0] < 3, state -> state[0]++);

        Assertions.assertEquals(0, iter.next());
        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_WithInitAndBiPredicate() {
        ObjIterator<Integer> iter = ObjIterator.generate(0, (state, prev) -> prev == null || prev < 3, (state, prev) -> prev == null ? 1 : prev + 1);

        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5).skip(2);

        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(4, iter.next());
        Assertions.assertEquals(5, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_Zero() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).skip(0);
        Assertions.assertEquals(1, iter.next());
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2).skip(5);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5).limit(3);

        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_Zero() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).limit(0);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5, 6).skipAndLimit(2, 2);

        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(4, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0);

        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(4, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testMap() {
        ObjIterator<String> iter = ObjIterator.of(1, 2, 3).map(n -> "num" + n);

        Assertions.assertEquals("num1", iter.next());
        Assertions.assertEquals("num2", iter.next());
        Assertions.assertEquals("num3", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinct() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 2, 3, 3, 3).distinct();

        Assertions.assertEquals(1, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctBy() {
        ObjIterator<String> iter = ObjIterator.of("a", "bb", "ccc", "dd", "e").distinctBy(String::length);

        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("bb", iter.next());
        Assertions.assertEquals("ccc", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFirst() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        Nullable<String> first = iter.first();

        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("a", first.get());
    }

    @Test
    public void testFirst_Empty() {
        ObjIterator<String> iter = ObjIterator.empty();
        Nullable<String> first = iter.first();

        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testFirstNonNull() {
        ObjIterator<String> iter = ObjIterator.of(null, null, "found", "next");
        u.Optional<String> first = iter.firstNonNull();

        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("found", first.get());
    }

    @Test
    public void testFirstNonNull_AllNull() {
        ObjIterator<String> iter = ObjIterator.of(null, null, null);
        u.Optional<String> first = iter.firstNonNull();

        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4);
        Nullable<Integer> last = iter.last();

        Assertions.assertTrue(last.isPresent());
        Assertions.assertEquals(4, last.get());
    }

    @Test
    public void testLast_Empty() {
        ObjIterator<String> iter = ObjIterator.empty();
        Nullable<String> last = iter.last();

        Assertions.assertFalse(last.isPresent());
    }

    @Test
    public void testSkipNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b", null, "c").skipNulls();

        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        Object[] array = iter.toArray();

        Assertions.assertArrayEquals(new Object[] { "a", "b", "c" }, array);
    }

    @Test
    public void testToArray_WithType() {
        ObjIterator<String> iter = ObjIterator.of("x", "y", "z");
        String[] array = iter.toArray(new String[0]);

        Assertions.assertArrayEquals(new String[] { "x", "y", "z" }, array);
    }

    @Test
    public void testToList() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3);
        List<Integer> list = iter.toList();

        Assertions.assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testStream() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        long count = iter.stream().count();

        Assertions.assertEquals(3, count);
    }

    @Test
    public void testIndexed() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        ObjIterator<Indexed<String>> indexed = iter.indexed();

        Indexed<String> i1 = indexed.next();
        Assertions.assertEquals(0, i1.index());
        Assertions.assertEquals("a", i1.value());

        Indexed<String> i2 = indexed.next();
        Assertions.assertEquals(1, i2.index());
        Assertions.assertEquals("b", i2.value());
    }

    @Test
    public void testIndexed_WithStartIndex() {
        ObjIterator<String> iter = ObjIterator.of("x", "y");
        ObjIterator<Indexed<String>> indexed = iter.indexed(10);

        Indexed<String> i1 = indexed.next();
        Assertions.assertEquals(10, i1.index());
        Assertions.assertEquals("x", i1.value());

        Indexed<String> i2 = indexed.next();
        Assertions.assertEquals(11, i2.index());
        Assertions.assertEquals("y", i2.value());
    }

    @Test
    public void testForeachRemaining() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        List<String> collected = new ArrayList<>();

        iter.foreachRemaining(collected::add);

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testForeachIndexed() {
        ObjIterator<String> iter = ObjIterator.of("x", "y", "z");
        List<String> collected = new ArrayList<>();

        iter.foreachIndexed((index, value) -> collected.add(index + ":" + value));

        Assertions.assertEquals(Arrays.asList("0:x", "1:y", "2:z"), collected);
    }

    @Test
    public void testNegativeSkip() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skip(-1));
    }

    @Test
    public void testNegativeLimit() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).limit(-1));
    }

    @Test
    public void testNegativeStartIndex() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.of("a", "b").indexed(-1));
    }

    @Test
    public void testNullIteratorSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.defer(null));
    }

    @Test
    public void testNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate((Supplier<String>) null));
    }

    @Test
    public void testNullHasNext() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(null, () -> "test"));
    }

    @Test
    public void testNullPredicate() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate("init", null, state -> state));
    }

    @Test
    public void testNullBiPredicate() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate("init", null, (state, prev) -> state));
    }

    @Test
    public void testNullConsumer() {
        ObjIterator<String> iter = ObjIterator.of("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    @Test
    public void testNullIntObjConsumer() {
        ObjIterator<String> iter = ObjIterator.of("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    @Test
    public void testEmptySkipNulls() {
        ObjIterator<String> iter = ObjIterator.<String> empty().skipNulls();
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray_FromRangeIterator() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        ObjIterator<Integer> iter = ObjIterator.of(array, 1, 4);
        Integer[] result = iter.toArray(new Integer[0]);

        Assertions.assertArrayEquals(new Integer[] { 2, 3, 4 }, result);
    }

    @Test
    public void testToList_FromRangeIterator() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        ObjIterator<Integer> iter = ObjIterator.of(array, 1, 4);
        List<Integer> result = iter.toList();

        Assertions.assertEquals(Arrays.asList(2, 3, 4), result);
    }
}
