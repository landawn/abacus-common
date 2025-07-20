package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
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

/**
 * Comprehensive unit tests for the Iterables utility class.
 * Tests cover all public methods including edge cases and error conditions.
 */
public class Iterables101Test extends TestBase {

    @Nested
    @DisplayName("Min Methods Tests")
    public class MinMethodsTests {

        @Test
        public void testMinChar() {
            // Test with valid array
            char[] array = { 'c', 'a', 'b' };
            OptionalChar result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals('a', result.get());

            // Test with null array
            OptionalChar nullResult = Iterables.min((char[]) null);
            assertFalse(nullResult.isPresent());

            // Test with empty array
            OptionalChar emptyResult = Iterables.min(new char[0]);
            assertFalse(emptyResult.isPresent());
        }

        @Test
        public void testMinByte() {
            byte[] array = { 3, 1, 2 };
            OptionalByte result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals(1, result.get());

            assertFalse(Iterables.min((byte[]) null).isPresent());
            assertFalse(Iterables.min(new byte[0]).isPresent());
        }

        @Test
        public void testMinShort() {
            short[] array = { 30, 10, 20 };
            OptionalShort result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals(10, result.get());

            assertFalse(Iterables.min((short[]) null).isPresent());
            assertFalse(Iterables.min(new short[0]).isPresent());
        }

        @Test
        public void testMinInt() {
            int[] array = { 30, 10, 20 };
            OptionalInt result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals(10, result.get());

            assertFalse(Iterables.min((int[]) null).isPresent());
            assertFalse(Iterables.min(new int[0]).isPresent());
        }

        @Test
        public void testMinLong() {
            long[] array = { 30L, 10L, 20L };
            OptionalLong result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals(10L, result.get());

            assertFalse(Iterables.min((long[]) null).isPresent());
            assertFalse(Iterables.min(new long[0]).isPresent());
        }

        @Test
        public void testMinFloat() {
            float[] array = { 3.0f, 1.0f, 2.0f };
            OptionalFloat result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals(1.0f, result.get());

            assertFalse(Iterables.min((float[]) null).isPresent());
            assertFalse(Iterables.min(new float[0]).isPresent());
        }

        @Test
        public void testMinDouble() {
            double[] array = { 3.0, 1.0, 2.0 };
            OptionalDouble result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals(1.0, result.get());

            assertFalse(Iterables.min((double[]) null).isPresent());
            assertFalse(Iterables.min(new double[0]).isPresent());
        }

        @Test
        public void testMinComparableArray() {
            String[] array = { "c", "a", "b" };
            Nullable<String> result = Iterables.min(array);
            assertTrue(result.isPresent());
            assertEquals("a", result.get());

            // Test with null values (null considered maximum)
            String[] arrayWithNulls = { "c", null, "a" };
            Nullable<String> resultWithNulls = Iterables.min(arrayWithNulls);
            assertTrue(resultWithNulls.isPresent());
            assertEquals("a", resultWithNulls.get());

            assertFalse(Iterables.min((String[]) null).isPresent());
            assertFalse(Iterables.min(new String[0]).isPresent());
        }

        @Test
        public void testMinArrayWithComparator() {
            String[] array = { "aaa", "bb", "c" };
            Comparator<String> lengthComparator = Comparator.comparing(String::length);

            Nullable<String> result = Iterables.min(array, lengthComparator);
            assertTrue(result.isPresent());
            assertEquals("c", result.get());

            assertFalse(Iterables.min((String[]) null, lengthComparator).isPresent());
            assertFalse(Iterables.min(new String[0], lengthComparator).isPresent());
        }

        @Test
        public void testMinIterable() {
            List<String> list = Arrays.asList("c", "a", "b");
            Nullable<String> result = Iterables.min(list);
            assertTrue(result.isPresent());
            assertEquals("a", result.get());

            assertFalse(Iterables.min((Iterable<String>) null).isPresent());
            assertFalse(Iterables.min(Collections.emptyList()).isPresent());
        }

        @Test
        public void testMinIterableWithComparator() {
            List<String> list = Arrays.asList("aaa", "bb", "c");
            Comparator<String> lengthComparator = Comparator.comparing(String::length);

            Nullable<String> result = Iterables.min(list, lengthComparator);
            assertTrue(result.isPresent());
            assertEquals("c", result.get());

            assertFalse(Iterables.min((Iterable<String>) null, lengthComparator).isPresent());
        }

        @Test
        public void testMinIterator() {
            Iterator<String> iterator = Arrays.asList("c", "a", "b").iterator();
            Nullable<String> result = Iterables.min(iterator);
            assertTrue(result.isPresent());
            assertEquals("a", result.get());

            assertFalse(Iterables.min((Iterator<String>) null).isPresent());
            assertFalse(Iterables.min(Collections.emptyIterator()).isPresent());
        }

        @Test
        public void testMinBy() {
            List<String> list = Arrays.asList("aaa", "bb", "c");
            Function<String, Integer> lengthExtractor = String::length;

            Nullable<String> result = Iterables.minBy(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals("c", result.get());

            // Test with array
            String[] array = { "aaa", "bb", "c" };
            Nullable<String> arrayResult = Iterables.minBy(array, lengthExtractor);
            assertTrue(arrayResult.isPresent());
            assertEquals("c", arrayResult.get());
        }

        @Test
        public void testMinInt_valueExtractor() {
            List<String> list = Arrays.asList("aaa", "bb", "c");
            ToIntFunction<String> lengthExtractor = String::length;

            OptionalInt result = Iterables.minInt(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals(1, result.get());

            assertFalse(Iterables.minInt((Iterable<String>) null, lengthExtractor).isPresent());
            assertFalse(Iterables.minInt(Collections.emptyList(), lengthExtractor).isPresent());
        }

        @Test
        public void testMinLong_valueExtractor() {
            List<String> list = Arrays.asList("aaa", "bb", "c");
            ToLongFunction<String> lengthExtractor = s -> (long) s.length();

            OptionalLong result = Iterables.minLong(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals(1L, result.get());
        }

        @Test
        public void testMinDouble_valueExtractor() {
            List<String> list = Arrays.asList("aaa", "bb", "c");
            ToDoubleFunction<String> lengthExtractor = s -> (double) s.length();

            OptionalDouble result = Iterables.minDouble(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals(1.0, result.get());
        }
    }

    @Nested
    @DisplayName("Max Methods Tests")
    public class MaxMethodsTests {

        @Test
        public void testMaxChar() {
            char[] array = { 'c', 'a', 'b' };
            OptionalChar result = Iterables.max(array);
            assertTrue(result.isPresent());
            assertEquals('c', result.get());

            assertFalse(Iterables.max((char[]) null).isPresent());
            assertFalse(Iterables.max(new char[0]).isPresent());
        }

        @Test
        public void testMaxInt() {
            int[] array = { 30, 10, 20 };
            OptionalInt result = Iterables.max(array);
            assertTrue(result.isPresent());
            assertEquals(30, result.get());
        }

        @Test
        public void testMaxComparableArray() {
            String[] array = { "c", "a", "b" };
            Nullable<String> result = Iterables.max(array);
            assertTrue(result.isPresent());
            assertEquals("c", result.get());

            // Test with null values (null considered minimum for max)
            String[] arrayWithNulls = { "c", null, "a" };
            Nullable<String> resultWithNulls = Iterables.max(arrayWithNulls);
            assertTrue(resultWithNulls.isPresent());
            assertEquals("c", resultWithNulls.get());
        }

        @Test
        public void testMaxBy() {
            List<String> list = Arrays.asList("a", "bb", "ccc");
            Function<String, Integer> lengthExtractor = String::length;

            Nullable<String> result = Iterables.maxBy(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals("ccc", result.get());
        }

        @Test
        public void testMaxInt_valueExtractor() {
            List<String> list = Arrays.asList("a", "bb", "ccc");
            ToIntFunction<String> lengthExtractor = String::length;

            OptionalInt result = Iterables.maxInt(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals(3, result.get());
        }
    }

    @Nested
    @DisplayName("MinMax Methods Tests")
    public class MinMaxMethodsTests {

        @Test
        public void testMinMaxArray() {
            String[] array = { "c", "a", "b" };
            Optional<Pair<String, String>> result = Iterables.minMax(array);
            assertTrue(result.isPresent());
            assertEquals("a", result.get().left());
            assertEquals("c", result.get().right());

            assertFalse(Iterables.minMax((String[]) null).isPresent());
            assertFalse(Iterables.minMax(new String[0]).isPresent());
        }

        @Test
        public void testMinMaxIterable() {
            List<String> list = Arrays.asList("c", "a", "b");
            Optional<Pair<String, String>> result = Iterables.minMax(list);
            assertTrue(result.isPresent());
            assertEquals("a", result.get().left());
            assertEquals("c", result.get().right());
        }

        @Test
        public void testMinMaxWithComparator() {
            List<String> list = Arrays.asList("a", "bb", "ccc");
            Comparator<String> lengthComparator = Comparator.comparing(String::length);

            Optional<Pair<String, String>> result = Iterables.minMax(list, lengthComparator);
            assertTrue(result.isPresent());
            assertEquals("a", result.get().left());
            assertEquals("ccc", result.get().right());
        }
    }

    @Nested
    @DisplayName("Median Methods Tests")
    public class MedianMethodsTests {

        @Test
        public void testMedianArray() {
            Integer[] array = { 3, 1, 2 };
            Nullable<Integer> result = Iterables.median(array);
            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(2), result.get());

            assertFalse(Iterables.median((Integer[]) null).isPresent());
            assertFalse(Iterables.median(new Integer[0]).isPresent());
        }

        @Test
        public void testMedianCollection() {
            List<Integer> list = Arrays.asList(3, 1, 2);
            Nullable<Integer> result = Iterables.median(list);
            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(2), result.get());
        }

        @Test
        public void testMedianWithComparator() {
            List<String> list = Arrays.asList("a", "bb", "ccc");
            Comparator<String> lengthComparator = Comparator.comparing(String::length);

            Nullable<String> result = Iterables.median(list, lengthComparator);
            assertTrue(result.isPresent());
            assertEquals("bb", result.get());
        }
    }

    @Nested
    @DisplayName("KthLargest Methods Tests")
    public class KthLargestMethodsTests {

        @Test
        public void testKthLargestArray() {
            Integer[] array = { 3, 1, 4, 2, 5 };

            Nullable<Integer> result = Iterables.kthLargest(array, 2);
            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(4), result.get());

            // Test k larger than array size
            assertFalse(Iterables.kthLargest(array, 10).isPresent());

            // Test null or empty
            assertFalse(Iterables.kthLargest((Integer[]) null, 1).isPresent());
            assertFalse(Iterables.kthLargest(new Integer[0], 1).isPresent());
        }

        @Test
        public void testKthLargestCollection() {
            List<Integer> list = Arrays.asList(3, 1, 4, 2, 5);

            Nullable<Integer> result = Iterables.kthLargest(list, 2);
            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(4), result.get());

            // Test k larger than collection size
            assertFalse(Iterables.kthLargest(list, 10).isPresent());
        }

        @Test
        public void testKthLargestWithComparator() {
            List<String> list = Arrays.asList("a", "bb", "ccc", "dddd");
            Comparator<String> lengthComparator = Comparator.comparing(String::length);

            Nullable<String> result = Iterables.kthLargest(list, 2);
            assertTrue(result.isPresent());
            assertEquals("ccc", result.get());
        }
    }

    @Nested
    @DisplayName("Sum Methods Tests")
    public class SumMethodsTests {

        @Test
        public void testSumInt() {
            List<Integer> list = Arrays.asList(1, 2, 3);
            OptionalInt result = Iterables.sumInt(list);
            assertTrue(result.isPresent());
            assertEquals(6, result.get());

            assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
            assertFalse(Iterables.sumInt(Collections.emptyList()).isPresent());
        }

        @Test
        public void testSumIntWithFunction() {
            List<String> list = Arrays.asList("a", "bb", "ccc");
            ToIntFunction<String> lengthExtractor = String::length;

            OptionalInt result = Iterables.sumInt(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals(6, result.get());
        }

        @Test
        public void testSumIntToLong() {
            List<Integer> list = Arrays.asList(1, 2, 3);
            OptionalLong result = Iterables.sumIntToLong(list);
            assertTrue(result.isPresent());
            assertEquals(6L, result.get());
        }

        @Test
        public void testSumLong() {
            List<Long> list = Arrays.asList(1L, 2L, 3L);
            OptionalLong result = Iterables.sumLong(list);
            assertTrue(result.isPresent());
            assertEquals(6L, result.get());
        }

        @Test
        public void testSumDouble() {
            List<Double> list = Arrays.asList(1.0, 2.0, 3.0);
            OptionalDouble result = Iterables.sumDouble(list);
            assertTrue(result.isPresent());
            assertEquals(6.0, result.get());
        }

        @Test
        public void testSumBigInteger() {
            List<BigInteger> list = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
            Optional<BigInteger> result = Iterables.sumBigInteger(list);
            assertTrue(result.isPresent());
            assertEquals(BigInteger.valueOf(6), result.get());

            assertFalse(Iterables.sumBigInteger((Iterable<BigInteger>) null).isPresent());
            assertFalse(Iterables.sumBigInteger(Collections.emptyList()).isPresent());
        }

        @Test
        public void testSumBigDecimal() {
            List<BigDecimal> list = Arrays.asList(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0));
            Optional<BigDecimal> result = Iterables.sumBigDecimal(list);
            assertTrue(result.isPresent());
            assertEquals(BigDecimal.valueOf(6.0), result.get());
        }
    }

    @Nested
    @DisplayName("Average Methods Tests")
    public class AverageMethodsTests {

        @Test
        public void testAverageInt() {
            Integer[] array = { 1, 2, 3 };
            OptionalDouble result = Iterables.averageInt(array);
            assertTrue(result.isPresent());
            assertEquals(2.0, result.get());

            assertFalse(Iterables.averageInt((Integer[]) null).isPresent());
            assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
        }

        @Test
        public void testAverageIntWithRange() {
            Integer[] array = { 1, 2, 3, 4, 5 };
            OptionalDouble result = Iterables.averageInt(array, 1, 4);
            assertTrue(result.isPresent());
            assertEquals(3.0, result.get()); // (2+3+4)/3

            // Test empty range
            OptionalDouble emptyResult = Iterables.averageInt(array, 2, 2);
            assertFalse(emptyResult.isPresent());
        }

        @Test
        public void testAverageIntWithFunction() {
            List<String> list = Arrays.asList("a", "bb", "ccc");
            ToIntFunction<String> lengthExtractor = String::length;

            OptionalDouble result = Iterables.averageInt(list, lengthExtractor);
            assertTrue(result.isPresent());
            assertEquals(2.0, result.get()); // (1+2+3)/3
        }

        @Test
        public void testAverageLong() {
            Long[] array = { 1L, 2L, 3L };
            OptionalDouble result = Iterables.averageLong(array);
            assertTrue(result.isPresent());
            assertEquals(2.0, result.get());
        }

        @Test
        public void testAverageDouble() {
            Double[] array = { 1.0, 2.0, 3.0 };
            OptionalDouble result = Iterables.averageDouble(array);
            assertTrue(result.isPresent());
            assertEquals(2.0, result.get());
        }

        @Test
        public void testAverageBigInteger() {
            List<BigInteger> list = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(6));
            Optional<BigDecimal> result = Iterables.averageBigInteger(list);
            assertTrue(result.isPresent());
            assertEquals(BigDecimal.valueOf(4), result.get());
        }

        @Test
        public void testAverageBigDecimal() {
            List<BigDecimal> list = Arrays.asList(BigDecimal.valueOf(2.0), BigDecimal.valueOf(4.0), BigDecimal.valueOf(6.0));
            Optional<BigDecimal> result = Iterables.averageBigDecimal(list);
            assertTrue(result.isPresent());
            assertEquals(BigDecimal.valueOf(4.0), result.get());
        }
    }

    @Nested
    @DisplayName("Index Methods Tests")
    public class IndexMethodsTests {

        @Test
        public void testIndexOf() {
            Object[] array = { "a", "b", "c", "b" };
            OptionalInt result = Iterables.indexOf(array, "b");
            assertTrue(result.isPresent());
            assertEquals(1, result.get());

            // Test not found
            OptionalInt notFound = Iterables.indexOf(array, "d");
            assertFalse(notFound.isPresent());

            // Test null array
            assertFalse(Iterables.indexOf((Object[]) null, "b").isPresent());
        }

        @Test
        public void testIndexOfCollection() {
            List<String> list = Arrays.asList("a", "b", "c", "b");
            OptionalInt result = Iterables.indexOf(list, "b");
            assertTrue(result.isPresent());
            assertEquals(1, result.get());

            assertFalse(Iterables.indexOf((Collection<?>) null, "b").isPresent());
        }

        @Test
        public void testLastIndexOf() {
            Object[] array = { "a", "b", "c", "b" };
            OptionalInt result = Iterables.lastIndexOf(array, "b");
            assertTrue(result.isPresent());
            assertEquals(3, result.get());

            // Test not found
            OptionalInt notFound = Iterables.lastIndexOf(array, "d");
            assertFalse(notFound.isPresent());
        }

        @Test
        public void testLastIndexOfCollection() {
            List<String> list = Arrays.asList("a", "b", "c", "b");
            OptionalInt result = Iterables.lastIndexOf(list, "b");
            assertTrue(result.isPresent());
            assertEquals(3, result.get());
        }
    }

    @Nested
    @DisplayName("Find Methods Tests")
    public class FindMethodsTests {

        @Test
        public void testFindFirstOrLast() {
            String[] array = { "a", "bb", "ccc", "dd" };

            Nullable<String> result = Iterables.findFirstOrLast(array, s -> s.length() > 2, // first predicate
                    s -> s.length() == 2 // last predicate
            );
            assertTrue(result.isPresent());
            assertEquals("ccc", result.get()); // First match for length > 2

            // Test when first predicate fails
            Nullable<String> fallbackResult = Iterables.findFirstOrLast(array, s -> s.length() > 10, // no match
                    s -> s.length() == 2 // fallback to last matching this
            );
            assertTrue(fallbackResult.isPresent());
            assertEquals("dd", fallbackResult.get()); // Last match for length == 2
        }

        @Test
        public void testFindFirstOrLastIndex() {
            String[] array = { "a", "bb", "ccc", "dd" };

            OptionalInt result = Iterables.findFirstOrLastIndex(array, s -> s.length() > 2, s -> s.length() == 2);
            assertTrue(result.isPresent());
            assertEquals(2, result.get()); // Index of "ccc"
        }

        @Test
        public void testFindFirstAndLast() {
            String[] array = { "a", "bb", "ccc", "dd", "eee" };

            Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(array, s -> s.length() >= 2);

            assertTrue(result.left().isPresent());
            assertTrue(result.right().isPresent());
            assertEquals("bb", result.left().get()); // First match
            assertEquals("eee", result.right().get()); // Last match
        }

        @Test
        public void testFindFirstAndLastWithDifferentPredicates() {
            String[] array = { "a", "bb", "ccc", "dd", "eee" };

            Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(array, s -> s.length() == 2, // first predicate
                    s -> s.length() == 3 // last predicate
            );

            assertTrue(result.left().isPresent());
            assertTrue(result.right().isPresent());
            assertEquals("bb", result.left().get());
            assertEquals("eee", result.right().get());
        }

        @Test
        public void testFindFirstAndLastIndex() {
            String[] array = { "a", "bb", "ccc", "dd", "eee" };

            Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(array, s -> s.length() >= 2);

            assertTrue(result.left().isPresent());
            assertTrue(result.right().isPresent());
            assertEquals(1, result.left().get()); // Index of "bb"
            assertEquals(4, result.right().get()); // Index of "eee"
        }
    }

    @Nested
    @DisplayName("Fill Methods Tests")
    public class FillMethodsTests {

        @Test
        public void testFillArrayWithSupplier() {
            String[] array = new String[3];
            Supplier<String> supplier = () -> "test";

            Iterables.fill(array, supplier);

            assertArrayEquals(new String[] { "test", "test", "test" }, array);
        }

        @Test
        public void testFillArrayRangeWithSupplier() {
            String[] array = new String[5];
            Arrays.fill(array, "initial");
            Supplier<String> supplier = () -> "test";

            Iterables.fill(array, 1, 4, supplier);

            assertArrayEquals(new String[] { "initial", "test", "test", "test", "initial" }, array);
        }

        @Test
        public void testFillListWithSupplier() {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
            Supplier<String> supplier = () -> "test";

            Iterables.fill(list, supplier);

            assertEquals(Arrays.asList("test", "test", "test"), list);
        }

        @Test
        public void testFillListRangeWithSupplier() {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
            Supplier<String> supplier = () -> "test";

            Iterables.fill(list, 1, 4, supplier);

            assertEquals(Arrays.asList("a", "test", "test", "test", "e"), list);
        }

        @Test
        public void testFillListExtension() {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
            Supplier<String> supplier = () -> "test";

            // Fill beyond current size - should extend the list
            Iterables.fill(list, 0, 5, supplier);

            assertEquals(5, list.size());
            assertEquals(Arrays.asList("test", "test", "test", "test", "test"), list);
        }

        @Test
        public void testFillNullArray() {
            // Should not throw exception with null array
            assertDoesNotThrow(() -> Iterables.fill((String[]) null, () -> "test"));
        }

        @Test
        public void testFillNullList() {
            assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, () -> "test"));
        }
    }

    @Nested
    @DisplayName("Reverse Methods Tests")
    public class ReverseMethodsTests {

        @Test
        public void testReverseList() {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
            List<String> reversed = Iterables.reverse(list);

            assertEquals(Arrays.asList("c", "b", "a"), reversed);

            // Original list should not be modified
            assertEquals(Arrays.asList("a", "b", "c"), list);

            // Changes to reversed list should reflect in original
            reversed.set(0, "changed");
            assertEquals("changed", list.get(2));
        }

        @Test
        public void testReverseRandomAccessList() {
            List<String> list = Arrays.asList("a", "b", "c");
            List<String> reversed = Iterables.reverse(list);

            assertEquals(Arrays.asList("c", "b", "a"), reversed);
            assertTrue(reversed instanceof RandomAccess);
        }

        @Test
        public void testReverseEmptyList() {
            List<String> emptyList = new ArrayList<>();
            List<String> reversed = Iterables.reverse(emptyList);

            assertTrue(reversed.isEmpty());
        }

        @Test
        public void testReverseDoubleReverse() {
            List<String> list = Arrays.asList("a", "b", "c");
            List<String> doubleReversed = Iterables.reverse(Iterables.reverse(list));

            assertEquals(list, doubleReversed);
        }
    }

    @Nested
    @DisplayName("Set Operations Tests")
    public class SetOperationsTests {

        @Test
        public void testUnion() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
            Set<String> set2 = new HashSet<>(Arrays.asList("c", "d", "e"));

            Iterables.SetView<String> union = Iterables.union(set1, set2);

            assertEquals(5, union.size());
            assertTrue(union.contains("a"));
            assertTrue(union.contains("b"));
            assertTrue(union.contains("c"));
            assertTrue(union.contains("d"));
            assertTrue(union.contains("e"));
            assertFalse(union.contains("f"));
        }

        @Test
        public void testUnionWithEmptySets() {
            Set<String> emptySet = Collections.emptySet();
            Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

            Iterables.SetView<String> union1 = Iterables.union(emptySet, nonEmptySet);
            assertEquals(2, union1.size());

            Iterables.SetView<String> union2 = Iterables.union(nonEmptySet, emptySet);
            assertEquals(2, union2.size());

            Iterables.SetView<String> union3 = Iterables.union(emptySet, emptySet);
            assertTrue(union3.isEmpty());
        }

        @Test
        public void testUnionCopyInto() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
            Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));

            Iterables.SetView<String> union = Iterables.union(set1, set2);
            Set<String> result = union.copyInto(new HashSet<>());

            assertEquals(4, result.size());
            assertTrue(result.containsAll(Arrays.asList("a", "b", "c", "d")));
        }

        @Test
        public void testIntersection() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
            Set<String> set2 = new HashSet<>(Arrays.asList("b", "c", "d"));

            Iterables.SetView<String> intersection = Iterables.intersection(set1, set2);

            assertEquals(2, intersection.size());
            assertTrue(intersection.contains("b"));
            assertTrue(intersection.contains("c"));
            assertFalse(intersection.contains("a"));
            assertFalse(intersection.contains("d"));
        }

        @Test
        public void testIntersectionWithEmptySets() {
            Set<String> emptySet = Collections.emptySet();
            Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

            Iterables.SetView<String> intersection1 = Iterables.intersection(nonEmptySet, emptySet);
            assertTrue(intersection1.isEmpty());

            Iterables.SetView<String> intersection2 = Iterables.intersection(emptySet, nonEmptySet);
            assertTrue(intersection2.isEmpty());
        }

        @Test
        public void testIntersectionDisjoint() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
            Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));

            Iterables.SetView<String> intersection = Iterables.intersection(set1, set2);

            assertTrue(intersection.isEmpty());
        }

        @Test
        public void testDifference() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
            Set<String> set2 = new HashSet<>(Arrays.asList("b", "c", "d"));

            Iterables.SetView<String> difference = Iterables.difference(set1, set2);

            assertEquals(1, difference.size());
            assertTrue(difference.contains("a"));
            assertFalse(difference.contains("b"));
            assertFalse(difference.contains("c"));
            assertFalse(difference.contains("d"));
        }

        @Test
        public void testDifferenceWithEmptySets() {
            Set<String> emptySet = Collections.emptySet();
            Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

            Iterables.SetView<String> difference1 = Iterables.difference(emptySet, nonEmptySet);
            assertTrue(difference1.isEmpty());

            Iterables.SetView<String> difference2 = Iterables.difference(nonEmptySet, emptySet);
            assertEquals(2, difference2.size());
        }

        @Test
        public void testSymmetricDifference() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
            Set<String> set2 = new HashSet<>(Arrays.asList("b", "c", "d"));

            Iterables.SetView<String> symDiff = Iterables.symmetricDifference(set1, set2);

            assertEquals(2, symDiff.size());
            assertTrue(symDiff.contains("a"));
            assertTrue(symDiff.contains("d"));
            assertFalse(symDiff.contains("b"));
            assertFalse(symDiff.contains("c"));
        }

        @Test
        public void testSymmetricDifferenceWithEmptySets() {
            Set<String> emptySet = Collections.emptySet();
            Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

            Iterables.SetView<String> symDiff1 = Iterables.symmetricDifference(emptySet, nonEmptySet);
            assertEquals(2, symDiff1.size());

            Iterables.SetView<String> symDiff2 = Iterables.symmetricDifference(nonEmptySet, emptySet);
            assertEquals(2, symDiff2.size());

            Iterables.SetView<String> symDiff3 = Iterables.symmetricDifference(emptySet, emptySet);
            assertTrue(symDiff3.isEmpty());
        }

        @Test
        public void testSymmetricDifferenceIdenticalSets() {
            Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
            Set<String> set2 = new HashSet<>(Arrays.asList("a", "b", "c"));

            Iterables.SetView<String> symDiff = Iterables.symmetricDifference(set1, set2);

            assertTrue(symDiff.isEmpty());
        }
    }

    @Nested
    @DisplayName("NavigableSet SubSet Tests")
    public class NavigableSetSubSetTests {

        @Test
        public void testSubSet() {
            NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
            Range<Integer> range = Range.closed(3, 7);

            NavigableSet<Integer> subset = Iterables.subSet(set, range);

            assertEquals(5, subset.size());
            assertTrue(subset.contains(3));
            assertTrue(subset.contains(7));
            assertFalse(subset.contains(2));
            assertFalse(subset.contains(8));
        }

        @Test
        public void testSubSetOpenRange() {
            NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
            Range<Integer> range = Range.open(2, 5);

            NavigableSet<Integer> subset = Iterables.subSet(set, range);

            assertEquals(2, subset.size());
            assertTrue(subset.contains(3));
            assertTrue(subset.contains(4));
            assertFalse(subset.contains(2));
            assertFalse(subset.contains(5));
        }

        @Test
        public void testSubSetEmptySet() {
            NavigableSet<Integer> emptySet = new TreeSet<>();
            Range<Integer> range = Range.closed(1, 5);

            NavigableSet<Integer> subset = Iterables.subSet(emptySet, range);

            assertTrue(subset.isEmpty());
        }
    }

    @Nested
    @DisplayName("PowerSet Tests")
    public class PowerSetTests {

        @Test
        public void testPowerSet() {
            Set<String> input = new HashSet<>(Arrays.asList("a", "b"));
            Set<Set<String>> powerSet = Iterables.powerSet(input);

            assertEquals(4, powerSet.size()); // 2^2 = 4

            // Should contain: {}, {a}, {b}, {a,b}
            assertTrue(powerSet.contains(Collections.emptySet()));
            assertTrue(powerSet.contains(Collections.singleton("a")));
            assertTrue(powerSet.contains(Collections.singleton("b")));
            assertTrue(powerSet.contains(new HashSet<>(Arrays.asList("a", "b"))));
        }

        @Test
        public void testPowerSetEmpty() {
            Set<String> emptySet = Collections.emptySet();
            Set<Set<String>> powerSet = Iterables.powerSet(emptySet);

            assertEquals(1, powerSet.size());
            assertTrue(powerSet.contains(Collections.emptySet()));
        }

        @Test
        public void testPowerSetSingleElement() {
            Set<String> singleElementSet = Collections.singleton("a");
            Set<Set<String>> powerSet = Iterables.powerSet(singleElementSet);

            assertEquals(2, powerSet.size());
            assertTrue(powerSet.contains(Collections.emptySet()));
            assertTrue(powerSet.contains(Collections.singleton("a")));
        }

        @Test
        public void testPowerSetThreeElements() {
            Set<String> input = new HashSet<>(Arrays.asList("a", "b", "c"));
            Set<Set<String>> powerSet = Iterables.powerSet(input);

            assertEquals(8, powerSet.size()); // 2^3 = 8
        }

        @Test
        public void testPowerSetContains() {
            Set<String> input = new HashSet<>(Arrays.asList("a", "b"));
            Set<Set<String>> powerSet = Iterables.powerSet(input);

            assertTrue(powerSet.contains(new HashSet<>(Arrays.asList("a"))));
            assertFalse(powerSet.contains(new HashSet<>(Arrays.asList("c"))));
            assertFalse(powerSet.contains("not a set"));
        }

        @Test
        public void testPowerSetIteration() {
            Set<String> input = new HashSet<>(Arrays.asList("a", "b"));
            Set<Set<String>> powerSet = Iterables.powerSet(input);

            List<Set<String>> allSubsets = new ArrayList<>();
            for (Set<String> subset : powerSet) {
                allSubsets.add(new HashSet<>(subset));
            }

            assertEquals(4, allSubsets.size());
        }

        @Test
        public void testPowerSetTooLarge() {
            Set<Integer> largeSet = new HashSet<>();
            for (int i = 0; i < 31; i++) {
                largeSet.add(i);
            }

            assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(largeSet));
        }
    }

    @Nested
    @DisplayName("Rollup Tests")
    public class RollupTests {

        @Test
        public void testRollup() {
            List<String> input = Arrays.asList("a", "b", "c");
            List<List<String>> rollup = Iterables.rollup(input);

            assertEquals(4, rollup.size());
            assertEquals(Collections.emptyList(), rollup.get(0));
            assertEquals(Arrays.asList("a"), rollup.get(1));
            assertEquals(Arrays.asList("a", "b"), rollup.get(2));
            assertEquals(Arrays.asList("a", "b", "c"), rollup.get(3));
        }

        @Test
        public void testRollupEmpty() {
            List<String> emptyList = Collections.emptyList();
            List<List<String>> rollup = Iterables.rollup(emptyList);

            assertEquals(1, rollup.size());
            assertEquals(Collections.emptyList(), rollup.get(0));
        }

        @Test
        public void testRollupNull() {
            List<List<String>> rollup = Iterables.rollup(null);

            assertEquals(1, rollup.size());
            assertEquals(Collections.emptyList(), rollup.get(0));
        }

        @Test
        public void testRollupSingleElement() {
            List<String> singleElement = Arrays.asList("a");
            List<List<String>> rollup = Iterables.rollup(singleElement);

            assertEquals(2, rollup.size());
            assertEquals(Collections.emptyList(), rollup.get(0));
            assertEquals(Arrays.asList("a"), rollup.get(1));
        }
    }

    @Nested
    @DisplayName("Permutations Tests")
    public class PermutationsTests {

        @Test
        public void testPermutations() {
            List<String> input = Arrays.asList("a", "b", "c");
            Collection<List<String>> perms = Iterables.permutations(input);

            assertEquals(6, perms.size()); // 3! = 6

            // Convert to set for easier testing
            Set<List<String>> permSet = new HashSet<>(perms);

            assertTrue(permSet.contains(Arrays.asList("a", "b", "c")));
            assertTrue(permSet.contains(Arrays.asList("a", "c", "b")));
            assertTrue(permSet.contains(Arrays.asList("b", "a", "c")));
            assertTrue(permSet.contains(Arrays.asList("b", "c", "a")));
            assertTrue(permSet.contains(Arrays.asList("c", "a", "b")));
            assertTrue(permSet.contains(Arrays.asList("c", "b", "a")));
        }

        @Test
        public void testPermutationsEmpty() {
            List<String> emptyList = Collections.emptyList();
            Collection<List<String>> permutations = Collections2.permutations(emptyList);
            N.println(permutations.size());
            assertFalse(permutations.isEmpty());
            assertEquals(1, permutations.size());
            assertEquals(Collections.emptyList(), permutations.iterator().next());

            Collection<List<String>> perms = Iterables.permutations(emptyList);

            N.println(perms);

            assertFalse(perms.isEmpty());
            assertEquals(1, perms.size());
            assertEquals(Collections.emptyList(), perms.iterator().next());
        }

        @Test
        public void testPermutationsSingleElement() {
            List<String> singleElement = Arrays.asList("a");
            Collection<List<String>> perms = Iterables.permutations(singleElement);

            assertEquals(1, perms.size());
            assertEquals(Arrays.asList("a"), perms.iterator().next());
        }

        @Test
        public void testPermutationsWithDuplicates() {
            List<String> input = Arrays.asList("a", "a", "b");
            Collection<List<String>> perms = Iterables.permutations(input);

            assertEquals(6, perms.size()); // Still 3! even with duplicates in this implementation
        }

        @Test
        public void testPermutationsContains() {
            List<String> input = Arrays.asList("a", "b");
            Collection<List<String>> perms = Iterables.permutations(input);

            assertTrue(perms.contains(Arrays.asList("a", "b")));
            assertTrue(perms.contains(Arrays.asList("b", "a")));
            assertFalse(perms.contains(Arrays.asList("a", "b", "c")));
            assertFalse(perms.contains(Arrays.asList("c", "d")));
        }
    }

    @Nested
    @DisplayName("Ordered Permutations Tests")
    public class OrderedPermutationsTests {

        @Test
        public void testOrderedPermutations() {
            List<String> input = Arrays.asList("b", "a", "c");
            Collection<List<String>> perms = Iterables.orderedPermutations(input);

            assertEquals(6, perms.size());

            // Should be in lexicographical order
            Iterator<List<String>> iter = perms.iterator();
            assertEquals(Arrays.asList("a", "b", "c"), iter.next());
            assertEquals(Arrays.asList("a", "c", "b"), iter.next());
            assertEquals(Arrays.asList("b", "a", "c"), iter.next());
            assertEquals(Arrays.asList("b", "c", "a"), iter.next());
            assertEquals(Arrays.asList("c", "a", "b"), iter.next());
            assertEquals(Arrays.asList("c", "b", "a"), iter.next());
        }

        @Test
        public void testOrderedPermutationsWithComparator() {
            List<String> input = Arrays.asList("aaa", "bb", "c");
            Comparator<String> lengthComparator = Comparator.comparing(String::length);
            Collection<List<String>> perms = Iterables.orderedPermutations(input, lengthComparator);

            assertEquals(6, perms.size());

            // Should be ordered by length
            Iterator<List<String>> iter = perms.iterator();
            List<String> first = iter.next();
            assertEquals("c", first.get(0)); // shortest first
        }

        @Test
        public void testOrderedPermutationsWithDuplicates() {
            List<Integer> input = Arrays.asList(1, 1, 2);
            Collection<List<Integer>> perms = Iterables.orderedPermutations(input);

            assertEquals(3, perms.size()); // 3!/2! = 3 (accounting for duplicates)

            Iterator<List<Integer>> iter = perms.iterator();
            assertEquals(Arrays.asList(1, 1, 2), iter.next());
            assertEquals(Arrays.asList(1, 2, 1), iter.next());
            assertEquals(Arrays.asList(2, 1, 1), iter.next());
        }

        @Test
        public void testOrderedPermutationsEmpty() {
            List<String> emptyList = Collections.emptyList();
            Collection<List<String>> perms = Iterables.orderedPermutations(emptyList);

            assertFalse(perms.isEmpty());
            assertEquals(1, perms.size());
            assertEquals(Collections.emptyList(), perms.iterator().next());
        }
    }

    @Nested
    @DisplayName("Cartesian Product Tests")
    public class CartesianProductTests {

        @Test
        public void testCartesianProductVarargs() {
            List<String> list1 = Arrays.asList("a", "b");
            List<String> list2 = Arrays.asList("1", "2");

            List<List<String>> product = Iterables.cartesianProduct(list1, list2);

            assertEquals(4, product.size());
            assertEquals(Arrays.asList("a", "1"), product.get(0));
            assertEquals(Arrays.asList("a", "2"), product.get(1));
            assertEquals(Arrays.asList("b", "1"), product.get(2));
            assertEquals(Arrays.asList("b", "2"), product.get(3));
        }

        @Test
        public void testCartesianProductCollection() {
            List<Collection<String>> collections = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("1", "2"));

            List<List<String>> product = Iterables.cartesianProduct(collections);

            assertEquals(4, product.size());
            assertEquals(Arrays.asList("a", "1"), product.get(0));
            assertEquals(Arrays.asList("a", "2"), product.get(1));
            assertEquals(Arrays.asList("b", "1"), product.get(2));
            assertEquals(Arrays.asList("b", "2"), product.get(3));
        }

        @Test
        public void testCartesianProductThreeSets() {
            List<String> list1 = Arrays.asList("a", "b");
            List<String> list2 = Arrays.asList("1", "2");
            List<String> list3 = Arrays.asList("x", "y");

            List<List<String>> product = Iterables.cartesianProduct(list1, list2, list3);

            assertEquals(8, product.size()); // 2 * 2 * 2 = 8
            assertEquals(Arrays.asList("a", "1", "x"), product.get(0));
            assertEquals(Arrays.asList("b", "2", "y"), product.get(7));
        }

        @Test
        public void testCartesianProductWithEmptySet() {
            List<String> list1 = Arrays.asList("a", "b");
            List<String> emptyList = Collections.emptyList();

            List<List<String>> product = Iterables.cartesianProduct(list1, emptyList);

            assertTrue(product.isEmpty());
        }

        @Test
        public void testCartesianProductEmpty() {
            List<Collection<String>> emptyCollections = Collections.emptyList();
            List<List<String>> product = Iterables.cartesianProduct(emptyCollections);

            assertEquals(1, product.size());
            assertEquals(Collections.emptyList(), product.get(0));
        }

        @Test
        public void testCartesianProductSingleElement() {
            List<String> singleList = Arrays.asList("a");
            List<List<String>> product = Iterables.cartesianProduct(singleList);

            assertEquals(1, product.size());
            assertEquals(Arrays.asList("a"), product.get(0));
        }

        @Test
        public void testCartesianProductContains() {
            List<String> list1 = Arrays.asList("a", "b");
            List<String> list2 = Arrays.asList("1", "2");

            List<List<String>> product = Iterables.cartesianProduct(list1, list2);

            assertTrue(product.contains(Arrays.asList("a", "1")));
            assertTrue(product.contains(Arrays.asList("b", "2")));
            assertFalse(product.contains(Arrays.asList("c", "1")));
            assertFalse(product.contains(Arrays.asList("a", "1", "extra")));
            assertFalse(product.contains("not a list"));
        }

        @Test
        public void testCartesianProductRandomAccess() {
            List<String> list1 = Arrays.asList("a", "b");
            List<String> list2 = Arrays.asList("1", "2");

            List<List<String>> product = Iterables.cartesianProduct(list1, list2);

            assertTrue(product instanceof RandomAccess);

            // Test random access
            assertEquals(Arrays.asList("b", "1"), product.get(2));
            assertEquals(Arrays.asList("a", "2"), product.get(1));
        }

        @Test
        public void testCartesianProductIndexOutOfBounds() {
            List<String> list1 = Arrays.asList("a");
            List<String> list2 = Arrays.asList("1");

            List<List<String>> product = Iterables.cartesianProduct(list1, list2);

            assertThrows(IndexOutOfBoundsException.class, () -> product.get(1));
            assertThrows(IndexOutOfBoundsException.class, () -> product.get(-1));

            List<List<String>> product2 = Lists.cartesianProduct(list1, list2);

            assertThrows(IndexOutOfBoundsException.class, () -> product2.get(1));
            assertThrows(IndexOutOfBoundsException.class, () -> product2.get(-1));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    public class EdgeCasesTests {

        @Test
        public void testNullInputHandling() {
            // Most methods should handle null gracefully
            assertFalse(Iterables.min((int[]) null).isPresent());
            assertFalse(Iterables.max((String[]) null).isPresent());
            assertFalse(Iterables.median((Integer[]) null).isPresent());
            assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
            assertFalse(Iterables.averageDouble((Double[]) null).isPresent());
        }

        @Test
        public void testEmptyInputHandling() {
            // Empty collections should return empty optionals
            assertFalse(Iterables.min(new int[0]).isPresent());
            assertFalse(Iterables.max(new String[0]).isPresent());
            assertFalse(Iterables.median(Collections.emptyList()).isPresent());
            assertFalse(Iterables.sumLong(Collections.emptyList()).isPresent());
            assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
        }

        @Test
        public void testSingleElementCollections() {
            // Single element collections should work correctly
            assertEquals(5, Iterables.min(new int[] { 5 }).get());
            assertEquals("test", Iterables.max(new String[] { "test" }).get());
            assertEquals(Integer.valueOf(42), Iterables.median(Arrays.asList(42)).get());
            assertEquals(10, Iterables.sumInt(Arrays.asList(10)).get());
            assertEquals(7.5, Iterables.averageDouble(new Double[] { 7.5 }).get());
        }

        @Test
        public void testLargeCollections() {
            // Test with larger collections to ensure performance is reasonable
            List<Integer> largeList = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                largeList.add(i);
            }

            assertEquals(Integer.valueOf(1), Iterables.min(largeList).get());
            assertEquals(Integer.valueOf(1000), Iterables.max(largeList).get());
            assertEquals(500500, Iterables.sumInt(largeList).get()); // sum of 1 to 1000
            assertEquals(500.5, Iterables.averageInt(largeList).get()); // average of 1 to 1000
        }

        @Test
        public void testWithNullElements() {
            // Test collections containing null elements
            List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");

            // Min/Max should handle nulls according to null comparator semantics
            Nullable<String> minResult = Iterables.min(listWithNulls);
            assertTrue(minResult.isPresent());
            assertEquals("a", minResult.get()); // null considered maximum

            Nullable<String> maxResult = Iterables.max(listWithNulls);
            assertTrue(maxResult.isPresent());
            assertEquals("c", maxResult.get()); // null considered minimum for max
        }

        @Test
        public void testRangeValidation() {
            Integer[] array = { 1, 2, 3, 4, 5 };

            // Valid range
            assertTrue(Iterables.averageInt(array, 1, 4).isPresent());

            // Invalid ranges should throw exceptions
            assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, -1, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, 2, 6));
            assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, 3, 2));
        }
    }

    // Tests for firstNonNull(T, T)
    @Test
    public void testFirstNonNull_TwoParams_BothNull() {
        String result = Iterables.firstNonNull(null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_TwoParams_FirstNonNull() {
        String result = Iterables.firstNonNull("first", null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_TwoParams_SecondNonNull() {
        String result = Iterables.firstNonNull(null, "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_TwoParams_BothNonNull() {
        String result = Iterables.firstNonNull("first", "second");
        Assertions.assertEquals("first", result);
    }

    // Tests for firstNonNull(T, T, T)
    @Test
    public void testFirstNonNull_ThreeParams_AllNull() {
        String result = Iterables.firstNonNull(null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_FirstNonNull() {
        String result = Iterables.firstNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_SecondNonNull() {
        String result = Iterables.firstNonNull(null, "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_ThirdNonNull() {
        String result = Iterables.firstNonNull(null, null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_AllNonNull() {
        String result = Iterables.firstNonNull("first", "second", "third");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_SecondAndThirdNonNull() {
        String result = Iterables.firstNonNull(null, "second", "third");
        Assertions.assertEquals("second", result);
    }

    // Tests for firstNonNull(T...)
    @Test
    public void testFirstNonNull_Varargs_NullArray() {
        String result = Iterables.firstNonNull((String[]) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_EmptyArray() {
        String result = Iterables.firstNonNull(new String[0]);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_AllNull() {
        String result = Iterables.firstNonNull(null, null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_FirstNonNull() {
        String result = Iterables.firstNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Varargs_MiddleNonNull() {
        String result = Iterables.firstNonNull(null, null, "middle", null, null);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Varargs_LastNonNull() {
        String result = Iterables.firstNonNull(null, null, null, "last");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testFirstNonNull_Varargs_MultipleNonNull() {
        String result = Iterables.firstNonNull(null, "first", "second", null, "third");
        Assertions.assertEquals("first", result);
    }

    // Tests for firstNonNull(Iterable)
    @Test
    public void testFirstNonNull_Iterable_Null() {
        String result = Iterables.firstNonNull((Iterable<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_Empty() {
        List<String> list = Collections.emptyList();
        String result = Iterables.firstNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_AllNull() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_FirstNonNull() {
        List<String> list = Arrays.asList("first", null, null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, null, "middle", null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Iterable_MultipleNonNull() {
        List<String> list = Arrays.asList(null, "first", "second", null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertEquals("first", result);
    }

    // Tests for firstNonNull(Iterator)
    @Test
    public void testFirstNonNull_Iterator_Null() {
        String result = Iterables.firstNonNull((Iterator<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_Empty() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_AllNull() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_FirstNonNull() {
        Iterator<String> iter = Arrays.asList("first", null, null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "middle", null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Iterator_MultipleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "first", "second", null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertEquals("first", result);
    }

    // Tests for firstNonNullOrDefault(Iterable, T)
    @Test
    public void testFirstNonNullOrDefault_Iterable_NullIterableReturnsDefault() {
        String result = Iterables.firstNonNullOrDefault((Iterable<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_EmptyReturnsDefault() {
        List<String> list = Collections.emptyList();
        String result = Iterables.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_AllNullReturnsDefault() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Iterables.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_FirstNonNull() {
        List<String> list = Arrays.asList("first", null, null);
        String result = Iterables.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, null, "middle", null);
        String result = Iterables.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_NullDefaultThrowsException() {
        List<String> list = Arrays.asList("test");
        Assertions.assertThrows(NullPointerException.class, () -> {
            Iterables.firstNonNullOrDefault(list, null);
        });
    }

    // Tests for firstNonNullOrDefault(Iterator, T)
    @Test
    public void testFirstNonNullOrDefault_Iterator_NullIteratorReturnsDefault() {
        String result = Iterables.firstNonNullOrDefault((Iterator<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_EmptyReturnsDefault() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Iterables.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_AllNullReturnsDefault() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Iterables.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_FirstNonNull() {
        Iterator<String> iter = Arrays.asList("first", null, null).iterator();
        String result = Iterables.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "middle", null).iterator();
        String result = Iterables.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_NullDefaultThrowsException() {
        Iterator<String> iter = Arrays.asList("test").iterator();
        Assertions.assertThrows(NullPointerException.class, () -> {
            Iterables.firstNonNullOrDefault(iter, null);
        });
    }

    // Tests for lastNonNull(T, T)
    @Test
    public void testLastNonNull_TwoParams_BothNull() {
        String result = Iterables.lastNonNull(null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_TwoParams_FirstNonNull() {
        String result = Iterables.lastNonNull("first", null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_TwoParams_SecondNonNull() {
        String result = Iterables.lastNonNull(null, "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_TwoParams_BothNonNull() {
        String result = Iterables.lastNonNull("first", "second");
        Assertions.assertEquals("second", result);
    }

    // Tests for lastNonNull(T, T, T)
    @Test
    public void testLastNonNull_ThreeParams_AllNull() {
        String result = Iterables.lastNonNull(null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_ThreeParams_FirstNonNull() {
        String result = Iterables.lastNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_SecondNonNull() {
        String result = Iterables.lastNonNull(null, "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_ThirdNonNull() {
        String result = Iterables.lastNonNull(null, null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_AllNonNull() {
        String result = Iterables.lastNonNull("first", "second", "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_FirstAndSecondNonNull() {
        String result = Iterables.lastNonNull("first", "second", null);
        Assertions.assertEquals("second", result);
    }

    // Tests for lastNonNull(T...)
    @Test
    public void testLastNonNull_Varargs_NullArray() {
        String result = Iterables.lastNonNull((String[]) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_EmptyArray() {
        String result = Iterables.lastNonNull(new String[0]);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_AllNull() {
        String result = Iterables.lastNonNull(null, null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_FirstNonNull() {
        String result = Iterables.lastNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_Varargs_MiddleNonNull() {
        String result = Iterables.lastNonNull(null, null, "middle", null, null);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Varargs_LastNonNull() {
        String result = Iterables.lastNonNull(null, null, null, "last");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Varargs_MultipleNonNull() {
        String result = Iterables.lastNonNull(null, "first", "second", null, "third");
        Assertions.assertEquals("third", result);
    }

    // Tests for lastNonNull(Iterable)
    @Test
    public void testLastNonNull_Iterable_Null() {
        String result = Iterables.lastNonNull((Iterable<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_Empty() {
        List<String> list = Collections.emptyList();
        String result = Iterables.lastNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_AllNull() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_LastNonNull() {
        List<String> list = Arrays.asList(null, null, "last");
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, "middle", null, null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Iterable_MultipleNonNull() {
        List<String> list = Arrays.asList(null, "first", "second", null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_Iterable_RandomAccessList() {
        // Test with ArrayList (RandomAccess)
        ArrayList<String> list = new ArrayList<>();
        list.add(null);
        list.add("first");
        list.add(null);
        list.add("last");
        list.add(null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterable_NonRandomAccessList() {
        // Test with LinkedList (non-RandomAccess)
        List<String> list = new LinkedList<>();
        list.add(null);
        list.add("first");
        list.add(null);
        list.add("last");
        list.add(null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    // Tests for lastNonNull(Iterator)
    @Test
    public void testLastNonNull_Iterator_Null() {
        String result = Iterables.lastNonNull((Iterator<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_Empty() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_AllNull() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_LastNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "last").iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "middle", null, null).iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Iterator_MultipleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "first", "second", null, "third").iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertEquals("third", result);
    }

    // Tests for lastNonNullOrDefault(Iterable, T)
    @Test
    public void testLastNonNullOrDefault_Iterable_NullIterableReturnsDefault() {
        String result = Iterables.lastNonNullOrDefault((Iterable<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_EmptyReturnsDefault() {
        List<String> list = Collections.emptyList();
        String result = Iterables.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_AllNullReturnsDefault() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Iterables.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_LastNonNull() {
        List<String> list = Arrays.asList(null, null, "last");
        String result = Iterables.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, "middle", null, null);
        String result = Iterables.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_NullDefaultThrowsException() {
        List<String> list = Arrays.asList("test");
        Assertions.assertThrows(NullPointerException.class, () -> {
            Iterables.lastNonNullOrDefault(list, null);
        });
    }

    // Tests for lastNonNullOrDefault(Iterator, T)
    @Test
    public void testLastNonNullOrDefault_Iterator_NullIteratorReturnsDefault() {
        String result = Iterables.lastNonNullOrDefault((Iterator<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_EmptyReturnsDefault() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Iterables.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_AllNullReturnsDefault() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Iterables.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_LastNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "last").iterator();
        String result = Iterables.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "middle", null, null).iterator();
        String result = Iterables.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_NullDefaultThrowsException() {
        Iterator<String> iter = Arrays.asList("test").iterator();
        Assertions.assertThrows(NullPointerException.class, () -> {
            Iterables.lastNonNullOrDefault(iter, null);
        });
    }

    // Additional tests for different types
    @Test
    public void testFirstNonNull_WithIntegers() {
        Integer result = Iterables.firstNonNull(null, 42, 100);
        Assertions.assertEquals(42, result);
    }

    @Test
    public void testLastNonNull_WithIntegers() {
        Integer result = Iterables.lastNonNull(10, null, 42);
        Assertions.assertEquals(42, result);
    }

    @Test
    public void testFirstNonNull_WithCustomObjects() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object result = Iterables.firstNonNull(null, obj1, obj2);
        Assertions.assertSame(obj1, result);
    }

    @Test
    public void testLastNonNull_WithCustomObjects() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object result = Iterables.lastNonNull(obj1, null, obj2);
        Assertions.assertSame(obj2, result);
    }
}
