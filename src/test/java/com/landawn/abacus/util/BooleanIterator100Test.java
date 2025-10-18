package com.landawn.abacus.util;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class BooleanIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextBoolean());
    }

    @Test
    public void testOf() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmpty() {
        BooleanIterator iter = BooleanIterator.of();
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRange() {
        boolean[] array = { true, false, true, false, true };
        BooleanIterator iter = BooleanIterator.of(array, 1, 4);
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithInvalidRange() {
        boolean[] array = { true, false, true };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            BooleanIterator.of(array, 2, 5);
        });
    }

    @Test
    public void testDefer() {
        Supplier<BooleanIterator> supplier = () -> BooleanIterator.of(true, false);
        BooleanIterator iter = BooleanIterator.defer(supplier);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
    }

    @Test
    public void testDeferWithNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            BooleanIterator.defer(null);
        });
    }

    @Test
    public void testGenerate() {
        int[] counter = { 0 };
        BooleanIterator iter = BooleanIterator.generate(() -> counter[0]++ % 2 == 0);
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.nextBoolean());
    }

    @Test
    public void testGenerateWithHasNext() {
        int[] counter = { 0 };
        BooleanIterator iter = BooleanIterator.generate(() -> counter[0] < 3, () -> true);
        Assertions.assertTrue(iter.nextBoolean());
        counter[0]++;
        Assertions.assertTrue(iter.nextBoolean());
        counter[0]++;
        Assertions.assertTrue(iter.nextBoolean());
        counter[0]++;
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testNext() {
        BooleanIterator iter = BooleanIterator.of(true);
        Boolean value = iter.next();
        Assertions.assertTrue(value);
    }

    @Test
    public void testSkip() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        BooleanIterator skipped = iter.skip(2);
        Assertions.assertTrue(skipped.nextBoolean());
        Assertions.assertFalse(skipped.nextBoolean());
        Assertions.assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipNegative() {
        BooleanIterator iter = BooleanIterator.of(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            iter.skip(-1);
        });
    }

    @Test
    public void testLimit() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        BooleanIterator limited = iter.limit(2);
        Assertions.assertTrue(limited.nextBoolean());
        Assertions.assertFalse(limited.nextBoolean());
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitZero() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        BooleanIterator limited = iter.limit(0);
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testFilter() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        BooleanIterator filtered = iter.filter(b -> b);
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertFalse(filtered.hasNext());
    }

    @Test
    public void testFirst() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        OptionalBoolean first = iter.first();
        Assertions.assertTrue(first.isPresent());
        Assertions.assertTrue(first.get());
    }

    @Test
    public void testFirstEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        OptionalBoolean first = iter.first();
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        OptionalBoolean last = iter.last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertTrue(last.get());
    }

    @Test
    public void testLastEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        OptionalBoolean last = iter.last();
        Assertions.assertFalse(last.isPresent());
    }

    @Test
    public void testToArray() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        boolean[] array = iter.toArray();
        Assertions.assertEquals(3, array.length);
        Assertions.assertTrue(array[0]);
        Assertions.assertFalse(array[1]);
        Assertions.assertTrue(array[2]);
    }

    @Test
    public void testToList() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        BooleanList list = iter.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertTrue(list.get(0));
        Assertions.assertFalse(list.get(1));
        Assertions.assertTrue(list.get(2));
    }

    @Test
    public void testStream() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        Stream<Boolean> stream = iter.stream();
        long count = stream.filter(b -> b).count();
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testIndexed() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        var indexed = iter.indexed();
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertTrue(first.value());
        IndexedBoolean second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertFalse(second.value());
    }

    @Test
    public void testIndexedWithStartIndex() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        var indexed = iter.indexed(10);
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(10, first.index());
        Assertions.assertTrue(first.value());
    }

    @Test
    public void testForeachRemaining() throws Exception {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        boolean[] values = new boolean[3];
        int[] index = { 0 };
        iter.foreachRemaining(b -> values[index[0]++] = b);
        Assertions.assertTrue(values[0]);
        Assertions.assertFalse(values[1]);
        Assertions.assertTrue(values[2]);
    }

    @Test
    public void testForeachIndexed() throws Exception {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        boolean[] values = new boolean[3];
        iter.foreachIndexed((idx, value) -> values[idx] = value);
        Assertions.assertTrue(values[0]);
        Assertions.assertFalse(values[1]);
        Assertions.assertTrue(values[2]);
    }
}
