package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class BooleanIterator2025Test extends TestBase {

    @Test
    public void testEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        Assertions.assertNotNull(iter);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testEmptyThrowsOnNext() {
        BooleanIterator iter = BooleanIterator.empty();
        Assertions.assertThrows(NoSuchElementException.class, iter::nextBoolean);
    }

    @Test
    public void testEmptyToArray() {
        BooleanIterator iter = BooleanIterator.empty();
        boolean[] array = iter.toArray();
        Assertions.assertEquals(0, array.length);
    }

    @Test
    public void testEmptyToList() {
        BooleanIterator iter = BooleanIterator.empty();
        BooleanList list = iter.toList();
        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void testOfVarargs() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        BooleanIterator iter = BooleanIterator.of();
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfNullArray() {
        boolean[] nullArray = null;
        BooleanIterator iter = BooleanIterator.of(nullArray);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfSingleElement() {
        BooleanIterator iter = BooleanIterator.of(true);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfAllTrue() {
        BooleanIterator iter = BooleanIterator.of(true, true, true);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(iter.nextBoolean());
        }
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfAllFalse() {
        BooleanIterator iter = BooleanIterator.of(false, false, false);
        for (int i = 0; i < 3; i++) {
            Assertions.assertFalse(iter.nextBoolean());
        }
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
    public void testOfWithRangeFullArray() {
        boolean[] array = { true, false, true };
        BooleanIterator iter = BooleanIterator.of(array, 0, 3);
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRangeEmptyRange() {
        boolean[] array = { true, false, true };
        BooleanIterator iter = BooleanIterator.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRangeInvalidIndices() {
        boolean[] array = { true, false, true };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of(array, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of(array, 2, 1));
    }

    @Test
    public void testOfWithRangeNullArray() {
        BooleanIterator iter = BooleanIterator.of(null, 0, 0);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRangeToArray() {
        boolean[] array = { true, false, true, false, true };
        BooleanIterator iter = BooleanIterator.of(array, 1, 4);
        boolean[] result = iter.toArray();
        Assertions.assertArrayEquals(new boolean[] { false, true, false }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRangeToList() {
        boolean[] array = { true, false, true, false, true };
        BooleanIterator iter = BooleanIterator.of(array, 1, 4);
        BooleanList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertFalse(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertFalse(result.get(2));
    }

    @Test
    public void testDefer() {
        AtomicBoolean initialized = new AtomicBoolean(false);
        BooleanIterator iter = BooleanIterator.defer(() -> {
            initialized.set(true);
            return BooleanIterator.of(true, false);
        });

        Assertions.assertFalse(initialized.get());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(initialized.get());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
    }

    @Test
    public void testDeferNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BooleanIterator.defer(null));
    }

    @Test
    public void testDeferInitOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        BooleanIterator iter = BooleanIterator.defer(() -> {
            callCount.incrementAndGet();
            return BooleanIterator.of(true);
        });

        iter.hasNext();
        iter.hasNext();
        iter.nextBoolean();
        Assertions.assertEquals(1, callCount.get());
    }

    @Test
    public void testDeferWithEmptyIterator() {
        BooleanIterator iter = BooleanIterator.defer(BooleanIterator::empty);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateInfinite() {
        AtomicBoolean toggle = new AtomicBoolean(true);
        BooleanIterator iter = BooleanIterator.generate(() -> toggle.getAndSet(!toggle.get()));

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
    }

    @Test
    public void testGenerateInfiniteNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BooleanIterator.generate((java.util.function.BooleanSupplier) null));
    }

    @Test
    public void testGenerateInfiniteAlwaysTrue() {
        BooleanIterator iter = BooleanIterator.generate(() -> true);
        for (int i = 0; i < 100; i++) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertTrue(iter.nextBoolean());
        }
    }

    @Test
    public void testGenerateWithCondition() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanIterator iter = BooleanIterator.generate(() -> counter.get() < 3, () -> counter.getAndIncrement() % 2 == 0);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateWithConditionNullHasNext() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BooleanIterator.generate(null, () -> true));
    }

    @Test
    public void testGenerateWithConditionNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BooleanIterator.generate(() -> true, null));
    }

    @Test
    public void testGenerateWithConditionThrowsWhenExhausted() {
        BooleanIterator iter = BooleanIterator.generate(() -> false, () -> true);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, iter::nextBoolean);
    }

    @Test
    public void testGenerateWithConditionNoElements() {
        BooleanIterator iter = BooleanIterator.generate(() -> false, () -> true);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testNextBoolean() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        Assertions.assertTrue(iter.nextBoolean());
        Assertions.assertFalse(iter.nextBoolean());
        Assertions.assertTrue(iter.nextBoolean());
    }

    @Test
    public void testNextBooleanThrowsWhenEmpty() {
        BooleanIterator iter = BooleanIterator.of(true);
        iter.nextBoolean();
        Assertions.assertThrows(NoSuchElementException.class, iter::nextBoolean);
    }

    @Test
    public void testNextDeprecated() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertEquals(Boolean.TRUE, iter.next());
        Assertions.assertEquals(Boolean.FALSE, iter.next());
    }

    @Test
    public void testNextDeprecatedBoxing() {
        BooleanIterator iter = BooleanIterator.of(true);
        Boolean result = iter.next();
        Assertions.assertInstanceOf(Boolean.class, result);
        Assertions.assertTrue(result);
    }

    @Test
    public void testSkip() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true);
        BooleanIterator skipped = iter.skip(2);
        Assertions.assertTrue(skipped.nextBoolean());
        Assertions.assertFalse(skipped.nextBoolean());
        Assertions.assertTrue(skipped.nextBoolean());
        Assertions.assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipZero() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        BooleanIterator skipped = iter.skip(0);
        Assertions.assertSame(iter, skipped);
    }

    @Test
    public void testSkipNegative() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testSkipAll() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        BooleanIterator skipped = iter.skip(10);
        Assertions.assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipPartial() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        BooleanIterator skipped = iter.skip(2);
        Assertions.assertTrue(skipped.hasNext());
        boolean[] result = skipped.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testSkipLazyEvaluation() {
        AtomicBoolean skipped = new AtomicBoolean(false);
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        BooleanIterator skippedIter = iter.skip(1);

        Assertions.assertFalse(skipped.get());
        skippedIter.hasNext();
        Assertions.assertFalse(iter.hasNext() && iter.nextBoolean());
    }

    @Test
    public void testLimit() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true);
        BooleanIterator limited = iter.limit(3);
        Assertions.assertTrue(limited.nextBoolean());
        Assertions.assertFalse(limited.nextBoolean());
        Assertions.assertTrue(limited.nextBoolean());
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitZero() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        BooleanIterator limited = iter.limit(0);
        Assertions.assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitNegative() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testLimitMoreThanAvailable() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        BooleanIterator limited = iter.limit(10);
        boolean[] result = limited.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testLimitExact() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        BooleanIterator limited = iter.limit(3);
        boolean[] result = limited.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testFilter() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true);
        BooleanIterator filtered = iter.filter(b -> b);
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterNone() {
        BooleanIterator iter = BooleanIterator.of(true, true, true);
        BooleanIterator filtered = iter.filter(b -> !b);
        Assertions.assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterAll() {
        BooleanIterator iter = BooleanIterator.of(true, true, true);
        BooleanIterator filtered = iter.filter(b -> b);
        boolean[] result = filtered.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, true, true }, result);
    }

    @Test
    public void testFilterNullPredicate() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFilterFalseValues() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, false);
        BooleanIterator filtered = iter.filter(b -> !b);
        boolean[] result = filtered.toArray();
        Assertions.assertArrayEquals(new boolean[] { false, false, false }, result);
    }

    @Test
    public void testFilterAlternating() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        AtomicBoolean toggle = new AtomicBoolean(true);
        BooleanIterator filtered = iter.filter(b -> {
            boolean result = toggle.get();
            toggle.set(!toggle.get());
            return result;
        });
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertTrue(filtered.nextBoolean());
        Assertions.assertFalse(filtered.hasNext());
    }

    @Test
    public void testFirst() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
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
    public void testFirstFalseValue() {
        BooleanIterator iter = BooleanIterator.of(false, true);
        OptionalBoolean first = iter.first();
        Assertions.assertTrue(first.isPresent());
        Assertions.assertFalse(first.get());
    }

    @Test
    public void testFirstConsumesOneElement() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        iter.first();
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertFalse(iter.nextBoolean());
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
    public void testLastFalseValue() {
        BooleanIterator iter = BooleanIterator.of(true, true, false);
        OptionalBoolean last = iter.last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertFalse(last.get());
    }

    @Test
    public void testLastConsumesAll() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        iter.last();
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLastSingleElement() {
        BooleanIterator iter = BooleanIterator.of(true);
        OptionalBoolean last = iter.last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertTrue(last.get());
    }

    @Test
    public void testToArray() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        boolean[] array = iter.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, false, true }, array);
    }

    @Test
    public void testToArrayEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        boolean[] array = iter.toArray();
        Assertions.assertEquals(0, array.length);
    }

    @Test
    public void testToArrayConsumesIterator() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        iter.toArray();
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayAfterPartialConsumption() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        iter.nextBoolean();
        iter.nextBoolean();
        boolean[] array = iter.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, false }, array);
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
    public void testToListEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        BooleanList list = iter.toList();
        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void testToListConsumesIterator() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        iter.toList();
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListAfterPartialConsumption() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        iter.nextBoolean();
        BooleanList list = iter.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertFalse(list.get(0));
        Assertions.assertTrue(list.get(1));
        Assertions.assertFalse(list.get(2));
    }

    @Test
    public void testStream() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        Stream<Boolean> stream = iter.stream();
        Assertions.assertNotNull(stream);
        long trueCount = stream.filter(b -> b).count();
        Assertions.assertEquals(2, trueCount);
    }

    @Test
    public void testStreamEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        Stream<Boolean> stream = iter.stream();
        Assertions.assertEquals(0, stream.count());
    }

    @Test
    public void testStreamCollect() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        Stream<Boolean> stream = iter.stream();
        java.util.List<Boolean> list = stream.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertTrue(list.get(0));
        Assertions.assertFalse(list.get(1));
        Assertions.assertTrue(list.get(2));
    }

    @Test
    public void testIndexed() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        ObjIterator<IndexedBoolean> indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertTrue(first.value());

        IndexedBoolean second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertFalse(second.value());

        IndexedBoolean third = indexed.next();
        Assertions.assertEquals(2, third.index());
        Assertions.assertTrue(third.value());

        Assertions.assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        ObjIterator<IndexedBoolean> indexed = iter.indexed();
        Assertions.assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedSingleElement() {
        BooleanIterator iter = BooleanIterator.of(true);
        ObjIterator<IndexedBoolean> indexed = iter.indexed();
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertTrue(first.value());
    }

    @Test
    public void testIndexedWithStartIndex() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        ObjIterator<IndexedBoolean> indexed = iter.indexed(10);

        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(10, first.index());
        Assertions.assertTrue(first.value());

        IndexedBoolean second = indexed.next();
        Assertions.assertEquals(11, second.index());
        Assertions.assertFalse(second.value());
    }

    @Test
    public void testIndexedWithStartIndexNegative() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testIndexedWithStartIndexZero() {
        BooleanIterator iter = BooleanIterator.of(true);
        ObjIterator<IndexedBoolean> indexed = iter.indexed(0);
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(0, first.index());
    }

    @Test
    public void testIndexedWithStartIndexLarge() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        ObjIterator<IndexedBoolean> indexed = iter.indexed(1000);
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(1000, first.index());
        IndexedBoolean second = indexed.next();
        Assertions.assertEquals(1001, second.index());
    }

    @Test
    public void testForEachRemainingDeprecated() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        java.util.List<Boolean> result = new java.util.ArrayList<>();
        iter.forEachRemaining(result::add);
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.get(0));
        Assertions.assertFalse(result.get(1));
        Assertions.assertTrue(result.get(2));
    }

    @Test
    public void testForEachRemainingDeprecatedEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining(b -> count.incrementAndGet());
        Assertions.assertEquals(0, count.get());
    }

    @Test
    public void testForeachRemaining() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        iter.foreachRemaining(b -> {
            if (b) {
                trueCount.incrementAndGet();
            } else {
                falseCount.incrementAndGet();
            }
        });

        Assertions.assertEquals(2, trueCount.get());
        Assertions.assertEquals(2, falseCount.get());
    }

    @Test
    public void testForeachRemainingEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        iter.foreachRemaining(b -> called.set(true));
        Assertions.assertFalse(called.get());
    }

    @Test
    public void testForeachRemainingNullAction() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    @Test
    public void testForeachRemainingAfterPartialConsumption() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        iter.nextBoolean();
        iter.nextBoolean();

        AtomicInteger count = new AtomicInteger(0);
        iter.foreachRemaining(b -> count.incrementAndGet());
        Assertions.assertEquals(2, count.get());
    }

    @Test
    public void testForeachRemainingSingleElement() {
        BooleanIterator iter = BooleanIterator.of(true);
        AtomicBoolean result = new AtomicBoolean(false);
        iter.foreachRemaining(b -> result.set(b));
        Assertions.assertTrue(result.get());
    }

    @Test
    public void testForeachIndexed() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        java.util.Map<Integer, Boolean> result = new java.util.HashMap<>();

        iter.foreachIndexed((idx, value) -> result.put(idx, value));

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.get(0));
        Assertions.assertFalse(result.get(1));
        Assertions.assertTrue(result.get(2));
    }

    @Test
    public void testForeachIndexedEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachIndexed((idx, value) -> count.incrementAndGet());
        Assertions.assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexedNullAction() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    @Test
    public void testForeachIndexedAfterPartialConsumption() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        iter.nextBoolean();

        java.util.List<Integer> indices = new java.util.ArrayList<>();
        iter.foreachIndexed((idx, value) -> indices.add(idx));

        Assertions.assertEquals(3, indices.size());
        Assertions.assertEquals(0, indices.get(0));
        Assertions.assertEquals(1, indices.get(1));
        Assertions.assertEquals(2, indices.get(2));
    }

    @Test
    public void testForeachIndexedOrder() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true);
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        java.util.List<Boolean> values = new java.util.ArrayList<>();

        iter.foreachIndexed((idx, value) -> {
            indices.add(idx);
            values.add(value);
        });

        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(i, indices.get(i));
        }
        Assertions.assertTrue(values.get(0));
        Assertions.assertFalse(values.get(1));
        Assertions.assertTrue(values.get(2));
        Assertions.assertFalse(values.get(3));
        Assertions.assertTrue(values.get(4));
    }

    @Test
    public void testChainedOperations() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true, false);
        boolean[] result = iter.skip(1).limit(4).filter(b -> b).toArray();
        Assertions.assertArrayEquals(new boolean[] { true, true }, result);
    }

    @Test
    public void testSkipAndLimit() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true);
        BooleanIterator modified = iter.skip(1).limit(3);
        boolean[] result = modified.toArray();
        Assertions.assertArrayEquals(new boolean[] { false, true, false }, result);
    }

    @Test
    public void testFilterAndLimit() {
        BooleanIterator iter = BooleanIterator.of(true, true, false, true, false, true);
        BooleanIterator modified = iter.filter(b -> b).limit(2);
        boolean[] result = modified.toArray();
        Assertions.assertArrayEquals(new boolean[] { true, true }, result);
    }

    @Test
    public void testMultipleConsumptions() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        Assertions.assertTrue(iter.nextBoolean());
        boolean[] remaining = iter.toArray();
        Assertions.assertArrayEquals(new boolean[] { false, true }, remaining);
    }

    @Test
    public void testFirstOnFilteredIterator() {
        BooleanIterator iter = BooleanIterator.of(false, false, true, false);
        OptionalBoolean first = iter.filter(b -> b).first();
        Assertions.assertTrue(first.isPresent());
        Assertions.assertTrue(first.get());
    }

    @Test
    public void testLastOnFilteredIterator() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true);
        OptionalBoolean last = iter.filter(b -> b).last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertTrue(last.get());
    }

    @Test
    public void testComplexChain() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false, true, false, true, false);
        long count = iter.skip(1).limit(6).filter(b -> !b).stream().count();
        Assertions.assertEquals(3, count);
    }

    @Test
    public void testHasNextMultipleCalls() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.hasNext());
        iter.nextBoolean();
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.hasNext());
        iter.nextBoolean();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testIndexedAfterSkip() {
        BooleanIterator iter = BooleanIterator.of(true, false, true, false);
        ObjIterator<IndexedBoolean> indexed = iter.skip(2).indexed(10);
        IndexedBoolean first = indexed.next();
        Assertions.assertEquals(10, first.index());
        Assertions.assertTrue(first.value());
    }

    @Test
    public void testStreamFromGenerated() {
        AtomicInteger count = new AtomicInteger(0);
        BooleanIterator iter = BooleanIterator.generate(() -> count.get() < 5, () -> count.getAndIncrement() % 2 == 0);
        long trueCount = iter.stream().filter(b -> b).count();
        Assertions.assertEquals(3, trueCount);
    }
}
