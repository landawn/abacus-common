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
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

/** Contracts for lazy iteration, bulk consumption, and terminal failure cleanup. */
public class IntStreamIteratorContractTest extends TestBase {
    private static IntStream source(boolean iterator) {
        int[] values = { 1, 2, 2, 3, 4 };
        return iterator ? IntStream.of(IntIterator.of(values)) : IntStream.of(values);
    }

    private static Arguments pipeline(String name, Supplier<IntStream> factory, int... values) {
        List<Integer> expected = new ArrayList<>();
        for (int value : values) {
            expected.add((int) value);
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
            cases.add(pipeline(prefix + "map", () -> source(iterator).map(v -> (int) (v + 1)), 2, 3, 3, 4, 5));
            cases.add(pipeline(prefix + "flatMap", () -> source(iterator).flatMap(v -> v == 2 ? IntStream.empty() : IntStream.of(v)), 1, 3, 4));
            cases.add(pipeline(prefix + "flatMapArray", () -> source(iterator).flatMapArray(v -> v == 2 ? null : new int[] { v }), 1, 3, 4));
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
            cases.add(pipeline(prefix + "scan", () -> source(iterator).scan((a, b) -> (int) (a + b)), 1, 3, 5, 8, 12));
            cases.add(pipeline(prefix + "scanWithIdentity", () -> source(iterator).scan((int) 0, true, (a, b) -> (int) (a + b)), 0, 1, 3, 5, 8, 12));
            cases.add(pipeline(prefix + "top", () -> source(iterator).top(1), 4));
            cases.add(pipeline(prefix + "sortedTop", () -> source(iterator).sorted().top(1), 4));
            cases.add(pipeline(prefix + "allTop", () -> source(iterator).top(10), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "zeroTop", () -> source(iterator).top(0)));
            cases.add(pipeline(prefix + "appendNonempty", () -> source(iterator).appendIfEmpty((int) 9), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "appendEmpty", () -> source(iterator).limit(0).appendIfEmpty((int) 9), 9));
            cases.add(pipeline(prefix + "emptyFilter", () -> source(iterator).filter(v -> false)));
            cases.add(pipeline(prefix + "cycled", () -> source(iterator).limit(2).cycled(2), 1, 2, 1, 2));
        }
        cases.add(pipeline("factory/repeat", () -> IntStream.repeat((int) 7, 3), 7, 7, 7));
        cases.add(pipeline("factory/repeatEmpty", () -> IntStream.repeat((int) 7, 0)));
        cases.add(pipeline("factory/defer", () -> IntStream.defer(() -> IntStream.of(new int[] { 1, 2, 3 })), 1, 2, 3));
        cases.add(pipeline("factory/deferEmpty", () -> IntStream.defer(IntStream::empty)));
        cases.add(pipeline("factory/flatten", () -> IntStream.flatten(new int[][] { { 1, 2 }, null, { 3 }, { 4, 5 } }), 1, 2, 3, 4, 5));
        cases.add(pipeline("factory/flattenVertical", () -> IntStream.flatten(new int[][] { { 1, 2 }, null, { 3 }, { 4, 5 } }, true), 1, 3, 4, 2, 5));
        cases.add(pipeline("factory/flattenAligned", () -> IntStream.flatten(new int[][] { { 1, 2 }, { 3 }, { 4, 5 } }, (int) 0, true), 1, 3, 4, 2, 0, 5));
        cases.add(pipeline("factory/flattenThreeDimensions", () -> IntStream.flatten(new int[][][] { { { 1 }, { 2, 3 } }, null, { { 4 } } }), 1, 2, 3, 4));
        cases.add(pipeline("factory/concatArrays", () -> IntStream.concat(new int[] { 1, 2 }, new int[0], new int[] { 3, 4 }), 1, 2, 3, 4));
        cases.add(pipeline("factory/concatArrayList", () -> IntStream.concat(java.util.Arrays.asList(new int[] { 1, 2 }, new int[0], new int[] { 3, 4 })), 1, 2,
                3, 4));
        cases.add(pipeline("factory/concatIterators",
                () -> IntStream.concatIterators(List.of(IntIterator.of(new int[] { 1, 2 }), IntIterator.empty(), IntIterator.of(new int[] { 3 }))), 1, 2, 3));
        cases.add(pipeline("factory/concatStreams",
                () -> IntStream.concat(IntStream.of(new int[] { 1, 2 }), IntStream.empty(), IntStream.of(new int[] { 3, 4 })), 1, 2, 3, 4));
        cases.add(pipeline("factory/iterate", () -> IntStream.iterate((int) 1, v -> v < 4, v -> (int) (v + 1)), 1, 2, 3));
        cases.add(pipeline("factory/generateLimited", () -> IntStream.generate(() -> (int) 5).limit(3), 5, 5, 5));
        cases.add(pipeline("factory/zip", () -> IntStream.zip(new int[] { 1, 2, 3 }, new int[] { 10, 20 }, (a, b) -> (int) (a + b)), 11, 22));
        cases.add(pipeline("factory/zipWithDefaults",
                () -> IntStream.zip(new int[] { 1, 2, 3 }, new int[] { 10, 20 }, (int) 0, (int) 0, (a, b) -> (int) (a + b)), 11, 22, 3));
        cases.add(pipeline("factory/zipThree",
                () -> IntStream.zip(new int[] { 1, 2 }, new int[] { 10 }, new int[] { 20, 30, 40 }, (a, b, c) -> (int) (a + b + c)), 31));
        cases.add(pipeline("factory/zipThreeWithDefaults",
                () -> IntStream.zip(new int[] { 1, 2 }, new int[] { 10 }, new int[] { 20, 30, 40 }, (int) 0, (int) 0, (int) 0, (a, b, c) -> (int) (a + b + c)),
                31, 32, 40));
        cases.add(
                pipeline("factory/merge",
                        () -> IntStream.merge(new int[] { 1, 3 }, new int[] { 2, 4 },
                                (a, b) -> a <= b ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND),
                        1, 2, 3, 4));
        cases.add(pipeline("factory/range", () -> IntStream.range((int) 1, (int) 4), 1, 2, 3));
        cases.add(pipeline("factory/rangeStep", () -> IntStream.range((int) 1, (int) 7, (int) 2), 1, 3, 5));
        cases.add(pipeline("factory/rangeEmpty", () -> IntStream.range((int) 1, (int) 1)));
        cases.add(pipeline("factory/rangeClosed", () -> IntStream.rangeClosed((int) 1, (int) 4), 1, 2, 3, 4));
        cases.add(pipeline("factory/rangeClosedStep", () -> IntStream.rangeClosed((int) 1, (int) 5, (int) 2), 1, 3, 5));
        // A sorted array slice exercises the array implementation's sorted paths directly.
        cases.add(pipeline("sortedArray/distinct", () -> new ArrayIntStream(new int[] { 0, 1, 1, 2, 2, 3, 9 }, 1, 6, true, null).distinct(), 1, 2, 3));
        cases.add(pipeline("sortedArray/top", () -> new ArrayIntStream(new int[] { 0, 1, 1, 2, 2, 3, 9 }, 1, 6, true, null).top(2), 2, 3));
        cases.add(pipeline("sortedArray/zeroTop", () -> new ArrayIntStream(new int[] { 1, 2, 3 }, true, null).top(0)));
        cases.add(pipeline("emptyArray/ifEmpty", () -> IntStream.of(new int[0]).ifEmpty(() -> {
        })));
        // Read parallel pipelines through their iterators, including next() after exhaustion.
        for (boolean iterator : new boolean[] { false, true }) {
            for (int threads : new int[] { 1, 3 }) {
                String prefix = "parallel/" + iterator + "/" + threads + "/";
                cases.add(pipeline(prefix + "map", () -> source(iterator).parallel(threads).map(v -> (int) (v + 1)), 2, 3, 3, 4, 5));
                cases.add(pipeline(prefix + "filter", () -> source(iterator).parallel(threads).filter(v -> v >= 3), 3, 4));
                cases.add(pipeline(prefix + "takeWhile", () -> source(iterator).parallel(threads).takeWhile(v -> v < 3), 1, 2, 2));
                cases.add(pipeline(prefix + "dropWhile", () -> source(iterator).parallel(threads).dropWhile(v -> v < 3), 3, 4));
                cases.add(pipeline(prefix + "flatMap", () -> source(iterator).parallel(threads).flatMap(v -> v == 2 ? IntStream.empty() : IntStream.of(v)), 1,
                        3, 4));
                cases.add(pipeline(prefix + "flatMapArray", () -> source(iterator).parallel(threads).flatMapArray(v -> v == 2 ? null : new int[] { v }), 1, 3,
                        4));
                cases.add(pipeline(prefix + "flatmap", () -> source(iterator).parallel(threads).flatmap(v -> v == 2 ? List.of() : List.of(v)), 1, 3, 4));
                cases.add(pipeline(prefix + "reverseSorted", () -> source(iterator).parallel(threads).reverseSorted(), 4, 3, 2, 2, 1));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void nextWithoutHasNextReturnsValuesAndRejectsExhaustion(String name, Supplier<IntStream> factory, List<Integer> expected) {
        try (IntStream stream = factory.get()) {
            IntIteratorEx iter = stream.iteratorEx();
            List<Integer> actual = new ArrayList<>();
            for (int i = 0; i < expected.size(); i++) {
                actual.add(iter.nextInt());
            }
            assertValues(name, expected, actual);
            assertThrows(NoSuchElementException.class, iter::nextInt);
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::nextInt);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void advanceAndCountConsumeExactlyTheRemainingElements(String name, Supplier<IntStream> factory, List<Integer> expected) {
        for (long skip : new long[] { 0, 1, 2, expected.size(), Long.MAX_VALUE }) {
            try (IntStream stream = factory.get()) {
                IntIteratorEx iter = stream.iteratorEx();
                iter.advance(0);
                iter.advance(skip);
                int offset = (int) Math.min(skip, expected.size());
                assertEquals(expected.size() - offset, iter.count(), "skip=" + skip);
                assertEquals(0, iter.count());
                assertFalse(iter.hasNext());
                assertThrows(NoSuchElementException.class, iter::nextInt);
            }
            try (IntStream stream = factory.get()) {
                IntIteratorEx iter = stream.iteratorEx();
                iter.advance(skip);
                List<Integer> actual = new ArrayList<>();
                while (iter.hasNext()) {
                    actual.add(iter.nextInt());
                }
                assertRemaining(name, expected, skip, actual);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void toListConsumesTheUnvisitedSuffix(String name, Supplier<IntStream> factory, List<Integer> expected) {
        try (IntStream stream = factory.get()) {
            IntIteratorEx iter = stream.iteratorEx();
            List<Integer> actual = new ArrayList<>();
            if (!expected.isEmpty()) {
                actual.add(iter.nextInt());
            }
            for (int value : iter.toList().toArray()) {
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
        IntStream original = source(iterator).onClose(closes::incrementAndGet);
        IntStream stream = mode == 0 ? original
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
                    case "reduceIdentity" -> stream.reduce((int) 0, (a, b) -> {
                        throw failure;
                    });
                    case "collectSupplier" -> stream.collect(() -> {
                        throw failure;
                    }, (List<Integer> a, int v) -> a.add(v), List::addAll);
                    case "collectAccumulator" -> stream.collect(ArrayList<Integer>::new, (a, v) -> {
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
