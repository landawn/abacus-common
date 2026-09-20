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
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.stream.CharStream;

public class CharIteratorTest extends TestBase {

    @Test
    public void testDeferRejectsReturningItself() {
        for (int mode = 0; mode < 2; mode++) {
            final int[] supplierCalls = { 0 };
            final java.util.concurrent.atomic.AtomicReference<CharIterator> reference = new java.util.concurrent.atomic.AtomicReference<>();
            final CharIterator iterator = CharIterator.defer(() -> {
                supplierCalls[0]++;
                return reference.get();
            });
            reference.set(iterator);

            final IllegalStateException failure = mode == 0 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextChar);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextChar));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testDeferCachesRecursiveInitializationFailure() {
        for (int mode = 0; mode < 4; mode++) {
            final boolean catchRecursion = (mode & 1) != 0;
            final int[] supplierCalls = { 0 };
            final CharIterator[] reference = new CharIterator[1];
            final IllegalStateException[] recursiveFailure = new IllegalStateException[1];
            reference[0] = CharIterator.defer(() -> {
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

                return CharIterator.empty();
            });

            final CharIterator iterator = reference[0];
            final IllegalStateException failure = mode < 2 ? assertThrows(IllegalStateException.class, iterator::hasNext)
                    : assertThrows(IllegalStateException.class, iterator::nextChar);
            assertSame(recursiveFailure[0], failure);
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::nextChar));
            assertEquals(1, supplierCalls[0]);
        }
    }

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final CharIterator iter = CharIterator.of('a', 'b', 'c');

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new char[] { 'b', 'c' }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        CharIterator iter = CharIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextChar);
        assertSame(CharIterator.EMPTY, CharIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextChar);
        assertFalse(CharIterator.of().hasNext());
        assertFalse(CharIterator.of((char[]) null).hasNext());
        assertEquals('z', CharIterator.of('z').nextChar());
        CharIterator special = CharIterator.of(Character.MIN_VALUE, '\n', '\t', '世', Character.MAX_VALUE);
        assertEquals(Character.MIN_VALUE, special.nextChar());
        assertEquals('\n', special.nextChar());
        assertEquals('\t', special.nextChar());
        assertEquals('世', special.nextChar());
        assertEquals(Character.MAX_VALUE, special.nextChar());
    }

    @Test
    public void testOf_Range() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, CharIterator.of(array, 1, 4).toArray());
        assertEquals(CharList.of('b', 'c', 'd'), CharIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, CharIterator.of(array, 0, array.length).toArray());
        assertFalse(CharIterator.of(array, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of((char[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        CharIterator iter = CharIterator.defer(() -> {
            calls.incrementAndGet();
            return CharIterator.of('a', 'b', 'c');
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());

        boolean[] calledOnNext = { false };
        CharIterator onNext = CharIterator.defer(() -> {
            calledOnNext[0] = true;
            return CharIterator.of('z');
        });
        assertEquals('z', onNext.nextChar());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        CharIterator failing = CharIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> CharIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger('a');
        CharIterator infinite = CharIterator.generate(() -> (char) n.getAndIncrement());
        assertEquals('a', infinite.nextChar());
        assertEquals('b', infinite.nextChar());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger('a');
        CharIterator finite = CharIterator.generate(() -> counter.get() < 'd', () -> (char) counter.getAndIncrement());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, finite.toArray());
        assertThrows(NoSuchElementException.class, finite::nextChar);
        assertFalse(CharIterator.generate(() -> false, () -> 'x').hasNext());
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate((CharSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate(null, () -> 'x'));
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate((BooleanSupplier) () -> true, null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertEquals(Character.valueOf('a'), iter.next());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertThrows(NoSuchElementException.class, iter::nextChar);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharIterator.of('a', 'b', 'c', 'd', 'e').skip(2).toArray());
        assertFalse(CharIterator.of('a', 'b', 'c').skip(5).hasNext());
        CharIterator original = CharIterator.of('a', 'b');
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').skip(-1));
        assertThrows(NoSuchElementException.class, () -> CharIterator.of('a', 'b').skip(2).nextChar());

        CharIterator source = new CharIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public char nextChar() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return (char) ('a' + next++);
            }
        };
        CharIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals('c', skipped.nextChar());
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharIterator.of('a', 'b', 'c', 'd', 'e').limit(3).toArray());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharIterator.of('a', 'b', 'c').limit(5).toArray());
        assertFalse(CharIterator.of('a').limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').limit(-1));

        int[] attempts = { 0 };
        CharIterator quota = CharIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return 'z';
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextChar);
        assertTrue(quota.hasNext());
        assertEquals('z', quota.nextChar());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new char[] { 'a', 'e' }, CharIterator.of('a', 'b', 'c', 'd', 'e').filter(c -> c == 'a' || c == 'e').toArray());
        CharIterator none = CharIterator.of('b', 'c').filter(c -> c == 'a');
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextChar);
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').filter(null));
        assertArrayEquals(new char[] { 'c', 'd' }, CharIterator.of('a', 'b', 'c', 'd', 'e').skip(2).limit(2).toArray());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharIterator.of('a', 'b', 'c').toArray());
        CharIterator partial = CharIterator.of('a', 'b', 'c', 'd', 'e');
        partial.nextChar();
        partial.nextChar();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, partial.toArray());
        assertEquals(CharList.of('a', 'b', 'c'), CharIterator.of('a', 'b', 'c').toList());
        assertTrue(CharIterator.empty().toList().isEmpty());
    }

    @Test
    public void testStream() {
        CharStream stream = CharIterator.of('a', 'b', 'c').stream();
        assertNotNull(stream);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
        assertEquals(2, CharIterator.of('a', 'b', 'c').stream().filter(c -> c > 'a').count());
        assertEquals(0, CharIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedChar> indexed = CharIterator.of('a', 'b', 'c').indexed();
        assertEquals(0, indexed.next().index());
        assertEquals('b', indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, CharIterator.of('a').indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').indexed(-1));

        CharIterator source = CharIterator.of('a', 'b');
        ObjIterator<IndexedChar> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals('b', source.nextChar());

        ObjIterator<IndexedChar> max = CharIterator.of('a').indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        CharList boxed = new CharList();
        CharIterator.of('a', 'b', 'c').forEachRemaining((Character c) -> boxed.add(c));
        assertEquals(CharList.of('a', 'b', 'c'), boxed);

        CharList values = new CharList();
        CharIterator.of('a', 'b', 'c').foreachRemaining(values::add);
        assertEquals(CharList.of('a', 'b', 'c'), values);

        CharIterator partial = CharIterator.of('a', 'b', 'c', 'd');
        partial.nextChar();
        partial.nextChar();
        CharList remaining = new CharList();
        partial.foreachRemaining(remaining::add);
        assertEquals(CharList.of('c', 'd'), remaining);
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> CharIterator.of('a').forEachRemaining((java.util.function.Consumer<Character>) null));
        assertThrows(NullPointerException.class, () -> CharIterator.empty().forEachRemaining((java.util.function.Consumer<Character>) null));
    }

    @Test
    public void testForeachIndexed() {
        CharList indices = new CharList();
        CharList values = new CharList();
        CharIterator.of('a', 'b', 'c').foreachIndexed((index, value) -> {
            indices.add((char) ('0' + index));
            values.add(value);
        });
        assertEquals(CharList.of('0', '1', '2'), indices);
        assertEquals(CharList.of('a', 'b', 'c'), values);

        CharIterator partial = CharIterator.of('a', 'b', 'c');
        partial.nextChar();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').foreachIndexed(null));
    }

    // Doc pin for the stream() javadoc de-duplicated on 2026-09-11. The removed sentence was a verbatim repeat of
    // the "Do not access this iterator independently" warning in the preceding paragraph; the sentence that was
    // KEPT makes a real, checkable claim - the returned stream starts out sequential.
    @Test
    public void reviewFixes20260911_streamIsInitiallySequential() {
        final CharStream stream = CharIterator.of('a', 'b', 'c').stream();
        assertFalse(stream.isParallel(), "stream() hands back a sequential stream");
        assertTrue(stream.parallel().isParallel(), "which the caller can convert with parallel(...)");

        assertFalse(CharIterator.empty().stream().isParallel());
    }

    // Doc pin for the class-javadoc paragraph added on 2026-09-11 (the sibling of the one IntIterator and
    // ObjIterator carry): every iterator the factory methods hand back throws UnsupportedOperationException
    // from remove(), but the class is extensible, so the guarantee is not implied by the declared type.
    @SuppressWarnings("deprecation")
    @Test
    public void reviewFixes20260911_removeIsUnsupportedButOverridable() {
        assertThrows(UnsupportedOperationException.class, CharIterator.of('a', 'b')::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.empty()::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.of(new char[] { 'a', 'b' }, 0, 2)::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.generate(() -> 'x')::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.defer(() -> CharIterator.of('a'))::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.of('a', 'b', 'c').skip(1)::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.of('a', 'b', 'c').limit(2)::remove);
        assertThrows(UnsupportedOperationException.class, CharIterator.of('a', 'b', 'c').filter(c -> true)::remove);

        final AtomicInteger removed = new AtomicInteger();
        final CharIterator overriding = new CharIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                return 'a';
            }

            @Override
            public void remove() {
                removed.incrementAndGet();
            }
        };

        overriding.remove();
        assertEquals(1, removed.get(), "a CharIterator subclass can override remove()");
    }

    // Doc pin for the defer() javadoc clarified on 2026-09-11: the supplier runs at most once, a runtime
    // failure is cached and rethrown as the SAME throwable, and the null-result IllegalStateException is
    // raised by the returned iterator's first access rather than by defer(..) itself.
    @Test
    public void reviewFixes20260911_deferCachesSupplierFailure() {
        final AtomicInteger calls = new AtomicInteger();
        final IllegalStateException boom = new IllegalStateException("boom");
        final CharIterator failing = CharIterator.defer(() -> {
            calls.incrementAndGet();
            throw boom;
        });

        assertEquals(0, calls.get(), "defer(..) must not invoke the supplier");
        assertSame(boom, assertThrows(IllegalStateException.class, failing::hasNext));
        assertSame(boom, assertThrows(IllegalStateException.class, failing::nextChar));
        assertSame(boom, assertThrows(IllegalStateException.class, failing::hasNext));
        assertEquals(1, calls.get(), "the supplier is invoked at most once, and the failure is cached");

        final AtomicInteger nullCalls = new AtomicInteger();
        final CharIterator nullResult = CharIterator.defer(() -> {
            nullCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, nullCalls.get(), "the IllegalStateException comes from the iterator, not from defer(..)");
        assertThrows(IllegalStateException.class, nullResult::hasNext);
        assertThrows(IllegalStateException.class, nullResult::hasNext);
        assertEquals(1, nullCalls.get());
    }
}
