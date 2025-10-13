package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.ShortStream;

@Tag("new-test")
public class ShortList101Test extends TestBase {

    private ShortList list;

    @BeforeEach
    public void setUp() {
        list = new ShortList();
    }

    @Test
    public void testShortBoundaryValues() {
        list.add(Short.MIN_VALUE);
        list.add((short) -1);
        list.add((short) 0);
        list.add((short) 1);
        list.add(Short.MAX_VALUE);

        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals((short) -1, list.get(1));
        assertEquals((short) 0, list.get(2));
        assertEquals((short) 1, list.get(3));
        assertEquals(Short.MAX_VALUE, list.get(4));

        assertEquals(OptionalShort.of(Short.MIN_VALUE), list.min());
        assertEquals(OptionalShort.of(Short.MAX_VALUE), list.max());
    }

    @Test
    public void testShortOverflowInOperations() {
        list.add(Short.MAX_VALUE);
        list.add((short) (Short.MAX_VALUE - 1));
        list.add(Short.MIN_VALUE);
        list.add((short) (Short.MIN_VALUE + 1));

        list.replaceAll(s -> (short) (s + 1));

        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals(Short.MAX_VALUE, list.get(1));
        assertEquals((short) (Short.MIN_VALUE + 1), list.get(2));
        assertEquals((short) (Short.MIN_VALUE + 2), list.get(3));
    }

    @Test
    public void testRangeWithExtremeValues() {
        ShortList list1 = ShortList.range((short) 32760, (short) 32767);
        assertEquals(7, list1.size());
        assertEquals((short) 32760, list1.get(0));
        assertEquals((short) 32766, list1.get(6));

        ShortList list2 = ShortList.range((short) 10, (short) 0, (short) -1);
        assertEquals(10, list2.size());
        assertEquals((short) 10, list2.get(0));
        assertEquals((short) 1, list2.get(9));

        ShortList list3 = ShortList.rangeClosed((short) -1000, (short) 1000, (short) 100);
        assertEquals(21, list3.size());
        assertEquals((short) -1000, list3.get(0));
        assertEquals((short) 1000, list3.get(20));
    }

    @Test
    public void testRangeWithLargeSteps() {
        ShortList list1 = ShortList.range((short) 0, (short) 10, (short) 20);
        assertEquals(1, list1.size());
        assertEquals((short) 0, list1.get(0));

        ShortList list2 = ShortList.range((short) -100, (short) -200, (short) -25);
        assertEquals(4, list2.size());
        assertEquals((short) -100, list2.get(0));
        assertEquals((short) -125, list2.get(1));
        assertEquals((short) -150, list2.get(2));
        assertEquals((short) -175, list2.get(3));
    }

    @Test
    public void testMinMaxMedianWithDuplicates() {
        for (int i = 0; i < 100; i++) {
            list.add((short) 42);
        }
        assertEquals(OptionalShort.of((short) 42), list.min());
        assertEquals(OptionalShort.of((short) 42), list.max());
        assertEquals(OptionalShort.of((short) 42), list.median());

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);
        OptionalShort median = list.median();
        assertTrue(median.isPresent());
        short medianValue = median.get();
        assertTrue(medianValue == 2 || medianValue == 3);
    }

    @Test
    public void testMinMaxMedianWithLargeDataset() {
        Random rand = new Random(42);
        for (int i = 0; i < 10000; i++) {
            list.add((short) rand.nextInt(65536));
        }

        OptionalShort min = list.min();
        OptionalShort max = list.max();
        OptionalShort median = list.median();

        assertTrue(min.isPresent());
        assertTrue(max.isPresent());
        assertTrue(median.isPresent());

        assertTrue(min.get() <= median.get());
        assertTrue(median.get() <= max.get());

        OptionalShort partialMin = list.min(1000, 2000);
        OptionalShort partialMax = list.max(1000, 2000);
        assertTrue(partialMin.isPresent());
        assertTrue(partialMax.isPresent());
        assertTrue(partialMin.get() <= partialMax.get());
    }

    @Test
    public void testBinarySearchWithDuplicates() {
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 3);
        list.add((short) 3);
        list.add((short) 5);
        list.add((short) 7);

        int index = list.binarySearch((short) 3);
        assertTrue(index >= 1 && index <= 3);
        assertEquals((short) 3, list.get(index));

        assertTrue(list.binarySearch((short) 4) < 0);
        assertTrue(list.binarySearch((short) 0) < 0);
        assertTrue(list.binarySearch((short) 10) < 0);
    }

    @Test
    public void testBinarySearchWithNegativeValues() {
        list.add((short) -100);
        list.add((short) -50);
        list.add((short) 0);
        list.add((short) 50);
        list.add((short) 100);

        assertEquals(0, list.binarySearch((short) -100));
        assertEquals(1, list.binarySearch((short) -50));
        assertEquals(2, list.binarySearch((short) 0));
        assertEquals(3, list.binarySearch((short) 50));
        assertEquals(4, list.binarySearch((short) 100));

        assertEquals(2, list.binarySearch(1, 4, (short) 0));
        assertTrue(list.binarySearch(0, 2, (short) 50) < 0);
    }

    @Test
    public void testParallelSortWithVariousDataPatterns() {
        for (int i = 0; i < 1000; i++) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        for (int i = 1000; i >= 0; i--) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        Random rand = new Random(42);
        for (int i = 0; i < 2000; i++) {
            list.add((short) rand.nextInt(65536));
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    @Test
    public void testRemoveIfWithComplexPredicates() {
        for (short i = -100; i <= 100; i++) {
            list.add(i);
        }

        assertTrue(list.removeIf(s -> s % 3 == 0 || s % 5 == 0));

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            assertTrue(value % 3 != 0 && value % 5 != 0);
        }

        assertTrue(list.removeIf(s -> s < 0));
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 0);
        }
    }

    @Test
    public void testBatchRemoveOptimization() {
        for (int i = 0; i < 1000; i++) {
            list.add((short) (i % 100));
        }

        ShortList toRemove = new ShortList();
        for (int i = 0; i < 50; i++) {
            toRemove.add((short) (i * 2));
        }

        int originalSize = list.size();
        list.removeAll(toRemove);
        assertTrue(list.size() < originalSize);

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            assertTrue(value % 2 == 1 || value >= 100);
        }
    }

    @Test
    public void testReplaceAllWithOverflow() {
        list.add((short) 20000);
        list.add((short) 25000);
        list.add((short) -20000);

        list.replaceAll(s -> (short) (s * 2));

        assertEquals((short) -25536, list.get(0));
        assertEquals((short) -15536, list.get(1));
        assertEquals((short) 25536, list.get(2));
    }

    @Test
    public void testReplaceIfWithRangeConditions() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        assertTrue(list.replaceIf(s -> s >= 20 && s <= 40, (short) -1));

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            if (i >= 20 && i <= 40) {
                assertEquals((short) -1, value);
            } else {
                assertEquals((short) i, value);
            }
        }
    }

    @Test
    public void testIntersectionWithLargeMultisets() {
        for (int i = 0; i < 100; i++) {
            list.add((short) (i % 10));
        }

        ShortList other = new ShortList();
        for (int i = 0; i < 50; i++) {
            other.add((short) (i % 5));
        }

        ShortList intersection = list.intersection(other);

        int[] counts = new int[5];
        for (int i = 0; i < intersection.size(); i++) {
            counts[intersection.get(i)]++;
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(10, counts[i]);
        }
    }

    @Test
    public void testSymmetricDifferenceWithComplexCases() {
        list.add((short) 0);
        list.add((short) 0);
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 2);

        ShortList other = ShortList.of((short) 0, (short) 1, (short) 1, (short) 2, (short) 2, (short) 2, (short) 3, (short) 3);

        ShortList symDiff = list.symmetricDifference(other);

        assertEquals(5, symDiff.size());

        int zeros = 0, ones = 0, twos = 0, threes = 0;
        for (int i = 0; i < symDiff.size(); i++) {
            switch (symDiff.get(i)) {
                case 0:
                    zeros++;
                    break;
                case 1:
                    ones++;
                    break;
                case 2:
                    twos++;
                    break;
                case 3:
                    threes++;
                    break;
            }
        }
        assertEquals(1, zeros);
        assertEquals(1, ones);
        assertEquals(1, twos);
        assertEquals(2, threes);
    }

    @Test
    public void testStreamOperations() {
        for (short i = 1; i <= 100; i++) {
            list.add(i);
        }

        ShortStream stream = list.stream();
        int sum = stream.sum();
        assertEquals(5050, sum);

        ShortStream rangeStream = list.stream(10, 20);
        short[] arr = rangeStream.toArray();
        assertEquals(10, arr.length);
        for (int i = 0; i < 10; i++) {
            assertEquals((short) (11 + i), arr[i]);
        }
    }

    @Test
    public void testStreamWithFiltering() {
        for (short i = 0; i < 50; i++) {
            list.add(i);
        }

        ShortStream stream = list.stream();
        short[] evens = stream.filter(s -> s % 2 == 0).toArray();
        assertEquals(25, evens.length);
        for (int i = 0; i < evens.length; i++) {
            assertEquals((short) (i * 2), evens[i]);
        }
    }

    @Test
    public void testToIntListWithSignExtension() {
        list.add((short) -1);
        list.add((short) -32768);
        list.add((short) 32767);
        list.add((short) 0);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(-1, intList.get(0));
        assertEquals(-32768, intList.get(1));
        assertEquals(32767, intList.get(2));
        assertEquals(0, intList.get(3));
    }

    @Test
    public void testForEachWithEarlyTermination() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        final int[] count = { 0 };
        final List<Short> collected = new ArrayList<>();

        list.forEach(s -> {
            collected.add(s);
            count[0]++;
        });

        assertEquals(100, count[0]);
        assertEquals(100, collected.size());

        collected.clear();
        list.forEach(50, 40, s -> collected.add(s));
        assertEquals(10, collected.size());
        assertEquals(Short.valueOf((short) 50), collected.get(0));
        assertEquals(Short.valueOf((short) 41), collected.get(9));
    }

    @Test
    public void testCopyWithNegativeStep() {
        for (short i = 0; i < 20; i++) {
            list.add(i);
        }

        ShortList reversed = list.copy(19, -1, -1);
        assertEquals(20, reversed.size());
        for (int i = 0; i < 20; i++) {
            assertEquals((short) (19 - i), reversed.get(i));
        }

        ShortList everyThirdReverse = list.copy(18, -1, -3);
        assertEquals(7, everyThirdReverse.size());
        assertEquals((short) 18, everyThirdReverse.get(0));
        assertEquals((short) 15, everyThirdReverse.get(1));
        assertEquals((short) 12, everyThirdReverse.get(2));
        assertEquals((short) 9, everyThirdReverse.get(3));
        assertEquals((short) 6, everyThirdReverse.get(4));
        assertEquals((short) 3, everyThirdReverse.get(5));
        assertEquals((short) 0, everyThirdReverse.get(6));
    }

    @Test
    public void testSplitWithPrimeNumberSize() {
        for (short i = 0; i < 23; i++) {
            list.add(i);
        }

        List<ShortList> chunks = list.split(0, 23, 5);
        assertEquals(5, chunks.size());
        assertEquals(5, chunks.get(0).size());
        assertEquals(5, chunks.get(1).size());
        assertEquals(5, chunks.get(2).size());
        assertEquals(5, chunks.get(3).size());
        assertEquals(3, chunks.get(4).size());

        short expected = 0;
        for (ShortList chunk : chunks) {
            for (int i = 0; i < chunk.size(); i++) {
                assertEquals(expected++, chunk.get(i));
            }
        }
    }

    @Test
    public void testArrayMethodReturnsSameReference() {
        list.add((short) 1);
        list.add((short) 2);

        short[] array1 = list.array();
        short[] array2 = list.array();

        assertSame(array1, array2);

        array1[0] = 100;
        assertEquals((short) 100, list.get(0));
    }

    @Test
    public void testCapacityGrowthWithLargeDataset() {
        ShortList smallList = new ShortList(2);
        for (int i = 0; i < 10000; i++) {
            smallList.add((short) i);
        }
        assertEquals(10000, smallList.size());

        for (int i = 0; i < Math.min(100, smallList.size()); i++) {
            assertEquals((short) i, smallList.get(i));
        }
    }

    @Test
    public void testLargeDatasetOperations() {
        final int size = 20000;
        for (int i = 0; i < size; i++) {
            list.add((short) (i % 1000));
        }

        assertEquals(size, list.size());

        assertTrue(list.contains((short) 500));
        assertTrue(list.indexOf((short) 999) >= 0);

        list.sort();
        assertTrue(list.isSorted());

        ShortList distinct = list.distinct(0, list.size());
        assertEquals(1000, distinct.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testEmptyListBehaviors() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new ShortList());
        assertEquals(list.hashCode(), new ShortList().hashCode());

        assertFalse(list.contains((short) 0));
        assertEquals(-1, list.indexOf((short) 0));
        assertEquals(-1, list.lastIndexOf((short) 0));
        assertEquals(0, list.occurrencesOf((short) 0));

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
        list.fill((short) 0);
        list.deleteRange(0, 0);

        assertTrue(list.isEmpty());
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list3 = ShortList.of((short) 3, (short) 2, (short) 1);

        assertEquals(list1, list1);

        assertEquals(list1, list2);
        assertEquals(list2, list1);

        ShortList list4 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(list1, list2);
        assertEquals(list2, list4);
        assertEquals(list1, list4);

        assertEquals(list1.hashCode(), list2.hashCode());

        assertNotEquals(list1, list3);

        assertNotEquals(list1, null);
        assertNotEquals(list1, new ArrayList<>());
    }
}
