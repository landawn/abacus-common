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
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

/** Contracts for lazy iteration, bulk consumption, and terminal failure cleanup. */
public class CharStreamIteratorContractTest extends TestBase {
    private static CharStream source(boolean iterator) {
        char[] values = { 1, 2, 2, 3, 4 };
        return iterator ? CharStream.of(CharIterator.of(values)) : CharStream.of(values);
    }

    private static Arguments pipeline(String name, Supplier<CharStream> factory, int... values) {
        List<Character> expected = new ArrayList<>();
        for (int value : values) {
            expected.add((char) value);
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
            cases.add(pipeline(prefix + "map", () -> source(iterator).map(v -> (char) (v + 1)), 2, 3, 3, 4, 5));
            cases.add(pipeline(prefix + "flatMap", () -> source(iterator).flatMap(v -> v == 2 ? CharStream.empty() : CharStream.of(v)), 1, 3, 4));
            cases.add(pipeline(prefix + "flatMapArray", () -> source(iterator).flatMapArray(v -> v == 2 ? null : new char[] { v }), 1, 3, 4));
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
            cases.add(pipeline(prefix + "scan", () -> source(iterator).scan((a, b) -> (char) (a + b)), 1, 3, 5, 8, 12));
            cases.add(pipeline(prefix + "scanWithIdentity", () -> source(iterator).scan((char) 0, true, (a, b) -> (char) (a + b)), 0, 1, 3, 5, 8, 12));
            cases.add(pipeline(prefix + "appendNonempty", () -> source(iterator).appendIfEmpty((char) 9), 1, 2, 2, 3, 4));
            cases.add(pipeline(prefix + "appendEmpty", () -> source(iterator).limit(0).appendIfEmpty((char) 9), 9));
            cases.add(pipeline(prefix + "emptyFilter", () -> source(iterator).filter(v -> false)));
            cases.add(pipeline(prefix + "cycled", () -> source(iterator).limit(2).cycled(2), 1, 2, 1, 2));
        }
        cases.add(pipeline("factory/repeat", () -> CharStream.repeat((char) 7, 3), 7, 7, 7));
        cases.add(pipeline("factory/repeatEmpty", () -> CharStream.repeat((char) 7, 0)));
        cases.add(pipeline("factory/defer", () -> CharStream.defer(() -> CharStream.of(new char[] { 1, 2, 3 })), 1, 2, 3));
        cases.add(pipeline("factory/deferEmpty", () -> CharStream.defer(CharStream::empty)));
        cases.add(pipeline("factory/flatten", () -> CharStream.flatten(new char[][] { { 1, 2 }, null, { 3 }, { 4, 5 } }), 1, 2, 3, 4, 5));
        cases.add(pipeline("factory/flattenVertical", () -> CharStream.flatten(new char[][] { { 1, 2 }, null, { 3 }, { 4, 5 } }, true), 1, 3, 4, 2, 5));
        cases.add(pipeline("factory/flattenAligned", () -> CharStream.flatten(new char[][] { { 1, 2 }, { 3 }, { 4, 5 } }, (char) 0, true), 1, 3, 4, 2, 0, 5));
        cases.add(pipeline("factory/flattenThreeDimensions", () -> CharStream.flatten(new char[][][] { { { 1 }, { 2, 3 } }, null, { { 4 } } }), 1, 2, 3, 4));
        cases.add(pipeline("factory/concatArrays", () -> CharStream.concat(new char[] { 1, 2 }, new char[0], new char[] { 3, 4 }), 1, 2, 3, 4));
        cases.add(pipeline("factory/concatArrayList", () -> CharStream.concat(java.util.Arrays.asList(new char[] { 1, 2 }, new char[0], new char[] { 3, 4 })),
                1, 2, 3, 4));
        cases.add(pipeline("factory/concatIterators",
                () -> CharStream.concatIterators(List.of(CharIterator.of(new char[] { 1, 2 }), CharIterator.empty(), CharIterator.of(new char[] { 3 }))), 1, 2,
                3));
        cases.add(pipeline("factory/concatStreams",
                () -> CharStream.concat(CharStream.of(new char[] { 1, 2 }), CharStream.empty(), CharStream.of(new char[] { 3, 4 })), 1, 2, 3, 4));
        cases.add(pipeline("factory/iterate", () -> CharStream.iterate((char) 1, v -> v < 4, v -> (char) (v + 1)), 1, 2, 3));
        cases.add(pipeline("factory/generateLimited", () -> CharStream.generate(() -> (char) 5).limit(3), 5, 5, 5));
        cases.add(pipeline("factory/zip", () -> CharStream.zip(new char[] { 1, 2, 3 }, new char[] { 10, 20 }, (a, b) -> (char) (a + b)), 11, 22));
        cases.add(pipeline("factory/zipWithDefaults",
                () -> CharStream.zip(new char[] { 1, 2, 3 }, new char[] { 10, 20 }, (char) 0, (char) 0, (a, b) -> (char) (a + b)), 11, 22, 3));
        cases.add(pipeline("factory/zipThree",
                () -> CharStream.zip(new char[] { 1, 2 }, new char[] { 10 }, new char[] { 20, 30, 40 }, (a, b, c) -> (char) (a + b + c)), 31));
        cases.add(pipeline("factory/zipThreeWithDefaults", () -> CharStream.zip(new char[] { 1, 2 }, new char[] { 10 }, new char[] { 20, 30, 40 }, (char) 0,
                (char) 0, (char) 0, (a, b, c) -> (char) (a + b + c)), 31, 32, 40));
        cases.add(
                pipeline("factory/merge",
                        () -> CharStream.merge(new char[] { 1, 3 }, new char[] { 2, 4 },
                                (a, b) -> a <= b ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND),
                        1, 2, 3, 4));
        cases.add(pipeline("factory/range", () -> CharStream.range((char) 1, (char) 4), 1, 2, 3));
        cases.add(pipeline("factory/rangeStep", () -> CharStream.range((char) 1, (char) 7, (char) 2), 1, 3, 5));
        cases.add(pipeline("factory/rangeEmpty", () -> CharStream.range((char) 1, (char) 1)));
        cases.add(pipeline("factory/rangeClosed", () -> CharStream.rangeClosed((char) 1, (char) 4), 1, 2, 3, 4));
        cases.add(pipeline("factory/rangeClosedStep", () -> CharStream.rangeClosed((char) 1, (char) 5, (char) 2), 1, 3, 5));
        // A sorted array slice exercises the array implementation's sorted paths directly.
        cases.add(pipeline("sortedArray/distinct", () -> new ArrayCharStream(new char[] { 0, 1, 1, 2, 2, 3, 9 }, 1, 6, true, null).distinct(), 1, 2, 3));
        cases.add(pipeline("emptyArray/ifEmpty", () -> CharStream.of(new char[0]).ifEmpty(() -> {
        })));
        // Read parallel pipelines through their iterators, including next() after exhaustion.
        for (boolean iterator : new boolean[] { false, true }) {
            for (int threads : new int[] { 1, 3 }) {
                String prefix = "parallel/" + iterator + "/" + threads + "/";
                cases.add(pipeline(prefix + "map", () -> source(iterator).parallel(threads).map(v -> (char) (v + 1)), 2, 3, 3, 4, 5));
                cases.add(pipeline(prefix + "filter", () -> source(iterator).parallel(threads).filter(v -> v >= 3), 3, 4));
                cases.add(pipeline(prefix + "takeWhile", () -> source(iterator).parallel(threads).takeWhile(v -> v < 3), 1, 2, 2));
                cases.add(pipeline(prefix + "dropWhile", () -> source(iterator).parallel(threads).dropWhile(v -> v < 3), 3, 4));
                cases.add(pipeline(prefix + "flatMap", () -> source(iterator).parallel(threads).flatMap(v -> v == 2 ? CharStream.empty() : CharStream.of(v)), 1,
                        3, 4));
                cases.add(pipeline(prefix + "flatMapArray", () -> source(iterator).parallel(threads).flatMapArray(v -> v == 2 ? null : new char[] { v }), 1, 3,
                        4));
                cases.add(pipeline(prefix + "flatmap", () -> source(iterator).parallel(threads).flatmap(v -> v == 2 ? List.of() : List.of(v)), 1, 3, 4));
                cases.add(pipeline(prefix + "reverseSorted", () -> source(iterator).parallel(threads).reverseSorted(), 4, 3, 2, 2, 1));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void nextWithoutHasNextReturnsValuesAndRejectsExhaustion(String name, Supplier<CharStream> factory, List<Character> expected) {
        try (CharStream stream = factory.get()) {
            CharIteratorEx iter = stream.iteratorEx();
            List<Character> actual = new ArrayList<>();
            for (int i = 0; i < expected.size(); i++) {
                actual.add(iter.nextChar());
            }
            assertValues(name, expected, actual);
            assertThrows(NoSuchElementException.class, iter::nextChar);
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::nextChar);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void advanceAndCountConsumeExactlyTheRemainingElements(String name, Supplier<CharStream> factory, List<Character> expected) {
        for (long skip : new long[] { 0, 1, 2, expected.size(), Long.MAX_VALUE }) {
            try (CharStream stream = factory.get()) {
                CharIteratorEx iter = stream.iteratorEx();
                iter.advance(0);
                iter.advance(skip);
                int offset = (int) Math.min(skip, expected.size());
                assertEquals(expected.size() - offset, iter.count(), "skip=" + skip);
                assertEquals(0, iter.count());
                assertFalse(iter.hasNext());
                assertThrows(NoSuchElementException.class, iter::nextChar);
            }
            try (CharStream stream = factory.get()) {
                CharIteratorEx iter = stream.iteratorEx();
                iter.advance(skip);
                List<Character> actual = new ArrayList<>();
                while (iter.hasNext()) {
                    actual.add(iter.nextChar());
                }
                assertRemaining(name, expected, skip, actual);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void toListConsumesTheUnvisitedSuffix(String name, Supplier<CharStream> factory, List<Character> expected) {
        try (CharStream stream = factory.get()) {
            CharIteratorEx iter = stream.iteratorEx();
            List<Character> actual = new ArrayList<>();
            if (!expected.isEmpty()) {
                actual.add(iter.nextChar());
            }
            for (char value : iter.toList().toArray()) {
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
        CharStream original = source(iterator).onClose(closes::incrementAndGet);
        CharStream stream = mode == 0 ? original
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
                    case "reduceIdentity" -> stream.reduce((char) 0, (a, b) -> {
                        throw failure;
                    });
                    case "collectSupplier" -> stream.collect(() -> {
                        throw failure;
                    }, (List<Character> a, char v) -> a.add(v), List::addAll);
                    case "collectAccumulator" -> stream.collect(ArrayList<Character>::new, (a, v) -> {
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
