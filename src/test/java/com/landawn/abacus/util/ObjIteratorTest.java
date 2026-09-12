package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjIteratorTest extends TestBase {

    @Test
    public void testEmpty() {
        ObjIterator<String> iter = ObjIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertSame(ObjIterator.empty(), ObjIterator.empty());
        assertTrue(iter.toList().isEmpty());
        assertEquals(0, iter.toArray().length);
        assertFalse(iter.skipNulls().hasNext());
        assertFalse(iter.firstNonNull().isPresent());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testJust() {
        ObjIterator<String> iter = ObjIterator.just("only");
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals("only", iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);

        ObjIterator<String> nil = ObjIterator.just(null);
        assertTrue(nil.hasNext());
        assertNull(nil.next());
        assertFalse(nil.hasNext());
    }

    @Test
    public void testOf() {
        assertEquals(List.of("a", "b", "c"), ObjIterator.of("a", "b", "c").toList());
        assertEquals(List.of("a"), ObjIterator.of("a").toList());
        assertFalse(ObjIterator.of().hasNext());
        assertFalse(ObjIterator.of((String[]) null).hasNext());
        assertEquals(Arrays.asList("a", null, "c"), ObjIterator.of("a", null, "c").toList());

        Integer[] array = { 1, 2, 3, 4, 5 };
        assertEquals(List.of(2, 3, 4), ObjIterator.of(array, 1, 4).toList());
        assertEquals(List.of(1, 2, 3, 4, 5), ObjIterator.of(array, 0, array.length).toList());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, ObjIterator.of(array, 1, 4).toArray(new Integer[0]));
        ObjIterator<Integer> partial = ObjIterator.of(array, 1, 4);
        partial.next();
        assertArrayEquals(new Integer[] { 3, 4 }, partial.toArray(new Integer[0]));
        assertFalse(ObjIterator.of(array, 1, 1).hasNext());
        assertFalse(ObjIterator.of((Integer[]) null, 0, 0).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, 2, 1));
        ObjIterator<Integer> exhausted = ObjIterator.of(array, 0, 1);
        exhausted.next();
        assertThrows(NoSuchElementException.class, exhausted::next);

        assertEquals(List.of("a", "b"), ObjIterator.of(List.of("a", "b").iterator()).toList());
        assertFalse(ObjIterator.of((Iterator<String>) null).hasNext());
        assertFalse(ObjIterator.of(List.<String> of().iterator()).hasNext());
        ObjIterator<String> original = ObjIterator.of("a", "b");
        ObjIterator<String> wrapped = ObjIterator.of(original);
        assertNotSame(original, wrapped);
        assertEquals(List.of("a", "b"), wrapped.toList());

        List<String> source = new ArrayList<>(List.of("a", "b"));
        Iterator<String> delegate = source.iterator();
        ObjIterator<String> mutable = new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public String next() {
                return delegate.next();
            }

            @Override
            public void remove() {
                delegate.remove();
            }
        };
        ObjIterator<String> blocked = ObjIterator.of(mutable);
        assertEquals("a", blocked.next());
        assertThrows(UnsupportedOperationException.class, blocked::remove);
        assertEquals(List.of("a", "b"), source);

        assertEquals(List.of(1, 2), ObjIterator.of(List.of(1, 2)).toList());
        assertFalse(ObjIterator.of((Collection<String>) null).hasNext());
        assertFalse(ObjIterator.of(new ArrayList<Integer>()).hasNext());
        assertEquals(List.of("x", "y"), ObjIterator.of((Iterable<String>) List.of("x", "y")).toList());
        assertFalse(ObjIterator.of((Iterable<String>) null).hasNext());
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        ObjIterator<String> iter = ObjIterator.defer(() -> {
            calls.incrementAndGet();
            return List.of("a", "b").iterator();
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals(1, calls.get());

        AtomicInteger nextCalls = new AtomicInteger();
        ObjIterator<Integer> onNext = ObjIterator.defer(() -> {
            nextCalls.incrementAndGet();
            return List.of(1).iterator();
        });
        assertEquals(1, onNext.next());
        assertEquals(1, nextCalls.get());

        assertFalse(ObjIterator.defer(() -> new ArrayList<String>().iterator()).hasNext());
        assertFalse(ObjIterator.defer(() -> null).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.defer(null));

        AtomicInteger failCount = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("boom");
        ObjIterator<String> failing = ObjIterator.defer(() -> {
            failCount.incrementAndGet();
            throw failure;
        });
        assertSame(failure, assertThrows(IllegalStateException.class, failing::hasNext));
        assertSame(failure, assertThrows(IllegalStateException.class, failing::next));
        assertEquals(1, failCount.get());

        AtomicInteger recursiveCount = new AtomicInteger();
        AtomicReference<ObjIterator<String>> ref = new AtomicReference<>();
        ObjIterator<String> recursive = ObjIterator.defer(() -> {
            recursiveCount.incrementAndGet();
            ref.get().hasNext();
            return ObjIterator.empty();
        });
        ref.set(recursive);
        IllegalStateException recursiveFailure = assertThrows(IllegalStateException.class, recursive::hasNext);
        assertSame(recursiveFailure, assertThrows(IllegalStateException.class, recursive::next));
        assertEquals(1, recursiveCount.get());
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        ObjIterator<Integer> infinite = ObjIterator.generate(n::incrementAndGet);
        assertEquals(1, infinite.next());
        assertEquals(2, infinite.next());
        assertTrue(infinite.hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate((Supplier<String>) null));

        AtomicInteger counter = new AtomicInteger();
        ObjIterator<Integer> finite = ObjIterator.generate(() -> counter.get() < 3, counter::incrementAndGet);
        assertEquals(List.of(1, 2, 3), finite.toList());
        assertThrows(NoSuchElementException.class, finite::next);
        assertFalse(ObjIterator.generate(() -> false, () -> "never").hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(null, () -> "value"));
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(() -> true, null));

        AtomicInteger predicateCalls = new AtomicInteger();
        AtomicInteger generated = new AtomicInteger();
        ObjIterator<Integer> stateful = ObjIterator.generate(2, remaining -> predicateCalls.incrementAndGet() <= remaining,
                ignored -> generated.getAndIncrement());
        assertTrue(stateful.hasNext());
        assertTrue(stateful.hasNext());
        assertEquals(0, stateful.next());
        assertEquals(1, stateful.next());
        assertFalse(stateful.hasNext());
        assertEquals(3, predicateCalls.get());
        assertEquals("value", ObjIterator.generate(null, s -> s == null, s -> "value").next());
        assertFalse(ObjIterator.generate(5, s -> s < 5, s -> s).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, null, x -> x));
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, x -> true, null));

        AtomicInteger biCalls = new AtomicInteger();
        ObjIterator<Integer> bi = ObjIterator.generate(2, (limit, prev) -> biCalls.incrementAndGet() <= limit, (limit, prev) -> prev == null ? 1 : prev + 1);
        assertTrue(bi.hasNext());
        assertTrue(bi.hasNext());
        assertEquals(1, bi.next());
        assertEquals(2, bi.next());
        assertFalse(bi.hasNext());
        assertEquals(3, biCalls.get());
        assertEquals(List.of(1, 2, 3),
                ObjIterator.<Integer, Integer> generate(0, (state, prev) -> prev == null || prev < 3, (state, prev) -> prev == null ? 1 : prev + 1).toList());
        assertFalse(ObjIterator.generate(0, (state, prev) -> false, (state, prev) -> 1).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, null, (s, p) -> p));
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, (s, p) -> true, null));
    }

    @Test
    public void testSkip() {
        assertEquals(List.of(3, 4, 5), ObjIterator.of(1, 2, 3, 4, 5).skip(2).toList());
        assertFalse(ObjIterator.of(1, 2, 3).skip(10).hasNext());
        assertFalse(ObjIterator.of("a", "b", "c").skip(3).hasNext());
        ObjIterator<Integer> original = ObjIterator.of(1, 2, 3);
        assertSame(original, original.skip(0));
        assertFalse(ObjIterator.<Integer> empty().skip(5).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skip(-1));
        assertThrows(NoSuchElementException.class, () -> ObjIterator.of(1, 2).skip(2).next());

        AtomicInteger mapped = new AtomicInteger();
        ObjIterator<Integer> lazy = ObjIterator.of(1, 2, 3, 4, 5).map(x -> {
            mapped.incrementAndGet();
            return x;
        }).skip(2);
        assertEquals(0, mapped.get());
        assertEquals(3, lazy.next());
        assertEquals(3, mapped.get());

        ObjIterator<Integer> source = new ObjIterator<>() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public Integer next() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return next++;
            }
        };
        ObjIterator<Integer> skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(2, skipped.next());
    }

    @Test
    public void testLimit() {
        assertEquals(List.of("a", "b", "c"), ObjIterator.of("a", "b", "c", "d", "e").limit(3).toList());
        assertEquals(List.of(1, 2, 3), ObjIterator.of(1, 2, 3).limit(5).toList());
        assertEquals(List.of("a"), ObjIterator.of("a", "b").limit(1).toList());
        assertFalse(ObjIterator.of(1, 2, 3).limit(0).hasNext());
        assertFalse(ObjIterator.empty().limit(3).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).limit(-1));
        ObjIterator<Integer> exhausted = ObjIterator.of(1, 2).limit(1);
        exhausted.next();
        assertThrows(NoSuchElementException.class, exhausted::next);

        AtomicInteger attempts = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("transient source failure");
        ObjIterator<String> limited = ObjIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw failure;
            }
            return "value";
        }).limit(1);
        assertSame(failure, assertThrows(IllegalStateException.class, limited::next));
        assertTrue(limited.hasNext());
        assertEquals("value", limited.next());
        assertFalse(limited.hasNext());
        assertEquals(2, attempts.get());
    }

    @Test
    public void testSkipAndLimit() {
        assertEquals(List.of(3, 4, 5), ObjIterator.of(1, 2, 3, 4, 5, 6).skipAndLimit(2, 3).toList());
        assertTrue(ObjIterator.of(1, 2, 3).skipAndLimit(10, 3).toList().isEmpty());
        assertEquals(List.of(1, 2, 3), ObjIterator.of(1, 2, 3).skipAndLimit(0, 10).toList());
        assertTrue(ObjIterator.of(1, 2, 3).skipAndLimit(0, 0).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skipAndLimit(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skipAndLimit(0, -1));
    }

    @Test
    public void testFilter() {
        assertEquals(List.of(2, 4), ObjIterator.of(1, 2, 3, 4).filter(n -> n % 2 == 0).toList());
        assertEquals(List.of(2, 4), ObjIterator.of(1, 2, 3, 4).filter(n -> n > 1).filter(n -> n % 2 == 0).toList());
        assertFalse(ObjIterator.of(1, 3, 5).filter(n -> n % 2 == 0).hasNext());
        assertEquals(List.of(2, 4), ObjIterator.of(2, 4).filter(n -> n % 2 == 0).toList());
        assertFalse(ObjIterator.<Integer> empty().filter(n -> true).hasNext());
    }

    @Test
    public void testMap() {
        assertEquals(List.of("A", "B"), ObjIterator.of("a", "b").map(String::toUpperCase).toList());
        assertEquals(List.of(2, 4), ObjIterator.of(1, 2).map(n -> n * 2).map(n -> n).toList());
        assertEquals(List.of(1, 2), ObjIterator.of("a", "bb").map(String::length).toList());
        assertFalse(ObjIterator.<String> empty().map(String::length).hasNext());
        assertEquals(Arrays.asList("A", null), ObjIterator.of("a", null).map(s -> s == null ? null : s.toUpperCase()).toList());
    }

    @Test
    public void testDistinct() {
        assertEquals(List.of("a", "b", "c"), ObjIterator.of("a", "b", "a", "c", "b").distinct().toList());
        assertEquals(List.of(1, 2, 3), ObjIterator.of(1, 2, 3).distinct().toList());
        assertEquals(List.of("x"), ObjIterator.of("x", "x", "x").distinct().toList());
        assertEquals(Arrays.asList("a", null, "b"), ObjIterator.of("a", null, "a", null, "b").distinct().toList());
        assertFalse(ObjIterator.empty().distinct().hasNext());
        assertEquals(List.of("aa", "b"), ObjIterator.of("aa", "bb", "b", "cc").distinctBy(String::length).toList());
        assertEquals(List.of("a", "b"), ObjIterator.of("a", "b", "a").distinctBy(s -> s).toList());
        assertFalse(ObjIterator.<String> empty().distinctBy(String::length).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.empty().distinctBy(null));
    }

    @Test
    public void testFirstNonNullAndSkipNulls() {
        assertEquals("b", ObjIterator.of(null, "b", "c").firstNonNull().get());
        assertEquals("a", ObjIterator.of("a", "b").firstNonNull().get());
        assertFalse(ObjIterator.of(null, null).firstNonNull().isPresent());
        assertFalse(ObjIterator.<String> empty().firstNonNull().isPresent());

        ObjIterator<String> source = ObjIterator.of(null, "x", "y");
        assertEquals("x", source.firstNonNull().get());
        assertEquals("y", source.next());

        assertEquals(List.of("a", "b", "c"), ObjIterator.of("a", null, "b", null, "c").skipNulls().toList());
        assertFalse(ObjIterator.of(null, null).skipNulls().hasNext());
        assertEquals(List.of("a", "b"), ObjIterator.of("a", "b").skipNulls().toList());
        assertFalse(ObjIterator.<String> empty().skipNulls().hasNext());
        assertEquals(List.of("A", "B", "C"), ObjIterator.of("a", null, "b", null, "c").skipNulls().map(String::toUpperCase).toList());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new Object[] { "a", "b", "c" }, ObjIterator.of("a", "b", "c").toArray());
        assertArrayEquals(new String[] { "a", "b" }, ObjIterator.of("a", "b").toArray(new String[0]));
        assertArrayEquals(new String[] { "a" }, ObjIterator.of("a").toArray(new String[1]));
        String[] larger = ObjIterator.of("a", "b").toArray(new String[4]);
        assertEquals(4, larger.length);
        assertEquals("a", larger[0]);
        assertNull(larger[2]);
        ObjIterator<Integer> partial = ObjIterator.of(1, 2, 3, 4);
        partial.next();
        assertArrayEquals(new Integer[] { 2, 3, 4 }, partial.toArray(new Integer[0]));
        assertEquals(0, ObjIterator.empty().toArray().length);
        assertArrayEquals(new Object[] { "a", null }, ObjIterator.of("a", null).toArray());
        assertEquals(0, ObjIterator.empty().toArray(new String[0]).length);

        List<String> list = ObjIterator.of("a", "b").toList();
        list.add("c");
        assertEquals(List.of("a", "b", "c"), list);
        ObjIterator<Integer> remaining = ObjIterator.of(1, 2, 3);
        remaining.next();
        assertEquals(List.of(2, 3), remaining.toList());
        assertTrue(ObjIterator.empty().toList().isEmpty());
        assertEquals(List.of("only"), ObjIterator.just("only").toList());
        assertEquals(Arrays.asList("a", null, "b"), ObjIterator.of("a", null, "b").toList());
    }

    @Test
    public void testStream() {
        assertEquals(List.of("a", "b"), ObjIterator.of("a", "b").stream().toList());
        assertEquals(2, ObjIterator.of(1, 2, 3, 4).stream().filter(n -> n % 2 == 0).count());
        assertEquals(0, ObjIterator.empty().stream().count());
        assertEquals(Arrays.asList("a", null), ObjIterator.of("a", null).stream().toList());
    }

    @Test
    public void testIndexed() {
        ObjIterator<Indexed<String>> indexed = ObjIterator.of("a", "b").indexed();
        Indexed<String> first = indexed.next();
        assertEquals(0, first.index());
        assertEquals("a", first.value());
        assertEquals(1, indexed.next().index());
        assertFalse(indexed.hasNext());

        ObjIterator<Indexed<String>> offset = ObjIterator.of("a", "b").indexed(100);
        assertEquals(100, offset.next().index());
        assertEquals(101, offset.next().index());
        assertEquals(0, ObjIterator.of("a").indexed(0).next().index());
        assertFalse(ObjIterator.empty().indexed().hasNext());
        assertFalse(ObjIterator.empty().indexed(5).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of("a").indexed(-1));

        ObjIterator<String> source = ObjIterator.of("a", "b");
        ObjIterator<Indexed<String>> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals("b", source.next());

        ObjIterator<Indexed<String>> max = ObjIterator.just("a").indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    public void testForeachRemaining() {
        List<String> collected = new ArrayList<>();
        ObjIterator.of("a", "b", "c").foreachRemaining(collected::add);
        assertEquals(List.of("a", "b", "c"), collected);

        List<Integer> remaining = new ArrayList<>();
        ObjIterator<Integer> partial = ObjIterator.of(1, 2, 3, 4, 5);
        partial.next();
        partial.next();
        partial.foreachRemaining(remaining::add);
        assertEquals(List.of(3, 4, 5), remaining);

        List<String> withNulls = new ArrayList<>();
        ObjIterator.of("a", null, "b").foreachRemaining(withNulls::add);
        assertEquals(Arrays.asList("a", null, "b"), withNulls);
        ObjIterator.empty().foreachRemaining(v -> collected.add("no"));
        assertEquals(3, collected.size());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of("a").foreachRemaining(null));
    }

    @Test
    public void testForeachIndexed() {
        List<String> collected = new ArrayList<>();
        ObjIterator.of("a", "b", "c").foreachIndexed((index, value) -> collected.add(index + ":" + value));
        assertEquals(List.of("0:a", "1:b", "2:c"), collected);

        List<String> remaining = new ArrayList<>();
        ObjIterator<Integer> partial = ObjIterator.of(10, 20, 30, 40);
        partial.next();
        partial.next();
        partial.foreachIndexed((index, value) -> remaining.add(index + ":" + value));
        assertEquals(List.of("0:30", "1:40"), remaining);

        List<String> withNulls = new ArrayList<>();
        ObjIterator.of("a", null).foreachIndexed((index, value) -> withNulls.add(index + ":" + value));
        assertEquals(List.of("0:a", "1:null"), withNulls);
        ObjIterator.empty().foreachIndexed((i, v) -> collected.add("no"));
        assertEquals(3, collected.size());
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of("a").foreachIndexed(null));
    }

    @Test
    public void testChaining() {
        assertEquals(List.of("B", "D"),
                ObjIterator.of("a", "b", null, "c", "d", "e")
                        .skipNulls()
                        .skip(1)
                        .limit(3)
                        .filter(s -> s.length() == 1)
                        .map(String::toUpperCase)
                        .distinct()
                        .filter(s -> s.equals("B") || s.equals("D"))
                        .toList());
        assertEquals(List.of("A", "B", "C"), ObjIterator.of("a", "a", "b", "b", "c").distinct().map(String::toUpperCase).toList());
    }

    /**
     * G54-005: the array-backed {@code toArray(A[])} override in {@code of(T[], from, to)} must reject a
     * {@code null} array with the library's named message, exactly like the base implementation.
     */
    @Test
    public void testToArrayRejectsNullArrayWithNamedMessage() {
        assertEquals("'a' cannot be null",
                assertThrows(NullPointerException.class, () -> ObjIterator.of(new String[] { "a", "b" }, 0, 2).toArray((String[]) null))
                        .getMessage());
        assertEquals("'a' cannot be null",
                assertThrows(NullPointerException.class, () -> ObjIterator.of(new String[] { "a", "b" }).toArray((String[]) null)).getMessage());
        // the iterator-backed base path, which already had the guard
        assertEquals("'a' cannot be null",
                assertThrows(NullPointerException.class, () -> ObjIterator.of(Arrays.asList("a", "b")).toArray((String[]) null)).getMessage());
        assertEquals("'a' cannot be null",
                assertThrows(NullPointerException.class, () -> ObjIterator.<String> empty().toArray((String[]) null)).getMessage());

        // "rejected before consuming any elements"
        final ObjIterator<String> iter = ObjIterator.of(new String[] { "a", "b" }, 0, 2);
        assertThrows(NullPointerException.class, () -> iter.toArray((String[]) null));
        assertEquals(List.of("a", "b"), iter.toList());
    }

    /**
     * G54-003: {@code remove()} throws {@link UnsupportedOperationException} on every iterator the
     * factory methods return, but the class is extensible, so a subclass may override it - which is why
     * {@code of(Iterator)} re-wraps even an existing {@code ObjIterator}.
     */
    @Test
    public void testRemoveContractAndDefensiveWrap() {
        assertThrows(UnsupportedOperationException.class, () -> ObjIterator.of("a", "b").remove());
        assertThrows(UnsupportedOperationException.class, () -> ObjIterator.empty().remove());
        assertThrows(UnsupportedOperationException.class, () -> ObjIterator.of(new String[] { "a", "b" }, 0, 2).remove());
        assertThrows(UnsupportedOperationException.class, () -> ObjIterator.of(Arrays.asList("a", "b").iterator()).remove());

        final AtomicBoolean removed = new AtomicBoolean(false);
        final ObjIterator<String> mutating = new ObjIterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < 2;
            }

            @Override
            public String next() {
                if (i >= 2) {
                    throw new NoSuchElementException();
                }

                i++;
                return "x";
            }

            @Override
            public void remove() {
                removed.set(true);
            }
        };

        mutating.next();
        mutating.remove();
        assertTrue(removed.get(), "an ObjIterator subclass can override remove()");

        removed.set(false);
        assertThrows(UnsupportedOperationException.class, () -> ObjIterator.of((Iterator<String>) mutating).remove());
        assertFalse(removed.get(), "of(Iterator) must not let the subclass's remove() through");
    }
}
