package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatPredicate;

@Tag("new-test")
public class FloatStream200Test extends TestBase {

    private static final float DELTA = 1e-5f;

    @Nested
    @DisplayName("Factory Methods")
    public class FactoryMethods {
        @Test
        public void testOfEmpty() {
            assertEquals(0, FloatStream.empty().count());
        }

        @Test
        public void testOfSingle() {
            assertArrayEquals(new float[] { 3.14f }, FloatStream.of(3.14f).toArray(), DELTA);
        }

        @Test
        public void testOfMultiple() {
            assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, FloatStream.of(1.1f, 2.2f, 3.3f).toArray(), DELTA);
        }

        @Test
        public void testOfArray() {
            float[] array = { 1.0f, 2.0f, 3.0f };
            assertArrayEquals(array, FloatStream.of(array).toArray(), DELTA);
        }

        @Test
        public void testOfNullable() {
            assertEquals(0, FloatStream.ofNullable(null).count());
            assertArrayEquals(new float[] { 9.9f }, FloatStream.ofNullable(9.9f).toArray(), DELTA);
        }

        @Test
        public void testIterate() {
            float[] expected = { 1.0f, 1.5f, 2.0f, 2.5f, 3.0f };
            assertArrayEquals(expected, FloatStream.iterate(1.0f, f -> f + 0.5f).limit(5).toArray(), DELTA);
        }

        @Test
        public void testGenerate() {
            final Random rng = new Random(1);
            float[] expected = { rng.nextFloat(), rng.nextFloat(), rng.nextFloat() };
            final Random rng2 = new Random(1);
            assertArrayEquals(expected, FloatStream.generate(rng2::nextFloat).limit(3).toArray(), DELTA);
        }

        @Test
        public void testConcat() {
            FloatStream s1 = FloatStream.of(1.1f, 2.2f);
            FloatStream s2 = FloatStream.of(3.3f, 4.4f);
            assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, FloatStream.concat(s1, s2).toArray(), DELTA);
        }

        @Test
        public void testZip() {
            float[] a = { 1.0f, 2.0f, 3.0f };
            float[] b = { 4.0f, 5.0f, 6.0f };
            assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, FloatStream.zip(a, b, (f1, f2) -> f1 + f2).toArray(), DELTA);
        }

        @Test
        public void testMerge() {
            float[] a = { 1.1f, 3.3f, 5.5f };
            float[] b = { 2.2f, 4.4f, 6.6f };
            float[] expected = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f, 6.6f };
            assertArrayEquals(expected, FloatStream.merge(a, b, (f1, f2) -> f1 <= f2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), DELTA);
        }
    }

    @Nested
    @DisplayName("Intermediate Operations")
    public class IntermediateOps {
        @Test
        public void testFilter() {
            assertArrayEquals(new float[] { 2.0f, 4.0f }, FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f).filter(f -> f % 2.0f == 0).toArray(), DELTA);
        }

        @Test
        public void testMap() {
            assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, FloatStream.of(1.0f, 2.0f, 3.0f).map(f -> f * 2.0f).toArray(), DELTA);
        }

        @Test
        public void testFlatMap() {
            float[] expected = { 1.0f, -1.0f, 2.0f, -2.0f };
            assertArrayEquals(expected, FloatStream.of(1.0f, 2.0f).flatMap(f -> FloatStream.of(f, -f)).toArray(), DELTA);
        }

        @Test
        public void testMapToObj() {
            String[] expected = { "1.1", "2.2" };
            assertArrayEquals(expected, FloatStream.of(1.1f, 2.2f).mapToObj(String::valueOf).toArray(String[]::new));
        }

        @Test
        public void testDistinct() {
            float[] result = FloatStream.of(1.1f, 2.2f, 1.1f, 3.3f, 2.2f).distinct().toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, result, DELTA);
        }

        @Test
        public void testSorted() {
            assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 5.5f }, FloatStream.of(5.5f, 2.2f, 1.1f, 3.3f).sorted().toArray(), DELTA);
        }

        @Test
        public void testReversed() {
            assertArrayEquals(new float[] { 3.3f, 2.2f, 1.1f }, FloatStream.of(1.1f, 2.2f, 3.3f).reversed().toArray(), DELTA);
        }

        @Test
        public void testSkipAndLimit() {
            assertArrayEquals(new float[] { 3.0f, 4.0f }, FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).skip(2).limit(2).toArray(), DELTA);
        }

        @Test
        public void testTakeWhile() {
            assertArrayEquals(new float[] { 1f, 2f, 3f }, FloatStream.of(1f, 2f, 3f, 4f, 2f).takeWhile(f -> f < 4f).toArray(), DELTA);
        }

        @Test
        public void testDropWhile() {
            assertArrayEquals(new float[] { 4f, 2f }, FloatStream.of(1f, 2f, 3f, 4f, 2f).dropWhile(f -> f < 4f).toArray(), DELTA);
        }

        @Test
        public void testScan() {
            assertArrayEquals(new float[] { 1.0f, 3.0f, 6.0f }, FloatStream.of(1.0f, 2.0f, 3.0f).scan((a, b) -> a + b).toArray(), DELTA);
        }

        @Test
        public void testBoxed() {
            List<Float> expected = Arrays.asList(1.1f, 2.2f, 3.3f);
            assertEquals(expected, FloatStream.of(1.1f, 2.2f, 3.3f).boxed().collect(Collectors.toList()));
        }

        @Test
        public void testAsDoubleStream() {
            double[] expected = { 1.1, 2.2, 3.3 };
            assertArrayEquals(expected, FloatStream.of(1.1f, 2.2f, 3.3f).asDoubleStream().toArray(), DELTA);
        }
    }

    @Nested
    @DisplayName("Terminal Operations")
    public class TerminalOps {
        @Test
        public void testCount() {
            assertEquals(4, FloatStream.of(1f, 2f, 3f, 4f).count());
            assertEquals(0, FloatStream.empty().count());
        }

        @Test
        public void testSum() {
            assertEquals(10.0, FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f).sum(), DELTA);
            assertEquals(0.0, FloatStream.empty().sum(), DELTA);
        }

        @Test
        public void testAverage() {
            assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).average().isPresent());
            assertEquals(3.0, FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).average().getAsDouble(), DELTA);
            assertFalse(FloatStream.empty().average().isPresent());
        }

        @Test
        public void testMinMax() {
            assertTrue(FloatStream.of(3.1f, 1.2f, 5.3f).min().isPresent());
            assertEquals(1.2f, FloatStream.of(3.1f, 1.2f, 5.3f).min().get(), DELTA);
            assertTrue(FloatStream.of(3.1f, 1.2f, 5.3f).max().isPresent());
            assertEquals(5.3f, FloatStream.of(3.1f, 1.2f, 5.3f).max().get(), DELTA);
        }

        @Test
        public void testReduce() {
            OptionalFloat result = FloatStream.of(1.0f, 2.0f, 3.0f).reduce((a, b) -> a + b);
            assertTrue(result.isPresent());
            assertEquals(6.0f, result.get(), DELTA);
        }

        @Test
        public void testReduceWithIdentity() {
            float result = FloatStream.of(1.0f, 2.0f, 3.0f).reduce(10.0f, (a, b) -> a + b);
            assertEquals(16.0f, result, DELTA);
        }

        @Test
        public void testAnyAllNoneMatch() {
            assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 2.5f));
            assertFalse(FloatStream.of(1.0f, 2.0f, 3.0f).allMatch(f -> f > 2.5f));
            assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 5.0f));
        }

        @Test
        public void testFindFirstLast() {
            assertEquals(1.0f, FloatStream.of(1.0f, 2.0f, 3.0f).first().get(), DELTA);
            assertEquals(3.0f, FloatStream.of(1.0f, 2.0f, 3.0f).last().get(), DELTA);
            assertFalse(FloatStream.empty().first().isPresent());
        }

        @Test
        public void testToArray() {
            float[] expected = { 1.0f, 2.0f, 3.0f };
            assertArrayEquals(expected, FloatStream.of(1.0f, 2.0f, 3.0f).toArray(), DELTA);
        }

        @Test
        public void testToFloatList() {
            FloatList expected = FloatList.of(1.0f, 2.0f, 3.0f);
            assertEquals(expected, FloatStream.of(1.0f, 2.0f, 3.0f).toFloatList());
        }

        @Test
        public void testToMap() {
            Map<Integer, Float> map = FloatStream.of(1.1f, 2.2f, 3.3f).toMap(f -> (int) Math.floor(f), f -> f);
            assertEquals(1.1f, map.get(1), DELTA);
            assertEquals(2.2f, map.get(2), DELTA);
            assertEquals(3.3f, map.get(3), DELTA);
        }

        @Test
        public void testsummaryStatistics() {
            FloatSummaryStatistics stats = FloatStream.of(1f, 2f, 3f, 4f, 5f).summaryStatistics();
            assertEquals(5, stats.getCount());
            assertEquals(15.0, stats.getSum(), DELTA);
            assertEquals(1.0f, stats.getMin(), DELTA);
            assertEquals(5.0f, stats.getMax(), DELTA);
            assertEquals(3.0, stats.getAverage(), DELTA);
        }

        @Test
        public void testKthLargest() {
            assertEquals(7.7f, FloatStream.of(3.3f, 1.1f, 4.4f, 8.8f, 1.1f, 5.5f, 9.9f, 2.2f, 6.6f, 7.7f).kthLargest(3).get(), DELTA);
            assertEquals(9.9f, FloatStream.of(3.3f, 1.1f, 4.4f, 8.8f, 1.1f, 5.5f, 9.9f, 2.2f, 6.6f, 7.7f).kthLargest(1).get(), DELTA);
            assertFalse(FloatStream.empty().kthLargest(1).isPresent());
        }
    }

    @Nested
    @DisplayName("Parallel and Conditional Operations")
    public class ParallelAndConditionalOps {
        @Test
        public void testParallelMapSum() {
            for (int k = 0; k < 1000; k++) {
                float[] data = new float[1000];
                for (int i = 0; i < data.length; i++) {
                    data[i] = i + 1;
                }

                double expectedSum = FloatStream.of(data).map(f -> f * 2.0f).sum();
                double actualSum = FloatStream.of(data).parallel().map(f -> f * 2.0f).sum();

                assertEquals(expectedSum, actualSum, DELTA);
            }
        }

        @Test
        public void testIsParallelAndSequential() {
            assertFalse(FloatStream.of(1.0f).isParallel());
            assertTrue(FloatStream.of(1.0f).parallel().isParallel());
            assertFalse(FloatStream.of(1.0f).parallel().sequential().isParallel());
        }

        @Test
        public void testIfEmpty() {
            AtomicBoolean executed = new AtomicBoolean(false);
            FloatStream.empty().ifEmpty(() -> executed.set(true)).count();
            assertTrue(executed.get());

            executed.set(false);
            FloatStream.of(1.0f).ifEmpty(() -> executed.set(true)).count();
            assertFalse(executed.get());
        }

        @Test
        public void testAppendIfEmpty() {
            float[] expected = { 9.8f, 7.6f };
            assertArrayEquals(expected, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(9.8f, 7.6f)).toArray(), DELTA);
            assertArrayEquals(new float[] { 1f, 2f }, FloatStream.of(1f, 2f).appendIfEmpty(() -> FloatStream.of(9.8f, 7.6f)).toArray(), DELTA);
        }
    }

    @Nested
    @DisplayName("Exception and Edge Cases")
    public class ExceptionAndEdgeCases {
        @Test
        public void testInvalidStep() {
            assertThrows(IllegalArgumentException.class, () -> FloatStream.of(1f, 2f).step(0));
            assertThrows(IllegalArgumentException.class, () -> FloatStream.of(1f, 2f).step(-1));
        }

        @Test
        public void testNextOnExhaustedIterator() {
            FloatIterator it = FloatStream.of(1.0f).iterator();
            it.nextFloat();
            assertThrows(NoSuchElementException.class, it::nextFloat);
        }

        @Test
        public void testStreamIsClosedAfterTerminalOp() {
            FloatStream stream = FloatStream.of(1.0f, 2.0f);
            stream.count();
            assertThrows(IllegalStateException.class, stream::count);
        }

        @Test
        public void testThrowIfEmpty() {
            assertThrows(NoSuchElementException.class, () -> FloatStream.empty().throwIfEmpty().count());
            assertDoesNotThrow(() -> FloatStream.of(1.0f).throwIfEmpty().count());
        }
    }

    @Nested
    @DisplayName("More Factory Methods")
    public class MoreFactoryMethods {

        @Test
        public void testFlatten2D() {
            float[][] source = { { 1.1f, 2.2f }, { 3.3f }, {}, { 4.4f, 5.5f } };
            float[] expected = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
            assertArrayEquals(expected, FloatStream.flatten(source).toArray(), DELTA);
        }

        @Test
        public void testRepeat() {
            assertArrayEquals(new float[] { 7.7f, 7.7f, 7.7f }, FloatStream.repeat(7.7f, 3).toArray(), DELTA);
            assertEquals(0, FloatStream.repeat(7.7f, 0).count());
        }

        @Test
        public void testZipWithDefaults() {
            float[] a = { 1.1f, 2.2f };
            float[] b = { 10.1f, 20.2f, 30.3f };
            float[] expected = { 11.2f, 22.4f, 30.3f };
            assertArrayEquals(expected, FloatStream.zip(a, b, 0.0f, 0.0f, (x, y) -> x + y).toArray(), DELTA);
        }

        @Test
        public void testZip3() {
            float[] a = { 1f, 2f, 3f };
            float[] b = { 10f, 20f, 30f };
            float[] c = { 100f, 200f, 300f };
            float[] expected = { 111f, 222f, 333f };
            assertArrayEquals(expected, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), DELTA);
        }

        @Test
        public void testDefer() {
            AtomicInteger counter = new AtomicInteger(0);
            FloatStream stream = FloatStream.defer(() -> {
                counter.incrementAndGet();
                return FloatStream.of(1.1f, 2.2f);
            });
            assertEquals(0, counter.get());
            assertEquals(2, stream.count());
            assertEquals(1, counter.get());
        }

        @Test
        public void testOfFloatBuffer() {
            FloatBuffer buffer = FloatBuffer.wrap(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            buffer.position(1).limit(4);
            assertArrayEquals(new float[] { 2.2f, 3.3f, 4.4f }, FloatStream.of(buffer).toArray(), DELTA);
        }
    }

    @Nested
    @DisplayName("More Intermediate Operations")
    public class MoreIntermediateOps {

        @Test
        public void testMapPartial() {
            float[] result = FloatStream.of(1f, 2f, 3f, 4f, 5f).mapPartial(f -> f % 2 != 0 ? OptionalFloat.of(f * 10) : OptionalFloat.empty()).toArray();
            assertArrayEquals(new float[] { 10f, 30f, 50f }, result, DELTA);
        }

        @Test
        public void testRangeMap() {
            FloatBiPredicate inSameRange = (a, b) -> Math.abs(a - b) < 1.0f;
            float[] data = { 1.1f, 1.5f, 1.9f, 3.5f, 4.4f, 6.0f, 6.2f };
            float[] expected = { 1.5f, 3.95f, 6.1f };
            assertArrayEquals(expected, FloatStream.of(data).rangeMap(inSameRange, (a, b) -> (a + b) / 2).toArray(), DELTA);
        }

        @Test
        public void testPrependAndAppend() {
            OptionalFloat opf = OptionalFloat.of(99.9f);
            FloatStream other = FloatStream.of(0.1f, 0.2f);

            float[] result = FloatStream.of(1.1f, 2.2f).prepend(opf).append(-1.1f, -2.2f).append(other).toArray();

            float[] expected = { 99.9f, 1.1f, 2.2f, -1.1f, -2.2f, 0.1f, 0.2f };
            assertArrayEquals(expected, result, DELTA);
        }

        @Test
        public void testZipWith() {
            FloatStream s1 = FloatStream.of(1f, 2f, 3f);
            FloatStream s2 = FloatStream.of(10f, 20f);
            float[] expected = { 11f, 22f };
            assertArrayEquals(expected, s1.zipWith(s2, (a, b) -> a + b).toArray(), DELTA);
        }

        @Test
        public void testTransform() {
            FloatStream transformed = FloatStream.of(1f, 2f, 3f, 4f, 5f).transform(s -> s.step(2).map(f -> f * 2));
            assertArrayEquals(new float[] { 2f, 6f, 10f }, transformed.map(f -> f).toArray(), DELTA);
        }
    }

    @Nested
    @DisplayName("More Terminal Operations")
    public class MoreTerminalOps {

        @Test
        public void testCollectWithCombiner() {
            ArrayList<Float> result = FloatStream.of(1.1f, 2.2f, 3.3f).parallel().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            result.sort(Comparator.naturalOrder());
            assertEquals(Arrays.asList(1.1f, 2.2f, 3.3f), result);
        }

        @Test
        public void testToMapWithFactoryAndMerge() {
            Map<Integer, Float> map = FloatStream.of(1.1f, 2.9f, 1.8f, 3.5f).toMap(f -> (int) Math.floor(f), f -> f, (v1, v2) -> v1 + v2, HashMap::new);

            assertEquals(1.1f + 1.8f, map.get(1), DELTA);
            assertEquals(2.9f, map.get(2), DELTA);
            assertEquals(3.5f, map.get(3), DELTA);
        }

        @Test
        public void testGroupToWithFactory() {
            Map<String, FloatList> map = FloatStream.of(1f, 2f, 3f, 4f).groupTo(f -> f % 2 == 0 ? "even" : "odd", Collectors.toFloatList(), HashMap::new);

            assertTrue(map instanceof HashMap);
            assertArrayEquals(new float[] { 2f, 4f }, map.get("even").toArray(), DELTA);
            assertArrayEquals(new float[] { 1f, 3f }, map.get("odd").toArray(), DELTA);
        }

        @Test
        public void testOnlyOne() {
            assertEquals(5.5f, FloatStream.of(5.5f).onlyOne().get(), DELTA);
            assertFalse(FloatStream.empty().onlyOne().isPresent());
            assertThrows(TooManyElementsException.class, () -> FloatStream.of(1.0f, 2.0f).onlyOne());
        }

        @Test
        public void testElementAt() {
            assertEquals(3.3f, FloatStream.of(1.1f, 2.2f, 3.3f).elementAt(2).get(), DELTA);
            assertFalse(FloatStream.of(1.1f, 2.2f).elementAt(2).isPresent());
            assertFalse(FloatStream.empty().elementAt(0).isPresent());
        }

        @Test
        public void testPercentiles() {
            float[] data = { 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f };
            Map<Percentage, Float> percentiles = FloatStream.of(data).percentiles().get();

            assertEquals(11f, percentiles.get(Percentage._50), DELTA);
            assertEquals(1f, percentiles.get(Percentage._0_0001), DELTA);
            assertEquals(20f, percentiles.get(Percentage._99_9999), DELTA);
            assertEquals(5f, percentiles.get(Percentage._20), DELTA);
        }

        @Test
        public void testJoinTo() {
            Joiner joiner = Joiner.with(" | ");
            FloatStream.of(1.1f, 2.2f, 3.3f).joinTo(joiner);
            assertEquals("1.1 | 2.2 | 3.3", joiner.toString());
        }
    }

    @Nested
    @DisplayName("Advanced Mapping and Flattening Operations")
    public class AdvancedMappingOps {

        @Test
        public void testFlatMapToInt() {
            int[] result = FloatStream.of(1.1f, 2.2f).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray();
            assertArrayEquals(new int[] { 1, 11, 2, 22 }, result);
        }

        @Test
        public void testFlatMapToLong() {
            long[] result = FloatStream.of(1.1f, 2.2f).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray();
            assertArrayEquals(new long[] { 1L, 11L, 2L, 22L }, result);
        }

        @Test
        public void testFlatMapToDouble() {
            double[] result = FloatStream.of(1.1f, 2.2f).flatMapToDouble(f -> DoubleStream.of(f, f * 10.0)).toArray();
            assertArrayEquals(new double[] { 1.100000023841858, 11.00000023841858, 2.200000047683716, 22.000000476837158 }, result, DELTA);
        }

        @Test
        public void testFlattMapToObjWithArray() {
            String[] result = FloatStream.of(1f, 2f).flattmapToObj(f -> new String[] { "val=" + f }).toArray(String[]::new);
            assertArrayEquals(new String[] { "val=1.0", "val=2.0" }, result);
        }

        @Test
        public void testCollapseWithTriPredicate() {
            float[] data = { 1.0f, 2.0f, 3.0f, 2.5f, 4.0f, 5.0f };
            float[] expected = { 6.0f, 11.5f };
            float[] result = FloatStream.of(data).collapse((first, prev, curr) -> curr > prev, (a, b) -> a + b).toArray();

            assertArrayEquals(expected, result, DELTA);
        }

        @Test
        public void testRangeMapToObj() {
            float[] data = { 1.1f, 1.2f, 1.9f, 5.0f, 5.4f, 8.2f };
            String[] expected = { "1.1-1.9", "5.0-5.4", "8.2-8.2" };
            String[] result = FloatStream.of(data)
                    .rangeMapToObj((left, next) -> Math.abs(left - next) < 2.0f, (left, right) -> left + "-" + right)
                    .toArray(String[]::new);
            assertArrayEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Advanced Control Flow and Side-Effect Operations")
    public class AdvancedControlFlowOps {

        @Test
        public void testSkipWithAction() {
            final FloatList skippedItems = new FloatList();
            FloatStream.of(1f, 2f, 3f, 4f, 5f).skip(3, skippedItems::add).forEach(f -> {
            });

            assertArrayEquals(new float[] { 1f, 2f, 3f }, skippedItems.toArray(), DELTA);
        }

        @Test
        public void testFilterWithActionOnDroppedItem() {
            final FloatList droppedItems = new FloatList();
            FloatPredicate predicate = f -> f > 3f;
            FloatStream.of(1f, 2f, 3f, 4f, 5f).filter(predicate, droppedItems::add).count();

            assertArrayEquals(new float[] { 1f, 2f, 3f }, droppedItems.toArray(), DELTA);
        }

        @Test
        public void testDropWhileWithActionOnDroppedItem() {
            final FloatList droppedItems = new FloatList();
            FloatStream.of(1f, 2f, 3f, 4f, 1f).dropWhile(f -> f < 4f, droppedItems::add).count();

            assertArrayEquals(new float[] { 1f, 2f, 3f }, droppedItems.toArray(), DELTA);
        }

        @Test
        public void testTopWithComparator() {
            float[] data = { 5.5f, 2.2f, 8.8f, 1.1f, 3.3f };
            float[] expected = { 3.3f, 8.8f, 5.5f };
            float[] result = FloatStream.of(data).top(3, Comparator.naturalOrder()).toArray();

            assertArrayEquals(expected, result, DELTA);
        }

        @Test
        public void testDelay() {
            long startTime = System.currentTimeMillis();
            FloatStream.of(1f, 2f, 3f).delay(com.landawn.abacus.util.Duration.ofMillis(50)).count();
            long endTime = System.currentTimeMillis();

            assertTrue((endTime - startTime) >= 100, "Stream processing should be delayed");
        }
    }

}
