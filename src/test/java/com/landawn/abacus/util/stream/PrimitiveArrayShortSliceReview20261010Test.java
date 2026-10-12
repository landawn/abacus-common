package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Parallel primitive array stages on slices shorter than the requested worker count must still produce the complete
 * result. The slow-test subclass PrimitiveArrayWorkerBoundReview20261010Test re-runs every case and also checks that
 * no more workers are submitted than the slice has elements.
 */
class PrimitiveArrayShortSliceReview20261010Test extends TestBase {
    static final int REQUESTED_WORKERS = 32;

    /**
     * Runs one stage on an inline executor and returns how many worker tasks it submitted.
     */
    static int runInline(final double expectedSum, final ToDoubleFunction<Executor> action) {
        final AtomicInteger submissions = new AtomicInteger();
        // These tiny pipelines fit in the handoff buffer, so inline execution makes submission counts deterministic.
        final Executor executor = command -> {
            submissions.incrementAndGet();
            command.run();
        };

        assertEquals(expectedSum, action.applyAsDouble(executor));
        return submissions.get();
    }

    /**
     * Checks the stage result; {@code expectedWorkers} is verified only by the slow-test subclass.
     */
    void assertWorkers(final int expectedWorkers, final double expectedSum, final ToDoubleFunction<Executor> action) {
        runInline(expectedSum, action);
    }

    private static ByteStream bytes(final Executor executor) {
        return ByteStream.of(new byte[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void byteArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> bytes(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> bytes(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> bytes(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> bytes(executor).map(value -> (byte) (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> bytes(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> bytes(executor).flatMap(value -> ByteStream.of(value, (byte) (value + 10))).sequential().sum());
        assertWorkers(2, 3, executor -> bytes(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> bytes(executor).flatMapArray(value -> new byte[] { value, (byte) (value + 10) }).sequential().sum());
        assertWorkers(2, 3, executor -> bytes(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> bytes(executor).flatMapToObj(value -> Stream.of((int) value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> bytes(executor).flatmapToObj(value -> Arrays.asList((int) value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 3, executor -> bytes(executor).mapToInt(value -> (int) value).sequential().sum());
        assertWorkers(2, 6, executor -> bytes(executor).flatMapToInt(value -> IntStream.of(value, value)).sequential().sum());
    }

    private static CharStream chars(final Executor executor) {
        return CharStream.of(new char[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void charArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> chars(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> chars(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> chars(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> chars(executor).map(value -> (char) (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> chars(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> chars(executor).flatMap(value -> CharStream.of(value, (char) (value + 10))).sequential().sum());
        assertWorkers(2, 3, executor -> chars(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> chars(executor).flatMapArray(value -> new char[] { value, (char) (value + 10) }).sequential().sum());
        assertWorkers(2, 3, executor -> chars(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> chars(executor).flatMapToObj(value -> Stream.of((int) value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> chars(executor).flatmapToObj(value -> Arrays.asList((int) value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 3, executor -> chars(executor).mapToInt(value -> (int) value).sequential().sum());
        assertWorkers(2, 6, executor -> chars(executor).flatMapToInt(value -> IntStream.of(value, value)).sequential().sum());
    }

    private static ShortStream shorts(final Executor executor) {
        return ShortStream.of(new short[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void shortArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> shorts(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> shorts(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> shorts(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> shorts(executor).map(value -> (short) (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> shorts(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> shorts(executor).flatMap(value -> ShortStream.of(value, (short) (value + 10))).sequential().sum());
        assertWorkers(2, 3, executor -> shorts(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> shorts(executor).flatMapArray(value -> new short[] { value, (short) (value + 10) }).sequential().sum());
        assertWorkers(2, 3, executor -> shorts(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> shorts(executor).flatMapToObj(value -> Stream.of((int) value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> shorts(executor).flatmapToObj(value -> Arrays.asList((int) value, value + 10))
                        .sequential()
                        .mapToInt(Integer::intValue)
                        .sum());
        assertWorkers(2, 3, executor -> shorts(executor).mapToInt(value -> (int) value).sequential().sum());
        assertWorkers(2, 6, executor -> shorts(executor).flatMapToInt(value -> IntStream.of(value, value)).sequential().sum());
    }

    private static IntStream ints(final Executor executor) {
        return IntStream.of(new int[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void intArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> ints(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> ints(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).map(value -> (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> ints(executor).flatMap(value -> IntStream.of(value, value + 10)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> ints(executor).flatMapArray(value -> new int[] { value, value + 10 }).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> ints(executor).flatMapToObj(value -> Stream.of(value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> ints(executor).flatmapToObj(value -> Arrays.asList(value, value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToByte(value -> (byte) value).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).flatMapToByte(value -> ByteStream.of((byte) value, (byte) value)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToChar(value -> (char) value).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).flatMapToChar(value -> CharStream.of((char) value, (char) value)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToShort(value -> (short) value).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).flatMapToShort(value -> ShortStream.of((short) value, (short) value)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToLong(value -> (long) value).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).flatMapToLong(value -> LongStream.of(value, value)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToFloat(value -> (float) value).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).flatMapToFloat(value -> FloatStream.of(value, value)).sequential().sum());
        assertWorkers(2, 3, executor -> ints(executor).mapToDouble(value -> (double) value).sequential().sum());
        assertWorkers(2, 6, executor -> ints(executor).flatMapToDouble(value -> DoubleStream.of(value, value)).sequential().sum());
    }

    private static LongStream longs(final Executor executor) {
        return LongStream.of(new long[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void longArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> longs(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> longs(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> longs(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> longs(executor).map(value -> (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> longs(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> longs(executor).flatMap(value -> LongStream.of(value, value + 10)).sequential().sum());
        assertWorkers(2, 3, executor -> longs(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> longs(executor).flatMapArray(value -> new long[] { value, value + 10 }).sequential().sum());
        assertWorkers(2, 3, executor -> longs(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> longs(executor).flatMapToObj(value -> Stream.of((int) value, (int) value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> longs(executor).flatmapToObj(value -> Arrays.asList((int) value, (int) value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 3, executor -> longs(executor).mapToInt(value -> (int) value).sequential().sum());
        assertWorkers(2, 6, executor -> longs(executor).flatMapToInt(value -> IntStream.of((int) value, (int) value)).sequential().sum());
        assertWorkers(2, 3, executor -> longs(executor).mapToFloat(value -> (float) value).sequential().sum());
        assertWorkers(2, 6, executor -> longs(executor).flatMapToFloat(value -> FloatStream.of(value, value)).sequential().sum());
        assertWorkers(2, 3, executor -> longs(executor).mapToDouble(value -> (double) value).sequential().sum());
        assertWorkers(2, 6, executor -> longs(executor).flatMapToDouble(value -> DoubleStream.of(value, value)).sequential().sum());
    }

    private static FloatStream floats(final Executor executor) {
        return FloatStream.of(new float[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void floatArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> floats(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> floats(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> floats(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> floats(executor).map(value -> (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> floats(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> floats(executor).flatMap(value -> FloatStream.of(value, value + 10)).sequential().sum());
        assertWorkers(2, 3, executor -> floats(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> floats(executor).flatMapArray(value -> new float[] { value, value + 10 }).sequential().sum());
        assertWorkers(2, 3, executor -> floats(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> floats(executor).flatMapToObj(value -> Stream.of((int) value, (int) value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> floats(executor).flatmapToObj(value -> Arrays.asList((int) value, (int) value + 10))
                        .sequential()
                        .mapToInt(Integer::intValue)
                        .sum());
        assertWorkers(2, 3, executor -> floats(executor).mapToInt(value -> (int) value).sequential().sum());
        assertWorkers(2, 6, executor -> floats(executor).flatMapToInt(value -> IntStream.of((int) value, (int) value)).sequential().sum());
        assertWorkers(2, 3, executor -> floats(executor).mapToLong(value -> (long) value).sequential().sum());
        assertWorkers(2, 6, executor -> floats(executor).flatMapToLong(value -> LongStream.of((long) value, (long) value)).sequential().sum());
        assertWorkers(2, 3, executor -> floats(executor).mapToDouble(value -> (double) value).sequential().sum());
        assertWorkers(2, 6, executor -> floats(executor).flatMapToDouble(value -> DoubleStream.of(value, value)).sequential().sum());
    }

    private static DoubleStream doubles(final Executor executor) {
        return DoubleStream.of(new double[] { 9, 1, 2, 9 }, 1, 3).parallel(REQUESTED_WORKERS, executor);
    }

    @Test
    void doubleArrayStagesOnAShortSlice() {
        assertWorkers(2, 3, executor -> doubles(executor).filter(value -> true).sequential().sum());
        assertWorkers(2, 3, executor -> doubles(executor).takeWhile(value -> true).sequential().sum());
        assertWorkers(2, 2, executor -> doubles(executor).dropWhile(value -> value < 2).sequential().sum());
        assertWorkers(2, 6, executor -> doubles(executor).map(value -> (value * 2)).sequential().sum());
        assertWorkers(2, 3, executor -> doubles(executor).onEach(value -> {
        }).sequential().sum());
        assertWorkers(2, 26, executor -> doubles(executor).flatMap(value -> DoubleStream.of(value, value + 10)).sequential().sum());
        assertWorkers(2, 3, executor -> doubles(executor).flatmap(value -> Arrays.asList(value, null)).sequential().sum());
        assertWorkers(2, 26, executor -> doubles(executor).flatMapArray(value -> new double[] { value, value + 10 }).sequential().sum());
        assertWorkers(2, 3, executor -> doubles(executor).mapToObj(value -> (int) value).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> doubles(executor).flatMapToObj(value -> Stream.of((int) value, (int) value + 10)).sequential().mapToInt(Integer::intValue).sum());
        assertWorkers(2, 26,
                executor -> doubles(executor).flatmapToObj(value -> Arrays.asList((int) value, (int) value + 10))
                        .sequential()
                        .mapToInt(Integer::intValue)
                        .sum());
        assertWorkers(2, 3, executor -> doubles(executor).mapToInt(value -> (int) value).sequential().sum());
        assertWorkers(2, 6, executor -> doubles(executor).flatMapToInt(value -> IntStream.of((int) value, (int) value)).sequential().sum());
        assertWorkers(2, 3, executor -> doubles(executor).mapToLong(value -> (long) value).sequential().sum());
        assertWorkers(2, 6, executor -> doubles(executor).flatMapToLong(value -> LongStream.of((long) value, (long) value)).sequential().sum());
        assertWorkers(2, 3, executor -> doubles(executor).mapToFloat(value -> (float) value).sequential().sum());
        assertWorkers(2, 6, executor -> doubles(executor).flatMapToFloat(value -> FloatStream.of((float) value, (float) value)).sequential().sum());
    }

    @Test
    void emptyAndSingleElementArraysRunSequentially() {
        assertWorkers(0, 0, executor -> IntStream.empty().parallel(REQUESTED_WORKERS, executor).map(value -> value + 1).sequential().sum());
        assertWorkers(0, 2, executor -> IntStream.of(1).parallel(REQUESTED_WORKERS, executor).map(value -> value + 1).sequential().sum());
    }

    @Test
    void stagesKeepTheConfiguredDownstreamParallelism() {
        assertWorkers(2, 3, executor -> {
            try (IntStream mapped = ints(executor).map(value -> value)) {
                assertEquals(REQUESTED_WORKERS, mapped.maxThreadNum());
                return mapped.sequential().sum();
            }
        });
    }
}
