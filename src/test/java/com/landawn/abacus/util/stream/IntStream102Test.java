package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("new-test")
public class IntStream102Test extends TestBase {

    private IntStream stream;

    protected IntStream createIntStream(int... a) {
        return IntStream.of(a).map(e -> e + 0);
    }

    protected IntStream createIntStream(int[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex).map(e -> e + 0);
    }

    protected IntStream createIntStream(Integer[] a) {
        return IntStream.of(a).map(e -> e + 0);
    }

    protected IntStream createIntStream(Integer[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex).map(e -> e + 0);
    }

    protected IntStream createIntStream(Collection<Integer> coll) {
        return IntStream.of(coll.toArray(new Integer[coll.size()])).map(e -> e + 0);
    }

    protected IntStream createIntStream(IntIterator iter) {
        return iter == null ? IntStream.empty() : IntStream.of(iter.toArray()).map(e -> e + 0);
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }
    }

    @Test
    public void testEmpty() {
        stream = IntStream.empty();
        assertFalse(stream.iterator().hasNext());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testOf() {
        stream = createIntStream(5);
        assertArrayEquals(new int[] { 5 }, stream.toArray());

        stream = createIntStream(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());

        stream = createIntStream(new int[] {});
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfWithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        stream = createIntStream(arr, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfNullable() {
        stream = IntStream.ofNullable(null);
        assertEquals(0, stream.count());

        stream = IntStream.ofNullable(42);
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testRange() {
        stream = IntStream.range(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());

        stream = IntStream.range(5, 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRangeWithStep() {
        stream = IntStream.range(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());

        stream = IntStream.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void testRangeClosed() {
        stream = IntStream.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        stream = IntStream.rangeClosed(0, 10, 3);
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, stream.toArray());
    }

    @Test
    public void testRepeat() {
        stream = IntStream.repeat(7, 3);
        assertArrayEquals(new int[] { 7, 7, 7 }, stream.toArray());

        stream = IntStream.repeat(7, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testIterate() {
        stream = IntStream.iterate(1, n -> n * 2).limit(5);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, stream.toArray());

        stream = IntStream.iterate(1, n -> n < 10, n -> n + 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.generate(counter::getAndIncrement).limit(5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(4, 5, 6);
        stream = IntStream.concat(s1, s2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testZip() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20, 30);
        stream = IntStream.zip(s1, s2, Integer::sum);
        assertArrayEquals(new int[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testFilter() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testMap() {
        stream = createIntStream(1, 2, 3).map(n -> n * 2);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testMapToLong() {
        LongStream longStream = createIntStream(1, 2, 3).mapToLong(n -> (long) n * 1000000000L);
        assertArrayEquals(new long[] { 1000000000L, 2000000000L, 3000000000L }, longStream.toArray());
    }

    @Test
    public void testMapToDouble() {
        DoubleStream doubleStream = createIntStream(1, 2, 3).mapToDouble(n -> n * 0.5);
        assertArrayEquals(new double[] { 0.5, 1.0, 1.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testMapToObj() {
        Stream<String> objStream = createIntStream(1, 2, 3).mapToObj(String::valueOf);
        assertArrayEquals(new String[] { "1", "2", "3" }, objStream.toArray());
    }

    @Test
    public void testFlatMap() {
        stream = createIntStream(1, 2, 3).flatMap(n -> createIntStream(n, n * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testDistinct() {
        stream = createIntStream(1, 2, 2, 3, 3, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testSorted() {
        stream = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).sorted();
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4, 5, 6, 9 }, stream.toArray());
    }

    @Test
    public void testReverseSorted() {
        stream = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).reverseSorted();
        assertArrayEquals(new int[] { 9, 6, 5, 4, 3, 2, 1, 1 }, stream.toArray());
    }

    @Test
    public void testLimit() {
        stream = createIntStream(1, 2, 3, 4, 5).limit(3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSkip() {
        stream = createIntStream(1, 2, 3, 4, 5).skip(2);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testStep() {
        stream = createIntStream(1, 2, 3, 4, 5, 6, 7, 8).step(2);
        assertArrayEquals(new int[] { 1, 3, 5, 7 }, stream.toArray());
    }

    @Test
    public void testPeek() {
        List<Integer> sideEffect = new ArrayList<>();
        stream = createIntStream(1, 2, 3).peek(sideEffect::add);
        stream.toArray();
        assertEquals(Arrays.asList(1, 2, 3), sideEffect);
    }

    @Test
    public void testTakeWhile() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).takeWhile(n -> n < 4);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testDropWhile() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).dropWhile(n -> n < 4);
        assertArrayEquals(new int[] { 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testReversed() {
        stream = createIntStream(1, 2, 3, 4, 5).reversed();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testRotated() {
        stream = createIntStream(1, 2, 3, 4, 5).rotated(2);
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testShuffled() {
        int[] original = { 1, 2, 3, 4, 5 };
        stream = createIntStream(original).shuffled();
        int[] shuffled = stream.toArray();

        Arrays.sort(shuffled);
        assertArrayEquals(original, shuffled);
    }

    @Test
    public void testCycled() {
        stream = createIntStream(1, 2, 3).cycled(2);
        assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testIndexed() {
        Stream<IndexedInt> indexed = createIntStream(10, 20, 30).indexed();
        List<IndexedInt> list = indexed.toList();

        assertEquals(3, list.size());
        assertEquals(10, list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals(20, list.get(1).value());
        assertEquals(1, list.get(1).index());
    }

    @Test
    public void testIntersection() {
        stream = createIntStream(1, 2, 3, 4, 5).intersection(Arrays.asList(3, 4, 5, 6, 7));
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testDifference() {
        stream = createIntStream(1, 2, 3, 4, 5).difference(Arrays.asList(3, 4));
        assertArrayEquals(new int[] { 1, 2, 5 }, stream.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        stream = createIntStream(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4));
        int[] result = stream.sorted().toArray();
        assertArrayEquals(new int[] { 1, 4 }, result);
    }

    @Test
    public void testToArray() {
        int[] array = createIntStream(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testToList() {
        List<Integer> list = createIntStream(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToSet() {
        Set<Integer> set = createIntStream(1, 2, 2, 3, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToIntList() {
        IntList list = createIntStream(1, 2, 3).toIntList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testCount() {
        assertEquals(5, createIntStream(1, 2, 3, 4, 5).count());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testSum() {
        assertEquals(15, createIntStream(1, 2, 3, 4, 5).sum());
        assertEquals(0, IntStream.empty().sum());
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = createIntStream(1, 2, 3, 4, 5).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);

        assertFalse(IntStream.empty().average().isPresent());
    }

    @Test
    public void testMin() {
        OptionalInt min = createIntStream(3, 1, 4, 1, 5).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());

        assertFalse(IntStream.empty().min().isPresent());
    }

    @Test
    public void testMax() {
        OptionalInt max = createIntStream(3, 1, 4, 1, 5).max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsInt());

        assertFalse(IntStream.empty().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalInt kth = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(3);
        assertTrue(kth.isPresent());
        assertEquals(5, kth.getAsInt());
    }

    @Test
    public void testsummaryStatistics() {
        IntSummaryStatistics stats = createIntStream(1, 2, 3, 4, 5).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testFindFirst() {
        OptionalInt first = createIntStream(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(1, first.getAsInt());

        assertFalse(IntStream.empty().first().isPresent());
    }

    @Test
    public void testFindLast() {
        OptionalInt last = createIntStream(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(3, last.getAsInt());
    }

    @Test
    public void testFindAny() {
        OptionalInt any = createIntStream(1, 2, 3).first();
        assertTrue(any.isPresent());
        assertTrue(Arrays.asList(1, 2, 3).contains(any.getAsInt()));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(IntStream.of(1, 2, 3, 4, 5).anyMatch(n -> n > 4));
        assertFalse(IntStream.of(1, 2, 3).anyMatch(n -> n > 5));
    }

    @Test
    public void testAllMatch() {
        assertTrue(IntStream.of(2, 4, 6, 8).allMatch(n -> n % 2 == 0));
        assertFalse(IntStream.of(2, 4, 5, 8).allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(IntStream.of(1, 3, 5, 7).noneMatch(n -> n % 2 == 0));
        assertFalse(IntStream.of(1, 3, 4, 7).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testOnlyOne() {
        OptionalInt only = createIntStream(42).onlyOne();
        assertTrue(only.isPresent());
        assertEquals(42, only.getAsInt());

        assertFalse(IntStream.empty().onlyOne().isPresent());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createIntStream(1, 2).onlyOne());
    }

    @Test
    public void testReduce() {
        int sum = createIntStream(1, 2, 3, 4, 5).reduce(0, Integer::sum);
        assertEquals(15, sum);

        OptionalInt product = createIntStream(2, 3, 4).reduce((a, b) -> a * b);
        assertTrue(product.isPresent());
        assertEquals(24, product.getAsInt());
    }

    @Test
    public void testCollect() {
        IntList list = createIntStream(1, 2, 3).collect(IntList::new, IntList::add);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testForEach() {
        List<Integer> list = new ArrayList<>();
        createIntStream(1, 2, 3).forEach(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testJoin() {
        String joined = createIntStream(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", joined);

        joined = createIntStream(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", joined);
    }

    @Test
    public void testBoxed() {
        Stream<Integer> boxed = createIntStream(1, 2, 3).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3 }, boxed.toArray());
    }

    @Test
    public void testAsLongStream() {
        LongStream longStream = createIntStream(1, 2, 3).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testAsDoubleStream() {
        DoubleStream doubleStream = createIntStream(1, 2, 3).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testParallel() {
        stream = IntStream.range(0, 1000).parallel();
        assertTrue(stream.isParallel());

        int sum = stream.sum();
        assertEquals(499500, sum);
    }

    @Test
    public void testSequential() {
        stream = IntStream.range(0, 100).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testTop() {
        stream = createIntStream(5, 2, 8, 1, 9, 3, 7).top(3);
        int[] top3 = stream.sorted().toArray();
        assertArrayEquals(new int[] { 7, 8, 9 }, top3);
    }

    @Test
    public void testScan() {
        stream = createIntStream(1, 2, 3, 4).scan(Integer::sum);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());

        stream = createIntStream(1, 2, 3, 4).scan(0, Integer::sum);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testCollapse() {
        Stream<IntList> collapsed = createIntStream(1, 1, 2, 2, 2, 3, 3).collapse((a, b) -> a == b);
        List<IntList> result = collapsed.toList();

        assertEquals(3, result.size());
        assertArrayEquals(new int[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new int[] { 2, 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new int[] { 3, 3 }, result.get(2).toArray());
    }

    @Test
    public void testAppendPrepend() {
        stream = createIntStream(1, 2, 3).append(4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());

        stream = createIntStream(3, 4, 5).prepend(1, 2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testMergeWith() {
        IntStream s1 = createIntStream(1, 3, 5);
        IntStream s2 = createIntStream(2, 4, 6);
        stream = s1.mergeWith(s2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testSplitByChunkCount() {
        List<int[]> chunks = Stream.splitByChunkCount(10, 3, (from, to) -> {
            int[] chunk = new int[to - from];
            for (int i = from; i < to; i++) {
                chunk[i - from] = i;
            }
            return chunk;
        }).toList();

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7, 8, 9 }, chunks.get(2));
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.range(0, 10, 0));
    }

    @Test
    public void testNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> createIntStream(1, 2, 3).limit(-1));
    }

    @Test
    public void testNegativeSkip() {
        assertThrows(IllegalArgumentException.class, () -> createIntStream(1, 2, 3).skip(-1));
    }

    @Test
    public void testOperationAfterClose() {
        stream = createIntStream(1, 2, 3);
        stream.close();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        stream = createIntStream(1, 2, 3).onClose(() -> closed.set(true));
        stream.count();
        assertTrue(closed.get());
    }

    @Test
    public void testElementAt() {
        OptionalInt element = createIntStream(10, 20, 30, 40, 50).elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(30, element.getAsInt());

        assertFalse(IntStream.of(1, 2, 3).elementAt(10).isPresent());
    }

    @Test
    public void testThrowIfEmpty() {
        stream = createIntStream(1, 2, 3).throwIfEmpty();
        assertEquals(3, stream.count());
    }

    @Test
    public void testThrowIfEmptyOnEmptyStream() {
        assertThrows(NoSuchElementException.class, () -> IntStream.empty().throwIfEmpty().count());
    }

    @Test
    public void testPrintln() {
        createIntStream(1, 2, 3).println();
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Integer>> percentiles = createIntStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles();
        assertTrue(percentiles.isPresent());

        Map<Percentage, Integer> map = percentiles.get();
        assertNotNull(map);
        assertTrue(map.containsKey(Percentage._50));
    }

    @Test
    public void testFromJavaStream() {
        java.util.stream.IntStream javaStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.from(javaStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.IntStream javaStream = createIntStream(1, 2, 3).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, javaStream.toArray());
    }
}
