package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;

/**
 * Regression locks for the EntryStream findings of the 2026-09-24 stream/function review, cycle 2 (all doc-only):
 *
 * <ul>
 *   <li><b>C-091</b> - {@code groupBy(.., BinaryOperator ..)}: the merge function receives a {@code null} existing
 *       value (unlike {@code Map.merge}) and a {@code null} result removes the key.</li>
 *   <li><b>C-092</b> - {@code invertedToDisposableEntry()}: which operations fail fast, which silently go wrong, and
 *       which uses are safe.</li>
 *   <li><b>C-093</b> - {@code distinct(merge)} / {@code distinctBy(keyMapper, merge)}: array identity, the
 *       {@code null}-merge-result occurrence rule, {@code null} keys.</li>
 *   <li><b>C-094</b> - the lambdas and method references the flat* ambiguity notes recommend do compile and work.</li>
 *   <li><b>R5-05</b> - a parallel toMap/toMultimap/groupTo keeps upstream parallelism.</li>
 *   <li><b>R5-06/D3-07</b> - min/maxByKey/Value throw NPE for a single {@code null} entry.</li>
 *   <li><b>R5-07</b> - transformViaStream throws ISE, after closing this stream, when the transfer returns a closed
 *       stream.</li>
 *   <li><b>D3-04</b> - a {@code null}-returning collection/map factory is rejected even on an empty stream.</li>
 * </ul>
 */
@Tag("unit")
@SuppressWarnings("deprecation")
public class EntryStreamReview20260924bTest extends TestBase {

    private static final BinaryOperator<Integer> NULL_AWARE = (x, y) -> x == null ? -1 : x + y;

    private static EntryStream<String, Integer> nullValueFirst(final boolean parallel) {
        final EntryStream<String, Integer> s = EntryStream.of("a", null, "b", 1, "a", 2);
        return parallel ? s.parallel(2) : s;
    }

    private static Map<String, Integer> lhm(final EntryStream<String, Integer> s) {
        return s.toMap(Suppliers.ofLinkedHashMap());
    }

    // ------------------------------------------------------------------ C-091

    @Test
    public void testGroupByMergeIsCalledWithNullExistingValue() {
        final Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", -1);
        expected.put("b", 1);

        for (final boolean parallel : new boolean[] { false, true }) {
            assertEquals(expected, lhm(nullValueFirst(parallel).groupBy(NULL_AWARE, LinkedHashMap::new)));
            assertEquals(expected, nullValueFirst(parallel).groupBy(NULL_AWARE).toMap());
            assertEquals(expected, nullValueFirst(parallel).groupBy(Fn.key(), Fn.value(), NULL_AWARE).toMap());
            assertEquals(expected, nullValueFirst(parallel).groupBy(Fn.key(), Fn.value(), NULL_AWARE, Suppliers.ofLinkedHashMap()).toMap());
        }
    }

    @Test
    public void testGroupByMergeJdkStyleMergerFailsOnNullExistingValue() {
        // Map.merge would treat the null as absent and store 2; groupBy passes (null, 2) to Integer::sum.
        assertThrows(NullPointerException.class, () -> nullValueFirst(false).groupBy(Integer::sum).toList());
        assertThrows(NullPointerException.class, () -> nullValueFirst(true).groupBy(Fn.key(), Fn.value(), Integer::sum).toList());
    }

    @Test
    public void testGroupByMergeNullResultRemovesKeyAndLaterValueStartsNewMapping() {
        final BinaryOperator<Integer> toNull = (x, y) -> null;

        assertEquals(Map.of("b", 2), EntryStream.of("a", 1, "b", 2, "a", 3).groupBy(toNull).toMap());
        assertEquals(Map.of("b", 2), EntryStream.of("a", 1, "b", 2, "a", 3).groupBy(Fn.key(), Fn.value(), toNull).toMap());

        // A third "a" re-creates the key, after the keys already present in an insertion-ordered map.
        assertEquals(Arrays.asList("b", "a"),
                EntryStream.of("a", 1, "b", 2, "a", 3, "a", 4).groupBy(toNull, LinkedHashMap::new).keys().toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2), new SimpleImmutableEntry<>("a", 4)),
                EntryStream.of("a", 1, "b", 2, "a", 3, "a", 4).groupBy(Fn.key(), Fn.value(), toNull, Suppliers.ofLinkedHashMap()).toList());
    }

    // ------------------------------------------------------------------ C-092

    private static EntryStream<Integer, String> inverted() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 3);
        m.put("b", 1);
        m.put("c", 2);
        return EntryStream.of(m).invertedToDisposableEntry();
    }

    private static Map.Entry<Integer, String> copy(final Map.Entry<Integer, String> e) {
        return new SimpleImmutableEntry<>(e.getKey(), e.getValue());
    }

    private static final Map.Entry<Integer, String> E3A = new SimpleImmutableEntry<>(3, "a");
    private static final Map.Entry<Integer, String> E1B = new SimpleImmutableEntry<>(1, "b");
    private static final Map.Entry<Integer, String> E2C = new SimpleImmutableEntry<>(2, "c");

    @Test
    public void testInvertedToDisposableEntryFailsFastWhenPulledWithoutReading() {
        final List<Function<EntryStream<Integer, String>, Object>> ops = Arrays.asList( //
                s -> s.count(), //
                s -> s.skip(1).map(EntryStreamReview20260924bTest::copy).toList(), //
                s -> s.toList(), //
                s -> s.toSet(), //
                s -> s.reduce((a, b) -> a), //
                s -> s.sortedByKey(Comparator.naturalOrder()).map(EntryStreamReview20260924bTest::copy).toList(), //
                s -> s.sortedByValue(Comparator.naturalOrder()).map(EntryStreamReview20260924bTest::copy).toList(), //
                s -> s.reversed().map(EntryStreamReview20260924bTest::copy).toList(), //
                s -> s.minByKey(Comparator.naturalOrder()), //
                s -> s.minByValue(Comparator.naturalOrder()), //
                s -> s.max(Map.Entry.comparingByKey()), //
                s -> s.distinct().toList(), //
                s -> s.sliding(2).toList(), //
                s -> s.split(2).toList(), //
                s -> s.filter(e -> true).toList());

        for (int i = 0; i < ops.size(); i++) {
            final Function<EntryStream<Integer, String>, Object> op = ops.get(i);
            final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> op.apply(inverted()), "op #" + i);
            assertTrue(ex.getMessage().startsWith("Entry has already been set"), "op #" + i + ": " + ex.getMessage());
        }
    }

    @Test
    public void testInvertedToDisposableEntrySilentlyWrongWhenEntriesAreReadAndRetained() {
        // Documented as silently wrong - locked so that the javadoc stays true (a fix would have to update both).
        // Directly: distinctByKey/distinctByValue read the key/value, then retain the one reused entry.
        assertEquals(Arrays.asList(E2C, E2C, E2C), inverted().distinctByKey().toList().stream().map(EntryStreamReview20260924bTest::copy).toList());
        assertEquals(Arrays.asList(E2C, E2C, E2C), inverted().distinctByValue().toList().stream().map(EntryStreamReview20260924bTest::copy).toList());

        // collapseByKey/collapseByValue end up comparing each entry with itself.
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c")), inverted().collapseByKey((x, y) -> true).toList());
        assertEquals(Arrays.asList(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")),
                inverted().collapseByKey((x, y) -> Math.abs(x - y) == 1).toList()); // correct would be [[a], [b, c]]
        assertEquals(Arrays.asList(Arrays.asList(3), Arrays.asList(1), Arrays.asList(2)), inverted().collapseByValue((x, y) -> !x.equals(y)).toList());

        // After an upstream op that reads each entry and passes the same entry on, retaining ops no longer fail fast.
        assertEquals(Arrays.asList(E2C, E2C, E2C),
                inverted().filterByKey(k -> true).sortedByKey(Comparator.naturalOrder()).map(EntryStreamReview20260924bTest::copy).toList());
        assertEquals(E2C, copy(inverted().filterByKey(k -> true).minByKey(Comparator.naturalOrder()).get())); // correct: 1=b
        assertEquals(E2C, copy(inverted().filterByValue(v -> true).max(Map.Entry.comparingByKey()).get())); // correct: 3=a
        assertEquals(Arrays.asList(E2C, E2C, E2C),
                inverted().filterByKey(k -> true).reversed().map(EntryStreamReview20260924bTest::copy).toList());
        assertEquals(Arrays.asList(E2C, E2C, E2C), inverted().filterByKey(k -> true).toList().stream().map(EntryStreamReview20260924bTest::copy).toList());
        assertEquals(Arrays.asList(E2C, E2C, E2C), inverted().filter((k, v) -> true).toList().stream().map(EntryStreamReview20260924bTest::copy).toList());
        assertEquals(Arrays.asList(E2C, E2C, E2C), inverted().peek((k, v) -> {
        }).toList().stream().map(EntryStreamReview20260924bTest::copy).toList());
        assertEquals(Arrays.asList(E2C, E2C, E2C),
                inverted().peek(e -> e.getKey()).sortedByKey(Comparator.naturalOrder()).map(EntryStreamReview20260924bTest::copy).toList());
    }

    @Test
    public void testInvertedToDisposableEntrySafeUses() {
        final List<String> seen = new ArrayList<>();
        inverted().forEach((k, v) -> seen.add(k + "=" + v));
        assertEquals(Arrays.asList("3=a", "1=b", "2=c"), seen);

        assertEquals(Map.of(3, "a", 1, "b", 2, "c"), inverted().toMap());
        assertEquals(Map.of(3, "a", 1, "b", 2, "c"), inverted().filterByKey(k -> true).toMap());
        assertEquals(Map.of(3, List.of("a"), 1, List.of("b"), 2, List.of("c")), inverted().groupTo());
        assertEquals(3, inverted().toMultimap().totalValueCount());
        assertEquals(Map.of(3, "a", 1, "b", 2, "c"), inverted().groupBy((x, y) -> x).toMap());
        assertEquals(Arrays.asList(3, 1, 2), inverted().keys().toList());
        assertEquals(Arrays.asList("a", "b", "c"), inverted().values().toList());
        assertTrue(inverted().anyMatch((k, v) -> k == 2));

        // Copy first, then any operation is safe.
        assertEquals(Arrays.asList(E1B, E2C, E3A),
                inverted().map(e -> new SimpleImmutableEntry<>(e.getKey(), e.getValue())).sortedByKey(Comparator.naturalOrder()).toList());
        assertEquals(Arrays.asList(E1B, E2C, E3A), inverted().mapValue(v -> v).sortedByKey(Comparator.naturalOrder()).toList());
        assertEquals(E1B, inverted().mapKey(k -> k).minByKey(Comparator.naturalOrder()).get());
        assertEquals(Arrays.asList(E3A, E1B, E2C), inverted().map(EntryStreamReview20260924bTest::copy).distinctByKey().toList());

        // A single element needs no second pull, so nothing fails.
        assertEquals(new SimpleImmutableEntry<>(1, "x"), copy(EntryStream.of("x", 1).invertedToDisposableEntry().minByKey(Comparator.naturalOrder()).get()));
    }

    // ------------------------------------------------------------------ C-093

    @Test
    public void testDistinctByMergeComparesArrayKeysByIdentityUnlikeOneArgDistinctBy() {
        final int[] k1 = { 1 };
        final int[] k2 = { 1 };
        final Function<Map.Entry<String, Integer>, int[]> keyMapper = e -> e.getKey().equals("x") ? k1 : k2;

        assertEquals(1, EntryStream.of("x", 1, "y", 2).distinctBy(keyMapper).count());
        assertEquals(2, EntryStream.of("x", 1, "y", 2).distinctBy(keyMapper, (a, b) -> a).count());
        assertEquals(2, EntryStream.of("x", 1, "y", 2).parallel(2).distinctBy(keyMapper, (a, b) -> a).count());
        assertEquals(1, EntryStream.of("x", 1, "y", 2).distinctBy(e -> k1, (a, b) -> a).count());
    }

    @Test
    public void testDistinctMergeComparesEntriesByEqualsSoArrayKeysByIdentity() {
        final int[] k1 = { 1 };
        final int[] k2 = { 1 };

        assertEquals(2, EntryStream.of(k1, 1, k2, 1).distinct((a, b) -> a).count());
        assertEquals(2, EntryStream.of(k1, 1, k2, 1).distinct().count());
        assertEquals(1, EntryStream.of(k1, 1, k1, 1).distinct((a, b) -> a).count());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1), new SimpleImmutableEntry<>("b", 2)),
                EntryStream.of("a", 1, "b", 2, "a", 1).distinct((first, dup) -> first).toList());
    }

    @Test
    public void testDistinctMergeNullResultOccurrenceRule() {
        final BinaryOperator<Map.Entry<String, Integer>> toNull = (x, y) -> null;

        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2)), EntryStream.of("a", 1, "b", 2, "a", 1).distinct(toNull).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2), new SimpleImmutableEntry<>("a", 1)),
                EntryStream.of("a", 1, "b", 2, "a", 1, "a", 1).distinct(toNull).toList());

        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2)), EntryStream.of("a", 1, "b", 2, "a", 3).distinctBy(Fn.key(), toNull).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2), new SimpleImmutableEntry<>("a", 4)),
                EntryStream.of("a", 1, "b", 2, "a", 3, "a", 4).distinctBy(Fn.key(), toNull).toList());
    }

    @Test
    public void testDistinctMergeNullEntriesAndNullGroupValuesArePassedToMerge() {
        final List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        entries.add(null);
        entries.add(new SimpleImmutableEntry<>("a", 1));
        entries.add(null);

        final AtomicInteger nullPairs = new AtomicInteger();
        final List<Map.Entry<String, Integer>> result = EntryStream.of(entries).distinct((x, y) -> {
            if (x == null && y == null) {
                nullPairs.incrementAndGet();
            }
            return x;
        }).toList();

        // The null group's current value is null, so the merge still runs and its null result removes the group.
        assertEquals(1, nullPairs.get());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), result);
    }

    @Test
    public void testDistinctByMergeNullKeyKeepsPositionOfFirstEntry() {
        final List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3)
                .distinctBy(e -> e.getKey().equals("b") ? "k" : null, (x, y) -> y)
                .toList();

        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("c", 3), new SimpleImmutableEntry<>("b", 2)), result);
    }

    @Test
    public void testDistinctByMergeParallelWithAssociativeMerge() {
        final Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4, "a", 5)
                .parallel(3)
                .distinctBy(Fn.key(), (e1, e2) -> new SimpleImmutableEntry<>(e1.getKey(), e1.getValue() + e2.getValue()))
                .toMap();

        assertEquals(Map.of("a", 9, "b", 6), result);
    }

    // ------------------------------------------------------------------ C-094

    @Test
    public void testFlatMapNotesRecommendedLambdasAndReferencesWork() {
        final Map<String, String[]> arrays = new LinkedHashMap<>();
        arrays.put("x", new String[] { "1", "2" });
        arrays.put("é", new String[] { "😀" });

        final List<Map.Entry<String, String>> expected = Arrays.asList(new SimpleImmutableEntry<>("x", "1"), new SimpleImmutableEntry<>("x", "2"),
                new SimpleImmutableEntry<>("é", "😀"));

        // The lambdas the notes recommend in place of Stream::of / Arrays::asList.
        assertEquals(expected, EntryStream.of(arrays).flatMapValue(v -> Stream.of(v)).toList());
        assertEquals(expected, EntryStream.of(arrays).flatmapValue(v -> Arrays.asList(v)).toList());
        assertEquals(Arrays.asList(Arrays.asList("1", "2"), Arrays.asList("😀")),
                EntryStream.of(arrays).mapValue(x -> Arrays.asList(x)).values().toList());

        // References to methods without a two-argument form compile and bind to the Function overload.
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), EntryStream.of("a", 1).flatMapKey(Stream::ofNullable).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), EntryStream.of("a", 1).flatMapValue(Stream::ofNullable).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), EntryStream.of("a", 1).flatmapKey(Collections::singletonList).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), EntryStream.of("a", 1).flatmapValue(Collections::singletonList).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), EntryStream.of("a", 1).flattMap(e -> Stream.of(e)).toList());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)),
                EntryStream.of("a", 1).flattMap((k, v) -> Stream.of(new SimpleImmutableEntry<>(k, v))).toList());
    }

    // ------------------------------------------------------------------ R5-05

    private static List<Map.Entry<Integer, Integer>> sixteen() {
        final List<Map.Entry<Integer, Integer>> list = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            list.add(new SimpleImmutableEntry<>(i % 8, i));
        }
        return list;
    }

    @Test
    public void testParallelToMapKeepsUpstreamParallelismAndMergesOnCallingThread() {
        final Thread caller = Thread.currentThread();
        final Set<Thread> mapperThreads = ConcurrentHashMap.newKeySet();
        final Set<Thread> mergeThreads = ConcurrentHashMap.newKeySet();

        final Map<Integer, Integer> result = EntryStream.of(sixteen()).parallel(4).mapValue(v -> {
            mapperThreads.add(Thread.currentThread());
            N.sleep(10);
            return v;
        }).toMap((x, y) -> {
            mergeThreads.add(Thread.currentThread());
            return x + y;
        });

        assertEquals(8, result.size());
        assertEquals(8, result.get(0).intValue());
        assertTrue(mapperThreads.size() > 1, "upstream mapValue ran on " + mapperThreads.size() + " thread(s)");
        assertEquals(Set.of(caller), mergeThreads);
    }

    @Test
    public void testParallelToMultimapAndGroupToKeepUpstreamParallelism() {
        final Set<Thread> mapperThreads = ConcurrentHashMap.newKeySet();
        final Function<Integer, Integer> slow = v -> {
            mapperThreads.add(Thread.currentThread());
            N.sleep(10);
            return v;
        };

        final ListMultimap<Integer, Integer> mm = EntryStream.of(sixteen()).parallel(4).mapValue(slow).toMultimap();
        assertEquals(16, mm.totalValueCount());
        assertTrue(mapperThreads.size() > 1);

        mapperThreads.clear();
        final Map<Integer, List<Integer>> groups = EntryStream.of(sixteen()).parallel(4).mapValue(slow).groupTo();
        assertEquals(8, groups.size());
        assertTrue(mapperThreads.size() > 1);
    }

    // ------------------------------------------------------------------ R5-06 / D3-07

    @Test
    public void testMinMaxByKeyValueSingleNullEntryThrowsNpe() {
        final List<Map.Entry<String, Integer>> single = new ArrayList<>();
        single.add(null);

        for (final boolean parallel : new boolean[] { false, true }) {
            final Supplier<EntryStream<String, Integer>> s = () -> parallel ? EntryStream.of(single).parallel(2) : EntryStream.of(single);
            assertThrows(NullPointerException.class, () -> s.get().minByKey(Comparator.naturalOrder()));
            assertThrows(NullPointerException.class, () -> s.get().minByValue(Comparator.naturalOrder()));
            assertThrows(NullPointerException.class, () -> s.get().maxByKey(Comparator.naturalOrder()));
            assertThrows(NullPointerException.class, () -> s.get().maxByValue(Comparator.naturalOrder()));
        }
    }

    // ------------------------------------------------------------------ R5-07

    @Test
    public void testTransformViaStreamReturningClosedStreamThrowsAndClosesSource() {
        final Function<Stream<Map.Entry<String, Integer>>, Stream<Map.Entry<String, Integer>>> closedOther = s -> {
            final Stream<Map.Entry<String, Integer>> other = Stream.of(new SimpleImmutableEntry<>("z", 9));
            other.close();
            return other;
        };

        final AtomicInteger c1 = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> EntryStream.of("a", 1).onClose(c1::incrementAndGet).transformViaStream(closedOther));
        assertEquals(1, c1.get());

        final AtomicInteger c2 = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> EntryStream.of("a", 1).onClose(c2::incrementAndGet).transformViaStream(closedOther, false));
        assertEquals(1, c2.get());

        // Deferred: the transfer runs on first traversal, so that is where the ISE surfaces.
        final AtomicInteger c3 = new AtomicInteger();
        final EntryStream<String, Integer> deferred = EntryStream.of("a", 1).onClose(c3::incrementAndGet).transformViaStream(closedOther, true);
        assertEquals(0, c3.get());
        assertThrows(IllegalStateException.class, deferred::toList);
        assertEquals(1, c3.get());

        // A transfer that closes and returns its own input behaves the same.
        final AtomicInteger c4 = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> EntryStream.of("a", 1).onClose(c4::incrementAndGet).transformViaStream(s -> {
            s.close();
            return s;
        }));
        assertEquals(1, c4.get());
    }

    // ------------------------------------------------------------------ D3-04

    @Test
    public void testNullReturningFactoriesAreRejectedEvenOnEmptyStreams() {
        final List<Function<EntryStream<String, Integer>, Object>> terminals = Arrays.asList( //
                s -> s.toCollection(() -> null), //
                s -> s.toMultiset(() -> null), //
                s -> s.toMap(() -> (Map<String, Integer>) null), //
                s -> s.toMap((x, y) -> x, () -> (Map<String, Integer>) null), //
                s -> s.toMultimap(() -> (ListMultimap<String, Integer>) null), //
                s -> s.groupTo(() -> (Map<String, List<Integer>>) null));

        for (final boolean empty : new boolean[] { true, false }) {
            for (final boolean parallel : new boolean[] { false, true }) {
                for (int i = 0; i < terminals.size(); i++) {
                    final AtomicInteger closed = new AtomicInteger();
                    EntryStream<String, Integer> s = (empty ? EntryStream.<String, Integer> empty() : EntryStream.of("a", 1, "b", 2))
                            .onClose(closed::incrementAndGet);
                    if (parallel) {
                        s = s.parallel(2);
                    }
                    final EntryStream<String, Integer> fs = s;
                    final Function<EntryStream<String, Integer>, Object> op = terminals.get(i);
                    final String label = "terminal #" + i + " empty=" + empty + " parallel=" + parallel;
                    final NullPointerException ex = assertThrows(NullPointerException.class, () -> op.apply(fs), label);
                    assertTrue(ex.getMessage().endsWith("returned null"), label + ": " + ex.getMessage());
                    assertEquals(1, closed.get(), label);
                }
            }
        }
    }

    @Test
    public void testGroupByNullReturningMapFactoryIsRejectedWhenGroupingRuns() {
        final List<Function<EntryStream<String, Integer>, EntryStream<?, ?>>> groupBys = Arrays.asList( //
                s -> s.groupBy(() -> null), //
                s -> s.groupBy((x, y) -> x, () -> null), //
                s -> s.groupBy(Collectors.counting(), () -> null), //
                s -> s.groupBy(Fn.key(), Fn.value(), () -> null), //
                s -> s.groupBy(Fn.key(), Collectors.counting(), () -> null), //
                s -> s.groupBy(Fn.key(), Fn.value(), (Integer x, Integer y) -> x, () -> null));

        for (int i = 0; i < groupBys.size(); i++) {
            final AtomicInteger closed = new AtomicInteger();
            // Lazy: returning the grouped stream does not call the factory yet ...
            final EntryStream<?, ?> grouped = groupBys.get(i).apply(EntryStream.<String, Integer> empty().onClose(closed::incrementAndGet));
            assertEquals(0, closed.get());
            // ... the first traversal does, and rejects a null map even though there is nothing to group.
            final NullPointerException ex = assertThrows(NullPointerException.class, grouped::toList, "groupBy #" + i);
            assertEquals("mapFactory returned null", ex.getMessage());
            assertEquals(1, closed.get(), "groupBy #" + i);
        }
    }

    @Test
    public void testNonNullFactoriesStillWork() {
        assertEquals(0, EntryStream.<String, Integer> empty().toMap(Suppliers.ofLinkedHashMap()).size());
        assertNull(EntryStream.of("a", (Integer) null).toMap(Suppliers.ofLinkedHashMap()).get("a"));
        assertEquals(Map.of("a", 1), EntryStream.of("a", 1).groupBy((x, y) -> x, LinkedHashMap::new).toMap());
    }
}
