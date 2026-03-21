package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import com.landawn.abacus.util.u.OptionalDouble;
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

public class AbstractByteStreamTest extends TestBase {

    private final byte[] testData = { 1, 2, 3, 4, 5 };
    private ByteStream stream;
    private ByteStream stream2;
    private ByteStream stream3;
    private ByteStream stream4;
    private ByteStream stream5;
    private ByteStream emptyStream;

    @BeforeEach
    public void setUp() {
        stream = ByteStream.of(testData);
        stream2 = ByteStream.of(testData);
        stream3 = ByteStream.of(testData);
        stream4 = ByteStream.of(testData);
        stream5 = ByteStream.of(testData);
        emptyStream = ByteStream.empty();
    }

    private ByteStream createByteStream(final byte... elements) {
        return ByteStream.of(elements);
    }

    // These tests force execution through AbstractByteStream (not ArrayByteStream) by using
    // a map() which produces an IteratorByteStream that delegates to abstract class implementations.
    private ByteStream createAbstractStream(byte... elements) {
        return ByteStream.of(elements).map(b -> b);
    }

    @Test
    public void rateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        long startTime = System.currentTimeMillis();
        ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rateLimited(rateLimiter).forEach(it -> {
        });
        long endTime = System.currentTimeMillis();
        assertTrue(endTime - startTime >= 400);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(1000);
        ByteStream result = stream.rateLimited(rateLimiter);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testRateLimitedWithNullRateLimiter() {
        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void delay() {
        long startTime = System.currentTimeMillis();
        stream.delay(Duration.ofMillis(100)).count();
        long endTime = System.currentTimeMillis();
        assertTrue(endTime - startTime >= 80);
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        ByteStream result = stream.delay(delay);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testDelayWithNullDuration() {
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void skipUntil() {
        byte[] result = stream.skipUntil(b -> b > 2).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
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
    public void testSkipUntilNoMatch() {
        BytePredicate predicate = value -> value > 10;
        ByteStream result = stream.skipUntil(predicate);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void distinct() {
        byte[] result = stream.distinct().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
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
    public void testDistinct_MultipleElements() {
        stream = ByteStream.of((byte) 1, (byte) 2, (byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = stream.distinct().collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void flatmap() {
        byte[] result = stream.flatmap(b -> new byte[] { b, b }).toArray();
        assertArrayEquals(new byte[] { 1, 1, 2, 2, 3, 3, 4, 4, 5, 5 }, result);
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
    public void flatmapToObj() {
        List<Byte> result = stream.flatmapToObj(b -> Arrays.asList(b, b)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 5), result);
    }

    @Test
    public void testFlatmapToObj() {
        ByteFunction<Collection<String>> mapper = value -> Arrays.asList("" + value, "" + (value * 2));
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6", "4", "8", "5", "10"), result);
    }

    @Test
    public void flatMapArrayToObj() {
        List<Byte> result = stream.flatMapArrayToObj(b -> new Byte[] { b, b }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 5), result);
    }

    @Test
    public void testFlattMapToObj() {
        ByteFunction<String[]> mapper = value -> new String[] { "" + value, "" + (value * 2) };
        List<String> result = stream.flatMapArrayToObj(mapper).toList();
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6", "4", "8", "5", "10"), result);
    }

    @Test
    public void mapPartial() {
        byte[] result = stream.mapPartial(b -> b > 2 ? OptionalByte.of(b) : OptionalByte.empty()).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
    }

    @Test
    public void testMapPartial() {
        ByteFunction<OptionalByte> mapper = value -> value % 2 == 0 ? OptionalByte.of(value) : OptionalByte.empty();
        byte[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new byte[] { 2, 4 }, result);
    }

    @Test
    public void rangeMap() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 4, (byte) 5, (byte) 6, (byte) 8)
                .rangeMap((a, b) -> b - a <= 2, (a, b) -> (byte) (a + b))
                .toArray();
        assertArrayEquals(new byte[] { 3, 10, 16 }, result);
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
    public void testRangeMapEmptyStream() {
        stream = ByteStream.empty();
        ByteBiPredicate sameRange = (a, b) -> true;
        ByteBinaryOperator mapper = (a, b) -> a;
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testRangeMapSingleElement() {
        stream = createByteStream((byte) 42);
        ByteBiPredicate sameRange = (a, b) -> true;
        ByteBinaryOperator mapper = (a, b) -> (byte) (a + b);
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(new byte[] { 84 }, result.toArray());
    }

    @Test
    public void testRangeMapNoMatches() {
        ByteBiPredicate sameRange = (a, b) -> false;
        ByteBinaryOperator mapper = (a, b) -> a;
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void rangeMapToObj() {
        List<String> result = ByteStream.of((byte) 1, (byte) 2, (byte) 4, (byte) 5, (byte) 6, (byte) 8)
                .rangeMapToObj((a, b) -> b - a <= 2, (a, b) -> a + "-" + b)
                .toList();
        assertEquals(Arrays.asList("1-2", "4-6", "8-8"), result);
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
    public void collapse() {
        List<ByteList> result = ByteStream.of((byte) 1, (byte) 2, (byte) 4, (byte) 3, (byte) 2).collapse((a, b) -> b < a).toList();
        assertEquals(3, result.size());
        assertEquals(ByteList.of((byte) 4, (byte) 3, (byte) 2), result.get(2));
    }

    @Test
    public void testCollapse() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 4, (byte) 3, (byte) 2).collapse((a, b) -> b < a, (a, b) -> (byte) (a + b)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 9 }, result);
    }

    @Test
    public void testCollapse1() {
        byte[] result = ByteStream.of((byte) 4, (byte) 3, (byte) 2, (byte) 5, (byte) 4)
                .collapse((first, prev, curr) -> curr < first, (a, b) -> (byte) (a + b))
                .toArray();
        assertArrayEquals(new byte[] { 9, 9 }, result);
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
    public void testCollapseNonConsecutive() {
        byte[] data = { 1, 3, 5, 7, 9 };
        stream = createByteStream(data);
        ByteBiPredicate collapsible = (a, b) -> b - a == 2;
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(data, lists.get(0).toArray());
    }

    @Test
    public void testCollapseWithTriPredicateComplex() {
        byte[] data = { 1, 2, 4, 8, 16, 17, 32 };
        stream = createByteStream(data);
        ByteTriPredicate collapsible = (first, prev, curr) -> curr == prev * 2 || curr == prev + 1;
        ByteBinaryOperator mergeFunction = (a, b) -> b;
        ByteStream result = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 17, 32 }, result.toArray());
    }

    @Test
    public void testCollapseAlternating() {
        byte[] data = { 1, 2, 1, 2, 1, 2 };
        stream = createByteStream(data);
        AtomicInteger counter = new AtomicInteger(0);
        ByteBiPredicate collapsible = (a, b) -> {
            counter.incrementAndGet();
            return b != a;
        };
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(new byte[] { 1, 2, 1, 2, 1, 2 }, lists.get(0).toArray());
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
    public void testCollapseEmptyStream() {
        stream = ByteStream.empty();
        ByteBiPredicate collapsible = (a, b) -> true;
        Stream<ByteList> result = stream.collapse(collapsible);
        assertEquals(0, result.count());
    }

    @Test
    public void testCollapseSingleElement() {
        stream = createByteStream((byte) 42);
        ByteBiPredicate collapsible = (a, b) -> true;
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(new byte[] { 42 }, lists.get(0).toArray());
    }

    @Test
    public void testCollapseWithBinaryOperatorGroups() {
        byte[] data = { 1, 2, 3, 10, 11, 20 };
        stream = createByteStream(data);
        ByteBiPredicate collapsible = (a, b) -> Math.abs(b - a) <= 1;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) Math.max(a, b);
        ByteStream result = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 3, 11, 20 }, result.toArray());
    }

    @Test
    public void skip() {
        List<Byte> skipped = new ArrayList<>();
        byte[] result = stream.skip(2, skipped::add).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);
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
    public void testSkipWithActionMoreThanSize() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = skipped::add;
        ByteStream result = stream.skip(10, action);
        assertArrayEquals(new byte[] {}, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), skipped);
    }

    @Test
    public void testSkipWithActionParallel() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = value -> {
            synchronized (skipped) {
                skipped.add(value);
            }
        };
        byte[] resultArray = stream.parallel().skip(3, action).sorted().toArray();

        assertEquals(2, resultArray.length);
        assertEquals(3, skipped.size());
    }

    @Test
    public void testSkipWithActionZeroElements() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = skipped::add;
        ByteStream result = stream.skip(0, action);
        assertArrayEquals(testData, result.toArray());
        assertEquals(0, skipped.size());
    }

    @Test
    public void testSkipWithActionNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> stream.skip(-1, value -> {
        }));
    }

    @Test
    public void testSkipWithActionNullAction() {
        assertThrows(IllegalArgumentException.class, () -> stream.skip(2, null));
    }

    @Test
    public void filter() {
        List<Byte> dropped = new ArrayList<>();
        byte[] result = stream.filter(b -> b > 2, dropped::add).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), dropped);
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
    public void testChainedOperations() {
        ByteStream result = stream.filter(v -> v % 2 == 1).map(v -> (byte) (v * 2)).sorted().reversed();
        assertArrayEquals(new byte[] { 10, 6, 2 }, result.toArray());
    }

    @Test
    public void testComplexPipeline() {
        byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        stream = createByteStream(data);

        ByteStream result = stream.filter(v -> v % 2 == 0).step(2).scan((a, b) -> (byte) (a + b)).distinct();

        assertArrayEquals(new byte[] { 2, 8, 18 }, result.toArray());
    }

    @Test
    public void dropWhile() {
        List<Byte> dropped = new ArrayList<>();
        byte[] result = stream.dropWhile(b -> b < 3, dropped::add).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), dropped);
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
    public void step() {
        byte[] result = stream.step(2).toArray();
        assertArrayEquals(new byte[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStepGreaterThanSize() {
        ByteStream result = stream.step(10);
        assertArrayEquals(new byte[] { 1 }, result.toArray());
    }

    @Test
    public void testStepExactSize() {
        ByteStream result = stream.step(5);
        assertArrayEquals(new byte[] { 1 }, result.toArray());
    }

    @Test
    public void testStepWithLargeStep() {
        ByteStream result = stream.step(3);
        assertArrayEquals(new byte[] { 1, 4 }, result.toArray());
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
    public void testStepWithInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
    }

    @Test
    public void scan() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3).scan((a, b) -> (byte) (a + b)).toArray();
        assertArrayEquals(new byte[] { 1, 3, 6 }, result);
    }

    @Test
    public void testScan() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3).scan((byte) 10, (a, b) -> (byte) (a + b)).toArray();
        assertArrayEquals(new byte[] { 11, 13, 16 }, result);
    }

    @Test
    public void testScan1() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3).scan((byte) 10, true, (a, b) -> (byte) (a + b)).toArray();
        assertArrayEquals(new byte[] { 10, 11, 13, 16 }, result);
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
    public void testScanWithInitNotIncluded() {
        byte init = 100;
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a - b);
        ByteStream result = stream.scan(init, false, accumulator);
        assertArrayEquals(new byte[] { 99, 97, 94, 90, 85 }, result.toArray());
    }

    @Test
    public void testScanEmptyStream() {
        stream = ByteStream.empty();
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        ByteStream result = stream.scan(accumulator);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testScanSingleElement() {
        stream = createByteStream((byte) 42);
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a * b);
        ByteStream result = stream.scan(accumulator);
        assertArrayEquals(new byte[] { 42 }, result.toArray());
    }

    @Test
    public void intersection() {
        byte[] result = stream.intersection(Arrays.asList((byte) 2, (byte) 3, (byte) 5)).toArray();
        assertArrayEquals(new byte[] { 2, 3, 5 }, result);
    }

    @Test
    public void testIntersectionWithDuplicates() {
        byte[] data = { 1, 2, 2, 3, 3, 3 };
        stream = createByteStream(data);
        List<Byte> c = Arrays.asList((byte) 2, (byte) 2, (byte) 3);
        ByteStream result = stream.intersection(c);
        assertArrayEquals(new byte[] { 2, 2, 3 }, result.toArray());
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
    public void testIntersectionEmptyCollection() {
        ByteStream result = stream.intersection(new ArrayList<>());
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void difference() {
        byte[] result = stream.difference(Arrays.asList((byte) 2, (byte) 3, (byte) 5)).toArray();
        assertArrayEquals(new byte[] { 1, 4 }, result);
    }

    @Test
    public void testDifferenceWithDuplicates() {
        byte[] data = { 1, 2, 2, 3, 3, 3 };
        stream = createByteStream(data);
        List<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 3);
        ByteStream result = stream.difference(c);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result.toArray());
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
    public void testDifferenceEmptyCollection() {
        ByteStream result = stream.difference(new ArrayList<>());
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void symmetricDifference() {
        byte[] result = stream.symmetricDifference(Arrays.asList((byte) 2, (byte) 3, (byte) 5)).toArray();
        assertArrayEquals(new byte[] { 1, 4 }, result);
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 6, (byte) 7);
        byte[] result = stream.symmetricDifference(c).toArray();
        assertArrayEquals(new byte[] { 1, 4, 5, 6, 7 }, result);
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        stream = ByteStream.empty();
        List<Byte> c = Arrays.asList((byte) 1, (byte) 2);
        ByteStream result = stream.symmetricDifference(c);
        assertArrayEquals(new byte[] { 1, 2 }, result.toArray());
    }

    @Test
    public void reversed() {
        byte[] result = stream.reversed().toArray();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result);
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
    public void testReversedParallel() {
        ByteStream result = stream.parallel().reversed();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result.toArray());
    }

    @Test
    public void rotated() {
        byte[] result = stream.rotated(2).toArray();
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, result);
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
    public void testRotatedFullCycle() {
        ByteStream result = stream.rotated(5);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testRotatedZero() {
        ByteStream result = stream.rotated(0);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testRotatedMultipleCycles() {
        ByteStream result = stream.rotated(12);
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testRotatedEmptyStream() {
        stream = ByteStream.empty();
        ByteStream result = stream.rotated(5);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testRotatedNegative() {
        ByteStream result = stream.rotated(-2);
        assertArrayEquals(new byte[] { 3, 4, 5, 1, 2 }, result.toArray());
    }

    @Test
    public void shuffled() {
        byte[] original = stream.toArray();
        byte[] shuffled = stream2.shuffled(new Random(1)).toArray();
        assertEquals(original.length, shuffled.length);
        assertFalse(Arrays.equals(original, shuffled));
    }

    @Test
    public void testShuffledEmptyStream() {
        stream = ByteStream.empty();
        ByteStream result = stream.shuffled(new Random());
        assertArrayEquals(new byte[] {}, result.toArray());
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
    public void sorted() {
        byte[] result = stream.sorted().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
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
    public void testSortedAlreadySorted() {
        ByteStream sorted1 = stream.sorted();
        ByteStream sorted2 = sorted1.sorted();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, sorted2.toArray());
    }

    @Test
    public void testSortedParallel() {
        byte[] unsorted = { 5, 1, 4, 2, 3 };
        stream = createByteStream(unsorted);
        ByteStream result = stream.parallel().sorted();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void reverseSorted() {
        byte[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testReverseSorted() {
        stream = createByteStream(new byte[] { 5, 3, 1, 4, 2 });
        byte[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void cycled() {
        byte[] result = stream.cycled().limit(8).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 1, 2, 3 }, result);
    }

    @Test
    public void testCycled() {
        byte[] result = stream.cycled(2).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testCycledOneRound() {
        ByteStream result = stream.cycled(1);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testCycled_long() {
        stream = ByteStream.of((byte) 1, (byte) 2);
        List<Byte> result = stream.cycled(3).collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 1, (byte) 2, (byte) 1, (byte) 2), result);
    }

    @Test
    public void testAbstractCycled_WithRounds() {
        byte[] result = createAbstractStream((byte) 1, (byte) 2).cycled(2).toArray();
        assertArrayEquals(new byte[] { 1, 2, 1, 2 }, result);
    }

    @Test
    public void testAbstractCycled_OneRound() {
        byte[] result = createAbstractStream((byte) 1, (byte) 2, (byte) 3).cycled(1).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testCycledZeroRounds() {
        ByteStream result = stream.cycled(0);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testCycledEmptyStream() {
        stream = ByteStream.empty();
        ByteStream result = stream.cycled().limit(5);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testAbstractCycled_ZeroRounds() {
        byte[] result = createAbstractStream((byte) 1, (byte) 2).cycled(0).toArray();
        assertEquals(0, result.length);
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
    public void testCycledNegativeRounds() {
        assertThrows(IllegalArgumentException.class, () -> stream.cycled(-1));
    }

    @Test
    public void testAbstractCycled_NegativeRounds() {
        assertThrows(IllegalArgumentException.class, () -> createAbstractStream((byte) 1).cycled(-1));
    }

    @Test
    public void indexed() {
        String result = stream.indexed().map(i -> i.index() + ":" + i.value()).join(", ");
        assertEquals("0:1, 1:2, 2:3, 3:4, 4:5", result);
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
    public void testIndexedEmptyStream() {
        stream = ByteStream.empty();
        Stream<IndexedByte> result = stream.indexed();
        assertEquals(0, result.count());
    }

    @Test
    public void testIndexedSingleElement() {
        stream = createByteStream((byte) 42);
        Stream<IndexedByte> result = stream.indexed();
        List<IndexedByte> indexed = result.toList();
        assertEquals(1, indexed.size());
        assertEquals(0, indexed.get(0).index());
        assertEquals(42, indexed.get(0).value());
    }

    @Test
    public void boxed() {
        List<Byte> result = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testBoxed() {
        List<Byte> result = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testPrependAppend_Stream_Parallel() {
        byte[] result = createByteStream((byte) 3, (byte) 4).parallel().prepend(ByteStream.of((byte) 1, (byte) 2)).append(ByteStream.of((byte) 5)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void prepend() {
        byte[] result = stream.prepend((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 8, 9, 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testPrepend() {
        byte[] result = stream.prepend(ByteStream.of((byte) 8, (byte) 9)).toArray();
        assertArrayEquals(new byte[] { 8, 9, 1, 2, 3, 4, 5 }, result);
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
    public void testPrepend1() {
        byte[] result = stream.prepend(OptionalByte.of((byte) 9)).toArray();
        assertArrayEquals(new byte[] { 9, 1, 2, 3, 4, 5 }, result);
        byte[] result2 = stream.prepend(OptionalByte.empty()).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result2);
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
    public void testPrependEmptyOptional() {
        OptionalByte op = OptionalByte.empty();
        ByteStream result = stream.prepend(op);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void append() {
        byte[] result = stream.append((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 8, 9 }, result);
    }

    @Test
    public void testAppend() {
        byte[] result = stream.append(ByteStream.of((byte) 8, (byte) 9)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 8, 9 }, result);
    }

    @Test
    public void testAppend1() {
        byte[] result = stream.append(OptionalByte.of((byte) 9)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 9 }, result);
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
    public void testAppend2() {
        byte[] result2 = stream.append(OptionalByte.empty()).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result2);
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
    public void appendIfEmpty() {
        byte[] result = ByteStream.empty().appendIfEmpty((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 8, 9 }, result);
        byte[] result2 = stream.appendIfEmpty((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result2);
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
    public void testAppendIfEmptyNotEmpty() {
        ByteStream result = stream.appendIfEmpty((byte) 6, (byte) 7);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void mergeWith() {
        ByteStream anotherStream = ByteStream.of((byte) 0, (byte) 4);
        byte[] result = stream.mergeWith(anotherStream, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 4, 5 }, result);
    }

    @Test
    public void testMergeWith() {
        ByteStream b = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        ByteBiFunction<MergeResult> nextSelector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        byte[] result = stream.mergeWith(b, nextSelector).toArray();
        assertArrayEquals(new byte[] { 1, 2, 2, 3, 4, 4, 5, 6 }, result);
    }

    @Test
    public void zipWith() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2).zipWith(ByteStream.of((byte) 5, (byte) 6), (a, b) -> (byte) (a + b)).toArray();
        assertArrayEquals(new byte[] { 6, 8 }, result);
    }

    @Test
    public void testZipWith() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2)
                .zipWith(ByteStream.of((byte) 5, (byte) 6), ByteStream.of((byte) 10, (byte) 11), (a, b, c) -> (byte) (a + b + c))
                .toArray();
        assertArrayEquals(new byte[] { 16, 19 }, result);
    }

    @Test
    public void testZipWith1() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2).zipWith(ByteStream.of((byte) 5), (byte) 99, (byte) 98, (a, b) -> (byte) (a + b)).toArray();
        assertArrayEquals(new byte[] { 6, 100 }, result);
    }

    @Test
    public void testZipWith2() {
        byte[] result = ByteStream.of((byte) 1)
                .zipWith(ByteStream.of((byte) 5, (byte) 6), ByteStream.of((byte) 10, (byte) 11, (byte) 12), (byte) 99, (byte) 98, (byte) 97,
                        (a, b, c) -> (byte) (a + b + c))
                .toArray();
        assertArrayEquals(new byte[] { 16, 116, (byte) (99 + 98 + 12) }, result);
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
    public void toMap() {
        Map<Byte, Byte> result = stream.distinct().toMap(b -> b, b -> (byte) (b * 2));
        Map<Byte, Byte> expected = new HashMap<>();
        expected.put((byte) 1, (byte) 2);
        expected.put((byte) 2, (byte) 4);
        expected.put((byte) 3, (byte) 6);
        expected.put((byte) 4, (byte) 8);
        expected.put((byte) 5, (byte) 10);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Map<Byte, Byte> result = stream.toMap(b -> b, b -> (byte) (b * 2), (v1, v2) -> v1);
        Map<Byte, Byte> expected = new HashMap<>();
        expected.put((byte) 1, (byte) 2);
        expected.put((byte) 2, (byte) 4);
        expected.put((byte) 3, (byte) 6);
        expected.put((byte) 4, (byte) 8);
        expected.put((byte) 5, (byte) 10);
        assertEquals(expected, result);
    }

    @Test
    public void testToMapWithMapFactory() {
        Map<Byte, Byte> result = stream.distinct().toMap(b -> b, b -> (byte) (b * 2), Suppliers.ofMap());
        Map<Byte, Byte> expected = new HashMap<>();
        expected.put((byte) 1, (byte) 2);
        expected.put((byte) 2, (byte) 4);
        expected.put((byte) 3, (byte) 6);
        expected.put((byte) 4, (byte) 8);
        expected.put((byte) 5, (byte) 10);
        assertEquals(expected, result);
    }

    @Test
    public void testToMapWithSupplier() throws Exception {
        Throwables.ByteFunction<String, Exception> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, Exception> valueMapper = value -> value * 10;
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
    public void testToMapWithFactory() {
        Throwables.ByteFunction<String, RuntimeException> keyMapper = value -> "key" + value;
        Throwables.ByteFunction<Integer, RuntimeException> valueMapper = value -> (int) value;
        Map<String, Integer> result = stream.toMap(keyMapper, valueMapper, Suppliers.ofMap());

        assertEquals(5, result.size());
        assertTrue(result instanceof HashMap);
    }

    @Test
    public void groupTo() {
        Map<Object, Long> result = stream.groupTo(b -> b % 2, Collectors.counting());
        Map<Object, Long> expected = new HashMap<>();
        expected.put(1, 3L);
        expected.put(0, 2L);
        assertEquals(expected, result);
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
    public void forEachIndexed() {
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((i, b) -> result.add(i + ":" + b));
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:4", "4:5"), result);
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
    public void first() {
        assertEquals(OptionalByte.of((byte) 1), stream.first());
        assertEquals(OptionalByte.empty(), ByteStream.empty().first());
    }

    @Test
    public void testFirstEmpty() {
        stream = ByteStream.empty();
        OptionalByte result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_MultipleElements() {
        stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.first();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testAbstractFirst_NonEmpty() {
        OptionalByte result = createAbstractStream((byte) 1, (byte) 2, (byte) 3).first();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testAbstractFirst_Empty() {
        OptionalByte result = createAbstractStream().first();
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
    public void last() {
        assertEquals(OptionalByte.of((byte) 5), stream.last());
        assertEquals(OptionalByte.empty(), ByteStream.empty().last());
    }

    @Test
    public void testLastEmpty() {
        stream = ByteStream.empty();
        OptionalByte result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_MultipleElements() {
        stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.last();
        assertTrue(result.isPresent());
        assertEquals((byte) 3, result.get());
    }

    @Test
    public void testAbstractLast_NonEmpty() {
        OptionalByte result = createAbstractStream((byte) 1, (byte) 2, (byte) 5).last();
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());
    }

    @Test
    public void testAbstractLast_Empty() {
        OptionalByte result = createAbstractStream().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOneEmpty() {
        stream = ByteStream.empty();
        OptionalByte result = stream.onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testAbstractOnlyOne_SingleElement() {
        OptionalByte result = createAbstractStream((byte) 42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals((byte) 42, result.get());
    }

    @Test
    public void testAbstractOnlyOne_Empty() {
        OptionalByte result = createAbstractStream().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void onlyOne() {
        assertEquals(OptionalByte.of((byte) 1), ByteStream.of((byte) 1).onlyOne());
        assertThrows(RuntimeException.class, () -> stream.onlyOne());
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
    public void testOnlyOne_MoreThanOne() {
        stream = ByteStream.of((byte) 1, (byte) 2);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testAbstractOnlyOne_MultipleElements() {
        assertThrows(RuntimeException.class, () -> createAbstractStream((byte) 1, (byte) 2).onlyOne());
    }

    @Test
    public void findAny() {
        assertTrue(stream.findAny(b -> b > 2).isPresent());
    }

    @Test
    public void findAny2() {
        assertFalse(stream.findAny(b -> b > 5).isPresent());
    }

    @Test
    public void testFindAnyNoMatch() {
        OptionalByte result = stream.findAny(value -> value > 10);
        assertFalse(result.isPresent());
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
    public void percentiles() {
        Map<Percentage, Byte> result = stream.percentiles().get();
        assertEquals(1, result.get(Percentage._0_0001).intValue());
        assertEquals(5, result.get(Percentage._99_9999).intValue());
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
    public void testPercentilesEmpty() {
        stream = ByteStream.empty();
        Optional<Map<Percentage, Byte>> result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void summaryStatisticsAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();
        ByteSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());

        Map<Percentage, Byte> percentiles = result.right().get();
        assertEquals(1, percentiles.get(Percentage._0_0001).intValue());
        assertEquals(5, percentiles.get(Percentage._99_9999).intValue());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();

        ByteSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());

        assertTrue(result.right().isPresent());
        assertNotNull(result.right().get().get(Percentage._50));

        stream = createByteStream(new byte[0]);
        result = stream.summaryStatisticsAndPercentiles();
        assertEquals(0, result.left().getCount());
        assertFalse(result.right().isPresent());
    }

    @Test
    public void join() {
        assertEquals("1, 2, 3, 4, 5", stream.join(", "));
        assertEquals("[1, 2, 3, 4, 5]", stream2.join(", ", "[", "]"));
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
    public void joinTo() {
        Joiner joiner = Joiner.with(", ");
        stream.joinTo(joiner);
        assertEquals("1, 2, 3, 4, 5", joiner.toString());
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = stream.joinTo(joiner);
        assertEquals("[1, 2, 3, 4, 5]", result.toString());
    }

    @Test
    public void collect() {
        List<Byte> result = stream.collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
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
    public void testCollectWithSupplierAndAccumulator() {
        List<Byte> result = stream.collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void collectWithCombiner() {
        Supplier<List<Byte>> supplier = ArrayList::new;
        ObjByteConsumer<List<Byte>> accumulator = List::add;
        BiConsumer<List<Byte>, List<Byte>> combiner = List::addAll;

        List<Byte> result = stream.collect(supplier, accumulator, combiner);
        assertEquals(5, result.size());
        assertTrue(result.containsAll(List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)));

        List<Byte> emptyResult = emptyStream.collect(supplier, accumulator, combiner);
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testCollectWithNullSupplier() {
        try {
            stream.collect(null, (list, value) -> {
            });
            fail("Should throw exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testCollectWithNullAccumulator() {
        try {
            stream.collect(ArrayList::new, null);
            fail("Should throw exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void iterator() {
        List<Byte> result = new ArrayList<>();
        stream.iterator().forEachRemaining(result::add);
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

    @Test
    public void reduceWithIdentity() {
        byte result = stream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 25, result);

        byte emptyResult = emptyStream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 10, emptyResult);
    }

    @Test
    public void reduce() {
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals((byte) 15, result.get());

        OptionalByte emptyResult = emptyStream.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void anyMatch() {
        assertTrue(stream.anyMatch(b -> b == 3));
        assertFalse(stream2.anyMatch(b -> b == 10));
        assertFalse(emptyStream.anyMatch(b -> b == 1));
    }

    @Test
    public void allMatch() {
        assertTrue(stream.allMatch(b -> b > 0));
        assertFalse(stream2.allMatch(b -> b > 3));
        assertTrue(emptyStream.allMatch(b -> b > 0));
    }

    @Test
    public void noneMatch() {
        assertTrue(stream.noneMatch(b -> b > 10));
        assertFalse(stream2.noneMatch(b -> b == 3));
        assertTrue(emptyStream.noneMatch(b -> b == 1));
    }

    @Test
    public void findFirstWithPredicate() {
        OptionalByte result = stream.findFirst(b -> b > 3);
        assertTrue(result.isPresent());
        assertEquals((byte) 4, result.get());

        OptionalByte notFound = stream2.findFirst(b -> b > 10);
        assertFalse(notFound.isPresent());

        OptionalByte emptyResult = emptyStream.findFirst(b -> b > 0);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void findLast() {
        OptionalByte result = stream.findLast(b -> b < 4);
        assertTrue(result.isPresent());
        assertEquals((byte) 3, result.get());

        OptionalByte notFound = stream2.findLast(b -> b < 1);
        assertFalse(notFound.isPresent());

        OptionalByte emptyResult = emptyStream.findLast(b -> b > 0);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void min() {
        OptionalByte result = stream.min();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        OptionalByte emptyResult = emptyStream.min();
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void max() {
        OptionalByte result = stream.max();
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());

        OptionalByte emptyResult = emptyStream.max();
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void kthLargest() {
        OptionalByte secondLargest = stream.kthLargest(2);
        assertTrue(secondLargest.isPresent());
        assertEquals((byte) 4, secondLargest.get());

        OptionalByte largest = stream2.kthLargest(1);
        assertTrue(largest.isPresent());
        assertEquals((byte) 5, largest.get());

        OptionalByte fifthLargest = stream3.kthLargest(5);
        assertTrue(fifthLargest.isPresent());
        assertEquals((byte) 1, fifthLargest.get());

        OptionalByte outOfBounds = stream4.kthLargest(6);
        assertFalse(outOfBounds.isPresent());

        assertThrows(IllegalArgumentException.class, () -> stream5.kthLargest(0));

        OptionalByte emptyResult = emptyStream.kthLargest(1);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void sum() {
        assertEquals(15, stream.sum());
        assertEquals(0, emptyStream.sum());
    }

    @Test
    public void average() {
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);

        OptionalDouble emptyResult = emptyStream.average();
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void summaryStatistics() {
        ByteSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);

        ByteSummaryStatistics emptyStats = emptyStream.summaryStatistics();
        assertEquals(0, emptyStats.getCount());
        assertEquals(0, emptyStats.getSum());
        assertEquals(Byte.MAX_VALUE, emptyStats.getMin());
        assertEquals(Byte.MIN_VALUE, emptyStats.getMax());
        assertEquals(0.0, emptyStats.getAverage(), 0.001);
    }

    @Test
    public void toByteList() {
        ByteList result = stream.toByteList();
        assertEquals(5, result.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());

        ByteList emptyResult = emptyStream.toByteList();
        assertTrue(emptyResult.isEmpty());
    }

    // TODO: Remaining AbstractByteStream anonymous ByteIterator coverage belongs to internal iterator implementations that are already exercised indirectly through the public ByteStream API.

}
