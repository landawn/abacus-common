package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.N;

@Tag("unit")
class StreamPerformanceRegressionTest extends TestBase {
    @Test
    void spinedBuffersGrowChunksAndKeepSnapshotIterators() throws Exception {
        final SpinedBuffer<Object> objects = new SpinedBuffer<>(1);
        final SpinedBuffer.OfInt ints = new SpinedBuffer.OfInt(1);
        final SpinedBuffer.OfLong longs = new SpinedBuffer.OfLong(1);
        final SpinedBuffer.OfDouble doubles = new SpinedBuffer.OfDouble(1);
        for (int i = 0; i < 100; i++) {
            objects.add(i % 3 == 0 ? null : Integer.valueOf(i));
            ints.add(i);
            longs.add(Long.MIN_VALUE + i);
            doubles.add(Double.longBitsToDouble(0x7ff8000000000000L + i));
        }
        final Iterator<Object> objectSnapshot = objects.iterator();
        final IntIterator intSnapshot = ints.iterator();
        final LongIterator longSnapshot = longs.iterator();
        final DoubleIterator doubleSnapshot = doubles.iterator();
        for (int i = 100; i < 1_000_000; i++) {
            objects.add(null);
            ints.add(i);
            longs.add(i);
            doubles.add(i);
        }
        for (int i = 0; i < 100; i++) {
            assertEquals(i % 3 == 0 ? null : Integer.valueOf(i), objectSnapshot.next());
            assertEquals(i, intSnapshot.nextInt());
            assertEquals(Long.MIN_VALUE + i, longSnapshot.nextLong());
            assertEquals(0x7ff8000000000000L + i, Double.doubleToRawLongBits(doubleSnapshot.nextDouble()));
        }
        assertFalse(objectSnapshot.hasNext());
        assertFalse(intSnapshot.hasNext());
        assertFalse(longSnapshot.hasNext());
        assertFalse(doubleSnapshot.hasNext());
        for (final Object buffer : List.of(objects, ints, longs, doubles)) {
            final Field field = buffer.getClass().getDeclaredField("spine");
            field.setAccessible(true);
            final Object[] chunks = (Object[]) field.get(buffer);
            int allocated = 0;
            for (final Object chunk : chunks) {
                if (chunk != null) {
                    allocated++;
                    assertTrue(Array.getLength(chunk) <= 65_536);
                }
            }
            assertTrue(allocated < 64, "Large emissions should use a bounded number of growing chunks");
        }
        long checksum = 0;
        final IntIterator all = ints.iterator();
        while (all.hasNext()) {
            checksum += all.nextInt();
        }
        assertEquals(499_999_500_000L, checksum);
    }

    @Test
    void primitiveParallelFamiliesFilterAndMapWithoutLosingElements() {
        final int n = 4096;
        final byte[] bytes = new byte[n];
        final char[] chars = new char[n];
        final short[] shorts = new short[n];
        final int[] ints = new int[n];
        final long[] longs = new long[n];
        final float[] floats = new float[n];
        final double[] doubles = new double[n];
        for (int i = 0; i < n; i++) {
            bytes[i] = (byte) (i % 100);
            chars[i] = (char) i;
            shorts[i] = (short) i;
            ints[i] = i;
            longs[i] = i;
            floats[i] = i;
            doubles[i] = i;
        }
        assertArrayEquals(ByteStream.of(bytes).filter(v -> v % 2 == 0).map(v -> (byte) (v + 1)).sorted().toArray(),
                ByteStream.of(bytes).parallel(3).filter(v -> v % 2 == 0).map(v -> (byte) (v + 1)).sorted().toArray());
        assertArrayEquals(CharStream.of(chars).filter(v -> v % 2 == 0).map(v -> (char) (v + 1)).sorted().toArray(),
                CharStream.of(CharIterator.of(chars)).parallel(3).filter(v -> v % 2 == 0).map(v -> (char) (v + 1)).sorted().toArray());
        assertArrayEquals(ShortStream.of(shorts).filter(v -> v % 2 == 0).map(v -> (short) (v + 1)).sorted().toArray(),
                ShortStream.of(shorts).parallel(3).filter(v -> v % 2 == 0).map(v -> (short) (v + 1)).sorted().toArray());
        assertArrayEquals(IntStream.of(ints).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray(),
                IntStream.of(IntIterator.of(ints)).parallel(3).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray());
        assertArrayEquals(LongStream.of(longs).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray(),
                LongStream.of(longs).parallel(3).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray());
        assertArrayEquals(FloatStream.of(floats).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray(),
                FloatStream.of(FloatIterator.of(floats)).parallel(3).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray());
        assertArrayEquals(DoubleStream.of(doubles).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray(),
                DoubleStream.of(DoubleIterator.of(doubles)).parallel(3).filter(v -> v % 2 == 0).map(v -> v + 1).sorted().toArray());
    }

    @Test
    void parallelPrimitiveCallbacksUseConfiguredWorkersAndCloseOnce() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final var threads = ConcurrentHashMap.<String> newKeySet();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(3, task -> new Thread(task, "primitive-worker-" + calls.get()))) {
            final double[] output = IntStream.range(0, 1024)
                    .mapToDouble(value -> value)
                    .onClose(closes::incrementAndGet)
                    .parallel(BaseStream.ParallelSettings.builder().maxThreadNum(2).executor(executor).build())
                    .map(value -> {
                        threads.add(Thread.currentThread().getName());
                        if (calls.getAndIncrement() < 2) {
                            try {
                                barrier.await(10, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return value + 1;
                    })
                    .sorted()
                    .toArray();
            assertEquals(1024, output.length);
            assertEquals(1024, calls.get());
            assertEquals(1, closes.get());
            assertTrue(threads.stream().allMatch(name -> name.startsWith("primitive-worker-")));
        }
    }

    @Test
    void primitiveBufferFailureDeliversPriorValuesAndNoSyntheticZero() {
        final IllegalStateException failure = new IllegalStateException("after three values");
        final DoubleIterator iterator = new DoubleIterator() {
            private int next;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                if (next == 3) {
                    throw failure;
                }
                return ++next;
            }
        };
        try (DoubleStream stream = ParallelPrimitiveBuffer.concatDouble(List.of(iterator), false, null)) {
            final DoubleIterator result = stream.iterator();
            assertEquals(1, result.nextDouble());
            assertEquals(2, result.nextDouble());
            assertEquals(3, result.nextDouble());
            assertSame(failure, assertThrows(IllegalStateException.class, result::hasNext));
        }
    }

    @Test
    void firstPublicationDoesNotWaitForFollowingSourceRead() throws Exception {
        final CountDownLatch firstConsumed = new CountDownLatch(1);
        final DoubleIterator source = new DoubleIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public double nextDouble() {
                if (cursor++ == 0) {
                    return 1;
                }
                try {
                    assertTrue(firstConsumed.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return 2;
            }
        };
        try (var consumer = Executors.newSingleThreadExecutor()) {
            final var first = consumer.submit(() -> DoubleStream.of(source).parallel(2).map(value -> value).limit(1).toArray());
            try {
                assertArrayEquals(new double[] { 1 }, first.get(5, TimeUnit.SECONDS));
            } finally {
                firstConsumed.countDown();
            }
        }
    }

    @Test
    void primitiveFlatMapClosesEveryAdoptedSourceIncludingShortCircuit() {
        final AtomicInteger opened = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        final AtomicInteger outerClosed = new AtomicInteger();
        IntStream.range(0, 10000).mapToDouble(value -> value).onClose(outerClosed::incrementAndGet).parallel(3).flatMap(value -> {
            opened.incrementAndGet();
            return DoubleStream.of(value, -value).onClose(closed::incrementAndGet);
        }).limit(10).toArray();
        // Parallel close stops new adoptions; an already-running mapper can finish just after close returns.
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (opened.get() != closed.get()) {
                Thread.sleep(1);
            }
        });
        assertEquals(1, outerClosed.get());
    }

    @Test
    void lastCollectorPreservesNullsWraparoundAndHugeRequestedLimit() {
        assertEquals(Arrays.asList(7, null, 9), Stream.of(0, 1, null, 3, 4, 5, 6, 7, null, 9).collect(Collectors.last(3)));
        assertEquals(Arrays.asList(null, 2), Stream.of(null, 2).collect(Collectors.last(Integer.MAX_VALUE)));
        assertEquals(List.of(), Stream.of(1, 2, 3).collect(Collectors.last(0)));
        assertEquals(List.of(3), Stream.of(1, 2, 3).collect(Collectors.last(1)));
        assertThrows(UnsupportedOperationException.class, () -> java.util.stream.IntStream.range(0, 1000).boxed().parallel().collect(Collectors.last(10)));
    }

    @Test
    void percentilesKeepInputAndFloatingPointStatisticsSemantics() {
        final double[] values = { 9, -0.0d, Double.NaN, 1, +0.0d, -3, Double.POSITIVE_INFINITY };
        final double[] snapshot = values.clone();
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        assertEquals(N.percentilesOfSorted(sorted), DoubleStream.of(values).parallel(3).percentiles().get());
        final var result = DoubleStream.of(DoubleIterator.of(values)).summaryStatisticsAndPercentiles();
        assertEquals(N.percentilesOfSorted(sorted), result.right().get());
        assertEquals(values.length, result.left().getCount());
        assertTrue(Double.isNaN(result.left().getSum()));
        assertArrayEquals(snapshot, values);
        assertFalse(DoubleStream.empty().percentiles().isPresent());
    }
}
