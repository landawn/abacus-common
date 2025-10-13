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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
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
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteTriPredicate;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class AbstractByteStream105Test extends TestBase {

    private ByteStream stream;
    private ByteStream stream2;

    @BeforeEach
    public void setUp() {
        stream = createByteStream(new byte[] { 1, 2, 3, 4, 5 });
        stream2 = createByteStream(new byte[] { 1, 2, 3, 4, 5 });
    }

    private ByteStream createByteStream(byte... elements) {
        return ByteStream.of(elements).map(e -> (byte) (e + 0));
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(1000);
        ByteStream result = stream.rateLimited(rateLimiter);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        ByteStream result = stream.delay(delay);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testSkipUntil() {
        BytePredicate predicate = value -> value > 3;
        byte[] result = stream.skipUntil(predicate).toArray();
        assertArrayEquals(new byte[] { 4, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.skipUntil(value -> false).toArray();
        assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testDistinct() {
        stream = createByteStream(new byte[] { 1, 2, 2, 3, 3, 3, 4, 5, 5 });
        byte[] result = stream.distinct().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.distinct().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFlatmap() {
        ByteFunction<byte[]> mapper = value -> new byte[] { value, (byte) (value * 2) };
        byte[] result = stream.flatmap(mapper).toArray();
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, result);

        stream = createByteStream(new byte[] { 1, 2 });
        result = stream.flatmap(value -> new byte[0]).toArray();
        assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testFlatmapToObj() {
        ByteFunction<Collection<String>> mapper = value -> Arrays.asList("" + value, "" + (value * 2));
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6", "4", "8", "5", "10"), result);
    }

    @Test
    public void testFlattMapToObj() {
        ByteFunction<String[]> mapper = value -> new String[] { "" + value, "" + (value * 2) };
        List<String> result = stream.flattmapToObj(mapper).toList();
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6", "4", "8", "5", "10"), result);
    }

    @Test
    public void testMapPartial() {
        ByteFunction<OptionalByte> mapper = value -> value % 2 == 0 ? OptionalByte.of(value) : OptionalByte.empty();
        byte[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new byte[] { 2, 4 }, result);
    }

    @Test
    public void testRangeMap() {
        stream = createByteStream(new byte[] { 1, 2, 3, 10, 11, 20, 21 });
        ByteBiPredicate sameRange = (a, b) -> Math.abs(b - a) <= 2;
        ByteBinaryOperator mapper = (a, b) -> (byte) ((a + b) / 2);

        byte[] result = stream.rangeMap(sameRange, mapper).toArray();
        assertArrayEquals(new byte[] { 2, 10, 20 }, result);
    }

    @Test
    public void testRangeMapToObj() {
        stream = createByteStream(new byte[] { 1, 2, 3, 10, 11, 20, 21 });
        ByteBiPredicate sameRange = (a, b) -> Math.abs(b - a) <= 2;
        ByteBiFunction<String> mapper = (a, b) -> a + "-" + b;

        List<String> result = stream.rangeMapToObj(sameRange, mapper).toList();
        assertEquals(Arrays.asList("1-3", "10-11", "20-21"), result);
    }

    @Test
    public void testCollapse() {
        stream = createByteStream(new byte[] { 1, 2, 5, 6, 7, 10 });
        ByteBiPredicate collapsible = (a, b) -> Math.abs(b - a) <= 2;

        List<ByteList> result = stream.collapse(collapsible).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new byte[] { 1, 2 }, result.get(0).toArray());
        assertArrayEquals(new byte[] { 5, 6, 7 }, result.get(1).toArray());
        assertArrayEquals(new byte[] { 10 }, result.get(2).toArray());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        stream = createByteStream(new byte[] { 1, 2, 5, 6, 7, 10 });
        ByteBiPredicate collapsible = (a, b) -> Math.abs(b - a) <= 2;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);

        byte[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertArrayEquals(new byte[] { 3, 18, 10 }, result);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        stream = createByteStream(new byte[] { 1, 3, 6, 10, 15, 16 });
        ByteTriPredicate collapsible = (first, last, next) -> next - first <= 10;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);

        byte[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertArrayEquals(new byte[] { 20, 31 }, result);
    }

    @Test
    public void testSkipWithAction() {
        List<Byte> skippedElements = new ArrayList<>();
        ByteConsumer action = skippedElements::add;

        byte[] result = stream.skip(2, action).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skippedElements);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.skip(0, action).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFilterWithAction() {
        List<Byte> droppedElements = new ArrayList<>();
        ByteConsumer action = droppedElements::add;
        BytePredicate predicate = value -> value % 2 == 0;

        byte[] result = stream.filter(predicate, action).toArray();
        assertArrayEquals(new byte[] { 2, 4 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), droppedElements);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Byte> droppedElements = new ArrayList<>();
        ByteConsumer action = droppedElements::add;
        BytePredicate predicate = value -> value < 3;

        byte[] result = stream.dropWhile(predicate, action).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), droppedElements);
    }

    @Test
    public void testStep() {
        byte[] result = stream.step(2).toArray();
        assertArrayEquals(new byte[] { 1, 3, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.step(1).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
        assertThrows(IllegalArgumentException.class, () -> stream2.step(-1));
    }

    @Test
    public void testScan() {
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        byte[] result = stream.scan(accumulator).toArray();
        assertArrayEquals(new byte[] { 1, 3, 6, 10, 15 }, result);

        stream = createByteStream(new byte[0]);
        result = stream.scan(accumulator).toArray();
        assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testScanWithInit() {
        byte init = 10;
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        byte[] result = stream.scan(init, accumulator).toArray();
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 }, result);
    }

    @Test
    public void testScanWithInitIncluded() {
        byte init = 10;
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);

        byte[] result = stream.scan(init, true, accumulator).toArray();
        assertArrayEquals(new byte[] { 10, 11, 13, 16, 20, 25 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3, 4, 5 });
        result = stream.scan(init, false, accumulator).toArray();
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 }, result);
    }

    @Test
    public void testIntersection() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6, (byte) 3);
        byte[] result = stream.intersection(c).toArray();
        assertArrayEquals(new byte[] { 2, 3 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.intersection(Collections.emptyList()).toArray();
        assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testDifference() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6);
        byte[] result = stream.difference(c).toArray();
        assertArrayEquals(new byte[] { 1, 4, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.difference(Collections.emptyList()).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6, (byte) 7);
        byte[] result = stream.symmetricDifference(c).toArray();
        assertArrayEquals(new byte[] { 1, 4, 5, 6, 7 }, result);
    }

    @Test
    public void testReversed() {
        byte[] result = stream.reversed().toArray();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result);

        stream = createByteStream(new byte[0]);
        result = stream.reversed().toArray();
        assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testRotated() {
        byte[] result = stream.rotated(2).toArray();
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3, 4, 5 });
        result = stream.rotated(-2).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5, 1, 2 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.rotated(0).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(123);
        stream = createByteStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        byte[] result = stream.shuffled(rnd).toArray();

        assertEquals(10, result.length);
        Set<Byte> resultSet = new HashSet<>();
        for (byte b : result) {
            resultSet.add(b);
        }
        assertEquals(10, resultSet.size());

        assertThrows(IllegalArgumentException.class, () -> stream2.shuffled(null));
    }

    @Test
    public void testSorted() {
        stream = createByteStream(new byte[] { 5, 3, 1, 4, 2 });
        byte[] result = stream.sorted().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.sorted().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testReverseSorted() {
        stream = createByteStream(new byte[] { 5, 3, 1, 4, 2 });
        byte[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testCycled() {
        stream = createByteStream(new byte[] { 1, 2, 3 });
        byte[] result = stream.cycled().limit(10).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, result);

        stream = createByteStream(new byte[0]);
        result = stream.cycled().limit(5).toArray();
        assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testCycledWithRounds() {
        stream = createByteStream(new byte[] { 1, 2, 3 });

        byte[] result = stream.cycled(0).toArray();
        assertArrayEquals(new byte[0], result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.cycled(1).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        stream = createByteStream(new byte[] { 1, 2 });
        result = stream.cycled(3).toArray();
        assertArrayEquals(new byte[] { 1, 2, 1, 2, 1, 2 }, result);

        assertThrows(IllegalArgumentException.class, () -> stream2.cycled(-1));
    }

    @Test
    public void testIndexed() {
        List<IndexedByte> result = stream.indexed().toList();
        assertEquals(5, result.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, result.get(i).value());
            assertEquals(i, result.get(i).index());
        }
    }

    @Test
    public void testBoxed() {
        List<Byte> result = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testPrependArray() {
        byte[] result = stream.prepend((byte) 10, (byte) 20).toArray();
        assertArrayEquals(new byte[] { 10, 20, 1, 2, 3, 4, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2 });
        result = stream.prepend().toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testPrependStream() {
        ByteStream toAdd = ByteStream.of((byte) 10, (byte) 20);
        byte[] result = stream.prepend(toAdd).toArray();
        assertArrayEquals(new byte[] { 10, 20, 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testPrependOptional() {
        OptionalByte op = OptionalByte.of((byte) 10);
        byte[] result = stream.prepend(op).toArray();
        assertArrayEquals(new byte[] { 10, 1, 2, 3, 4, 5 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        op = OptionalByte.empty();
        result = stream.prepend(op).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAppendArray() {
        byte[] result = stream.append((byte) 10, (byte) 20).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 10, 20 }, result);

        stream = createByteStream(new byte[] { 1, 2 });
        result = stream.append().toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testAppendStream() {
        ByteStream toAdd = ByteStream.of((byte) 10, (byte) 20);
        byte[] result = stream.append(toAdd).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 10, 20 }, result);
    }

    @Test
    public void testAppendOptional() {
        OptionalByte op = OptionalByte.of((byte) 10);
        byte[] result = stream.append(op).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 10 }, result);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        op = OptionalByte.empty();
        result = stream.append(op).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAppendIfEmpty() {
        byte[] result = stream.appendIfEmpty((byte) 10, (byte) 20).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);

        stream = createByteStream(new byte[0]);
        result = stream.appendIfEmpty((byte) 10, (byte) 20).toArray();
        assertArrayEquals(new byte[] { 10, 20 }, result);
    }

    @Test
    public void testMergeWith() {
        ByteStream b = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        ByteBiFunction<MergeResult> nextSelector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        byte[] result = stream.mergeWith(b, nextSelector).toArray();
        assertArrayEquals(new byte[] { 1, 2, 2, 3, 4, 4, 5, 6 }, result);
    }

    @Test
    public void testZipWith() {
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        ByteBinaryOperator zipFunction = (x, y) -> (byte) (x + y);

        byte[] result = stream.zipWith(b, zipFunction).toArray();
        assertArrayEquals(new byte[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        ByteStream c = ByteStream.of((byte) 50, (byte) 60, (byte) 70);
        ByteTernaryOperator zipFunction = (x, y, z) -> (byte) (x + y + z);

        byte[] result = stream.zipWith(b, c, zipFunction).toArray();
        assertArrayEquals(new byte[] { 61, 82, 103 }, result);
    }

    @Test
    public void testZipWithDefaults() {
        ByteStream b = ByteStream.of((byte) 10, (byte) 20);
        byte valueForNoneA = 0;
        byte valueForNoneB = 100;
        ByteBinaryOperator zipFunction = (x, y) -> (byte) (x + y);

        byte[] result = stream.zipWith(b, valueForNoneA, valueForNoneB, zipFunction).toArray();
        assertArrayEquals(new byte[] { 11, 22, 103, 104, 105 }, result);
    }

    @Test
    public void testZipWithThreeStreamsDefaults() {
        ByteStream b = ByteStream.of((byte) 10, (byte) 20);
        ByteStream c = ByteStream.of((byte) 20);
        byte valueForNoneA = 0;
        byte valueForNoneB = 20;
        byte valueForNoneC = 50;
        ByteTernaryOperator zipFunction = (x, y, z) -> (byte) (x + y + z);

        byte[] result = stream.zipWith(b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction).toArray();
        assertArrayEquals(new byte[] { 31, 72, 73, 74, 75 }, result);
    }

    @Test
    public void testToMap() throws Exception {
        Throwables.ByteFunction<String, Exception> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, Exception> valueMapper = value -> (int) value * 10;

        Map<String, Integer> result = stream.toMap(keyMapper, valueMapper);
        assertEquals(5, result.size());
        assertEquals(10, result.get("key1"));
        assertEquals(20, result.get("key2"));
        assertEquals(50, result.get("key5"));
    }

    @Test
    public void testToMapWithSupplier() throws Exception {
        Throwables.ByteFunction<String, Exception> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, Exception> valueMapper = value -> (int) value * 10;
        Supplier<TreeMap<String, Integer>> mapFactory = TreeMap::new;

        TreeMap<String, Integer> result = stream.toMap(keyMapper, valueMapper, mapFactory);
        assertEquals(5, result.size());
        assertEquals("key1", result.firstKey());
        assertEquals("key5", result.lastKey());
    }

    @Test
    public void testToMapWithMergeFunction() throws Exception {
        stream = createByteStream(new byte[] { 1, 2, 1, 3, 2 });
        Throwables.ByteFunction<String, Exception> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, Exception> valueMapper = value -> (int) value;
        BinaryOperator<Integer> mergeFunction = (v1, v2) -> v1 + v2;

        Map<String, Integer> result = stream.toMap(keyMapper, valueMapper, mergeFunction);
        assertEquals(3, result.size());
        assertEquals(2, result.get("key1"));
        assertEquals(4, result.get("key2"));
        assertEquals(3, result.get("key3"));
    }

    @Test
    public void testToMapWithMergeFunctionAndSupplier() throws Exception {
        stream = createByteStream(new byte[] { 1, 2, 1, 3, 2 });
        Throwables.ByteFunction<String, Exception> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, Exception> valueMapper = value -> (int) value;
        BinaryOperator<Integer> mergeFunction = (v1, v2) -> v1 + v2;
        Supplier<LinkedHashMap<String, Integer>> mapFactory = LinkedHashMap::new;

        LinkedHashMap<String, Integer> result = stream.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        assertEquals(3, result.size());
        assertEquals(2, result.get("key1"));
    }

    @Test
    public void testGroupTo() throws Exception {
        Throwables.ByteFunction<String, Exception> keyMapper = value -> value % 2 == 0 ? "even" : "odd";
        Collector<Byte, ?, List<Byte>> downstream = Collectors.toList();

        Map<String, List<Byte>> result = stream.groupTo(keyMapper, downstream);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), result.get("odd"));
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result.get("even"));
    }

    @Test
    public void testGroupToWithSupplier() throws Exception {
        Throwables.ByteFunction<String, Exception> keyMapper = value -> value % 2 == 0 ? "even" : "odd";
        Collector<Byte, ?, List<Byte>> downstream = Collectors.toList();
        Supplier<TreeMap<String, List<Byte>>> mapFactory = TreeMap::new;

        TreeMap<String, List<Byte>> result = stream.groupTo(keyMapper, downstream, mapFactory);
        assertEquals(2, result.size());
        assertEquals("even", result.firstKey());
    }

    @Test
    public void testForEachIndexed() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.IntByteConsumer<Exception> action = (index, value) -> result.add(index + ":" + value);

        stream.forEachIndexed(action);
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:4", "4:5"), result);
    }

    @Test
    public void testFirst() {
        OptionalByte result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsByte());

        stream = createByteStream(new byte[0]);
        result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        OptionalByte result = stream.last();
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsByte());

        stream = createByteStream(new byte[0]);
        result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        stream = createByteStream(new byte[] { 42 });
        OptionalByte result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsByte());

        stream = createByteStream(new byte[0]);
        result = stream.onlyOne();
        assertFalse(result.isPresent());

        stream = createByteStream(new byte[] { 1, 2 });
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testFindAny() throws Exception {
        Throwables.BytePredicate<Exception> predicate = value -> value > 3;
        OptionalByte result = stream.findAny(predicate);
        assertTrue(result.isPresent());
        assertTrue(result.getAsByte() > 3);

        stream = createByteStream(new byte[] { 1, 2, 3 });
        result = stream.findAny(value -> value > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Byte>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Byte> percentiles = result.get();
        assertNotNull(percentiles.get(Percentage._50));

        stream = createByteStream(new byte[0]);
        result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summarizeAndPercentiles();

        ByteSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());

        assertTrue(result.right().isPresent());
        assertNotNull(result.right().get().get(Percentage._50));

        stream = createByteStream(new byte[0]);
        result = stream.summarizeAndPercentiles();
        assertEquals(0, result.left().getCount());
        assertFalse(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        String result = stream.join(", ", "[", "]");
        assertEquals("[1, 2, 3, 4, 5]", result);

        stream = createByteStream(new byte[0]);
        result = stream.join(", ", "[", "]");
        assertEquals("[]", result);

        stream = createByteStream(new byte[] { 42 });
        result = stream.join(", ", "[", "]");
        assertEquals("[42]", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = stream.joinTo(joiner);
        assertEquals("[1, 2, 3, 4, 5]", result.toString());
    }

    @Test
    public void testCollectWithSupplierAccumulator() {
        Supplier<List<Byte>> supplier = ArrayList::new;
        ObjByteConsumer<List<Byte>> accumulator = List::add;

        List<Byte> result = stream.collect(supplier, accumulator);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        Supplier<List<Byte>> supplier = ArrayList::new;
        ObjByteConsumer<List<Byte>> accumulator = List::add;
        BiConsumer<List<Byte>, List<Byte>> combiner = List::addAll;

        List<Byte> result = stream.collect(supplier, accumulator, combiner);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testIterator() {
        ByteIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());

        while (iter.hasNext()) {
            iter.nextByte();
        }
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
    }
}
