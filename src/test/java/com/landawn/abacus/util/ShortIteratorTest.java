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
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.stream.ShortStream;

public class ShortIteratorTest extends TestBase {

    @Test
    public void testDeferRejectsReturningItself() {
        for (int mode = 0; mode < 2; mode++) {
            final int[] supplierCalls = { 0 };
            final java.util.concurrent.atomic.AtomicReference<ShortIterator> reference = new java.util.concurrent.atomic.AtomicReference<>();
            final ShortIterator iterator = ShortIterator.defer(() -> {
                supplierCalls[0]++;
                return reference.get();
            });
            reference.set(iterator);

            final IllegalStateException failure = mode == 0 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextShort);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextShort));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testDeferCachesRecursiveInitializationFailure() {
        for (int mode = 0; mode < 4; mode++) {
            final boolean catchRecursion = (mode & 1) != 0;
            final int[] supplierCalls = { 0 };
            final ShortIterator[] reference = new ShortIterator[1];
            final IllegalStateException[] recursiveFailure = new IllegalStateException[1];
            reference[0] = ShortIterator.defer(() -> {
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

                return ShortIterator.empty();
            });

            final ShortIterator iterator = reference[0];
            final IllegalStateException failure = mode < 2 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextShort);
            assertSame(recursiveFailure[0], failure);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextShort));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new short[] { 2, 3 }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        ShortIterator iter = ShortIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
        assertSame(ShortIterator.EMPTY, ShortIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertEquals(0, iter.stream().toArray().length);
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals((short) 1, iter.nextShort());
        assertEquals((short) 2, iter.nextShort());
        assertEquals((short) 3, iter.nextShort());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);

        assertFalse(ShortIterator.of().hasNext());
        assertFalse(ShortIterator.of((short[]) null).hasNext());
        assertEquals((short) 42, ShortIterator.of((short) 42).nextShort());
        ShortIterator special = ShortIterator.of(Short.MIN_VALUE, (short) -1, (short) 0, (short) 1, Short.MAX_VALUE);
        assertEquals(Short.MIN_VALUE, special.nextShort());
        assertEquals((short) -1, special.nextShort());
        assertEquals((short) 0, special.nextShort());
        assertEquals((short) 1, special.nextShort());
        assertEquals(Short.MAX_VALUE, special.nextShort());
    }

    @Test
    public void testOf_Range() {
        short[] array = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new short[] { 2, 3, 4 }, ShortIterator.of(array, 1, 4).toArray());
        assertEquals(ShortList.of((short) 2, (short) 3, (short) 4), ShortIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, ShortIterator.of(array, 0, array.length).toArray());
        assertFalse(ShortIterator.of(array, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of((short[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        ShortIterator iter = ShortIterator.defer(() -> {
            calls.incrementAndGet();
            return ShortIterator.of((short) 1, (short) 2, (short) 3);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals((short) 1, iter.nextShort());
        assertEquals((short) 3, iter.skip(0).toList().get(1));

        boolean[] calledOnNext = { false };
        ShortIterator onNext = ShortIterator.defer(() -> {
            calledOnNext[0] = true;
            return ShortIterator.of((short) 42);
        });
        assertEquals((short) 42, onNext.nextShort());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        ShortIterator failing = ShortIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        ShortIterator infinite = ShortIterator.generate(() -> (short) n.getAndIncrement());
        assertEquals((short) 0, infinite.nextShort());
        assertEquals((short) 1, infinite.nextShort());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        ShortIterator finite = ShortIterator.generate(() -> counter.get() < 3, () -> (short) counter.getAndIncrement());
        assertArrayEquals(new short[] { 0, 1, 2 }, finite.toArray());
        assertThrows(NoSuchElementException.class, finite::nextShort);
        assertFalse(ShortIterator.generate(() -> false, () -> (short) 1).hasNext());

        ShortSupplier supplier = () -> 0;
        BooleanSupplier hasNext = () -> true;
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate((ShortSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(hasNext, null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        assertEquals(Short.valueOf((short) 10), iter.next());
        assertEquals((short) 20, iter.nextShort());
        assertEquals((short) 30, iter.nextShort());
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toArray());
        assertFalse(ShortIterator.of((short) 1, (short) 2, (short) 3).skip(5).hasNext());
        ShortIterator original = ShortIterator.of((short) 1, (short) 2, (short) 3);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.of((short) 1).skip(-1));
        assertThrows(NoSuchElementException.class, () -> ShortIterator.of((short) 1, (short) 2).skip(2).nextShort());

        ShortIterator source = new ShortIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public short nextShort() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return (short) next++;
            }
        };
        ShortIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals((short) 2, skipped.nextShort());
    }

    @Test
    public void testLimit() {
        ShortIterator limited = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3);
        assertArrayEquals(new short[] { 1, 2, 3 }, limited.toArray());
        assertFalse(limited.hasNext());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortIterator.of((short) 1, (short) 2, (short) 3).limit(5).toArray());
        assertFalse(ShortIterator.of((short) 1).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.of((short) 1).limit(-1));

        int[] attempts = { 0 };
        ShortIterator quota = ShortIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return (short) 7;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextShort);
        assertTrue(quota.hasNext());
        assertEquals((short) 7, quota.nextShort());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new short[] { 2, 4, 6 },
                ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).filter(x -> x % 2 == 0).toArray());
        ShortIterator none = ShortIterator.of((short) 1, (short) 3, (short) 5).filter(x -> x % 2 == 0);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextShort);
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.of((short) 1).filter(null));
        assertEquals(18,
                ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9, (short) 10)
                        .skip(2)
                        .limit(6)
                        .filter(x -> x % 2 == 0)
                        .stream()
                        .sum());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortIterator.of((short) 1, (short) 2, (short) 3).toArray());
        ShortIterator partial = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        partial.nextShort();
        partial.nextShort();
        assertArrayEquals(new short[] { 3, 4, 5 }, partial.toArray());
        assertEquals(ShortList.of((short) 1, (short) 2, (short) 3), ShortIterator.of((short) 1, (short) 2, (short) 3).toList());
        assertTrue(ShortIterator.empty().toList().isEmpty());
    }

    @Test
    public void testStream() {
        ShortStream stream = ShortIterator.of((short) 1, (short) 2, (short) 3).stream();
        assertNotNull(stream);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
        assertEquals(0, ShortIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedShort> indexed = ShortIterator.of((short) 10, (short) 20, (short) 30).indexed();
        assertEquals(0, indexed.next().index());
        assertEquals((short) 20, indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, ShortIterator.of((short) 10).indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.of((short) 1).indexed(-1));

        ShortIterator source = ShortIterator.of((short) 1, (short) 2);
        ObjIterator<IndexedShort> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals((short) 2, source.nextShort());

        ObjIterator<IndexedShort> max = ShortIterator.of((short) 1).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        ShortList boxed = new ShortList();
        ShortIterator.of((short) 1, (short) 2, (short) 3).forEachRemaining((Short i) -> boxed.add(i));
        assertEquals(ShortList.of((short) 1, (short) 2, (short) 3), boxed);

        ShortList values = new ShortList();
        ShortIterator.of((short) 1, (short) 2, (short) 3).foreachRemaining(values::add);
        assertEquals(ShortList.of((short) 1, (short) 2, (short) 3), values);

        ShortIterator partial = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        partial.nextShort();
        partial.nextShort();
        AtomicInteger sum = new AtomicInteger();
        partial.foreachRemaining(v -> sum.addAndGet(v));
        assertEquals(12, sum.get());
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.of((short) 1).foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> ShortIterator.of((short) 1).forEachRemaining((java.util.function.Consumer<Short>) null));
        assertThrows(NullPointerException.class, () -> ShortIterator.empty().forEachRemaining((java.util.function.Consumer<Short>) null));
    }

    @Test
    public void testForeachIndexed() {
        ShortList indices = new ShortList();
        ShortList values = new ShortList();
        ShortIterator.of((short) 10, (short) 20, (short) 30).foreachIndexed((index, value) -> {
            indices.add((short) index);
            values.add(value);
        });
        assertEquals(ShortList.of((short) 0, (short) 1, (short) 2), indices);
        assertEquals(ShortList.of((short) 10, (short) 20, (short) 30), values);

        ShortIterator partial = ShortIterator.of((short) 10, (short) 20, (short) 30);
        partial.nextShort();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.of((short) 1).foreachIndexed(null));
    }
}
