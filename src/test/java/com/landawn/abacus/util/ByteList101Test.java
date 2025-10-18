package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.ByteStream;

@Tag("new-test")
public class ByteList101Test extends TestBase {

    private ByteList list;

    @BeforeEach
    public void setUp() {
        list = new ByteList();
    }

    @Test
    public void testRangeWithExtremeValues() {
        ByteList list1 = ByteList.range((byte) 120, (byte) 127);
        assertEquals(7, list1.size());
        assertEquals((byte) 120, list1.get(0));
        assertEquals((byte) 126, list1.get(6));

        ByteList list2 = ByteList.range((byte) -5, (byte) 5);
        assertEquals(10, list2.size());
        assertEquals((byte) -5, list2.get(0));
        assertEquals((byte) 4, list2.get(9));

        ByteList list3 = ByteList.range((byte) -128, (byte) 127, (byte) 25);
        assertTrue(list3.size() > 0);
        assertEquals((byte) -128, list3.get(0));

        ByteList list4 = ByteList.rangeClosed(Byte.MIN_VALUE, Byte.MIN_VALUE);
        assertEquals(1, list4.size());
        assertEquals(Byte.MIN_VALUE, list4.get(0));

        ByteList list5 = ByteList.rangeClosed(Byte.MAX_VALUE, Byte.MAX_VALUE);
        assertEquals(1, list5.size());
        assertEquals(Byte.MAX_VALUE, list5.get(0));
    }

    @Test
    public void testRangeWithNegativeStep() {
        ByteList list1 = ByteList.range((byte) 10, (byte) 0, (byte) -1);
        assertEquals(10, list1.size());
        assertEquals((byte) 10, list1.get(0));
        assertEquals((byte) 1, list1.get(9));

        ByteList list2 = ByteList.rangeClosed((byte) 10, (byte) 0, (byte) -2);
        assertEquals(6, list2.size());
        assertEquals((byte) 10, list2.get(0));
        assertEquals((byte) 8, list2.get(1));
        assertEquals((byte) 0, list2.get(5));
    }

    @Test
    public void testCapacityGrowthWithByteOverflow() {
        for (byte b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
            list.add(b);
            if (list.size() > 1000)
                break;
        }

        assertTrue(list.size() > 255);

        for (int i = 0; i < Math.min(10, list.size()); i++) {
            assertEquals((byte) (Byte.MIN_VALUE + i), list.get(i));
        }
    }

    @Test
    public void testConstructorWithZeroCapacity() {
        ByteList zeroCapList = new ByteList(0);
        assertEquals(0, zeroCapList.size());

        zeroCapList.add((byte) 1);
        assertEquals(1, zeroCapList.size());
        assertEquals((byte) 1, zeroCapList.get(0));
    }

    @Test
    public void testMinMaxMedianWithSpecialCases() {
        for (int i = 0; i < 5; i++) {
            list.add((byte) 42);
        }
        assertEquals(OptionalByte.of((byte) 42), list.min());
        assertEquals(OptionalByte.of((byte) 42), list.max());
        assertEquals(OptionalByte.of((byte) 42), list.median());

        list.clear();
        list.add(Byte.MAX_VALUE);
        list.add(Byte.MIN_VALUE);
        list.add((byte) 0);

        assertEquals(OptionalByte.of(Byte.MIN_VALUE), list.min());
        assertEquals(OptionalByte.of(Byte.MAX_VALUE), list.max());
        assertEquals(OptionalByte.of((byte) 0), list.median());

        list.add((byte) 1);
        OptionalByte median = list.median();
        assertTrue(median.isPresent());
        byte medianValue = median.get();
        assertTrue(medianValue == 0 || medianValue == 1);
    }

    @Test
    public void testMinMaxMedianWithLargeDataset() {
        Random rand = new Random(42);
        for (int i = 0; i < 1000; i++) {
            list.add((byte) rand.nextInt(256));
        }

        OptionalByte min = list.min();
        OptionalByte max = list.max();
        OptionalByte median = list.median();

        assertTrue(min.isPresent());
        assertTrue(max.isPresent());
        assertTrue(median.isPresent());

        assertTrue(min.get() <= median.get());
        assertTrue(median.get() <= max.get());
    }

    @Test
    public void testBinarySearchWithDuplicates() {
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 3);
        list.add((byte) 3);
        list.add((byte) 5);

        int index = list.binarySearch((byte) 3);
        assertTrue(index >= 1 && index <= 3);
        assertEquals((byte) 3, list.get(index));

        index = list.binarySearch(2, 4, (byte) 3);
        assertTrue(index >= 2 && index <= 3);
    }

    @Test
    public void testBinarySearchWithAllSameElements() {
        for (int i = 0; i < 10; i++) {
            list.add((byte) 5);
        }

        int index = list.binarySearch((byte) 5);
        assertTrue(index >= 0 && index < 10);
        assertEquals((byte) 5, list.get(index));

        assertTrue(list.binarySearch((byte) 3) < 0);
        assertTrue(list.binarySearch((byte) 7) < 0);
    }

    @Test
    public void testParallelSortWithVariousDataPatterns() {
        for (int i = 0; i < 100; i++) {
            list.add((byte) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        for (int i = 100; i >= 0; i--) {
            list.add((byte) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        Random rand = new Random(42);
        for (int i = 0; i < 200; i++) {
            list.add((byte) rand.nextInt(256));
        }
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testRemoveIfWithComplexPredicates() {
        for (byte i = -10; i <= 10; i++) {
            list.add(i);
        }

        assertTrue(list.removeIf(b -> b < 0));
        assertEquals(11, list.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 0);
        }

        assertTrue(list.removeIf(b -> b % 2 == 0));
        assertEquals(5, list.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) % 2 == 1);
        }
    }

    @Test
    public void testBatchRemoveOptimization() {
        for (int i = 0; i < 100; i++) {
            list.add((byte) (i % 10));
        }

        ByteList toRemove = new ByteList();
        for (int i = 0; i < 50; i++) {
            toRemove.add((byte) (i % 5));
        }

        int originalSize = list.size();
        list.removeAll(toRemove);
        assertTrue(list.size() < originalSize);

        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 5 && list.get(i) <= 9);
        }
    }

    @Test
    public void testReplaceAllWithByteOverflow() {
        list.add((byte) 100);
        list.add((byte) 120);
        list.add((byte) -100);

        list.replaceAll(b -> (byte) (b * 2));

        assertEquals((byte) -56, list.get(0));
        assertEquals((byte) -16, list.get(1));
        assertEquals((byte) 56, list.get(2));
    }

    @Test
    public void testReplaceIfWithMultipleConditions() {
        for (byte i = 0; i < 20; i++) {
            list.add(i);
        }

        assertTrue(list.replaceIf(b -> b % 3 == 0 || b % 5 == 0, (byte) -1));

        assertEquals((byte) -1, list.get(0));
        assertEquals((byte) 1, list.get(1));
        assertEquals((byte) 2, list.get(2));
        assertEquals((byte) -1, list.get(3));
        assertEquals((byte) 4, list.get(4));
        assertEquals((byte) -1, list.get(5));
    }

    @Test
    public void testIntersectionWithMultipleDuplicates() {
        list.add((byte) 1);
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);

        ByteList other = ByteList.of((byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 2);
        ByteList intersection = list.intersection(other);

        assertEquals(4, intersection.size());
        int onesCount = 0;
        int twosCount = 0;
        for (int i = 0; i < intersection.size(); i++) {
            if (intersection.get(i) == 1)
                onesCount++;
            else if (intersection.get(i) == 2)
                twosCount++;
        }
        assertEquals(3, onesCount);
        assertEquals(1, twosCount);
    }

    @Test
    public void testSymmetricDifferenceWithComplexCases() {
        list.add((byte) 1);
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        ByteList other = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 4);

        ByteList symDiff = list.symmetricDifference(other);

        assertEquals(4, symDiff.size());
        assertTrue(symDiff.contains((byte) 1));
        assertTrue(symDiff.contains((byte) 2));
        assertTrue(symDiff.contains((byte) 3));
        assertTrue(symDiff.contains((byte) 4));
    }

    @Test
    public void testStreamOperations() {
        for (byte i = 1; i <= 10; i++) {
            list.add(i);
        }

        ByteStream stream = list.stream();
        int sum = stream.sum();
        assertEquals((byte) 55, sum);

        ByteStream rangeStream = list.stream(2, 5);
        byte[] arr = rangeStream.toArray();
        assertEquals(3, arr.length);
        assertEquals((byte) 3, arr[0]);
        assertEquals((byte) 4, arr[1]);
        assertEquals((byte) 5, arr[2]);
    }

    @Test
    public void testStreamWithEmptyList() {
        ByteStream stream = list.stream();
        byte[] arr = stream.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testToIntListWithSignExtension() {
        list.add((byte) -1);
        list.add((byte) -128);
        list.add((byte) 127);
        list.add((byte) 0);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(-1, intList.get(0));
        assertEquals(-128, intList.get(1));
        assertEquals(127, intList.get(2));
        assertEquals(0, intList.get(3));
    }

    @Test
    public void testForEachWithEarlyTermination() {
        for (byte i = 0; i < 10; i++) {
            list.add(i);
        }

        final int[] count = { 0 };
        final List<Byte> collected = new ArrayList<>();

        list.forEach(b -> {
            collected.add(b);
            count[0]++;
        });

        assertEquals(10, count[0]);
        assertEquals(10, collected.size());
    }

    @Test
    public void testForEachReverseWithComplexRange() {
        for (byte i = 0; i < 10; i++) {
            list.add(i);
        }

        List<Byte> collected = new ArrayList<>();
        list.forEach(7, 2, b -> collected.add(b));

        assertEquals(5, collected.size());
        assertEquals(Byte.valueOf((byte) 7), collected.get(0));
        assertEquals(Byte.valueOf((byte) 6), collected.get(1));
        assertEquals(Byte.valueOf((byte) 5), collected.get(2));
        assertEquals(Byte.valueOf((byte) 4), collected.get(3));
        assertEquals(Byte.valueOf((byte) 3), collected.get(4));
    }

    @Test
    public void testCopyWithNegativeStep() {
        for (byte i = 0; i < 10; i++) {
            list.add(i);
        }

        ByteList reversed = list.copy(9, -1, -1);
        assertEquals(10, reversed.size());
        for (int i = 0; i < 10; i++) {
            assertEquals((byte) (9 - i), reversed.get(i));
        }

        ByteList everyOtherReverse = list.copy(9, -1, -2);
        assertEquals(5, everyOtherReverse.size());
        assertEquals((byte) 9, everyOtherReverse.get(0));
        assertEquals((byte) 7, everyOtherReverse.get(1));
        assertEquals((byte) 5, everyOtherReverse.get(2));
        assertEquals((byte) 3, everyOtherReverse.get(3));
        assertEquals((byte) 1, everyOtherReverse.get(4));
    }

    @Test
    public void testSplitWithUnevenChunks() {
        for (byte i = 1; i <= 17; i++) {
            list.add(i);
        }

        List<ByteList> chunks = list.split(0, 17, 5);
        assertEquals(4, chunks.size());
        assertEquals(5, chunks.get(0).size());
        assertEquals(5, chunks.get(1).size());
        assertEquals(5, chunks.get(2).size());
        assertEquals(2, chunks.get(3).size());

        assertEquals((byte) 1, chunks.get(0).get(0));
        assertEquals((byte) 6, chunks.get(1).get(0));
        assertEquals((byte) 11, chunks.get(2).get(0));
        assertEquals((byte) 16, chunks.get(3).get(0));
    }

    @Test
    public void testArrayMethodReturnsSameReference() {
        list.add((byte) 1);
        list.add((byte) 2);

        byte[] array1 = list.array();
        byte[] array2 = list.array();

        assertSame(array1, array2);

        array1[0] = 100;
        assertEquals((byte) 100, list.get(0));
    }

    @Test
    public void testLargeDatasetOperations() {
        final int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add((byte) (i % 256));
        }

        assertEquals(size, list.size());

        assertTrue(list.contains((byte) 100));
        assertTrue(list.indexOf((byte) 50) >= 0);

        list.sort();
        assertTrue(list.isSorted());

        ByteList distinct = list.distinct(0, list.size());
        assertEquals(256, distinct.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testByteValueBoundaries() {
        list.add(Byte.MIN_VALUE);
        list.add((byte) -1);
        list.add((byte) 0);
        list.add((byte) 1);
        list.add(Byte.MAX_VALUE);

        assertEquals(Byte.MIN_VALUE, list.get(0));
        assertEquals((byte) -1, list.get(1));
        assertEquals((byte) 0, list.get(2));
        assertEquals((byte) 1, list.get(3));
        assertEquals(Byte.MAX_VALUE, list.get(4));

        list.replaceAll(b -> (byte) (b + 1));
        assertEquals((byte) -127, list.get(0));
        assertEquals((byte) 0, list.get(1));
        assertEquals((byte) 1, list.get(2));
        assertEquals((byte) 2, list.get(3));
        assertEquals(Byte.MIN_VALUE, list.get(4));
    }

    @Test
    public void testEmptyListBehaviors() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new ByteList());
        assertEquals(list.hashCode(), new ByteList().hashCode());

        assertFalse(list.contains((byte) 0));
        assertEquals(-1, list.indexOf((byte) 0));
        assertEquals(-1, list.lastIndexOf((byte) 0));
        assertEquals(0, list.occurrencesOf((byte) 0));

        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());
        assertFalse(list.first().isPresent());
        assertFalse(list.last().isPresent());

        assertTrue(list.toArray().length == 0);
        assertTrue(list.boxed().isEmpty());
        assertTrue(list.distinct(0, 0).isEmpty());
        assertFalse(list.iterator().hasNext());

        list.sort();
        list.parallelSort();
        list.reverse();
        list.shuffle();
        list.fill((byte) 0);
        list.deleteRange(0, 0);

        assertTrue(list.isEmpty());
    }
}
