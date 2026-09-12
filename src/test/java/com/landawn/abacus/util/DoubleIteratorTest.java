package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.DoubleStream;

public class DoubleIteratorTest extends TestBase {

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new double[] { 2.0, 3.0 }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        DoubleIterator iter = DoubleIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextDouble);
        assertSame(DoubleIterator.EMPTY, DoubleIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1.0, iter.nextDouble());
        assertEquals(2.0, iter.nextDouble());
        assertEquals(3.0, iter.nextDouble());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextDouble);

        assertFalse(DoubleIterator.of().hasNext());
        assertFalse(DoubleIterator.of((double[]) null).hasNext());
        assertEquals(42.5, DoubleIterator.of(42.5).nextDouble());
        DoubleIterator special = DoubleIterator.of(Double.NEGATIVE_INFINITY, -0.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.MIN_VALUE,
                Double.MAX_VALUE);
        assertEquals(Double.NEGATIVE_INFINITY, special.nextDouble());
        assertEquals(-0.0, special.nextDouble());
        assertEquals(0.0, special.nextDouble());
        assertTrue(Double.isNaN(special.nextDouble()));
        assertEquals(Double.POSITIVE_INFINITY, special.nextDouble());
        assertEquals(Double.MIN_VALUE, special.nextDouble());
        assertEquals(Double.MAX_VALUE, special.nextDouble());
    }

    @Test
    public void testOf_Range() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, DoubleIterator.of(array, 1, 4).toArray());
        assertEquals(DoubleList.of(2.0, 3.0, 4.0), DoubleIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, DoubleIterator.of(array, 0, array.length).toArray());
        assertFalse(DoubleIterator.of(array, 1, 1).hasNext());
        assertFalse(DoubleIterator.of(null, 0, 0).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of((double[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        DoubleIterator iter = DoubleIterator.defer(() -> {
            calls.incrementAndGet();
            return DoubleIterator.of(1.0, 2.0, 3.0);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals(1.0, iter.nextDouble());
        assertEquals(2.0, iter.nextDouble());
        assertEquals(3.0, iter.nextDouble());
        assertEquals(1, calls.get());

        boolean[] calledOnNext = { false };
        DoubleIterator onNext = DoubleIterator.defer(() -> {
            calledOnNext[0] = true;
            return DoubleIterator.of(42.0);
        });
        assertEquals(42.0, onNext.nextDouble());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        DoubleIterator failing = DoubleIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        DoubleIterator infinite = DoubleIterator.generate(n::getAndIncrement);
        assertEquals(0.0, infinite.nextDouble());
        assertEquals(1.0, infinite.nextDouble());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        DoubleIterator finite = DoubleIterator.generate(() -> counter.get() < 3, () -> counter.getAndIncrement());
        assertArrayEquals(new double[] { 0.0, 1.0, 2.0 }, finite.toArray());
        assertThrows(NoSuchElementException.class, finite::nextDouble);
        assertFalse(DoubleIterator.generate(() -> false, () -> 1.0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate((DoubleSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null, () -> 0.0));
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate((BooleanSupplier) () -> true, null));

        AtomicInteger budget = new AtomicInteger(3);
        DoubleIterator cached = DoubleIterator.generate(() -> budget.getAndDecrement() > 0, () -> 42.0);
        assertTrue(cached.hasNext());
        int count = 0;
        while (cached.hasNext()) {
            assertEquals(42.0, cached.nextDouble());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        DoubleIterator iter = DoubleIterator.of(10.0, 20.0, 30.0);
        assertEquals(Double.valueOf(10.0), iter.next());
        assertEquals(20.0, iter.nextDouble());
        assertEquals(30.0, iter.nextDouble());
        assertThrows(NoSuchElementException.class, iter::nextDouble);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).skip(2).toArray());
        assertFalse(DoubleIterator.of(1.0, 2.0, 3.0).skip(5).hasNext());
        DoubleIterator original = DoubleIterator.of(1.0, 2.0);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).skip(-1));
        assertThrows(NoSuchElementException.class, () -> DoubleIterator.of(1.0, 2.0).skip(2).nextDouble());

        DoubleIterator source = new DoubleIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public double nextDouble() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return next++;
            }
        };
        DoubleIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(2.0, skipped.nextDouble());
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).limit(3).toArray());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleIterator.of(1.0, 2.0, 3.0).limit(5).toArray());
        assertFalse(DoubleIterator.of(1.0).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).limit(-1));

        int[] attempts = { 0 };
        DoubleIterator quota = DoubleIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return 7.0;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextDouble);
        assertTrue(quota.hasNext());
        assertEquals(7.0, quota.nextDouble());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).filter(x -> x % 2 == 0).toArray());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleIterator.of(1.0, Double.NaN, 2.0, Double.NaN, 3.0).filter(d -> !Double.isNaN(d)).toArray());
        DoubleIterator none = DoubleIterator.of(1.0, 3.0, 5.0).filter(x -> x % 2 == 0);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextDouble);
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).filter(null));
        assertEquals(18.0, DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0).skip(2).limit(6).filter(x -> x % 2 == 0).stream().sum());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleIterator.of(1.0, 2.0, 3.0).toArray());
        DoubleIterator partial = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        partial.nextDouble();
        partial.nextDouble();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, partial.toArray());
        assertEquals(DoubleList.of(1.0, 2.0, 3.0), DoubleIterator.of(1.0, 2.0, 3.0).toList());
        assertTrue(DoubleIterator.empty().toList().isEmpty());
        DoubleIterator consumed = DoubleIterator.of(1.0, 2.0, 3.0);
        consumed.toArray();
        assertFalse(consumed.hasNext());
    }

    @Test
    public void testStream() {
        DoubleStream stream = DoubleIterator.of(1.0, 2.0, 3.0).stream();
        assertNotNull(stream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray());
        assertEquals(15.0, DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).stream().sum());
        assertEquals(0, DoubleIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedDouble> indexed = DoubleIterator.of(10.0, 20.0, 30.0).indexed();
        assertEquals(0, indexed.next().index());
        assertEquals(20.0, indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, DoubleIterator.of(10.0).indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).indexed(-1));

        DoubleIterator source = DoubleIterator.of(1.0, 2.0);
        ObjIterator<IndexedDouble> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals(2.0, source.nextDouble());

        ObjIterator<IndexedDouble> max = DoubleIterator.of(1.0).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        DoubleList boxed = new DoubleList();
        DoubleIterator.of(1.0, 2.0, 3.0).forEachRemaining((Double i) -> boxed.add(i));
        assertEquals(DoubleList.of(1.0, 2.0, 3.0), boxed);

        DoubleList values = new DoubleList();
        DoubleIterator.of(1.0, 2.0, 3.0).foreachRemaining(values::add);
        assertEquals(DoubleList.of(1.0, 2.0, 3.0), values);

        DoubleIterator partial = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        partial.nextDouble();
        partial.nextDouble();
        DoubleList remaining = new DoubleList();
        partial.foreachRemaining(remaining::add);
        assertEquals(DoubleList.of(3.0, 4.0, 5.0), remaining);
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).foreachRemaining((Throwables.DoubleConsumer<Exception>) null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> DoubleIterator.of(1.0).forEachRemaining((java.util.function.Consumer<Double>) null));
        assertThrows(NullPointerException.class, () -> DoubleIterator.empty().forEachRemaining((java.util.function.Consumer<Double>) null));
    }

    @Test
    public void testForeachIndexed() {
        DoubleList indices = new DoubleList();
        DoubleList values = new DoubleList();
        DoubleIterator.of(10.0, 20.0, 30.0).foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });
        assertEquals(DoubleList.of(0.0, 1.0, 2.0), indices);
        assertEquals(DoubleList.of(10.0, 20.0, 30.0), values);

        DoubleIterator partial = DoubleIterator.of(10.0, 20.0, 30.0);
        partial.nextDouble();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).foreachIndexed(null));
    }
}
