package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

/**
 * Regression tests for the EntryStream findings of the 2026-09-24 stream/function review (cycle 1, base r9619).
 *
 * <ul>
 *   <li><b>C-014</b> - {@code transformViaStream(fn[, false])} left the source open when the eager transfer threw.</li>
 *   <li><b>C-030</b> - deprecated {@code sorted()/reverseSorted()/percentiles()} threw without closing the stream.</li>
 *   <li><b>C-061</b> - {@code mapPartial} rejected typed functions and method references (invariant Optional).</li>
 *   <li><b>C-063</b> - a {@code *Partial} mapper returning a {@code null} Optional failed with an NPE naming the
 *       internal variable {@code op}.</li>
 *   <li><b>E1-09</b> - {@code intersection(Map)} was narrower than {@code difference(Map)}.</li>
 *   <li><b>E1-11</b> - {@code prepend/append/appendIfEmpty(emptyMap)} returned a new wrapper, not this stream.</li>
 *   <li>Doc-only locks for C-028 (null-key policy), C-029 (toMap merge / null semantics), E2-08, E2-09.</li>
 * </ul>
 */
@Tag("unit")
public class EntryStreamReview20260924Test extends TestBase {

    private static EntryStream<String, Integer> counted(final AtomicInteger closeCount) {
        return EntryStream.of("a", 1, "b", 2).onClose(closeCount::incrementAndGet);
    }

    // ------------------------------------------------------------------ C-014

    @Test
    public void testTransformViaStreamEagerFailureClosesSource() {
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> es = counted(closed);

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> es.<String, Integer> transformViaStream(s -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals("boom", ex.getMessage());
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, es::count); // the source is closed, not reusable
    }

    @Test
    public void testTransformViaStreamExplicitNotDeferredFailureClosesSource() {
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> es = counted(closed);

        assertThrows(ArithmeticException.class, () -> es.<String, Integer> transformViaStream(s -> {
            throw new ArithmeticException("boom");
        }, false));

        assertEquals(1, closed.get());
    }

    @Test
    public void testTransformViaStreamEagerFailureOnEmptySourceClosesSource() {
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> es = EntryStream.<String, Integer> empty().onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> es.<String, Integer> transformViaStream(s -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals(1, closed.get());
    }

    @Test
    public void testTransformViaStreamCloseFailureIsSuppressedIntoTransferFailure() {
        final EntryStream<String, Integer> es = EntryStream.of("a", 1).onClose(() -> {
            throw new IllegalArgumentException("close failed");
        });

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> es.<String, Integer> transformViaStream(s -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals(1, ex.getSuppressed().length);
        assertEquals("close failed", ex.getSuppressed()[0].getMessage());
    }

    @Test
    public void testTransformViaStreamSuccessStillWorksAndLinksClose() {
        final AtomicInteger closed = new AtomicInteger();

        final EntryStream<String, Integer> result = counted(closed).<String, Integer> transformViaStream(s -> s.filter(e -> e.getValue() > 1));

        assertEquals(0, closed.get());
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2)), result.toList());
        assertEquals(1, closed.get());
    }

    // ------------------------------------------------------------------ C-030

    @Test
    @SuppressWarnings("deprecation")
    public void testSortedClosesStreamBeforeThrowing() {
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> es = counted(closed);

        assertThrows(UnsupportedOperationException.class, es::sorted);
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, es::toList);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testReverseSortedClosesStreamBeforeThrowing() {
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> es = counted(closed);

        assertThrows(UnsupportedOperationException.class, es::reverseSorted);
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, es::toList);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPercentilesClosesStreamBeforeThrowing() {
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> es = counted(closed);

        assertThrows(UnsupportedOperationException.class, es::percentiles);
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, es::count);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUnsupportedOnParallelAndEmptyStreamsAlsoCloses() {
        final AtomicInteger closed = new AtomicInteger();

        assertThrows(UnsupportedOperationException.class, () -> counted(closed).parallel(2).sorted());
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.<String, Integer> empty().onClose(closed::incrementAndGet).reverseSorted());
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.<String, Integer> empty().onClose(closed::incrementAndGet).percentiles());

        assertEquals(3, closed.get());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUnsupportedKeepsCloseHandlerFailureAsSuppressed() {
        final EntryStream<String, Integer> es = EntryStream.of("a", 1).onClose(() -> {
            throw new IllegalArgumentException("close failed");
        });

        final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, es::sorted);

        assertEquals("Use sorted(Comparator) instead.", ex.getMessage());
        assertEquals(1, ex.getSuppressed().length);
        assertEquals("close failed", ex.getSuppressed()[0].getMessage());
    }

    // ------------------------------------------------------------------ C-061 (compile-level: these did not compile on the base)

    private static Optional<Map.Entry<String, Integer>> keepAboveOne(final Map.Entry<String, Integer> e) {
        return e.getValue() > 1 ? Optional.of(e) : Optional.empty();
    }

    @Test
    public void testMapPartialAcceptsTypedFunction() {
        final Function<Map.Entry<String, Integer>, Optional<Map.Entry<String, Integer>>> f = e -> e.getValue() > 1
                ? Optional.of(new SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 10))
                : Optional.empty();

        final Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3).mapPartial(f).toMap();

        assertEquals(Map.of("B", 20, "C", 30), result);
    }

    @Test
    public void testMapPartialAcceptsMethodReference() {
        final List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "éß", 2, "😀", 3)
                .mapPartial(EntryStreamReview20260924Test::keepAboveOne)
                .toList();

        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("éß", 2), new SimpleImmutableEntry<>("😀", 3)), result);
    }

    @Test
    public void testMapPartialAcceptsTypedBiFunction() {
        final BiFunction<String, Integer, Optional<Map.Entry<String, Integer>>> f = (k, v) -> v > 1 ? Optional.of(new SimpleImmutableEntry<>(k, -v))
                : Optional.empty();

        assertEquals(Map.of("b", -2), EntryStream.of("a", 1, "b", 2).mapPartial(f).toMap());
        assertEquals(Collections.emptyMap(), EntryStream.<String, Integer> empty().mapPartial(f).toMap());
    }

    @Test
    public void testMapPartialImplicitLambdaStillInfersTypes() {
        final Map<String, Integer> result = EntryStream.of("a", 1, "b", 2)
                .mapPartial(e -> e.getValue() > 1 ? Optional.of(new SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 10)) : Optional.empty())
                .toMap();

        assertEquals(Map.of("B", 20), result);

        final Map<String, Integer> result2 = EntryStream.of("a", 1, "b", 2)
                .mapPartial((k, v) -> v > 1 ? Optional.of(new SimpleImmutableEntry<>(k.toUpperCase(), v * 10)) : Optional.empty())
                .toMap();

        assertEquals(Map.of("B", 20), result2);
    }

    @Test
    public void testMapPartialParallelWithTypedFunction() {
        final Function<Map.Entry<Integer, Integer>, Optional<Map.Entry<Integer, Integer>>> f = e -> e.getKey() % 2 == 0 ? Optional.of(e) : Optional.empty();
        final Map<Integer, Integer> source = new HashMap<>();

        for (int i = 0; i < 1000; i++) {
            source.put(i, i);
        }

        assertEquals(500, EntryStream.of(source).parallel(4).mapPartial(f).count());
    }

    // ------------------------------------------------------------------ C-063

    @Test
    public void testMapKeyPartialNullOptionalMessageNamesTheMapper() {
        final NullPointerException ex1 = assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).mapKeyPartial(k -> null).toList());
        assertTrue(ex1.getMessage().contains("keyMapper returned a null Optional"), ex1.getMessage());

        final NullPointerException ex2 = assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).mapKeyPartial((k, v) -> null).toList());
        assertTrue(ex2.getMessage().contains("keyMapper returned a null Optional"), ex2.getMessage());
    }

    @Test
    public void testMapValuePartialNullOptionalMessageNamesTheMapper() {
        final NullPointerException ex1 = assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).mapValuePartial(v -> null).toList());
        assertTrue(ex1.getMessage().contains("valueMapper returned a null Optional"), ex1.getMessage());

        final NullPointerException ex2 = assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).mapValuePartial((k, v) -> null).toList());
        assertTrue(ex2.getMessage().contains("valueMapper returned a null Optional"), ex2.getMessage());
    }

    @Test
    public void testPartialNullOptionalOnEmptyStreamIsNeverReached() {
        assertEquals(0, EntryStream.<String, Integer> empty().mapKeyPartial(k -> null).count());
        assertEquals(0, EntryStream.<String, Integer> empty().mapValuePartial((k, v) -> null).count());
    }

    @Test
    public void testMapPartialNullOptionalIsNpe() {
        // documented (doc-only lock): the whole-entry mapPartial delegates to Stream.mapPartial and also fails with NPE
        assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).mapPartial(e -> null).toList());
    }

    // ------------------------------------------------------------------ E1-09 (compile-level)

    @Test
    public void testIntersectionMapAcceptsWiderValueType() {
        final EntryStream<String, Number> es = EntryStream.of("a", (Number) 1, "b", 2, "c", 3);
        final Map<String, Object> other = new LinkedHashMap<>();
        other.put("b", 2);
        other.put("c", "3");
        other.put(null, null);

        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("b", 2)), es.intersection(other).toList());
        assertEquals(Collections.emptyList(), EntryStream.of("a", 1).intersection((Map<?, ?>) null).toList());
    }

    // ------------------------------------------------------------------ E1-11

    @Test
    public void testPrependAppendEmptyMapReturnsThisStream() {
        final EntryStream<String, Integer> es = EntryStream.of("a", 1);

        assertSame(es, es.prepend(new HashMap<String, Integer>()));
        assertSame(es, es.append(new HashMap<String, Integer>()));
        assertSame(es, es.appendIfEmpty(new HashMap<String, Integer>()));
        assertSame(es, es.prepend((Map<String, Integer>) null));
        assertSame(es, es.append((Map<String, Integer>) null));
        assertSame(es, es.appendIfEmpty((Map<String, Integer>) null));

        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1)), es.toList());
    }

    @Test
    public void testAppendEmptyMapKeepsMapBackedKeysFastPath() {
        final Map<String, Integer> source = new LinkedHashMap<>();
        source.put("über", 1);
        source.put("b", 2);

        assertEquals(Arrays.asList("über", "b"), EntryStream.of(source).append(Collections.<String, Integer> emptyMap()).keys().toList());
        assertEquals(Arrays.asList(1, 2), EntryStream.of(source).prepend(Collections.<String, Integer> emptyMap()).values().toList());
    }

    @Test
    public void testPrependAppendNonEmptyMapStillAddsEntries() {
        final EntryStream<String, Integer> empty = EntryStream.empty();
        assertEquals(Map.of("x", 9), empty.appendIfEmpty(Map.of("x", 9)).toMap());
        assertEquals(Arrays.asList("x", "a"), EntryStream.of("a", 1).prepend(Map.of("x", 9)).keys().toList());
        assertEquals(Arrays.asList("a", "x"), EntryStream.of("a", 1).append(Map.of("x", 9)).keys().toList());
    }

    // ------------------------------------------------------------------ C-013 (compile-level: these were "ambiguous" on the base)

    private static EntryStream<String, Integer> defaults() {
        return EntryStream.of("x", 1);
    }

    @Test
    public void testAppendIfEmptyAcceptsInlineLambda() {
        assertEquals(Map.of("x", 1), EntryStream.<String, Integer> empty().appendIfEmpty(() -> EntryStream.of("x", 1)).toMap());

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(Map.of("a", 1), EntryStream.of("a", 1).appendIfEmpty(() -> {
            calls.incrementAndGet();
            return EntryStream.of("x", 1);
        }).toMap());
        assertEquals(0, calls.get());

        // a null supplier result appends nothing
        assertEquals(Collections.emptyMap(), EntryStream.<String, Integer> empty().appendIfEmpty(() -> null).toMap());
    }

    @Test
    public void testAppendIfEmptyAcceptsMethodReferences() {
        assertEquals(Map.of("x", 1), EntryStream.<String, Integer> empty().appendIfEmpty(EntryStreamReview20260924Test::defaults).toMap());
        assertEquals(Collections.emptyMap(), EntryStream.<String, Integer> empty().appendIfEmpty(EntryStream::<String, Integer> empty).toMap());
    }

    @Test
    public void testMapOverloadsStillBindForMapArguments() {
        final Map<String, Integer> tree = new TreeMap<>(Map.of("é", 2, "b", 1));

        assertEquals(Arrays.asList("b", "é"), EntryStream.<String, Integer> empty().appendIfEmpty(tree).keys().toList());
        assertEquals(Arrays.asList("b", "é", "a"), EntryStream.of("a", 0).prepend(tree).keys().toList());
        assertEquals(Arrays.asList("a", "b", "é"), EntryStream.of("a", 0).append(tree).keys().toList());

        // a wider receiver still accepts a narrower map (Map<? extends K, ? extends V>)
        final EntryStream<Object, Object> wide = EntryStream.of((Object) "a", (Object) 0);
        assertEquals(2, wide.append(Map.of("k", 1)).count());
    }

    // ------------------------------------------------------------------ doc-only locks: C-029 (toMap)

    @Test
    public void testToMapMergeFunctionReceivesNullExistingValue() {
        final List<String> calls = new ArrayList<>();

        final Map<String, Integer> result = EntryStream.of("a", (Integer) null, "a", 1).toMap((x, y) -> {
            calls.add(x + "," + y);
            return x == null ? -1 : x + y;
        });

        assertEquals(Arrays.asList("null,1"), calls);
        assertEquals(Map.of("a", -1), result); // Map.merge would have stored 1 without calling the function

        // a null merge result removes the key
        assertEquals(Collections.emptyMap(), EntryStream.of("a", 1, "a", 2).toMap((x, y) -> null));
    }

    @Test
    public void testToMapAcceptsNullKeysAndValues() {
        final Map<String, Integer> result = EntryStream.of((String) null, 1, "b", (Integer) null).toMap();

        assertEquals(2, result.size());
        assertEquals(1, result.get(null));
        assertTrue(result.containsKey("b"));
        assertEquals(null, result.get("b"));

        // a key already present counts as a duplicate even when its value is null
        assertThrows(IllegalStateException.class, () -> EntryStream.of("a", (Integer) null, "a", 1).toMap());
    }

    @Test
    public void testToMapNullKeyRejectedByFactoryMap() {
        assertThrows(NullPointerException.class, () -> EntryStream.of((String) null, 1).toMap(() -> new TreeMap<String, Integer>()));
    }

    // ------------------------------------------------------------------ doc-only locks: C-028 (null keys)

    @Test
    public void testGroupToAndGroupByRejectNullKeyAndCloseStream() {
        final Map<String, Integer> source = new HashMap<>();
        source.put(null, 1);
        source.put("b", 2);

        final AtomicInteger closed = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> EntryStream.of(source).onClose(closed::incrementAndGet).groupTo());
        assertEquals(1, closed.get());

        assertThrows(NullPointerException.class, () -> EntryStream.of(source).groupBy().toMap());
        assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).groupBy(e -> (String) null, Map.Entry::getValue).toMap());
    }

    @Test
    public void testGroupByMergeAndToMultimapAcceptNullKey() {
        final Map<String, Integer> source = new HashMap<>();
        source.put(null, 1);
        source.put("b", 2);

        final Map<String, Integer> merged = EntryStream.of(source).groupBy(Integer::sum).toMap();
        assertEquals(1, merged.get(null));
        assertEquals(2, merged.get("b"));

        assertNotNull(EntryStream.of(source).toMultimap().get(null));
    }

    // ------------------------------------------------------------------ doc-only locks: E2-08 / E2-09

    @Test
    public void testMinByKeyNullEntryThrowsEvenWhenNotSelected() {
        final List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        entries.add(new SimpleImmutableEntry<>("b", 2));
        entries.add(null);
        entries.add(new SimpleImmutableEntry<>("a", 1));

        assertThrows(NullPointerException.class, () -> EntryStream.of(entries).minByKey(Comparable::compareTo));
        assertThrows(NullPointerException.class, () -> EntryStream.of(entries).maxByValue(Integer::compare));
    }

    @Test
    public void testConcatSkipsNullMaps() {
        assertEquals(Map.of("a", 1), EntryStream.concat(Map.of("a", 1), null).toMap());
        assertEquals(Map.of("a", 1), EntryStream.concat(Arrays.asList(null, Map.of("a", 1))).toMap());
    }

    @Test
    public void testZipWithDefaultsRepeatedDefaultKey() {
        assertEquals(Arrays.asList(new SimpleImmutableEntry<>("a", 1), new SimpleImmutableEntry<>("X", 2), new SimpleImmutableEntry<>("X", 3)),
                EntryStream.zip(new String[] { "a" }, new Integer[] { 1, 2, 3 }, "X", 0).toList());
    }
}
