package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ParallelArrayPartitionTest extends TestBase {
    private static final BaseStream.ParallelSettings ARRAY_SPLIT = BaseStream.ParallelSettings.builder()
            .maxThreadNum(8)
            .splitStrategy(BaseStream.SplitStrategy.ARRAY)
            .build();

    @Test
    void highIndexSlicesStayInsideTheirArrayRange() {
        final int from = Integer.MAX_VALUE - 100;
        final int to = Integer.MAX_VALUE - 8;
        int covered = 0;
        int empty = 0;
        for (int worker = 0; worker < 91; worker++) {
            final int start = StreamBase.arraySliceBoundary(from, to, worker, 2);
            final int end = StreamBase.arraySliceBoundary(from, to, worker + 1, 2);
            assertTrue(start >= from && start <= to, "start for worker " + worker);
            assertTrue(end >= start && end <= to, "end for worker " + worker);
            covered += end - start;
            empty += start == end ? 1 : 0;
        }
        assertEquals(92, covered);
        assertEquals(45, empty);
    }

    @Test
    void boundaryMultiplicationDoesNotOverflow() {
        final int to = Integer.MAX_VALUE - 8;
        assertEquals(to, StreamBase.arraySliceBoundary(0, to, 100, 21_474_837));
        assertEquals(to, StreamBase.arraySliceBoundary(0, to, Integer.MAX_VALUE, 2));
    }

    @Test
    void unevenAndEmptySlicesMeetWithoutGapsOrOverlap() {
        final int[] boundaries = new int[5];
        for (int i = 0; i < boundaries.length; i++) {
            boundaries[i] = StreamBase.arraySliceBoundary(4, 9, i, 2);
        }
        assertArrayEquals(new int[] { 4, 6, 8, 9, 9 }, boundaries);
        assertEquals(12, StreamBase.arraySliceBoundary(12, 12, 5, 1));
        assertEquals(9, StreamBase.arraySliceBoundary(4, 9, 8, 1));
    }

    @Test
    void objectOperationsHandleMoreWorkersThanNonemptySlices() {
        final Integer[] values = { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 };
        assertEquals(List.of(2, 4, 6, 8, 10, 12, 14, 16, 18), Stream.of(values, 1, 10).parallel(ARRAY_SPLIT).map(value -> value * 2).sorted().toList());
        assertEquals(45, Stream.of(values, 1, 10).parallel(ARRAY_SPLIT).reduce(0, Integer::sum, Integer::sum));
        assertEquals(3, Stream.of(values, 1, 10).parallel(ARRAY_SPLIT).findFirst(value -> value % 3 == 0).get());
        assertEquals(9, Stream.of(values, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        final AtomicInteger count = new AtomicInteger();
        Stream.of(values, 1, 10).parallel(ARRAY_SPLIT).forEach(value -> count.incrementAndGet());
        assertEquals(9, count.get());
    }

    @Test
    void objectTransformationsHandleEmptyWorkerSlices() {
        assertEquals(List.of(2, 4, 6, 8), unevenObjectStream().filter(value -> value % 2 == 0).sorted().toList());
        assertEquals(45, unevenObjectStream().mapToByte(value -> (byte) value.intValue()).sum());
        assertEquals(45, unevenObjectStream().mapToChar(value -> (char) value.intValue()).sum());
        assertEquals(45, unevenObjectStream().mapToShort(value -> (short) value.intValue()).sum());
        assertEquals(45, unevenObjectStream().mapToInt(Integer::intValue).sum());
        assertEquals(45, unevenObjectStream().mapToLong(Integer::longValue).sum());
        assertEquals(45, unevenObjectStream().mapToFloat(Integer::floatValue).sum());
        assertEquals(45, unevenObjectStream().mapToDouble(Integer::doubleValue).sum());

        final AtomicInteger objectCloses = new AtomicInteger();
        assertEquals(18, unevenObjectStream().flatMap(value -> Stream.of(value, value).onClose(objectCloses::incrementAndGet)).count());
        assertEquals(9, objectCloses.get());
        final AtomicInteger primitiveCloses = new AtomicInteger();
        assertEquals(90, unevenObjectStream().flatMapToInt(value -> IntStream.of(value, value).onClose(primitiveCloses::incrementAndGet)).sum());
        assertEquals(9, primitiveCloses.get());
        assertEquals(90, unevenObjectStream().flatMapToDouble(value -> DoubleStream.of(value, value)).sum());
    }

    @Test
    void objectCollectorsAndPredicatesHandleEmptyWorkerSlices() {
        final List<Integer> collected = unevenObjectStream().collect(ArrayList<Integer>::new, List::add, List::addAll);
        collected.sort(Integer::compare);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), collected);
        assertEquals(45, unevenObjectStream().reduce(Integer::sum).get());
        assertFalse(unevenObjectStream().anyMatch(value -> value == 99));
        assertTrue(unevenObjectStream().allMatch(value -> value > 0 && value < 10));
        assertTrue(unevenObjectStream().noneMatch(value -> value == 99));
        assertTrue(unevenObjectStream().hasMatchCountBetween(3, 3, value -> value % 3 == 0));
        assertEquals(0, unevenObjectStream().findAny(value -> value % 3 == 0).get() % 3);
    }

    @Test
    void callbackFailureClosesAnUnevenPartitionedSource() {
        final IllegalStateException failure = new IllegalStateException("worker callback");
        final AtomicInteger objectCloses = new AtomicInteger();
        assertSame(failure, assertThrows(IllegalStateException.class, () -> unevenObjectStream().onClose(objectCloses::incrementAndGet).forEach(value -> {
            if (value == 4) {
                throw failure;
            }
        })));
        assertEquals(1, objectCloses.get());

        final AtomicInteger primitiveCloses = new AtomicInteger();
        assertSame(failure,
                assertThrows(IllegalStateException.class,
                        () -> IntStream.of(new int[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10)
                                .parallel(ARRAY_SPLIT)
                                .onClose(primitiveCloses::incrementAndGet)
                                .forEach(value -> {
                                    if (value == 4) {
                                        throw failure;
                                    }
                                })));
        assertEquals(1, primitiveCloses.get());
    }

    private static Stream<Integer> unevenObjectStream() {
        return Stream.of(new Integer[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT);
    }

    @Test
    void primitiveTerminalsHandleUnevenArraySlices() {
        assertEquals(45, ByteStream.of(new byte[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals((byte) 9,
                ByteStream.of(new byte[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        assertEquals(45, CharStream.of(new char[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals((char) 9,
                CharStream.of(new char[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        assertEquals(45, ShortStream.of(new short[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals((short) 9,
                ShortStream.of(new short[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        assertEquals(45, IntStream.of(new int[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals(9, IntStream.of(new int[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        assertEquals(45, LongStream.of(new long[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals(9, LongStream.of(new long[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        assertEquals(45, FloatStream.of(new float[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals(9, FloatStream.of(new float[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
        assertEquals(45, DoubleStream.of(new double[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).sum());
        assertEquals(9,
                DoubleStream.of(new double[] { 99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 }, 1, 10).parallel(ARRAY_SPLIT).findLast(value -> value % 3 == 0).get());
    }
}
