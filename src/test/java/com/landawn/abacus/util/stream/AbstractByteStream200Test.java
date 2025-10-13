package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;

@Tag("new-test")
public class AbstractByteStream200Test extends TestBase {

    private ByteStream stream;
    private ByteStream stream2;

    @BeforeEach
    public void setUp() {
        stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
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
    public void delay() {
        long startTime = System.currentTimeMillis();
        stream.delay(Duration.ofMillis(100)).count();
        long endTime = System.currentTimeMillis();
        assertTrue(endTime - startTime >= 400);
    }

    @Test
    public void skipUntil() {
        byte[] result = stream.skipUntil(b -> b > 2).toArray();
        assertArrayEquals(new byte[] { 3, 2 }, result);
    }

    @Test
    public void distinct() {
        byte[] result = stream.distinct().toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void flatmap() {
        byte[] result = stream.flatmap(b -> new byte[] { b, b }).toArray();
        assertArrayEquals(new byte[] { 1, 1, 2, 2, 3, 3, 2, 2 }, result);
    }

    @Test
    public void flatmapToObj() {
        List<Byte> result = stream.flatmapToObj(b -> Arrays.asList(b, b)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 2, (byte) 2), result);
    }

    @Test
    public void flattmapToObj() {
        List<Byte> result = stream.flattmapToObj(b -> new Byte[] { b, b }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 2, (byte) 2), result);
    }

    @Test
    public void mapPartial() {
        byte[] result = stream.mapPartial(b -> b > 2 ? OptionalByte.of(b) : OptionalByte.empty()).toArray();
        assertArrayEquals(new byte[] { 3 }, result);
    }

    @Test
    public void rangeMap() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 4, (byte) 5, (byte) 6, (byte) 8)
                .rangeMap((a, b) -> b - a <= 2, (a, b) -> (byte) (a + b))
                .toArray();
        assertArrayEquals(new byte[] { 3, 10, 16 }, result);
    }

    @Test
    public void rangeMapToObj() {
        List<String> result = ByteStream.of((byte) 1, (byte) 2, (byte) 4, (byte) 5, (byte) 6, (byte) 8)
                .rangeMapToObj((a, b) -> b - a <= 2, (a, b) -> a + "-" + b)
                .toList();
        assertEquals(Arrays.asList("1-2", "4-6", "8-8"), result);
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
    public void skip() {
        List<Byte> skipped = new ArrayList<>();
        byte[] result = stream.skip(2, skipped::add).toArray();
        assertArrayEquals(new byte[] { 3, 2 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);
    }

    @Test
    public void filter() {
        List<Byte> dropped = new ArrayList<>();
        byte[] result = stream.filter(b -> b > 2, dropped::add).toArray();
        assertArrayEquals(new byte[] { 3 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 2), dropped);
    }

    @Test
    public void dropWhile() {
        List<Byte> dropped = new ArrayList<>();
        byte[] result = stream.dropWhile(b -> b < 3, dropped::add).toArray();
        assertArrayEquals(new byte[] { 3, 2 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), dropped);
    }

    @Test
    public void step() {
        byte[] result = stream.step(2).toArray();
        assertArrayEquals(new byte[] { 1, 3 }, result);
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
    public void intersection() {
        byte[] result = stream.intersection(Arrays.asList((byte) 2, (byte) 3, (byte) 5)).toArray();
        assertArrayEquals(new byte[] { 2, 3 }, result);
    }

    @Test
    public void difference() {
        byte[] result = stream.difference(Arrays.asList((byte) 2, (byte) 3, (byte) 5)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void symmetricDifference() {
        byte[] result = stream.symmetricDifference(Arrays.asList((byte) 2, (byte) 3, (byte) 5)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 5 }, result);
    }

    @Test
    public void reversed() {
        byte[] result = stream.reversed().toArray();
        assertArrayEquals(new byte[] { 2, 3, 2, 1 }, result);
    }

    @Test
    public void rotated() {
        byte[] result = stream.rotated(2).toArray();
        assertArrayEquals(new byte[] { 3, 2, 1, 2 }, result);
    }

    @Test
    public void shuffled() {
        byte[] original = stream.toArray();
        byte[] shuffled = stream2.shuffled(new Random(1)).toArray();
        assertEquals(original.length, shuffled.length);
        assertFalse(Arrays.equals(original, shuffled));
    }

    @Test
    public void sorted() {
        byte[] result = stream.sorted().toArray();
        assertArrayEquals(new byte[] { 1, 2, 2, 3 }, result);
    }

    @Test
    public void reverseSorted() {
        byte[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new byte[] { 3, 2, 2, 1 }, result);
    }

    @Test
    public void cycled() {
        byte[] result = stream.cycled().limit(8).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2, 1, 2, 3, 2 }, result);
    }

    @Test
    public void testCycled() {
        byte[] result = stream.cycled(2).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2, 1, 2, 3, 2 }, result);
    }

    @Test
    public void indexed() {
        String result = stream.indexed().map(i -> i.index() + ":" + i.value()).join(", ");
        assertEquals("0:1, 1:2, 2:3, 3:2", result);
    }

    @Test
    public void boxed() {
        List<Byte> result = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 2), result);
    }

    @Test
    public void prepend() {
        byte[] result = stream.prepend((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 8, 9, 1, 2, 3, 2 }, result);
    }

    @Test
    public void testPrepend() {
        byte[] result = stream.prepend(ByteStream.of((byte) 8, (byte) 9)).toArray();
        assertArrayEquals(new byte[] { 8, 9, 1, 2, 3, 2 }, result);
    }

    @Test
    public void testPrepend1() {
        byte[] result = stream.prepend(OptionalByte.of((byte) 9)).toArray();
        assertArrayEquals(new byte[] { 9, 1, 2, 3, 2 }, result);
        byte[] result2 = stream.prepend(OptionalByte.empty()).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2 }, result2);
    }

    @Test
    public void append() {
        byte[] result = stream.append((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2, 8, 9 }, result);
    }

    @Test
    public void testAppend() {
        byte[] result = stream.append(ByteStream.of((byte) 8, (byte) 9)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2, 8, 9 }, result);
    }

    @Test
    public void testAppend1() {
        byte[] result = stream.append(OptionalByte.of((byte) 9)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2, 9 }, result);
    }

    @Test
    public void testAppend2() {
        byte[] result2 = stream.append(OptionalByte.empty()).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2 }, result2);
    }

    @Test
    public void appendIfEmpty() {
        byte[] result = ByteStream.empty().appendIfEmpty((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 8, 9 }, result);
        byte[] result2 = stream.appendIfEmpty((byte) 8, (byte) 9).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 2 }, result2);
    }

    @Test
    public void mergeWith() {
        ByteStream anotherStream = ByteStream.of((byte) 0, (byte) 4);
        byte[] result = stream.mergeWith(anotherStream, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 2, 4 }, result);
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
    public void toMap() {
        Map<Byte, Byte> result = stream.distinct().toMap(b -> b, b -> (byte) (b * 2));
        Map<Byte, Byte> expected = new HashMap<>();
        expected.put((byte) 1, (byte) 2);
        expected.put((byte) 2, (byte) 4);
        expected.put((byte) 3, (byte) 6);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Map<Byte, Byte> result = stream.toMap(b -> b, b -> (byte) (b * 2), (v1, v2) -> v1);
        Map<Byte, Byte> expected = new HashMap<>();
        expected.put((byte) 1, (byte) 2);
        expected.put((byte) 2, (byte) 4);
        expected.put((byte) 3, (byte) 6);
        assertEquals(expected, result);
    }

    @Test
    public void testToMapWithMapFactory() {
        Map<Byte, Byte> result = stream.distinct().toMap(b -> b, b -> (byte) (b * 2), Suppliers.ofMap());
        Map<Byte, Byte> expected = new HashMap<>();
        expected.put((byte) 1, (byte) 2);
        expected.put((byte) 2, (byte) 4);
        expected.put((byte) 3, (byte) 6);
        assertEquals(expected, result);
    }

    @Test
    public void groupTo() {
        Map<Object, Long> result = stream.groupTo(b -> b % 2, Collectors.counting());
        Map<Object, Long> expected = new HashMap<>();
        expected.put(1, 2L);
        expected.put(0, 2L);
        assertEquals(expected, result);
    }

    @Test
    public void forEachIndexed() {
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((i, b) -> result.add(i + ":" + b));
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:2"), result);
    }

    @Test
    public void first() {
        assertEquals(OptionalByte.of((byte) 1), stream.first());
        assertEquals(OptionalByte.empty(), ByteStream.empty().first());
    }

    @Test
    public void last() {
        assertEquals(OptionalByte.of((byte) 2), stream.last());
        assertEquals(OptionalByte.empty(), ByteStream.empty().last());
    }

    @Test
    public void onlyOne() {
        assertEquals(OptionalByte.of((byte) 1), ByteStream.of((byte) 1).onlyOne());
        assertThrows(RuntimeException.class, () -> stream.onlyOne());
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
    public void percentiles() {
        Map<Percentage, Byte> result = stream.percentiles().get();
        assertEquals(1, result.get(Percentage._0_0001).intValue());
        assertEquals(3, result.get(Percentage._99_9999).intValue());
    }

    @Test
    public void summarizeAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summarizeAndPercentiles();
        ByteSummaryStatistics stats = result.left();
        assertEquals(4, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(3, stats.getMax());
        assertEquals(8, stats.getSum());

        Map<Percentage, Byte> percentiles = result.right().get();
        assertEquals(1, percentiles.get(Percentage._0_0001).intValue());
        assertEquals(3, percentiles.get(Percentage._99_9999).intValue());
    }

    @Test
    public void join() {
        assertEquals("1, 2, 3, 2", stream.join(", "));
        assertEquals("[1, 2, 3, 2]", stream2.join(", ", "[", "]"));
    }

    @Test
    public void joinTo() {
        Joiner joiner = Joiner.with(", ");
        stream.joinTo(joiner);
        assertEquals("1, 2, 3, 2", joiner.toString());
    }

    @Test
    public void collect() {
        List<Byte> result = stream.collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 2), result);
    }

    @Test
    public void iterator() {
        List<Byte> result = new ArrayList<>();
        stream.iterator().forEachRemaining(result::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 2), result);
    }
}
