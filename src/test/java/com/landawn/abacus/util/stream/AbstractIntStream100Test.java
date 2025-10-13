package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("new-test")
public class AbstractIntStream100Test extends TestBase {

    private IntStream stream;

    @BeforeEach
    public void setUp() {
        stream = createIntStream(new int[] { 1, 2, 3, 4, 5 });
    }

    protected IntStream createIntStream(int... elements) {
        return IntStream.of(elements);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        IntStream result = stream.rateLimited(rateLimiter);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testRateLimitedWithNullArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.rateLimited(null);
        });
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(1);
        IntStream result = stream.delay(delay);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testDelayWithNullArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.delay((Duration) null);
        });
    }

    @Test
    public void testSkipUntil() {
        IntStream result = stream.skipUntil(x -> x > 3);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 5 }, array);
    }

    @Test
    public void testSkipUntilNoMatch() {
        IntStream result = stream.skipUntil(x -> x > 10);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] {}, array);
    }

    @Test
    public void testDistinct() {
        IntStream distinctStream = createIntStream(new int[] { 1, 2, 2, 3, 3, 3, 4, 5, 5 });
        int[] result = distinctStream.distinct().toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlatmap() {
        IntStream result = stream.flatmap(x -> new int[] { x, x * 2 });
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, array);
    }

    @Test
    public void testFlattmap() {
        IntStream result = stream.flattMap(x -> java.util.stream.IntStream.of(x, x * 2));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, array);
    }

    @Test
    public void testFlatmapToObj() {
        Stream<String> result = stream.flatmapToObj(x -> Arrays.asList(String.valueOf(x), String.valueOf(x * 2)));
        String[] array = result.toArray(String[]::new);
        Assertions.assertArrayEquals(new String[] { "1", "2", "2", "4", "3", "6", "4", "8", "5", "10" }, array);
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> result = stream.flattmapToObj(x -> new String[] { String.valueOf(x), String.valueOf(x * 2) });
        String[] array = result.toArray(String[]::new);
        Assertions.assertArrayEquals(new String[] { "1", "2", "2", "4", "3", "6", "4", "8", "5", "10" }, array);
    }

    @Test
    public void testMapMulti() {
        IntStream result = stream.mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 2);
        });
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, array);
    }

    @Test
    public void testMapPartial() {
        IntStream result = stream.mapPartial(x -> x % 2 == 0 ? OptionalInt.of(x * 2) : OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 8 }, array);
    }

    @Test
    public void testMapPartialJdk() {
        IntStream result = stream.mapPartialJdk(x -> x % 2 == 0 ? java.util.OptionalInt.of(x * 2) : java.util.OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 8 }, array);
    }

    @Test
    public void testRangeMap() {
        IntStream rangeStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        IntStream result = rangeStream.rangeMap((left, right) -> Math.abs(left - right) <= 1, Integer::sum);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 6, 11, 16 }, array);
    }

    @Test
    public void testRangeMapToObj() {
        IntStream rangeStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        Stream<String> result = rangeStream.rangeMapToObj((left, right) -> Math.abs(left - right) <= 1, (left, right) -> left + "-" + right);
        String[] array = result.toArray(String[]::new);
        Assertions.assertArrayEquals(new String[] { "1-2", "3-3", "5-6", "8-8" }, array);
    }

    @Test
    public void testCollapse() {
        IntStream collapseStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        Stream<IntList> result = collapseStream.collapse((a, b) -> Math.abs(a - b) <= 1);
        List<IntList> lists = result.toList();
        assertEquals(3, lists.size());
        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, lists.get(0).toArray());
        Assertions.assertArrayEquals(new int[] { 5, 6 }, lists.get(1).toArray());
        Assertions.assertArrayEquals(new int[] { 8 }, lists.get(2).toArray());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        IntStream collapseStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        IntStream result = collapseStream.collapse((a, b) -> Math.abs(a - b) <= 1, Integer::sum);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 6, 11, 8 }, array);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        IntStream collapseStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        IntStream result = collapseStream.collapse((first, prev, next) -> next - first <= 2, Integer::sum);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 6, 11, 8 }, array);
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        IntStream result = stream.skip(2, skipped::add);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 4, 5 }, array);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream result = stream.filter(x -> x % 2 == 0, dropped::add);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 2, 4 }, array);
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream result = stream.dropWhile(x -> x < 3, dropped::add);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 4, 5 }, array);
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testStep() {
        IntStream stepStream = createIntStream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        IntStream result = stepStream.step(3);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 4, 7, 10 }, array);
    }

    @Test
    public void testStepWithOne() {
        IntStream result = stream.step(1);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testScan() {
        IntStream result = stream.scan((a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, array);
    }

    @Test
    public void testScanWithInit() {
        IntStream result = stream.scan(10, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, array);
    }

    @Test
    public void testScanWithInitIncluded() {
        IntStream result = stream.scan(10, true, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, array);
    }

    @Test
    public void testScanWithInitNotIncluded() {
        IntStream result = stream.scan(10, false, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, array);
    }

    @Test
    public void testTop() {
        IntStream topStream = createIntStream(new int[] { 5, 2, 8, 1, 9, 3 });
        IntStream result = topStream.top(3);
        int[] array = result.toArray();
        Arrays.sort(array);
        Assertions.assertArrayEquals(new int[] { 5, 8, 9 }, array);
    }

    @Test
    public void testIntersection() {
        IntStream result = stream.intersection(Arrays.asList(2, 3, 4, 6, 7));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 2, 3, 4 }, array);
    }

    @Test
    public void testDifference() {
        IntStream result = stream.difference(Arrays.asList(2, 3, 4, 6, 7));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 5 }, array);
    }

    @Test
    public void testSymmetricDifference() {
        IntStream result = stream.symmetricDifference(Arrays.asList(2, 3, 4, 6, 7));
        int[] array = result.toArray();
        Arrays.sort(array);
        Assertions.assertArrayEquals(new int[] { 1, 5, 6, 7 }, array);
    }

    @Test
    public void testReversed() {
        IntStream result = stream.reversed();
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, array);
    }

    @Test
    public void testRotated() {
        IntStream result = stream.rotated(2);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, array);
    }

    @Test
    public void testRotatedNegative() {
        IntStream result = stream.rotated(-2);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 4, 5, 1, 2 }, array);
    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(42);
        IntStream result = stream.shuffled(rnd);
        int[] array = result.toArray();
        assertEquals(5, array.length);
        Arrays.sort(array);
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testSorted() {
        IntStream unsortedStream = createIntStream(new int[] { 5, 2, 8, 1, 3 });
        IntStream result = unsortedStream.sorted();
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 5, 8 }, array);
    }

    @Test
    public void testReverseSorted() {
        IntStream unsortedStream = createIntStream(new int[] { 5, 2, 8, 1, 3 });
        IntStream result = unsortedStream.reverseSorted();
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 8, 5, 3, 2, 1 }, array);
    }

    @Test
    public void testCycled() {
        IntStream cycledStream = createIntStream(new int[] { 1, 2, 3 });
        IntStream result = cycledStream.cycled().limit(10);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, array);
    }

    @Test
    public void testCycledWithRounds() {
        IntStream cycledStream = createIntStream(new int[] { 1, 2, 3 });
        IntStream result = cycledStream.cycled(2);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3 }, array);
    }

    @Test
    public void testCycledWithZeroRounds() {
        IntStream result = stream.cycled(0);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] {}, array);
    }

    @Test
    public void testIndexed() {
        Stream<IndexedInt> result = stream.indexed();
        List<IndexedInt> list = result.toList();
        assertEquals(5, list.size());
        assertEquals(1, list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals(5, list.get(4).value());
        assertEquals(4, list.get(4).index());
    }

    @Test
    public void testBoxed() {
        Stream<Integer> result = stream.boxed();
        Integer[] array = result.toArray(Integer[]::new);
        Assertions.assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrepend() {
        IntStream result = stream.prepend(0, -1);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 0, -1, 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependStream() {
        IntStream prependStream = createIntStream(new int[] { -2, -1, 0 });
        IntStream result = stream.prepend(prependStream);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependOptionalInt() {
        IntStream result = stream.prepend(OptionalInt.of(0));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependEmptyOptionalInt() {
        IntStream result = stream.prepend(OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testAppend() {
        IntStream result = stream.append(6, 7);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7 }, array);
    }

    @Test
    public void testAppendStream() {
        IntStream appendStream = createIntStream(new int[] { 6, 7, 8 });
        IntStream result = stream.append(appendStream);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, array);
    }

    @Test
    public void testAppendOptionalInt() {
        IntStream result = stream.append(OptionalInt.of(6));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, array);
    }

    @Test
    public void testAppendEmptyOptionalInt() {
        IntStream result = stream.append(OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testAppendIfEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        IntStream result = emptyStream.appendIfEmpty(1, 2, 3);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, array);
    }

    @Test
    public void testAppendIfEmptyNotEmpty() {
        IntStream result = stream.appendIfEmpty(10, 20);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testMergeWith() {
        IntStream otherStream = createIntStream(new int[] { 2, 4, 6 });
        IntStream result = stream.mergeWith(otherStream, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 3, 4, 4, 5, 6 }, array);
    }

    @Test
    public void testZipWith() {
        IntStream otherStream = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream result = stream.zipWith(otherStream, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 22, 33, 44, 55 }, array);
    }

    @Test
    public void testZipWithThreeStreams() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream stream3 = createIntStream(new int[] { 100, 200, 300, 400, 500 });
        IntStream result = stream.zipWith(stream2, stream3, (a, b, c) -> a + b + c);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 111, 222, 333, 444, 555 }, array);
    }

    @Test
    public void testZipWithDefault() {
        IntStream otherStream = createIntStream(new int[] { 10, 20 });
        IntStream result = stream.zipWith(otherStream, 0, 100, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 22, 103, 104, 105 }, array);
    }

    @Test
    public void testZipWithDefaultThreeStreams() {
        IntStream stream2 = createIntStream(new int[] { 10, 20 });
        IntStream stream3 = createIntStream(new int[] { 100 });
        IntStream result = stream.zipWith(stream2, stream3, 0, 50, 500, (a, b, c) -> a + b + c);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 111, 522, 553, 554, 555 }, array);
    }

    @Test
    public void testToMap() throws Exception {
        Map<String, Integer> result = stream.toMap(x -> "key" + x, x -> x * 10);
        assertEquals(5, result.size());
        assertEquals(10, result.get("key1"));
        assertEquals(50, result.get("key5"));
    }

    @Test
    public void testToMapWithMapFactory() throws Exception {
        Map<String, Integer> result = stream.toMap(x -> "key" + x, x -> x * 10, Suppliers.ofLinkedHashMap());
        Assertions.assertTrue(result instanceof LinkedHashMap);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMapWithMergeFunction() throws Exception {
        IntStream duplicateStream = createIntStream(new int[] { 1, 1, 2, 2, 3 });
        Map<String, Integer> result = duplicateStream.toMap(x -> "key" + x, x -> x, (a, b) -> a + b);
        assertEquals(3, result.size());
        assertEquals(2, result.get("key1"));
        assertEquals(4, result.get("key2"));
    }

    @Test
    public void testToMapWithMergeFunctionAndMapFactory() throws Exception {
        IntStream duplicateStream = createIntStream(new int[] { 1, 1, 2, 2, 3 });
        Map<String, Integer> result = duplicateStream.toMap(x -> "key" + x, x -> x, (a, b) -> a + b, TreeMap::new);
        Assertions.assertTrue(result instanceof TreeMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupTo() throws Exception {
        Map<String, List<Integer>> result = stream.groupTo(x -> x % 2 == 0 ? "even" : "odd", Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2, 4), result.get("even"));
        assertEquals(Arrays.asList(1, 3, 5), result.get("odd"));
    }

    @Test
    public void testGroupToWithMapFactory() throws Exception {
        Map<String, Long> result = stream.groupTo(x -> x % 2 == 0 ? "even" : "odd", Collectors.counting(), TreeMap::new);
        Assertions.assertTrue(result instanceof TreeMap);
        assertEquals(2L, result.get("even"));
        assertEquals(3L, result.get("odd"));
    }

    @Test
    public void testForEachIndexed() throws Exception {
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:4", "4:5"), result);
    }

    @Test
    public void testFirst() {
        OptionalInt result = stream.first();
        Assertions.assertTrue(result.isPresent());
        assertEquals(1, result.orElseThrow());
    }

    @Test
    public void testFirstEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt result = emptyStream.first();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        OptionalInt result = stream.last();
        Assertions.assertTrue(result.isPresent());
        assertEquals(5, result.orElseThrow());
    }

    @Test
    public void testLastEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt result = emptyStream.last();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        IntStream singleStream = createIntStream(new int[] { 42 });
        OptionalInt result = singleStream.onlyOne();
        Assertions.assertTrue(result.isPresent());
        assertEquals(42, result.orElseThrow());
    }

    @Test
    public void testOnlyOneEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt result = emptyStream.onlyOne();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOneTooMany() {
        Assertions.assertThrows(TooManyElementsException.class, () -> {
            stream.onlyOne();
        });
    }

    @Test
    public void testFindAny() throws Exception {
        OptionalInt result = stream.findAny(x -> x > 3);
        Assertions.assertTrue(result.isPresent());
        assertEquals(4, result.orElseThrow());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Integer>> result = stream.percentiles();
        Assertions.assertTrue(result.isPresent());
        Map<Percentage, Integer> percentiles = result.get();
        Assertions.assertTrue(percentiles.containsKey(Percentage._20));
        Assertions.assertTrue(percentiles.containsKey(Percentage._50));
        Assertions.assertTrue(percentiles.containsKey(Percentage._70));
    }

    @Test
    public void testPercentilesEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        Optional<Map<Percentage, Integer>> result = emptyStream.percentiles();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = stream.summarizeAndPercentiles();

        IntSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());

        Optional<Map<Percentage, Integer>> percentiles = result.right();
        Assertions.assertTrue(percentiles.isPresent());
    }

    @Test
    public void testJoin() {
        String result = stream.join(",", "[", "]");
        assertEquals("[1,2,3,4,5]", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with("-");
        Joiner result = stream.joinTo(joiner);
        assertEquals("1-2-3-4-5", result.toString());
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        List<Integer> result = stream.collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testIterator() {
        IntIterator iter = stream.iterator();
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextInt());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
}
