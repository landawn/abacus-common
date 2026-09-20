package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream.WindowHandler;

/**
 * Regression tests for the fixes applied after the 2026-09-02 line-by-line review of {@code BaseStream},
 * {@code Stream}, {@code IntStream}, {@code LongStream}, {@code DoubleStream}, {@code EntryStream}, {@code Seq},
 * {@code Collectors}, {@code Fn} and {@code Fnn}.
 *
 * <ul>
 *   <li><b>B1</b> — event-time windows ({@code WindowHandler.timeExtractor}) closed on the wall clock, so every
 *       window whose end lay in the past kept a single element and dropped the rest as late data.</li>
 *   <li><b>B2</b> — {@code Seq.prepend/append(Optional)} lost {@code assertNotClosed()}.</li>
 *   <li><b>B3</b> — {@code Stream.of(ArrayList)} streamed the live backing array.</li>
 *   <li><b>B4</b> — {@code EntryStream.reduce(identity, accumulator)} ran the parallel reduce.</li>
 *   <li><b>B7</b> — {@code Collectors.groupingBy} accepted a {@code null} downstream.</li>
 *   <li><b>B8</b> — {@code IntStream.ofIndices} computed the first index eagerly.</li>
 *   <li><b>B9</b> — {@code Stream.iterate(null, ...)} was rejected.</li>
 *   <li><b>B10</b> — {@code EntryStream.defer(() -> null)} NPEd at the first pull.</li>
 *   <li><b>B11</b> — {@code Collectors.toBiMap} silently stole a merged value from another key.</li>
 *   <li><b>B12</b> — a {@code null} merge result was stored as a {@code null} value instead of removing the key.</li>
 *   <li><b>D3/D4/O3</b> — named messages, {@code EntryStream.merge} selector type, closed-stream / null-argument
 *       checks. (Seq null group keys and negative {@code Seq.delay} durations are owner-locked by earlier tests and
 *       were left as they are.)</li>
 * </ul>
 */
@Tag("unit")
public class StreamSeqFnReviewFixes20260902Test extends TestBase {

    /** An event carrying its own timestamp. */
    private record Ev(int id, long ts) {
    }

    private static List<Ev> historicalEvents(final int n, final long stepMillis) {
        final long base = System.currentTimeMillis() - 3_600_000L; // one hour ago: every window end is in the past
        final List<Ev> evs = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            evs.add(new Ev(i, base + i * stepMillis));
        }

        return evs;
    }

    private static Collector<Ev, ?, List<Integer>> ids() {
        return Collectors.mapping(Ev::id, Collectors.toList());
    }

    // ---------------------------------------------------------------- B1: event-time windows

    @Test
    public void testB1_slidingWindow_eventTime_pastTimestamps_keepsEveryElement_arraySource() {
        final List<Ev> evs = historicalEvents(10, 1_000);
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        final List<List<Integer>> windows = Stream.of(evs)
                .window(Duration.ofMillis(3_000), Duration.ofMillis(3_000), () -> evs.get(0).ts(), handler, ids())
                .toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8), List.of(9)), windows);
    }

    @Test
    public void testB1_slidingWindow_eventTime_pastTimestamps_keepsEveryElement_iteratorSource_async() {
        // An iterator-backed source with the default async = true goes through the buffered-queue branch.
        final List<Ev> evs = historicalEvents(10, 1_000);
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        final List<List<Integer>> windows = Stream.of(evs.iterator())
                .window(Duration.ofMillis(3_000), Duration.ofMillis(3_000), () -> evs.get(0).ts(), handler, ids())
                .toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8), List.of(9)), windows);
    }

    @Test
    public void testB1_slidingWindow_eventTime_overlappingWindows_pastTimestamps() {
        final List<Ev> evs = historicalEvents(6, 1_000);
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        // duration 3 s, increment 2 s: [0,3) -> 0,1,2 ; [2,5) -> 2,3,4 ; [4,7) -> 4,5
        final List<List<Integer>> windows = Stream.of(evs)
                .window(Duration.ofMillis(3_000), Duration.ofMillis(2_000), () -> evs.get(0).ts(), handler, ids())
                .toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(2, 3, 4), List.of(4, 5)), windows);
    }

    @Test
    public void testB1_slidingWindow_eventTime_pendingElementBeyondWindowEndIsNotLost() {
        // Overlapping windows (3 s / 2 s) and a gap: after [0,3) -> 0,1,2 the queue still holds 2 for [2,5), while
        // the element at 10 s is already pending (it closed [0,3)). The [2,5) window must not pull further and
        // overwrite that pending element (which used to yield [[0,1,2],[2],[11]]): 10 must appear in [8,11) and,
        // through the overlap queue, again in [10,13).
        final long base = System.currentTimeMillis() - 3_600_000L;
        final List<Ev> evs = List.of(new Ev(0, base), new Ev(1, base + 1_000), new Ev(2, base + 2_000), new Ev(10, base + 10_000), new Ev(11, base + 11_000));
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        final List<List<Integer>> windows = Stream.of(evs).window(Duration.ofMillis(3_000), Duration.ofMillis(2_000), () -> base, handler, ids()).toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(2), List.of(10), List.of(10, 11)), windows);

        // Same through the async (iterator-source) path.
        final List<List<Integer>> windows2 = Stream.of(evs.iterator())
                .window(Duration.ofMillis(3_000), Duration.ofMillis(2_000), () -> base, handler, ids())
                .toList();

        assertEquals(windows, windows2);
    }

    @Test
    public void testB1_slidingWindow_eventTime_noLateDataCallbackForInOrderEvents() {
        final List<Ev> evs = historicalEvents(10, 1_000);
        final AtomicInteger late = new AtomicInteger();
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder()
                .timeExtractor(Ev::ts)
                .onLateData((e, r) -> late.incrementAndGet())
                .build();

        final List<List<Integer>> windows = Stream.of(evs)
                .window(Duration.ofMillis(3_000), Duration.ofMillis(3_000), () -> evs.get(0).ts(), handler, ids())
                .toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8), List.of(9)), windows);
        assertEquals(0, late.get());
    }

    @Test
    public void testB1_slidingWindow_eventTime_lateElementGoesToOnLateData_orIsDropped() {
        final long base = System.currentTimeMillis() - 3_600_000L;
        // 0 s, 1 s, 2 s, 4 s (closes the first window), then a late 1.5 s element, then 5 s.
        final List<Ev> evs = List.of(new Ev(0, base), new Ev(1, base + 1_000), new Ev(2, base + 2_000), new Ev(4, base + 4_000), new Ev(15, base + 1_500),
                new Ev(5, base + 5_000));

        final List<Integer> lateIds = new ArrayList<>();
        final WindowHandler<Ev, List<Integer>> withHandler = WindowHandler.<Ev, List<Integer>> builder()
                .timeExtractor(Ev::ts)
                .onLateData((e, windowResult) -> lateIds.add(e.id()))
                .build();

        List<List<Integer>> windows = Stream.of(evs).window(Duration.ofMillis(3_000), Duration.ofMillis(3_000), () -> base, withHandler, ids()).toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(4, 5)), windows);
        assertEquals(List.of(15), lateIds);

        // Without a late-data action the late element is dropped silently.
        final WindowHandler<Ev, List<Integer>> noHandler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        windows = Stream.of(evs).window(Duration.ofMillis(3_000), Duration.ofMillis(3_000), () -> base, noHandler, ids()).toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(4, 5)), windows);
    }

    @Test
    public void testB1_boundedWindow_eventTime_pastTimestamps_closesOnEventTimeOnly() {
        final List<Ev> evs = historicalEvents(10, 1_000);
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        final List<List<Integer>> windows = Stream.of(evs).window(Duration.ofMillis(3_000), 100, () -> evs.get(0).ts(), handler, ids()).toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8), List.of(9)), windows);
    }

    @Test
    public void testB1_boundedWindow_eventTime_pastTimestamps_iteratorSource_async() {
        final List<Ev> evs = historicalEvents(10, 1_000);
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        final List<List<Integer>> windows = Stream.of(evs.iterator()).window(Duration.ofMillis(3_000), 100, () -> evs.get(0).ts(), handler, ids()).toList();

        assertEquals(List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8), List.of(9)), windows);
    }

    @Test
    public void testB1_boundedWindow_eventTime_countLimitStillCloses() {
        final List<Ev> evs = historicalEvents(10, 1_000);
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        final List<List<Integer>> windows = Stream.of(evs).window(Duration.ofMillis(60_000), 4, () -> evs.get(0).ts(), handler, ids()).toList();

        // The count limit closes windows; nothing is dropped.
        final List<Integer> flattened = new ArrayList<>();
        windows.forEach(flattened::addAll);

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), flattened);
        assertTrue(windows.stream().allMatch(w -> w.size() <= 4), windows.toString());
        assertEquals(List.of(0, 1, 2, 3), windows.get(0));
    }

    @Test
    public void testB1_processingTimeWindow_stillWorks() {
        // No time extractor: elements are stamped as they are pulled, all land in the first (long) window.
        final List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5).window(Duration.ofMillis(60_000), Duration.ofMillis(60_000), Collectors.toList()).toList();

        assertEquals(List.of(List.of(1, 2, 3, 4, 5)), windows);
    }

    // ---------------------------------------------------------------- B2: Seq.prepend/append(Optional) on a closed Seq

    @Test
    public void testB2_seqPrependAppendOptional_onClosedSeq_throwsIllegalStateException() throws Exception {
        final Seq<String, Exception> closed = Seq.of("a");
        closed.close();

        assertThrows(IllegalStateException.class, () -> closed.prepend(Optional.empty()));
        assertThrows(IllegalStateException.class, () -> closed.append(Optional.empty()));
        assertThrows(IllegalStateException.class, () -> closed.prepend(Optional.of("x")));
        assertThrows(IllegalStateException.class, () -> closed.append(Optional.of("x")));

        // Still works on an open sequence, and a null Optional is rejected with IAE.
        assertEquals(List.of("a"), Seq.<String, Exception> of("a").prepend(Optional.empty()).toList());
        assertEquals(List.of("x", "a"), Seq.<String, Exception> of("a").prepend(Optional.of("x")).toList());
        assertEquals(List.of("a", "x"), Seq.<String, Exception> of("a").append(Optional.of("x")).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.<String, Exception> of("a").prepend((Optional<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.<String, Exception> of("a").append((Optional<String>) null));
    }

    // ---------------------------------------------------------------- B3: Stream.of(ArrayList) does not alias the backing array

    @Test
    public void testB3_streamOfArrayList_clearedBeforeTerminalOp_doesNotYieldNulls() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> s = Stream.of(list);

        list.clear();

        List<Integer> result;

        try {
            result = s.toList();
        } catch (final ConcurrentModificationException e) {
            return; // fail-fast is acceptable
        }

        assertFalse(result.contains(null), result.toString());
        assertTrue(result.isEmpty(), result.toString());
    }

    @Test
    public void testB3_streamOfArrayList_removeBeforeTerminalOp_neverShowsShiftedTail() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> s = Stream.of(list, 0, 3);

        list.remove(0);

        try {
            final List<Integer> result = s.toList();
            assertFalse(result.contains(null), result.toString());
            assertTrue(list.containsAll(result), result.toString());
        } catch (final ConcurrentModificationException e) {
            // fail-fast is acceptable
        }
    }

    @Test
    public void testB3_streamOfArrayList_unchanged_stillStreamsAllElements() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));

        assertEquals(List.of(1, 2, 3, 4, 5), Stream.of(list).toList());
        assertEquals(List.of(2, 3, 4), Stream.of(list, 1, 4).toList());
        assertEquals(5, Stream.of(list).count());
        assertEquals(List.of(1, 2, 3, 4, 5), Stream.of(list).parallel().sorted().toList());
    }

    // ---------------------------------------------------------------- B4: EntryStream.reduce(identity, accumulator) is sequential

    @Test
    public void testB4_entryStreamReduceWithIdentity_isSequentialEvenOnParallelStream() {
        final Map<Integer, String> m = new TreeMap<>();

        for (int i = 0; i < 8; i++) {
            m.put(i, "v" + i);
        }

        final Map.Entry<Integer, String> identity = new AbstractMap.SimpleEntry<>(-1, "");

        // A deliberately non-associative accumulator: only a sequential left fold yields "01234567".
        final BiFunction<Map.Entry<Integer, String>, Map.Entry<Integer, String>, Map.Entry<Integer, String>> concatKeys = (a,
                b) -> new AbstractMap.SimpleEntry<>(-1, a.getValue() + b.getKey());

        for (int round = 0; round < 5; round++) {
            final Map.Entry<Integer, String> r = EntryStream.of(m).parallel(4).reduce(identity, concatKeys::apply);

            assertEquals("01234567", r.getValue());
        }

        assertEquals("01234567", EntryStream.of(m).reduce(identity, concatKeys::apply).getValue());
    }

    // ---------------------------------------------------------------- B7: Collectors.groupingBy rejects a null downstream

    @Test
    public void testB7_groupingBy_nullDownstream_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.groupingBy(String::length, (Collector<String, ?, Long>) null));
        assertThrows(IllegalArgumentException.class, () -> Collectors.groupingBy(String::length, (Collector<String, ?, Long>) null, HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Collectors.groupingByConcurrent(String::length, (Collector<String, ?, Long>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Collectors.groupingByConcurrent(String::length, (Collector<String, ?, Long>) null, java.util.concurrent.ConcurrentHashMap::new));

        assertEquals(Map.of(1, 2L, 2, 1L), Stream.of("a", "b", "cc").collect(Collectors.groupingBy(String::length, Collectors.counting())));
    }

    // ---------------------------------------------------------------- B8: IntStream.ofIndices is lazy

    @Test
    public void testB8_intStreamOfIndices_isLazy() {
        final int[] source = { 1, 2, 3, 1, 5, 1 };
        final AtomicInteger calls = new AtomicInteger();

        final IntStream s = IntStream.ofIndices(source, 0, 1, (a, fromIndex) -> {
            calls.incrementAndGet();
            return N.indexOf(a, 1, fromIndex);
        });

        assertEquals(0, calls.get(), "indexFunc must not run before the stream is pulled");
        assertEquals(List.of(0, 3, 5), s.boxed().toList());
        assertTrue(calls.get() > 0);
    }

    @Test
    public void testB8_intStreamOfIndices_resultsUnchanged() {
        final int[] source = { 1, 2, 3, 1, 5, 1 };

        assertEquals(List.of(0, 3, 5), IntStream.ofIndices(source, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).boxed().toList());
        assertEquals(List.of(3, 5), IntStream.ofIndices(source, 1, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).boxed().toList());
        assertEquals(List.of(5, 3, 0), IntStream.ofIndices(source, 5, -1, (a, fromIndex) -> N.lastIndexOf(a, 1, fromIndex)).boxed().toList());
        assertEquals(List.of(3, 0), IntStream.ofIndices(source, 4, -1, (a, fromIndex) -> N.lastIndexOf(a, 1, fromIndex)).boxed().toList());
        assertEquals(List.of(), IntStream.ofIndices(source, (a, fromIndex) -> N.indexOf(a, 9, fromIndex)).boxed().toList());
        assertEquals(List.of(), IntStream.ofIndices((int[]) null, (a, fromIndex) -> 0).boxed().toList());

        // Repeated hasNext() calls do not advance; next() after exhaustion throws.
        final IntIterator it = IntStream.ofIndices(source, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).iterator();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals(0, it.nextInt());
        assertEquals(3, it.nextInt());
        assertEquals(5, it.nextInt());
        assertFalse(it.hasNext());
        assertThrows(java.util.NoSuchElementException.class, it::nextInt);
    }

    // ---------------------------------------------------------------- B9: Stream.iterate accepts a null seed

    @Test
    public void testB9_streamIterate_nullSeedIsAllowed() {
        final List<String> a = Stream.<String> iterate(null, x -> x == null ? "a" : x + "a").limit(3).toList();
        assertEquals(Arrays.asList(null, "a", "aa"), a);

        final List<String> b = Stream.<String> iterate(null, () -> true, x -> x == null ? "a" : x + "a").limit(2).toList();
        assertEquals(Arrays.asList(null, "a"), b);

        final List<String> c = Stream.<String> iterate(null, x -> x == null || x.length() < 2, x -> x == null ? "a" : x + "a").toList();
        assertEquals(Arrays.asList(null, "a"), c);

        assertThrows(IllegalArgumentException.class, () -> Stream.iterate("a", null));
        assertThrows(IllegalArgumentException.class, () -> Stream.iterate("a", (java.util.function.BooleanSupplier) null, x -> x));
        assertThrows(IllegalArgumentException.class, () -> Stream.iterate("a", (java.util.function.Predicate<String>) null, x -> x));
    }

    // ---------------------------------------------------------------- B10: EntryStream.defer(() -> null)

    @Test
    public void testB10_entryStreamDefer_nullResultIsEmpty() {
        assertEquals(0, EntryStream.<String, Integer> defer(() -> null).count());
        assertEquals(Map.of("a", 1), EntryStream.<String, Integer> defer(() -> EntryStream.of("a", 1)).toMap());
        assertThrows(IllegalArgumentException.class, () -> EntryStream.defer(null));
    }

    // ---------------------------------------------------------------- B11: toBiMap value collisions

    @Test
    public void testB11_toBiMap_mergedValueBoundToAnotherKey_throws() {
        // "a" merges to "2", but "2" is already the value of "b".
        assertThrows(IllegalArgumentException.class,
                () -> Stream.of("a:1", "b:2", "a:2").collect(Collectors.toBiMap(s -> s.split(":")[0], s -> s.split(":")[1], (v1, v2) -> v2)));

        // Same key, same value: a no-op.
        final BiMap<String, String> same = Stream.of("a:1", "b:2", "a:1")
                .collect(Collectors.toBiMap(s -> s.split(":")[0], s -> s.split(":")[1], (v1, v2) -> v1));
        assertEquals(Map.of("a", "1", "b", "2"), same);

        // Merged into a brand-new value.
        final BiMap<String, String> merged = Stream.of("a:1", "b:2", "a:3")
                .collect(Collectors.toBiMap(s -> s.split(":")[0], s -> s.split(":")[1], (v1, v2) -> v1 + v2));
        assertEquals(Map.of("a", "13", "b", "2"), merged);

        // A null merge result removes the key.
        final BiMap<String, String> removed = Stream.of("a:1", "b:2", "a:3")
                .collect(Collectors.toBiMap(s -> s.split(":")[0], s -> s.split(":")[1], (v1, v2) -> null));
        assertEquals(Map.of("b", "2"), removed);
    }

    // ---------------------------------------------------------------- B12: null merge result removes the key

    @Test
    public void testB12_nullMergeResult_removesKey_everywhere() throws Exception {
        assertEquals(Map.of(), Stream.of("a", "a").collect(Collectors.toMap(k -> k, v -> v, (x, y) -> null)));
        assertEquals(Map.of(), Stream.of("a", "a").collect(Collectors.toLinkedHashMap(k -> k, v -> v, (x, y) -> null)));
        assertEquals(Map.of(), Stream.of("a", "a").toMap(k -> k, v -> v, (x, y) -> null));
        assertEquals(Map.of(), Seq.<String, Exception> of("a", "a").toMap(k -> k, v -> v, (x, y) -> null));
        assertEquals(Map.of(), EntryStream.of("a", 1, "a", 2).toMap((x, y) -> null));

        // The key is only removed when it actually collides; other keys are untouched and re-insertion works.
        assertEquals(Map.of("b", "b"), Stream.of("a", "b", "a").collect(Collectors.toMap(k -> k, v -> v, (x, y) -> null)));
        assertEquals(Map.of("a", "a"), Stream.of("a", "a", "a").collect(Collectors.toMap(k -> k, v -> v, (x, y) -> null)));

        // A non-null merge result is stored as before.
        assertEquals(Map.of("a", "aa"), Stream.of("a", "a").collect(Collectors.toMap(k -> k, v -> v, (x, y) -> x + y)));
    }

    // ---------------------------------------------------------------- D2 (WONTFIX, locked): Seq.delay rejects negatives, accepts zero

    @Test
    public void testD2_seqDelay_rejectsNegative_acceptsZero() throws Exception {
        assertEquals(List.of(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).delay(Duration.ofMillis(0)).toList());
        assertEquals(List.of(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).delay(java.time.Duration.ofMillis(0)).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2, 3).delay(Duration.ofMillis(-5)));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2, 3).delay(java.time.Duration.ofMillis(-5)));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2, 3).delay((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2, 3).delay((java.time.Duration) null));
    }

    // ---------------------------------------------------------------- D3: named messages

    @Test
    public void testD3_collectingAndThen_nullDownstream_namedMessage() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> Collectors.collectingAndThen((Collector<String, ?, List<String>>) null, List::size));
        assertTrue(e1.getMessage().contains("downstream"), e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> Collectors.collectingOrEmpty((Collector<String, ?, List<String>>) null));
        assertTrue(e2.getMessage().contains("collector"), e2.getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                () -> Collectors.collectingOrElseGetIfEmpty((Collector<String, ?, List<String>>) null, ArrayList::new));
        assertTrue(e3.getMessage().contains("collector"), e3.getMessage());
    }

    // ---------------------------------------------------------------- D4: EntryStream.merge(Collection<Map>) selector type

    @Test
    public void testD4_entryStreamMergeCollection_selectorReceivesPlainEntries() {
        final Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("c", 3);
        final Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("d", 4);

        // An explicitly typed lambda compiles against Map.Entry<K, V>, as with the two-map overload.
        final BiFunction<Map.Entry<String, Integer>, Map.Entry<String, Integer>, MergeResult> selector = (Map.Entry<String, Integer> e1,
                Map.Entry<String, Integer> e2) -> e1.getKey().compareTo(e2.getKey()) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        final List<String> keys = EntryStream.merge(List.of(m1, m2), selector).keys().toList();
        assertEquals(List.of("a", "b", "c", "d"), keys);

        final List<String> keys2 = EntryStream.merge(m1, m2, selector).keys().toList();
        assertEquals(keys, keys2);

        assertEquals(2, EntryStream.merge(List.of(m1, Collections.<String, Integer> emptyMap()), selector).count());
        assertEquals(0, EntryStream.<String, Integer> merge(List.of(), selector).count());
    }

    // ---------------------------------------------------------------- D6: WindowHandler.of(int, boolean, extractor, action)

    @Test
    public void testD6_windowHandlerOf_fourArgs_stillWiresLateDataAction() {
        final long base = System.currentTimeMillis() - 3_600_000L;
        final List<Ev> evs = List.of(new Ev(0, base), new Ev(4, base + 4_000), new Ev(1, base + 1_000));
        final List<Integer> lateIds = new ArrayList<>();

        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.of(3, false, Ev::ts, (e, r) -> lateIds.add(e.id()));

        final List<List<Integer>> windows = Stream.of(evs).window(Duration.ofMillis(3_000), Duration.ofMillis(3_000), () -> base, handler, ids()).toList();

        assertEquals(List.of(List.of(0), List.of(4)), windows);
        assertEquals(List.of(1), lateIds);
        assertEquals(3, handler.cacheSizeForLateData());
        assertNotNull(handler.timeExtractor());
        assertNotNull(handler.onLateDataAction());
        assertThrows(IllegalArgumentException.class, () -> WindowHandler.of(3, false, Ev::ts, null));
    }

    // ---------------------------------------------------------------- O3: closed / null checks on EntryStream

    @Test
    public void testO3_entryStreamBiIterator_onClosedStream_throwsIllegalStateException() {
        final EntryStream<String, Integer> es = EntryStream.of("a", 1);
        es.close();

        assertThrows(IllegalStateException.class, es::biIterator);
    }

    @Test
    public void testO3_entryStreamPrependAppend_nullOptional_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> EntryStream.of("a", 1).prepend((Optional<Map.Entry<String, Integer>>) null));
        assertThrows(IllegalArgumentException.class, () -> EntryStream.of("a", 1).append((Optional<Map.Entry<String, Integer>>) null));

        assertEquals(List.of("a"), EntryStream.of("a", 1).prepend(Optional.<Map.Entry<String, Integer>> empty()).keys().toList());
        assertEquals(List.of("x", "a"), EntryStream.of("a", 1).prepend(Optional.of(Map.entry("x", 0))).keys().toList());
        assertEquals(List.of("a", "x"), EntryStream.of("a", 1).append(Optional.of(Map.entry("x", 0))).keys().toList());
    }
}
