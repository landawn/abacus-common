package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;
import com.landawn.abacus.util.u.Optional;

@Tag("unit")
public class StreamOptionalResultContractTest extends TestBase {
    private static final Comparator<Integer> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<Integer> NULLS_LAST = Comparator.nullsLast(Comparator.naturalOrder());

    enum Source {
        ARRAY, ITERATOR, PARALLEL_ARRAY, PARALLEL_ARRAY_ITERATOR_SPLIT, PARALLEL_ITERATOR, SINGLE_WORKER_ARRAY, SINGLE_WORKER_ITERATOR;

        Stream<Integer> stream(final Integer... values) {
            return streamOf(values);
        }

        <T> Stream<T> streamOf(final T[] values) {
            final boolean iterator = this == ITERATOR || this == PARALLEL_ITERATOR || this == SINGLE_WORKER_ITERATOR;
            final Stream<T> stream = iterator ? Stream.of(Arrays.asList(values).iterator()) : Stream.of(values);
            if (this == ARRAY || this == ITERATOR) {
                return stream;
            }
            return stream.parallel(ParallelSettings.builder()
                    .maxThreadNum(this == SINGLE_WORKER_ARRAY || this == SINGLE_WORKER_ITERATOR ? 1 : 2)
                    .splitStrategy(this == PARALLEL_ARRAY_ITERATOR_SPLIT ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY)
                    .build());
        }
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void selectedNullThrowsAndCloses(final Source source) {
        assertNullResult(source.stream(null, 1), Stream::first);
        assertNullResult(source.stream(1, null), Stream::last);
        assertNullResult(source.stream(null, 1), Stream::findFirst);
        assertNullResult(source.stream(null, 1), Stream::findAny);
        assertNullResult(source.stream(1, null, 2), s -> s.findFirst(v -> v == null));
        assertNullResult(source.stream(1, null, 2), s -> s.findAny(v -> v == null));
        assertNullResult(source.stream(1, null, 2), s -> s.findLast(v -> v == null));
        assertNullResult(source.stream(1, null, 2), s -> s.min(NULLS_FIRST));
        assertNullResult(source.stream(1, null, 2), s -> s.max(NULLS_LAST));
        assertNullResult(source.stream((Integer) null), s -> s.reduce(Integer::sum));
        assertNullResult(source.stream(1, 2, 3, 4), s -> s.reduce((a, b) -> null));
        assertNullResult(source.stream(1, 2, 3, 4), s -> s.foldLeft((a, b) -> null));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void sortedNullExtremaThrow(final Source source) {
        assertNullResult(source.stream(2, null, 1).sorted(NULLS_FIRST), s -> s.min(NULLS_FIRST));
        assertNullResult(source.stream(2, null, 1).sorted(NULLS_LAST), s -> s.max(NULLS_LAST));
        assertNullResult(source.stream((Integer) null).sorted(NULLS_FIRST), s -> s.min(NULLS_FIRST));
        assertNullResult(source.stream((Integer) null).sorted(NULLS_LAST), s -> s.max(NULLS_LAST));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void keyedExtremaRejectNullWinnersButRetainNullKeys(final Source source) {
        assertNullResult(source.stream(1, null, 2), s -> s.minBy(value -> value == null ? -1 : value));
        assertNullResult(source.stream(1, null, 2), s -> s.maxBy(value -> value == null ? 3 : value));
        assertFalse(source.stream().minBy(Function.identity()).isPresent());
        assertFalse(source.stream().maxBy(Function.identity()).isPresent());
        assertEquals(Optional.of(1), source.stream(1, 1).minBy(value -> null));
        assertEquals(Optional.of(1), source.stream(1, 1).maxBy(value -> null));
        assertEquals(Optional.of(1), source.stream(null, 1).minBy(value -> value == null ? 2 : value));
        assertEquals(Optional.of(1), source.stream(null, 1).maxBy(value -> value == null ? 0 : value));
        assertKeyedExtremaCollectorContract((values, collector) -> source.stream(values).collect(collector));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void jdkKeyedExtremaCollectorsRejectNullWinnersButRetainNullKeys(final boolean parallel) {
        assertKeyedExtremaCollectorContract((values, collector) -> {
            try (final java.util.stream.Stream<Integer> stream = Arrays.stream(values)) {
                return (parallel ? stream.parallel() : stream).collect(collector);
            }
        });
    }

    private static void assertKeyedExtremaCollectorContract(
            final BiFunction<Integer[], Collector<Integer, ?, Optional<Integer>>, Optional<Integer>> collect) {
        assertThrows(NullPointerException.class, () -> collect.apply(new Integer[] { 1, null, 2 },
                Collectors.minBy(value -> value == null ? -1 : value)));
        assertThrows(NullPointerException.class, () -> collect.apply(new Integer[] { 1, null, 2 },
                Collectors.maxBy(value -> value == null ? 3 : value)));
        assertFalse(collect.apply(new Integer[0], Collectors.minBy(Function.identity())).isPresent());
        assertFalse(collect.apply(new Integer[0], Collectors.maxBy(Function.identity())).isPresent());
        // The Optional contains the selected element, so a null sort key is still permitted.
        assertEquals(Optional.of(1), collect.apply(new Integer[] { 1, 1 }, Collectors.minBy(value -> null)));
        assertEquals(Optional.of(1), collect.apply(new Integer[] { 1, 1 }, Collectors.maxBy(value -> null)));
        assertEquals(Optional.of(1), collect.apply(new Integer[] { null, 1 }, Collectors.minBy(value -> value == null ? 2 : value)));
        assertEquals(Optional.of(1), collect.apply(new Integer[] { null, 1 }, Collectors.maxBy(value -> value == null ? 0 : value)));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void emptyAndNoMatchRemainEmpty(final Source source) {
        assertFalse(source.stream().first().isPresent());
        assertFalse(source.stream().last().isPresent());
        assertFalse(source.stream().findFirst(v -> true).isPresent());
        assertFalse(source.stream().findAny(v -> true).isPresent());
        assertFalse(source.stream().findLast(v -> true).isPresent());
        assertFalse(source.stream().min(NULLS_FIRST).isPresent());
        assertFalse(source.stream().max(NULLS_LAST).isPresent());
        assertFalse(source.stream().reduce(Integer::sum).isPresent());
        assertFalse(source.stream().foldLeft(Integer::sum).isPresent());
        assertFalse(source.stream(null, 1, 2).findFirst(v -> false).isPresent());
        assertFalse(source.stream(null, 1, 2).findAny(v -> false).isPresent());
        assertFalse(source.stream(null, 1, 2).findLast(v -> false).isPresent());
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void nonNullWinnersRemainPresent(final Source source) {
        assertEquals(Optional.of(1), source.stream(1, null).first());
        assertEquals(Optional.of(2), source.stream(null, 2).last());
        assertEquals(Optional.of(1), source.stream(1, null, 2).findFirst(v -> v != null));
        assertEquals(Optional.of(2), source.stream(1, null, 2).findLast(v -> v != null));
        assertEquals(Optional.of(2), source.stream(1, null, 2).findAny(v -> Integer.valueOf(2).equals(v)));
        assertEquals(Optional.of(1), source.stream(2, null, 1).min(NULLS_LAST));
        assertEquals(Optional.of(2), source.stream(2, null, 1).max(NULLS_FIRST));
        assertEquals(Optional.of(10), source.stream(1, 2, 3, 4).reduce(Integer::sum));
        // A null intermediate value is permitted when the final reduction result is non-null.
        assertEquals(Optional.of(3), source.stream(null, 1, 2).reduce((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void callbackValidationStillRunsOnEmptyInput(final Source source) {
        assertThrows(IllegalArgumentException.class, () -> source.stream().min(null));
        assertThrows(IllegalArgumentException.class, () -> source.stream().max(null));
        assertThrows(IllegalArgumentException.class, () -> source.stream().findFirst(null));
        assertThrows(IllegalArgumentException.class, () -> source.stream().findAny(null));
        assertThrows(IllegalArgumentException.class, () -> source.stream().findLast(null));
        assertThrows(IllegalArgumentException.class, () -> source.stream().reduce(null));
    }

    @Test
    void firstLastCollectorsDistinguishEmptyFromNull() {
        assertThrows(NullPointerException.class, () -> Stream.of((Integer) null, 1).collect(Collectors.first()));
        assertThrows(NullPointerException.class, () -> Stream.of(1, (Integer) null).collect(Collectors.last()));
        assertFalse(Stream.<Integer>empty().collect(Collectors.first()).isPresent());
        assertFalse(Stream.<Integer>empty().collect(Collectors.last()).isPresent());
        assertEquals(Optional.of(1), Stream.of(1, (Integer) null).collect(Collectors.first()));
        assertEquals(Optional.of(2), Stream.of((Integer) null, 2).collect(Collectors.last()));
        assertThrows(NullPointerException.class, () -> java.util.stream.Stream.of((Integer) null, 1).collect(Collectors.first()));
        assertThrows(NullPointerException.class, () -> java.util.stream.Stream.of(1, (Integer) null).collect(Collectors.last()));
    }

    @Test
    void reducingCollectorRejectsNullFinalResults() {
        assertThrows(NullPointerException.class, () -> Stream.of(1, 2).collect(Collectors.reducing((a, b) -> null)));
        assertThrows(NullPointerException.class, () -> Stream.of(1, 2).parallel(2).collect(Collectors.reducing((a, b) -> null)));
        assertFalse(Stream.<Integer>empty().collect(Collectors.reducing(Integer::sum)).isPresent());
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void mappedReducingCollectorRejectsOnlyNullFinalResults(final Source source) {
        assertMappedReducingContract((values, collector) -> source.stream(values).collect(collector));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void jdkMappedReducingCollectorRejectsOnlyNullFinalResults(final boolean parallel) {
        assertMappedReducingContract((values, collector) -> {
            try (final java.util.stream.Stream<Integer> stream = Arrays.stream(values)) {
                return (parallel ? stream.parallel() : stream).collect(collector);
            }
        });
    }

    private static void assertMappedReducingContract(
            final BiFunction<Integer[], Collector<Integer, ?, Optional<Integer>>, Optional<Integer>> collect) {
        assertFalse(collect.apply(new Integer[0], Collectors.<Integer, Integer>reducing(value -> {
            throw new AssertionError("empty input must not invoke the mapper");
        }, (left, right) -> {
            throw new AssertionError("empty input must not invoke the operator");
        })).isPresent());

        assertThrows(NullPointerException.class, () -> collect.apply(new Integer[] { 1 },
                Collectors.<Integer, Integer>reducing(value -> null, (left, right) -> {
                    throw new AssertionError("a singleton must not invoke the operator");
                })));
        assertThrows(NullPointerException.class, () -> collect.apply(new Integer[] { null },
                Collectors.<Integer, Integer>reducing(Function.identity(), (left, right) -> 7)));
        assertThrows(NullPointerException.class, () -> collect.apply(new Integer[] { 1, 2, 3, 4 },
                Collectors.<Integer, Integer>reducing(Function.identity(), (left, right) -> null)));

        // Null mapped values are allowed when subsequent reduction produces a non-null result.
        assertEquals(Optional.of(7), collect.apply(new Integer[] { 1, 2 },
                Collectors.<Integer, Integer>reducing(value -> null, (left, right) -> 7)));
        assertEquals(Optional.of(10), collect.apply(new Integer[] { 1, 2, 3, 4 },
                Collectors.<Integer, Integer>reducing(Function.identity(), Integer::sum)));
        assertEquals(Optional.of(6), collect.apply(new Integer[] { 3 },
                Collectors.<Integer, Integer>reducing(value -> value * 2, (left, right) -> {
                    throw new AssertionError("a singleton must not invoke the operator");
                })));

        // Represent additive zero as null, keeping the operator associative for parallel collection.
        final Collector<Integer, ?, Optional<Integer>> nullableSum = Collectors.<Integer, Integer>reducing(Function.identity(), (left, right) -> {
            final int sum = (left == null ? 0 : left) + (right == null ? 0 : right);
            return sum == 0 ? null : sum;
        });
        assertEquals(Optional.of(5), collect.apply(new Integer[] { 1, -1, 2, 3 }, nullableSum));
        assertEquals(Optional.of(5), collect.apply(new Integer[] { null, 2, null, 3 }, nullableSum));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void elementAtAndOnlyOneRejectSelectedNull(final Source source) {
        assertNullResult(source.stream(null, 1), s -> s.elementAt(0));
        assertNullResult(source.stream(1, null), s -> s.elementAt(1));
        assertNullResult(source.stream((Integer) null), Stream::onlyOne);
        assertEquals(Optional.of(2), source.stream(null, 2).elementAt(1));
        assertEquals(Optional.of(2), source.stream(2).onlyOne());
        assertFalse(source.stream((Integer) null).elementAt(1).isPresent());
        assertFalse(source.stream().elementAt(0).isPresent());
        assertFalse(source.stream().onlyOne().isPresent());
        assertThrows(TooManyElementsException.class, () -> source.stream(null, 1).onlyOne());
        assertThrows(TooManyElementsException.class, () -> source.stream(null, null).onlyOne());
        assertThrows(IllegalArgumentException.class, () -> source.stream().elementAt(-1));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void kthLargestRejectsNullInSortedAndUnsortedPaths(final Source source) {
        assertNullResult(source.stream(2, null, 1), s -> s.kthLargest(3, NULLS_FIRST));
        assertNullResult(source.stream(2, null, 1), s -> s.kthLargest(1, NULLS_LAST));
        assertNullResult(source.stream(2, null, 1).sorted(NULLS_FIRST), s -> s.kthLargest(3, NULLS_FIRST));
        assertNullResult(source.stream(2, null, 1).sorted(NULLS_LAST), s -> s.kthLargest(1, NULLS_LAST));
        assertEquals(Optional.of(1), source.stream(2, null, 1).kthLargest(2, NULLS_FIRST));
        assertFalse(source.stream(null, 1).kthLargest(3, NULLS_FIRST).isPresent());
        assertFalse(source.stream(null, 1).sorted(NULLS_FIRST).kthLargest(3, NULLS_FIRST).isPresent());
        assertFalse(source.stream().kthLargest(1, NULLS_FIRST).isPresent());
        assertThrows(IllegalArgumentException.class, () -> source.stream().kthLargest(0, NULLS_FIRST));
        assertThrows(IllegalArgumentException.class, () -> source.stream().kthLargest(1, null));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void foldRightRejectsOnlyNullFinalResults(final Source source) {
        assertNullResult(source.stream((Integer) null), s -> s.foldRight(Integer::sum));
        assertNullResult(source.stream(1, 2, 3), s -> s.foldRight((a, b) -> null));
        assertFalse(source.stream().foldRight(Integer::sum).isPresent());
        assertEquals(Optional.of(321), source.stream(1, 2, 3).foldRight((a, b) -> a * 10 + b));
        assertEquals(Optional.of(1), source.stream(1, 2, 3).foldRight((a, b) -> b == 2 ? null : b));
        assertThrows(IllegalArgumentException.class, () -> source.stream().foldRight(null));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void applyIfNotEmptyRejectsNullCallbackResults(final Source source) {
        assertNullResult(source.stream(1, 2), s -> s.applyIfNotEmpty(v -> null));
        assertNullResult(source.stream((Integer) null), s -> s.applyIfNotEmpty(v -> null));
        final AtomicInteger calls = new AtomicInteger();
        assertFalse(source.stream().applyIfNotEmpty(s -> {
            calls.incrementAndGet();
            return null;
        }).isPresent());
        assertEquals(0, calls.get());
        assertEquals(Optional.of(2L), source.stream(null, 1).applyIfNotEmpty(Stream::count));
        assertThrows(IllegalArgumentException.class, () -> source.stream().applyIfNotEmpty(null));
    }

    @Test
    void primitiveAndEntryStreamsRejectNullCallbackResults() {
        assertThrows(NullPointerException.class, () -> ByteStream.of((byte) 1).applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> CharStream.of('a').applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> ShortStream.of((short) 1).applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> IntStream.of(1).applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> LongStream.of(1L).applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> FloatStream.of(1F).applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> DoubleStream.of(1D).applyIfNotEmpty(s -> null));
        assertThrows(NullPointerException.class, () -> EntryStream.of("a", 1).applyIfNotEmpty(s -> null));
        assertFalse(IntStream.empty().applyIfNotEmpty(s -> { throw new AssertionError("must not run"); }).isPresent());
    }

    @Test
    void onlyOneCollectorsCheckCardinalityBeforeWrapping() {
        assertThrows(NullPointerException.class, () -> Stream.of((Integer) null).collect(Collectors.onlyOne()));
        assertThrows(NullPointerException.class, () -> Stream.of(1, null, 2).collect(Collectors.onlyOne(v -> v == null)));
        assertThrows(NullPointerException.class, () -> java.util.stream.Stream.of((Integer) null).parallel().collect(Collectors.onlyOne()));
        assertThrows(TooManyElementsException.class, () -> java.util.stream.Stream.of(null, null).parallel().collect(Collectors.onlyOne()));
        assertFalse(Stream.of(1, 2).collect(Collectors.onlyOne(v -> false)).isPresent());
        assertEquals(Optional.of(1), Stream.of(null, 1).collect(Collectors.onlyOne(v -> v != null)));
    }

    @Test
    void collectingOrEmptyRejectsNullOnlyWhenInputWasSeen() {
        final AtomicInteger finishes = new AtomicInteger();
        final var collector = Collectors.collectingOrEmpty(Collectors.collectingAndThen(Collectors.<Integer>toList(), list -> {
            finishes.incrementAndGet();
            return (Integer) null;
        }));
        assertFalse(Stream.<Integer>empty().collect(collector).isPresent());
        assertEquals(0, finishes.get());
        assertThrows(NullPointerException.class, () -> Stream.of((Integer) null).collect(collector));
        assertEquals(1, finishes.get());
        assertThrows(NullPointerException.class, () -> java.util.stream.Stream.of(1, 2, 3).parallel().collect(collector));
        assertEquals(2, finishes.get());
        assertEquals(Optional.of(Arrays.asList((Integer) null)), Stream.of((Integer) null).collect(Collectors.collectingOrEmpty(Collectors.toList())));
        assertFalse(Stream.of(1).collect(Collectors.filtering(v -> false, collector)).isPresent());
        assertEquals(2, finishes.get());
    }

    @Test
    void minMaxRejectsNullFinisherResultButRetainsNullExtrema() {
        final AtomicInteger finishes = new AtomicInteger();
        final var collector = Collectors.minMax(NULLS_FIRST, (Integer min, Integer max) -> {
            finishes.incrementAndGet();
            return (Integer) null;
        });
        assertFalse(Stream.<Integer>empty().collect(collector).isPresent());
        assertEquals(0, finishes.get());
        assertThrows(NullPointerException.class, () -> Stream.of(1, 2).collect(collector));
        assertThrows(NullPointerException.class, () -> Stream.of(1, null, 2).parallel(2).collect(collector));
        assertThrows(NullPointerException.class, () -> java.util.stream.Stream.of(1, 2).parallel().collect(collector));
        assertEquals(3, finishes.get());
        assertEquals(Optional.of(Pair.of(null, null)), Stream.of((Integer) null).collect(Collectors.minMax(NULLS_FIRST)));
        assertEquals(Optional.of(Pair.of(null, 2)), Stream.of(null, 2).collect(Collectors.minMax(NULLS_FIRST)));
        assertEquals(Optional.of(1), Stream.of(2, 1).collect(Collectors.minMax(NULLS_FIRST, (min, max) -> max - min)));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void keyExtractorIsAppliedToEveryElementIncludingNulls(final Source source) {
        // nullsLastBy/nullsFirstBy extract a key from every element, so an extractor that cannot take null
        // fails on a null element even when that element is not the winner.
        assertThrows(NullPointerException.class, () -> source.stream(1, null).minBy(Object::toString));
        assertThrows(NullPointerException.class, () -> source.stream(1, null).maxBy(Object::toString));
        assertThrows(NullPointerException.class, () -> source.stream(1, null).collect(Collectors.minBy(Object::toString)));
        assertThrows(NullPointerException.class, () -> source.stream(1, null).collect(Collectors.maxBy(Object::toString)));

        // A null-tolerant extractor still returns the non-null winner.
        assertEquals(Optional.of(1), source.stream(1, null).minBy(value -> value == null ? 2 : value));
        assertEquals(Optional.of(1), source.stream(1, null).maxBy(value -> value == null ? 0 : value));
    }

    private static void assertNullResult(final Stream<Integer> stream, final Function<Stream<Integer>, Optional<Integer>> terminal) {
        final AtomicInteger closes = new AtomicInteger();
        try (stream) {
            stream.onClose(closes::incrementAndGet);
            assertThrows(NullPointerException.class, () -> terminal.apply(stream));
            assertEquals(1, closes.get());
        }
        assertEquals(1, closes.get());
    }
}
