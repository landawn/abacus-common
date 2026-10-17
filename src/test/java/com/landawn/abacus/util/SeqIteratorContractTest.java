package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class SeqIteratorContractTest extends TestBase {
    record Pipeline(String name, Supplier<Seq<?, RuntimeException>> factory, List<?> expected) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static Seq<Integer, RuntimeException> source() {
        return Seq.of(1, 2, 2, 3, 4);
    }

    static List<Pipeline> pipelines() {
        return List.of(new Pipeline("booleanArray", () -> Seq.of(new boolean[] { true, false, true }), List.of(true, false, true)),
                new Pipeline("charArray", () -> Seq.of(new char[] { 'a', 'b', 'c' }), List.of('a', 'b', 'c')),
                new Pipeline("byteArray", () -> Seq.of(new byte[] { 1, 2, 3 }), List.of((byte) 1, (byte) 2, (byte) 3)),
                new Pipeline("shortArray", () -> Seq.of(new short[] { 1, 2, 3 }), List.of((short) 1, (short) 2, (short) 3)),
                new Pipeline("intArray", () -> Seq.of(new int[] { 1, 2, 3 }), List.of(1, 2, 3)),
                new Pipeline("longArray", () -> Seq.of(new long[] { 1, 2, 3 }), List.of(1L, 2L, 3L)),
                new Pipeline("floatArray", () -> Seq.of(new float[] { 1, 2, 3 }), List.of(1F, 2F, 3F)),
                new Pipeline("doubleArray", () -> Seq.of(new double[] { 1, 2, 3 }), List.of(1D, 2D, 3D)),
                new Pipeline("objectArray", SeqIteratorContractTest::source, List.of(1, 2, 2, 3, 4)),
                new Pipeline("iterable", () -> Seq.of(List.of(1, 2, 3)), List.of(1, 2, 3)),
                new Pipeline("iterator", () -> Seq.of(List.of(1, 2, 3).iterator()), List.of(1, 2, 3)),
                new Pipeline("enumeration", () -> Seq.of(Collections.enumeration(List.of(1, 2, 3))), List.of(1, 2, 3)),
                new Pipeline("filter", () -> source().filter(v -> v > 2), List.of(3, 4)),
                new Pipeline("map", () -> source().map(v -> v + 1), List.of(2, 3, 3, 4, 5)),
                new Pipeline("takeWhile", () -> source().takeWhile(v -> v < 3), List.of(1, 2, 2)),
                new Pipeline("dropWhile", () -> source().dropWhile(v -> v < 3), List.of(3, 4)), new Pipeline("step", () -> source().step(2), List.of(1, 2, 4)),
                new Pipeline("hugeStep", () -> source().step(Long.MAX_VALUE), List.of(1)),
                new Pipeline("skipAndLimit", () -> source().skipAndLimit(1, 2), List.of(2, 2)),
                new Pipeline("limit", () -> source().limit(3), List.of(1, 2, 2)), new Pipeline("skip", () -> source().skip(2), List.of(2, 3, 4)),
                new Pipeline("top", () -> source().top(1), List.of(4)), new Pipeline("topEmpty", () -> source().top(0), List.of()),
                new Pipeline("reversed", () -> source().reversed(), List.of(4, 3, 2, 2, 1)),
                new Pipeline("rotated", () -> source().rotated(2), List.of(3, 4, 1, 2, 2)),
                new Pipeline("sorted", () -> source().reversed().sorted(), List.of(1, 2, 2, 3, 4)),
                new Pipeline("distinct", () -> source().distinct(), List.of(1, 2, 3, 4)),
                new Pipeline("append", () -> source().append(5, 6), List.of(1, 2, 2, 3, 4, 5, 6)),
                new Pipeline("prepend", () -> source().prepend(-1, 0), List.of(-1, 0, 1, 2, 2, 3, 4)),
                new Pipeline("emptyAppendCollection", () -> source().limit(0).appendIfEmpty(List.of(8, 9)), List.of(8, 9)),
                new Pipeline("emptyAppendSupplier", () -> source().limit(0).appendIfEmpty(() -> Seq.of(8, 9)), List.of(8, 9)),
                new Pipeline("nonemptyAppend", () -> source().appendIfEmpty(List.of(9)), List.of(1, 2, 2, 3, 4)),
                new Pipeline("cycled", () -> source().limit(2).cycled(2), List.of(1, 2, 1, 2)),
                new Pipeline("repeat", () -> Seq.repeat(7, 3), List.of(7, 7, 7)), new Pipeline("range", () -> Seq.range(1, 6, 2), List.of(1, 3, 5)),
                new Pipeline("split", () -> source().split(2), List.of(List.of(1, 2), List.of(2, 3), List.of(4))),
                new Pipeline("splitAtIndex", () -> source().splitAt(2), List.of(List.of(1, 2), List.of(2, 3, 4))),
                new Pipeline("splitAtPredicate", () -> source().splitAt(v -> v == 3), List.of(List.of(1, 2, 2), List.of(3, 4))),
                new Pipeline("splitAtAbsentPredicate", () -> source().splitAt(v -> v == 9), List.of(List.of(1, 2, 2, 3, 4), List.of())),
                new Pipeline("splitAtZero", () -> source().splitAt(0), List.of(List.of(), List.of(1, 2, 2, 3, 4))),
                new Pipeline("sliding", () -> source().sliding(3, 2), List.of(List.of(1, 2, 2), List.of(2, 3, 4))),
                new Pipeline("slidingGap", () -> source().sliding(2, 3), List.of(List.of(1, 2), List.of(3, 4))));
    }

    @ParameterizedTest
    @MethodSource("pipelines")
    void nextWithoutHasNextPreservesValuesAndExhaustion(Pipeline pipeline) throws Exception {
        try (Seq<?, RuntimeException> seq = pipeline.factory.get()) {
            Throwables.Iterator<?, RuntimeException> iter = seq.iteratorEx();
            for (Object expected : pipeline.expected) {
                assertEquals(expected, materialize(iter.next()));
            }
            assertThrows(NoSuchElementException.class, iter::next);
            assertFalse(iter.hasNext());
        }
    }

    @ParameterizedTest
    @MethodSource("pipelines")
    void bulkAdvancePreservesTheRemainingSuffix(Pipeline pipeline) throws Exception {
        for (long skip : new long[] { 0, 1, 2, pipeline.expected.size(), Long.MAX_VALUE }) {
            try (Seq<?, RuntimeException> seq = pipeline.factory.get()) {
                Throwables.Iterator<?, RuntimeException> iter = seq.iteratorEx();
                iter.advance(0);
                iter.advance(skip);
                List<Object> actual = new ArrayList<>();
                while (iter.hasNext()) {
                    actual.add(materialize(iter.next()));
                }
                assertEquals(pipeline.expected.subList((int) Math.min(skip, pipeline.expected.size()), pipeline.expected.size()), actual, "skip=" + skip);
                assertThrows(NoSuchElementException.class, iter::next);
                iter.advance(Long.MAX_VALUE);
                assertFalse(iter.hasNext());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipelines")
    void countConsumesOnlyUnvisitedElements(Pipeline pipeline) throws Exception {
        for (int visited = 0; visited <= Math.min(2, pipeline.expected.size()); visited++) {
            try (Seq<?, RuntimeException> seq = pipeline.factory.get()) {
                Throwables.Iterator<?, RuntimeException> iter = seq.iteratorEx();
                for (int i = 0; i < visited; i++) {
                    assertEquals(pipeline.expected.get(i), materialize(iter.next()));
                }
                assertEquals(pipeline.expected.size() - visited, iter.count());
                assertEquals(0, iter.count());
                assertFalse(iter.hasNext());
            }
        }
    }

    private static Object materialize(Object value) throws Exception {
        return value instanceof Seq<?, ?> seq ? seq.toList() : value;
    }
}
