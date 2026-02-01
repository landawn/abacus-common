package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteTriPredicate;

@Tag("new-test")
public class AbstractByteStream102Test extends TestBase {

    private ByteStream stream;
    private byte[] testData;

    protected ByteStream createByteStream(byte... a) {
        return ByteStream.of(a).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Byte[] a) {
        return ByteStream.of(a).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Collection<Byte> coll) {
        return ByteStream.of(coll.toArray(new Byte[coll.size()])).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(ByteIterator iter) {
        return iter == null ? ByteStream.empty() : ByteStream.of(iter.toArray()).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(ByteBuffer buff) {
        return ByteStream.of(buff).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(File file) {
        return ByteStream.of(file).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(InputStream is) {
        return ByteStream.of(is).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(InputStream is, boolean closeInputStreamOnClose) {
        return ByteStream.of(is, closeInputStreamOnClose).map(e -> (byte) (e + 0));
    }

    @BeforeEach
    public void setUp() {
        testData = new byte[] { 1, 2, 3, 4, 5 };
        stream = createByteStream(testData);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        ByteStream result = stream.rateLimited(rateLimiter);
        assertNotNull(result);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testRateLimitedWithNullRateLimiter() {
        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        long startTime = System.currentTimeMillis();
        ByteStream result = stream.delay(delay);
        byte[] array = result.toArray();
        long endTime = System.currentTimeMillis();

        assertArrayEquals(testData, array);
        assertTrue((endTime - startTime) >= 40);
    }

    @Test
    public void testDelayWithNullDuration() {
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testSkipUntil() {
        BytePredicate predicate = value -> value > 3;
        ByteStream result = stream.skipUntil(predicate);
        assertArrayEquals(new byte[] { 4, 5 }, result.toArray());
    }

    @Test
    public void testSkipUntilNoMatch() {
        BytePredicate predicate = value -> value > 10;
        ByteStream result = stream.skipUntil(predicate);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testDistinct() {
        byte[] duplicates = { 1, 2, 2, 3, 3, 3, 4, 5, 5 };
        stream = createByteStream(duplicates);
        ByteStream result = stream.distinct();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testFlatmap() {
        ByteFunction<byte[]> mapper = value -> new byte[] { value, (byte) (value * 2) };
        ByteStream result = stream.flatmap(mapper);
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, result.toArray());
    }

    @Test
    public void testFlatmapToObj() {
        ByteFunction<Collection<String>> mapper = value -> Arrays.asList("" + value, "" + (value * 2));
        Stream<String> result = stream.flatmapToObj(mapper);
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6", "4", "8", "5", "10"), result.toList());
    }

    @Test
    public void testFlattMapToObj() {
        ByteFunction<String[]> mapper = value -> new String[] { "" + value, "" + (value * 2) };
        Stream<String> result = stream.flattmapToObj(mapper);
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6", "4", "8", "5", "10"), result.toList());
    }

    @Test
    public void testMapPartial() {
        ByteFunction<OptionalByte> mapper = value -> value % 2 == 0 ? OptionalByte.of((byte) (value * 2)) : OptionalByte.empty();
        ByteStream result = stream.mapPartial(mapper);
        assertArrayEquals(new byte[] { 4, 8 }, result.toArray());
    }

    @Test
    public void testRangeMap() {
        byte[] data = { 1, 2, 3, 5, 6, 8 };
        stream = createByteStream(data);
        ByteBiPredicate sameRange = (a, b) -> Math.abs(b - a) == 1;
        ByteBinaryOperator mapper = (first, last) -> (byte) ((first + last) / 2);
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(new byte[] { 1, 3, 5, 8 }, result.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        ByteBiPredicate sameRange = (a, b) -> Math.abs(b - a) == 1;
        ByteBiFunction<String> mapper = (first, last) -> first + "-" + last;
        Stream<String> result = stream.rangeMapToObj(sameRange, mapper);
        assertEquals(Arrays.asList("1-2", "3-4", "5-5"), result.toList());
    }

    @Test
    public void testCollapseToPairs() {
        ByteBiPredicate collapsible = (a, b) -> Math.abs(b - a) <= 1;
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(testData, lists.get(0).toArray());
    }

    @Test
    public void testCollapseWithBinaryOperator() {
        ByteBiPredicate collapsible = (a, b) -> true;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);
        ByteStream result = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 15 }, result.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ByteTriPredicate collapsible = (first, prev, curr) -> true;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);
        ByteStream result = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 15 }, result.toArray());
    }

    @Test
    public void testSkipWithAction() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = skipped::add;
        ByteStream result = stream.skip(2, action);
        assertArrayEquals(new byte[] { 3, 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);
    }

    @Test
    public void testFilterWithAction() {
        List<Byte> dropped = new ArrayList<>();
        BytePredicate predicate = value -> value % 2 == 0;
        ByteConsumer actionOnDropped = dropped::add;
        ByteStream result = stream.filter(predicate, actionOnDropped);
        assertArrayEquals(new byte[] { 2, 4 }, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Byte> dropped = new ArrayList<>();
        BytePredicate predicate = value -> value < 3;
        ByteConsumer actionOnDropped = dropped::add;
        ByteStream result = stream.dropWhile(predicate, actionOnDropped);
        assertArrayEquals(new byte[] { 3, 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), dropped);
    }

    @Test
    public void testStep() {
        ByteStream result = stream.step(2);
        assertArrayEquals(new byte[] { 1, 3, 5 }, result.toArray());
    }

    @Test
    public void testStepWithLargeStep() {
        ByteStream result = stream.step(3);
        assertArrayEquals(new byte[] { 1, 4 }, result.toArray());
    }

    @Test
    public void testStepWithInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
    }

    @Test
    public void testScan() {
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        ByteStream result = stream.scan(accumulator);
        assertArrayEquals(new byte[] { 1, 3, 6, 10, 15 }, result.toArray());
    }

    @Test
    public void testScanWithInit() {
        byte init = 10;
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        ByteStream result = stream.scan(init, accumulator);
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 }, result.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        byte init = 10;
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        ByteStream result = stream.scan(init, true, accumulator);
        assertArrayEquals(new byte[] { 10, 11, 13, 16, 20, 25 }, result.toArray());
    }

    @Test
    public void testIntersection() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6);
        ByteStream result = stream.intersection(c);
        assertArrayEquals(new byte[] { 2, 3 }, result.toArray());
    }

    @Test
    public void testDifference() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6);
        ByteStream result = stream.difference(c);
        assertArrayEquals(new byte[] { 1, 4, 5 }, result.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6);
        ByteStream result = stream.symmetricDifference(c);
        assertArrayEquals(new byte[] { 1, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testReversed() {
        ByteStream result = stream.reversed();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result.toArray());
    }

    @Test
    public void testRotated() {
        ByteStream result = stream.rotated(2);
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testRotatedNegative() {
        ByteStream result = stream.rotated(-2);
        assertArrayEquals(new byte[] { 3, 4, 5, 1, 2 }, result.toArray());
    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(42);
        ByteStream result = stream.shuffled(rnd);
        byte[] shuffled = result.toArray();
        assertEquals(5, shuffled.length);
        Set<Byte> set = new HashSet<>();
        for (byte b : shuffled) {
            set.add(b);
        }
        assertEquals(5, set.size());
    }

    @Test
    public void testSorted() {
        byte[] unsorted = { 5, 3, 1, 4, 2 };
        stream = createByteStream(unsorted);
        ByteStream result = stream.sorted();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testReverseSorted() {
        ByteStream result = stream.reverseSorted();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result.toArray());
    }

    @Test
    public void testCycled() {
        ByteStream result = stream.cycled().limit(12);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2 }, result.toArray());
    }

    @Test
    public void testCycledWithRounds() {
        ByteStream result = stream.cycled(2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testIndexed() {
        Stream<IndexedByte> result = stream.indexed();
        List<IndexedByte> indexed = result.toList();
        assertEquals(5, indexed.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, indexed.get(i).index());
            assertEquals(testData[i], indexed.get(i).value());
        }
    }

    @Test
    public void testBoxed() {
        Stream<Byte> result = stream.boxed();
        List<Byte> boxed = result.toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), boxed);
    }

    @Test
    public void testPrependArray() {
        ByteStream result = stream.prepend((byte) 0);
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testPrependStream() {
        ByteStream toAdd = createByteStream((byte) -1, (byte) 0);
        ByteStream result = stream.prepend(toAdd);
        assertArrayEquals(new byte[] { -1, 0, 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testPrependOptional() {
        OptionalByte op = OptionalByte.of((byte) 0);
        ByteStream result = stream.prepend(op);
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testPrependEmptyOptional() {
        OptionalByte op = OptionalByte.empty();
        ByteStream result = stream.prepend(op);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testAppendArray() {
        ByteStream result = stream.append((byte) 6);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testAppendStream() {
        ByteStream toAdd = createByteStream((byte) 6, (byte) 7);
        ByteStream result = stream.append(toAdd);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, result.toArray());
    }

    @Test
    public void testAppendOptional() {
        OptionalByte op = OptionalByte.of((byte) 6);
        ByteStream result = stream.append(op);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        stream = ByteStream.empty();
        ByteStream result = stream.appendIfEmpty((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, result.toArray());
    }

    @Test
    public void testAppendIfEmptyNotEmpty() {
        ByteStream result = stream.appendIfEmpty((byte) 6, (byte) 7);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testMergeWith() {
        ByteStream other = createByteStream((byte) 2, (byte) 4, (byte) 6);
        ByteBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ByteStream result = stream.mergeWith(other, selector);
        assertArrayEquals(new byte[] { 1, 2, 2, 3, 4, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testZipWith() {
        ByteStream other = createByteStream((byte) 10, (byte) 20, (byte) 30);
        ByteBinaryOperator zipFunction = (a, b) -> (byte) (a + b);
        ByteStream result = stream.zipWith(other, zipFunction);
        assertArrayEquals(new byte[] { 11, 22, 33 }, result.toArray());
    }

    @Test
    public void testZipWithThree() {
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        ByteStream c = createByteStream((byte) 100, (byte) 200);
        ByteTernaryOperator zipFunction = (x, y, z) -> (byte) (x + y + z);
        ByteStream result = stream.zipWith(b, c, zipFunction);
        assertArrayEquals(new byte[] { 111, (byte) (2 + 20 + 200) }, result.toArray());
    }

    @Test
    public void testZipWithDefault() {
        ByteStream other = createByteStream((byte) 10, (byte) 20);
        ByteBinaryOperator zipFunction = (a, b) -> (byte) (a + b);
        ByteStream result = stream.zipWith(other, (byte) 0, (byte) 100, zipFunction);
        assertArrayEquals(new byte[] { 11, 22, 103, 104, 105 }, result.toArray());
    }

    @Test
    public void testZipWithThreeDefaults() {
        ByteStream b = createByteStream((byte) 10, (byte) 20);
        ByteStream c = createByteStream((byte) 100);
        ByteTernaryOperator zipFunction = (x, y, z) -> (byte) (x + y + z);
        ByteStream result = stream.zipWith(b, c, (byte) 0, (byte) 0, (byte) 0, zipFunction);
        assertArrayEquals(new byte[] { 111, 22, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testToMap() {
        Throwables.ByteFunction<String, RuntimeException> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, RuntimeException> valueMapper = value -> (int) value;
        Map<String, Integer> result = stream.toMap(keyMapper, valueMapper);

        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(1), result.get("key1"));
        assertEquals(Integer.valueOf(5), result.get("key5"));
    }

    @Test
    public void testToMapWithFactory() {
        Throwables.ByteFunction<String, RuntimeException> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, RuntimeException> valueMapper = value -> (int) value;
        Map<String, Integer> result = stream.toMap(keyMapper, valueMapper, Suppliers.ofMap());

        assertEquals(5, result.size());
        assertTrue(result instanceof HashMap);
    }

    @Test
    public void testToMapWithMergeFunction() {
        byte[] duplicates = { 1, 2, 2, 3, 3 };
        stream = createByteStream(duplicates);
        Throwables.ByteFunction<String, RuntimeException> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, RuntimeException> valueMapper = value -> 1;
        BinaryOperator<Integer> mergeFunction = Integer::sum;

        Map<String, Integer> result = stream.toMap(keyMapper, valueMapper, mergeFunction);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(2), result.get("key2"));
        assertEquals(Integer.valueOf(2), result.get("key3"));
    }

    @Test
    public void testGroupTo() {
        Throwables.ByteFunction<String, RuntimeException> keyMapper = value -> value % 2 == 0 ? "even" : "odd";
        Collector<Byte, ?, List<Byte>> downstream = Collectors.toList();
        Map<String, List<Byte>> result = stream.groupTo(keyMapper, downstream);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), result.get("odd"));
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result.get("even"));
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:4", "4:5"), result);
    }

    @Test
    public void testFirst() {
        OptionalByte result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFirstEmpty() {
        stream = ByteStream.empty();
        OptionalByte result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        OptionalByte result = stream.last();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testLastEmpty() {
        stream = ByteStream.empty();
        OptionalByte result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        stream = createByteStream((byte) 42);
        OptionalByte result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testOnlyOneEmpty() {
        stream = ByteStream.empty();
        OptionalByte result = stream.onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOneTooMany() {
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testFindAny() {
        OptionalByte result = stream.findAny(value -> value > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testFindAnyNoMatch() {
        OptionalByte result = stream.findAny(value -> value > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Byte>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Byte> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._50));
    }

    @Test
    public void testPercentilesEmpty() {
        stream = ByteStream.empty();
        Optional<Map<Percentage, Byte>> result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();

        ByteSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum().intValue());

        assertTrue(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        String result = stream.join(", ", "[", "]");
        assertEquals("[1, 2, 3, 4, 5]", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = stream.joinTo(joiner);
        assertEquals("[1, 2, 3, 4, 5]", result.toString());
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        List<Byte> result = stream.collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testIterator() {
        ByteIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
    }
}
