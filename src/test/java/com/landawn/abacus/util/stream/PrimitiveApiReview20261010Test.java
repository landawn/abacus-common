package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ShortIterator;

class PrimitiveApiReview20261010Test extends TestBase {
    @Test
    void chunkAdvanceCrossesBothChunkSizeRegions() {
        final int[][] expected = { { 4, 408, 811, 1114, 1417 }, { 3, 306, 609, 913, 1317 } };

        for (int order = 0; order < 2; order++) {
            for (int skipped = 0; skipped <= 5; skipped++) {
                try (IntStream stream = IntStream.splitByChunkCount(17, 5, order == 1, (from, to) -> from * 100 + to)) {
                    final IntIteratorEx iter = stream.iteratorEx();
                    iter.advance(skipped);
                    assertArrayEquals(java.util.Arrays.copyOfRange(expected[order], skipped, 5), iter.toArray());
                    assertFalse(iter.hasNext());
                }
            }
        }
    }

    @Test
    void chunkAdvanceHandlesPartialTraversalOversizedSkipsAndMaximumSizes() {
        for (boolean smallerFirst : new boolean[] { false, true }) {
            final AtomicInteger mapped = new AtomicInteger();

            try (IntStream stream = IntStream.splitByChunkCount(Integer.MAX_VALUE, Integer.MAX_VALUE, smallerFirst, (from, to) -> {
                mapped.incrementAndGet();
                assertEquals(from + 1, to);
                return from;
            })) {
                final IntIteratorEx iter = stream.iteratorEx();
                iter.advance(0);
                iter.advance(-1);
                assertEquals(0, iter.nextInt());
                iter.advance(Integer.MAX_VALUE - 3L);
                assertEquals(Integer.MAX_VALUE - 2, iter.nextInt());
                assertEquals(2, mapped.get());
                iter.advance(Long.MAX_VALUE);
                assertFalse(iter.hasNext());
                assertEquals(0, iter.count());
                assertThrows(NoSuchElementException.class, iter::nextInt);
            }

            try (IntStream stream = IntStream.splitByChunkCount(3, Integer.MAX_VALUE, smallerFirst, (from, to) -> from)) {
                final IntIteratorEx iter = stream.iteratorEx();
                iter.advance(2);
                assertEquals(2, iter.nextInt());
                assertFalse(iter.hasNext());
            }

            try (IntStream stream = IntStream.splitByChunkCount(Integer.MAX_VALUE, 2, smallerFirst, (from, to) -> from)) {
                final IntIteratorEx iter = stream.iteratorEx();
                iter.advance(1);
                assertEquals(smallerFirst ? Integer.MAX_VALUE / 2 : Integer.MAX_VALUE / 2 + 1, iter.nextInt());
                assertFalse(iter.hasNext());
            }
        }
    }

    @Test
    void finiteAndInfiniteCyclesReplayValuesAndCloseUpstreamOnce() {
        for (Supplier<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory : sources()) {
            for (boolean finite : new boolean[] { false, true }) {
                final AtomicInteger closes = new AtomicInteger();
                final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> source = factory.get().onClose(closes::incrementAndGet);
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> cycle = finite ? source.cycled(2) : source.cycled().limit(7)) {
                    assertEquals(finite ? List.of(1, 2, 3, 1, 2, 3) : List.of(1, 2, 3, 1, 2, 3, 1), intValues(cycle.toList()));
                    assertEquals(1, closes.get());
                }
                assertEquals(1, closes.get());

                final AtomicInteger partialCloses = new AtomicInteger();
                final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> partialSource = factory.get().onClose(partialCloses::incrementAndGet);
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> cycle = finite ? partialSource.cycled(2) : partialSource.cycled()) {
                    final Iterator<?> iterator = cycle.iterator();
                    for (int i = 0; i < 4; i++) {
                        assertEquals(i % 3 + 1, intValue(iterator.next()));
                    }
                }
                assertEquals(1, partialCloses.get());
            }
        }
    }

    @Test
    void shortCircuitAndFailingTerminalsCloseCycleSources() {
        for (Supplier<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory : sources()) {
            final AtomicInteger firstPassCloses = new AtomicInteger();
            try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> cycle = factory.get().onClose(firstPassCloses::incrementAndGet).cycled()) {
                assertEquals(List.of(1), intValues(cycle.limit(1).toList()));
                assertEquals(1, firstPassCloses.get());
            }
            assertEquals(1, firstPassCloses.get());

            final AtomicInteger replayCloses = new AtomicInteger();
            final AtomicInteger seen = new AtomicInteger();
            final IllegalStateException failure = new IllegalStateException("stop during replay");
            try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> cycle = factory.get().onClose(replayCloses::incrementAndGet).cycled(3)) {
                assertSame(failure, assertThrows(IllegalStateException.class, () -> failDuringReplay(cycle, seen, failure)));
                assertEquals(4, seen.get());
                assertEquals(1, replayCloses.get());
            }
            assertEquals(1, replayCloses.get());
        }
    }

    @Test
    void emptyAndZeroOrOneRoundCyclesCloseUpstreamOnce() {
        for (Supplier<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory : sources()) {
            for (int rounds : new int[] { 0, 1 }) {
                final AtomicInteger closes = new AtomicInteger();
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> cycle = factory.get().onClose(closes::incrementAndGet).cycled(rounds)) {
                    assertEquals(rounds == 0 ? List.of() : List.of(1, 2, 3), intValues(cycle.toList()));
                    assertEquals(1, closes.get());
                }
                assertEquals(1, closes.get());
            }

            for (boolean finite : new boolean[] { false, true }) {
                final AtomicInteger closes = new AtomicInteger();
                final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> source = factory.get().limit(0).onClose(closes::incrementAndGet);
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> cycle = finite ? source.cycled(3) : source.cycled()) {
                    assertEquals(List.of(), cycle.toList());
                    assertEquals(1, closes.get());
                }
                assertEquals(1, closes.get());
            }
        }
    }

    private static <T> void failDuringReplay(final BaseStream<T, ?, ?, ?, ?, ?, ?, ?> cycle, final AtomicInteger seen, final IllegalStateException failure) {
        cycle.toCollection(() -> new ArrayList<T>() {
            @Override
            public boolean add(final T value) {
                assertEquals(seen.get() % 3 + 1, intValue(value));
                if (seen.incrementAndGet() == 4) {
                    throw failure;
                }
                return super.add(value);
            }
        });
    }

    private static List<Integer> intValues(final List<?> values) {
        return values.stream().map(PrimitiveApiReview20261010Test::intValue).toList();
    }

    private static int intValue(Object value) {
        return value instanceof Character c ? c : ((Number) value).intValue();
    }

    private static List<Supplier<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>>> sources() {
        return List.of(() -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)), () -> CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3)),
                () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)), () -> IntStream.of(IntIterator.of(1, 2, 3)),
                () -> LongStream.of(LongIterator.of(1, 2, 3)), () -> FloatStream.of(FloatIterator.of(1, 2, 3)),
                () -> DoubleStream.of(DoubleIterator.of(1, 2, 3)));
    }

}
