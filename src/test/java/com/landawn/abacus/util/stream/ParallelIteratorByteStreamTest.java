package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorByteStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final byte[] TEST_ARRAY = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3 };

    private ByteStream createStream(byte... elements) {
        return ByteStream.of(elements).map(e -> (byte) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
    }

    // ---- Sequential-fallback path: 1-thread iterator stream => canBeSequential(maxThreadNum) == true ----

    private ByteStream createSingleThreadStream(byte... elements) {
        return ByteStream.of(elements).map(e -> (byte) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1));
    }

    @Test
    public void testFilter() {
        ByteStream stream = createStream(TEST_ARRAY);
        byte[] result = stream.filter(b -> b > 5).toArray();
        for (byte b : result) {
            assertTrue(b > 5);
        }

        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 4, 5 });
        byte[] result2 = stream2.filter(b -> b % 2 == 0).toArray();
        assertHaveSameElements(new byte[] { 2, 4 }, result2);

        ByteStream stream3 = createStream(new byte[] {});
        byte[] result3 = stream3.filter(b -> true).toArray();
        assertEquals(0, result3.length);
    }

    @Test
    public void testFilter_SequentialFallback() {
        byte[] result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3, (byte) 4).filter(b -> b % 2 == 0).toArray();
        assertHaveSameElements(new byte[] { 2, 4 }, result);
    }

    @Test
    public void testTakeWhile() {
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result1 = stream1.takeWhile(b -> b < 4).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 3 }, result1);

        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result2 = stream2.takeWhile(b -> b < 4).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 3 }, result2);

        ByteStream stream3 = createStream(TEST_ARRAY);
        byte[] result3 = stream3.takeWhile(b -> true).toArray();
        assertHaveSameElements(N.toList(TEST_ARRAY), N.toList(result3));

        ByteStream stream4 = createStream(TEST_ARRAY);
        byte[] result4 = stream4.takeWhile(b -> false).toArray();
        assertArrayEquals(new byte[] {}, result4);
        stream4.close();
    }

    @Test
    public void testTakeWhile_SequentialFallback() {
        byte[] result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3, (byte) 4).takeWhile(b -> b < 3).toArray();
        assertHaveSameElements(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testDropWhile() {
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result1 = stream1.dropWhile(b -> b < 4).toArray();
        assertHaveSameElements(new byte[] { 4, 5, 6 }, result1);

        ByteStream stream2 = createStream(new byte[] { 1, 2, 3 });
        byte[] result2 = stream2.dropWhile(b -> true).toArray();
        assertEquals(0, result2.length);

        ByteStream stream3 = createStream(new byte[] { 1, 2, 3 });
        byte[] result3 = stream3.dropWhile(b -> false).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 3 }, result3);
    }

    @Test
    public void testDropWhile_SequentialFallback() {
        byte[] result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3, (byte) 4).dropWhile(b -> b < 3).toArray();
        assertHaveSameElements(new byte[] { 3, 4 }, result);
    }

    @Test
    public void testMap() {
        ByteStream stream1 = createStream(TEST_ARRAY);
        byte[] result1 = stream1.map(b -> (byte) (b * 2)).toArray();
        assertHaveSameElements(new byte[] { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 2, 4, 6 }, result1);

        ByteStream stream2 = createStream(TEST_ARRAY);
        byte[] result2 = stream2.map(b -> (byte) (b + 1)).toArray();
        assertHaveSameElements(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 2, 3, 4 }, result2);

        ByteStream stream3 = createStream(new byte[] {});
        byte[] result3 = stream3.map(b -> (byte) (b + 1)).toArray();
        assertHaveSameElements(new byte[] {}, result3);
    }

    @Test
    public void testMap_SequentialFallback() {
        byte[] result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).map(b -> (byte) (b * 2)).toArray();
        assertHaveSameElements(new byte[] { 2, 4, 6 }, result);
    }

    @Test
    public void testMapToInt() {
        ByteStream stream = createStream(new byte[] { 1, 2, 3 });
        List<Integer> result = stream.mapToInt(b -> b * 10).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);

        ByteStream stream2 = createStream(new byte[] {});
        List<Integer> result2 = stream2.mapToInt(b -> b * 10).toList();
        assertEquals(0, result2.size());
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).mapToInt(b -> b * 10).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
    }

    @Test
    public void testMapToObj() {
        ByteStream stream1 = createStream(TEST_ARRAY);
        List<String> result1 = stream1.mapToObj(b -> "Byte_" + b).toList();
        assertHaveSameElements(Arrays.asList("Byte_1", "Byte_2", "Byte_3", "Byte_4", "Byte_5", "Byte_6", "Byte_7", "Byte_8", "Byte_9", "Byte_10", "Byte_1",
                "Byte_2", "Byte_3"), result1);

        ByteStream stream2 = createStream(TEST_ARRAY);
        List<Integer> result2 = stream2.mapToObj(b -> (int) b).toList();
        assertHaveSameElements(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3), result2);
    }

    @Test
    public void testMapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream((byte) 1, (byte) 2).mapToObj(b -> "v" + b).toList();
        assertHaveSameElements(Arrays.asList("v1", "v2"), result);
    }

    @Test
    public void testFlatMap() {
        ByteStream stream = createStream(new byte[] { 1, 2, 3 });
        byte[] result = stream.flatMap(b -> ByteStream.of(b, (byte) (b + 10))).toArray();
        assertHaveSameElements(new byte[] { 1, 11, 2, 12, 3, 13 }, result);
    }

    @Test
    public void testFlatmapByteArray() {
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3 });
        byte[] result1 = stream1.flatmap(b -> new byte[] { b, (byte) (b + 10) }).toArray();
        assertHaveSameElements(new byte[] { 1, 11, 2, 12, 3, 13 }, result1);

        ByteStream stream2 = createStream(new byte[] { 1, 2, 3 });
        byte[] result2 = stream2.flatmap(b -> new byte[] { b, (byte) (b * 2) }).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 2, 4, 3, 6 }, result2);
    }

    @Test
    public void testFlatMap_SequentialFallback() {
        byte[] result = createSingleThreadStream((byte) 1, (byte) 2).flatMap(b -> ByteStream.of(b, (byte) (b + 10))).toArray();
        assertHaveSameElements(new byte[] { 1, 11, 2, 12 }, result);
    }

    @Test
    public void testFlatmap_SequentialFallback() {
        byte[] result = createSingleThreadStream((byte) 1, (byte) 2).flatmap(b -> new byte[] { b, (byte) (b + 10) }).toArray();
        assertHaveSameElements(new byte[] { 1, 11, 2, 12 }, result);
    }

    @Test
    public void testFlatMapToInt() {
        ByteStream stream = createStream(new byte[] { 1, 2, 3 });
        List<Integer> result = stream.flatMapToInt(b -> IntStream.of(b, b * 10)).toList();
        assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createSingleThreadStream((byte) 1, (byte) 2).flatMapToInt(b -> IntStream.of(b, b * 10)).toList();
        assertHaveSameElements(Arrays.asList(1, 10, 2, 20), result);
    }

    @Test
    public void testFlatMapToObj_largeArray() {
        List<String> result = createStream(TEST_ARRAY).flatMapToObj(b -> Stream.of(new String[] { "v" + b })).toList();
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testFlatmapToObj_largeArray() {
        List<String> result = createStream(TEST_ARRAY).flatmapToObj(b -> Arrays.asList("a" + b, "b" + b)).toList();
        assertEquals(TEST_ARRAY.length * 2, result.size());
    }

    @Test
    public void testFlatMapToObj() {
        ByteStream stream1 = createStream(new byte[] { 1, 2 });
        List<String> result1 = stream1.flatMapToObj(b -> Stream.of("A" + b, "B" + b)).toList();
        assertHaveSameElements(Arrays.asList("A1", "B1", "A2", "B2"), result1);

        ByteStream stream2 = createStream(new byte[] { 1, 2 });
        List<Integer> result2 = stream2.flatMapToObj(b -> Stream.of((int) b, b * 10)).toList();
        assertHaveSameElements(Arrays.asList(1, 10, 2, 20), result2);
    }

    @Test
    public void testFlatmapToObj() {
        ByteStream stream = createStream(new byte[] { 1, 2 });
        List<String> result = stream.flatmapToObj(b -> Arrays.asList("X" + b, "Y" + b)).toList();
        assertHaveSameElements(Arrays.asList("X1", "Y1", "X2", "Y2"), result);
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream((byte) 1, (byte) 2).flatMapToObj(b -> Stream.of("A" + b, "B" + b)).toList();
        assertHaveSameElements(Arrays.asList("A1", "B1", "A2", "B2"), result);
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream((byte) 1, (byte) 2).flatmapToObj(b -> Arrays.asList("X" + b, "Y" + b)).toList();
        assertHaveSameElements(Arrays.asList("X1", "Y1", "X2", "Y2"), result);
    }

    @Test
    public void testOnEach() {
        List<Byte> consumedElements = new CopyOnWriteArrayList<>();

        ByteStream stream1 = createStream(TEST_ARRAY);
        stream1.peek(consumedElements::add).count();
        assertEquals(TEST_ARRAY.length, consumedElements.size());
        assertTrue(consumedElements.containsAll(N.toList(TEST_ARRAY)));

        consumedElements.clear();

        ByteStream stream2 = createStream(TEST_ARRAY);
        stream2.peek(consumedElements::add).count();
        assertEquals(TEST_ARRAY.length, consumedElements.size());
        assertTrue(consumedElements.containsAll(N.toList(TEST_ARRAY)));
    }

    @Test
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).peek(b -> count.incrementAndGet()).count();
        assertEquals(3, count.get());
    }

    @Test
    public void testOnEach_SequentialFallback_parallel() {
        // Verify onEach with 1 thread uses sequential path (covers onEach sequential branch)
        List<Byte> peeked = new java.util.concurrent.CopyOnWriteArrayList<>();
        createSingleThreadStream(TEST_ARRAY).peek(peeked::add).forEach(b -> {
        });
        assertEquals(TEST_ARRAY.length, peeked.size());
    }

    @Test
    public void testForEach() {
        List<Byte> consumed = new CopyOnWriteArrayList<>();
        ByteStream stream = createStream(TEST_ARRAY);
        stream.forEach(consumed::add);
        assertEquals(TEST_ARRAY.length, consumed.size());
        assertTrue(consumed.containsAll(N.toList(TEST_ARRAY)));
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).forEach(b -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testToMap() {
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 1, 4 });
        Map<String, Integer> map1 = stream1.toMap(b -> "Key_" + b, b -> (int) b, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        Map<String, Integer> expectedMap1 = new ConcurrentHashMap<>();
        expectedMap1.put("Key_1", 1 + 1);
        expectedMap1.put("Key_2", 2);
        expectedMap1.put("Key_3", 3);
        expectedMap1.put("Key_4", 4);
        assertEquals(expectedMap1, map1);

        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 1, 4 });
        Map<String, Integer> map2 = stream2.toMap(b -> "Key_" + b, b -> (int) b, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(expectedMap1, map2);
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Integer> result = createSingleThreadStream((byte) 1, (byte) 2).toMap(b -> "k" + b, b -> (int) b, (v1, v2) -> v1, ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals(1, (int) result.get("k1"));
    }

    @Test
    public void testGroupTo() {
        ByteStream stream = createStream(new byte[] { 1, 2, 1, 3, 2 });
        Map<String, List<Byte>> result = stream.groupTo(b -> String.valueOf(b), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(2, result.get("1").size());
        assertEquals(2, result.get("2").size());
        assertEquals(1, result.get("3").size());
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Byte>> result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3, (byte) 4).groupTo(b -> b % 2 == 0,
                java.util.stream.Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals(2, result.get(true).size());
        assertEquals(2, result.get(false).size());
    }

    @Test
    public void testReduceWithIdentity() {
        ByteStream stream1 = createStream(TEST_ARRAY);
        byte result1 = stream1.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(N.sum(TEST_ARRAY), result1);

        ByteStream stream2 = createStream(TEST_ARRAY);
        byte result2 = stream2.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(N.sum(TEST_ARRAY), result2);
    }

    @Test
    public void testReduce_parallelWithLargeArray() {
        // sum of 1..10,1..3 = 55+6 = 61 as byte (fits in byte range)
        byte sum = createStream(TEST_ARRAY).reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals((byte) 61, sum);

        OptionalByte opt = createStream(TEST_ARRAY).reduce((a, b) -> (byte) (a + b));
        assertTrue(opt.isPresent());
        assertEquals((byte) 61, opt.get());
    }

    @Test
    public void testReduceWithoutIdentity() {
        ByteStream stream = createStream(new byte[] { 1, 2, 3, 4, 5 });
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals(15, result.get());

        ByteStream emptyStream = createStream(new byte[] {});
        OptionalByte emptyResult = emptyStream.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testReduce_emptyStreamParallel() {
        byte result = createStream(new byte[0]).reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals((byte) 0, result);

        OptionalByte opt = createStream(new byte[0]).reduce((a, b) -> (byte) (a + b));
        assertFalse(opt.isPresent());
    }

    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() throws Exception {
        ByteStream reduceWithIdentityStream = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte reducedWithIdentity = reduceWithIdentityStream.reduce((byte) 0, (left, right) -> (byte) (left + right));
        assertEquals(21, reducedWithIdentity);

        ByteStream reduceStream = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        OptionalByte reduced = reduceStream.reduce((left, right) -> (byte) (left + right));
        assertTrue(reduced.isPresent());
        assertEquals(21, reduced.get());

        ByteStream findFirstStream = createStream(new byte[] { 2, 4, 6, 8 });
        OptionalByte firstMatch = findFirstStream.findFirst(b -> {
            Thread.sleep(b == 2 ? 40L : 5L);
            return b == 2 || b % 4 == 0;
        });
        assertTrue(firstMatch.isPresent());
        assertEquals(2, firstMatch.get());

        ByteStream findAnyStream = createStream(new byte[] { 1, 3, 5, 7, 8 });
        OptionalByte anyMatch = findAnyStream.findAny(b -> b % 2 == 0);
        assertTrue(anyMatch.isPresent());
        assertEquals(8, anyMatch.get());

        ByteStream findLastStream = createStream(new byte[] { 2, 4, 6, 8 });
        OptionalByte lastMatch = findLastStream.findLast(b -> {
            Thread.sleep(b == 8 ? 40L : 5L);
            return b == 2 || b % 4 == 0;
        });
        assertTrue(lastMatch.isPresent());
        assertEquals(8, lastMatch.get());
    }

    @Test
    public void testConstructor_withDefaultValues() {
        ByteStream stream = ByteStream
                .of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13,
                        (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20)
                .parallel();
        byte sum = stream.reduce((byte) 0, (a, b) -> (byte) (a + b));
        // just ensure no exception
        assertTrue(true);
    }

    @Test
    public void testCollect() {
        ByteStream stream1 = createStream(TEST_ARRAY);
        List<Byte> collected1 = stream1.collect(CopyOnWriteArrayList::new, CopyOnWriteArrayList::add, CopyOnWriteArrayList::addAll);
        assertEquals(TEST_ARRAY.length, collected1.size());
        assertTrue(collected1.containsAll(N.toList(TEST_ARRAY)));

        ByteStream stream2 = createStream(TEST_ARRAY);
        List<Byte> collected2 = stream2.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, collected2.size());
        assertTrue(collected2.containsAll(N.toList(TEST_ARRAY)));
    }

    @Test
    public void testCollect_SequentialFallback() {
        List<Byte> result = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(N.toList(new byte[] { 1, 2, 3 }), result);
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).anyMatch(b -> b == 3));
        assertFalse(createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).anyMatch(b -> b > 10));
    }

    @Test
    public void testAnyMatch() throws Exception {
        ByteStream stream1 = createStream(TEST_ARRAY);
        assertTrue(stream1.anyMatch(b -> b == 5));

        ByteStream stream2 = createStream(TEST_ARRAY);
        assertFalse(stream2.anyMatch(b -> b == 50));

        ByteStream stream3 = createStream(new byte[] {});
        assertFalse(stream3.anyMatch(b -> true));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).allMatch(b -> b > 0));
        assertFalse(createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).allMatch(b -> b > 1));
    }

    @Test
    public void testAllMatch() throws Exception {
        ByteStream stream1 = createStream(new byte[] { 2, 4, 6 });
        assertTrue(stream1.allMatch(b -> b % 2 == 0));

        ByteStream stream2 = createStream(TEST_ARRAY);
        assertFalse(stream2.allMatch(b -> b < 5));

        ByteStream stream3 = createStream(new byte[] { 1, 3, 5 });
        assertTrue(stream3.allMatch(b -> b % 2 != 0));

        ByteStream stream4 = createStream(TEST_ARRAY);
        assertFalse(stream4.allMatch(b -> b < 5));
        stream4.close();

        ByteStream stream5 = createStream(new byte[] {});
        assertTrue(stream5.allMatch(b -> true));
        stream5.close();
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).noneMatch(b -> b > 10));
        assertFalse(createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).noneMatch(b -> b == 2));
    }

    @Test
    public void testNoneMatch() throws Exception {
        ByteStream stream1 = createStream(TEST_ARRAY);
        assertTrue(stream1.noneMatch(b -> b > 100));

        ByteStream stream2 = createStream(TEST_ARRAY);
        assertFalse(stream2.noneMatch(b -> b == 5));

        ByteStream stream3 = createStream(new byte[] {});
        assertTrue(stream3.noneMatch(b -> true));
    }

    @Test
    public void testFindFirst_SequentialFallback() {
        OptionalByte found = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).findFirst(b -> b == 2);
        assertTrue(found.isPresent());
        assertEquals(2, found.get());

        OptionalByte notFound = createSingleThreadStream((byte) 1, (byte) 2).findFirst(b -> b > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindFirst() throws Exception {
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.findFirst(b -> b == 5);
        assertTrue(result1.isPresent());
        assertEquals(5, result1.get());

        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.findFirst(b -> b == 50);
        assertFalse(result2.isPresent());

        ByteStream stream3 = createStream(TEST_ARRAY);
        OptionalByte result3 = stream3.findFirst(b -> b % 2 == 0);
        assertTrue(result3.isPresent());
        assertEquals(2, result3.get());

        ByteStream stream4 = createStream(new byte[] {});
        OptionalByte result4 = stream4.findFirst(b -> true);
        assertFalse(result4.isPresent());
        stream4.close();
    }

    @Test
    public void testFindAny_SequentialFallback() {
        OptionalByte found = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3).findAny(b -> b == 2);
        assertTrue(found.isPresent());
        assertEquals(2, found.get());

        OptionalByte notFound = createSingleThreadStream((byte) 1, (byte) 2).findAny(b -> b > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindAny() throws Exception {
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.findAny(b -> b == 5);
        assertTrue(result1.isPresent());
        assertEquals(5, result1.get());

        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.findAny(b -> b == 50);
        assertFalse(result2.isPresent());

        ByteStream stream3 = createStream(new byte[] {});
        OptionalByte result3 = stream3.findAny(b -> true);
        assertFalse(result3.isPresent());
    }

    @Test
    public void testFindLast_SequentialFallback() {
        OptionalByte found = createSingleThreadStream((byte) 1, (byte) 2, (byte) 3, (byte) 1).findLast(b -> b == 1);
        assertTrue(found.isPresent());
        assertEquals(1, found.get());

        OptionalByte notFound = createSingleThreadStream((byte) 1, (byte) 2).findLast(b -> b > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindLast() throws Exception {
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.findLast(b -> b == 1);
        assertTrue(result1.isPresent());
        assertEquals(1, result1.get());

        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.findLast(b -> b == 50);
        assertFalse(result2.isPresent());

        ByteStream stream3 = createStream(TEST_ARRAY);
        OptionalByte result3 = stream3.findLast(b -> b % 2 == 0);
        assertTrue(result3.isPresent());
        assertEquals(2, result3.get());

        ByteStream stream4 = createStream(new byte[] {});
        OptionalByte result4 = stream4.findLast(b -> true);
        assertFalse(result4.isPresent());
        stream4.close();
    }

    @Test
    public void testZipWithTwoStreams() {
        ByteStream streamA = createStream(new byte[] { 1, 2, 3 });
        ByteStream streamB = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        byte[] result = streamA.zipWith(streamB, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(new byte[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        byte[] dataA = { 1, 2, 3 };
        byte[] dataB = { 10, 20, 30, 40 };
        byte[] dataC = { 50, 60 };

        ByteStream stream1A = createStream(dataA);
        ByteStream stream1B = createStream(dataB);
        ByteStream stream1C = createStream(dataC);
        byte[] result1 = stream1A.zipWith(stream1B, stream1C, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertHaveSameElements(new byte[] { (byte) (1 + 10 + 50), (byte) (2 + 20 + 60) }, result1);
        stream1A.close();
        stream1B.close();
        stream1C.close();

        ByteStream stream2A = createStream(dataA);
        ByteStream stream2B = createStream(dataB);
        ByteStream stream2C = createStream(dataC);
        byte[] result2 = stream2A.zipWith(stream2B, stream2C, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertHaveSameElements(new byte[] { (byte) (1 + 10 + 50), (byte) (2 + 20 + 60) }, result2);
        stream2A.close();
        stream2B.close();
        stream2C.close();
    }

    @Test
    public void testZipWithTwoStreamsWithDefaultValues() {
        ByteStream streamA = createStream(new byte[] { 1, 2, 3 });
        ByteStream streamB = ByteStream.of((byte) 10);
        byte defaultA = (byte) -1;
        byte defaultB = (byte) -2;
        byte[] result = streamA.zipWith(streamB, defaultA, defaultB, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(new byte[] { 11, (byte) (2 + (-2)), (byte) (3 + (-2)) }, result);
    }

    @Test
    public void testZipWithThreeStreamsWithDefaultValues() {
        byte[] dataA = { 1 };
        byte[] dataB = { 10, 20 };
        byte[] dataC = { 50, 60, 50 };
        byte defaultA = (byte) -1;
        byte defaultB = (byte) -2;
        byte defaultC = (byte) -3;

        ByteStream stream1A = createStream(dataA);
        ByteStream stream1B = createStream(dataB);
        ByteStream stream1C = createStream(dataC);
        byte[] result1 = stream1A.zipWith(stream1B, stream1C, defaultA, defaultB, defaultC, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertHaveSameElements(N.toList(new byte[] { (byte) (1 + 10 + 50), (byte) (defaultA + 20 + 60), (byte) (defaultA + defaultB + 50) }),
                N.toList(result1));
        stream1A.close();
        stream1B.close();
        stream1C.close();

        ByteStream stream2A = createStream(dataA);
        ByteStream stream2B = createStream(dataB);
        ByteStream stream2C = createStream(dataC);
        byte[] result2 = stream2A.zipWith(stream2B, stream2C, defaultA, defaultB, defaultC, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertHaveSameElements(N.toList(new byte[] { (byte) (1 + 10 + 50), (byte) (defaultA + 20 + 60), (byte) (defaultA + defaultB + 50) }),
                N.toList(result2));
        stream2A.close();
        stream2B.close();
        stream2C.close();
    }

    @Test
    public void testZipWithDefaultValues_SequentialFallback() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                .map(e -> (byte) (e + 0))
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .zipWith(ByteStream.of((byte) 10), (byte) 0, (byte) -1, (a, b) -> (byte) (a + b))
                .toArray();

        assertArrayEquals(new byte[] { 11, 1, 2 }, result);
    }

    @Test
    public void testIsParallel() {
        ByteStream stream = createStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
        stream.close();
    }

    @Test
    public void testSequential() {
        ByteStream parallelStream = createStream(TEST_ARRAY);
        ByteStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        byte[] result = sequentialStream.toArray();
        assertArrayEquals(TEST_ARRAY, result);
        parallelStream.close();
    }

    @Test
    public void testMaxThreadNum() {
        assertEquals(testMaxThreadNum, ((ParallelIteratorByteStream) createStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorByteStream) createStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() {
        assertTrue(((ParallelIteratorByteStream) createStream(TEST_ARRAY)).asyncExecutor() != null);
    }
}
