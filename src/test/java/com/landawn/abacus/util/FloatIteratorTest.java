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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.FloatSupplier;
import com.landawn.abacus.util.stream.FloatStream;

public class FloatIteratorTest extends TestBase {

    @Test
    public void testDeferRejectsReturningItself() {
        for (int mode = 0; mode < 2; mode++) {
            final int[] supplierCalls = { 0 };
            final java.util.concurrent.atomic.AtomicReference<FloatIterator> reference = new java.util.concurrent.atomic.AtomicReference<>();
            final FloatIterator iterator = FloatIterator.defer(() -> {
                supplierCalls[0]++;
                return reference.get();
            });
            reference.set(iterator);

            final IllegalStateException failure = mode == 0 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextFloat);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextFloat));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testDeferCachesRecursiveInitializationFailure() {
        for (int mode = 0; mode < 4; mode++) {
            final boolean catchRecursion = (mode & 1) != 0;
            final int[] supplierCalls = { 0 };
            final FloatIterator[] reference = new FloatIterator[1];
            final IllegalStateException[] recursiveFailure = new IllegalStateException[1];
            reference[0] = FloatIterator.defer(() -> {
                if (++supplierCalls[0] > 1) {
                    throw new AssertionError("Supplier must not be reentered");
                }

                try {
                    reference[0].hasNext();
                } catch (final IllegalStateException failure) {
                    recursiveFailure[0] = failure;
                    if (!catchRecursion) {
                        throw failure;
                    }
                }

                return FloatIterator.empty();
            });

            final FloatIterator iterator = reference[0];
            final IllegalStateException failure = mode < 2 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextFloat);
            assertSame(recursiveFailure[0], failure);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextFloat));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final FloatIterator iter = FloatIterator.of(1f, 2f, 3f);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new float[] { 2f, 3f }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        FloatIterator iter = FloatIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertSame(FloatIterator.EMPTY, FloatIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        FloatIterator iter = FloatIterator.of(1f, 2f, 3f);
        assertTrue(iter.hasNext());
        assertEquals(1f, iter.nextFloat());
        assertEquals(2f, iter.nextFloat());
        assertEquals(3f, iter.nextFloat());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertFalse(FloatIterator.of().hasNext());
        assertFalse(FloatIterator.of((float[]) null).hasNext());
        assertEquals(42f, FloatIterator.of(42f).nextFloat());
        FloatIterator special = FloatIterator.of(Float.NEGATIVE_INFINITY, -0f, 0f, Float.NaN, Float.POSITIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, special.nextFloat());
        assertEquals(-0f, special.nextFloat());
        assertEquals(0f, special.nextFloat());
        assertTrue(Float.isNaN(special.nextFloat()));
        assertEquals(Float.POSITIVE_INFINITY, special.nextFloat());
    }

    @Test
    public void testOf_Range() {
        float[] array = { 1f, 2f, 3f, 4f, 5f };
        assertArrayEquals(new float[] { 2f, 3f, 4f }, FloatIterator.of(array, 1, 4).toArray());
        assertEquals(FloatList.of(2f, 3f, 4f), FloatIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, FloatIterator.of(array, 0, array.length).toArray());
        assertFalse(FloatIterator.of(array, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of((float[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        FloatIterator iter = FloatIterator.defer(() -> {
            calls.incrementAndGet();
            return FloatIterator.of(1f, 2f, 3f);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals(1f, iter.nextFloat());
        assertEquals(2f, iter.nextFloat());
        assertEquals(3f, iter.nextFloat());

        boolean[] calledOnNext = { false };
        FloatIterator onNext = FloatIterator.defer(() -> {
            calledOnNext[0] = true;
            return FloatIterator.of(42f);
        });
        assertEquals(42f, onNext.nextFloat());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        FloatIterator failing = FloatIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        FloatIterator infinite = FloatIterator.generate(() -> n.getAndIncrement());
        assertEquals(0f, infinite.nextFloat());
        assertEquals(1f, infinite.nextFloat());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        FloatIterator finite = FloatIterator.generate(() -> counter.get() < 3, () -> counter.getAndIncrement());
        assertArrayEquals(new float[] { 0f, 1f, 2f }, finite.toArray());
        assertThrows(NoSuchElementException.class, finite::nextFloat);
        assertFalse(FloatIterator.generate(() -> false, () -> 1f).hasNext());
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate((FloatSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate(null, () -> 0f));
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate((BooleanSupplier) () -> true, null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        FloatIterator iter = FloatIterator.of(10f, 20f, 30f);
        assertEquals(Float.valueOf(10f), iter.next());
        assertEquals(20f, iter.nextFloat());
        assertEquals(30f, iter.nextFloat());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new float[] { 3f, 4f, 5f }, FloatIterator.of(1f, 2f, 3f, 4f, 5f).skip(2).toArray());
        assertFalse(FloatIterator.of(1f, 2f, 3f).skip(5).hasNext());
        FloatIterator original = FloatIterator.of(1f, 2f);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1f).skip(-1));
        assertThrows(NoSuchElementException.class, () -> FloatIterator.of(1f, 2f).skip(2).nextFloat());

        FloatIterator source = new FloatIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public float nextFloat() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return next++;
            }
        };
        FloatIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(2f, skipped.nextFloat());
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new float[] { 1f, 2f, 3f }, FloatIterator.of(1f, 2f, 3f, 4f, 5f).limit(3).toArray());
        assertArrayEquals(new float[] { 1f, 2f, 3f }, FloatIterator.of(1f, 2f, 3f).limit(5).toArray());
        assertFalse(FloatIterator.of(1f).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1f).limit(-1));

        int[] attempts = { 0 };
        FloatIterator quota = FloatIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return 7f;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextFloat);
        assertTrue(quota.hasNext());
        assertEquals(7f, quota.nextFloat());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new float[] { 2f, 4f, 6f }, FloatIterator.of(1f, 2f, 3f, 4f, 5f, 6f).filter(x -> x % 2 == 0).toArray());
        assertArrayEquals(new float[] { 1f, 2f, 3f }, FloatIterator.of(1f, Float.NaN, 2f, Float.NaN, 3f).filter(f -> !Float.isNaN(f)).toArray());
        FloatIterator none = FloatIterator.of(1f, 3f, 5f).filter(x -> x % 2 == 0);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextFloat);
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1f).filter(null));
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new float[] { 1f, 2f, 3f }, FloatIterator.of(1f, 2f, 3f).toArray());
        FloatIterator partial = FloatIterator.of(1f, 2f, 3f, 4f, 5f);
        partial.nextFloat();
        partial.nextFloat();
        assertArrayEquals(new float[] { 3f, 4f, 5f }, partial.toArray());
        assertEquals(FloatList.of(1f, 2f, 3f), FloatIterator.of(1f, 2f, 3f).toList());
        assertTrue(FloatIterator.empty().toList().isEmpty());
    }

    @Test
    public void testStream() {
        FloatStream stream = FloatIterator.of(1f, 2f, 3f).stream();
        assertNotNull(stream);
        assertArrayEquals(new float[] { 1f, 2f, 3f }, stream.toArray());
        assertEquals(0, FloatIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedFloat> indexed = FloatIterator.of(10f, 20f, 30f).indexed();
        assertEquals(0, indexed.next().index());
        assertEquals(20f, indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, FloatIterator.of(10f).indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1f).indexed(-1));

        FloatIterator source = FloatIterator.of(1f, 2f);
        ObjIterator<IndexedFloat> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals(2f, source.nextFloat());

        ObjIterator<IndexedFloat> max = FloatIterator.of(1f).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        FloatList boxed = new FloatList();
        FloatIterator.of(1f, 2f, 3f).forEachRemaining((Float i) -> boxed.add(i));
        assertEquals(FloatList.of(1f, 2f, 3f), boxed);

        FloatList values = new FloatList();
        FloatIterator.of(1f, 2f, 3f).foreachRemaining(values::add);
        assertEquals(FloatList.of(1f, 2f, 3f), values);

        FloatIterator partial = FloatIterator.of(1f, 2f, 3f, 4f, 5f);
        partial.nextFloat();
        partial.nextFloat();
        FloatList remaining = new FloatList();
        partial.foreachRemaining(remaining::add);
        assertEquals(FloatList.of(3f, 4f, 5f), remaining);
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1f).foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> FloatIterator.of(1f).forEachRemaining((java.util.function.Consumer<Float>) null));
        assertThrows(NullPointerException.class, () -> FloatIterator.empty().forEachRemaining((java.util.function.Consumer<Float>) null));
    }

    @Test
    public void testForeachIndexed() {
        FloatList indices = new FloatList();
        FloatList values = new FloatList();
        FloatIterator.of(10f, 20f, 30f).foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });
        assertEquals(FloatList.of(0f, 1f, 2f), indices);
        assertEquals(FloatList.of(10f, 20f, 30f), values);

        FloatIterator partial = FloatIterator.of(10f, 20f, 30f);
        partial.nextFloat();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1f).foreachIndexed(null));
    }
}
