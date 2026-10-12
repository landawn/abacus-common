package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Seq. */
@Tag("slow-test")
public class SeqPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G061 end ----
    // ---- perf review 2026-10-08 G062 begin ----
    @Nested
    class G062 {
        private WeakReference<Object> g062Payload;

        private boolean g062AwaitCleared(final WeakReference<?> ref) {
            for (int i = 0; i < 50 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        // Built in a separate frame so that only the returned sequence references the payload array.
        private Seq<Integer, RuntimeException> g062ConcatOverPrivateArray(final int n) {
            final Integer[] payload = new Integer[n];

            for (int i = 0; i < n; i++) {
                payload[i] = i;
            }

            g062Payload = new WeakReference<>(payload);

            return Seq.concat(Seq.<Integer, RuntimeException> of(payload), null, Seq.<Integer, RuntimeException> of(-1, -2));
        }

        // Built in a separate frame so that only the returned sequence references the elements.
        private Seq<String, RuntimeException> g062SortedThenAppend(final int n) {
            final List<String> values = new ArrayList<>(n);

            for (int i = n - 1; i >= 0; i--) {
                values.add(String.valueOf(1_000_000 + i));
            }

            g062Payload = new WeakReference<>(values.get(n / 2));

            return Seq.<String, RuntimeException> of(values).sorted().append(Seq.<String, RuntimeException> of("tail1", "tail2"));
        }

        // G062-01: an exhausted (and closed) input of concat(..) is no longer reachable from the concatenation while the later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final Seq<Integer, RuntimeException> seq = g062ConcatOverPrivateArray(100_000);
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Integer> tail = new ArrayList<>();

            seq.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = g062AwaitCleared(g062Payload);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(4_999_950_000L, sum[0]);
            assertEquals(Arrays.asList(-1, -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G062-01: append(..) delegates to concat(this, s): the exhausted receiver (here a sorted() buffer) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedSequenceRuns() {
            final Seq<String, RuntimeException> seq = g062SortedThenAppend(20_000);
            final boolean[] released = { false };
            final List<String> seen = new ArrayList<>();

            seq.forEach(x -> {
                if (x.equals("tail1")) {
                    released[0] = g062AwaitCleared(g062Payload);
                }

                if (x.startsWith("tail") || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList("1000000", "1000001", "1000002", "tail1", "tail2"), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its buffer) is still reachable from the appended sequence");
        }

        // G062-01: order, null inputs, close order (each exhausted input before the next is activated, the rest on close)
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, RuntimeException> a = Seq.<Integer, RuntimeException> of(1, 2).onClose(() -> log.add("closeA"));
            final Seq<Integer, RuntimeException> b = Seq.<Integer, RuntimeException> of(3).onClose(() -> log.add("closeB"));
            final Seq<Integer, RuntimeException> c = Seq.<Integer, RuntimeException> of(4, 5).onClose(() -> log.add("closeC"));
            final Seq<Integer, RuntimeException> d = Seq.<Integer, RuntimeException> of(6).onClose(() -> log.add("closeD"));
            final List<Seq<Integer, RuntimeException>> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final Seq<Integer, RuntimeException> seq = Seq.concat(input);
            final Throwables.Iterator<Integer, RuntimeException> iter = seq.iteratorEx();
            final List<Integer> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final Integer x = iter.next();
                seen.add(x);
                log.add("next" + x);
            }

            seq.close();

            assertEquals(Arrays.asList(1, 2, 3, 4), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            // only the private snapshot is touched: the caller's collection keeps its elements
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toList);
            assertThrows(IllegalStateException.class, d::toList);

            // varargs: the caller's array is not modified either
            final Seq<Integer, RuntimeException> e = Seq.of(7, 8);
            final Seq<Integer, RuntimeException> f = Seq.of(9);
            @SuppressWarnings("unchecked")
            final Seq<Integer, RuntimeException>[] array = new Seq[] { e, null, f };
            assertEquals(Arrays.asList(7, 8, 9), Seq.concat(array).toList());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(Collections.emptyList(), Seq.concat(Arrays.<Seq<Integer, RuntimeException>> asList(null, null)).toList());
            assertEquals(Arrays.asList(1, 2, 3, 4), Seq.<Integer, RuntimeException> of(1, 2).append(Seq.<Integer, RuntimeException> of(3, 4)).toList());
            assertEquals(Arrays.asList(3, 4, 1, 2), Seq.<Integer, RuntimeException> of(1, 2).prepend(Seq.<Integer, RuntimeException> of(3, 4)).toList());
            assertEquals(5L,
                    Seq.concat(Seq.<Integer, RuntimeException> of(1, 2), Seq.<Integer, RuntimeException> empty(), Seq.<Integer, RuntimeException> of(3, 4, 5))
                            .count());
        }

        // G062-01: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, RuntimeException> a = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final Seq<Integer, RuntimeException> closed = Seq.of(99);
            closed.close();
            final Seq<Integer, RuntimeException> c = Seq.<Integer, RuntimeException> of(3).onClose(() -> log.add("closeC"));
            final Seq<Integer, RuntimeException> seq = Seq.concat(Arrays.asList(a, closed, c));
            final Throwables.Iterator<Integer, RuntimeException> iter = seq.iteratorEx();

            assertEquals(1, iter.next());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already, the next input is closed too and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This sequence is already closed.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3, iter.next());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::next);

            seq.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same sequence twice: the second occurrence is rejected, as before
            final Seq<Integer, RuntimeException> twice = Seq.of(1, 2);
            final List<Integer> collected = new ArrayList<>();
            assertThrows(IllegalStateException.class, () -> Seq.concat(twice, twice).forEach(collected::add));
            assertEquals(Arrays.asList(1, 2), collected);

            // a terminal operation over a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final Seq<Integer, RuntimeException> x = Seq.<Integer, RuntimeException> of(1).onClose(() -> log2.add("closeX"));
            final Seq<Integer, RuntimeException> closed2 = Seq.of(2);
            closed2.close();
            final Seq<Integer, RuntimeException> z = Seq.<Integer, RuntimeException> of(3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> Seq.concat(x, closed2, z).toList());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);
        }
    }

    // ---- perf review 2026-10-08 G062 end ----
    // ---- perf review 2026-10-08 G064 begin ----
    @Nested
    class G064 {

        /**
         * A source whose resource release is observable: logs "name" when its resource is closed and optionally fails then.
         */
        private Throwables.Iterator<Integer, Exception> source(final String name, final List<String> log, final int size, final boolean failOnClose) {
            return new Throwables.Iterator<>() {
                private int next = 1;

                @Override
                public boolean hasNext() {
                    return next <= size;
                }

                @Override
                public Integer next() {
                    if (next > size) {
                        throw new java.util.NoSuchElementException();
                    }

                    return next++;
                }

                @Override
                protected void closeResourceInternal() {
                    log.add(name);

                    if (failOnClose) {
                        throw new IllegalStateException(name + " failed");
                    }
                }
            };
        }

        // G064-01: the single-handler deque of a created sequence still runs its resource handler once, before onClose handlers
        @Test
        void testCreate_resourceHandlerRunsOnceBeforeOnCloseHandlers() throws Exception {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.of(source("resource", log, 3, false)).onClose(() -> log.add("h1")).onClose(() -> log.add("h2"));

            assertEquals(Arrays.asList(1, 2, 3), seq.toList());
            assertEquals(Arrays.asList("resource", "h1", "h2"), log);

            seq.close();
            assertEquals(Arrays.asList("resource", "h1", "h2"), log);

            final List<String> log2 = new ArrayList<>();
            final Seq<Integer, Exception> plain = Seq.of(source("resource", log2, 0, false));
            assertEquals(0, plain.count());
            assertEquals(Collections.singletonList("resource"), log2);
            plain.close();
            assertEquals(Collections.singletonList("resource"), log2);
        }

        // G064-01: explicit close before traversal releases the resource once and the sequence then rejects reuse
        @Test
        void testCreate_explicitCloseBeforeTraversal() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.of(source("resource", log, 3, false));

            seq.close();
            seq.close();
            assertEquals(Collections.singletonList("resource"), log);
            assertThrows(IllegalStateException.class, seq::toList);
            assertThrows(IllegalStateException.class, () -> seq.onClose(() -> log.add("late")));
            assertEquals(Collections.singletonList("resource"), log);
        }

        // G064-01: a derived pipeline closes the created source exactly once, ahead of the handlers registered downstream
        @Test
        void testCreate_derivedPipelineClosesSourceOnce() throws Exception {
            final List<String> log = new ArrayList<>();
            final List<Integer> result = Seq.of(source("resource", log, 6, false))
                    .onClose(() -> log.add("h0"))
                    .filter(i -> i % 2 == 0)
                    .onClose(() -> log.add("h1"))
                    .map(i -> i * 10)
                    .onClose(() -> log.add("h2"))
                    .toList();

            assertEquals(Arrays.asList(20, 40, 60), result);
            assertEquals(Arrays.asList("resource", "h0", "h1", "h2"), log);
        }

        // G064-01: every inner sequence created per element by flatMap is closed exactly once, in encounter order
        @Test
        void testCreate_flatMapInnerSequencesEachClosedOnce() throws Exception {
            final List<String> log = new ArrayList<>();
            final AtomicInteger onCloseCount = new AtomicInteger();
            final List<Integer> result = Seq.<Integer, Exception> of(1, 2, 3, 4)
                    .flatMap(i -> Seq.of(source("inner" + i, log, i, false)).onClose(onCloseCount::incrementAndGet))
                    .toList();

            assertEquals(Arrays.asList(1, 1, 2, 1, 2, 3, 1, 2, 3, 4), result);
            assertEquals(Arrays.asList("inner1", "inner2", "inner3", "inner4"), log);
            assertEquals(4, onCloseCount.get());

            final List<String> log2 = new ArrayList<>();
            assertEquals(2, Seq.<Integer, Exception> of(1, 2, 3).flatMap(i -> Seq.of(source("x" + i, log2, 5, false))).limit(2).count());
            assertEquals(Collections.singletonList("x1"), log2);
        }

        // G064-01: a failing resource release still runs the later handlers and propagates the same exception
        @Test
        void testCreate_failingResourceCloseStillRunsLaterHandlers() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.of(source("resource", log, 2, true)).onClose(() -> log.add("h1"));

            final IllegalStateException e = assertThrows(IllegalStateException.class, seq::toList);
            assertEquals("resource failed", e.getMessage());
            assertEquals(Arrays.asList("resource", "h1"), log);

            seq.close();
            assertEquals(Arrays.asList("resource", "h1"), log);
        }

        // G064-01: a sequence rebuilt from a stream (create(Stream) -> create(iterator)) closes that stream once
        @Test
        void testCreate_streamBackedSequenceClosesItsStreamOnce() throws Exception {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2, 3)
                    .onClose(() -> log.add("source"))
                    .transformViaStream(st -> st.map(i -> i + 1).onClose(() -> log.add("stream")))
                    .onClose(() -> log.add("h1"));

            assertEquals(Arrays.asList(2, 3, 4), seq.toList());
            assertEquals(Arrays.asList("source", "stream", "h1"), log);

            seq.close();
            assertEquals(Arrays.asList("source", "stream", "h1"), log);
        }
    }

    // ---- perf review 2026-10-08 G120 end ----
    // ---- perf review 2026-10-08 G122 begin ----
    @Nested
    class G122 {
        private final class Item implements Comparable<Item> {
            private final int key;
            private final int id;

            Item(final int key, final int id) {
                this.key = key;
                this.id = id;
            }

            @Override
            public int compareTo(final Item other) {
                return Integer.compare(key, other.key);
            }

            @Override
            public String toString() {
                return key + "#" + id;
            }
        }

        private final class ProbeException extends Exception {
            private static final long serialVersionUID = 1L;

            ProbeException(final String message) {
                super(message);
            }
        }

        private List<Item> items(final int size, final long seed, final int keyRange) {
            final Random random = new Random(seed);
            final List<Item> list = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                list.add(new Item(random.nextInt(keyRange), i));
            }

            return list;
        }

        private Map<Percentage, Item> stableOracle(final List<Item> list, final java.util.Comparator<? super Item> comparator) {
            final Item[] sorted = list.toArray(new Item[0]);
            Arrays.sort(sorted, comparator); // stable, as N.sort is
            return N.percentilesOfSorted(sorted);
        }

        private void assertSameElements(final Map<Percentage, Item> expected, final Map<Percentage, Item> actual) {
            assertEquals(expected.keySet(), actual.keySet());

            for (final Map.Entry<Percentage, Item> e : expected.entrySet()) {
                assertSame(e.getValue(), actual.get(e.getKey()), String.valueOf(e.getKey()));
            }
        }

        // G122-01: percentiles()/percentiles(Comparator) pick the same elements as a stable sort of the remaining elements, for
        // every source shape (array, iterator, pipeline stages, already sorted by the same or by another comparator), with ties.
        @Test
        void testPercentiles_matchesStableSortOracleForEverySourceShape() throws Exception {
            final java.util.Comparator<Item> natural = java.util.Comparator.naturalOrder();
            final java.util.Comparator<Item> reverse = java.util.Comparator.reverseOrder();

            for (final int size : new int[] { 1, 2, 3, 10, 99, 1000 }) {
                for (final int keyRange : new int[] { 1, 3, 1_000_000 }) {
                    final List<Item> list = items(size, size * 31L + keyRange, keyRange);
                    final Item[] array = list.toArray(new Item[0]);

                    assertSameElements(stableOracle(list, natural), Seq.<Item, Exception> of(array).percentiles().get());
                    assertSameElements(stableOracle(list, natural), Seq.<Item, Exception> of(list.iterator()).percentiles().get());
                    assertSameElements(stableOracle(list, reverse), Seq.<Item, Exception> of(array).percentiles(reverse).get());
                    assertSameElements(stableOracle(list, reverse), Seq.<Item, Exception> of(list).map(e -> e).percentiles(reverse).get());
                    // already sorted by the same comparator: the shortcut keeps the sorted order without a second sort
                    assertSameElements(stableOracle(list, natural), Seq.<Item, Exception> of(array).sorted().percentiles().get());
                    assertSameElements(stableOracle(list, reverse), Seq.<Item, Exception> of(array).sorted(reverse).percentiles(reverse).get());
                    // already sorted by another comparator: sorted again, stably, from that order
                    final List<Item> reversedFirst = new ArrayList<>(list);
                    reversedFirst.sort(reverse);
                    assertSameElements(stableOracle(reversedFirst, natural), Seq.<Item, Exception> of(array).sorted(reverse).percentiles().get());

                    final List<Item> filtered = new ArrayList<>();
                    for (final Item e : list) {
                        if (e.id % 3 != 1) {
                            filtered.add(e);
                        }
                    }

                    if (!filtered.isEmpty()) {
                        assertSameElements(stableOracle(filtered, natural), Seq.<Item, Exception> of(array).filter(e -> e.id % 3 != 1).percentiles().get());
                    }
                }
            }

            // nulls: percentiles() orders them first; the caller's array is never reordered
            final Item[] withNulls = { new Item(5, 0), null, new Item(1, 2), null, new Item(3, 4) };
            final Item[] copy = withNulls.clone();
            final Map<Percentage, Item> result = Seq.<Item, Exception> of(withNulls).percentiles().get();
            assertNull(result.get(Percentage._0_0001));
            assertSame(withNulls[0], result.get(Percentage._99_9999));
            assertArrayEquals(copy, withNulls);
            assertTrue(Seq.<Item, Exception> empty().percentiles().isEmpty());
            assertTrue(Seq.<Item, Exception> of(new Item[0]).percentiles(reverse).isEmpty());
        }

        // G122-01: the source is closed exactly once, before the first comparison, and the sequence is unusable afterwards.
        @Test
        void testPercentiles_closesTheSourceOnceBeforeTheFirstComparison() throws Exception {
            final List<String> log = new ArrayList<>();
            final java.util.Comparator<Item> logging = (a, b) -> {
                log.add("compare");
                return Integer.compare(a.key, b.key);
            };
            final Seq<Item, Exception> seq = Seq.<Item, Exception> of(items(50, 7, 10)).onClose(() -> log.add("close1")).onClose(() -> log.add("close2"));

            assertTrue(seq.percentiles(logging).isPresent());
            assertEquals(List.of("close1", "close2"), log.subList(0, 2));
            assertEquals(log.size() - 2, Collections.frequency(log, "compare"));
            assertTrue(log.size() > 2);
            assertThrows(IllegalStateException.class, seq::count);

            log.clear();
            final Seq<Item, Exception> natural = Seq.<Item, Exception> of(items(3, 8, 10)).onClose(() -> log.add("close"));
            assertTrue(natural.percentiles().isPresent());
            assertEquals(List.of("close"), log);
            assertThrows(IllegalStateException.class, natural::percentiles);
        }

        // G122-01: on an already sorted sequence nothing is compared, and the inherited close handlers still run (once) while the
        // sorted sequence itself is not yet marked closed - exactly as sorted(comparator).toArray() does; draining and closing it
        // directly (the stream-side shape) would let such a handler see the sequence closed.
        @Test
        void testPercentiles_alreadySortedShortcutKeepsTheCloseOrder() throws Exception {
            final List<String> log = new ArrayList<>();
            final java.util.Comparator<Item> logging = (a, b) -> {
                log.add("compare");
                return Integer.compare(a.key, b.key);
            };
            final Seq<Item, RuntimeException> sorted = Seq.<Item, RuntimeException> of(items(20, 9, 5)).sorted(logging);
            assertTrue(log.isEmpty()); // sorted(..) is lazy

            final Seq<Item, RuntimeException> withHandler = sorted.onClose(() -> {
                try {
                    log.add("reentrant count=" + sorted.count());
                } catch (final IllegalStateException e) {
                    log.add("reentrant closed");
                }
            });
            final Map<Percentage, Item> result = withHandler.percentiles(logging).get();
            final int compares = Collections.frequency(log, "compare");
            assertTrue(compares > 0); // the sort of sorted(logging) itself
            assertEquals(List.of("reentrant count=0"), log.subList(compares, log.size()));
            assertEquals(Collections.nCopies(compares, "compare"), log.subList(0, compares));
            assertNotNull(result);
            assertThrows(IllegalStateException.class, withHandler::count);

            log.clear();
            final Seq<Item, RuntimeException> unsorted = Seq.<Item, RuntimeException> of(items(20, 9, 5));
            final Seq<Item, RuntimeException> withHandler2 = unsorted.onClose(() -> {
                try {
                    log.add("reentrant count=" + unsorted.count());
                } catch (final IllegalStateException e) {
                    log.add("reentrant closed");
                }
            });
            withHandler2.percentiles(logging);
            assertEquals("reentrant closed", log.get(0)); // closed before the first comparison
            assertEquals(log.size() - 1, Collections.frequency(log, "compare"));
        }

        // G122-01: failures keep their order: a source failure wins (a failing close handler is suppressed into it, once); a
        // comparator failure comes after the close; a failing close handler stops the operation before any comparison.
        @Test
        void testPercentiles_failuresKeepTheirOrderAndCloseOnce() {
            final AtomicInteger closes = new AtomicInteger();
            final AtomicInteger compares = new AtomicInteger();
            final java.util.Comparator<Item> counting = (a, b) -> {
                compares.incrementAndGet();
                return Integer.compare(a.key, b.key);
            };

            final Seq<Item, Exception> failingSource = Seq.<Item, Exception> of(items(10, 1, 10)).map(e -> {
                if (e.id == 4) {
                    throw new ProbeException("source fails at 4");
                }
                return e;
            }).onClose(() -> {
                closes.incrementAndGet();
                throw new IllegalStateException("close fails");
            });
            final ProbeException sourceFailure = assertThrows(ProbeException.class, () -> failingSource.percentiles(counting));
            assertEquals("source fails at 4", sourceFailure.getMessage());
            assertEquals(1, sourceFailure.getSuppressed().length);
            assertEquals("close fails", sourceFailure.getSuppressed()[0].getMessage());
            assertEquals(1, closes.get());
            assertEquals(0, compares.get());

            closes.set(0);
            final java.util.Comparator<Item> failing = (a, b) -> {
                compares.incrementAndGet();
                if (compares.get() == 3) {
                    throw new UnsupportedOperationException("compare fails at 3");
                }
                return Integer.compare(a.key, b.key);
            };
            compares.set(0);
            final Seq<Item, Exception> seq = Seq.<Item, Exception> of(items(10, 2, 10)).onClose(closes::incrementAndGet);
            final UnsupportedOperationException compareFailure = assertThrows(UnsupportedOperationException.class, () -> seq.percentiles(failing));
            assertEquals("compare fails at 3", compareFailure.getMessage());
            assertEquals(0, compareFailure.getSuppressed().length);
            assertEquals(1, closes.get());
            assertEquals(3, compares.get());

            compares.set(0);
            final Seq<Item, Exception> failingClose = Seq.<Item, Exception> of(items(10, 3, 10)).onClose(() -> {
                throw new IllegalStateException("close fails");
            });
            assertEquals("close fails", assertThrows(IllegalStateException.class, () -> failingClose.percentiles(counting)).getMessage());
            assertEquals(0, compares.get());
            assertThrows(IllegalStateException.class, failingClose::count);

            final Seq<Item, Exception> closed = Seq.<Item, Exception> of(items(3, 4, 10));
            closed.close();
            assertThrows(IllegalStateException.class, closed::percentiles);
            assertThrows(IllegalArgumentException.class, () -> Seq.<Item, Exception> of(items(3, 5, 10)).percentiles(null));
        }

        /** A HashSet subclass: SetMultimap copies it through the value supplier. */
        private final class SupplierCopiedSet<T> extends HashSet<T> {
            private static final long serialVersionUID = 1L;
        }

        /** An element whose hashCode can run an action once, at a given call. */
        private final class HookElement {
            private final String name;
            private Runnable hook;

            HookElement(final String name) {
                this.name = name;
            }

            @Override
            public int hashCode() {
                if (hook != null) {
                    final Runnable action = hook;
                    hook = null;
                    action.run();
                }
                return name.hashCode();
            }

            @Override
            public boolean equals(final Object o) {
                return o instanceof HookElement && ((HookElement) o).name.equals(name);
            }

            @Override
            public String toString() {
                return name;
            }
        }

        // G122-02: SetMultimap value sets that are copied with their own constructors (HashSet, LinkedHashSet, sorted sets,
        // IdentityHashSet) never reach the value supplier; every copy() / toMap() / toMap(IntFunction) result is equal to the
        // source, keeps each set's type, order and membership policy, and shares no set with the source or another key.
        @Test
        void testCopy_constructorCopiedValueSetsNeverCallTheValueSupplier() {
            final String first = new String("x");
            final String second = new String("x");
            final IdentityHashSet<String> identity = new IdentityHashSet<>();
            identity.add(first);
            identity.add(second);
            final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Collections.addAll(caseInsensitive, "b", "A", "c");
            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("hash", new HashSet<>(Arrays.asList("p", "q")));
            backing.put("linked", new LinkedHashSet<>(Arrays.asList("z", "y", "x")));
            backing.put("sorted", caseInsensitive);
            backing.put("identity", identity);
            backing.put("emptied", new LinkedHashSet<>(Arrays.asList("gone")));

            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return new HashSet<>();
            });
            backing.get("emptied").clear();

            final List<Map<String, Set<String>>> results = new ArrayList<>();
            final SetMultimap<String, String> copy = multimap.copy();
            results.add(copy.toMap());
            results.add(multimap.toMap());
            results.add(multimap.toMap(size -> new TreeMap<>()));
            assertEquals(multimap, copy);

            for (final Map<String, Set<String>> result : results) {
                assertEquals(backing.keySet(), result.keySet());

                for (final Map.Entry<String, Set<String>> e : result.entrySet()) {
                    final Set<String> source = backing.get(e.getKey());
                    assertNotSame(source, e.getValue());
                    assertSame(source.getClass(), e.getValue().getClass());
                    if (!"identity".equals(e.getKey())) {
                        assertEquals(new ArrayList<>(source), new ArrayList<>(e.getValue()), e.getKey()); // same order
                    }
                }

                assertEquals(2, result.get("identity").size());
                assertTrue(result.get("sorted").contains("a"));
                assertTrue(result.get("emptied").isEmpty());
            }

            for (final Set<String> value : copy.toMap().values()) {
                for (final Set<String> source : backing.values()) {
                    assertNotSame(source, value);
                }
            }

            assertEquals(0, supplierCalls.get());
        }

        // G122-02: a set copied through the value supplier after constructor copies were made still cannot be handed an
        // earlier copy (captured from the target map) or any source set; the supplier is called once per such set.
        @Test
        void testCopy_valueSupplierCannotReturnAnEarlierCopyOrASource() {
            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("a", new HashSet<>(Arrays.asList("1")));
            backing.put("b", new TreeSet<>(Arrays.asList("2", "3")));
            final SupplierCopiedSet<String> odd = new SupplierCopiedSet<>();
            odd.add("4");
            backing.put("c", odd);
            final Set<String> last = new LinkedHashSet<>(Arrays.asList("5"));
            backing.put("d", last);

            final List<Set<String>> captured = new ArrayList<>();
            final int[] mode = { 0 };
            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                switch (mode[0]) {
                    case 0:
                        return captured.get(captured.size() - 1); // the copy of "b"
                    case 1:
                        return last; // a later source
                    case 2:
                        return backing.get("a"); // an earlier source
                    default:
                        return new LinkedHashSet<>();
                }
            });
            final java.util.function.IntFunction<Map<String, Set<String>>> capturingMap = size -> new LinkedHashMap<>() {
                private static final long serialVersionUID = 1L;

                @Override
                public Set<String> put(final String key, final Set<String> value) {
                    captured.add(value);
                    return super.put(key, value);
                }
            };

            for (int m = 0; m < 3; m++) {
                mode[0] = m;
                captured.clear();
                supplierCalls.set(0);
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> multimap.toMap(capturingMap));
                assertEquals("The value supplier returned a collection already in use; it must return a new empty collection on every call", e.getMessage());
                assertEquals(1, supplierCalls.get());
                assertEquals(2, captured.size());
            }

            mode[0] = 3;
            captured.clear();
            supplierCalls.set(0);
            final Map<String, Set<String>> result = multimap.toMap(capturingMap);
            assertEquals(1, supplierCalls.get());
            assertEquals(new LinkedHashSet<>(Arrays.asList("4")), result.get("c"));
            assertEquals(Arrays.asList("1"), new ArrayList<>(backing.get("a")));
            assertEquals(Arrays.asList("2", "3"), new ArrayList<>(backing.get("b")));
            assertEquals(Arrays.asList("4"), new ArrayList<>(odd));
            assertEquals(Arrays.asList("5"), new ArrayList<>(last));
        }

        // G122-02: the source sets are reserved as they were when the copy started, even when a callback replaces one of them
        // in the backing map before the value supplier is first called: handing back the replaced set is still rejected as a
        // set in use (not as a non-empty set).
        @Test
        void testCopy_sourcesAreReservedAsOfTheStartOfTheCopy() {
            final HookElement hookElement = new HookElement("h");
            final Map<String, Set<Object>> backing = new LinkedHashMap<>();
            backing.put("a", new HashSet<>(Arrays.asList(hookElement)));
            final SupplierCopiedSet<Object> odd = new SupplierCopiedSet<>();
            odd.add("o");
            backing.put("b", odd);
            final Set<Object> original = new HashSet<>(Arrays.asList("c1"));
            backing.put("c", original);
            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, Object> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return original;
            });

            // runs while "a" is copied, i.e. after the copy started and before the value supplier is called for "b"
            hookElement.hook = () -> backing.put("c", new LinkedHashSet<>());
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, multimap::toMap);
            assertEquals("The value supplier returned a collection already in use; it must return a new empty collection on every call", e.getMessage());
            assertEquals(1, supplierCalls.get());
            assertEquals(Arrays.asList("c1"), new ArrayList<>(original));
        }

        // G122-02: ListMultimap copies are unchanged: every list comes from the value supplier, which can return neither a
        // source list nor the same list twice.
        @Test
        void testCopy_listMultimapStillChecksEverySuppliedList() {
            final Map<String, List<String>> backing = new LinkedHashMap<>();
            backing.put("a", new ArrayList<>(Arrays.asList("1", "1")));
            backing.put("b", new ArrayList<>(Arrays.asList("2")));
            final ListMultimap<String, String> multimap = ListMultimap.wrap(backing);
            final ListMultimap<String, String> copy = multimap.copy();
            assertEquals(multimap, copy);
            assertNotSame(backing.get("a"), copy.get("a"));
            assertEquals(Arrays.asList("1", "1"), multimap.toMap().get("a"));

            final List<String> shared = new ArrayList<>();
            final ListMultimap<String, String> reusing = CommonUtil.newListMultimap(LinkedHashMap::new, () -> shared);
            reusing.putValues("x", Arrays.asList("1"));
            assertThrows(IllegalArgumentException.class, reusing::copy);
        }
    }

    // ---- perf review 2026-10-08 G122 end ----

    // ---- perf review 2026-10-08 C001 begin ----
    @Nested
    class C001 {
        // C001 (coordinator, from verifier V9): a close handler of an input that re-enters the concatenation's iterator must not
        // make the retention fix clear the slot of the next, not yet closed, input.
        @Test
        void testConcat_closeHandlerReenteringIteratorStillClosesEveryInput() {
            final List<String> log = new ArrayList<>();
            final Iterator<?>[] outer = new Iterator<?>[1];
            final Seq<Integer, RuntimeException> a = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
                log.add("closeA");
                log.add("reenter=" + outer[0].hasNext());
            });
            final Seq<Integer, RuntimeException> b = Seq.<Integer, RuntimeException> of(2).onClose(() -> log.add("closeB"));
            final Seq<Integer, RuntimeException> c = Seq.<Integer, RuntimeException> of(3).onClose(() -> log.add("closeC"));
            final Seq<Integer, RuntimeException> s = Seq.concat(Arrays.asList(a, b, c)).onClose(() -> log.add("closeOuter"));
            final com.landawn.abacus.util.stream.Stream<Integer> st = s.stream();
            final Iterator<Integer> it = st.iterator();
            outer[0] = it;
            final List<Integer> got = new ArrayList<>();

            while (it.hasNext()) {
                got.add(it.next());
            }

            log.add("got=" + got);
            st.close();
            assertTrue(log.contains("closeA") && log.contains("closeB") && log.contains("closeC") && log.contains("closeOuter"), log.toString());
        }
    }
}
