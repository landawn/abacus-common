package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("new-test")
public class ShortStream100Test extends TestBase {

    protected ShortStream createShortStream(short... a) {
        return ShortStream.of(a);
    }

    protected ShortStream createShortStream(short[] a, int fromIndex, int toIndex) {
        return ShortStream.of(a, fromIndex, toIndex);
    }

    protected ShortStream createShortStream(Short[] a) {
        return ShortStream.of(a);
    }

    protected ShortStream createShortStream(Short[] a, int fromIndex, int toIndex) {
        return ShortStream.of(a, fromIndex, toIndex);
    }

    protected ShortStream createShortStream(Collection<Short> coll) {
        return ShortStream.of(coll.toArray(new Short[coll.size()]));
    }

    protected ShortStream createShortStream(ShortIterator iter) {
        return iter == null ? ShortStream.empty() : ShortStream.of(iter.toArray());
    }

    protected ShortStream createShortStream(ShortBuffer buff) {
        return ShortStream.of(buff);
    }

    @Test
    public void testEmpty() {
        ShortStream stream = ShortStream.empty();
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullable() {
        ShortStream stream1 = ShortStream.ofNullable(null);
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.ofNullable((short) 42);
        assertEquals(1, stream2.count());
    }

    @Test
    public void testOfArray() {
        ShortStream stream1 = createShortStream();
        assertEquals(0, stream1.count());

        ShortStream stream2 = createShortStream((short) 1);
        assertArrayEquals(new short[] { 1 }, stream2.toArray());

        ShortStream stream3 = createShortStream((short) 1, (short) 2, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream3.toArray());
    }

    @Test
    public void testOfArrayWithRange() {
        short[] array = { 1, 2, 3, 4, 5 };

        ShortStream stream1 = createShortStream(array, 0, 5);
        assertArrayEquals(array, stream1.toArray());

        ShortStream stream2 = createShortStream(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream2.toArray());

        ShortStream stream3 = createShortStream(array, 2, 2);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testOfCollection() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        ShortStream stream = createShortStream(list);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOfIterator() {
        ShortList list = new ShortList(new short[] { 1, 2, 3 });
        ShortStream stream = createShortStream(list.iterator());
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testRange() {
        ShortStream stream1 = ShortStream.range((short) 1, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = ShortStream.range((short) 5, (short) 5);
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.range((short) 5, (short) 3);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testRangeWithStep() {
        ShortStream stream1 = ShortStream.range((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, stream1.toArray());

        ShortStream stream2 = ShortStream.range((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, stream2.toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.range((short) 0, (short) 10, (short) 0));
    }

    @Test
    public void testRangeClosed() {
        ShortStream stream = ShortStream.rangeClosed((short) 1, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testRepeat() {
        ShortStream stream1 = ShortStream.repeat((short) 5, 3);
        assertArrayEquals(new short[] { 5, 5, 5 }, stream1.toArray());

        ShortStream stream2 = ShortStream.repeat((short) 5, 0);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testIterate() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.iterate(() -> count[0] < 3, () -> (short) count[0]++);
        assertArrayEquals(new short[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testIterateWithSeed() {
        ShortStream stream = ShortStream.iterate((short) 1, n -> (short) (n * 2)).limit(4);
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testGenerate() {
        final short[] value = { 0 };
        ShortStream stream = ShortStream.generate(() -> value[0]++).limit(3);
        assertArrayEquals(new short[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testConcat() {
        short[] a1 = { 1, 2 };
        short[] a2 = { 3, 4 };
        ShortStream stream1 = ShortStream.concat(a1, a2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream1.toArray());

        ShortStream s1 = createShortStream((short) 1, (short) 2);
        ShortStream s2 = createShortStream((short) 3, (short) 4);
        ShortStream stream2 = ShortStream.concat(s1, s2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream2.toArray());
    }

    @Test
    public void testZip() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };

        ShortStream stream = ShortStream.zip(a, b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testMerge() {
        short[] a = { 1, 3, 5 };
        short[] b = { 2, 4, 6 };

        ShortStream stream = ShortStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMap() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).map(n -> (short) (n * 2));
        assertArrayEquals(new short[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testMapToInt() {
        IntStream stream = createShortStream((short) 1, (short) 2, (short) 3).mapToInt(n -> n * 10);
        assertArrayEquals(new int[] { 10, 20, 30 }, stream.toArray());
    }

    @Test
    public void testMapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2, (short) 3).mapToObj(n -> "S" + n);
        assertArrayEquals(new String[] { "S1", "S2", "S3" }, stream.toArray());
    }

    @Test
    public void testFlatMap() {
        ShortStream stream = createShortStream((short) 1, (short) 2).flatMap(n -> createShortStream(n, (short) (n + 10)));
        assertArrayEquals(new short[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFilter() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).filter(n -> n % 2 == 0);
        assertArrayEquals(new short[] { 2, 4 }, stream.toArray());
    }

    @Test
    public void testTakeWhile() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).takeWhile(n -> n < 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testDropWhile() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).dropWhile(n -> n < 3);
        assertArrayEquals(new short[] { 3, 4, 2 }, stream.toArray());
    }

    @Test
    public void testDistinct() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 2, (short) 3, (short) 1).distinct();
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSorted() {
        ShortStream stream = createShortStream((short) 3, (short) 1, (short) 4, (short) 2).sorted();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testReverseSorted() {
        ShortStream stream = createShortStream((short) 3, (short) 1, (short) 4, (short) 2).reverseSorted();
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testLimit() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).limit(2);
        assertArrayEquals(new short[] { 1, 2 }, stream.toArray());
    }

    @Test
    public void testSkip() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).skip(2);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
    }

    @Test
    public void testPeek() {
        List<Short> peeked = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).peek(peeked::add);
        stream.toArray();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), peeked);
    }

    @Test
    public void testForEach() {
        List<Short> collected = new ArrayList<>();
        createShortStream((short) 1, (short) 2, (short) 3).forEach(collected::add);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), collected);
    }

    @Test
    public void testToArray() {
        short[] array = createShortStream((short) 1, (short) 2, (short) 3).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToList() {
        List<Short> list = createShortStream((short) 1, (short) 2, (short) 3).toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToSet() {
        var set = createShortStream((short) 1, (short) 2, (short) 2, (short) 3).toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToMultiset() {
        Multiset<Short> multiset = createShortStream((short) 1, (short) 2, (short) 2, (short) 3).toMultiset();
        assertEquals(1, multiset.getCount((short) 1));
        assertEquals(2, multiset.getCount((short) 2));
        assertEquals(1, multiset.getCount((short) 3));
    }

    @Test
    public void testSum() {
        int sum = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).sum();
        assertEquals(10, sum);
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.get(), 0.0001);
    }

    @Test
    public void testMin() {
        OptionalShort min = createShortStream((short) 3, (short) 1, (short) 4, (short) 2).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());
    }

    @Test
    public void testMax() {
        OptionalShort max = createShortStream((short) 3, (short) 1, (short) 4, (short) 2).max();
        assertTrue(max.isPresent());
        assertEquals(4, max.get());
    }

    @Test
    public void testCount() {
        long count = createShortStream((short) 1, (short) 2, (short) 3).count();
        assertEquals(3, count);
    }

    @Test
    public void testAnyMatch() {
        boolean result = createShortStream((short) 1, (short) 2, (short) 3).anyMatch(n -> n > 2);
        assertTrue(result);
    }

    @Test
    public void testAllMatch() {
        boolean result = createShortStream((short) 1, (short) 2, (short) 3).allMatch(n -> n > 0);
        assertTrue(result);
    }

    @Test
    public void testNoneMatch() {
        boolean result = createShortStream((short) 1, (short) 2, (short) 3).noneMatch(n -> n > 5);
        assertTrue(result);
    }

    @Test
    public void testFindFirst() {
        OptionalShort first = createShortStream((short) 1, (short) 2, (short) 3).first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get());
    }

    @Test
    public void testFindLast() {
        OptionalShort last = createShortStream((short) 1, (short) 2, (short) 3).last();
        assertTrue(last.isPresent());
        assertEquals(3, last.get());
    }

    @Test
    public void testReduce() {
        short result1 = createShortStream((short) 1, (short) 2, (short) 3).reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals(6, result1);

        OptionalShort result2 = createShortStream((short) 1, (short) 2, (short) 3).reduce((a, b) -> (short) (a + b));
        assertTrue(result2.isPresent());
        assertEquals(6, result2.get());
    }

    @Test
    public void testOnlyOne() {
        OptionalShort result1 = createShortStream((short) 42).onlyOne();
        assertTrue(result1.isPresent());
        assertEquals(42, result1.get());

        OptionalShort result2 = ShortStream.empty().onlyOne();
        assertFalse(result2.isPresent());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createShortStream((short) 1, (short) 2).onlyOne());
    }

    @Test
    public void testJoin() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testsummaryStatistics() {
        ShortSummaryStatistics stats = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).summaryStatistics();
        assertEquals(4, stats.getCount());
        assertEquals(10, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(4, stats.getMax());
    }

    @Test
    public void testBoxed() {
        Stream<Short> stream = createShortStream((short) 1, (short) 2, (short) 3).boxed();
        assertArrayEquals(new Short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAsIntStream() {
        IntStream stream = createShortStream((short) 1, (short) 2, (short) 3).asIntStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAppend() {
        ShortStream stream = createShortStream((short) 1, (short) 2).append((short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testPrepend() {
        ShortStream stream = createShortStream((short) 3, (short) 4).prepend((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testIndexed() {
        Stream<IndexedShort> stream = createShortStream((short) 10, (short) 20, (short) 30).indexed();
        List<IndexedShort> list = stream.toList();
        assertEquals(3, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals(10, list.get(0).value());
        assertEquals(1, list.get(1).index());
        assertEquals(20, list.get(1).value());
    }

    @Test
    public void testReversed() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).reversed();
        assertArrayEquals(new short[] { 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testShuffled() {
        short[] original = { 1, 2, 3, 4, 5 };
        short[] shuffled = createShortStream(original).shuffled().sorted().toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, shuffled);
    }

    @Test
    public void testRotated() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).rotated(2);
        assertArrayEquals(new short[] { 3, 4, 1, 2 }, stream.toArray());
    }

    @Test
    public void testTop() {
        ShortStream stream = createShortStream((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).top(3);
        short[] result = stream.sorted().toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
    }

    @Test
    public void testKthLargest() {
        OptionalShort result = createShortStream((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testStep() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2);
        assertArrayEquals(new short[] { 1, 3, 5 }, stream.toArray());
    }

    @Test
    public void testCycled() {
        ShortStream stream = createShortStream((short) 1, (short) 2).cycled(3);
        assertArrayEquals(new short[] { 1, 2, 1, 2, 1, 2 }, stream.toArray());
    }

    @Test
    public void testIntersection() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).intersection(Arrays.asList((short) 2, (short) 3, (short) 5));
        assertArrayEquals(new short[] { 2, 3 }, stream.toArray());
    }

    @Test
    public void testDifference() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).difference(Arrays.asList((short) 2, (short) 3));
        assertArrayEquals(new short[] { 1, 4 }, stream.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).symmetricDifference(Arrays.asList((short) 2, (short) 3, (short) 4));
        assertArrayEquals(new short[] { 1, 4 }, stream.toArray());
    }

    @Test
    public void testCollapse() {
        Stream<ShortList> stream1 = createShortStream((short) 1, (short) 1, (short) 2, (short) 3, (short) 3, (short) 3).collapse((a, b) -> a == b);
        List<ShortList> lists = stream1.toList();
        assertEquals(3, lists.size());
        assertArrayEquals(new short[] { 1, 1 }, lists.get(0).toArray());
        assertArrayEquals(new short[] { 2 }, lists.get(1).toArray());
        assertArrayEquals(new short[] { 3, 3, 3 }, lists.get(2).toArray());

        ShortStream stream2 = createShortStream((short) 1, (short) 1, (short) 2, (short) 3, (short) 3).collapse((a, b) -> a == b, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 2, 2, 6 }, stream2.toArray());
    }

    @Test
    public void testScan() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 3, 6 }, stream.toArray());
    }

    @Test
    public void testScanWithSeed() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream.toArray());
    }

    @Test
    public void testRangeMap() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 5, (short) 6, (short) 10).rangeMap((a, b) -> b - a <= 1,
                (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 3, 11, 20 }, stream.toArray());
    }

    @Test
    public void testMapPartial() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4)
                .mapPartial(n -> n % 2 == 0 ? OptionalShort.of((short) (n * 10)) : OptionalShort.empty());
        assertArrayEquals(new short[] { 20, 40 }, stream.toArray());
    }

    @Test
    public void testClose() {
        final boolean[] closed = { false };
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).onClose(() -> closed[0] = true);
        stream.close();
        assertTrue(closed[0]);
    }

    @Test
    public void testParallel() {
        short[] sequential = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(n -> (short) (n * 2)).toArray();

        short[] parallel = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel().map(n -> (short) (n * 2)).toArray();

        Arrays.sort(sequential);
        Arrays.sort(parallel);
        assertArrayEquals(sequential, parallel);
    }

    @Test
    public void testElementAt() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).elementAt(1);
        assertTrue(result.isPresent());
        assertEquals(20, result.get());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Short>> result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Short> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._50));
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .summaryStatisticsAndPercentiles();

        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(15, result.left().getSum());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testFlatten() {
        short[][] array2d = { { 1, 2 }, { 3, 4 }, { 5 } };
        ShortStream stream1 = ShortStream.flatten(array2d);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream1.toArray());

        short[][][] array3d = { { { 1, 2 }, { 3 } }, { { 4, 5 } } };
        ShortStream stream2 = ShortStream.flatten(array3d);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream2.toArray());
    }

    @Test
    public void testFlattenVertically() {
        short[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6, 7, 8, 9 } };
        ShortStream stream = ShortStream.flatten(array, true);
        assertArrayEquals(new short[] { 1, 4, 6, 2, 5, 7, 3, 8, 9 }, stream.toArray());
    }

    @Test
    public void testZipWithDefault() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20 };

        ShortStream stream = ShortStream.zip(a, b, (short) 0, (short) 100, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 103 }, stream.toArray());
    }

    @Test
    public void testThrowIfEmpty() {
        short[] result1 = createShortStream((short) 1, (short) 2).throwIfEmpty().toArray();
        assertArrayEquals(new short[] { 1, 2 }, result1);

        try {
            ShortStream.empty().throwIfEmpty().toArray();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testRandom() {
        short[] values = ShortStream.random().limit(100).toArray();
        assertEquals(100, values.length);
        for (short value : values) {
            assertTrue(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
        }
    }

    @Test
    public void testDefer() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.defer(() -> {
            count[0]++;
            return createShortStream((short) 1, (short) 2, (short) 3);
        });

        assertEquals(0, count[0]);

        short[] result = stream.toArray();
        assertEquals(1, count[0]);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }
}
