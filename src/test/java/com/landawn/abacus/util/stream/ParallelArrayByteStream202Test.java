package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelArrayByteStream202Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final byte[] TEST_ARRAY = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3 };

    // Helper method to create a ByteStream for testing
    private ByteStream createStream(byte... elements) {
        // Using default values for maxThreadNum, executorNumForVirtualThread, splitor, cancelUncompletedThreads
        // For testing, we might want to vary these parameters.
        return ByteStream.of(elements).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testFilter() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        byte[] result1 = stream1.filter(b -> b % 2 == 0).toArray();
        assertHaveSameElements(new byte[] { 2, 4, 6, 8, 10, 2 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        byte[] result2 = stream2.filter(b -> b > 5).toArray();
        assertHaveSameElements(new byte[] { 6, 7, 8, 9, 10 }, result2);

        // Test with empty stream
        ByteStream stream3 = createStream(new byte[] {});
        byte[] result3 = stream3.filter(b -> true).toArray();
        assertHaveSameElements(new byte[] {}, result3);

        // Test with all elements filtered out
        ByteStream stream4 = createStream(TEST_ARRAY);
        byte[] result4 = stream4.filter(b -> b > 50).toArray();
        assertArrayEquals(new byte[] {}, result4);
        stream4.close();
    }

    @Test
    public void testTakeWhile() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result1 = stream1.takeWhile(b -> b < 4).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 3 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result2 = stream2.takeWhile(b -> b < 4).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 3 }, result2);

        // Test where predicate is always true
        ByteStream stream3 = createStream(TEST_ARRAY);
        byte[] result3 = stream3.takeWhile(b -> true).toArray();
        assertHaveSameElements(N.toList(TEST_ARRAY), N.toList(result3));

        // Test where predicate is always false (should be empty)
        ByteStream stream4 = createStream(TEST_ARRAY);
        byte[] result4 = stream4.takeWhile(b -> false).toArray();
        assertArrayEquals(new byte[] {}, result4);
        stream4.close();
    }

    @Test
    public void testDropWhile() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result1 = stream1.dropWhile(b -> b < 3).toArray();
        assertHaveSameElements(new byte[] { 3, 4, 5, 6 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 4, 5, 6 });
        byte[] result2 = stream2.dropWhile(b -> b < 3).toArray();
        assertHaveSameElements(new byte[] { 3, 4, 5, 6 }, result2);

        // Test where predicate is always true (should be empty)
        ByteStream stream3 = createStream(TEST_ARRAY);
        byte[] result3 = stream3.dropWhile(b -> true).toArray();
        assertHaveSameElements(new byte[] {}, result3);

        // Test where predicate is always false
        ByteStream stream4 = createStream(TEST_ARRAY);
        byte[] result4 = stream4.dropWhile(b -> false).toArray();
        assertHaveSameElements(N.toList(TEST_ARRAY), N.toList(result4));
        stream4.close();
    }

    @Test
    public void testMap() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        byte[] result1 = stream1.map(b -> (byte) (b * 2)).toArray();
        assertHaveSameElements(new byte[] { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 2, 4, 6 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        byte[] result2 = stream2.map(b -> (byte) (b + 1)).toArray();
        assertHaveSameElements(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 2, 3, 4 }, result2);

        // Test with empty stream
        ByteStream stream3 = createStream(new byte[] {});
        byte[] result3 = stream3.map(b -> (byte) (b + 1)).toArray();
        assertHaveSameElements(new byte[] {}, result3);
    }

    @Test
    public void testMapToInt() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        int[] result1 = stream1.mapToInt(b -> b * 10).toArray();
        assertHaveSameElements(new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 10, 20, 30 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        int[] result2 = stream2.mapToInt(b -> b + 100).toArray();
        assertHaveSameElements(new int[] { 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 101, 102, 103 }, result2);
    }

    @Test
    public void testMapToObj() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        List<String> result1 = stream1.mapToObj(b -> "Byte_" + b).toList();
        assertHaveSameElements(Arrays.asList("Byte_1", "Byte_2", "Byte_3", "Byte_4", "Byte_5", "Byte_6", "Byte_7", "Byte_8", "Byte_9", "Byte_10", "Byte_1",
                "Byte_2", "Byte_3"), result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        List<Integer> result2 = stream2.mapToObj(b -> (int) b).toList();
        assertHaveSameElements(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3), result2);
    }

    @Test
    public void testFlatMap() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3 });
        byte[] result1 = stream1.flatMap(b -> ByteStream.of(b, (byte) (b + 10))).toArray();
        assertHaveSameElements(new byte[] { 1, 11, 2, 12, 3, 13 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3 });
        byte[] result2 = stream2.flatMap(b -> ByteStream.of(b, (byte) (b * 2))).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 2, 4, 3, 6 }, result2);

        // Test with empty stream
        ByteStream stream3 = createStream(new byte[] {});
        byte[] result3 = stream3.flatMap(b -> ByteStream.of(b)).toArray();
        assertHaveSameElements(new byte[] {}, result3);
    }

    @Test
    public void testFlatmapByteArray() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3 });
        byte[] result1 = stream1.flatmap(b -> new byte[] { b, (byte) (b + 10) }).toArray();
        assertHaveSameElements(new byte[] { 1, 11, 2, 12, 3, 13 }, result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3 });
        byte[] result2 = stream2.flatmap(b -> new byte[] { b, (byte) (b * 2) }).toArray();
        assertHaveSameElements(new byte[] { 1, 2, 2, 4, 3, 6 }, result2);
    }

    @Test
    public void testFlatMapToInt() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3 });
        int[] result1 = stream1.flatMapToInt(b -> IntStream.of(b, b + 100)).toArray();
        assertHaveSameElements(N.asList(1, 101, 2, 102, 3, 103), N.toList(result1));

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3 });
        int[] result2 = stream2.flatMapToInt(b -> IntStream.of(b * 2)).toArray();
        assertHaveSameElements(N.asList(2, 4, 6), N.toList(result2));
    }

    @Test
    public void testFlatMapToObj() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2 });
        List<String> result1 = stream1.flatMapToObj(b -> Stream.of("A" + b, "B" + b)).toList();
        assertHaveSameElements(Arrays.asList("A1", "B1", "A2", "B2"), result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2 });
        List<Integer> result2 = stream2.flatMapToObj(b -> Stream.of((int) b, (int) (b * 10))).toList();
        assertHaveSameElements(Arrays.asList(1, 10, 2, 20), result2);
    }

    @Test
    public void testFlatmapToObjCollection() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2 });
        List<String> result1 = stream1.flatmapToObj(b -> Arrays.asList("A" + b, "B" + b)).toList();
        assertHaveSameElements(Arrays.asList("A1", "B1", "A2", "B2"), result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2 });
        List<Integer> result2 = stream2.flatmapToObj(b -> Arrays.asList((int) b, (int) (b * 10))).toList();
        assertHaveSameElements(Arrays.asList(1, 10, 2, 20), result2);
    }

    @Test
    public void testOnEach() {
        List<Byte> consumedElements = new CopyOnWriteArrayList<>();

        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        stream1.onEach(consumedElements::add).count(); // Trigger terminal op
        assertEquals(TEST_ARRAY.length, consumedElements.size());
        assertTrue(consumedElements.containsAll(N.toList(TEST_ARRAY)));

        consumedElements.clear();

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        stream2.onEach(consumedElements::add).count(); // Trigger terminal op
        assertEquals(TEST_ARRAY.length, consumedElements.size());
        assertTrue(consumedElements.containsAll(N.toList(TEST_ARRAY)));
    }

    @Test
    public void testForEach() throws Exception {
        List<Byte> consumedElements = new CopyOnWriteArrayList<>();

        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        stream1.forEach(consumedElements::add);
        assertEquals(TEST_ARRAY.length, consumedElements.size());
        assertTrue(consumedElements.containsAll(N.toList(TEST_ARRAY)));

        consumedElements.clear();

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        stream2.forEach(consumedElements::add);
        assertEquals(TEST_ARRAY.length, consumedElements.size());
        assertTrue(consumedElements.containsAll(N.toList(TEST_ARRAY)));

        // Test with exception
        ByteStream stream3 = createStream(TEST_ARRAY);
        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> stream3.forEach(b -> {
            if (counter.incrementAndGet() == 5) {
                throw new RuntimeException("Test Exception");
            }
        }));
    }

    @Test
    public void testToMap() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 1, 4 });
        Map<String, Integer> map1 = stream1.toMap(b -> "Key_" + b, b -> (int) b, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        Map<String, Integer> expectedMap1 = new ConcurrentHashMap<>();
        expectedMap1.put("Key_1", 1 + 1);
        expectedMap1.put("Key_2", 2);
        expectedMap1.put("Key_3", 3);
        expectedMap1.put("Key_4", 4);
        assertEquals(expectedMap1, map1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 1, 4 });
        Map<String, Integer> map2 = stream2.toMap(b -> "Key_" + b, b -> (int) b, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(expectedMap1, map2);
    }

    @Test
    public void testGroupTo() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 1, 2, 3, 1, 4 });
        Map<Boolean, List<Byte>> map1 = stream1.groupTo(b -> b % 2 == 0, java.util.stream.Collectors.toList(), ConcurrentHashMap::new);
        Map<Boolean, List<Byte>> expectedMap1 = new ConcurrentHashMap<>();
        expectedMap1.put(true, Arrays.asList((byte) 2, (byte) 4));
        expectedMap1.put(false, Arrays.asList((byte) 1, (byte) 3, (byte) 1));
        // Order within lists might vary due to parallel execution, so check content
        assertEquals(expectedMap1.keySet(), map1.keySet());
        assertTrue(map1.get(true).containsAll(expectedMap1.get(true)) && expectedMap1.get(true).containsAll(map1.get(true)));
        assertTrue(map1.get(false).containsAll(expectedMap1.get(false)) && expectedMap1.get(false).containsAll(map1.get(false)));

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(new byte[] { 1, 2, 3, 1, 4 });
        Map<Boolean, List<Byte>> map2 = stream2.groupTo(b -> b % 2 == 0, java.util.stream.Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(expectedMap1.keySet(), map2.keySet());
        assertTrue(map2.get(true).containsAll(expectedMap1.get(true)) && expectedMap1.get(true).containsAll(map2.get(true)));
        assertTrue(map2.get(false).containsAll(expectedMap1.get(false)) && expectedMap1.get(false).containsAll(map2.get(false)));
    }

    @Test
    public void testReduceWithIdentity() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        byte result1 = stream1.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(N.sum(TEST_ARRAY), result1);

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        byte result2 = stream2.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(N.sum(TEST_ARRAY), result2);
    }

    @Test
    public void testReduce() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.reduce((a, b) -> (byte) (a + b));
        assertTrue(result1.isPresent());
        assertEquals(N.sum(TEST_ARRAY), result1.get());

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.reduce((a, b) -> (byte) (a + b));
        assertTrue(result2.isPresent());
        assertEquals(N.sum(TEST_ARRAY), result2.get());

        // Test with empty stream
        ByteStream stream3 = createStream(new byte[] {});
        OptionalByte result3 = stream3.reduce((a, b) -> (byte) (a + b));
        assertFalse(result3.isPresent());

        // Test with single element
        ByteStream stream4 = createStream(new byte[] { 10 });
        OptionalByte result4 = stream4.reduce((a, b) -> (byte) (a + b));
        assertTrue(result4.isPresent());
        assertEquals(10, result4.get());
        stream4.close();
    }

    @Test
    public void testCollect() {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        List<Byte> collected1 = stream1.collect(CopyOnWriteArrayList::new, CopyOnWriteArrayList::add, CopyOnWriteArrayList::addAll);
        assertEquals(TEST_ARRAY.length, collected1.size());
        assertTrue(collected1.containsAll(N.toList(TEST_ARRAY)));

        // Test with Splitor.QUEUE
        ByteStream stream2 = createStream(TEST_ARRAY);
        List<Byte> collected2 = stream2.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, collected2.size());
        assertTrue(collected2.containsAll(N.toList(TEST_ARRAY)));
    }

    @Test
    public void testAnyMatch() throws Exception {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        assertTrue(stream1.anyMatch(b -> b == 5));

        ByteStream stream2 = createStream(TEST_ARRAY);
        assertFalse(stream2.anyMatch(b -> b == 50));

        // Test with Splitor.QUEUE
        ByteStream stream3 = createStream(TEST_ARRAY);
        assertTrue(stream3.anyMatch(b -> b % 3 == 0));

        ByteStream stream4 = createStream(TEST_ARRAY);
        assertFalse(stream4.anyMatch(b -> b < 0));
        stream4.close();

        // Test with empty stream
        ByteStream stream5 = createStream(new byte[] {});
        assertFalse(stream5.anyMatch(b -> true));
        stream5.close();
    }

    @Test
    public void testAllMatch() throws Exception {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 2, 4, 6 });
        assertTrue(stream1.allMatch(b -> b % 2 == 0));

        ByteStream stream2 = createStream(TEST_ARRAY);
        assertFalse(stream2.allMatch(b -> b < 5));

        // Test with Splitor.QUEUE
        ByteStream stream3 = createStream(new byte[] { 1, 3, 5 });
        assertTrue(stream3.allMatch(b -> b % 2 != 0));

        ByteStream stream4 = createStream(TEST_ARRAY);
        assertFalse(stream4.allMatch(b -> b < 5));
        stream4.close();

        // Test with empty stream
        ByteStream stream5 = createStream(new byte[] {});
        assertTrue(stream5.allMatch(b -> true));
        stream5.close();
    }

    @Test
    public void testNoneMatch() throws Exception {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(new byte[] { 2, 4, 6 });
        assertFalse(stream1.noneMatch(b -> b % 2 == 0));

        ByteStream stream2 = createStream(TEST_ARRAY);
        assertTrue(stream2.noneMatch(b -> b > 50));

        // Test with Splitor.QUEUE
        ByteStream stream3 = createStream(new byte[] { 1, 3, 5 });
        assertFalse(stream3.noneMatch(b -> b % 2 != 0));

        ByteStream stream4 = createStream(TEST_ARRAY);
        assertTrue(stream4.noneMatch(b -> b < 0));
        stream4.close();

        // Test with empty stream
        ByteStream stream5 = createStream(new byte[] {});
        assertTrue(stream5.noneMatch(b -> true));
        stream5.close();
    }

    @Test
    public void testFindFirst() throws Exception {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.findFirst(b -> b == 5);
        assertTrue(result1.isPresent());
        assertEquals(5, result1.get());

        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.findFirst(b -> b == 50);
        assertFalse(result2.isPresent());

        // Test with Splitor.QUEUE
        ByteStream stream3 = createStream(TEST_ARRAY);
        OptionalByte result3 = stream3.findFirst(b -> b % 2 == 0);
        assertTrue(result3.isPresent());
        assertEquals(2, result3.get()); // First even number is 2

        // Test with empty stream
        ByteStream stream4 = createStream(new byte[] {});
        OptionalByte result4 = stream4.findFirst(b -> true);
        assertFalse(result4.isPresent());
        stream4.close();
    }

    @Test
    public void testFindAny() throws Exception {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.findAny(b -> b == 5);
        assertTrue(result1.isPresent());
        assertEquals(5, result1.get()); // Could be any '5' if duplicates, but for single 5 it's definite.

        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.findAny(b -> b == 50);
        assertFalse(result2.isPresent());

        // Test with Splitor.QUEUE
        ByteStream stream3 = createStream(TEST_ARRAY);
        OptionalByte result3 = stream3.findAny(b -> b % 3 == 0);
        assertTrue(result3.isPresent());
        // The exact value could be 3, 6, or 9 depending on thread execution order
        assertTrue(Arrays.asList((byte) 3, (byte) 6, (byte) 9).contains(result3.get()));

        // Test with empty stream
        ByteStream stream4 = createStream(new byte[] {});
        OptionalByte result4 = stream4.findAny(b -> true);
        assertFalse(result4.isPresent());
        stream4.close();
    }

    @Test
    public void testFindLast() throws Exception {
        // Test with Splitor.ARRAY
        ByteStream stream1 = createStream(TEST_ARRAY);
        OptionalByte result1 = stream1.findLast(b -> b == 1);
        assertTrue(result1.isPresent());
        assertEquals(1, result1.get()); // Last '1' in testData

        ByteStream stream2 = createStream(TEST_ARRAY);
        OptionalByte result2 = stream2.findLast(b -> b == 50);
        assertFalse(result2.isPresent());

        // Test with Splitor.QUEUE
        ByteStream stream3 = createStream(TEST_ARRAY);
        OptionalByte result3 = stream3.findLast(b -> b % 2 == 0);
        assertTrue(result3.isPresent());
        assertEquals(2, result3.get()); // Last even number in the list is '2'

        // Test with empty stream
        ByteStream stream4 = createStream(new byte[] {});
        OptionalByte result4 = stream4.findLast(b -> true);
        assertFalse(result4.isPresent());
        stream4.close();
    }

    @Test
    public void testZipWithTwoStreams() {
        byte[] dataA = { 1, 2, 3, 4 };
        byte[] dataB = { 10, 20, 30 };

        // Test with Splitor.ARRAY, shorter stream dictates length
        ByteStream stream1A = createStream(dataA);
        ByteStream stream1B = createStream(dataB);
        byte[] result1 = stream1A.zipWith(stream1B, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(new byte[] { 11, 22, 33 }, result1);
        stream1A.close();
        stream1B.close();

        // Test with Splitor.QUEUE
        ByteStream stream2A = createStream(dataA);
        ByteStream stream2B = createStream(dataB);
        byte[] result2 = stream2A.zipWith(stream2B, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(N.asList((byte) 11, (byte) 22, (byte) 33), N.toList(result2));
        stream2A.close();
        stream2B.close();

        // Test with empty stream
        ByteStream stream3A = createStream(new byte[] {});
        ByteStream stream3B = createStream(dataB);
        byte[] result3 = stream3A.zipWith(stream3B, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(new byte[] {}, result3);
        stream3A.close();
        stream3B.close();
    }

    @Test
    public void testZipWithThreeStreams() {
        byte[] dataA = { 1, 2, 3 };
        byte[] dataB = { 10, 20, 30, 40 };
        byte[] dataC = { 50, 60 }; // 60 will wrap around to -56 due to byte overflow

        // Test with Splitor.ARRAY, shortest stream dictates length
        ByteStream stream1A = createStream(dataA);
        ByteStream stream1B = createStream(dataB);
        ByteStream stream1C = createStream(dataC);
        byte[] result1 = stream1A.zipWith(stream1B, stream1C, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertHaveSameElements(new byte[] { (byte) (1 + 10 + 50), (byte) (2 + 20 + 60) }, result1);
        stream1A.close();
        stream1B.close();
        stream1C.close();

        // Test with Splitor.QUEUE
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
        byte[] dataA = { 1, 2 };
        byte[] dataB = { 10, 20, 30 };
        byte defaultA = (byte) 50;
        byte defaultB = (byte) 60;

        // Test with Splitor.ARRAY, longest stream dictates length, uses default values
        ByteStream stream1A = createStream(dataA);
        ByteStream stream1B = createStream(dataB);
        byte[] result1 = stream1A.zipWith(stream1B, defaultA, defaultB, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(new byte[] { (byte) (1 + 10), (byte) (2 + 20), (byte) (defaultA + 30) }, result1);
        stream1A.close();
        stream1B.close();

        // Test with Splitor.QUEUE
        ByteStream stream2A = createStream(dataA);
        ByteStream stream2B = createStream(dataB);
        byte[] result2 = stream2A.zipWith(stream2B, defaultA, defaultB, (a, b) -> (byte) (a + b)).toArray();
        assertHaveSameElements(new byte[] { (byte) (1 + 10), (byte) (2 + 20), (byte) (defaultA + 30) }, result2);
        stream2A.close();
        stream2B.close();
    }

    @Test
    public void testZipWithThreeStreamsWithDefaultValues() {
        byte[] dataA = { 1 };
        byte[] dataB = { 10, 20 };
        byte[] dataC = { 50, 60, 50 };
        byte defaultA = (byte) -1;
        byte defaultB = (byte) -2;
        byte defaultC = (byte) -3;

        // Test with Splitor.ARRAY, longest stream dictates length, uses default values
        ByteStream stream1A = createStream(dataA);
        ByteStream stream1B = createStream(dataB);
        ByteStream stream1C = createStream(dataC);
        byte[] result1 = stream1A.zipWith(stream1B, stream1C, defaultA, defaultB, defaultC, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertHaveSameElements(N.toList(new byte[] { (byte) (1 + 10 + 50), (byte) (defaultA + 20 + 60), (byte) (defaultA + defaultB + 50) }),
                N.toList(result1));
        stream1A.close();
        stream1B.close();
        stream1C.close();

        // Test with Splitor.QUEUE
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
    public void testIsParallel() {
        ByteStream stream = createStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        ByteStream parallelStream = createStream(TEST_ARRAY);
        ByteStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        byte[] result = sequentialStream.toArray();
        assertArrayEquals(TEST_ARRAY, result);
        parallelStream.close(); // Closing the parallel stream should also close the derived sequential stream
    }

    @Test
    public void testOnClose() {
        AtomicReference<String> closeMessage = new AtomicReference<>();
        Runnable closeHandler = () -> closeMessage.set("Stream Closed");

        ByteStream stream = createStream(TEST_ARRAY);
        stream.onClose(closeHandler).count(); // Trigger terminal operation to close
        assertEquals("Stream Closed", closeMessage.get()); // Redundant, but ensures no issues
    }
}
