package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

public class IterablesAverageTest extends IterablesTestSupport {

    @Test
    public void testAverageIntCollectionWithRangeAndFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        List<String> list = Arrays.asList("a", "hi", "hello", "world", "x");
        OptionalDouble result = Iterables.averageInt(list, 1, 4, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageIntCollectionRangeExtractor() {
        List<String> coll = list("a", "bb", "ccc", "dddd", "eeeee");
        ToIntFunction<String> len = String::length;
        assertEquals(3.0, Iterables.averageInt(coll, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(coll, 0, 3, len).get(), 0.001);
    }

    @Test
    public void testAverageIntArray() {
        Integer[] arr = { 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageInt(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
        assertFalse(Iterables.averageInt((Integer[]) null).isPresent());
    }

    @Test
    public void testAverageIntIterableWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalDouble result = Iterables.averageInt(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(5.25, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testAverageIntArrayExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.averageInt((String[]) null, len).isEmpty());
        assertTrue(Iterables.averageInt(new String[0], len).isEmpty());
        assertEquals(2.0, Iterables.averageInt(new String[] { "a", "bb", "ccc" }, len).get(), 0.001);
    }

    @Test
    public void testAverageIntArrayRangeExtractor() {
        String[] arr = { "a", "bb", "ccc", "dddd", "eeeee" };
        ToIntFunction<String> len = String::length;
        assertEquals(3.0, Iterables.averageInt(arr, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(arr, 0, 3, len).get(), 0.001);
        assertTrue(Iterables.averageInt(arr, 1, 1, len).isEmpty());
    }

    @Test
    public void testAverageIntCollectionRangeNumber() {
        List<Integer> coll = list(1, 2, 3, 4, 5);
        assertEquals(3.0, Iterables.averageInt(coll, 0, 5).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(coll, 0, 3).get(), 0.001);
        assertTrue(Iterables.averageInt(coll, 1, 1).isEmpty());
    }

    @Test
    public void testAverageIntIterableNumber() {
        assertTrue(Iterables.averageInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.averageInt(list()).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list(1, 2, 3)).get(), 0.001);
    }

    @Test
    public void testAverageIntIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.averageInt((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.averageInt(list(), len).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list("a", "bb", "ccc"), len).get(), 0.001);
    }

    @Test
    public void testAverageIntArrayFromTo() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertTrue(Iterables.averageInt(arr, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageInt(arr, 1, 4).orElseThrow(), 0.0);
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
    }

    @Test
    public void testAverageIntArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToIntFunction<TestObject> extractor = TestObject::getId;
        assertTrue(Iterables.averageInt(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageInt(arr, 1, 4, extractor).orElseThrow(), 0.0);
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2, extractor));
    }

    @Test
    public void testAverageIntCollectionFromTo() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(Iterables.averageInt(list, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageInt(list, 1, 4).orElseThrow(), 0.0);
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(list, 0, 6));
    }

    @Test
    public void testAverageIntCollectionFromToWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""),
                new TestObject(5, ""));
        ToIntFunction<TestObject> extractor = TestObject::getId;
        assertTrue(Iterables.averageInt(list, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageInt(list, 1, 4, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageIntIterable() {
        assertTrue(Iterables.averageInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.averageInt(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(3.0, Iterables.averageInt(Arrays.asList(1, 2, 3, 4, 5)).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageIntArrayRangeNumber() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(3.0, Iterables.averageInt(arr, 0, 5).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(arr, 0, 3).get(), 0.001);
        assertEquals(4.0, Iterables.averageInt(arr, 2, 5).get(), 0.001);
        assertTrue(Iterables.averageInt(arr, 1, 1).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
    }

    @Test
    public void testAverageLongCollectionWithRangeAndFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        List<Integer> list = Arrays.asList(1, 2, 4, 6, 8);
        OptionalDouble result = Iterables.averageLong(list, 1, 4, toLong);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageLongArray() {
        Long[] arr = { 2L, 4L, 6L, 8L };
        OptionalDouble result = Iterables.averageLong(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong(new Long[0]).isPresent());
        assertFalse(Iterables.averageLong((Long[]) null).isPresent());
    }

    @Test
    public void testAverageLongIterableWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        OptionalDouble result = Iterables.averageLong(intList, toLong);
        assertTrue(result.isPresent());
        assertEquals(3.875, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong((Iterable<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testAverageLongArrayFromTo() {
        Long[] arr = { 1L, 2L, 3L, 4L, 5L };
        assertTrue(Iterables.averageLong(arr, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageLong(arr, 1, 4).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.averageLong(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageLong(arr, 1, 4, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongCollectionFromTo() {
        List<Long> list = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertTrue(Iterables.averageLong(list, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageLong(list, 1, 4).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongCollectionFromToWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""),
                new TestObject(5, ""));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.averageLong(list, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageLong(list, 1, 4, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongIterable() {
        assertTrue(Iterables.averageLong((Iterable<Long>) null).isEmpty());
        assertTrue(Iterables.averageLong(Collections.<Long> emptyList()).isEmpty());
        assertEquals(3.0, Iterables.averageLong(Arrays.asList(1L, 2L, 3L, 4L, 5L)).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageDoubleCollectionWithRangeAndFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        List<Integer> list = Arrays.asList(1, 2, 4, 6, 8);
        OptionalDouble result = Iterables.averageDouble(list, 1, 4, toDouble);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageDouble_NonListRandomAccess() {
        // Use a non-RandomAccess collection by wrapping the list
        List<Double> list = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        OptionalDouble result = Iterables.averageDouble(list, 1, 4, d -> d);
        assertTrue(result.isPresent());
        assertEquals((2.0 + 3.0 + 4.0) / 3.0, result.getAsDouble(), 1e-10);
    }

    @Test
    public void testAverageDoubleArray() {
        Double[] arr = { 2.5, 3.5, 4.5 };
        OptionalDouble result = Iterables.averageDouble(arr);
        assertTrue(result.isPresent());
        assertEquals(3.5, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble(new Double[0]).isPresent());
        assertFalse(Iterables.averageDouble((Double[]) null).isPresent());
    }

    @Test
    public void testAverageDoubleIterable() {
        OptionalDouble result = Iterables.averageDouble(doubleList);
        assertTrue(result.isPresent());
        assertEquals(2.2475, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble((Iterable<Double>) null).isPresent());
    }

    @Test
    public void testAverageDoubleIterableWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        OptionalDouble result = Iterables.averageDouble(intList, toDouble);
        assertTrue(result.isPresent());
        assertEquals(3.875, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble((Iterable<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testAverageDoubleArrayExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.averageDouble((String[]) null, len).isEmpty());
        assertTrue(Iterables.averageDouble(new String[0], len).isEmpty());
        assertEquals(2.0, Iterables.averageDouble(new String[] { "a", "bb", "ccc" }, len).get(), 0.001);
    }

    @Test
    public void testAverageDoubleIterableExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.averageDouble((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.averageDouble(list(), len).isEmpty());
        assertEquals(2.0, Iterables.averageDouble(list("a", "bb", "ccc"), len).get(), 0.001);
        List<Double> doubles = new ArrayList<>();
        doubles.add(1.0e100);
        doubles.add(1.0);
        doubles.add(-1.0e100);
        assertEquals(0.0 / 3.0, Iterables.averageDouble(doubles, d -> d).get(), 1e-15);

        doubles = new ArrayList<>();
        doubles.add(1.0e10);
        doubles.add(1.0);
        doubles.add(-1.0e10);
        assertEquals(1.0 / 3.0, Iterables.averageDouble(doubles, d -> d).get(), 1e-15);
    }

    @Test
    public void testAverageDoubleCollectionRangeExtractor() {
        List<String> coll = list("a", "bb", "ccc", "dddd", "eeeee");
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertEquals(3.0, Iterables.averageDouble(coll, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageDouble(coll, 0, 3, len).get(), 0.001);
        assertTrue(Iterables.averageDouble(coll, 1, 1, len).isEmpty());

        LinkedList<String> linkedList = new LinkedList<>(coll);
        assertEquals(3.0, Iterables.averageDouble(linkedList, 0, 5, len).get(), 0.001);
        assertEquals(4.0, Iterables.averageDouble(linkedList, 2, 5, len).get(), 0.001);
    }

    // ===================== averageInt / averageLong / averageDouble empty inputs =====================

    @Test
    public void testAverageDoubleArrayFromTo() {
        Double[] arr = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        assertTrue(Iterables.averageDouble(arr, 1, 1).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(arr, 1, 4).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1;
        assertTrue(Iterables.averageDouble(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.1, Iterables.averageDouble(arr, 1, 4, extractor).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleCollectionFromTo() {
        List<Double> list = Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
        assertTrue(Iterables.averageDouble(list, 1, 1).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(list, 1, 4).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleCollectionFromToWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""),
                new TestObject(5, ""));
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1;
        assertTrue(Iterables.averageDouble(list, 1, 1, extractor).isEmpty());
        assertEquals(3.1, Iterables.averageDouble(list, 1, 4, extractor).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageBigInteger() {
        List<BigInteger> list = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(6));
        Optional<BigDecimal> result = Iterables.averageBigInteger(list);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(4), result.get());
    }

    // ===================== averageBigInteger / averageBigDecimal with function =====================

    @Test
    public void testAverageBigIntegerIterable() {
        Optional<BigDecimal> result = Iterables.averageBigInteger(bigIntList);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(200), result.get());

        assertFalse(Iterables.averageBigInteger((Iterable<BigInteger>) null).isPresent());
        assertFalse(Iterables.averageBigInteger(new ArrayList<>()).isPresent());
    }

    @Test
    public void testAverageBigIntegerIterableWithFunction() {
        Function<Integer, BigInteger> toBigInt = i -> BigInteger.valueOf(i.longValue());
        Optional<BigDecimal> result = Iterables.averageBigInteger(intList, toBigInt);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("3.875"), result.get());

        assertFalse(Iterables.averageBigInteger((Iterable<Integer>) null, toBigInt).isPresent());
    }

    @Test
    public void testAverageBigDecimal() {
        List<BigDecimal> list = Arrays.asList(BigDecimal.valueOf(2.0), BigDecimal.valueOf(4.0), BigDecimal.valueOf(6.0));
        Optional<BigDecimal> result = Iterables.averageBigDecimal(list);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(4.0), result.get());
    }

    @Test
    public void testAverageBigDecimalIterable() {
        Optional<BigDecimal> result = Iterables.averageBigDecimal(bigDecimalList);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("20.5"), result.get());

        assertFalse(Iterables.averageBigDecimal((Iterable<BigDecimal>) null).isPresent());
        assertFalse(Iterables.averageBigDecimal(new ArrayList<>()).isPresent());
    }

    @Test
    public void testAverageBigDecimalIterableWithFunction() {
        Function<Integer, BigDecimal> toBigDec = i -> BigDecimal.valueOf(i.longValue());
        Optional<BigDecimal> result = Iterables.averageBigDecimal(intList, toBigDec);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("3.875"), result.get());

        assertFalse(Iterables.averageBigDecimal((Iterable<Integer>) null, toBigDec).isPresent());
    }

    @Test
    public void testAverageInt_SingleUseIterable() {
        Iterable<Integer> single = singleUseIterable(Arrays.asList(1, 2, 3, 4, 5));
        OptionalDouble result = Iterables.averageInt(single);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 1e-9);
    }

    @Test
    public void testAverageInt_SingleUseIterable_WithFunc() {
        Iterable<String> single = singleUseIterable(Arrays.asList("a", "bb", "ccc"));
        OptionalDouble result = Iterables.averageInt(single, String::length);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble(), 1e-9);
    }

    @Test
    public void testAverageLong_SingleUseIterable() {
        Iterable<Long> single = singleUseIterable(Arrays.asList(10L, 20L, 30L));
        OptionalDouble result = Iterables.averageLong(single);
        assertTrue(result.isPresent());
        assertEquals(20.0, result.getAsDouble(), 1e-9);
    }

    @Test
    public void testAverageBigInteger_SingleUseIterable() {
        Iterable<BigInteger> single = singleUseIterable(Arrays.asList(BigInteger.valueOf(10), BigInteger.valueOf(20), BigInteger.valueOf(30)));
        Optional<BigDecimal> result = Iterables.averageBigInteger(single);
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("20").compareTo(result.get()));
    }

    @Test
    public void testAverageBigDecimal_SingleUseIterable() {
        Iterable<BigDecimal> single = singleUseIterable(Arrays.asList(new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("3.5")));
        Optional<BigDecimal> result = Iterables.averageBigDecimal(single);
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("2.5").compareTo(result.get()));
    }

    @Test
    public void testAverageBigInteger_EmptyAndNull() {
        assertFalse(Iterables.averageBigInteger(null).isPresent());
        assertFalse(Iterables.averageBigInteger(Collections.<BigInteger> emptyList()).isPresent());
    }

    @Test
    public void testAverageDoubleFloatWideningExampleValue() {
        // pins the javadoc example: Float -> double conversion goes through Numbers.toDouble
        // (decimal string based), so each element converts to its exact decimal value and the
        // average is the plain double result 2.1999999999999997, not the float-bit-pattern value.
        final OptionalDouble avg = Iterables.averageDouble(Arrays.asList(1.1f, 2.2f, 3.3f));
        assertTrue(avg.isPresent());
        assertEquals(2.1999999999999997d, avg.getAsDouble(), 0.0d);
    }

    @Test
    public void testAverageLongDoesNotOverflowLargeValues() {
        final OptionalDouble avg = Iterables.averageLong(Arrays.asList(Long.MAX_VALUE, Long.MAX_VALUE), v -> v);

        assertTrue(avg.isPresent());
        assertEquals(Long.MAX_VALUE, avg.getAsDouble(), 0.0d);

        final OptionalDouble oppositeExtremes = Iterables.averageLong(Arrays.asList(Long.MIN_VALUE, Long.MAX_VALUE), v -> v);
        assertTrue(oppositeExtremes.isPresent());
        assertEquals(-0.5d, oppositeExtremes.getAsDouble(), 0.0d);
    }

    @Test
    public void testAverageBigInteger_AllNullValues_ReturnsEmpty() {
        // Every "nothing to average" case reports Optional.empty(), so a real average of zero stays distinguishable
        // from the absence of one. (This used to return Optional[0] for the all-null case.)
        assertFalse(Iterables.averageBigInteger(Arrays.asList("a", "b"), s -> null).isPresent());
        assertFalse(Iterables.averageBigDecimal(Arrays.asList("a", "b"), s -> null).isPresent());

        // ... including through the no-extractor overloads
        assertFalse(Iterables.averageBigInteger(Arrays.asList((BigInteger) null, null)).isPresent());
        assertFalse(Iterables.averageBigDecimal(Arrays.asList((BigDecimal) null, null)).isPresent());

        // ... and it agrees with the empty/null inputs it is now indistinguishable from
        assertFalse(Iterables.averageBigInteger(Collections.<BigInteger> emptyList()).isPresent());
        assertFalse(Iterables.averageBigInteger((Iterable<BigInteger>) null).isPresent());
        assertFalse(Iterables.averageBigDecimal(Collections.<BigDecimal> emptyList()).isPresent());
        assertFalse(Iterables.averageBigDecimal((Iterable<BigDecimal>) null).isPresent());

        // a genuine zero average is still present, and still zero
        assertEquals(0, BigDecimal.ZERO.compareTo(Iterables.averageBigInteger(Arrays.asList(BigInteger.ZERO, BigInteger.ZERO)).get()));
        assertEquals(0, BigDecimal.ZERO.compareTo(Iterables.averageBigDecimal(Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO)).get()));

        // a partially-null input averages only the non-null values
        assertEquals(0, new BigDecimal("3").compareTo(Iterables.averageBigInteger(Arrays.asList(BigInteger.valueOf(3), null)).get()));
        assertEquals(0, new BigDecimal("3").compareTo(Iterables.averageBigDecimal(Arrays.asList(new BigDecimal("3"), null)).get()));
    }

    @Test
    public void testAverage_char() {
        // the javadoc examples
        assertEquals(66.0, Iterables.average('A', 'B', 'C').get());
        assertEquals(97.0, Iterables.average('a').get());
        assertFalse(Iterables.average(new char[0]).isPresent());
        assertFalse(Iterables.average((char[]) null).isPresent());

        // a char contributes its UTF-16 code unit value, so it is never negative
        assertEquals(0.0, Iterables.average('\u0000').get());
        assertEquals(65535.0, Iterables.average('\uFFFF').get());
        assertEquals(32767.5, Iterables.average('\u0000', '\uFFFF').get());
    }

    @Test
    public void testAverage_byte() {
        assertEquals(2.0, Iterables.average(new byte[] { 1, 2, 3 }).get());
        assertEquals(-2.0, Iterables.average(new byte[] { -1, -2, -3 }).get());
        assertFalse(Iterables.average(new byte[0]).isPresent());
        assertFalse(Iterables.average((byte[]) null).isPresent());

        assertEquals(Byte.MAX_VALUE, Iterables.average(new byte[] { Byte.MAX_VALUE, Byte.MAX_VALUE }).get());
        assertEquals(-0.5, Iterables.average(new byte[] { Byte.MIN_VALUE, Byte.MAX_VALUE }).get());
    }

    @Test
    public void testAverage_short() {
        assertEquals(20.0, Iterables.average(new short[] { 10, 20, 30 }).get());
        assertFalse(Iterables.average(new short[0]).isPresent());
        assertFalse(Iterables.average((short[]) null).isPresent());

        assertEquals(Short.MAX_VALUE, Iterables.average(new short[] { Short.MAX_VALUE, Short.MAX_VALUE }).get());
        assertEquals(-0.5, Iterables.average(new short[] { Short.MIN_VALUE, Short.MAX_VALUE }).get());
    }

    @Test
    public void testAverage_int() {
        assertEquals(2.5, Iterables.average(1, 2, 3, 4).get());
        assertFalse(Iterables.average(new int[0]).isPresent());
        assertFalse(Iterables.average((int[]) null).isPresent());

        // the sum of the elements exceeds int range; the average must not wrap
        assertEquals(2.147483647E9, Iterables.average(Integer.MAX_VALUE, Integer.MAX_VALUE).get());
        assertEquals(Integer.MIN_VALUE, Iterables.average(Integer.MIN_VALUE, Integer.MIN_VALUE).get());
        assertEquals(-0.5, Iterables.average(Integer.MIN_VALUE, Integer.MAX_VALUE).get());
    }

    @Test
    public void testAverage_long() {
        assertEquals(2.0, Iterables.average(1L, 2L, 3L).get());
        assertFalse(Iterables.average(new long[0]).isPresent());
        assertFalse(Iterables.average((long[]) null).isPresent());

        assertEquals(9.223372036854776E18, Iterables.average(Long.MAX_VALUE, Long.MAX_VALUE).get());
        assertEquals(-9.223372036854776E18, Iterables.average(Long.MIN_VALUE, Long.MIN_VALUE).get());
        assertEquals(-0.5, Iterables.average(Long.MIN_VALUE, Long.MAX_VALUE).get());
    }

    @Test
    public void testAverage_float() {
        assertEquals(2.0, Iterables.average(1.5f, 2.5f).get());
        assertEquals(0.2000000054637591, Iterables.average(0.1f, 0.2f, 0.3f).get());
        assertFalse(Iterables.average(new float[0]).isPresent());
        assertFalse(Iterables.average((float[]) null).isPresent());

        assertTrue(Double.isNaN(Iterables.average(1.0f, Float.NaN).get()));
        assertTrue(Double.isInfinite(Iterables.average(1.0f, Float.POSITIVE_INFINITY).get()));
        assertEquals(0.0, Iterables.average(-0.0f, 0.0f).get());
    }

    @Test
    public void testAverage_double() {
        assertEquals(2.3333333333333335, Iterables.average(1.5, 2.5, 3.0).get());
        assertFalse(Iterables.average(new double[0]).isPresent());
        assertFalse(Iterables.average((double[]) null).isPresent());

        assertTrue(Double.isNaN(Iterables.average(1.0, Double.NaN).get()));
        assertTrue(Double.isInfinite(Iterables.average(1.0, Double.POSITIVE_INFINITY).get()));

        // a finite sum that would overflow must still report a representable average, not Infinity
        final double huge = Iterables.average(Double.MAX_VALUE, Double.MAX_VALUE).get();
        assertEquals(Double.MAX_VALUE, huge);
        assertFalse(Double.isInfinite(huge));
    }

    @Test
    public void testAverage_primitiveFamily_emptyAndNullAreAbsentNotZero() {
        // This is the whole point of the Iterables variants: N.average collapses "no elements" to 0d,
        // which is indistinguishable from a genuine average of zero.
        assertFalse(Iterables.average(new char[0]).isPresent());
        assertFalse(Iterables.average(new byte[0]).isPresent());
        assertFalse(Iterables.average(new short[0]).isPresent());
        assertFalse(Iterables.average(new int[0]).isPresent());
        assertFalse(Iterables.average(new long[0]).isPresent());
        assertFalse(Iterables.average(new float[0]).isPresent());
        assertFalse(Iterables.average(new double[0]).isPresent());

        assertFalse(Iterables.average((char[]) null).isPresent());
        assertFalse(Iterables.average((byte[]) null).isPresent());
        assertFalse(Iterables.average((short[]) null).isPresent());
        assertFalse(Iterables.average((int[]) null).isPresent());
        assertFalse(Iterables.average((long[]) null).isPresent());
        assertFalse(Iterables.average((float[]) null).isPresent());
        assertFalse(Iterables.average((double[]) null).isPresent());

        assertEquals(0.0d, N.average(new int[0]));
        assertEquals(0.0d, N.average((int[]) null));

        // an array whose elements really do average to zero IS present, and equals zero
        assertTrue(Iterables.average(0, 0, 0).isPresent());
        assertEquals(0.0, Iterables.average(0, 0, 0).get());
        assertEquals(0.0, Iterables.average(-5, 5).get());
    }

    @Test
    public void testAverage_primitiveFamily_agreesWithNAverageForNonEmptyInput() {
        final java.util.Random rnd = new java.util.Random(20260828L);

        for (int round = 0; round < 500; round++) {
            final int len = 1 + rnd.nextInt(16);

            final char[] chars = new char[len];
            final byte[] bytes = new byte[len];
            final short[] shorts = new short[len];
            final int[] ints = new int[len];
            final long[] longs = new long[len];
            final float[] floats = new float[len];
            final double[] doubles = new double[len];

            for (int i = 0; i < len; i++) {
                chars[i] = (char) rnd.nextInt(Character.MAX_VALUE + 1);
                bytes[i] = (byte) rnd.nextInt();
                shorts[i] = (short) rnd.nextInt();
                ints[i] = rnd.nextInt();
                longs[i] = rnd.nextLong();
                floats[i] = rnd.nextFloat() * 1000f - 500f;
                doubles[i] = rnd.nextDouble() * 1000d - 500d;
            }

            assertEquals(N.average(chars), Iterables.average(chars).get());
            assertEquals(N.average(bytes), Iterables.average(bytes).get());
            assertEquals(N.average(shorts), Iterables.average(shorts).get());
            assertEquals(N.average(ints), Iterables.average(ints).get());
            assertEquals(N.average(longs), Iterables.average(longs).get());
            assertEquals(N.average(floats), Iterables.average(floats).get());
            assertEquals(N.average(doubles), Iterables.average(doubles).get());
        }
    }

    @Test
    public void testAverage_primitiveFamily_singleElementIsThatElement() {
        assertEquals(7.0, Iterables.average('\u0007').get());
        assertEquals(7.0, Iterables.average(new byte[] { 7 }).get());
        assertEquals(7.0, Iterables.average(new short[] { 7 }).get());
        assertEquals(7.0, Iterables.average(7).get());
        assertEquals(7.0, Iterables.average(7L).get());
        assertEquals(7.5, Iterables.average(7.5f).get());
        assertEquals(7.5, Iterables.average(7.5d).get());
    }

    @Test
    public void testAverageDouble_floatElementsUseDecimalNotWidening() {
        assertEquals(2.1999999999999997, Iterables.averageDouble(Arrays.asList(1.1f, 2.2f, 3.3f)).get());

        // average(float...) widens instead, and therefore does show the binary artefacts
        assertEquals(0.2000000054637591, Iterables.average(0.1f, 0.2f, 0.3f).get());
    }

    @Test
    public void testAverageDouble_floatConversionIsTheSameOnEveryNumberOverload() {
        final Float[] a = { 1.1f, 2.2f, 3.3f };
        final double expected = Iterables.averageDouble(Arrays.asList(a)).get();

        assertEquals(2.1999999999999997, expected);
        assertEquals(expected, Iterables.averageDouble(a).get());
        assertEquals(expected, Iterables.averageDouble(a, 0, 3).get());
        assertEquals(expected, Iterables.averageDouble(Arrays.asList(a), 0, 3).get());

        // the func overloads use the caller's function instead, which is why they carry no such note
        assertEquals(2.200000007947286, Iterables.averageDouble(Arrays.asList(a), f -> f.doubleValue()).get());
        assertEquals(2.200000007947286, Iterables.averageDouble(a, f -> f.doubleValue()).get());
    }
}
