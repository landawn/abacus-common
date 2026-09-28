package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Multiset;

/**
 * C-114: a close handler that fails while a terminal operation (or an argument check) is failing must not replace
 * the original failure - it is added as a suppressed exception (sequential and parallel, all stream families).
 */
@Tag("unit")
public class CloseFailurePrecedenceReview20260924bTest extends TestBase {

    private static final class Boom extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Boom(final String message) {
            super(message);
        }
    }

    private static void assertPrimaryWins(final Supplier<? extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> source, final Consumer<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> terminal,
            final String label) {
        final Boom closeFailure = new Boom("closeX");
        final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> s = source.get().onClose(() -> {
            throw closeFailure;
        });

        final Boom thrown = assertThrows(Boom.class, () -> terminal.accept(s), label);
        assertEquals("up boom", thrown.getMessage(), label);
        assertEquals(1, thrown.getSuppressed().length, label);
        assertSame(closeFailure, thrown.getSuppressed()[0], label);
    }

    private static Integer failOn2(final Integer x) {
        if (x == 2) {
            throw new Boom("up boom");
        }
        return x;
    }

    @Test
    public void objectStreamTerminalsKeepThePrimaryFailure() {
        final List<Supplier<Stream<Integer>>> sources = new ArrayList<>();
        sources.add(() -> Stream.of(1, 2, 3).map(CloseFailurePrecedenceReview20260924bTest::failOn2));
        sources.add(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()).map(CloseFailurePrecedenceReview20260924bTest::failOn2));
        sources.add(() -> Stream.of(1, 2, 3).parallel(2).map(CloseFailurePrecedenceReview20260924bTest::failOn2));

        final List<Consumer<Stream<Integer>>> terminals = List.of(Stream::toList, Stream::count, s -> s.toArray(), s -> s.forEach(Fn.emptyConsumer()),
                s -> s.reduce(Integer::sum), s -> s.toSet(), s -> s.toMap(x -> x, x -> x), s -> s.min(Integer::compare), s -> s.last(),
                s -> s.sorted().toList(), s -> s.join(","), s -> s.groupTo(x -> x % 2));

        int i = 0;
        for (final Supplier<Stream<Integer>> src : sources) {
            int j = 0;
            for (final Consumer<Stream<Integer>> t : terminals) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Consumer<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> terminal = (Consumer) t;
                assertPrimaryWins(src, terminal, "source " + i + " terminal " + j);
                j++;
            }
            i++;
        }
    }

    @Test
    public void primitiveStreamTerminalsKeepThePrimaryFailure() {
        final List<Supplier<IntStream>> sources = List.of(() -> IntStream.of(1, 2, 3).map(x -> failOn2(x)),
                () -> IntStream.of(com.landawn.abacus.util.IntIterator.of(1, 2, 3)).map(x -> failOn2(x)),
                () -> IntStream.of(1, 2, 3).parallel(2).map(x -> failOn2(x)));

        final List<Consumer<IntStream>> terminals = List.of(IntStream::toArray, IntStream::count, IntStream::sum, s -> s.toList(), s -> s.max(),
                s -> s.forEach(x -> {
                }), s -> s.boxed().toList());

        int i = 0;
        for (final Supplier<IntStream> src : sources) {
            int j = 0;
            for (final Consumer<IntStream> t : terminals) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Consumer<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> terminal = (Consumer) t;
                assertPrimaryWins(src, terminal, "int source " + i + " terminal " + j);
                j++;
            }
            i++;
        }

        assertPrimaryWins(() -> DoubleStream.of(1, 2, 3).map(x -> failOn2((int) x)), s -> ((DoubleStream) s).sum(), "double sum");
        assertPrimaryWins(() -> CharStream.of('a', 'b').map(c -> {
            throw new Boom("up boom");
        }), s -> ((CharStream) s).toArray(), "char toArray");
    }

    @Test
    public void entryStreamTerminalKeepsThePrimaryFailure() {
        assertPrimaryWins(() -> EntryStream.of("a", 1, "b", 2).mapValue(v -> failOn2(v)), s -> ((EntryStream<?, ?>) s).toMap(), "entry toMap");
    }

    @Test
    public void argumentCheckFailureKeepsThePrimaryFailure() {
        final Boom closeFailure = new Boom("closeX");
        final Stream<Integer> s = Stream.of(1, 2, 3).onClose(() -> {
            throw closeFailure;
        });

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> s.toCollection(null));
        assertEquals(1, e.getSuppressed().length);
        assertSame(closeFailure, e.getSuppressed()[0]);
    }

    @Test
    public void successfulTerminalStillReportsTheCloseFailure() {
        final Stream<Integer> s = Stream.of(1, 2, 3).onClose(() -> {
            throw new Boom("closeX");
        });

        final Boom e = assertThrows(Boom.class, s::toList);
        assertEquals("closeX", e.getMessage());
    }

    @Test
    public void closeHandlersRunExactlyOnceOnFailure() {
        final int[] runs = new int[1];
        final Stream<Integer> s = Stream.of(1, 2, 3).map(CloseFailurePrecedenceReview20260924bTest::failOn2).onClose(() -> runs[0]++);

        assertThrows(Boom.class, s::toList);
        assertEquals(1, runs[0]);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Cycle 3 (C-119): sources whose hasNext() fails (filter predicate, raw iterator), not only next(), and the
    // parallel min/max sites, whose early-exit try used to be guarded only by `finally { if (isDone) close(); }`.
    // ---------------------------------------------------------------------------------------------------------

    private static boolean failOn2Test(final Integer x) {
        return failOn2(x) > 0;
    }

    /**
     * An iterator whose hasNext() itself fails (once) after the first element has been returned, and reports
     * exhaustion afterwards - so that parallel workers do not each record a distinct failure.
     */
    private static com.landawn.abacus.util.ObjIterator<Integer> hasNextFailsAfterFirst() {
        return hasNextFailsAfter(1);
    }

    /** As {@link #hasNextFailsAfterFirst()}, but hasNext() fails after {@code n} elements (0: on the very first call). */
    private static com.landawn.abacus.util.ObjIterator<Integer> hasNextFailsAfter(final int n) {
        return new com.landawn.abacus.util.ObjIterator<>() {
            private int returned = 0;
            private boolean failed = false;

            @Override
            public synchronized boolean hasNext() {
                if (returned >= n) {
                    if (failed) {
                        return false;
                    }

                    failed = true;
                    throw new Boom("up boom");
                }
                return true;
            }

            @Override
            public synchronized Integer next() {
                return ++returned;
            }
        };
    }

    @Test
    public void hasNextFailuresKeepThePrimaryFailure() {
        final List<Supplier<Stream<Integer>>> sources = new ArrayList<>();
        sources.add(() -> Stream.of(1, 2, 3).filter(CloseFailurePrecedenceReview20260924bTest::failOn2Test));
        sources.add(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()).filter(CloseFailurePrecedenceReview20260924bTest::failOn2Test));
        sources.add(() -> Stream.of(hasNextFailsAfterFirst()));
        sources.add(() -> Stream.of(1, 2, 3).parallel(2).filter(CloseFailurePrecedenceReview20260924bTest::failOn2Test));
        sources.add(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()).parallel(2).filter(CloseFailurePrecedenceReview20260924bTest::failOn2Test));
        sources.add(() -> Stream.of(hasNextFailsAfterFirst()).parallel(2));
        // The FIRST hasNext() fails: this is the call made by the parallel min/max early-exit check itself.
        sources.add(() -> Stream.of(1, 2, 3).parallel(2).filter(x -> failOn2(x + 1) > 0));
        sources.add(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()).parallel(2).filter(x -> failOn2(x + 1) > 0));
        sources.add(() -> Stream.of(hasNextFailsAfter(0)).parallel(2));
        sources.add(() -> Stream.of(hasNextFailsAfter(0)));

        final List<Consumer<Stream<Integer>>> terminals = List.of(Stream::toList, Stream::count, s -> s.toArray(), s -> s.forEach(Fn.emptyConsumer()),
                s -> s.reduce(Integer::sum), s -> s.min(Integer::compare), s -> s.max(Integer::compare), s -> s.min(Comparator.naturalOrder()),
                s -> s.max(Comparator.naturalOrder()), s -> s.last(), s -> s.sorted().toList(), s -> s.join(","), s -> s.anyMatch(x -> x > 5),
                s -> s.toMap(x -> x, x -> x));

        int i = 0;
        for (final Supplier<Stream<Integer>> src : sources) {
            int j = 0;
            for (final Consumer<Stream<Integer>> t : terminals) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Consumer<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> terminal = (Consumer) t;
                assertPrimaryWins(src, terminal, "hasNext source " + i + " terminal " + j);
                j++;
            }
            i++;
        }
    }

    @Test
    public void primitiveHasNextFailuresKeepThePrimaryFailure() {
        final List<Supplier<IntStream>> sources = List.of(() -> IntStream.of(1, 2, 3).filter(x -> failOn2(x) > 0),
                () -> IntStream.of(1, 2, 3).parallel(2).filter(x -> failOn2(x) > 0));
        final List<Consumer<IntStream>> terminals = List.of(IntStream::toArray, IntStream::count, IntStream::sum, s -> s.min(), s -> s.max(),
                s -> s.boxed().max(Integer::compare));

        int i = 0;
        for (final Supplier<IntStream> src : sources) {
            int j = 0;
            for (final Consumer<IntStream> t : terminals) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Consumer<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> terminal = (Consumer) t;
                assertPrimaryWins(src, terminal, "int hasNext source " + i + " terminal " + j);
                j++;
            }
            i++;
        }
    }

    private static <X extends Throwable> void assertFailureWinsOverClose(final Class<X> expected, final Stream<?> s,
            final Consumer<Stream<?>> terminal, final String label) {
        final Boom closeFailure = new Boom("closeX");
        final int[] closes = new int[1];
        s.onClose(() -> {
            closes[0]++;
            throw closeFailure;
        });

        final X thrown = assertThrows(expected, () -> terminal.accept(s), label);
        assertEquals(1, thrown.getSuppressed().length, label);
        assertSame(closeFailure, thrown.getSuppressed()[0], label);
        assertEquals(1, closes[0], label);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void parallelMinMaxSortedShortcutFailuresKeepThePrimaryFailure() {
        final Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        final Comparator<String> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());

        // ParallelArrayStream: the sorted shortcut selects a null element -> the documented NPE of Optional.of.
        Stream<String> s = new ArrayStream<>(new String[] { null, "a", "b" }, true, nullsFirst, null).parallel(2);
        assertTrue(s instanceof ParallelArrayStream, s.getClass().getName());
        assertFailureWinsOverClose(NullPointerException.class, s, x -> ((Stream<String>) x).min(nullsFirst), "parallel array min");

        s = new ArrayStream<>(new String[] { "a", "b", null }, true, nullsLast, null).parallel(2);
        assertFailureWinsOverClose(NullPointerException.class, s, x -> ((Stream<String>) x).max(nullsLast), "parallel array max");

        // ParallelArrayStream.max tie-walk: a throwing comparator.
        final Comparator<Integer> throwing = (a, b) -> {
            throw new Boom("cmp boom");
        };
        final Stream<Integer> ints = new ArrayStream<>(new Integer[] { 1, 2 }, true, throwing, null).parallel(2);
        assertFailureWinsOverClose(Boom.class, ints, x -> ((Stream<Integer>) x).max(throwing), "parallel array max tie-walk");

        // ParallelIteratorStream: sorted shortcut of min.
        s = new IteratorStream<>(Arrays.asList(null, "a", "b").iterator(), true, nullsFirst, null).parallel(2);
        assertTrue(s instanceof ParallelIteratorStream, s.getClass().getName());
        assertFailureWinsOverClose(NullPointerException.class, s, x -> ((Stream<String>) x).min(nullsFirst), "parallel iterator min");

        // Sequential controls (already right before cycle 3).
        s = new ArrayStream<>(new String[] { null, "a" }, true, nullsFirst, null);
        assertFailureWinsOverClose(NullPointerException.class, s, x -> ((Stream<String>) x).min(nullsFirst), "sequential array min");
    }

    @Test
    public void parallelMinMaxWithoutFailureStillCloseOnce() {
        final int[] closes = new int[1];
        assertEquals("a", Stream.of("b", "a", "c").parallel(2).onClose(() -> closes[0]++).min(Comparator.naturalOrder()).orElseThrow());
        assertEquals("c", Stream.of(Arrays.asList("b", "a", "c").iterator()).parallel(2).onClose(() -> closes[0]++).max(Comparator.naturalOrder()).orElseThrow());
        assertTrue(Stream.<String> empty().parallel(2).onClose(() -> closes[0]++).min(Comparator.naturalOrder()).isEmpty());
        assertTrue(Stream.of(java.util.Collections.<String> emptyIterator()).parallel(2).onClose(() -> closes[0]++).max(Comparator.naturalOrder()).isEmpty());
        assertEquals(4, closes[0]);
    }

    // ---------------------------------------------------------------------------------------------------------
    // U23-05 (2026-09-25): the C-114 conversion touched 27 terminals in IteratorStream and 35 in ArrayStream, but
    // the lists above sample 12 of them. The remaining converted terminals are pinned here for an iterator-backed
    // source (the failure comes from upstream) and for a genuine ArrayStream (the array itself cannot fail, so the
    // failure comes from the terminal's own callback or element - this is the only way to reach ArrayStream's own
    // catch blocks: ArrayStream.map(..) already yields an IteratorStream).
    // ---------------------------------------------------------------------------------------------------------

    private static void assertPrimaryWinsOn(final Supplier<Stream<Integer>> source, final Consumer<Stream<Integer>> terminal, final String label) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Consumer<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> t = (Consumer) terminal;
        assertPrimaryWins(source, t, label);
    }

    private static Integer failOn1(final Integer x) {
        if (x == 1) {
            throw new Boom("up boom");
        }
        return x;
    }

    private static final Comparator<Integer> THROWING_COMPARATOR = (a, b) -> {
        throw new Boom("up boom");
    };

    /**
     * A list whose add(..)/addAll(..) fail on the element 2 - the accumulation itself is the primary failure
     * (ArrayStream.toCollection uses addAll for a full array and add(..) for a short sub-range).
     */
    private static List<Integer> listFailingOn2() {
        return new ArrayList<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean add(final Integer x) {
                return super.add(failOn2(x));
            }

            @Override
            public boolean addAll(final java.util.Collection<? extends Integer> c) {
                c.forEach(CloseFailurePrecedenceReview20260924bTest::failOn2);
                return super.addAll(c);
            }
        };
    }

    /** An element whose hashCode() fails: the only way a hash-based terminal on a plain array can fail. */
    private static final class BadHash {
        @Override
        public int hashCode() {
            throw new Boom("up boom");
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this;
        }
    }

    @Test
    public void convertedIteratorTerminalsKeepThePrimaryFailure() {
        final List<Supplier<Stream<Integer>>> sources = new ArrayList<>();
        sources.add(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()).map(CloseFailurePrecedenceReview20260924bTest::failOn2));
        sources.add(() -> Stream.of(1, 2, 3).map(CloseFailurePrecedenceReview20260924bTest::failOn2));
        sources.add(() -> Stream.of(new Integer[] { 0, 1, 2, 3, 4 }, 1, 4).map(CloseFailurePrecedenceReview20260924bTest::failOn2));

        final List<Consumer<Stream<Integer>>> terminals = List.of(s -> s.forEach(Fn.emptyConsumer(), () -> {
        }), s -> s.forEach(x -> Arrays.asList(x, x), (x, y) -> {
        }), s -> s.forEach(x -> Arrays.asList(x), y -> Arrays.asList(y), (x, y, z) -> {
        }), s -> s.forEachPair((a, b) -> {
        }), s -> s.forEachPair(2, (a, b) -> {
        }), s -> s.forEachTriple((a, b, c) -> {
        }), s -> s.forEachTriple(2, (a, b, c) -> {
        }), s -> s.toArray(Integer[]::new), s -> s.toCollection(ArrayList::new), s -> s.toMultiset(), s -> s.toMultiset(Multiset::new),
                s -> s.toMap(x -> x, x -> x, (a, b) -> a, HashMap::new), s -> s.toMultimap(x -> x, x -> x), s -> s.onlyOne(), s -> s.elementAt(2),
                s -> s.collect(ArrayList::new, List::add), s -> s.collect(ArrayList::new, List::add, List::addAll), s -> s.collect(Collectors.toList()),
                s -> s.maxAll(Integer::compare), s -> s.kthLargest(1, Integer::compare), s -> s.allMatch(x -> true), s -> s.noneMatch(x -> false),
                s -> s.hasMatchCountBetween(0, 9, x -> true), s -> s.findFirst(x -> x > 5), s -> s.findLast(x -> x > 5), s -> s.foldLeft(Integer::sum),
                s -> s.foldLeft(0, Integer::sum));

        int i = 0;
        for (final Supplier<Stream<Integer>> src : sources) {
            assertTrue(src.get() instanceof IteratorStream, "source " + i);
            int j = 0;
            for (final Consumer<Stream<Integer>> t : terminals) {
                assertPrimaryWinsOn(src, t, "iterator source " + i + " terminal " + j);
                j++;
            }
            i++;
        }

        // Terminals that stop at the first element need a source whose FIRST element fails.
        for (final Supplier<Stream<Integer>> src : List.<Supplier<Stream<Integer>>> of(
                () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).map(CloseFailurePrecedenceReview20260924bTest::failOn1),
                () -> Stream.of(1, 2, 3).map(CloseFailurePrecedenceReview20260924bTest::failOn1))) {
            assertPrimaryWinsOn(src, s -> s.first(), "iterator first");
            assertPrimaryWinsOn(src, s -> s.findFirst(x -> true), "iterator findFirst(match)");
            assertPrimaryWinsOn(src, s -> s.elementAt(0), "iterator elementAt(0)");
            assertPrimaryWinsOn(src, s -> s.onlyOne(), "iterator onlyOne");
        }

        // onComplete itself is the primary failure (the traversal succeeded).
        assertPrimaryWinsOn(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()).map(x -> x), s -> s.forEach(Fn.emptyConsumer(), () -> {
            throw new Boom("up boom");
        }), "iterator forEach onComplete");
    }

    @Test
    public void convertedArrayTerminalsKeepThePrimaryFailure() {
        final List<Supplier<Stream<Integer>>> sources = new ArrayList<>();
        sources.add(() -> Stream.of(1, 2, 3));
        sources.add(() -> Stream.of(new Integer[] { 0, 1, 2, 3, 4 }, 1, 4));

        final List<Consumer<Stream<Integer>>> terminals = List.of(s -> s.forEach(x -> failOn2(x), () -> {
        }), s -> s.forEach(Fn.emptyConsumer(), () -> {
            throw new Boom("up boom");
        }), s -> s.forEach(x -> Arrays.asList(failOn2(x)), (x, y) -> {
        }), s -> s.forEach(x -> Arrays.asList(x), y -> Arrays.asList(failOn2(y)), (x, y, z) -> {
        }), s -> s.forEachPair((a, b) -> failOn2(b)), s -> s.forEachPair(2, (a, b) -> failOn2(b)), s -> s.forEachTriple((a, b, c) -> failOn2(b)),
                s -> s.forEachTriple(2, (a, b, c) -> failOn2(b)), s -> s.toArray(n -> {
                    throw new Boom("up boom");
                }), s -> s.toCollection(CloseFailurePrecedenceReview20260924bTest::listFailingOn2),
                s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, HashMap::new), s -> s.toMultimap(x -> failOn2(x), x -> x),
                s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x))), s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), List::addAll),
                s -> s.collect(Collectors.mapping(CloseFailurePrecedenceReview20260924bTest::failOn2, Collectors.toList())), s -> s.maxAll(THROWING_COMPARATOR),
                s -> s.kthLargest(2, THROWING_COMPARATOR), s -> s.min(THROWING_COMPARATOR), s -> s.max(THROWING_COMPARATOR), s -> s.allMatch(x -> failOn2(x) > 0),
                s -> s.noneMatch(x -> failOn2(x) < 0), s -> s.anyMatch(x -> failOn2(x) > 5), s -> s.hasMatchCountBetween(0, 9, x -> failOn2(x) > 0),
                s -> s.findFirst(x -> failOn2(x) > 5), s -> s.findLast(x -> failOn2(x) > 5), s -> s.foldLeft((a, b) -> failOn2(a) + failOn2(b)),
                s -> s.foldLeft(0, (a, b) -> failOn2(a) + failOn2(b)), s -> s.foldRight((a, b) -> failOn2(a) + failOn2(b)),
                s -> s.foldRight(0, (a, b) -> failOn2(a) + failOn2(b)));

        int i = 0;
        for (final Supplier<Stream<Integer>> src : sources) {
            assertTrue(src.get() instanceof ArrayStream, "source " + i);
            int j = 0;
            for (final Consumer<Stream<Integer>> t : terminals) {
                assertPrimaryWinsOn(src, t, "array source " + i + " terminal " + j);
                j++;
            }
            i++;
        }
    }

    @Test
    public void convertedArrayTerminalsWithoutCallbacksKeepThePrimaryFailure() {
        // toMultiset(): the element's hashCode() fails.
        for (int variant = 0; variant < 2; variant++) {
            final Boom closeFailure = new Boom("closeX");
            final Stream<BadHash> s = Stream.of(new BadHash(), new BadHash()).onClose(() -> {
                throw closeFailure;
            });
            assertTrue(s instanceof ArrayStream);
            final Boom thrown = variant == 0 ? assertThrows(Boom.class, s::toMultiset) : assertThrows(Boom.class, () -> s.toMultiset(Multiset::new));
            assertEquals("up boom", thrown.getMessage());
            assertEquals(1, thrown.getSuppressed().length);
            assertSame(closeFailure, thrown.getSuppressed()[0]);
        }

        // first()/last()/elementAt()/onlyOne(): the documented NPE of Optional.of on a null element.
        assertFailureWinsOverClose(NullPointerException.class, Stream.of((Integer) null, 1), s -> ((Stream<Integer>) s).first(), "array first null");
        assertFailureWinsOverClose(NullPointerException.class, Stream.of(1, (Integer) null), s -> ((Stream<Integer>) s).last(), "array last null");
        assertFailureWinsOverClose(NullPointerException.class, Stream.of(1, (Integer) null), s -> ((Stream<Integer>) s).elementAt(1), "array elementAt null");
        assertFailureWinsOverClose(NullPointerException.class, Stream.of((Integer) null), s -> ((Stream<Integer>) s).onlyOne(), "array onlyOne null");
        assertFailureWinsOverClose(NullPointerException.class, Stream.of(new Integer[] { 0, null, 2 }, 1, 2), s -> ((Stream<Integer>) s).first(),
                "array sub-range first null");

        // onlyOne(): TooManyElementsException is raised inside the terminal.
        assertFailureWinsOverClose(TooManyElementsException.class, Stream.of(1, 2), s -> ((Stream<Integer>) s).onlyOne(), "array onlyOne too many");
        assertFailureWinsOverClose(TooManyElementsException.class, Stream.of(Arrays.asList(1, 2).iterator()), s -> ((Stream<Integer>) s).onlyOne(),
                "iterator onlyOne too many");

        // toArray(A[]) (package-private, ArrayStream only) and ArrayStream.toArray(generator): the ArrayStoreException
        // of the copy. (The iterator-backed AbstractStream.toArray(generator) is deliberately NOT pinned here: it runs
        // the closing terminal toArray() before the generator is called, so a failing close handler pre-empts the
        // generator/copy failure there - see the F10b fix report.)
        assertFailureWinsOverClose(ArrayStoreException.class, Stream.of(1, 2, 3), s -> ((ArrayStream<Integer>) s).toArray(new String[3]), "array toArray(A[])");
        assertFailureWinsOverClose(ArrayStoreException.class, Stream.of(new Integer[] { 0, 1, 2, 3 }, 1, 3), s -> ((ArrayStream<Integer>) s).toArray(new String[2]),
                "array sub-range toArray(A[])");
        assertFailureWinsOverClose(ArrayStoreException.class, Stream.of(1, 2, 3), s -> ((Stream<Integer>) s).toArray(String[]::new), "array toArray(generator)");
        assertFailureWinsOverClose(NullPointerException.class, Stream.of(1, 2, 3), s -> ((Stream<Integer>) s).toArray(n -> null), "array toArray(null generator result)");
        assertFailureWinsOverClose(NullPointerException.class, Stream.of(new Integer[0]), s -> ((Stream<Integer>) s).toArray(n -> null),
                "empty array toArray(null generator result)");
    }
}
