package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for the iterator-family review of 2026-09-02 (r9443).
 *
 * <p>Each test names the defect it locks down. Probes that produced the original evidence live in
 * {@code scripts/cross_review/iterator_review_2026-09-01/}.
 */
public class IteratorRegressionTest extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // B1: BiIterator.limit(count) must cap the pairs produced by the limited iterator AND every view
    //     derived from it. Previously map() built a second, independent budget over the unlimited source.
    // ------------------------------------------------------------------------------------------------

    private static Map<String, Integer> map(final int size) {
        final Map<String, Integer> m = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            m.put("k" + i, i);
        }

        return m;
    }

    @Test
    @DisplayName("B1: BiIterator.limit(n).map(..) shares the remaining-count with the limited iterator")
    public void testBiLimitMapSharesTheQuota() {
        final BiIterator<String, Integer> limited = BiIterator.of(map(6)).limit(2);
        final ObjIterator<Integer> mapped = limited.map((k, v) -> v);

        assertEquals(Arrays.asList(0, 1), mapped.toList());

        // The budget is spent: the limited iterator itself must not hand out more pairs.
        assertFalse(limited.hasNext());
        assertEquals(0, limited.toList().size());
        assertThrows(NoSuchElementException.class, limited::next);
    }

    @Test
    @DisplayName("B1: interleaving BiIterator.limit(n) with its mapped view still yields exactly n pairs")
    public void testBiLimitMapInterleaved() {
        final BiIterator<String, Integer> limited = BiIterator.of(map(10)).limit(3);
        final ObjIterator<String> mapped = limited.map((k, v) -> k);

        assertEquals("k0", mapped.next());
        assertEquals(Pair.of("k1", 1), limited.next());
        assertEquals("k2", mapped.next());

        assertFalse(mapped.hasNext());
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("B1: two mapped views of one BiIterator.limit(n) share a single budget")
    public void testBiLimitTwoMappedViewsShareOneBudget() {
        final BiIterator<String, Integer> limited = BiIterator.of(map(10)).limit(2);

        assertEquals(Collections.singletonList("k0"), limited.map((k, v) -> k).limit(1).toList());
        assertEquals(Collections.singletonList("k1"), limited.map((k, v) -> k).toList());
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("B1: TriIterator.limit(n).map(..) shares the remaining-count too (parity check)")
    public void testTriLimitMapSharesTheQuota() {
        final TriIterator<Integer, Integer, Integer> limited = TriIterator.<Integer, Integer, Integer> generate(0, 10, (i, t) -> t.set(i, i, i)).limit(2);

        assertEquals(Arrays.asList(0, 1), limited.map((a, b, c) -> a).toList());
        assertFalse(limited.hasNext());
    }

    // ------------------------------------------------------------------------------------------------
    // B1 (accounting): a source that fails while producing must not consume limit quota.
    //     Mirrors TriIteratorTest.testIndexedGeneratorAndLimitRetryAfterOutputFailure.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("B1: BiIterator.limit(1) keeps its quota when the generator fails once")
    public void testBiLimitRetryAfterOutputFailure() {
        final AtomicInteger attempts = new AtomicInteger();
        final BiIterator<Integer, Integer> limited = BiIterator.<Integer, Integer> generate(7, 8, (index, output) -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("transient");
            }

            output.set(index, index);
        }).limit(1);

        assertThrows(IllegalStateException.class, limited::next);
        assertEquals(Pair.of(7, 7), limited.next());
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("B1: forEachRemaining on BiIterator.limit(n) also charges quota only for produced pairs")
    public void testBiLimitForEachRemainingRetryAfterOutputFailure() {
        final AtomicInteger attempts = new AtomicInteger();
        final BiIterator<Integer, Integer> limited = BiIterator.<Integer, Integer> generate(0, 10, (index, output) -> {
            if (index == 0 && attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("transient");
            }

            output.set(index, index);
        }).limit(2);

        final List<Pair<Integer, Integer>> seen = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> limited.forEachRemaining((a, b) -> seen.add(Pair.of(a, b))));
        assertTrue(seen.isEmpty());

        limited.forEachRemaining((a, b) -> seen.add(Pair.of(a, b)));
        assertEquals(Arrays.asList(Pair.of(0, 0), Pair.of(1, 1)), seen);
    }

    @Test
    @DisplayName("B1: a failing downstream action still charges the quota for the pair it received")
    public void testBiLimitChargesQuotaWhenTheActionFails() {
        final BiIterator<Integer, Integer> limited = BiIterator.<Integer, Integer> generate(0, 10, (i, p) -> p.set(i, i)).limit(3);

        assertThrows(RuntimeException.class, () -> limited.forEachRemaining((a, b) -> {
            if (a == 1) {
                throw new RuntimeException("stop");
            }
        }));

        // 0 and 1 were produced and therefore charged; exactly one of the three remains.
        assertEquals(Collections.singletonList(Pair.of(2, 2)), limited.toList());
    }

    // ------------------------------------------------------------------------------------------------
    // B2: generate(fromIndex, toIndex, output) must not consume the index when the generator throws.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("B2: BiIterator.generate(from,to,..) retries the failing index instead of dropping it")
    public void testBiIndexedGenerateRetriesFailedIndex() {
        final AtomicInteger attempts = new AtomicInteger();
        final BiIterator<Integer, Integer> iter = BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> {
            if (i == 1 && attempts.getAndIncrement() == 0) {
                throw new RuntimeException("boom@1");
            }

            p.set(i, i);
        });

        assertEquals(Pair.of(0, 0), iter.next());
        assertThrows(RuntimeException.class, iter::next);
        assertEquals(Pair.of(1, 1), iter.next()); // index 1 re-run, not skipped
        assertEquals(Pair.of(2, 2), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("B2: TriIterator.generate(from,to,..) behaves identically (parity check)")
    public void testTriIndexedGenerateRetriesFailedIndex() {
        final AtomicInteger attempts = new AtomicInteger();
        final TriIterator<Integer, Integer, Integer> iter = TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> {
            if (i == 1 && attempts.getAndIncrement() == 0) {
                throw new RuntimeException("boom@1");
            }

            t.set(i, i, i);
        });

        assertEquals(Triple.of(0, 0, 0), iter.next());
        assertThrows(RuntimeException.class, iter::next);
        assertEquals(Triple.of(1, 1, 1), iter.next());
        assertEquals(Triple.of(2, 2, 2), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("B2: the retry also applies to the mapped and forEachRemaining paths")
    public void testBiIndexedGenerateRetryAcrossViews() {
        final AtomicInteger attempts = new AtomicInteger();
        final BiIterator<Integer, Integer> iter = BiIterator.<Integer, Integer> generate(0, 2, (i, p) -> {
            if (i == 0 && attempts.getAndIncrement() == 0) {
                throw new RuntimeException("boom@0");
            }

            p.set(i, i);
        });

        final ObjIterator<Integer> mapped = iter.map((a, b) -> a);
        assertThrows(RuntimeException.class, mapped::next);
        assertEquals(Arrays.asList(0, 1), mapped.toList());
    }

    // ------------------------------------------------------------------------------------------------
    // B3: skip(n).map(..) must not consume the skipped elements when map() is called.
    // ------------------------------------------------------------------------------------------------

    private static Iterator<Integer> counting(final AtomicInteger consumed, final int size) {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Integer next() {
                consumed.incrementAndGet();
                return i++;
            }
        };
    }

    @Test
    @DisplayName("B3: BiIterator.skip(n).map(..) performs the skip lazily, not at composition time")
    public void testBiSkipMapIsLazy() {
        final AtomicInteger consumed = new AtomicInteger();
        final BiIterator<Integer, Integer> skipped = BiIterator.zip(counting(consumed, 5), counting(new AtomicInteger(), 5)).skip(2);

        final ObjIterator<String> mapped = skipped.map((a, b) -> a + "/" + b);
        assertEquals(0, consumed.get(), "map() must not consume the skipped pairs");

        assertEquals(Arrays.asList("2/2", "3/3", "4/4"), mapped.toList());
        assertEquals(5, consumed.get());
    }

    @Test
    @DisplayName("B3: TriIterator.skip(n).map(..) performs the skip lazily too")
    public void testTriSkipMapIsLazy() {
        final AtomicInteger consumed = new AtomicInteger();
        final TriIterator<Integer, Integer, Integer> skipped = TriIterator
                .zip(counting(consumed, 5), counting(new AtomicInteger(), 5), counting(new AtomicInteger(), 5))
                .skip(2);

        final ObjIterator<String> mapped = skipped.map((a, b, c) -> a + "/" + b + "/" + c);
        assertEquals(0, consumed.get(), "map() must not consume the skipped triples");

        assertEquals(Arrays.asList("2/2/2", "3/3/3", "4/4/4"), mapped.toList());
    }

    @Test
    @DisplayName("B3: skip(n) still resumes a partially completed skip after a failure")
    public void testBiSkipRetainsProgressAcrossOutputFailure() {
        final AtomicInteger failures = new AtomicInteger();
        final BiIterator<Integer, Integer> skipped = BiIterator.<Integer, Integer> generate(0, 4, (index, output) -> {
            if (index == 1 && failures.getAndIncrement() == 0) {
                throw new IllegalStateException("transient");
            }

            output.set(index, index);
        }).skip(2);

        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(Pair.of(2, 2), skipped.next());
    }

    // ------------------------------------------------------------------------------------------------
    // B4: BiIterator.of(Iterator<Map.Entry>) does NOT normalise exhaustion - it reports whatever the
    //     wrapped entry iterator throws. Normalising it to this library's message was tried and
    //     deliberately reverted, so every reachable view is pinned here to keep it that way.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("B4: BiIterator.of(entryIterator) propagates the entry iterator's own exhaustion exception")
    public void testBiOfEntryIteratorExhaustionMessage() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);

        final BiIterator<String, Integer> iter = BiIterator.of(m.entrySet().iterator());
        assertEquals(Pair.of("a", 1), iter.next());

        // a LinkedHashMap entry iterator raises a message-less NoSuchElementException, and that is what a
        // caller sees - not InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, iter::next);
        assertNull(ex.getMessage());

        // ... and on the empty case, on next(action) and through the mapped view.
        ex = assertThrows(NoSuchElementException.class, () -> BiIterator.of(Collections.<String, Integer> emptyMap().entrySet().iterator()).next());
        assertNull(ex.getMessage());

        final ObjIterator<String> mapped = BiIterator.of(m.entrySet().iterator()).map((k, v) -> k + v);
        assertEquals("a1", mapped.next());
        ex = assertThrows(NoSuchElementException.class, mapped::next);
        assertNull(ex.getMessage());

        final BiIterator<String, Integer> drained = BiIterator.of(m.entrySet().iterator());
        drained.forEachRemaining((k, v) -> {
        });
        ex = assertThrows(NoSuchElementException.class, () -> drained.map((k, v) -> k).next());
        assertNull(ex.getMessage());

        // of(Map) routes through of(Iterator) too, so it reports the same way
        final BiIterator<String, Integer> fromMap = BiIterator.of(m);
        assertEquals(Pair.of("a", 1), fromMap.next());
        assertNull(assertThrows(NoSuchElementException.class, fromMap::next).getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // B6: covariant result types on ObjIterator.map / ObjIterator.generate / Iterators.map.
    //     These are compile-time assertions: the bodies would not compile before the widening.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("B6: ObjIterator.map accepts a Function whose result type is a subtype of U")
    public void testObjIteratorMapIsCovariantInTheResult() {
        final Function<String, String> identity = s -> s;

        final ObjIterator<CharSequence> mapped = ObjIterator.of("a", "b").map(identity);
        assertEquals(Arrays.asList("a", "b"), mapped.toList());

        final ObjIterator<CharSequence> viaIterators = Iterators.map(Arrays.asList("a", "b").iterator(), identity);
        assertEquals(Arrays.asList("a", "b"), viaIterators.toList());

        final ObjIterator<CharSequence> viaIterable = Iterators.map(Arrays.asList("a", "b"), identity);
        assertEquals(Arrays.asList("a", "b"), viaIterable.toList());
    }

    @Test
    @DisplayName("B6: ObjIterator.generate accepts a supplier whose result type is a subtype of T")
    public void testObjIteratorGenerateIsCovariantInTheResult() {
        final Predicate<int[]> hasNext = st -> st[0] < 3;
        final Function<int[], Integer> supplier = st -> st[0]++;

        final ObjIterator<Number> numbers = ObjIterator.generate(new int[] { 0 }, hasNext, supplier);
        assertEquals(Arrays.asList(0, 1, 2), numbers.toList());

        final BiPredicate<int[], Object> biHasNext = (st, prev) -> st[0] < 2;
        final BiFunction<int[], Object, Integer> biSupplier = (st, prev) -> st[0]++;

        final ObjIterator<Object> objects = ObjIterator.generate(new int[] { 0 }, biHasNext, biSupplier);
        assertEquals(Arrays.asList(0, 1), objects.toList());
    }

    // ------------------------------------------------------------------------------------------------
    // B7: LineIterator.stream() must own the iterator (and therefore the reader).
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("B7: closing the stream returned by LineIterator.stream() closes the iterator")
    public void testLineIteratorStreamClosesTheIterator(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("lines.txt");
        Files.write(file, Arrays.asList("l1", "l2", "l3"), StandardCharsets.UTF_8);

        final LineIterator iter = LineIterator.of(file.toFile(), StandardCharsets.UTF_8);
        List<String> lines;

        try (Stream<String> stream = iter.stream()) {
            lines = stream.toList();
        }

        assertEquals(Arrays.asList("l1", "l2", "l3"), lines);
        assertFalse(iter.hasNext());

        // The reader is released, so the file can be deleted even on platforms that lock open handles.
        assertTrue(file.toFile().delete());
    }

    @Test
    @DisplayName("B7: an unconsumed LineIterator.stream() still releases the reader when closed")
    public void testLineIteratorStreamClosesWithoutConsuming(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("lines2.txt");
        Files.write(file, Arrays.asList("a", "b"), StandardCharsets.UTF_8);

        final LineIterator iter = LineIterator.of(file.toFile(), StandardCharsets.UTF_8);
        iter.stream().close();

        assertFalse(iter.hasNext());
        assertTrue(file.toFile().delete());
    }

    @Test
    @DisplayName("B7: closing the iterator and the stream is idempotent in either order")
    public void testLineIteratorCloseIsIdempotent(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("lines3.txt");
        Files.write(file, Arrays.asList("x"), StandardCharsets.UTF_8);
        final File f = file.toFile();

        try (LineIterator iter = LineIterator.of(f, StandardCharsets.UTF_8)) {
            try (Stream<String> stream = iter.stream()) {
                assertEquals(Collections.singletonList("x"), stream.toList());
            }

            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
        }

        assertTrue(f.delete());
    }

    // ------------------------------------------------------------------------------------------------
    // D1: forEachRemaining / foreachRemaining / map are no longer abstract, so a subclass only has to
    //     implement hasNext(), next() and next(action). The inherited defaults must behave correctly.
    // ------------------------------------------------------------------------------------------------

    /** A minimal BiIterator implemented with only the three methods that are still abstract. */
    private static final class MinimalBiIterator extends BiIterator<String, Integer> {
        private int cursor = 0;
        private final int size;

        MinimalBiIterator(final int size) {
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Pair<String, Integer> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final int i = cursor++;
            return Pair.of("k" + i, i);
        }

        @Override
        protected <E extends Exception> void next(final Throwables.BiConsumer<? super String, ? super Integer, E> action) throws NoSuchElementException, E {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final int i = cursor++;
            action.accept("k" + i, i);
        }
    }

    /** A minimal TriIterator implemented with only the three methods that are still abstract. */
    private static final class MinimalTriIterator extends TriIterator<String, Integer, Boolean> {
        private int cursor = 0;
        private final int size;

        MinimalTriIterator(final int size) {
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Triple<String, Integer, Boolean> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final int i = cursor++;
            return Triple.of("k" + i, i, i % 2 == 0);
        }

        @Override
        protected <E extends Exception> void next(final Throwables.TriConsumer<? super String, ? super Integer, ? super Boolean, E> action)
                throws NoSuchElementException, E {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final int i = cursor++;
            action.accept("k" + i, i, i % 2 == 0);
        }
    }

    @Test
    @DisplayName("D1: a BiIterator subclass needs only hasNext/next/next(action); the defaults do the rest")
    public void testMinimalBiIteratorSubclass() {
        final List<String> expected = Arrays.asList("k0=0", "k1=1", "k2=2");

        final List<String> viaForEachRemaining = new ArrayList<>();
        new MinimalBiIterator(3).forEachRemaining((k, v) -> viaForEachRemaining.add(k + "=" + v));
        assertEquals(expected, viaForEachRemaining);

        final List<String> viaLowercaseForeachRemaining = new ArrayList<>();
        new MinimalBiIterator(3).foreachRemaining((k, v) -> viaLowercaseForeachRemaining.add(k + "=" + v));
        assertEquals(expected, viaLowercaseForeachRemaining);

        assertEquals(expected, new MinimalBiIterator(3).map((k, v) -> k + "=" + v).toList());
        assertEquals(expected, new MinimalBiIterator(3).stream((k, v) -> k + "=" + v).toList());

        // the inherited defaults also compose with skip/limit/filter
        assertEquals(Collections.singletonList("k1=1"), new MinimalBiIterator(3).skip(1).limit(1).map((k, v) -> k + "=" + v).toList());
        assertEquals(Arrays.asList("k0=0", "k2=2"), new MinimalBiIterator(3).filter((k, v) -> v % 2 == 0).map((k, v) -> k + "=" + v).toList());
        assertEquals(3, new MinimalBiIterator(3).count());
    }

    @Test
    @DisplayName("D1: the same for a TriIterator subclass")
    public void testMinimalTriIteratorSubclass() {
        final List<String> expected = Arrays.asList("k0=0/true", "k1=1/false", "k2=2/true");

        final List<String> viaForEachRemaining = new ArrayList<>();
        new MinimalTriIterator(3).forEachRemaining((k, v, b) -> viaForEachRemaining.add(k + "=" + v + "/" + b));
        assertEquals(expected, viaForEachRemaining);

        final List<String> viaLowercaseForeachRemaining = new ArrayList<>();
        new MinimalTriIterator(3).foreachRemaining((k, v, b) -> viaLowercaseForeachRemaining.add(k + "=" + v + "/" + b));
        assertEquals(expected, viaLowercaseForeachRemaining);

        assertEquals(expected, new MinimalTriIterator(3).map((k, v, b) -> k + "=" + v + "/" + b).toList());
        assertEquals(expected, new MinimalTriIterator(3).stream((k, v, b) -> k + "=" + v + "/" + b).toList());
        assertEquals(Collections.singletonList("k1=1/false"), new MinimalTriIterator(3).skip(1).limit(1).map((k, v, b) -> k + "=" + v + "/" + b).toList());
        assertEquals(Arrays.asList("k0=0/true", "k2=2/true"),
                new MinimalTriIterator(3).filter((k, v, b) -> v % 2 == 0).map((k, v, b) -> k + "=" + v + "/" + b).toList());
        assertEquals(3, new MinimalTriIterator(3).count());
    }

    @Test
    @DisplayName("D1: the inherited map() default is lazy and propagates exhaustion correctly")
    public void testInheritedMapDefaultIsLazyAndFailsFast() {
        final AtomicInteger applied = new AtomicInteger();
        final ObjIterator<String> mapped = new MinimalBiIterator(2).map((k, v) -> {
            applied.incrementAndGet();
            return k;
        });

        assertEquals(0, applied.get(), "the mapper must not run before the first pull");
        assertTrue(mapped.hasNext());
        assertEquals(0, applied.get(), "hasNext() must not run the mapper");

        assertEquals("k0", mapped.next());
        assertEquals(1, applied.get());
        assertEquals("k1", mapped.next());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, mapped::next);
    }

    // ------------------------------------------------------------------------------------------------
    // D6: the protected next(action) overloads validate their argument uniformly.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("D6: null actions and mappers are rejected with IllegalArgumentException everywhere")
    public void testNullArgumentsRejectedUniformly() {
        final List<BiIterator<Integer, Integer>> biIterators = Arrays.asList( //
                BiIterator.<Integer, Integer> generate(() -> true, p -> p.set(1, 1)), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)), //
                BiIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator()), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)).filter((a, b) -> true), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)).skip(1), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)).limit(2));

        for (final BiIterator<Integer, Integer> iter : biIterators) {
            assertThrows(IllegalArgumentException.class, () -> iter.forEachRemaining((java.util.function.BiConsumer<Integer, Integer>) null));
            assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining((Throwables.BiConsumer<Integer, Integer, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> iter.map(null));
        }

        final List<TriIterator<Integer, Integer, Integer>> triIterators = Arrays.asList( //
                TriIterator.<Integer, Integer, Integer> generate(() -> true, t -> t.set(1, 1, 1)), //
                TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i)), //
                TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i)).filter((a, b, c) -> true));

        for (final TriIterator<Integer, Integer, Integer> iter : triIterators) {
            // the cast is required: a bare `null` is ambiguous between forEachRemaining(TriConsumer)
            // and the deprecated forEachRemaining(Consumer<? super Triple>) inherited from Iterator.
            assertThrows(IllegalArgumentException.class,
                    () -> iter.forEachRemaining((com.landawn.abacus.util.function.TriConsumer<Integer, Integer, Integer>) null));
            assertThrows(IllegalArgumentException.class,
                    () -> iter.foreachRemaining((Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> iter.map(null));
        }
    }

    @Test
    @DisplayName("D6: the protected next(action) overloads reject a null action uniformly")
    public void testProtectedNextRejectsNullAction() {
        // next(action) is protected, so only an in-package caller (or a subclass) can reach it - which is
        // exactly the audience that used to get a bare NPE from some factories and an IAE from others.
        final List<BiIterator<Integer, Integer>> biIterators = Arrays.asList( //
                BiIterator.<Integer, Integer> generate(() -> true, p -> p.set(1, 1)), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)), //
                BiIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator()), //
                BiIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), 0, 0), //
                BiIterator.of(Collections.singletonMap(1, 1).entrySet().iterator()), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)).filter((a, b) -> true), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)).skip(1), //
                BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i)).limit(2));

        for (final BiIterator<Integer, Integer> iter : biIterators) {
            assertThrows(IllegalArgumentException.class, () -> iter.next(null));
            assertTrue(iter.hasNext(), "rejecting a null action must not consume a pair");
        }

        final List<TriIterator<Integer, Integer, Integer>> triIterators = Arrays.asList( //
                TriIterator.<Integer, Integer, Integer> generate(() -> true, t -> t.set(1, 1, 1)), //
                TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i)), //
                TriIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator()), //
                TriIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator(), 0, 0, 0), //
                TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i)).filter((a, b, c) -> true), //
                TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i)).skip(1), //
                TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i)).limit(2));

        for (final TriIterator<Integer, Integer, Integer> iter : triIterators) {
            assertThrows(IllegalArgumentException.class, () -> iter.next(null));
            assertTrue(iter.hasNext(), "rejecting a null action must not consume a triple");
        }
    }

    @Test
    @DisplayName("D6: validating a null action must not consume an element")
    public void testNullValidationDoesNotConsume() {
        final BiIterator<Integer, Integer> iter = BiIterator.<Integer, Integer> generate(0, 2, (i, p) -> p.set(i, i));

        assertThrows(IllegalArgumentException.class, () -> iter.map(null));
        assertEquals(Pair.of(0, 0), iter.next(), "map(null) must not have consumed a pair");
    }

    // ------------------------------------------------------------------------------------------------
    // O1: the index-range generators report their bounds without array-shaped wording.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("O1: generate(from,to,..) rejects a bad range with a range-shaped message")
    public void testIndexedGenerateBoundsMessage() {
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> BiIterator.generate(3, 1, (i, p) -> p.set(i, i)));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("fromIndex = 3"), ex.getMessage());
        assertTrue(ex.getMessage().contains("toIndex = 1"), ex.getMessage());
        assertFalse(ex.getMessage().contains("length"), "the array-oriented wording must be gone: " + ex.getMessage());

        ex = assertThrows(IndexOutOfBoundsException.class, () -> BiIterator.generate(-1, 1, (i, p) -> p.set(i, i)));
        assertTrue(ex.getMessage().contains("fromIndex = -1"), ex.getMessage());

        ex = assertThrows(IndexOutOfBoundsException.class, () -> TriIterator.generate(3, 1, (i, t) -> t.set(i, i, i)));
        assertFalse(ex.getMessage().contains("length"), ex.getMessage());

        // the valid degenerate range is still accepted
        assertFalse(BiIterator.generate(2, 2, (i, p) -> p.set(i, i)).hasNext());
        assertFalse(TriIterator.generate(2, 2, (i, t) -> t.set(i, i, i)).hasNext());
    }

    // ------------------------------------------------------------------------------------------------
    // O2: generate(from, to, ..) no longer wraps its cursor in a MutableInt; the full index range,
    //     including the mapped view, must still be walked exactly once.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("O2: the index-range generators still walk [from, to) exactly once across all views")
    public void testIndexedGenerateWalksTheRangeOnce() {
        final List<Integer> indices = new ArrayList<>();
        final BiIterator<Integer, Integer> iter = BiIterator.<Integer, Integer> generate(5, 9, (i, p) -> {
            indices.add(i);
            p.set(i, i * i);
        });

        assertEquals(Pair.of(5, 25), iter.next());
        assertEquals(Arrays.asList(6, 7), iter.map((a, b) -> a).limit(2).toList());
        assertEquals(Collections.singletonList(Pair.of(8, 64)), iter.toList());
        assertEquals(Arrays.asList(5, 6, 7, 8), indices);
        assertFalse(iter.hasNext());
    }

    // ------------------------------------------------------------------------------------------------
    // D2: the documented split between ObjIterator.defer and the primitive defer(..) siblings.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("D2: ObjIterator.defer treats a null supplier result as empty; the primitives reject it")
    public void testDeferNullSupplierResultContract() {
        assertFalse(ObjIterator.defer(() -> null).hasNext());

        assertThrows(IllegalStateException.class, () -> IntIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> LongIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> DoubleIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> FloatIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> ShortIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> ByteIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> CharIterator.defer(() -> null).hasNext());
        assertThrows(IllegalStateException.class, () -> BooleanIterator.defer(() -> null).hasNext());
    }

    // ------------------------------------------------------------------------------------------------
    // J5: the ObjIterator array overloads read the backing array live, like the primitive siblings.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("J5: ObjIterator.of(array) reads elements live, and the varargs null forms differ")
    public void testObjIteratorArrayIsALiveView() {
        final String[] a = { "a", "b", "c" };
        final ObjIterator<String> iter = ObjIterator.of(a, 0, 3);

        assertEquals("a", iter.next());
        a[1] = "B"; // not yet consumed -> visible
        a[0] = "A"; // already consumed -> not visible
        assertEquals(Arrays.asList("B", "c"), iter.toList());

        assertEquals(Collections.singletonList(null), ObjIterator.of((String) null).toList());
        assertFalse(ObjIterator.of((String[]) null).hasNext());
    }

    // ------------------------------------------------------------------------------------------------
    // Cross-cutting: every drain path of a limited/skipped/filtered iterator must agree.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("All BiIterator drain paths agree for skip/limit/filter combinations")
    public void testBiDrainPathsAgree() {
        record Case(String name, java.util.function.Supplier<BiIterator<String, Integer>> factory) {
        }

        final List<Case> cases = Arrays.asList( //
                new Case("of(Map)", () -> BiIterator.of(map(4))), //
                new Case("skip(1)", () -> BiIterator.of(map(4)).skip(1)), //
                new Case("limit(3)", () -> BiIterator.of(map(4)).limit(3)), //
                new Case("filter", () -> BiIterator.of(map(4)).filter((k, v) -> v % 2 == 0)), //
                new Case("skip(1).limit(2)", () -> BiIterator.of(map(4)).skip(1).limit(2)), //
                new Case("limit(3).skip(1)", () -> BiIterator.of(map(4)).limit(3).skip(1)), //
                new Case("filter.limit(2)", () -> BiIterator.of(map(4)).filter((k, v) -> true).limit(2)));

        for (final Case c : cases) {
            final List<String> expected = new ArrayList<>();
            final BiIterator<String, Integer> base = c.factory().get();

            while (base.hasNext()) {
                final Pair<String, Integer> p = base.next();
                expected.add(p.left() + "=" + p.right());
            }

            final List<String> viaForEach = new ArrayList<>();
            c.factory().get().forEachRemaining((k, v) -> viaForEach.add(k + "=" + v));
            assertEquals(expected, viaForEach, c.name() + " forEachRemaining");

            final List<String> viaForeach = new ArrayList<>();
            c.factory().get().foreachRemaining((k, v) -> viaForeach.add(k + "=" + v));
            assertEquals(expected, viaForeach, c.name() + " foreachRemaining");

            assertEquals(expected, c.factory().get().map((k, v) -> k + "=" + v).toList(), c.name() + " map");
            assertEquals(expected, c.factory().get().stream((k, v) -> k + "=" + v).toList(), c.name() + " stream");
            assertEquals(expected.size(), c.factory().get().count(), c.name() + " count");

            final Pair<List<String>, List<Integer>> unzipped = c.factory().get().unzipToLists(ArrayList::new);
            final List<String> viaUnzip = new ArrayList<>();

            for (int i = 0; i < unzipped.left().size(); i++) {
                viaUnzip.add(unzipped.left().get(i) + "=" + unzipped.right().get(i));
            }

            assertEquals(expected, viaUnzip, c.name() + " unzipToLists");

            final Object[] asArray = c.factory().get().toArray();
            assertEquals(expected.size(), asArray.length, c.name() + " toArray");
        }
    }

    // ================================================================================================
    // Cycle 2
    // ================================================================================================

    // ------------------------------------------------------------------------------------------------
    // C-101: the array-backed toArray(A[]) must null-terminate AFTER copying, so that a caller passing
    //        the very array the iterator reads from does not have a source element destroyed.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("C-101: toArray(a) where a is the backing array must not clobber an uncopied element")
    public void testArrayBackedToArrayWithAliasedTarget() {
        final String[] a = { "a", "b", "c", "d" };
        final String[] result = ObjIterator.of(a, 1, 3).toArray(a);

        // exactly what new ArrayList<>(List.of("b","c")).toArray(a) produces
        assertArrayEquals(new String[] { "b", "c", null, "d" }, result);
        assertSame(a, result, "an over-sized target must be reused, per Collection.toArray(T[])");

        // reachable through the plainest entry point too: of(T...) plus a partial read
        final String[] b = { "a", "b", "c", "d", "e" };
        final ObjIterator<String> iter = ObjIterator.of(b);
        iter.next();
        assertArrayEquals(new String[] { "b", "c", "d", "e", null }, iter.toArray(b));

        // and the whole-range case, where no sentinel is written at all
        final String[] c = { "x", "y" };
        assertArrayEquals(new String[] { "x", "y" }, ObjIterator.of(c, 0, 2).toArray(c));
    }

    @Test
    @DisplayName("C-101: the array-backed toArray(A[]) matches ArrayList across the whole size matrix")
    public void testArrayBackedToArrayMatchesArrayListContract() {
        final Integer[] source = { 0, 1, 2, 3, 4 };

        for (int from = 0; from <= source.length; from++) {
            for (int to = from; to <= source.length; to++) {
                for (int consumed = 0; consumed <= to - from; consumed++) {
                    for (int targetLen = 0; targetLen <= source.length + 2; targetLen++) {
                        final List<Integer> expectedElements = new ArrayList<>(Arrays.asList(source).subList(from + consumed, to));

                        final ObjIterator<Integer> iter = ObjIterator.of(source, from, to);
                        for (int i = 0; i < consumed; i++) {
                            iter.next();
                        }

                        final Integer[] target = new Integer[targetLen];
                        Arrays.fill(target, 99);
                        final Integer[] reference = new Integer[targetLen];
                        Arrays.fill(reference, 99);

                        final String tag = "from=" + from + " to=" + to + " consumed=" + consumed + " targetLen=" + targetLen;
                        assertArrayEquals(new ArrayList<>(expectedElements).toArray(reference), iter.toArray(target), tag);
                        assertFalse(iter.hasNext(), tag + " must be exhausted");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("C-101: a component-type mismatch must not have mutated the target array")
    public void testArrayBackedToArrayTypeMismatchLeavesTargetUntouched() {
        final Object[] source = { "a", "b", "c", "d" };
        final Integer[] target = { 1, 2, 3, 4, 5, 6 };

        assertThrows(ArrayStoreException.class, () -> ObjIterator.of(source, 1, 3).toArray(target));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, target, "the sentinel must not be written before the copy fails");
    }

    @Test
    @DisplayName("C-101: empty range, null target and Unicode payloads keep their documented behaviour")
    public void testArrayBackedToArrayEdges() {
        final String[] a = { "a", "b", "c" };

        final String[] target = { "x", "y", "z" };
        assertArrayEquals(new String[] { null, "y", "z" }, ObjIterator.of(a, 1, 1).toArray(target));

        assertThrows(NullPointerException.class, () -> ObjIterator.of(a, 0, 2).toArray((String[]) null));

        final String[] uni = { "caf\u00e9", "\ud83d\ude00", "\u4e2d\u6587" };
        assertArrayEquals(new String[] { "\ud83d\ude00", "\u4e2d\u6587" }, ObjIterator.of(uni, 1, 3).toArray(new String[0]));
        assertArrayEquals(new String[] { "\ud83d\ude00", "\u4e2d\u6587", null, "\u4e2d\u6587" },
                ObjIterator.of(uni, 1, 3).toArray(new String[] { "p", "q", "r", "\u4e2d\u6587" }));
    }

    // ------------------------------------------------------------------------------------------------
    // C-104: the array-backed toList() builds the list in one copy and still exhausts the iterator.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("C-104: array-backed toList() is a mutable, independent, single-copy ArrayList")
    public void testArrayBackedToList() {
        final String[] a = { "a", "b", "c", "d" };

        final ObjIterator<String> iter = ObjIterator.of(a, 1, 3);
        final List<String> list = iter.toList();

        assertEquals(Arrays.asList("b", "c"), list);
        assertFalse(iter.hasNext());

        list.add("z");
        list.set(0, "B");
        assertEquals(Arrays.asList("B", "c", "z"), list);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, a, "the backing array must not be aliased by the result");

        final ObjIterator<String> partial = ObjIterator.of(a, 0, 4);
        partial.next();
        assertEquals(Arrays.asList("b", "c", "d"), partial.toList());

        assertEquals(Collections.emptyList(), ObjIterator.of(a, 2, 2).toList());
        assertEquals(Arrays.asList("a", "b", "c", "d"), ObjIterator.of(a, 0, 4).toList());
        assertTrue(ObjIterator.of(a, 2, 2).toList() instanceof ArrayList);
    }

    // ------------------------------------------------------------------------------------------------
    // C-102: both BiIterator.zip overloads latch exhaustion instead of re-probing their sources.
    // ------------------------------------------------------------------------------------------------

    private static Iterator<Integer> probeCounting(final int[] probes, final Iterator<Integer> delegate) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                probes[0]++;
                return delegate.hasNext();
            }

            @Override
            public Integer next() {
                return delegate.next();
            }
        };
    }

    @Test
    @DisplayName("C-102: an exhausted BiIterator.zip stops probing its sources")
    public void testBiZipLatchesExhaustion() {
        final int[] probes = { 0 };
        final BiIterator<Integer, Integer> zipped = BiIterator.zip(probeCounting(probes, Collections.<Integer> emptyList().iterator()),
                Arrays.asList(1, 2).iterator());

        for (int i = 0; i < 5; i++) {
            assertFalse(zipped.hasNext());
        }

        assertEquals(1, probes[0], "the source must be probed once, then latched");

        final int[] probes2 = { 0 };
        final BiIterator<Integer, Integer> withDefaults = BiIterator.zip(probeCounting(probes2, Collections.<Integer> emptyList().iterator()),
                Collections.<Integer> emptyList().iterator(), 0, 0);

        for (int i = 0; i < 5; i++) {
            assertFalse(withDefaults.hasNext());
        }

        assertEquals(1, probes2[0], "the defaults overload must latch as well");
    }

    @Test
    @DisplayName("C-102: the latch is shared with the mapped view and survives a full drain")
    public void testBiZipLatchIsSharedAndSticky() {
        final int[] probes = { 0 };
        final BiIterator<Integer, Integer> zipped = BiIterator.zip(probeCounting(probes, Collections.<Integer> emptyList().iterator()),
                Arrays.asList(1).iterator());

        final ObjIterator<Integer> mapped = zipped.map((a, b) -> a);
        for (int i = 0; i < 3; i++) {
            assertFalse(mapped.hasNext());
        }
        assertFalse(zipped.hasNext());
        assertEquals(1, probes[0], "the mapped view must share the enclosing latch, not keep its own");

        final int[] probes2 = { 0 };
        final BiIterator<Integer, Integer> drained = BiIterator.zip(probeCounting(probes2, Arrays.asList(1, 2).iterator()), Arrays.asList(9, 8).iterator());
        final List<String> seen = new ArrayList<>();
        drained.forEachRemaining((a, b) -> seen.add(a + "/" + b));
        assertEquals(Arrays.asList("1/9", "2/8"), seen);

        final int afterDrain = probes2[0];
        for (int i = 0; i < 5; i++) {
            assertFalse(drained.hasNext());
        }
        assertEquals(afterDrain, probes2[0], "hasNext() after a full drain must not resume probing");
    }

    @Test
    @DisplayName("C-102: zip still yields the same elements through every drain path after the change")
    public void testBiZipUnchangedForWellBehavedSources() {
        assertEquals(Arrays.asList("1/9", "2/8"),
                BiIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(9, 8).iterator()).map((a, b) -> a + "/" + b).toList());

        final List<String> viaForeach = new ArrayList<>();
        BiIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(9, 8).iterator()).foreachRemaining((a, b) -> viaForeach.add(a + "/" + b));
        assertEquals(Arrays.asList("1/9", "2/8"), viaForeach);

        assertEquals(Arrays.asList("1/9", "2/0"),
                BiIterator.zip(Arrays.asList(1, 2).iterator(), Arrays.asList(9).iterator(), -1, 0).map((a, b) -> a + "/" + b).toList());

        final BiIterator<Integer, Integer> once = BiIterator.zip(Arrays.asList(1).iterator(), Arrays.asList(9).iterator());
        assertEquals(1, once.toList().size());
        assertEquals(0, once.toList().size());
    }

    // ------------------------------------------------------------------------------------------------
    // C-103: a supplier that RETURNS null must be distinguishable from a null supplier ARGUMENT, and
    //        for the per-component form the failing supplier must be identifiable.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("C-103: null supplier argument and null supplier result carry different messages")
    public void testSupplierDiagnosticsAreDistinguishable() {
        final BiIterator<String, Integer> bi = BiIterator.zip(new String[] { "a" }, new Integer[] { 1 });

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bi.unzipToLists(null));
        assertTrue(ex.getMessage().contains("'supplier'"), ex.getMessage());
        assertFalse(ex.getMessage().contains("get()"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> BiIterator.zip(new String[] { "a" }, new Integer[] { 1 }).unzipToLists(() -> null));
        assertTrue(ex.getMessage().contains("supplier.get()"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> BiIterator.zip(new String[] { "a" }, new Integer[] { 1 }).unzipToSets(() -> null));
        assertTrue(ex.getMessage().contains("supplier.get()"), ex.getMessage());

        final TriIterator<String, Integer, Boolean> tri = TriIterator.zip(new String[] { "a" }, new Integer[] { 1 }, new Boolean[] { true });
        ex = assertThrows(IllegalArgumentException.class, () -> tri.unzipToLists(() -> null));
        assertTrue(ex.getMessage().contains("supplier.get()"), ex.getMessage());
    }

    @Test
    @DisplayName("C-103: unzipToCollections names WHICH supplier returned null")
    public void testUnzipToCollectionsNamesTheFailingSupplier() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BiIterator.zip(new String[] { "a" }, new Integer[] { 1 }).unzipToCollections(() -> null, ArrayList::new));
        assertTrue(ex.getMessage().contains("leftSupplier.get()"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class,
                () -> BiIterator.zip(new String[] { "a" }, new Integer[] { 1 }).unzipToCollections(ArrayList::new, () -> null));
        assertTrue(ex.getMessage().contains("rightSupplier.get()"), ex.getMessage());

        final TriIterator<String, Integer, Boolean> tri = TriIterator.zip(new String[] { "a" }, new Integer[] { 1 }, new Boolean[] { true });
        ex = assertThrows(IllegalArgumentException.class, () -> tri.unzipToCollections(ArrayList::new, () -> null, ArrayList::new));
        assertTrue(ex.getMessage().contains("middleSupplier.get()"), ex.getMessage());
    }

    @Test
    @DisplayName("C-103: supplier validation still happens before any element is consumed")
    public void testSupplierValidationConsumesNothing() {
        final BiIterator<Integer, Integer> bi = BiIterator.<Integer, Integer> generate(0, 3, (i, p) -> p.set(i, i));

        assertThrows(IllegalArgumentException.class, () -> bi.unzipToLists(() -> null));
        assertEquals(Pair.of(0, 0), bi.next(), "no pair may have been consumed while validating");

        final TriIterator<Integer, Integer, Integer> tri = TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, t) -> t.set(i, i, i));
        assertThrows(IllegalArgumentException.class, () -> tri.unzipToCollections(ArrayList::new, ArrayList::new, () -> null));
        assertEquals(Triple.of(0, 0, 0), tri.next());
    }

    // ================================================================================================
    // Cycle 3
    // ================================================================================================

    // ------------------------------------------------------------------------------------------------
    // C-201 / C-202: LineIterator resource lifetime. Draining does NOT close (documented, matches the
    //        Apache Commons IO class this was copied from), and the inherited transformations return a
    //        plain non-closeable ObjIterator - so stream() is the only self-owning path. These tests
    //        pin the documented behaviour so a future change to it is deliberate rather than accidental.
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("C-201: draining a LineIterator does not close it; only close()/stream() release the reader")
    public void testLineIteratorDrainingDoesNotClose(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("drain.txt");
        Files.write(file, Arrays.asList("l1", "l2", "l3"), StandardCharsets.UTF_8);

        final LineIterator drained = LineIterator.of(file.toFile(), StandardCharsets.UTF_8);
        assertEquals(Arrays.asList("l1", "l2", "l3"), drained.toList());
        assertFalse(drained.hasNext(), "the iterator is finished ...");

        // ... but the reader is still open: the file cannot be deleted on a platform that locks handles.
        // Guard on the platform so the assertion means something everywhere.
        if (isWindows()) {
            assertFalse(file.toFile().delete(), "draining must not release the reader (documented behaviour)");
        }

        drained.close();
        assertTrue(file.toFile().delete(), "close() must release the reader");
    }

    @Test
    @DisplayName("C-202: transformations yield a plain ObjIterator; stream() is the self-owning path")
    public void testLineIteratorTransformationsAreNotCloseable(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("transform.txt");
        Files.write(file, Arrays.asList("a1", "b2", "a3"), StandardCharsets.UTF_8);

        final LineIterator base = LineIterator.of(file.toFile(), StandardCharsets.UTF_8);
        final ObjIterator<String> filtered = base.filter(l -> l.startsWith("a"));

        assertFalse(filtered instanceof AutoCloseable, "the documented trap: the transformed view cannot be closed");
        assertEquals(Arrays.asList("a1", "a3"), filtered.toList());
        base.close();

        // skip(0) is the one transformation that returns `this`, so it stays closeable
        final LineIterator base2 = LineIterator.of(file.toFile(), StandardCharsets.UTF_8);
        assertSame(base2, base2.skip(0));
        assertTrue(base2.skip(0) instanceof AutoCloseable);
        base2.close();

        // the documented safe alternative: let the stream own the iterator
        try (Stream<String> lines = LineIterator.of(file.toFile(), StandardCharsets.UTF_8).stream()) {
            assertEquals(Arrays.asList("a1", "a3"), lines.filter(l -> l.startsWith("a")).toList());
        }

        assertTrue(file.toFile().delete(), "the stream must have released the reader");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Test
    @DisplayName("BiIterator.empty() keeps returning the shared singleton and stays inert")
    public void testEmptyStaysASingleton() {
        assertSame(BiIterator.empty(), BiIterator.of(map(4)).limit(0));
        assertSame(TriIterator.empty(), TriIterator.zip(new Integer[] { 1 }, new Integer[] { 1 }, new Integer[] { 1 }).limit(0));

        assertArrayEquals(new Object[0], BiIterator.empty().toArray());
        assertEquals(0, BiIterator.empty().map((a, b) -> a).toList().size());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().map(null));
    }
}
