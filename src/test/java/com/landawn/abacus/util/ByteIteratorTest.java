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
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.stream.ByteStream;

public class ByteIteratorTest extends TestBase {

    @Test
    public void testDeferRejectsReturningItself() {
        for (int mode = 0; mode < 2; mode++) {
            final int[] supplierCalls = { 0 };
            final java.util.concurrent.atomic.AtomicReference<ByteIterator> reference = new java.util.concurrent.atomic.AtomicReference<>();
            final ByteIterator iterator = ByteIterator.defer(() -> {
                supplierCalls[0]++;
                return reference.get();
            });
            reference.set(iterator);

            final IllegalStateException failure = mode == 0 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextByte);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextByte));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testDeferCachesRecursiveInitializationFailure() {
        for (int mode = 0; mode < 4; mode++) {
            final boolean catchRecursion = (mode & 1) != 0;
            final int[] supplierCalls = { 0 };
            final ByteIterator[] reference = new ByteIterator[1];
            final IllegalStateException[] recursiveFailure = new IllegalStateException[1];
            reference[0] = ByteIterator.defer(() -> {
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

                return ByteIterator.empty();
            });

            final ByteIterator iterator = reference[0];
            final IllegalStateException failure = mode < 2 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextByte);
            assertSame(recursiveFailure[0], failure);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextByte));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new byte[] { 2, 3 }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        ByteIterator iter = ByteIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertSame(ByteIterator.EMPTY, ByteIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(iter.hasNext());
        assertEquals((byte) 1, iter.nextByte());
        assertEquals((byte) 2, iter.nextByte());
        assertEquals((byte) 3, iter.nextByte());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertFalse(ByteIterator.of().hasNext());
        assertFalse(ByteIterator.of((byte[]) null).hasNext());
        assertEquals((byte) 42, ByteIterator.of((byte) 42).nextByte());
        ByteIterator special = ByteIterator.of(Byte.MIN_VALUE, (byte) -1, (byte) 0, (byte) 1, Byte.MAX_VALUE);
        assertEquals(Byte.MIN_VALUE, special.nextByte());
        assertEquals((byte) -1, special.nextByte());
        assertEquals((byte) 0, special.nextByte());
        assertEquals((byte) 1, special.nextByte());
        assertEquals(Byte.MAX_VALUE, special.nextByte());
    }

    @Test
    public void testOf_Range() {
        byte[] array = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new byte[] { 2, 3, 4 }, ByteIterator.of(array, 1, 4).toArray());
        assertEquals(ByteList.of((byte) 2, (byte) 3, (byte) 4), ByteIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, ByteIterator.of(array, 0, array.length).toArray());
        assertFalse(ByteIterator.of(array, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of((byte[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        ByteIterator iter = ByteIterator.defer(() -> {
            calls.incrementAndGet();
            return ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals((byte) 1, iter.nextByte());
        assertEquals((byte) 2, iter.nextByte());
        assertEquals((byte) 3, iter.nextByte());

        boolean[] calledOnNext = { false };
        ByteIterator onNext = ByteIterator.defer(() -> {
            calledOnNext[0] = true;
            return ByteIterator.of((byte) 42);
        });
        assertEquals((byte) 42, onNext.nextByte());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        ByteIterator failing = ByteIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        ByteIterator infinite = ByteIterator.generate(() -> (byte) n.getAndIncrement());
        assertEquals((byte) 0, infinite.nextByte());
        assertEquals((byte) 1, infinite.nextByte());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        ByteIterator finite = ByteIterator.generate(() -> counter.get() < 3, () -> (byte) counter.getAndIncrement());
        assertArrayEquals(new byte[] { 0, 1, 2 }, finite.toArray());
        assertThrows(NoSuchElementException.class, finite::nextByte);
        assertFalse(ByteIterator.generate(() -> false, () -> (byte) 1).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate((ByteSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate(null, () -> (byte) 0));
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate((BooleanSupplier) () -> true, null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(Byte.valueOf((byte) 10), iter.next());
        assertEquals((byte) 20, iter.nextByte());
        assertEquals((byte) 30, iter.nextByte());
        assertThrows(NoSuchElementException.class, iter::nextByte);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2).toArray());
        assertFalse(ByteIterator.of((byte) 1, (byte) 2, (byte) 3).skip(5).hasNext());
        ByteIterator original = ByteIterator.of((byte) 1, (byte) 2);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.of((byte) 1).skip(-1));
        assertThrows(NoSuchElementException.class, () -> ByteIterator.of((byte) 1, (byte) 2).skip(2).nextByte());

        ByteIterator source = new ByteIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public byte nextByte() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return (byte) next++;
            }
        };
        ByteIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals((byte) 2, skipped.nextByte());
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).toArray());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteIterator.of((byte) 1, (byte) 2, (byte) 3).limit(5).toArray());
        assertFalse(ByteIterator.of((byte) 1).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.of((byte) 1).limit(-1));

        int[] attempts = { 0 };
        ByteIterator quota = ByteIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return (byte) 7;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextByte);
        assertTrue(quota.hasNext());
        assertEquals((byte) 7, quota.nextByte());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new byte[] { 2, 4, 6 },
                ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6).filter(x -> x % 2 == 0).toArray());
        ByteIterator none = ByteIterator.of((byte) 1, (byte) 3, (byte) 5).filter(x -> x % 2 == 0);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextByte);
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.of((byte) 1).filter(null));
        assertEquals(18,
                ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10)
                        .skip(2)
                        .limit(6)
                        .filter(x -> x % 2 == 0)
                        .stream()
                        .sum());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteIterator.of((byte) 1, (byte) 2, (byte) 3).toArray());
        ByteIterator partial = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        partial.nextByte();
        partial.nextByte();
        assertArrayEquals(new byte[] { 3, 4, 5 }, partial.toArray());
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), ByteIterator.of((byte) 1, (byte) 2, (byte) 3).toList());
        assertTrue(ByteIterator.empty().toList().isEmpty());
    }

    @Test
    public void testStream() {
        ByteStream stream = ByteIterator.of((byte) 1, (byte) 2, (byte) 3).stream();
        assertNotNull(stream);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());
        assertEquals(0, ByteIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedByte> indexed = ByteIterator.of((byte) 10, (byte) 20, (byte) 30).indexed();
        assertEquals(0, indexed.next().index());
        assertEquals((byte) 20, indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, ByteIterator.of((byte) 10).indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.of((byte) 1).indexed(-1));

        ByteIterator source = ByteIterator.of((byte) 1, (byte) 2);
        ObjIterator<IndexedByte> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals((byte) 2, source.nextByte());

        ObjIterator<IndexedByte> max = ByteIterator.of((byte) 1).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        ByteList boxed = new ByteList();
        ByteIterator.of((byte) 1, (byte) 2, (byte) 3).forEachRemaining((Byte i) -> boxed.add(i));
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), boxed);

        ByteList values = new ByteList();
        ByteIterator.of((byte) 1, (byte) 2, (byte) 3).foreachRemaining(values::add);
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), values);

        ByteIterator partial = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        partial.nextByte();
        partial.nextByte();
        AtomicInteger sum = new AtomicInteger();
        partial.foreachRemaining(v -> sum.addAndGet(v));
        assertEquals(12, sum.get());
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.of((byte) 1).foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> ByteIterator.of((byte) 1).forEachRemaining((java.util.function.Consumer<Byte>) null));
        assertThrows(NullPointerException.class, () -> ByteIterator.empty().forEachRemaining((java.util.function.Consumer<Byte>) null));
    }

    @Test
    public void testForeachIndexed() {
        ByteList indices = new ByteList();
        ByteList values = new ByteList();
        ByteIterator.of((byte) 10, (byte) 20, (byte) 30).foreachIndexed((index, value) -> {
            indices.add((byte) index);
            values.add(value);
        });
        assertEquals(ByteList.of((byte) 0, (byte) 1, (byte) 2), indices);
        assertEquals(ByteList.of((byte) 10, (byte) 20, (byte) 30), values);

        ByteIterator partial = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        partial.nextByte();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.of((byte) 1).foreachIndexed(null));
    }
}
