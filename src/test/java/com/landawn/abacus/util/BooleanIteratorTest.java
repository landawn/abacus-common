package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

public class BooleanIteratorTest extends TestBase {

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final BooleanIterator iter = BooleanIterator.of(true, false, true);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertEquals(List.of(false, true), iter.stream().toList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        BooleanIterator iter = BooleanIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextBoolean);
        assertSame(BooleanIterator.EMPTY, BooleanIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        BooleanIterator iter = BooleanIterator.of(true, false, true);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextBoolean);

        assertFalse(BooleanIterator.of().hasNext());
        assertFalse(BooleanIterator.of((boolean[]) null).hasNext());
        assertTrue(BooleanIterator.of(true).nextBoolean());

        BooleanIterator allTrue = BooleanIterator.of(true, true, true);
        assertTrue(allTrue.nextBoolean());
        assertTrue(allTrue.nextBoolean());
        assertTrue(allTrue.nextBoolean());
        assertFalse(allTrue.hasNext());

        BooleanIterator allFalse = BooleanIterator.of(false, false, false);
        assertFalse(allFalse.nextBoolean());
        assertFalse(allFalse.nextBoolean());
        assertFalse(allFalse.nextBoolean());
        assertFalse(allFalse.hasNext());
    }

    @Test
    public void testOf_Range() {
        boolean[] array = { true, false, true, false, true };
        assertArrayEquals(new boolean[] { false, true, false }, BooleanIterator.of(array, 1, 4).toArray());
        assertEquals(BooleanList.of(false, true, false), BooleanIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, BooleanIterator.of(array, 0, array.length).toArray());
        assertFalse(BooleanIterator.of(array, 1, 1).hasNext());
        assertFalse(BooleanIterator.of(null, 0, 0).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of(array, 0, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanIterator.of((boolean[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        BooleanIterator iter = BooleanIterator.defer(() -> {
            calls.incrementAndGet();
            return BooleanIterator.of(true, false);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertEquals(1, calls.get());

        boolean[] calledOnNext = { false };
        BooleanIterator onNext = BooleanIterator.defer(() -> {
            calledOnNext[0] = true;
            return BooleanIterator.of(true);
        });
        assertTrue(onNext.nextBoolean());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        BooleanIterator failing = BooleanIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertFalse(BooleanIterator.defer(BooleanIterator::empty).hasNext());
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        BooleanIterator infinite = BooleanIterator.generate(() -> n.getAndIncrement() % 2 == 0);
        assertTrue(infinite.nextBoolean());
        assertFalse(infinite.nextBoolean());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        BooleanIterator finite = BooleanIterator.generate(() -> counter.get() < 3, () -> counter.getAndIncrement() % 2 == 0);
        assertTrue(finite.nextBoolean());
        assertFalse(finite.nextBoolean());
        assertTrue(finite.nextBoolean());
        assertFalse(finite.hasNext());
        assertThrows(NoSuchElementException.class, finite::nextBoolean);
        assertFalse(BooleanIterator.generate(() -> false, () -> true).hasNext());
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.generate((BooleanSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.generate(null, () -> true));
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.generate((BooleanSupplier) () -> true, null));

        AtomicInteger budget = new AtomicInteger(3);
        BooleanIterator cached = BooleanIterator.generate(() -> budget.getAndDecrement() > 0, () -> true);
        assertTrue(cached.hasNext());
        int count = 0;
        while (cached.hasNext()) {
            assertTrue(cached.nextBoolean());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        BooleanIterator iter = BooleanIterator.of(true, false);
        assertEquals(Boolean.TRUE, iter.next());
        Boolean boxed = iter.next();
        assertInstanceOf(Boolean.class, boxed);
        assertFalse(boxed);
        assertThrows(NoSuchElementException.class, iter::nextBoolean);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new boolean[] { true, false, true }, BooleanIterator.of(true, false, true, false, true).skip(2).toArray());
        assertFalse(BooleanIterator.of(true, false, true).skip(10).hasNext());
        BooleanIterator original = BooleanIterator.of(true, false);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.of(true).skip(-1));
        assertThrows(NoSuchElementException.class, () -> BooleanIterator.of(true, false).skip(2).nextBoolean());
        assertArrayEquals(new boolean[] { false, true, false }, BooleanIterator.of(true, false, true, false, true).skip(1).limit(3).toArray());

        BooleanIterator source = new BooleanIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public boolean nextBoolean() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return next++ % 2 == 0;
            }
        };
        BooleanIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertTrue(skipped.nextBoolean());
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new boolean[] { true, false, true }, BooleanIterator.of(true, false, true, false, true).limit(3).toArray());
        assertArrayEquals(new boolean[] { true, false }, BooleanIterator.of(true, false).limit(10).toArray());
        assertFalse(BooleanIterator.of(true, false).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.of(true).limit(-1));

        int[] attempts = { 0 };
        BooleanIterator quota = BooleanIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return true;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextBoolean);
        assertTrue(quota.hasNext());
        assertTrue(quota.nextBoolean());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new boolean[] { true, true, true }, BooleanIterator.of(true, false, true, false, true).filter(b -> b).toArray());
        assertArrayEquals(new boolean[] { false, false, false }, BooleanIterator.of(true, false, true, false, false).filter(b -> !b).toArray());
        BooleanIterator none = BooleanIterator.of(true, true, true).filter(b -> !b);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextBoolean);
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.of(true).filter(null));
        assertEquals(3, BooleanIterator.of(true, false, true, false, true, false, true, false).skip(1).limit(6).filter(b -> !b).stream().count());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new boolean[] { true, false, true }, BooleanIterator.of(true, false, true).toArray());
        BooleanIterator partial = BooleanIterator.of(true, false, true, false);
        partial.nextBoolean();
        partial.nextBoolean();
        assertArrayEquals(new boolean[] { true, false }, partial.toArray());
        assertEquals(BooleanList.of(true, false, true), BooleanIterator.of(true, false, true).toList());
        assertTrue(BooleanIterator.empty().toList().isEmpty());
        BooleanIterator consumed = BooleanIterator.of(true, false);
        consumed.toArray();
        assertFalse(consumed.hasNext());
    }

    @Test
    public void testStream() {
        Stream<Boolean> stream = BooleanIterator.of(true, false, true, false).stream();
        assertNotNull(stream);
        assertEquals(2, stream.filter(b -> b).count());
        assertEquals(0, BooleanIterator.empty().stream().count());
        AtomicInteger count = new AtomicInteger();
        assertEquals(3, BooleanIterator.generate(() -> count.get() < 5, () -> count.getAndIncrement() % 2 == 0).stream().filter(b -> b).count());
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedBoolean> indexed = BooleanIterator.of(true, false, true).indexed();
        assertEquals(0, indexed.next().index());
        assertFalse(indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, BooleanIterator.of(true).indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.of(true).indexed(-1));

        BooleanIterator source = BooleanIterator.of(true, false);
        ObjIterator<IndexedBoolean> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertFalse(source.nextBoolean());

        ObjIterator<IndexedBoolean> max = BooleanIterator.of(true).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        BooleanList boxed = new BooleanList();
        BooleanIterator.of(true, false, true).forEachRemaining((Boolean b) -> boxed.add(b));
        assertEquals(BooleanList.of(true, false, true), boxed);

        BooleanList values = new BooleanList();
        BooleanIterator.of(true, false, true, false).foreachRemaining(values::add);
        assertEquals(BooleanList.of(true, false, true, false), values);

        BooleanIterator partial = BooleanIterator.of(true, false, true, false);
        partial.nextBoolean();
        partial.nextBoolean();
        BooleanList remaining = new BooleanList();
        partial.foreachRemaining(remaining::add);
        assertEquals(BooleanList.of(true, false), remaining);
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.of(true).foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> BooleanIterator.of(true).forEachRemaining((java.util.function.Consumer<Boolean>) null));
        assertThrows(NullPointerException.class, () -> BooleanIterator.empty().forEachRemaining((java.util.function.Consumer<Boolean>) null));
    }

    @Test
    public void testForeachIndexed() {
        List<Integer> indices = new ArrayList<>();
        BooleanList values = new BooleanList();
        BooleanIterator.of(true, false, true).foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });
        assertEquals(List.of(0, 1, 2), indices);
        assertEquals(BooleanList.of(true, false, true), values);

        BooleanIterator partial = BooleanIterator.of(true, false, true, false);
        partial.nextBoolean();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> BooleanIterator.of(true).foreachIndexed(null));
    }
}
