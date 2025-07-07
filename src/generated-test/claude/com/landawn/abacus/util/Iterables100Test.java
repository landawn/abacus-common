package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class Iterables100Test extends TestBase {

    private List<Integer> intList;
    private List<String> stringList;
    private List<Double> doubleList;
    private Set<Integer> intSet;
    private List<BigInteger> bigIntList;
    private List<BigDecimal> bigDecimalList;

    @BeforeEach
    public void setUp() {
        intList = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        stringList = Arrays.asList("apple", "banana", "cherry", "date");
        doubleList = Arrays.asList(3.14, 2.71, 1.41, 1.73);
        intSet = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        bigIntList = Arrays.asList(BigInteger.valueOf(100), BigInteger.valueOf(200), BigInteger.valueOf(300));
        bigDecimalList = Arrays.asList(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.5), BigDecimal.valueOf(30.5));
    }

    // Tests for min methods with primitive arrays
    @Test
    public void testMinCharArray() {
        char[] arr = { 'd', 'a', 'c', 'b' };
        OptionalChar result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        // Test with null array
        OptionalChar emptyResult = Iterables.min((char[]) null);
        assertFalse(emptyResult.isPresent());

        // Test with empty array
        OptionalChar emptyArrResult = Iterables.min(new char[0]);
        assertFalse(emptyArrResult.isPresent());
    }

    @Test
    public void testMinByteArray() {
        byte[] arr = { 5, 2, 8, 1, 9 };
        OptionalByte result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        // Test with null/empty
        assertFalse(Iterables.min((byte[]) null).isPresent());
        assertFalse(Iterables.min(new byte[0]).isPresent());
    }

    @Test
    public void testMinShortArray() {
        short[] arr = { 100, 50, 200, 25 };
        OptionalShort result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals((short) 25, result.get());

        assertFalse(Iterables.min((short[]) null).isPresent());
        assertFalse(Iterables.min(new short[0]).isPresent());
    }

    @Test
    public void testMinIntArray() {
        int[] arr = { 3, 1, 4, 1, 5, 9 };
        OptionalInt result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.min(new int[0]).isPresent());
    }

    @Test
    public void testMinLongArray() {
        long[] arr = { 100L, 50L, 200L, 25L };
        OptionalLong result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(25L, result.getAsLong());

        assertFalse(Iterables.min((long[]) null).isPresent());
        assertFalse(Iterables.min(new long[0]).isPresent());
    }

    @Test
    public void testMinFloatArray() {
        float[] arr = { 3.14f, 1.41f, 2.71f };
        OptionalFloat result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1.41f, result.get(), 0.001f);

        assertFalse(Iterables.min((float[]) null).isPresent());
        assertFalse(Iterables.min(new float[0]).isPresent());
    }

    @Test
    public void testMinDoubleArray() {
        double[] arr = { 3.14, 1.41, 2.71 };
        OptionalDouble result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1.41, result.getAsDouble(), 0.001);

        assertFalse(Iterables.min((double[]) null).isPresent());
        assertFalse(Iterables.min(new double[0]).isPresent());
    }

    // Tests for min methods with object arrays and collections
    @Test
    public void testMinObjectArray() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        assertTrue(Iterables.min(new Integer[0]).isEmpty());
        assertTrue(Iterables.min((Integer[]) null).isEmpty());
    }

    @Test
    public void testMinObjectArrayWithComparator() {
        String[] arr = { "apple", "banana", "cherry" };
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Nullable<String> result = Iterables.min(arr, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());
    }

    @Test
    public void testMinIterable() {
        Nullable<Integer> result = Iterables.min(intList);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        assertTrue(Iterables.min((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.min(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void testMinIterableWithComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        Nullable<String> result = Iterables.min(stringList, reverseComparator);
        assertTrue(result.isPresent());
        assertEquals("date", result.get());
    }

    @Test
    public void testMinIterator() {
        Nullable<Integer> result = Iterables.min(intList.iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        assertTrue(Iterables.min((Iterator<Integer>) null).isEmpty());
    }

    @Test
    public void testMinBy() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.minBy(arr, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("cat", result.get());

        // Test with collection
        Nullable<String> collResult = Iterables.minBy(Arrays.asList(arr), lengthExtractor);
        assertTrue(collResult.isPresent());
        assertEquals("cat", collResult.get());

        // Test with iterator
        Nullable<String> iterResult = Iterables.minBy(Arrays.asList(arr).iterator(), lengthExtractor);
        assertTrue(iterResult.isPresent());
        assertEquals("cat", iterResult.get());
    }

    @Test
    public void testMinInt() {
        ToIntFunction<String> lengthFunction = String::length;

        String[] arr = { "hi", "hello", "world" };
        OptionalInt result = Iterables.minInt(arr, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        // Test with empty array
        assertFalse(Iterables.minInt(new String[0], lengthFunction).isPresent());

        // Test with collection
        OptionalInt collResult = Iterables.minInt(Arrays.asList(arr), lengthFunction);
        assertTrue(collResult.isPresent());
        assertEquals(2, collResult.getAsInt());

        // Test with iterator
        OptionalInt iterResult = Iterables.minInt(Arrays.asList(arr).iterator(), lengthFunction);
        assertTrue(iterResult.isPresent());
        assertEquals(2, iterResult.getAsInt());
    }

    @Test
    public void testMinLong() {
        ToLongFunction<Integer> toLong = i -> i.longValue();

        Integer[] arr = { 5, 2, 8, 1 };
        OptionalLong result = Iterables.minLong(arr, toLong);
        assertTrue(result.isPresent());
        assertEquals(1L, result.getAsLong());

        assertFalse(Iterables.minLong(new Integer[0], toLong).isPresent());

        OptionalLong collResult = Iterables.minLong(intList, toLong);
        assertTrue(collResult.isPresent());
        assertEquals(1L, collResult.getAsLong());
    }

    @Test
    public void testMinDouble() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();

        Integer[] arr = { 5, 2, 8, 1 };
        OptionalDouble result = Iterables.minDouble(arr, toDouble);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.minDouble(new Integer[0], toDouble).isPresent());
    }

    // Tests for max methods (similar structure to min tests)
    @Test
    public void testMaxIntArray() {
        int[] arr = { 3, 1, 4, 1, 5, 9 };
        OptionalInt result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(9, result.getAsInt());

        assertFalse(Iterables.max((int[]) null).isPresent());
        assertFalse(Iterables.max(new int[0]).isPresent());
    }

    @Test
    public void testMaxObjectArray() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get());
    }

    @Test
    public void testMaxBy() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.maxBy(arr, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("elephant", result.get());
    }

    @Test
    public void testMaxInt() {
        ToIntFunction<String> lengthFunction = String::length;

        String[] arr = { "hi", "hello", "world" };
        OptionalInt result = Iterables.maxInt(arr, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    // Tests for minMax
    @Test
    public void testMinMax() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().left());
        assertEquals(Integer.valueOf(9), result.get().right());

        assertFalse(Iterables.minMax(new Integer[0]).isPresent());
    }

    @Test
    public void testMinMaxWithComparator() {
        String[] arr = { "apple", "banana", "cherry" };
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Optional<Pair<String, String>> result = Iterables.minMax(arr, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("apple", result.get().left());
        assertEquals("banana", result.get().right());
    }

    // Tests for median
    @Test
    public void testMedian() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Nullable<Integer> result = Iterables.median(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.median(new Integer[0]).isEmpty());
    }

    @Test
    public void testMedianCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Nullable<Integer> result = Iterables.median(list);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    // Tests for kthLargest
    @Test
    public void testKthLargest() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.kthLargest(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.kthLargest(arr, 10).isEmpty());
        assertTrue(Iterables.kthLargest(new Integer[0], 1).isEmpty());
    }

    // Tests for sum methods
    @Test
    public void testSumInt() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        OptionalInt result = Iterables.sumInt(numbers);
        assertTrue(result.isPresent());
        assertEquals(15, result.getAsInt());

        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.sumInt(new ArrayList<Integer>()).isPresent());
    }

    @Test
    public void testSumIntWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalInt result = Iterables.sumInt(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(21, result.getAsInt()); // 5 + 6 + 6 + 4
    }

    @Test
    public void testSumIntToLong() {
        List<Integer> largeNumbers = Arrays.asList(Integer.MAX_VALUE, 1);
        OptionalLong result = Iterables.sumIntToLong(largeNumbers);
        assertTrue(result.isPresent());
        assertEquals((long) Integer.MAX_VALUE + 1, result.getAsLong());
    }

    @Test
    public void testSumLong() {
        List<Long> numbers = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        OptionalLong result = Iterables.sumLong(numbers);
        assertTrue(result.isPresent());
        assertEquals(15L, result.getAsLong());
    }

    @Test
    public void testSumDouble() {
        OptionalDouble result = Iterables.sumDouble(doubleList);
        assertTrue(result.isPresent());
        assertEquals(8.99, result.getAsDouble(), 0.001);
    }

    @Test
    public void testSumBigInteger() {
        Optional<BigInteger> result = Iterables.sumBigInteger(bigIntList);
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(600), result.get());

        assertFalse(Iterables.sumBigInteger((Iterable<BigInteger>) null).isPresent());
    }

    @Test
    public void testSumBigDecimal() {
        Optional<BigDecimal> result = Iterables.sumBigDecimal(bigDecimalList);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(61.5), result.get());
    }

    // Tests for average methods
    @Test
    public void testAverageInt() {
        Integer[] arr = { 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageInt(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
    }

    @Test
    public void testAverageIntWithRange() {
        Integer[] arr = { 2, 4, 6, 8, 10 };
        OptionalDouble result = Iterables.averageInt(arr, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(arr, 2, 2).isPresent());
    }

    @Test
    public void testAverageIntWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };
        OptionalDouble result = Iterables.averageInt(arr, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001); // (2 + 5 + 5) / 3
    }

    @Test
    public void testAverageLong() {
        Long[] arr = { 2L, 4L, 6L, 8L };
        OptionalDouble result = Iterables.averageLong(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageDouble() {
        Double[] arr = { 2.5, 3.5, 4.5 };
        OptionalDouble result = Iterables.averageDouble(arr);
        assertTrue(result.isPresent());
        assertEquals(3.5, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageBigInteger() {
        Optional<BigDecimal> result = Iterables.averageBigInteger(bigIntList);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(200), result.get());
    }

    @Test
    public void testAverageBigDecimal() {
        Optional<BigDecimal> result = Iterables.averageBigDecimal(bigDecimalList);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("20.5"), result.get());
    }

    // Tests for indexOf and lastIndexOf
    @Test
    public void testIndexOf() {
        Integer[] arr = { 1, 2, 3, 2, 4 };
        OptionalInt result = Iterables.indexOf(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.indexOf(arr, 5).isPresent());
        assertFalse(Iterables.indexOf((Object[]) null, 1).isPresent());
    }

    @Test
    public void testIndexOfCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 4);
        OptionalInt result = Iterables.indexOf(list, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testLastIndexOf() {
        Integer[] arr = { 1, 2, 3, 2, 4 };
        OptionalInt result = Iterables.lastIndexOf(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    // Tests for find methods
    @Test
    public void testFindFirstOrLast() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        Nullable<Integer> result = Iterables.findFirstOrLast(arr, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());

        // When first predicate doesn't match
        Predicate<Integer> isGreaterThan10 = n -> n > 10;
        result = Iterables.findFirstOrLast(arr, isGreaterThan10, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testFindFirstOrLastIndex() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        OptionalInt result = Iterables.findFirstOrLastIndex(arr, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testFindFirstAndLast() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(arr, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(4), result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndex() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(arr, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(3, result.right().getAsInt());
    }

    // Tests for fill methods
    @Test
    public void testFillArray() {
        String[] arr = new String[5];
        Supplier<String> supplier = () -> "test";

        Iterables.fill(arr, supplier);

        for (String s : arr) {
            assertEquals("test", s);
        }
    }

    @Test
    public void testFillArrayWithRange() {
        String[] arr = new String[5];
        Supplier<String> supplier = () -> "test";

        Iterables.fill(arr, 1, 4, supplier);

        assertNull(arr[0]);
        assertEquals("test", arr[1]);
        assertEquals("test", arr[2]);
        assertEquals("test", arr[3]);
        assertNull(arr[4]);
    }

    @Test
    public void testFillList() {
        List<String> list = new ArrayList<>(Arrays.asList(null, null, null));
        Supplier<String> supplier = () -> "filled";

        Iterables.fill(list, supplier);

        for (String s : list) {
            assertEquals("filled", s);
        }
    }

    @Test
    public void testFillListWithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Supplier<String> supplier = () -> "X";

        Iterables.fill(list, 1, 4, supplier);

        assertEquals("a", list.get(0));
        assertEquals("X", list.get(1));
        assertEquals("X", list.get(2));
        assertEquals("X", list.get(3));
        assertEquals("e", list.get(4));
    }

    // Tests for copy methods
    @Test
    public void testCopyLists() {
        List<String> src = Arrays.asList("a", "b", "c");
        List<String> dest = new ArrayList<>(Arrays.asList("x", "y", "z", "w"));

        Iterables.copy(src, dest);

        assertEquals("a", dest.get(0));
        assertEquals("b", dest.get(1));
        assertEquals("c", dest.get(2));
        assertEquals("w", dest.get(3));
    }

    @Test
    public void testCopyListsWithPositions() {
        List<String> src = Arrays.asList("a", "b", "c", "d", "e");
        List<String> dest = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6"));

        Iterables.copy(src, 1, dest, 2, 3);

        assertEquals("1", dest.get(0));
        assertEquals("2", dest.get(1));
        assertEquals("b", dest.get(2));
        assertEquals("c", dest.get(3));
        assertEquals("d", dest.get(4));
        assertEquals("6", dest.get(5));
    }

    // Tests for reverse
    @Test
    public void testReverse() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> reversed = Iterables.reverse(original);

        assertEquals(5, reversed.size());
        assertEquals(Integer.valueOf(5), reversed.get(0));
        assertEquals(Integer.valueOf(4), reversed.get(1));
        assertEquals(Integer.valueOf(3), reversed.get(2));
        assertEquals(Integer.valueOf(2), reversed.get(3));
        assertEquals(Integer.valueOf(1), reversed.get(4));

        // Test that it's a view
        original = new ArrayList<>(original);
        original.set(0, 10);
        reversed = Iterables.reverse(original);
        assertEquals(Integer.valueOf(10), reversed.get(4));
    }

    // Tests for set operations
    @Test
    public void testUnion() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5));

        Iterables.SetView<Integer> union = Iterables.union(set1, set2);

        assertEquals(5, union.size());
        assertTrue(union.contains(1));
        assertTrue(union.contains(2));
        assertTrue(union.contains(3));
        assertTrue(union.contains(4));
        assertTrue(union.contains(5));

        // Test copyInto
        Set<Integer> target = new HashSet<>();
        union.copyInto(target);
        assertEquals(5, target.size());
    }

    @Test
    public void testIntersection() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Iterables.SetView<Integer> intersection = Iterables.intersection(set1, set2);

        assertEquals(2, intersection.size());
        assertTrue(intersection.contains(3));
        assertTrue(intersection.contains(4));
        assertFalse(intersection.contains(1));
        assertFalse(intersection.contains(5));
    }

    @Test
    public void testDifference() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Iterables.SetView<Integer> difference = Iterables.difference(set1, set2);

        assertEquals(2, difference.size());
        assertTrue(difference.contains(1));
        assertTrue(difference.contains(2));
        assertFalse(difference.contains(3));
        assertFalse(difference.contains(4));
    }

    @Test
    public void testSymmetricDifference() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Iterables.SetView<Integer> symDiff = Iterables.symmetricDifference(set1, set2);

        assertEquals(4, symDiff.size());
        assertTrue(symDiff.contains(1));
        assertTrue(symDiff.contains(2));
        assertTrue(symDiff.contains(5));
        assertTrue(symDiff.contains(6));
        assertFalse(symDiff.contains(3));
        assertFalse(symDiff.contains(4));
    }

    @Test
    public void testSubSet() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Range<Integer> range = Range.closedOpen(3, 8);

        NavigableSet<Integer> subset = Iterables.subSet(set, range);

        assertEquals(5, subset.size());
        assertTrue(subset.contains(3));
        assertTrue(subset.contains(7));
        assertFalse(subset.contains(8));
        assertFalse(subset.contains(2));
    }

    // Tests for powerSet
    @Test
    public void testPowerSet() {
        Set<Integer> input = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Set<Integer>> powerSet = Iterables.powerSet(input);

        assertEquals(8, powerSet.size()); // 2^3 = 8

        // Check some specific subsets
        assertTrue(powerSet.contains(new HashSet<>())); // empty set
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(2))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(3))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1, 2))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1, 3))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(2, 3))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPowerSetEmpty() {
        Set<Integer> input = new HashSet<>();
        Set<Set<Integer>> powerSet = Iterables.powerSet(input);

        assertEquals(1, powerSet.size());
        assertTrue(powerSet.contains(new HashSet<>()));
    }

    @Test
    public void testPowerSetTooLarge() {
        Set<Integer> input = new HashSet<>();
        for (int i = 0; i < 31; i++) {
            input.add(i);
        }

        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(input));
    }

    // Tests for rollup
    @Test
    public void testRollup() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<List<String>> rollup = Iterables.rollup(input);

        assertEquals(4, rollup.size());
        assertEquals(Arrays.asList(), rollup.get(0));
        assertEquals(Arrays.asList("a"), rollup.get(1));
        assertEquals(Arrays.asList("a", "b"), rollup.get(2));
        assertEquals(Arrays.asList("a", "b", "c"), rollup.get(3));
    }

    @Test
    public void testRollupEmpty() {
        List<String> input = new ArrayList<>();
        List<List<String>> rollup = Iterables.rollup(input);

        assertEquals(1, rollup.size());
        assertEquals(Arrays.asList(), rollup.get(0));
    }

    // Tests for permutations
    @Test
    public void testPermutations() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Collection<List<Integer>> perms = Iterables.permutations(input);

        assertEquals(6, perms.size()); // 3! = 6

        // Check that all permutations are different
        Set<List<Integer>> uniquePerms = new HashSet<>(perms);
        assertEquals(6, uniquePerms.size());
    }

    @Test
    public void testPermutationsEmpty() {
        List<Integer> input = new ArrayList<>();
        Collection<List<Integer>> perms = Iterables.permutations(input);

        assertEquals(1, perms.size());
        assertTrue(perms.iterator().next().isEmpty());
    }

    @Test
    public void testPermutationsWithDuplicates() {
        List<Integer> input = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> perms = Iterables.permutations(input);

        // Should still generate 6 permutations, but some will be equal
        assertEquals(6, perms.size());
    }

    // Tests for orderedPermutations
    @Test
    public void testOrderedPermutations() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Collection<List<Integer>> perms = Iterables.orderedPermutations(input);

        List<List<Integer>> permList = new ArrayList<>(perms);
        assertEquals(6, permList.size());

        // First should be in ascending order
        assertEquals(Arrays.asList(1, 2, 3), permList.get(0));
        // Last should be in descending order
        assertEquals(Arrays.asList(3, 2, 1), permList.get(permList.size() - 1));
    }

    @Test
    public void testOrderedPermutationsWithComparator() {
        List<String> input = Arrays.asList("a", "b", "c");
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        Collection<List<String>> perms = Iterables.orderedPermutations(input, reverseOrder);

        List<List<String>> permList = new ArrayList<>(perms);
        assertEquals(6, permList.size());

        // With reverse comparator, first should be c, b, a
        assertEquals(Arrays.asList("c", "b", "a"), permList.get(0));
    }

    @Test
    public void testOrderedPermutationsWithDuplicates() {
        List<Integer> input = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> perms = Iterables.orderedPermutations(input);

        // With duplicates, there should be fewer unique permutations
        Set<List<Integer>> uniquePerms = new HashSet<>(perms);
        assertEquals(3, uniquePerms.size()); // [1,1,2], [1,2,1], [2,1,1]
    }

    // Tests for cartesianProduct
    @Test
    public void testCartesianProduct() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));

        assertEquals(8, product.size()); // 2 * 2 * 2 = 8

        // Check first and last elements
        assertEquals(Arrays.asList(1, 3, 5), product.get(0));
        assertEquals(Arrays.asList(2, 4, 6), product.get(7));
    }

    @Test
    public void testCartesianProductWithEmptyList() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList(5, 6));

        assertEquals(0, product.size());
    }

    @Test
    public void testCartesianProductNoLists() {
        List<List<Integer>> product = Iterables.cartesianProduct(new ArrayList<Collection<Integer>>());

        assertEquals(1, product.size());
        assertTrue(product.get(0).isEmpty());
    }

    @Test
    public void testCartesianProductSingleList() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(Arrays.asList(1, 2, 3)));

        assertEquals(3, product.size());
        assertEquals(Arrays.asList(1), product.get(0));
        assertEquals(Arrays.asList(2), product.get(1));
        assertEquals(Arrays.asList(3), product.get(2));
    }

    // Edge cases and error conditions
    @Test
    public void testNullInputHandling() {
        // Test that methods handle null inputs appropriately
        assertTrue(Iterables.min((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.max((Iterable<Integer>) null).isEmpty());
        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.averageInt((Iterable<Integer>) null).isPresent());
    }

    @Test
    public void testEmptyCollectionHandling() {
        List<Integer> empty = new ArrayList<>();

        assertTrue(Iterables.min(empty).isEmpty());
        assertTrue(Iterables.max(empty).isEmpty());
        assertFalse(Iterables.sumInt(empty).isPresent());
        assertFalse(Iterables.averageInt(empty).isPresent());
        assertTrue(Iterables.median(empty).isEmpty());
    }

    @Test
    public void testRangeValidation() {
        Integer[] arr = { 1, 2, 3, 4, 5 };

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 2, 6));

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
    }

    // Performance-related tests (using larger datasets)
    @Test
    public void testLargeDatasetOperations() {
        // Create a large list
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add(i);
        }

        // Test operations still work correctly
        Nullable<Integer> min = Iterables.min(largeList);
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(0), min.get());

        Nullable<Integer> max = Iterables.max(largeList);
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(9999), max.get());

        OptionalDouble avg = Iterables.averageInt(largeList);
        assertTrue(avg.isPresent());
        assertEquals(4999.5, avg.getAsDouble(), 0.001);
    }

    // Tests for special comparator behavior
    @Test
    public void testNullHandlingInComparators() {
        Integer[] arrWithNulls = { 3, null, 1, 4, null, 2 };

        // Natural ordering should handle nulls
        Nullable<Integer> min = Iterables.min(arrWithNulls);
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());

        Nullable<Integer> max = Iterables.max(arrWithNulls);
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(4), max.get());
    }

    // Tests for number type conversions
    @Test
    public void testNumberTypeConversions() {
        List<Number> mixedNumbers = Arrays.asList((byte) 1, (short) 2, 3, 4L, 5.0f, 6.0);

        OptionalInt sumInt = Iterables.sumInt(mixedNumbers);
        assertTrue(sumInt.isPresent());
        assertEquals(21, sumInt.getAsInt());

        OptionalLong sumLong = Iterables.sumLong(mixedNumbers);
        assertTrue(sumLong.isPresent());
        assertEquals(21L, sumLong.getAsLong());

        OptionalDouble sumDouble = Iterables.sumDouble(mixedNumbers);
        assertTrue(sumDouble.isPresent());
        assertEquals(21.0, sumDouble.getAsDouble(), 0.001);
    }
}
