package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongBiPredicate;


public class LongStream200Test extends TestBase {

    @Nested
    @DisplayName("Factory Methods")
    public class FactoryMethods {

        @Test
        public void testOfEmpty() {
            assertEquals(0, LongStream.of().count());
        }

        @Test
        public void testOfSingleElement() {
            assertArrayEquals(new long[] { 1L }, LongStream.of(1L).toArray());
        }

        @Test
        public void testOfMultipleElements() {
            assertArrayEquals(new long[] { 1L, 2L, 3L }, LongStream.of(1L, 2L, 3L).toArray());
        }

        @Test
        public void testOfArray() {
            long[] arr = { 1, 2, 3 };
            assertArrayEquals(arr, LongStream.of(arr).toArray());
        }

        @Test
        public void testOfNullable() {
            assertEquals(0, LongStream.ofNullable(null).count());
            assertArrayEquals(new long[] { 5L }, LongStream.ofNullable(5L).toArray());
        }

        @Test
        public void testRange() {
            assertArrayEquals(new long[] { 0, 1, 2, 3, 4 }, LongStream.range(0, 5).toArray());
            assertEquals(0, LongStream.range(5, 5).count());
            assertEquals(0, LongStream.range(6, 5).count());
        }

        @Test
        public void testRangeClosed() {
            assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5 }, LongStream.rangeClosed(0, 5).toArray());
            assertArrayEquals(new long[] { 5 }, LongStream.rangeClosed(5, 5).toArray());
            assertEquals(0, LongStream.rangeClosed(6, 5).count());
        }

        @Test
        public void testIterate() {
            assertArrayEquals(new long[] { 0, 2, 4, 6, 8 }, LongStream.iterate(0, n -> n + 2).limit(5).toArray());
        }

        @Test
        public void testIterateWithPredicate() {
            assertArrayEquals(new long[] { 1, 2, 4, 8 }, LongStream.iterate(1, i -> i < 10, i -> i * 2).toArray());
        }

        @Test
        public void testGenerate() {
            final AtomicLong a = new AtomicLong(0);
            assertArrayEquals(new long[] { 0, 1, 2, 3, 4 }, LongStream.generate(a::getAndIncrement).limit(5).toArray());
        }

        @Test
        public void testConcat() {
            LongStream s1 = LongStream.of(1, 2);
            LongStream s2 = LongStream.of(3, 4);
            assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.concat(s1, s2).toArray());
        }

        @Test
        public void testConcatWithEmptyStream() {
            LongStream s1 = LongStream.of(1, 2);
            LongStream s2 = LongStream.empty();
            assertArrayEquals(new long[] { 1, 2 }, LongStream.concat(s1, s2).toArray());
        }

        @Test
        public void testZip() {
            long[] a = { 1, 2, 3 };
            long[] b = { 4, 5, 6 };
            assertArrayEquals(new long[] { 5, 7, 9 }, LongStream.zip(a, b, (x, y) -> x + y).toArray());
        }

        @Test
        public void testZipWithDifferentLength() {
            long[] a = { 1, 2 };
            long[] b = { 4, 5, 6 };
            assertArrayEquals(new long[] { 5, 7 }, LongStream.zip(a, b, (x, y) -> x + y).toArray());
        }

        @Test
        public void testZipWithDefaultValues() {
            long[] a = { 1, 2 };
            long[] b = { 4, 5, 6 };
            assertArrayEquals(new long[] { 5, 7, 6 }, LongStream.zip(a, b, 0, 0, (x, y) -> x + y).toArray());
        }

        @Test
        public void testMerge() {
            long[] a = { 1, 3, 5 };
            long[] b = { 2, 4, 6 };
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 },
                    LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        }

        @Test
        public void testFlatten() {
            long[][] twoDimArray = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.flatten(twoDimArray).toArray());
        }

        @Test
        public void testFromJdkStream() {
            java.util.stream.LongStream jdkStream = java.util.stream.LongStream.of(1, 2, 3);
            assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.from(jdkStream).toArray());
        }
    }

    @Nested
    @DisplayName("Intermediate Operations")
    public class IntermediateOps {

        @Test
        public void testFilter() {
            assertArrayEquals(new long[] { 2, 4 }, LongStream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0).toArray());
        }

        @Test
        public void testMap() {
            assertArrayEquals(new long[] { 1, 4, 9, 16, 25 }, LongStream.of(1, 2, 3, 4, 5).map(n -> n * n).toArray());
        }

        @Test
        public void testFlatMap() {
            assertArrayEquals(new long[] { 1, -1, 2, -2, 3, -3 }, LongStream.of(1, 2, 3).flatMap(n -> LongStream.of(n, -n)).toArray());
        }

        @Test
        public void testDistinct() {
            assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.of(1, 2, 2, 3, 1, 3).distinct().sorted().toArray());
        }

        @Test
        public void testSorted() {
            assertArrayEquals(new long[] { 1, 2, 3, 5, 8 }, LongStream.of(5, 2, 8, 1, 3).sorted().toArray());
        }

        @Test
        public void testReverseSorted() {
            assertArrayEquals(new long[] { 8, 5, 3, 2, 1 }, LongStream.of(5, 2, 8, 1, 3).reverseSorted().toArray());
        }

        @Test
        public void testReversed() {
            assertArrayEquals(new long[] { 3, 2, 1 }, LongStream.of(1, 2, 3).reversed().toArray());
        }

        @Test
        public void testLimit() {
            assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.range(1, 10).limit(3).toArray());
        }

        @Test
        public void testSkip() {
            assertArrayEquals(new long[] { 4, 5, 6, 7, 8, 9 }, LongStream.range(1, 10).skip(3).toArray());
        }

        @Test
        public void testSkipWithAction() {
            LongList skipped = new LongList();
            assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(3, skipped::add).toArray());
            assertArrayEquals(new long[] { 1, 2, 3 }, skipped.toArray());
        }

        @Test
        public void testStep() {
            assertArrayEquals(new long[] { 1, 4, 7 }, LongStream.range(1, 10).step(3).toArray());
        }

        @Test
        public void testPeek() {
            LongList peeked = new LongList();
            long[] result = LongStream.of(1, 2, 3).peek(peeked::add).toArray();
            assertArrayEquals(new long[] { 1, 2, 3 }, result);
            assertArrayEquals(new long[] { 1, 2, 3 }, peeked.toArray());
        }

        @Test
        public void testTakeWhile() {
            assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.of(1, 2, 3, 4, 5, 1, 2).takeWhile(n -> n < 5).toArray());
        }

        @Test
        public void testDropWhile() {
            assertArrayEquals(new long[] { 5, 1, 2 }, LongStream.of(1, 2, 3, 4, 5, 1, 2).dropWhile(n -> n < 5).toArray());
        }

        @Test
        public void testBoxed() {
            java.util.List<Long> expected = Arrays.asList(1L, 2L, 3L);
            assertEquals(expected, LongStream.of(1, 2, 3).boxed().collect(Collectors.toList()));
        }

        @Test
        public void testScan() {
            assertArrayEquals(new long[] { 1, 3, 6, 10 }, LongStream.of(1, 2, 3, 4).scan((a, b) -> a + b).toArray());
        }

        @Test
        public void testScanWithInitialValue() {
            assertArrayEquals(new long[] { 10, 11, 13, 16, 20 }, LongStream.of(1, 2, 3, 4).scan(10, true, (a, b) -> a + b).toArray());
            assertArrayEquals(new long[] { 11, 13, 16, 20 }, LongStream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b).toArray());
        }

        @Test
        public void testPrepend() {
            assertArrayEquals(new long[] { -1, 0, 1, 2, 3 }, LongStream.of(1, 2, 3).prepend(-1, 0).toArray());
        }

        @Test
        public void testAppend() {
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3).append(4, 5).toArray());
        }

        @Test
        public void testIntersection() {
            assertArrayEquals(new long[] { 2, 3 }, LongStream.of(1, 2, 3, 4).intersection(Arrays.asList(2L, 3L, 5L)).toArray());
        }

        @Test
        public void testDifference() {
            assertArrayEquals(new long[] { 1, 4 }, LongStream.of(1, 2, 3, 4).difference(Arrays.asList(2L, 3L, 5L)).toArray());
        }
    }

    @Nested
    @DisplayName("Terminal Operations")
    public class TerminalOps {

        @Test
        public void testCount() {
            assertEquals(5, LongStream.range(0, 5).count());
            assertEquals(0, LongStream.empty().count());
        }

        @Test
        public void testSum() {
            assertEquals(10, LongStream.of(1, 2, 3, 4).sum());
            assertEquals(0, LongStream.empty().sum());
        }

        @Test
        public void testAverage() {
            assertEquals(2.5, LongStream.of(1, 2, 3, 4).average().getAsDouble());
            assertFalse(LongStream.empty().average().isPresent());
        }

        @Test
        public void testMin() {
            assertEquals(1, LongStream.of(5, 2, 8, 1, 3).min().getAsLong());
            assertFalse(LongStream.empty().min().isPresent());
        }

        @Test
        public void testMax() {
            assertEquals(8, LongStream.of(5, 2, 8, 1, 3).max().getAsLong());
            assertFalse(LongStream.empty().max().isPresent());
        }

        @Test
        public void testReduceWithIdentity() {
            assertEquals(10, LongStream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b));
            assertEquals(0, LongStream.empty().reduce(0, (a, b) -> a + b));
        }

        @Test
        public void testReduce() {
            assertEquals(10, LongStream.of(1, 2, 3, 4).reduce((a, b) -> a + b).getAsLong());
            assertFalse(LongStream.empty().reduce((a, b) -> a + b).isPresent());
        }

        @Test
        public void testFindFirst() {
            assertEquals(OptionalLong.of(1), LongStream.of(1, 2, 3).first());
            assertEquals(OptionalLong.empty(), LongStream.empty().first());
        }

        @Test
        public void testFindLast() {
            assertEquals(OptionalLong.of(3), LongStream.of(1, 2, 3).last());
            assertEquals(OptionalLong.empty(), LongStream.empty().last());
        }

        @Test
        public void testAnyMatch() {
            assertTrue(LongStream.of(1, 2, 3).anyMatch(n -> n == 2));
            assertFalse(LongStream.of(1, 2, 3).anyMatch(n -> n == 4));
            assertFalse(LongStream.empty().anyMatch(n -> true));
        }

        @Test
        public void testAllMatch() {
            assertTrue(LongStream.of(2, 4, 6).allMatch(n -> n % 2 == 0));
            assertFalse(LongStream.of(1, 2, 3).allMatch(n -> n % 2 == 0));
            assertTrue(LongStream.empty().allMatch(n -> false));
        }

        @Test
        public void testNoneMatch() {
            assertTrue(LongStream.of(1, 3, 5).noneMatch(n -> n % 2 == 0));
            assertFalse(LongStream.of(1, 2, 3).noneMatch(n -> n % 2 == 0));
            assertTrue(LongStream.empty().noneMatch(n -> true));
        }

        @Test
        public void testToArray() {
            assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.of(1, 2, 3).toArray());
        }

        @Test
        public void testToLongList() {
            assertEquals(LongList.of(1, 2, 3), LongStream.of(1, 2, 3).toLongList());
        }

        @Test
        public void testCollect() {
            LongList result = LongStream.of(1, 2, 3).collect(LongList::new, LongList::add);
            assertEquals(LongList.of(1, 2, 3), result);
        }

        @Test
        public void testJoin() {
            assertEquals("1, 2, 3", LongStream.of(1, 2, 3).join(", "));
            assertEquals("[1, 2, 3]", LongStream.of(1, 2, 3).join(", ", "[", "]"));
        }

        @Test
        public void testOnlyOne() {
            assertEquals(OptionalLong.of(1), LongStream.of(1).onlyOne());
            assertEquals(OptionalLong.empty(), LongStream.empty().onlyOne());
            assertThrows(TooManyElementsException.class, () -> LongStream.of(1, 2).onlyOne());
        }

        @Test
        public void testKthLargest() {
            assertEquals(OptionalLong.of(7), LongStream.of(3, 1, 4, 8, 1, 5, 9, 2, 6, 7).kthLargest(3));
            assertEquals(OptionalLong.of(9), LongStream.of(3, 1, 4, 8, 1, 5, 9, 2, 6, 7).kthLargest(1));
            assertEquals(OptionalLong.of(1), LongStream.of(3, 1, 4, 8, 1, 5, 9, 2, 6, 7).kthLargest(9));
            assertEquals(OptionalLong.empty(), LongStream.of(1, 2, 3).kthLargest(4));
            assertEquals(OptionalLong.empty(), LongStream.empty().kthLargest(1));
        }
    }

    @Nested
    @DisplayName("Parallel Operations")
    public class ParallelOps {
        @Test
        public void testParallelMapSum() {
            long expectedSum = LongStream.range(0, 100).map(i -> i * 2).sum();
            long actualSum = LongStream.range(0, 100).parallel().map(i -> i * 2).sum();
            assertEquals(expectedSum, actualSum);
        }

        @Test
        public void testIsParallel() {
            assertFalse(LongStream.of(1, 2, 3).isParallel());
            assertTrue(LongStream.of(1, 2, 3).parallel().isParallel());
            assertFalse(LongStream.of(1, 2, 3).parallel().sequential().isParallel());
        }

        @Test
        public void testSps() {
            LongList collected = new LongList();
            long sum = LongStream.range(0, 100).sps(s -> s.peek(e -> {
                synchronized (collected) { // Ensure thread-safe collection
                    collected.add(e);
                }
            }).map(i -> i * 2)) // parallel
                    .filter(i -> i > 50) // sequential
                    .sum();

            assertEquals(100, collected.size());

            long expectedSum = LongStream.range(0, 100).map(i -> i * 2).filter(i -> i > 50).sum();
            assertEquals(expectedSum, sum);
        }
    }

    @Nested
    @DisplayName("Resource Management")
    public class ResourceManagement {

        @Test
        public void testOnClose() {
            AtomicLong counter = new AtomicLong(0);
            try (LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet)) {
                stream.count();
            }
            // A terminal operation will close the stream.
            assertEquals(1, counter.get());
        }

        @Test
        public void testExplicitClose() {
            AtomicLong counter = new AtomicLong(0);
            LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet);
            stream.close();
            assertEquals(1, counter.get());
            // Close is idempotent
            stream.close();
            assertEquals(1, counter.get());
        }

        @Test
        public void testIteratorDoesNotCloseStream() {
            AtomicLong counter = new AtomicLong(0);
            LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet);
            LongIterator iter = stream.iterator();
            while (iter.hasNext()) {
                iter.nextLong();
            }
            assertEquals(0, counter.get());
            stream.close();
            assertEquals(1, counter.get());
        }
    }

    @Nested
    @DisplayName("Exception and Edge Cases")
    public class ExceptionAndEdgeCases {
        @Test
        public void testInvalidSkip() {
            assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).skip(-1));
        }

        @Test
        public void testInvalidLimit() {
            assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).limit(-1));
        }

        @Test
        public void testInvalidStep() {
            assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(0));
            assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(-1));
        }

        @Test
        public void testNextOnExhaustedIterator() {
            LongStream s = LongStream.of(1);
            LongIterator it = s.iterator();
            it.nextLong();
            assertThrows(NoSuchElementException.class, it::nextLong);
        }

        @Test
        public void testThrowIfEmpty() {
            assertThrows(NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().count());
            assertDoesNotThrow(() -> LongStream.of(1).throwIfEmpty().count());
        }
    }

    @Nested
    @DisplayName("Advanced Intermediate Operations")
    public class AdvancedIntermediateOps {

        @Test
        public void testMapToObj() {
            String[] expected = { "1", "2", "3" };
            assertArrayEquals(expected, LongStream.of(1, 2, 3).mapToObj(String::valueOf).toArray(String[]::new));
        }

        @Test
        public void testMapToInt() {
            int[] expected = { 1, 2, 3 };
            assertArrayEquals(expected, LongStream.of(1, 2, 3).mapToInt(l -> (int) l).toArray());
        }

        @Test
        public void testMapToDouble() {
            double[] expected = { 1.0, 2.0, 3.0 };
            assertArrayEquals(expected, LongStream.of(1, 2, 3).mapToDouble(l -> (double) l).toArray());
        }

        @Test
        public void testFlatmapToObj() {
            List<String> result = LongStream.of(1, 2).flatmapToObj(l -> Arrays.asList(String.valueOf(l), String.valueOf(l * 10))).toList();
            assertEquals(Arrays.asList("1", "10", "2", "20"), result);
        }

        @Test
        public void testCollapse() {
            // Collapse consecutive equal numbers
            LongBiPredicate isSame = (a, b) -> a == b;
            long[] array = { 1, 1, 2, 3, 3, 3, 1, 4 };
            long[][] expected = { { 1, 1 }, { 2 }, { 3, 3, 3 }, { 1 }, { 4 } };

            List<LongList> result = LongStream.of(array).collapse(isSame).toList();

            assertEquals(expected.length, result.size());
            for (int i = 0; i < expected.length; i++) {
                assertArrayEquals(expected[i], result.get(i).toArray());
            }
        }

        @Test
        public void testCollapseWithMerge() {
            // Collapse consecutive equal numbers by summing them
            LongBiPredicate isSame = (a, b) -> a == b;
            long[] array = { 1, 1, 2, 3, 3, 3, 1, 4 };
            long[] expected = { 2, 2, 9, 1, 4 }; // 1+1, 2, 3+3+3, 1, 4

            long[] result = LongStream.of(array).collapse(isSame, Long::sum).toArray();

            assertArrayEquals(expected, result);
        }

        @Test
        public void testSymmetricDifference() {
            Collection<Long> other = Arrays.asList(3L, 4L, 5L, 5L); // {3, 4, 5, 5}
            long[] result = LongStream.of(1, 2, 3, 3).symmetricDifference(other).sorted().toArray();
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 5 }, result);
        }

        @Test
        public void testRotated() {
            long[] a = { 1, 2, 3, 4, 5 };
            assertArrayEquals(new long[] { 4, 5, 1, 2, 3 }, LongStream.of(a).rotated(2).toArray());
            assertArrayEquals(new long[] { 3, 4, 5, 1, 2 }, LongStream.of(a).rotated(-2).toArray());
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(a).rotated(5).toArray());
        }

        @Test
        public void testCycled() {
            assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, LongStream.of(1, 2, 3).cycled().limit(6).toArray());
        }

        @Test
        public void testCycledWithRounds() {
            assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, LongStream.of(1, 2, 3).cycled(2).toArray());
            assertEquals(0, LongStream.of(1, 2, 3).cycled(0).count());
        }

        @Test
        public void testIndexed() {
            String result = LongStream.of(10, 20, 30).indexed().map(indexed -> indexed.index() + ":" + indexed.value()).join(", ");
            assertEquals("0:10, 1:20, 2:30", result);
        }

        @Test
        public void testSkipUntil() {
            long[] result = LongStream.of(1, 2, 3, 4, 5, 1, 2).skipUntil(x -> x >= 4).toArray();
            assertArrayEquals(new long[] { 4, 5, 1, 2 }, result);
        }

        @Test
        public void testFilterWithActionOnDropped() {
            LongList dropped = new LongList();
            long[] result = LongStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, dropped::add).toArray();
            assertArrayEquals(new long[] { 2, 4 }, result);
            assertArrayEquals(new long[] { 1, 3, 5 }, dropped.toArray());
        }
    }

    @Nested
    @DisplayName("Advanced Terminal Operations")
    public class AdvancedTerminalOps {

        @Test
        public void testToMap() {
            Map<Long, String> map = LongStream.of(1, 2, 3).toMap(l -> l, String::valueOf);
            assertEquals("1", map.get(1L));
            assertEquals("2", map.get(2L));
            assertEquals("3", map.get(3L));
            assertEquals(3, map.size());
        }

        @Test
        public void testToMapWithMerge() {
            Map<Long, Long> map = LongStream.of(1, 2, 1, 3, 2).toMap(l -> l, l -> 1L, Long::sum);
            assertEquals(2, map.get(1L));
            assertEquals(2, map.get(2L));
            assertEquals(1, map.get(3L));
        }

        @Test
        public void testToMultiset() {
            Multiset<Long> multiset = LongStream.of(1, 2, 1, 3, 2, 1).toMultiset();
            assertEquals(3, multiset.count(1L));
            assertEquals(2, multiset.count(2L));
            assertEquals(1, multiset.count(3L));
        }

        @Test
        public void testGroupTo() {
            Map<String, LongList> map = LongStream.range(0, 5).groupTo(l -> l % 2 == 0 ? "even" : "odd", Collectors.toLongList());

            assertArrayEquals(new long[] { 0, 2, 4 }, map.get("even").toArray());
            assertArrayEquals(new long[] { 1, 3 }, map.get("odd").toArray());
        }

        @Test
        public void testSummarize() {
            LongSummaryStatistics stats = LongStream.of(1, 2, 3, 4, 5).summarize();
            assertEquals(5, stats.getCount());
            assertEquals(15, stats.getSum());
            assertEquals(1, stats.getMin());
            assertEquals(5, stats.getMax());
            assertEquals(3.0, stats.getAverage());
        }

        @Test
        public void testSummarizeAndPercentiles() {
            Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = LongStream.of(1, 2, 3, 4, 5).summarizeAndPercentiles();

            LongSummaryStatistics stats = result.left();
            assertEquals(5, stats.getCount());
            assertEquals(15, stats.getSum());

            Map<Percentage, Long> percentiles = result.right().get();
            assertEquals(3, percentiles.get(Percentage._50)); // Median
            assertEquals(5, percentiles.get(Percentage._99));
        }

        @Test
        public void testFindFirstWithPredicate() {
            OptionalLong result = LongStream.of(1, 2, 3, 4, 5).findFirst(l -> l > 3);
            assertTrue(result.isPresent());
            assertEquals(4, result.getAsLong());
        }

        @Test
        public void testFindLastWithPredicate() {
            OptionalLong result = LongStream.of(1, 2, 3, 4, 3, 2, 1).findLast(l -> l > 3);
            assertTrue(result.isPresent());
            assertEquals(4, result.getAsLong());
        }

        @Test
        public void testForEachIndexed() {
            List<String> list = new ArrayList<>();
            LongStream.of(10, 20, 30).forEachIndexed((i, val) -> list.add(i + ":" + val));
            assertEquals(Arrays.asList("0:10", "1:20", "2:30"), list);
        }

        @Test
        public void testTop() {
            assertArrayEquals(new long[] { 8, 9, 10 }, LongStream.range(1, 11).top(3).toArray());
            assertArrayEquals(new long[] { 8, 9, 10 }, LongStream.range(1, 11).top(3, Long::compare).toArray());
        }
    }

    @Nested
    @DisplayName("Conditional and Control Flow Operations")
    public class ConditionalOps {

        @Test
        public void testIfEmpty() {
            AtomicBoolean executed = new AtomicBoolean(false);
            LongStream.empty().ifEmpty(() -> executed.set(true)).count();
            assertTrue(executed.get());

            executed.set(false);
            LongStream.of(1).ifEmpty(() -> executed.set(true)).count();
            assertFalse(executed.get());
        }

        @Test
        public void testAppendIfEmpty() {
            assertArrayEquals(new long[] { 10, 11 }, LongStream.empty().appendIfEmpty(() -> LongStream.of(10, 11)).toArray());
            assertArrayEquals(new long[] { 1, 2 }, LongStream.of(1, 2).appendIfEmpty(() -> LongStream.of(10, 11)).toArray());
        }

        @Test
        public void testApplyIfNotEmpty() {
            Optional<Long> result = LongStream.of(1, 2, 3).applyIfNotEmpty(LongStream::sum);
            assertTrue(result.isPresent());
            assertEquals(6, result.get());

            Optional<Long> emptyResult = LongStream.empty().applyIfNotEmpty(LongStream::sum);
            assertFalse(emptyResult.isPresent());
        }

        @Test
        public void testAcceptIfNotEmpty() {
            AtomicInteger counter = new AtomicInteger(0);

            // Test non-empty stream
            LongStream.of(1, 2, 3).acceptIfNotEmpty(s -> counter.incrementAndGet());
            assertEquals(1, counter.get());

            // Test empty stream
            final AtomicBoolean elseExecuted = new AtomicBoolean(false);
            LongStream.empty().acceptIfNotEmpty(s -> counter.incrementAndGet()).orElse(() -> elseExecuted.set(true));
            assertEquals(1, counter.get()); // Should not have incremented
            assertTrue(elseExecuted.get());
        }

        @Test
        public void testPsp() {
            // Parallel -> Sequential -> Parallel
            long[] result = LongStream.range(1, 100)
                    .parallel()
                    .psp(s -> s.filter(i -> i > 10).limit(5)) // The filter and limit run sequentially
                    .map(i -> i * 10) // This map runs in parallel again
                    .sorted() // Ensure order
                    .toArray();

            assertArrayEquals(new long[] { 110, 120, 130, 140, 150 }, result);
        }
    }
}
