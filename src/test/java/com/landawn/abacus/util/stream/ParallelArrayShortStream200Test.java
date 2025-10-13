package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelArrayShortStream200Test extends TestBase {
    private static final int testMaxThreadNum = 4;

    private short[] largeArray;
    private short[] smallArray;
    private ShortStream parallelStream;
    private ShortStream parallelStream2;
    private ShortStream emptyParallelStream;
    private ShortStream smallParallelStream;

    ShortStream createShortStream(short... data) {
        return ShortStream.of(data).parallel(PS.create(testMaxThreadNum, Splitor.ARRAY));
    }

    @BeforeEach
    public void setUp() {
        largeArray = Array.rangeClosed((short) 1, (short) 1000);
        smallArray = new short[] { 1, 2, 3, 4, 5 };

        parallelStream = createShortStream(largeArray);
        parallelStream2 = createShortStream(largeArray);

        emptyParallelStream = createShortStream(new short[] {});
        smallParallelStream = createShortStream(smallArray);
    }

    @Test
    @DisplayName("isParallel should always return true")
    public void testIsParallel() {
        assertTrue(parallelStream.isParallel());
        assertTrue(emptyParallelStream.isParallel());
    }

    @Test
    @DisplayName("sequential() should return a non-parallel stream")
    public void testSequential() {
        ShortStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        assertArrayEquals(largeArray, sequentialStream.toArray());
    }

    @Test
    @DisplayName("onClose handler should be called upon stream closure")
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        try (ShortStream stream = createShortStream(largeArray).parallel().onClose(() -> closed.set(true))) {
            stream.count();
        }
        assertTrue(closed.get(), "onClose handler should have been called.");
    }

    @Nested
    @DisplayName("Overridden Intermediate Operations")
    public class ParallelIntermediateOps {

        @Test
        public void testFilter() {
            long count = parallelStream.filter(x -> x % 2 == 0).count();
            assertEquals(500, count);
        }

        @Test
        public void testMap() {
            long sum = parallelStream.map(x -> (short) (x * 2)).sum();
            long expectedSum = N.sum(largeArray) * 2;
            assertEquals(expectedSum, sum);
        }

        @Test
        public void testMapToInt() {
            long sum = parallelStream.mapToInt(x -> x + 1).sum();
            long expectedSum = N.sum(largeArray) + largeArray.length;
            assertEquals(expectedSum, sum);
        }

        @Test
        public void testMapToObj() {
            long count = parallelStream.mapToObj(x -> "s" + x).count();
            assertEquals(largeArray.length, count);
        }

        @Test
        public void testFlatMap() {
            long count = createShortStream((short) 1, (short) 2, (short) 3).parallel().flatMap(x -> createShortStream(x, x, x)).count();
            assertEquals(9, count);
        }

        @Test
        public void testOnEach() {
            AtomicInteger counter = new AtomicInteger(0);
            parallelStream.onEach(e -> counter.incrementAndGet()).count();
            assertEquals(largeArray.length, counter.get());
        }

        @Test
        public void testTakeWhile() {
            short[] result = parallelStream.takeWhile(x -> x <= 5).toArray();
            assertHaveSameElements(smallParallelStream.toList(), N.toList(result));
        }

        @Test
        public void testDropWhile() {
            short[] result = parallelStream.dropWhile(x -> x <= 10).sorted().toArray();
            assertEquals(largeArray.length - 10, result.length);
            assertEquals(11, result[0]);
        }
    }

    @Nested
    @DisplayName("Overridden Terminal Operations")
    public class ParallelTerminalOps {

        @Test
        public void testForEach() {
            final Map<Short, Boolean> map = new ConcurrentHashMap<>();
            parallelStream.forEach(e -> map.put(e, true));
            assertEquals(largeArray.length, map.size());
            assertTrue(map.containsKey((short) 100));
            assertTrue(map.containsKey((short) 500));
        }

        @Test
        public void testToMap() {
            Map<Short, Short> map = parallelStream.toMap(k -> k, v -> (short) (v * 2));
            assertEquals(largeArray.length, map.size());
            assertEquals((short) 200, map.get((short) 100));
        }

        @Test
        public void testGroupTo() {
            Map<String, List<Short>> map = parallelStream.groupTo(k -> k % 2 == 0 ? "even" : "odd", Collectors.toList());
            assertEquals(2, map.size());
            assertEquals(500, map.get("even").size());
            assertEquals(500, map.get("odd").size());
        }

        @Test
        public void testReduceWithIdentity() {
            ShortStream stream = createShortStream(largeArray);
            short sum = stream.reduce((short) 0, (a, b) -> (short) (a + b));
            short expectedSum = (short) N.sum(largeArray);
            assertEquals(expectedSum, sum);
        }

        @Test
        public void testReduce() {
            short sum = parallelStream.reduce((a, b) -> (short) (a + b)).orElse((short) -1);
            short expectedSum = (short) N.sum(largeArray);
            assertEquals(expectedSum, sum);
            assertFalse(emptyParallelStream.reduce((a, b) -> (short) (a + b)).isPresent());
        }

        @Test
        public void testCollect() {
            ShortList result = parallelStream.collect(ShortList::new, ShortList::add, ShortList::addAll);
            assertEquals(largeArray.length, result.size());
            assertTrue(result.containsAll(ShortList.of(smallArray)));
        }

        @Test
        public void testAnyMatch() {
            assertTrue(parallelStream.anyMatch(x -> x == 500));
            assertFalse(parallelStream2.anyMatch(x -> x == -1));
        }

        @Test
        public void testAllMatch() {
            assertTrue(parallelStream.allMatch(x -> x > 0));
            assertFalse(parallelStream2.allMatch(x -> x % 2 == 0));
        }

        @Test
        public void testNoneMatch() {
            assertTrue(parallelStream.noneMatch(x -> x < 0));
            assertFalse(parallelStream2.noneMatch(x -> x == 500));
        }

        @Test
        public void testFindFirst() {
            assertEquals((short) 500, parallelStream.findFirst(x -> x == 500).orElse((short) -1));
            assertEquals((short) 1, parallelStream2.findFirst(x -> true).orElse((short) -1));
        }

        @Test
        public void testFindAny() {
            short found = parallelStream.findAny(x -> x > 500).orElse((short) -1);
            assertTrue(found > 500);
        }

        @Test
        public void testFindLast() {
            short found = parallelStream.findLast(x -> x < 500).orElse((short) -1);
            assertEquals((short) 499, found);
        }
    }

    @Nested
    @DisplayName("Parallel Zip Operations")
    public class ParallelZipOps {
        @Test
        public void testZipWithTwoStreams() {
            ShortStream s1 = createShortStream(largeArray).parallel();
            ShortStream s2 = createShortStream(largeArray).parallel();
            long count = s1.zipWith(s2, (a, b) -> (short) (a + b)).count();
            assertEquals(largeArray.length, count);

            long sum = createShortStream(largeArray).parallel().zipWith(createShortStream(largeArray), (a, b) -> (short) (a + b)).sum();
            assertEquals(N.sum(largeArray) * 2, sum);
        }

        @Test
        public void testZipWithThreeStreams() {
            ShortStream s1 = createShortStream(largeArray).parallel();
            ShortStream s2 = createShortStream(largeArray).parallel();
            ShortStream s3 = createShortStream(largeArray).parallel();
            long sum = s1.zipWith(s2, s3, (a, b, c) -> (short) (a + b + c)).sum();
            assertEquals(N.sum(largeArray) * 3, sum);
        }

        @Test
        public void testZipWithDefaultValues() {
            short[] shortArr = { 10, 20 };
            ShortStream s1 = createShortStream(smallArray).parallel();
            ShortStream s2 = createShortStream(shortArr).parallel();

            ShortList result = s1.zipWith(s2, (short) 99, (short) 100, (a, b) -> (short) (a + b)).toShortList();

            ShortList expected = new ShortList();
            expected.add((short) (1 + 10));
            expected.add((short) (2 + 20));
            expected.add((short) (3 + 100));
            expected.add((short) (4 + 100));
            expected.add((short) (5 + 100));

            assertHaveSameElements(expected.boxed(), result.boxed());
        }
    }
}
