package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;
import static com.landawn.abacus.util.stream.IteratorContractAssertions.assertValues;
import static com.landawn.abacus.util.stream.IteratorContractAssertions.assertRemaining;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

/** Contracts for lazy iteration, bulk consumption, and terminal failure cleanup. */
public class LongStreamIteratorContractTest extends TestBase {
    private static LongStream source(boolean iterator) {
        long[] values = { 1, 2, 2, 3, 4 };
        return iterator ? LongStream.of(LongIterator.of(values)) : LongStream.of(values);
    }

    private static Arguments pipeline(String name, Supplier<LongStream> factory, int... values) {
        List<Long> expected = new ArrayList<>();
        for (int value : values) {
            expected.add((long) value);
        }
        return Arguments.of(name, factory, expected);
    }

    static java.util.stream.Stream<Arguments> pipelines() {
        List<Arguments> cases = new ArrayList<>();
        for (boolean iterator : new boolean[] { false, true }) {
            String prefix = iterator ? "iterator/" : "array/";
            cases.add(pipeline(prefix + "identity", () -> source(iterator), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "filter", () -> source(iterator).filter(v -> v >= 2), 2, 2, 3, 4));
            cases.add(pipeline(prefix + "takeWhile", () -> source(iterator).takeWhile(v -> v < 3), 1, 2, 2));
            cases.add(pipeline(prefix + "dropWhile", () -> source(iterator).dropWhile(v -> v < 3), 3, 4));
            cases.add(pipeline(prefix + "step", () -> source(iterator).step(2), 1, 2, 4));
            cases.add(pipeline(prefix + "hugeStep", () -> source(iterator).step(Long.MAX_VALUE), 1));
            cases.add(pipeline(prefix + "limit", () -> source(iterator).limit(3), 1, 2, 2));
            cases.add(pipeline(prefix + "skip", () -> source(iterator).skip(2), 2, 3, 4));
            cases.add(pipeline(prefix + "skipAndLimit", () -> source(iterator).skipAndLimit(1, 2), 2, 2));
            cases.add(pipeline(prefix + "map", () -> source(iterator).map(v -> (long) (v + 1)), 2, 3, 3, 4, 5));
            cases.add(pipeline(prefix + "flatMap", () -> source(iterator).flatMap(v -> v == 2 ? LongStream.empty() : LongStream.of(v)), 1, 3, 4));
            cases.add(pipeline(prefix + "flatMapArray", () -> source(iterator).flatMapArray(v -> v == 2 ? null : new long[] { v }), 1, 3, 4));
            cases.add(pipeline(prefix + "flatmap", () -> source(iterator).flatmap(v -> v == 2 ? List.of() : List.of(v)), 1, 3, 4));
            cases.add(pipeline(prefix + "sortedDistinct", () -> source(iterator).sorted().distinct(), 1, 2, 3, 4));
            cases.add(pipeline(prefix + "reverseSorted", () -> source(iterator).reverseSorted(), 4, 3, 2, 2, 1));
            cases.add(pipeline(prefix + "unsortedDistinct", () -> source(iterator).distinct(), 1, 2, 3, 4));
            cases.add(pipeline(prefix + "skipUntil", () -> source(iterator).skipUntil(v -> v >= 3), 3, 4));
            cases.add(pipeline(prefix + "skipUntilEmpty", () -> source(iterator).skipUntil(v -> false)));
            cases.add(pipeline(prefix + "emptyCycle", () -> source(iterator).limit(0).cycled()));
            cases.add(pipeline(prefix + "emptyReverseSorted", () -> source(iterator).limit(0).reverseSorted()));
            cases.add(pipeline(prefix + "onEach", () -> source(iterator).onEach(v -> {
            }), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "reversed", () -> source(iterator).reversed(), 4, 3, 2, 2, 1));
            cases.add(pipeline(prefix + "rotated", () -> source(iterator).rotated(2), 3, 4, 1, 2, 2));
            cases.add(pipeline(prefix + "sorted", () -> source(iterator).sorted(), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "scan", () -> source(iterator).scan((a, b) -> (long) (a + b)), 1, 3, 5, 8, 12));
            cases.add(pipeline(prefix + "scanWithIdentity", () -> source(iterator).scan((long) 0, true, (a, b) -> (long) (a + b)), 0, 1, 3, 5, 8, 12));
            cases.add(pipeline(prefix + "top", () -> source(iterator).top(1), 4));
            cases.add(pipeline(prefix + "sortedTop", () -> source(iterator).sorted().top(1), 4));
            cases.add(pipeline(prefix + "allTop", () -> source(iterator).top(10), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "zeroTop", () -> source(iterator).top(0)));
            cases.add(pipeline(prefix + "appendNonempty", () -> source(iterator).appendIfEmpty((long) 9), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "appendEmpty", () -> source(iterator).limit(0).appendIfEmpty((long) 9), 9));
            cases.add(pipeline(prefix + "emptyFilter", () -> source(iterator).filter(v -> false)));
            cases.add(pipeline(prefix + "cycled", () -> source(iterator).limit(2).cycled(2), 1, 2, 1, 2));
        }
        cases.add(pipeline("factory/repeat", () -> LongStream.repeat((long) 7, 3), 7, 7, 7));
        cases.add(pipeline("factory/repeatEmpty", () -> LongStream.repeat((long) 7, 0)));
        cases.add(pipeline("factory/defer", () -> LongStream.defer(() -> LongStream.of(new long[] { 1, 2, 3 })), 1, 2, 3));
        cases.add(pipeline("factory/deferEmpty", () -> LongStream.defer(LongStream::empty)));
        cases.add(pipeline("factory/flatten", () -> LongStream.flatten(new long[][] { { 1, 2 }, null, { 3 }, { 4, 5 } }), 1, 2, 3, 4, 5));
        cases.add(pipeline("factory/flattenVertical", () -> LongStream.flatten(new long[][] { { 1, 2 }, null, { 3 }, { 4, 5 } }, true), 1, 3, 4, 2, 5));
        cases.add(pipeline("factory/flattenAligned", () -> LongStream.flatten(new long[][] { { 1, 2 }, { 3 }, { 4, 5 } }, (long) 0, true), 1, 3, 4, 2, 0, 5));
        cases.add(pipeline("factory/flattenThreeDimensions", () -> LongStream.flatten(new long[][][] { { { 1 }, { 2, 3 } }, null, { { 4 } } }), 1, 2, 3, 4));
        cases.add(pipeline("factory/concatArrays", () -> LongStream.concat(new long[] { 1, 2 }, new long[0], new long[] { 3, 4 }), 1, 2, 3, 4));
        cases.add(pipeline("factory/concatArrayList", () -> LongStream.concat(java.util.Arrays.asList(new long[] { 1, 2 }, new long[0], new long[] { 3, 4 })),
                1, 2, 3, 4));
        cases.add(pipeline("factory/concatIterators",
                () -> LongStream.concatIterators(List.of(LongIterator.of(new long[] { 1, 2 }), LongIterator.empty(), LongIterator.of(new long[] { 3 }))), 1, 2,
                3));
        cases.add(pipeline("factory/concatStreams",
                () -> LongStream.concat(LongStream.of(new long[] { 1, 2 }), LongStream.empty(), LongStream.of(new long[] { 3, 4 })), 1, 2, 3, 4));
        cases.add(pipeline("factory/iterate", () -> LongStream.iterate((long) 1, v -> v < 4, v -> (long) (v + 1)), 1, 2, 3));
        cases.add(pipeline("factory/generateLimited", () -> LongStream.generate(() -> (long) 5).limit(3), 5, 5, 5));
        cases.add(pipeline("factory/zip", () -> LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, (a, b) -> (long) (a + b)), 11, 22));
        cases.add(pipeline("factory/zipWithDefaults",
                () -> LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, (long) 0, (long) 0, (a, b) -> (long) (a + b)), 11, 22, 3));
        cases.add(pipeline("factory/zipThree",
                () -> LongStream.zip(new long[] { 1, 2 }, new long[] { 10 }, new long[] { 20, 30, 40 }, (a, b, c) -> (long) (a + b + c)), 31));
        cases.add(pipeline("factory/zipThreeWithDefaults", () -> LongStream.zip(new long[] { 1, 2 }, new long[] { 10 }, new long[] { 20, 30, 40 }, (long) 0,
                (long) 0, (long) 0, (a, b, c) -> (long) (a + b + c)), 31, 32, 40));
        cases.add(
                pipeline("factory/merge",
                        () -> LongStream.merge(new long[] { 1, 3 }, new long[] { 2, 4 },
                                (a, b) -> a <= b ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND),
                        1, 2, 3, 4));
        cases.add(pipeline("factory/range", () -> LongStream.range((long) 1, (long) 4), 1, 2, 3));
        cases.add(pipeline("factory/rangeStep", () -> LongStream.range((long) 1, (long) 7, (long) 2), 1, 3, 5));
        cases.add(pipeline("factory/rangeEmpty", () -> LongStream.range((long) 1, (long) 1)));
        cases.add(pipeline("factory/rangeClosed", () -> LongStream.rangeClosed((long) 1, (long) 4), 1, 2, 3, 4));
        cases.add(pipeline("factory/rangeClosedStep", () -> LongStream.rangeClosed((long) 1, (long) 5, (long) 2), 1, 3, 5));
        // A sorted array slice exercises the array implementation's sorted paths directly.
        cases.add(pipeline("sortedArray/distinct", () -> new ArrayLongStream(new long[] { 0, 1, 1, 2, 2, 3, 9 }, 1, 6, true, null).distinct(), 1, 2, 3));
        cases.add(pipeline("sortedArray/top", () -> new ArrayLongStream(new long[] { 0, 1, 1, 2, 2, 3, 9 }, 1, 6, true, null).top(2), 2, 3));
        cases.add(pipeline("sortedArray/zeroTop", () -> new ArrayLongStream(new long[] { 1, 2, 3 }, true, null).top(0)));
        cases.add(pipeline("emptyArray/ifEmpty", () -> LongStream.of(new long[0]).ifEmpty(() -> {
        })));
        // Read parallel pipelines through their iterators, including next() after exhaustion.
        for (boolean iterator : new boolean[] { false, true }) {
            for (int threads : new int[] { 1, 3 }) {
                String prefix = "parallel/" + iterator + "/" + threads + "/";
                cases.add(pipeline(prefix + "map", () -> source(iterator).parallel(threads).map(v -> (long) (v + 1)), 2, 3, 3, 4, 5));
                cases.add(pipeline(prefix + "filter", () -> source(iterator).parallel(threads).filter(v -> v >= 3), 3, 4));
                cases.add(pipeline(prefix + "takeWhile", () -> source(iterator).parallel(threads).takeWhile(v -> v < 3), 1, 2, 2));
                cases.add(pipeline(prefix + "dropWhile", () -> source(iterator).parallel(threads).dropWhile(v -> v < 3), 3, 4));
                cases.add(pipeline(prefix + "flatMap", () -> source(iterator).parallel(threads).flatMap(v -> v == 2 ? LongStream.empty() : LongStream.of(v)), 1,
                        3, 4));
                cases.add(pipeline(prefix + "flatMapArray", () -> source(iterator).parallel(threads).flatMapArray(v -> v == 2 ? null : new long[] { v }), 1, 3,
                        4));
                cases.add(pipeline(prefix + "flatmap", () -> source(iterator).parallel(threads).flatmap(v -> v == 2 ? List.of() : List.of(v)), 1, 3, 4));
                cases.add(pipeline(prefix + "reverseSorted", () -> source(iterator).parallel(threads).reverseSorted(), 4, 3, 2, 2, 1));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void nextWithoutHasNextReturnsValuesAndRejectsExhaustion(String name, Supplier<LongStream> factory, List<Long> expected) {
        try (LongStream stream = factory.get()) {
            LongIteratorEx iter = stream.iteratorEx();
            List<Long> actual = new ArrayList<>();
            for (int i = 0; i < expected.size(); i++) {
                actual.add(iter.nextLong());
            }
            assertValues(name, expected, actual);
            assertThrows(NoSuchElementException.class, iter::nextLong);
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::nextLong);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void advanceAndCountConsumeExactlyTheRemainingElements(String name, Supplier<LongStream> factory, List<Long> expected) {
        for (long skip : new long[] { 0, 1, 2, expected.size(), Long.MAX_VALUE }) {
            try (LongStream stream = factory.get()) {
                LongIteratorEx iter = stream.iteratorEx();
                iter.advance(0);
                iter.advance(skip);
                int offset = (int) Math.min(skip, expected.size());
                assertEquals(expected.size() - offset, iter.count(), "skip=" + skip);
                assertEquals(0, iter.count());
                assertFalse(iter.hasNext());
                assertThrows(NoSuchElementException.class, iter::nextLong);
            }
            try (LongStream stream = factory.get()) {
                LongIteratorEx iter = stream.iteratorEx();
                iter.advance(skip);
                List<Long> actual = new ArrayList<>();
                while (iter.hasNext()) {
                    actual.add(iter.nextLong());
                }
                assertRemaining(name, expected, skip, actual);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void toListConsumesTheUnvisitedSuffix(String name, Supplier<LongStream> factory, List<Long> expected) {
        try (LongStream stream = factory.get()) {
            LongIteratorEx iter = stream.iteratorEx();
            List<Long> actual = new ArrayList<>();
            if (!expected.isEmpty()) {
                actual.add(iter.nextLong());
            }
            for (long value : iter.toList().toArray()) {
                actual.add(value);
            }
            assertValues(name, expected, actual);
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
        }
    }

    static java.util.stream.Stream<Arguments> failingTerminals() {
        List<Arguments> cases = new ArrayList<>();
        for (boolean iterator : new boolean[] { false, true }) {
            for (int mode = 0; mode < 4; mode++) {
                for (String operation : List.of("forEach", "forEachIndexed", "anyMatch", "allMatch", "noneMatch", "findFirst", "findAny", "findLast", "reduce",
                        "reduceIdentity", "collectSupplier", "collectAccumulator", "toCollection", "toMultiset", "toMap")) {
                    cases.add(Arguments.of(iterator, mode, operation));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "iterator={0}, parallelMode={1}, {2}")
    @MethodSource("failingTerminals")
    void callbackFailureClosesSourceExactlyOnce(boolean iterator, int mode, String operation) {
        AtomicInteger closes = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("callback failed");
        LongStream original = source(iterator).onClose(closes::incrementAndGet);
        LongStream stream = mode == 0 ? original
                : original.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
        try (stream) {
            RuntimeException actual = assertThrows(RuntimeException.class, () -> {
                switch (operation) {
                    case "forEach" -> stream.forEach(v -> {
                        throw failure;
                    });
                    case "forEachIndexed" -> stream.forEachIndexed((i, v) -> {
                        throw failure;
                    });
                    case "anyMatch" -> stream.anyMatch(v -> {
                        throw failure;
                    });
                    case "allMatch" -> stream.allMatch(v -> {
                        throw failure;
                    });
                    case "noneMatch" -> stream.noneMatch(v -> {
                        throw failure;
                    });
                    case "findFirst" -> stream.findFirst(v -> {
                        throw failure;
                    });
                    case "findAny" -> stream.findAny(v -> {
                        throw failure;
                    });
                    case "findLast" -> stream.findLast(v -> {
                        throw failure;
                    });
                    case "reduce" -> stream.reduce((a, b) -> {
                        throw failure;
                    });
                    case "reduceIdentity" -> stream.reduce((long) 0, (a, b) -> {
                        throw failure;
                    });
                    case "collectSupplier" -> stream.collect(() -> {
                        throw failure;
                    }, (List<Long> a, long v) -> a.add(v), List::addAll);
                    case "collectAccumulator" -> stream.collect(ArrayList<Long>::new, (a, v) -> {
                        throw failure;
                    }, List::addAll);
                    case "toCollection" -> stream.toCollection(() -> {
                        throw failure;
                    });
                    case "toMultiset" -> stream.toMultiset(() -> {
                        throw failure;
                    });
                    case "toMap" -> stream.toMap(v -> {
                        throw failure;
                    }, v -> v);
                    default -> throw new AssertionError(operation);
                }
            });
            assertSame(failure, actual);
            assertEquals(1, closes.get());
            assertThrows(IllegalStateException.class, stream::count);
        }
        assertEquals(1, closes.get());
    }
}
