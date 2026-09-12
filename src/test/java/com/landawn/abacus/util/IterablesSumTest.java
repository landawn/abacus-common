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
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

public class IterablesSumTest extends IterablesTestSupport {
    @Test
    public void testSumIntIterable() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        OptionalInt result = Iterables.sumInt(numbers);
        assertTrue(result.isPresent());
        assertEquals(15, result.getAsInt());

        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.sumInt(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumIntIterableWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalInt result = Iterables.sumInt(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(21, result.getAsInt());

        assertFalse(Iterables.sumInt((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testSumInt_SingleElement() {
        OptionalInt result = Iterables.sumInt(Arrays.asList(42));
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testSumIntToLongIterable() {
        List<Integer> largeNumbers = Arrays.asList(Integer.MAX_VALUE, 1);
        OptionalLong result = Iterables.sumIntToLong(largeNumbers);
        assertTrue(result.isPresent());
        assertEquals((long) Integer.MAX_VALUE + 1, result.getAsLong());

        assertFalse(Iterables.sumIntToLong((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.sumIntToLong(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumIntToLongIterableWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalLong result = Iterables.sumIntToLong(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(21L, result.getAsLong());

        assertFalse(Iterables.sumIntToLong((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testSumIntToLongIterableNumber() {
        assertTrue(Iterables.sumIntToLong((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumIntToLong(list()).isEmpty());
        assertEquals(6L, Iterables.sumIntToLong(list(1, 2, 3)).get());
        assertEquals(2L * Integer.MAX_VALUE, Iterables.sumIntToLong(list(Integer.MAX_VALUE, Integer.MAX_VALUE)).get());
    }

    @Test
    public void testSumIntToLongIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.sumIntToLong((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumIntToLong(list(), len).isEmpty());
        assertEquals(6L, Iterables.sumIntToLong(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumIntToLong() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        OptionalLong result = Iterables.sumIntToLong(list);
        assertTrue(result.isPresent());
        assertEquals(6L, result.get());
    }

    @Test
    public void testSumIntToLong_EmptyIterable() {
        assertFalse(Iterables.sumIntToLong(Collections.<Integer> emptyList()).isPresent());
    }

    @Test
    public void testSumLong() {
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        OptionalLong result = Iterables.sumLong(list);
        assertTrue(result.isPresent());
        assertEquals(6L, result.get());
    }

    @Test
    public void testSumLongIterable() {
        List<Long> numbers = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        OptionalLong result = Iterables.sumLong(numbers);
        assertTrue(result.isPresent());
        assertEquals(15L, result.getAsLong());

        assertFalse(Iterables.sumLong((Iterable<Long>) null).isPresent());
        assertFalse(Iterables.sumLong(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumLongIterableWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        OptionalLong result = Iterables.sumLong(intList, toLong);
        assertTrue(result.isPresent());
        assertEquals(31L, result.getAsLong());

        assertFalse(Iterables.sumLong((Iterable<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testSumLongIterableExtractor() {
        ToLongFunction<String> len = s -> (long) s.length();
        assertTrue(Iterables.sumLong((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumLong(list(), len).isEmpty());
        assertEquals(6L, Iterables.sumLong(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumLong_SingleElement() {
        OptionalLong result = Iterables.sumLong(Arrays.asList(42L));
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testSumDouble() {
        List<Double> list = Arrays.asList(1.0, 2.0, 3.0);
        OptionalDouble result = Iterables.sumDouble(list);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.get());
    }

    @Test
    public void testSumDoubleIterable() {
        OptionalDouble result = Iterables.sumDouble(doubleList);
        assertTrue(result.isPresent());
        assertEquals(8.99, result.getAsDouble(), 0.001);

        assertFalse(Iterables.sumDouble((Iterable<Double>) null).isPresent());
        assertFalse(Iterables.sumDouble(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumDoubleIterableWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        OptionalDouble result = Iterables.sumDouble(intList, toDouble);
        assertTrue(result.isPresent());
        assertEquals(31.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.sumDouble((Iterable<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testSumDoubleIterableExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.sumDouble((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumDouble(list(), len).isEmpty());
        assertEquals(6.0, Iterables.sumDouble(list("a", "bb", "ccc"), len).get(), 0.001);
    }

    // ===================== sumInt / sumLong / sumDouble empty iterable =====================

    @Test
    public void testSumDouble_SingleElement() {
        OptionalDouble result = Iterables.sumDouble(Arrays.asList(3.14));
        assertTrue(result.isPresent());
        assertEquals(3.14, result.getAsDouble(), 0.001);
    }

    // ===================== sumBigInteger / sumBigDecimal with function =====================

    @Test
    public void testSumBigIntegerIterable() {
        Optional<BigInteger> result = Iterables.sumBigInteger(bigIntList);
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(600), result.get());

        assertFalse(Iterables.sumBigInteger((Iterable<BigInteger>) null).isPresent());
        assertFalse(Iterables.sumBigInteger(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumBigIntegerIterableWithFunction() {
        Function<Integer, BigInteger> toBigInt = i -> BigInteger.valueOf(i.longValue());
        Optional<BigInteger> result = Iterables.sumBigInteger(intList, toBigInt);
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(31), result.get());

        assertFalse(Iterables.sumBigInteger((Iterable<Integer>) null, toBigInt).isPresent());
    }

    @Test
    public void testSumBigIntegerIterableExtractor() {
        Function<String, BigInteger> lenToBi = s -> BigInteger.valueOf(s.length());
        assertTrue(Iterables.sumBigInteger((Iterable<String>) null, lenToBi).isEmpty());
        assertTrue(Iterables.sumBigInteger(list(), lenToBi).isEmpty());
        assertEquals(new BigInteger("6"), Iterables.sumBigInteger(list("a", "bb", "ccc"), lenToBi).get());
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

    @Test
    public void testSumBigDecimalIterable() {
        Optional<BigDecimal> result = Iterables.sumBigDecimal(bigDecimalList);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(61.5), result.get());

        assertFalse(Iterables.sumBigDecimal((Iterable<BigDecimal>) null).isPresent());
        assertFalse(Iterables.sumBigDecimal(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumBigDecimalIterableWithFunction() {
        Function<Integer, BigDecimal> toBigDec = i -> BigDecimal.valueOf(i.longValue());
        Optional<BigDecimal> result = Iterables.sumBigDecimal(intList, toBigDec);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(31), result.get());

        assertFalse(Iterables.sumBigDecimal((Iterable<Integer>) null, toBigDec).isPresent());
    }

    @Test
    public void testSumBigDecimalIterableExtractor() {
        Function<String, BigDecimal> lenToBd = s -> BigDecimal.valueOf(s.length());
        assertTrue(Iterables.sumBigDecimal((Iterable<String>) null, lenToBd).isEmpty());
        assertTrue(Iterables.sumBigDecimal(list(), lenToBd).isEmpty());
        assertEquals(new BigDecimal("6"), Iterables.sumBigDecimal(list("a", "bb", "ccc"), lenToBd).get());
    }

    @Test
    public void testSumIntThrowsOnOverflowLikeNSumInt() {
        // regression: sumInt accumulated in int and silently wrapped, while N.sumInt throws
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> Iterables.sumInt(Arrays.asList(Integer.MAX_VALUE, 1)));
        assertEquals(3, Iterables.sumInt(Arrays.asList(1, 2)).orElseThrow());
    }

    @Test
    public void testSumDoubleUsesCompensatedSummation() {
        // regression: naive accumulation made sumDouble disagree with averageDouble in this class
        assertEquals(1.0d, Iterables.sumDouble(java.util.Collections.nCopies(10, 0.1d)).orElseThrow(), 0.0d);
    }

    @Test
    public void testSumLong_overflowWrapsSilently() {
        assertEquals(OptionalLong.of(Long.MIN_VALUE), Iterables.sumLong(Arrays.asList(Long.MAX_VALUE, 1L)));
        assertEquals(N.sumLong(Arrays.asList(Long.MAX_VALUE, 1L)), Iterables.sumLong(Arrays.asList(Long.MAX_VALUE, 1L)).get(),
                "Iterables.sumLong must match N.sumLong");
    }

    @Test
    public void testSumInt_overflowThrows() {
        assertThrows(ArithmeticException.class, () -> Iterables.sumInt(Arrays.asList(Integer.MAX_VALUE, 1)));
        assertEquals(OptionalLong.of(2147483648L), Iterables.sumIntToLong(Arrays.asList(Integer.MAX_VALUE, 1)));
    }

    @Test
    public void testSumDouble_floatElementsUseDecimalNotWidening() {
        // documented on sumDouble(Iterable): a Float goes through its shortest decimal representation, so this is
        // 6.6 and NOT the 6.600000023841858 that Number#doubleValue() widening would produce.
        assertEquals(6.6, Iterables.sumDouble(Arrays.asList(1.1f, 2.2f, 3.3f)).get());

        double viaDoubleValue = 0;

        for (final Float f : Arrays.asList(1.1f, 2.2f, 3.3f)) {
            viaDoubleValue += f.doubleValue();
        }

        assertEquals(6.600000023841858, viaDoubleValue);

        // the func overload uses the caller's function, so it does show the widening artefacts
        assertEquals(6.600000023841858, Iterables.sumDouble(Arrays.asList(1.1f, 2.2f, 3.3f), f -> f.doubleValue()).get());
    }

    @Test
    public void testSumDouble_nonFiniteValuesPropagate() {
        assertTrue(Double.isNaN(Iterables.sumDouble(Arrays.asList(1.0, Double.NaN)).get()));
        assertTrue(Double.isNaN(Iterables.sumDouble(Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)).get()));
        assertEquals(Double.POSITIVE_INFINITY, Iterables.sumDouble(Arrays.asList(Double.POSITIVE_INFINITY, 1.0)).get());
    }
}
