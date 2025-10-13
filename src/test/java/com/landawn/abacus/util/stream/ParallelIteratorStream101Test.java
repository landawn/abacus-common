package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;

@ExtendWith(MockitoExtension.class)
@Tag("new-test")
public class ParallelIteratorStream101Test extends TestBase {

    private List<Integer> testData;
    private Stream<Integer> stream;
    private Stream<Integer> stream2;

    @BeforeEach
    public void setUp() {
        testData = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        stream = createStream(testData);
        stream2 = createStream(testData);
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }
    }

    private <T> Stream<T> createStream(Collection<T> data) {
        return Stream.of(data.iterator()).parallel();
    }

    @Test
    public void testConstructorWithStream() {
        Stream<Integer> sourceStream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> parallelStream = sourceStream.parallel();

        List<Integer> result = parallelStream.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testConstructorWithSortedStream() {
        Stream<Integer> sourceStream = Stream.of(5, 3, 1, 4, 2).sorted();
        Stream<Integer> parallelStream = sourceStream.parallel();

        List<Integer> result = parallelStream.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testComplexChainedOperations() {
        List<String> result = stream.filter(n -> n % 2 == 0)
                .map(n -> n * 10)
                .flatMap(n -> Stream.of(n, n + 1, n + 2))
                .map(String::valueOf)
                .filter(s -> s.contains("2"))
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(s -> s.contains("2")));
    }

    @Test
    public void testWithNullElements() {
        List<String> dataWithNulls = Arrays.asList("a", null, "b", null, "c");
        try (Stream<String> nullStream = createStream(dataWithNulls)) {
            List<String> result = nullStream.filter(Objects::nonNull).map(String::toUpperCase).toList();

            assertHaveSameElements(Arrays.asList("A", "B", "C"), result);
        }
    }

    @Test
    public void testExceptionPropagation() {
        try {
            stream.map(n -> {
                if (n == 7) {
                    throw new IllegalArgumentException("Test error at 7");
                }
                return n;
            }).toList();

            fail("Expected exception to be thrown");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Test error at 7") || e.getCause().getMessage().contains("Test error at 7"));
        }
    }

    @Test
    public void testPrimitiveStreamOperations() {
        int[] intResult = stream.limit(3).flatMapToInt(n -> IntStream.range(n, n + 3)).sorted().toArray();

        assertEquals(9, intResult.length);
        assertEquals(1, intResult[0]);
        assertEquals(5, intResult[8]);

        double sum = stream2.flatMapToDouble(n -> DoubleStream.of(n * 0.1, n * 0.2)).sum();

        assertTrue(sum > 0);
    }

    @Test
    public void testParallelGroupingOperations() throws Exception {
        Map<String, List<String>> grouped = stream.groupTo(n -> n % 2 == 0 ? "even" : "odd", n -> "Value:" + n);

        assertEquals(2, grouped.size());
        assertEquals(5, grouped.get("even").size());
        assertEquals(5, grouped.get("odd").size());
    }

    @Test
    public void testParallelGroupingOperations2() throws Exception {
        Map<Integer, List<Integer>> flatGrouped = stream.limit(3).flatGroupTo(n -> Arrays.asList(n, n + 10), (k, v) -> v * 100);

        assertTrue(flatGrouped.containsKey(1));
        assertTrue(flatGrouped.containsKey(11));
        assertEquals(Arrays.asList(100), flatGrouped.get(1));
    }

    @Test
    public void testParallelCollectOperations() {
        Set<Integer> concurrentSet = stream.collect(Collectors.toConcurrentMap(n -> n, n -> n * 2, (a, b) -> a, ConcurrentHashMap::new)).keySet();

        assertEquals(10, concurrentSet.size());
    }

    @Test
    public void testParallelCollectOperations2() {

        String joined = stream.map(String::valueOf).collect(Collectors.joining(","));

        String[] parts = joined.split(",");
        assertEquals(10, parts.length);
    }

    @Test
    public void testOrderPreservation() {
        List<Integer> sortedData = Arrays.asList(1, 2, 3, 4, 5);
        try (Stream<Integer> sortedStream = Stream.of(sortedData.toArray(new Integer[0])).parallel().sorted()) {

            List<Integer> result = sortedStream.toList();
            assertEquals(sortedData, result);
        }
    }

    @Test
    public void testMemoryEfficientOperations() {
        Iterator<Integer> largeIterator = new Iterator<Integer>() {
            private int current = 0;
            private final int max = 10000;

            @Override
            public boolean hasNext() {
                return current < max;
            }

            @Override
            public Integer next() {
                return current++;
            }
        };

        try (Stream<Integer> largeStream = Stream.of(N.toList(largeIterator).toArray(Integer[]::new)).parallel()) {

            long sum = largeStream.filter(n -> n % 2 == 0).mapToLong(Integer::longValue).sum();

            assertEquals(24995000L, sum);
        }
    }

    @Test
    public void testThreadLocalBehavior() {
        ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
        Set<Integer> threadIds = N.newConcurrentHashSet();

        List<Integer> result = stream.map(n -> {
            if (threadLocal.get() == null) {
                threadLocal.set(Thread.currentThread().hashCode());
            }
            threadIds.add(threadLocal.get());
            return n;
        }).toList();

        assertEquals(10, result.size());
        assertTrue(threadIds.size() >= 1);
    }

    @Test
    public void testSingleElementStream() {
        try (Stream<Integer> singleStream = createStream(Collections.singletonList(42))) {

            assertEquals(Integer.valueOf(42), singleStream.min(Integer::compare).get());
        }
        try (Stream<Integer> singleStream = createStream(Collections.singletonList(42))) {

            assertEquals(Integer.valueOf(42), singleStream.max(Integer::compare).get());
        }
        try (Stream<Integer> singleStream = createStream(Collections.singletonList(42))) {

            assertEquals(Integer.valueOf(42), singleStream.findFirst(n -> true).get());
        }
    }

    @Test
    public void testInterruptibility() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            try (Stream<Integer> interruptibleStream = createStream(Arrays.asList(1, 2, 3, 4, 5))) {

                latch.countDown();
                interruptibleStream.forEach(n -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        wasInterrupted.set(true);
                        Thread.currentThread().interrupt();
                    }
                });
            }
        });

        thread.start();
        latch.await();
        Thread.sleep(100);
        thread.interrupt();
        thread.join(2000);

        assertTrue(wasInterrupted.get() || !thread.isAlive());
    }

    @Test
    public void testPerformanceCharacteristics() {
        long startTime = System.currentTimeMillis();

        List<Double> result = stream.map(n -> {
            double sum = 0;
            for (int i = 0; i < 1000; i++) {
                sum += Math.sqrt(n * i);
            }
            return sum;
        }).toList();

        long duration = System.currentTimeMillis() - startTime;

        assertEquals(10, result.size());
        assertTrue(duration < 5000, "Operation took too long: " + duration + "ms");
    }
}
