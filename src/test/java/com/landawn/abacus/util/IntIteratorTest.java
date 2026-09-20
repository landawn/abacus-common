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
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.IntStream;

public class IntIteratorTest extends TestBase {

    @Test
    public void testDeferRejectsReturningItself() {
        for (int mode = 0; mode < 2; mode++) {
            final int[] supplierCalls = { 0 };
            final java.util.concurrent.atomic.AtomicReference<IntIterator> reference = new java.util.concurrent.atomic.AtomicReference<>();
            final IntIterator iterator = IntIterator.defer(() -> {
                supplierCalls[0]++;
                return reference.get();
            });
            reference.set(iterator);

            final IllegalStateException failure = mode == 0 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextInt);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextInt));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testDeferCachesRecursiveInitializationFailure() {
        for (int mode = 0; mode < 4; mode++) {
            final boolean catchRecursion = (mode & 1) != 0;
            final int[] supplierCalls = { 0 };
            final IntIterator[] reference = new IntIterator[1];
            final IllegalStateException[] recursiveFailure = new IllegalStateException[1];
            reference[0] = IntIterator.defer(() -> {
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

                return IntIterator.empty();
            });

            final IntIterator iterator = reference[0];
            final IllegalStateException failure = mode < 2 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextInt);
            assertSame(recursiveFailure[0], failure);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextInt));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final IntIterator iter = IntIterator.of(1, 2, 3);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new int[] { 2, 3 }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        IntIterator iter = IntIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);
        assertSame(IntIterator.EMPTY, IntIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertEquals(0, iter.stream().toArray().length);
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);

        assertFalse(IntIterator.of().hasNext());
        assertFalse(IntIterator.of((int[]) null).hasNext());
        assertEquals(42, IntIterator.of(42).nextInt());
        IntIterator special = IntIterator.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, special.nextInt());
        assertEquals(-1, special.nextInt());
        assertEquals(0, special.nextInt());
        assertEquals(1, special.nextInt());
        assertEquals(Integer.MAX_VALUE, special.nextInt());
    }

    @Test
    public void testOf_Range() {
        int[] array = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new int[] { 2, 3, 4 }, IntIterator.of(array, 1, 4).toArray());
        assertEquals(IntList.of(2, 3, 4), IntIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, IntIterator.of(array, 0, array.length).toArray());
        assertFalse(IntIterator.of(array, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of((int[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        IntIterator iter = IntIterator.defer(() -> {
            calls.incrementAndGet();
            return IntIterator.of(1, 2, 3);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(1, calls.get());

        boolean[] calledOnNext = { false };
        IntIterator onNext = IntIterator.defer(() -> {
            calledOnNext[0] = true;
            return IntIterator.of(42);
        });
        assertEquals(42, onNext.nextInt());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        IntIterator failing = IntIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> IntIterator.defer(null));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testRemoveIsUnsupported() {
        // The contract lives on the package-private supertype ImmutableIterator, so it is pinned here.
        final IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(UnsupportedOperationException.class, iter::remove);
        assertEquals(1, iter.nextInt());
        assertThrows(UnsupportedOperationException.class, iter::remove);
        assertThrows(UnsupportedOperationException.class, IntIterator.empty()::remove);
        assertThrows(UnsupportedOperationException.class, IntIterator.generate(() -> 1)::remove);
        assertThrows(UnsupportedOperationException.class, IntIterator.defer(() -> IntIterator.of(1))::remove);
        assertThrows(UnsupportedOperationException.class, IntIterator.of(1, 2, 3).skip(1)::remove);

        // ...but the class is extensible, so a subclass may override remove(): the class javadoc therefore
        // scopes the guarantee to the iterators the factory methods return, as ObjIterator's does.
        final AtomicInteger removed = new AtomicInteger();
        final IntIterator overriding = new IntIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                return 1;
            }

            @Override
            public void remove() {
                removed.incrementAndGet();
            }
        };

        overriding.remove();
        assertEquals(1, removed.get(), "an IntIterator subclass can override remove()");
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        IntIterator infinite = IntIterator.generate(n::getAndIncrement);
        assertEquals(0, infinite.nextInt());
        assertEquals(1, infinite.nextInt());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        IntIterator finite = IntIterator.generate(() -> counter.get() < 3, counter::getAndIncrement);
        assertEquals(0, finite.nextInt());
        assertEquals(1, finite.nextInt());
        assertEquals(2, finite.nextInt());
        assertFalse(finite.hasNext());
        assertThrows(NoSuchElementException.class, finite::nextInt);
        assertFalse(IntIterator.generate(() -> false, () -> 1).hasNext());

        IntSupplier supplier = () -> 0;
        BooleanSupplier hasNext = () -> true;
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate((IntSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate(hasNext, null));

        AtomicInteger budget = new AtomicInteger(3);
        IntIterator cached = IntIterator.generate(() -> budget.getAndDecrement() > 0, () -> 42);
        assertTrue(cached.hasNext());
        int count = 0;
        while (cached.hasNext()) {
            assertEquals(42, cached.nextInt());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        assertEquals(Integer.valueOf(10), iter.next());
        assertEquals(20, iter.nextInt());
        assertEquals(30, iter.nextInt());
        assertThrows(NoSuchElementException.class, iter::nextInt);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new int[] { 3, 4, 5 }, IntIterator.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertFalse(IntIterator.of(1, 2, 3).skip(5).hasNext());
        IntIterator original = IntIterator.of(1, 2, 3);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> IntIterator.of(1, 2, 3).skip(-1));
        assertThrows(NoSuchElementException.class, () -> IntIterator.of(1, 2).skip(2).nextInt());

        IntIterator source = new IntIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public int nextInt() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return next++;
            }
        };
        IntIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(2, skipped.nextInt());
    }

    @Test
    public void testLimit() {
        IntIterator limited = IntIterator.of(1, 2, 3, 4, 5).limit(3);
        assertEquals(1, limited.nextInt());
        assertEquals(2, limited.nextInt());
        assertEquals(3, limited.nextInt());
        assertFalse(limited.hasNext());
        assertThrows(NoSuchElementException.class, limited::nextInt);
        assertArrayEquals(new int[] { 1, 2, 3 }, IntIterator.of(1, 2, 3).limit(5).toArray());
        assertFalse(IntIterator.of(1, 2, 3).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> IntIterator.of(1, 2, 3).limit(-1));

        int[] attempts = { 0 };
        IntIterator quota = IntIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return 7;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextInt);
        assertTrue(quota.hasNext());
        assertEquals(7, quota.nextInt());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new int[] { 2, 4, 6 }, IntIterator.of(1, 2, 3, 4, 5, 6).filter(x -> x % 2 == 0).toArray());
        assertArrayEquals(new int[] { 2, 4, 6 }, IntIterator.of(2, 4, 6).filter(x -> x % 2 == 0).toArray());
        IntIterator none = IntIterator.of(1, 3, 5).filter(x -> x % 2 == 0);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextInt);
        assertThrows(IllegalArgumentException.class, () -> IntIterator.of(1, 2, 3).filter(null));
        assertEquals(18, IntIterator.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).skip(2).limit(6).filter(x -> x % 2 == 0).stream().sum());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new int[] { 1, 2, 3 }, IntIterator.of(1, 2, 3).toArray());
        assertEquals(0, IntIterator.empty().toArray().length);
        IntIterator partial = IntIterator.of(1, 2, 3, 4, 5);
        partial.nextInt();
        partial.nextInt();
        assertArrayEquals(new int[] { 3, 4, 5 }, partial.toArray());

        IntList list = IntIterator.of(1, 2, 3).toList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(2));
        assertTrue(IntIterator.empty().toList().isEmpty());
        IntIterator consumed = IntIterator.of(1, 2, 3);
        consumed.toArray();
        assertFalse(consumed.hasNext());
    }

    @Test
    public void testStream() {
        IntStream stream = IntIterator.of(1, 2, 3).stream();
        assertNotNull(stream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
        assertEquals(15, IntIterator.of(1, 2, 3, 4, 5).stream().sum());
        assertEquals(2, IntIterator.of(1, 2, 3).stream().filter(x -> x > 1).count());
        assertEquals(0, IntIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedInt> indexed = IntIterator.of(10, 20, 30).indexed();
        assertEquals(0, indexed.next().index());
        assertEquals(20, indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertFalse(IntIterator.empty().indexed().hasNext());

        ObjIterator<IndexedInt> offset = IntIterator.of(10, 20).indexed(100);
        assertEquals(100, offset.next().index());
        assertEquals(101, offset.next().index());
        assertEquals(0, IntIterator.of(10).indexed(0).next().index());
        assertThrows(IllegalArgumentException.class, () -> IntIterator.of(1, 2, 3).indexed(-1));

        IntIterator source = IntIterator.of(1, 2);
        ObjIterator<IndexedInt> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals(2, source.nextInt());

        ObjIterator<IndexedInt> max = IntIterator.of(1).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        IntList boxed = new IntList();
        IntIterator.of(1, 2, 3).forEachRemaining((Integer i) -> boxed.add(i));
        assertEquals(IntList.of(1, 2, 3), boxed);

        IntList values = new IntList();
        IntIterator.of(1, 2, 3, 4, 5).foreachRemaining(values::add);
        assertEquals(IntList.of(1, 2, 3, 4, 5), values);

        IntIterator partial = IntIterator.of(1, 2, 3, 4, 5);
        partial.nextInt();
        partial.nextInt();
        AtomicInteger sum = new AtomicInteger();
        partial.foreachRemaining(sum::addAndGet);
        assertEquals(12, sum.get());

        AtomicInteger count = new AtomicInteger();
        IntIterator.empty().foreachRemaining(i -> count.incrementAndGet());
        IntIterator.empty().forEachRemaining((Integer i) -> count.incrementAndGet());
        assertEquals(0, count.get());
        assertThrows(IllegalArgumentException.class, () -> IntIterator.of(1, 2, 3).foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> IntIterator.of(1, 2, 3).forEachRemaining((java.util.function.Consumer<Integer>) null));
        assertThrows(NullPointerException.class, () -> IntIterator.empty().forEachRemaining((java.util.function.Consumer<Integer>) null));
    }

    @Test
    public void testForeachIndexed() {
        IntList indices = new IntList();
        IntList values = new IntList();
        IntIterator.of(10, 20, 30).foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });
        assertEquals(IntList.of(0, 1, 2), indices);
        assertEquals(IntList.of(10, 20, 30), values);

        IntIterator partial = IntIterator.of(10, 20, 30);
        partial.nextInt();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);

        AtomicInteger count = new AtomicInteger();
        IntIterator.empty().foreachIndexed((i, v) -> count.incrementAndGet());
        assertEquals(0, count.get());
        assertThrows(IllegalArgumentException.class, () -> IntIterator.of(1, 2, 3).foreachIndexed(null));
    }
}
