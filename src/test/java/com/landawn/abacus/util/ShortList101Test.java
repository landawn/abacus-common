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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.ShortStream;

public class ShortList101Test extends TestBase {

    private ShortList list;

    @BeforeEach
    public void setUp() {
        list = new ShortList();
    }

    // ========== Edge Cases for Short Boundaries ==========

    @Test
    public void testShortBoundaryValues() {
        // Test operations at short boundaries
        list.add(Short.MIN_VALUE);
        list.add((short) -1);
        list.add((short) 0);
        list.add((short) 1);
        list.add(Short.MAX_VALUE);

        // Test that values are preserved correctly
        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals((short) -1, list.get(1));
        assertEquals((short) 0, list.get(2));
        assertEquals((short) 1, list.get(3));
        assertEquals(Short.MAX_VALUE, list.get(4));

        // Test min/max with boundary values
        assertEquals(OptionalShort.of(Short.MIN_VALUE), list.min());
        assertEquals(OptionalShort.of(Short.MAX_VALUE), list.max());
    }

    @Test
    public void testShortOverflowInOperations() {
        list.add(Short.MAX_VALUE);
        list.add((short) (Short.MAX_VALUE - 1));
        list.add(Short.MIN_VALUE);
        list.add((short) (Short.MIN_VALUE + 1));

        // Test replaceAll with operator that might overflow
        list.replaceAll(s -> (short) (s + 1));

        // MAX_VALUE + 1 overflows to MIN_VALUE
        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals(Short.MAX_VALUE, list.get(1));
        assertEquals((short) (Short.MIN_VALUE + 1), list.get(2));
        assertEquals((short) (Short.MIN_VALUE + 2), list.get(3));
    }

    // ========== Range Methods with Extreme Values ==========

    @Test
    public void testRangeWithExtremeValues() {
        // Test range near Short boundaries
        ShortList list1 = ShortList.range((short) 32760, (short) 32767);
        assertEquals(7, list1.size());
        assertEquals((short) 32760, list1.get(0));
        assertEquals((short) 32766, list1.get(6));

        // Test range with negative step
        ShortList list2 = ShortList.range((short) 10, (short) 0, (short) -1);
        assertEquals(10, list2.size());
        assertEquals((short) 10, list2.get(0));
        assertEquals((short) 1, list2.get(9));

        // Test rangeClosed with large step
        ShortList list3 = ShortList.rangeClosed((short) -1000, (short) 1000, (short) 100);
        assertEquals(21, list3.size());
        assertEquals((short) -1000, list3.get(0));
        assertEquals((short) 1000, list3.get(20));
    }

    @Test
    public void testRangeWithLargeSteps() {
        // Test range with step larger than range
        ShortList list1 = ShortList.range((short) 0, (short) 10, (short) 20);
        assertEquals(1, list1.size());
        assertEquals((short) 0, list1.get(0));

        // Test negative range with negative step
        ShortList list2 = ShortList.range((short) -100, (short) -200, (short) -25);
        assertEquals(4, list2.size());
        assertEquals((short) -100, list2.get(0));
        assertEquals((short) -125, list2.get(1));
        assertEquals((short) -150, list2.get(2));
        assertEquals((short) -175, list2.get(3));
    }

    // ========== Complex Numeric Operations ==========

    @Test
    public void testMinMaxMedianWithDuplicates() {
        // Test with all same values
        for (int i = 0; i < 100; i++) {
            list.add((short) 42);
        }
        assertEquals(OptionalShort.of((short) 42), list.min());
        assertEquals(OptionalShort.of((short) 42), list.max());
        assertEquals(OptionalShort.of((short) 42), list.median());

        // Test median with even number of elements
        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);
        // Median of [1,2,3,4] should be 2 or 3 (implementation specific)
        OptionalShort median = list.median();
        assertTrue(median.isPresent());
        short medianValue = median.get();
        assertTrue(medianValue == 2 || medianValue == 3);
    }

    @Test
    public void testMinMaxMedianWithLargeDataset() {
        // Create a large dataset with known distribution
        Random rand = new Random(42);
        for (int i = 0; i < 10000; i++) {
            list.add((short) rand.nextInt(65536)); // Full short range
        }

        OptionalShort min = list.min();
        OptionalShort max = list.max();
        OptionalShort median = list.median();

        assertTrue(min.isPresent());
        assertTrue(max.isPresent());
        assertTrue(median.isPresent());

        // Verify min <= median <= max
        assertTrue(min.get() <= median.get());
        assertTrue(median.get() <= max.get());

        // Test partial min/max/median
        OptionalShort partialMin = list.min(1000, 2000);
        OptionalShort partialMax = list.max(1000, 2000);
        assertTrue(partialMin.isPresent());
        assertTrue(partialMax.isPresent());
        assertTrue(partialMin.get() <= partialMax.get());
    }

    // ========== Binary Search Edge Cases ==========

    @Test
    public void testBinarySearchWithDuplicates() {
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 3);
        list.add((short) 3);
        list.add((short) 5);
        list.add((short) 7);

        // Binary search should find one of the duplicate values
        int index = list.binarySearch((short) 3);
        assertTrue(index >= 1 && index <= 3);
        assertEquals((short) 3, list.get(index));

        // Test with value not in list
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

        // Test range binary search
        assertEquals(2, list.binarySearch(1, 4, (short) 0));
        assertTrue(list.binarySearch(0, 2, (short) 50) < 0);
    }

    // ========== Sort and Parallel Sort Performance ==========

    @Test
    public void testParallelSortWithVariousDataPatterns() {
        // Test with already sorted data
        for (int i = 0; i < 1000; i++) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        // Test with reverse sorted data
        list.clear();
        for (int i = 1000; i >= 0; i--) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        // Test with random data
        list.clear();
        Random rand = new Random(42);
        for (int i = 0; i < 2000; i++) {
            list.add((short) rand.nextInt(65536));
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        // Verify correctness
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    // ========== Complex Remove Operations ==========

    @Test
    public void testRemoveIfWithComplexPredicates() {
        // Add values from -100 to 100
        for (short i = -100; i <= 100; i++) {
            list.add(i);
        }

        // Remove all values divisible by 3 or 5
        assertTrue(list.removeIf(s -> s % 3 == 0 || s % 5 == 0));

        // Verify remaining values
        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            assertTrue(value % 3 != 0 && value % 5 != 0);
        }

        // Remove all negative values
        assertTrue(list.removeIf(s -> s < 0));
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 0);
        }
    }

    @Test
    public void testBatchRemoveOptimization() {
        // This tests the optimization that uses Set when appropriate
        for (int i = 0; i < 1000; i++) {
            list.add((short) (i % 100));
        }

        ShortList toRemove = new ShortList();
        for (int i = 0; i < 50; i++) {
            toRemove.add((short) (i * 2)); // Even numbers 0-98
        }

        int originalSize = list.size();
        list.removeAll(toRemove);
        assertTrue(list.size() < originalSize);

        // Verify only odd numbers remain (plus 0 which wasn't in toRemove's range)
        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            assertTrue(value % 2 == 1 || value >= 100);
        }
    }

    // ========== Replace Operations with Edge Cases ==========

    @Test
    public void testReplaceAllWithOverflow() {
        list.add((short) 20000);
        list.add((short) 25000);
        list.add((short) -20000);

        // Test replaceAll with operator that might overflow
        list.replaceAll(s -> (short) (s * 2));

        // 20000 * 2 = 40000, which overflows to -25536
        // 25000 * 2 = 50000, which overflows to -15536
        // -20000 * 2 = -40000, which overflows to 25536
        assertEquals((short) -25536, list.get(0));
        assertEquals((short) -15536, list.get(1));
        assertEquals((short) 25536, list.get(2));
    }

    @Test
    public void testReplaceIfWithRangeConditions() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        // Replace all values in range [20, 40] with -1
        assertTrue(list.replaceIf(s -> s >= 20 && s <= 40, (short) -1));

        // Verify replacements
        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            if (i >= 20 && i <= 40) {
                assertEquals((short) -1, value);
            } else {
                assertEquals((short) i, value);
            }
        }
    }

    // ========== Set Operations with Complex Cases ==========

    @Test
    public void testIntersectionWithLargeMultisets() {
        // Create two lists with many duplicates
        for (int i = 0; i < 100; i++) {
            list.add((short) (i % 10));
        }

        ShortList other = new ShortList();
        for (int i = 0; i < 50; i++) {
            other.add((short) (i % 5));
        }

        ShortList intersection = list.intersection(other);

        // Should contain values 0-4 with appropriate frequencies
        int[] counts = new int[5];
        for (int i = 0; i < intersection.size(); i++) {
            counts[intersection.get(i)]++;
        }

        // Each value 0-4 should appear exactly 10 times (min of occurrences in both lists)
        for (int i = 0; i < 5; i++) {
            assertEquals(10, counts[i]);
        }
    }

    @Test
    public void testSymmetricDifferenceWithComplexCases() {
        // First list: 0, 0, 1, 1, 1, 2, 2, 2, 2
        list.add((short) 0);
        list.add((short) 0);
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 2);

        // Second list: 0, 1, 1, 2, 2, 2, 3, 3
        ShortList other = ShortList.of((short) 0, (short) 1, (short) 1, (short) 2, (short) 2, (short) 2, (short) 3, (short) 3);

        ShortList symDiff = list.symmetricDifference(other);

        // Should have: one 0, one 1, one 2, two 3s
        assertEquals(5, symDiff.size());

        // Count occurrences
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

    // ========== Stream Operations ==========

    @Test
    public void testStreamOperations() {
        for (short i = 1; i <= 100; i++) {
            list.add(i);
        }

        // Test stream sum
        ShortStream stream = list.stream();
        int sum = stream.sum();
        assertEquals(5050, sum); // 1+2+...+100 = 5050

        // Test stream with range
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

        // Stream and filter even numbers
        ShortStream stream = list.stream();
        short[] evens = stream.filter(s -> s % 2 == 0).toArray();
        assertEquals(25, evens.length);
        for (int i = 0; i < evens.length; i++) {
            assertEquals((short) (i * 2), evens[i]);
        }
    }

    // ========== ToIntList Conversion ==========

    @Test
    public void testToIntListWithSignExtension() {
        // Test that negative shorts are properly sign-extended to ints
        list.add((short) -1);
        list.add((short) -32768); // Short.MIN_VALUE
        list.add((short) 32767); // Short.MAX_VALUE
        list.add((short) 0);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(-1, intList.get(0));
        assertEquals(-32768, intList.get(1));
        assertEquals(32767, intList.get(2));
        assertEquals(0, intList.get(3));
    }

    // ========== forEach Edge Cases ==========

    @Test
    public void testForEachWithEarlyTermination() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        // Test forEach with a consumer that tracks state
        final int[] count = { 0 };
        final List<Short> collected = new ArrayList<>();

        list.forEach(s -> {
            collected.add(s);
            count[0]++;
        });

        assertEquals(100, count[0]);
        assertEquals(100, collected.size());

        // Test forEach with range in reverse
        collected.clear();
        list.forEach(50, 40, s -> collected.add(s));
        assertEquals(10, collected.size());
        assertEquals(Short.valueOf((short) 50), collected.get(0));
        assertEquals(Short.valueOf((short) 41), collected.get(9));
    }

    // ========== Copy Operations with Special Steps ==========

    @Test
    public void testCopyWithNegativeStep() {
        for (short i = 0; i < 20; i++) {
            list.add(i);
        }

        // Copy in reverse order
        ShortList reversed = list.copy(19, -1, -1);
        assertEquals(20, reversed.size());
        for (int i = 0; i < 20; i++) {
            assertEquals((short) (19 - i), reversed.get(i));
        }

        // Copy every third element in reverse
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

    // ========== Split with Various Chunk Sizes ==========

    @Test
    public void testSplitWithPrimeNumberSize() {
        // Use a prime number of elements
        for (short i = 0; i < 23; i++) {
            list.add(i);
        }

        // Split into chunks of size 5
        List<ShortList> chunks = list.split(0, 23, 5);
        assertEquals(5, chunks.size());
        assertEquals(5, chunks.get(0).size());
        assertEquals(5, chunks.get(1).size());
        assertEquals(5, chunks.get(2).size());
        assertEquals(5, chunks.get(3).size());
        assertEquals(3, chunks.get(4).size());

        // Verify continuity
        short expected = 0;
        for (ShortList chunk : chunks) {
            for (int i = 0; i < chunk.size(); i++) {
                assertEquals(expected++, chunk.get(i));
            }
        }
    }

    // ========== Memory and Reference Tests ==========

    @Test
    public void testArrayMethodReturnsSameReference() {
        list.add((short) 1);
        list.add((short) 2);

        short[] array1 = list.array();
        short[] array2 = list.array();

        // Should return the same array reference
        assertSame(array1, array2);

        // Modifications through array should affect list
        array1[0] = 100;
        assertEquals((short) 100, list.get(0));
    }

    @Test
    public void testCapacityGrowthWithLargeDataset() {
        // Test capacity growth with many elements
        ShortList smallList = new ShortList(2);
        for (int i = 0; i < 10000; i++) {
            smallList.add((short) i);
        }
        assertEquals(10000, smallList.size());

        // Verify elements
        for (int i = 0; i < Math.min(100, smallList.size()); i++) {
            assertEquals((short) i, smallList.get(i));
        }
    }

    // ========== Performance with Large Dataset ==========

    @Test
    public void testLargeDatasetOperations() {
        // Create a large list
        final int size = 20000;
        for (int i = 0; i < size; i++) {
            list.add((short) (i % 1000));
        }

        assertEquals(size, list.size());

        // Test various operations
        assertTrue(list.contains((short) 500));
        assertTrue(list.indexOf((short) 999) >= 0);

        // Test sort on large list
        list.sort();
        assertTrue(list.isSorted());

        // Test distinct
        ShortList distinct = list.distinct(0, list.size());
        assertEquals(1000, distinct.size()); // Should have values 0-999

        // Test clear
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ========== Comprehensive Empty List Tests ==========

    @Test
    public void testEmptyListBehaviors() {
        // Verify all operations on empty list
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new ShortList());
        assertEquals(list.hashCode(), new ShortList().hashCode());

        // Test search operations
        assertFalse(list.contains((short) 0));
        assertEquals(-1, list.indexOf((short) 0));
        assertEquals(-1, list.lastIndexOf((short) 0));
        assertEquals(0, list.occurrencesOf((short) 0));

        // Test aggregate operations
        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());
        assertFalse(list.first().isPresent());
        assertFalse(list.last().isPresent());

        // Test operations that return empty
        assertTrue(list.toArray().length == 0);
        assertTrue(list.boxed().isEmpty());
        assertTrue(list.distinct(0, 0).isEmpty());
        assertFalse(list.iterator().hasNext());

        // Test operations that should be no-ops
        list.sort();
        list.parallelSort();
        list.reverse();
        list.shuffle();
        list.fill((short) 0);
        list.deleteRange(0, 0);

        assertTrue(list.isEmpty());
    }

    // ========== Equals and HashCode Contract ==========

    @Test
    public void testEqualsAndHashCodeContract() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list3 = ShortList.of((short) 3, (short) 2, (short) 1);

        // Reflexive
        assertEquals(list1, list1);

        // Symmetric
        assertEquals(list1, list2);
        assertEquals(list2, list1);

        // Transitive
        ShortList list4 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(list1, list2);
        assertEquals(list2, list4);
        assertEquals(list1, list4);

        // Consistent with hashCode
        assertEquals(list1.hashCode(), list2.hashCode());

        // Not equal to different list
        assertNotEquals(list1, list3);

        // Not equal to null or different type
        assertNotEquals(list1, null);
        assertNotEquals(list1, new ArrayList<>());
    }
}
