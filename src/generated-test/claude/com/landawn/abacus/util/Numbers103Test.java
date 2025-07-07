package com.landawn.abacus.util;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Numbers103Test extends TestBase {

    @Test
    public void testLog2BigInteger() {
        // Test basic cases
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.DOWN));
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.FLOOR));
        
        // Test non-power-of-two
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(10), RoundingMode.DOWN));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(10), RoundingMode.UP));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(10), RoundingMode.CEILING));
        
        // Test HALF_DOWN, HALF_UP, HALF_EVEN
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_DOWN));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_UP));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_EVEN));
        
        // Test large values
        BigInteger large = new BigInteger("1000000000000000000000000000000");
        Assertions.assertTrue(Numbers.log2(large, RoundingMode.DOWN) > 0);
        
        // Test edge cases
        Assertions.assertEquals(0, Numbers.log2(BigInteger.ONE, RoundingMode.UNNECESSARY));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log2(BigInteger.ZERO, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log2(BigInteger.valueOf(-1), RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.log2(BigInteger.valueOf(10), RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testLog10Int() {
        // Test powers of 10
        Assertions.assertEquals(0, Numbers.log10(1, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log10(10, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(1, Numbers.log10(15, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log10(15, RoundingMode.UP));
        Assertions.assertEquals(1, Numbers.log10(15, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.log10(15, RoundingMode.CEILING));
        
        // Test HALF modes
        Assertions.assertEquals(1, Numbers.log10(30, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.log10(32, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.log10(32, RoundingMode.HALF_EVEN));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log10(0, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log10(-1, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.log10(15, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testLog10Long() {
        // Test powers of 10
        Assertions.assertEquals(0, Numbers.log10(1L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.log10(1000L, RoundingMode.UNNECESSARY));
        
        // Test large values
        Assertions.assertEquals(9, Numbers.log10(1_000_000_000L, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(5, Numbers.log10(123456L, RoundingMode.DOWN));
        Assertions.assertEquals(6, Numbers.log10(123456L, RoundingMode.UP));
        
        // Test HALF modes
        Assertions.assertEquals(2, Numbers.log10(316L, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.log10(316L, RoundingMode.HALF_UP));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log10(0L, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.log10(999L, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testLog10Double() {
        // Test basic cases
        Assertions.assertEquals(0.0, Numbers.log10(1.0), 0.0001);
        Assertions.assertEquals(1.0, Numbers.log10(10.0), 0.0001);
        Assertions.assertEquals(2.0, Numbers.log10(100.0), 0.0001);
        
        // Test fractional values
        Assertions.assertEquals(-1.0, Numbers.log10(0.1), 0.0001);
        
        // Test edge cases
        Assertions.assertTrue(Double.isInfinite(Numbers.log10(0.0)));
        Assertions.assertTrue(Double.isNaN(Numbers.log10(-1.0)));
    }

    @Test
    public void testLog10BigInteger() {
        // Test small values that fit in long
        Assertions.assertEquals(3, Numbers.log10(BigInteger.valueOf(1000), RoundingMode.UNNECESSARY));
        
        // Test large values
        BigInteger large = new BigInteger("1000000000000000000000000000000");
        int result = Numbers.log10(large, RoundingMode.DOWN);
        Assertions.assertTrue(result > 20);
        
        // Test rounding modes
        Assertions.assertEquals(2, Numbers.log10(BigInteger.valueOf(150), RoundingMode.DOWN));
        Assertions.assertEquals(3, Numbers.log10(BigInteger.valueOf(150), RoundingMode.UP));
        
        // Test HALF modes
        BigInteger val = BigInteger.valueOf(316);
        Assertions.assertEquals(2, Numbers.log10(val, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.log10(val, RoundingMode.HALF_UP));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log10(BigInteger.ZERO, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.log10(BigInteger.valueOf(999), RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testPowInt() {
        // Test basic cases
        Assertions.assertEquals(1, Numbers.pow(2, 0));
        Assertions.assertEquals(8, Numbers.pow(2, 3));
        Assertions.assertEquals(1024, Numbers.pow(2, 10));
        
        // Test special bases
        Assertions.assertEquals(1, Numbers.pow(0, 0));
        Assertions.assertEquals(0, Numbers.pow(0, 5));
        Assertions.assertEquals(1, Numbers.pow(1, 100));
        Assertions.assertEquals(1, Numbers.pow(-1, 0));
        Assertions.assertEquals(-1, Numbers.pow(-1, 1));
        Assertions.assertEquals(1, Numbers.pow(-1, 2));
        
        // Test negative base -2
        Assertions.assertEquals(4, Numbers.pow(-2, 2));
        Assertions.assertEquals(-8, Numbers.pow(-2, 3));
        
        // Test overflow
        Assertions.assertEquals(0, Numbers.pow(2, 32));
        
        // Test general case
        Assertions.assertEquals(81, Numbers.pow(3, 4));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.pow(2, -1);
        });
    }

    @Test
    public void testPowLong() {
        // Test basic cases
        Assertions.assertEquals(1L, Numbers.pow(2L, 0));
        Assertions.assertEquals(8L, Numbers.pow(2L, 3));
        
        // Test special bases
        Assertions.assertEquals(1L, Numbers.pow(0L, 0));
        Assertions.assertEquals(0L, Numbers.pow(0L, 5));
        Assertions.assertEquals(1L, Numbers.pow(1L, 100));
        Assertions.assertEquals(-1L, Numbers.pow(-1L, 1));
        
        // Test large powers
        Assertions.assertEquals(1L << 30, Numbers.pow(2L, 30));
        
        // Test general case
        Assertions.assertEquals(27L, Numbers.pow(3L, 3));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.pow(2L, -1);
        });
    }

    @Test
    public void testCeilingPowerOfTwo() {
        // Test exact powers of two
        Assertions.assertEquals(1L, Numbers.ceilingPowerOfTwo(1L));
        Assertions.assertEquals(2L, Numbers.ceilingPowerOfTwo(2L));
        Assertions.assertEquals(4L, Numbers.ceilingPowerOfTwo(4L));
        
        // Test non-powers of two
        Assertions.assertEquals(4L, Numbers.ceilingPowerOfTwo(3L));
        Assertions.assertEquals(8L, Numbers.ceilingPowerOfTwo(5L));
        Assertions.assertEquals(16L, Numbers.ceilingPowerOfTwo(9L));
        
        // Test large values
        Assertions.assertEquals(1L << 30, Numbers.ceilingPowerOfTwo((1L << 30) - 1));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.ceilingPowerOfTwo(0L);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.ceilingPowerOfTwo((1L << 62) + 1);
        });
    }

    @Test
    public void testCeilingPowerOfTwoBigInteger() {
        // Test basic cases
        Assertions.assertEquals(BigInteger.valueOf(4), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(3)));
        Assertions.assertEquals(BigInteger.valueOf(8), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(8)));
        
        // Test large values
        BigInteger large = new BigInteger("1000000000000000000000000");
        BigInteger result = Numbers.ceilingPowerOfTwo(large);
        Assertions.assertTrue(result.compareTo(large) >= 0);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.ceilingPowerOfTwo(BigInteger.ZERO);
        });
    }

    @Test
    public void testFloorPowerOfTwo() {
        // Test exact powers of two
        Assertions.assertEquals(1L, Numbers.floorPowerOfTwo(1L));
        Assertions.assertEquals(2L, Numbers.floorPowerOfTwo(2L));
        Assertions.assertEquals(4L, Numbers.floorPowerOfTwo(4L));
        
        // Test non-powers of two
        Assertions.assertEquals(2L, Numbers.floorPowerOfTwo(3L));
        Assertions.assertEquals(4L, Numbers.floorPowerOfTwo(5L));
        Assertions.assertEquals(8L, Numbers.floorPowerOfTwo(15L));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.floorPowerOfTwo(0L);
        });
    }

    @Test
    public void testFloorPowerOfTwoBigInteger() {
        // Test basic cases
        Assertions.assertEquals(BigInteger.valueOf(2), Numbers.floorPowerOfTwo(BigInteger.valueOf(3)));
        Assertions.assertEquals(BigInteger.valueOf(8), Numbers.floorPowerOfTwo(BigInteger.valueOf(8)));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.floorPowerOfTwo(BigInteger.ZERO);
        });
    }

    @Test
    public void testSqrtInt() {
        // Test perfect squares
        Assertions.assertEquals(0, Numbers.sqrt(0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.sqrt(1, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.sqrt(4, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(1, Numbers.sqrt(2, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.sqrt(2, RoundingMode.UP));
        Assertions.assertEquals(1, Numbers.sqrt(2, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.sqrt(2, RoundingMode.CEILING));
        
        // Test HALF modes
        Assertions.assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_EVEN));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.sqrt(-1, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.sqrt(2, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testSqrtLong() {
        // Test perfect squares
        Assertions.assertEquals(0L, Numbers.sqrt(0L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(10L, Numbers.sqrt(100L, RoundingMode.UNNECESSARY));
        
        // Test large values
        Assertions.assertEquals(100000L, Numbers.sqrt(10000000000L, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(3L, Numbers.sqrt(10L, RoundingMode.DOWN));
        Assertions.assertEquals(4L, Numbers.sqrt(10L, RoundingMode.UP));
        
        // Test HALF modes
        Assertions.assertEquals(7L, Numbers.sqrt(50L, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(7L, Numbers.sqrt(50L, RoundingMode.HALF_UP));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.sqrt(-1L, RoundingMode.DOWN);
        });
    }

    @Test
    public void testSqrtBigInteger() {
        // Test perfect squares
        Assertions.assertEquals(BigInteger.ZERO, Numbers.sqrt(BigInteger.ZERO, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.sqrt(BigInteger.valueOf(10000), RoundingMode.UNNECESSARY));
        
        // Test large values
        BigInteger large = new BigInteger("1000000000000000000000000");
        BigInteger largeSq = large.multiply(large);
        Assertions.assertEquals(large, Numbers.sqrt(largeSq, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.DOWN));
        Assertions.assertEquals(BigInteger.valueOf(4), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.UP));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.DOWN);
        });
    }

    @Test
    public void testDivideInt() {
        // Test exact division
        Assertions.assertEquals(2, Numbers.divide(10, 5, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(3, Numbers.divide(10, 3, RoundingMode.DOWN));
        Assertions.assertEquals(4, Numbers.divide(10, 3, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.divide(10, 3, RoundingMode.FLOOR));
        Assertions.assertEquals(4, Numbers.divide(10, 3, RoundingMode.CEILING));
        
        // Test negative values
        Assertions.assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.DOWN));
        Assertions.assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.UP));
        Assertions.assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.FLOOR));
        Assertions.assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.CEILING));
        
        // Test HALF modes
        Assertions.assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(3, Numbers.divide(5, 2, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_EVEN));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(10, 0, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(10, 3, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testDivideLong() {
        // Test exact division
        Assertions.assertEquals(2L, Numbers.divide(10L, 5L, RoundingMode.UNNECESSARY));
        
        // Test rounding modes
        Assertions.assertEquals(3L, Numbers.divide(10L, 3L, RoundingMode.DOWN));
        Assertions.assertEquals(4L, Numbers.divide(10L, 3L, RoundingMode.UP));
        
        // Test large values
        Assertions.assertEquals(1000000L, Numbers.divide(1000000000000L, 1000000L, RoundingMode.UNNECESSARY));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(10L, 0L, RoundingMode.DOWN);
        });
    }

    @Test
    public void testDivideBigInteger() {
        // Test exact division
        BigInteger result = Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(5), RoundingMode.UNNECESSARY);
        Assertions.assertEquals(BigInteger.valueOf(2), result);
        
        // Test rounding modes
        result = Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.DOWN);
        Assertions.assertEquals(BigInteger.valueOf(3), result);
        
        result = Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.UP);
        Assertions.assertEquals(BigInteger.valueOf(4), result);
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(BigInteger.valueOf(10), BigInteger.ZERO, RoundingMode.DOWN);
        });
    }

    @Test
    public void testModInt() {
        // Test basic cases
        Assertions.assertEquals(3, Numbers.mod(7, 4));
        Assertions.assertEquals(1, Numbers.mod(-7, 4));
        Assertions.assertEquals(3, Numbers.mod(-1, 4));
        Assertions.assertEquals(0, Numbers.mod(-8, 4));
        Assertions.assertEquals(0, Numbers.mod(8, 4));
        
        // Test edge cases
        Assertions.assertEquals(0, Numbers.mod(0, 5));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10, 0);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10, -1);
        });
    }

    @Test
    public void testModLongInt() {
        // Test basic cases
        Assertions.assertEquals(3, Numbers.mod(7L, 4));
        Assertions.assertEquals(1, Numbers.mod(-7L, 4));
        
        // Test large values
        Assertions.assertEquals(123456 % 1000, Numbers.mod(123456L, 1000));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10L, 0);
        });
    }

    @Test
    public void testModLong() {
        // Test basic cases
        Assertions.assertEquals(3L, Numbers.mod(7L, 4L));
        Assertions.assertEquals(1L, Numbers.mod(-7L, 4L));
        Assertions.assertEquals(3L, Numbers.mod(-1L, 4L));
        Assertions.assertEquals(0L, Numbers.mod(-8L, 4L));
        Assertions.assertEquals(0L, Numbers.mod(8L, 4L));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10L, 0L);
        });
    }

    @Test
    public void testGcdInt() {
        // Test basic cases
        Assertions.assertEquals(6, Numbers.gcd(12, 18));
        Assertions.assertEquals(1, Numbers.gcd(17, 19));
        Assertions.assertEquals(10, Numbers.gcd(0, 10));
        Assertions.assertEquals(10, Numbers.gcd(10, 0));
        
        // Test negative values
        Assertions.assertEquals(6, Numbers.gcd(-12, 18));
        Assertions.assertEquals(6, Numbers.gcd(12, -18));
        Assertions.assertEquals(6, Numbers.gcd(-12, -18));
        
        // Test edge cases
        Assertions.assertEquals(0, Numbers.gcd(0, 0));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.gcd(0, Integer.MIN_VALUE);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.gcd(Integer.MIN_VALUE, 0);
        });
    }

    @Test
    public void testGcdLong() {
        // Test basic cases
        Assertions.assertEquals(6L, Numbers.gcd(12L, 18L));
        Assertions.assertEquals(1L, Numbers.gcd(17L, 19L));
        Assertions.assertEquals(10L, Numbers.gcd(0L, 10L));
        
        // Test negative values
        Assertions.assertEquals(6L, Numbers.gcd(-12L, 18L));
        
        // Test large values
        Assertions.assertEquals(1000000L, Numbers.gcd(1000000L, 2000000L));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.gcd(0L, Long.MIN_VALUE);
        });
    }

    @Test
    public void testLcmInt() {
        // Test basic cases
        Assertions.assertEquals(36, Numbers.lcm(12, 18));
        Assertions.assertEquals(323, Numbers.lcm(17, 19));
        
        // Test with zero
        Assertions.assertEquals(0, Numbers.lcm(0, 10));
        Assertions.assertEquals(0, Numbers.lcm(10, 0));
        
        // Test negative values
        Assertions.assertEquals(36, Numbers.lcm(-12, 18));
        Assertions.assertEquals(36, Numbers.lcm(12, -18));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testLcmLong() {
        // Test basic cases
        Assertions.assertEquals(36L, Numbers.lcm(12L, 18L));
        
        // Test with zero
        Assertions.assertEquals(0L, Numbers.lcm(0L, 10L));
        
        // Test large values
        long a = 1000000L;
        long b = 2000000L;
        Assertions.assertEquals(2000000L, Numbers.lcm(a, b));
    }

    @Test
    public void testAddExactInt() {
        // Test normal addition
        Assertions.assertEquals(5, Numbers.addExact(2, 3));
        Assertions.assertEquals(0, Numbers.addExact(-5, 5));
        
        // Test edge cases
        Assertions.assertEquals(0, Numbers.addExact(Integer.MAX_VALUE, -Integer.MAX_VALUE));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.addExact(Integer.MAX_VALUE, 1);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.addExact(Integer.MIN_VALUE, -1);
        });
    }

    @Test
    public void testAddExactLong() {
        // Test normal addition
        Assertions.assertEquals(5L, Numbers.addExact(2L, 3L));
        
        // Test large values
        Assertions.assertEquals(2000000000L, Numbers.addExact(1000000000L, 1000000000L));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.addExact(Long.MAX_VALUE, 1L);
        });
    }

    @Test
    public void testSubtractExactInt() {
        // Test normal subtraction
        Assertions.assertEquals(-1, Numbers.subtractExact(2, 3));
        Assertions.assertEquals(5, Numbers.subtractExact(8, 3));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.subtractExact(Integer.MIN_VALUE, 1);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.subtractExact(Integer.MAX_VALUE, -1);
        });
    }

    @Test
    public void testSubtractExactLong() {
        // Test normal subtraction
        Assertions.assertEquals(-1L, Numbers.subtractExact(2L, 3L));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.subtractExact(Long.MIN_VALUE, 1L);
        });
    }

    @Test
    public void testMultiplyExactInt() {
        // Test normal multiplication
        Assertions.assertEquals(6, Numbers.multiplyExact(2, 3));
        Assertions.assertEquals(0, Numbers.multiplyExact(0, Integer.MAX_VALUE));
        
        // Test negative values
        Assertions.assertEquals(-6, Numbers.multiplyExact(-2, 3));
        Assertions.assertEquals(6, Numbers.multiplyExact(-2, -3));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.multiplyExact(Integer.MAX_VALUE, 2);
        });
    }

    @Test
    public void testMultiplyExactLong() {
        // Test normal multiplication
        Assertions.assertEquals(6L, Numbers.multiplyExact(2L, 3L));
        
        // Test large values
        Assertions.assertEquals(1000000000000L, Numbers.multiplyExact(1000000L, 1000000L));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.multiplyExact(Long.MAX_VALUE, 2L);
        });
    }

    @Test
    public void testPowExactInt() {
        // Test basic cases
        Assertions.assertEquals(1, Numbers.powExact(2, 0));
        Assertions.assertEquals(8, Numbers.powExact(2, 3));
        
        // Test special bases
        Assertions.assertEquals(1, Numbers.powExact(0, 0));
        Assertions.assertEquals(0, Numbers.powExact(0, 5));
        Assertions.assertEquals(1, Numbers.powExact(1, 100));
        Assertions.assertEquals(-1, Numbers.powExact(-1, 1));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.powExact(2, 31);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.powExact(3, 20);
        });
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.powExact(2, -1);
        });
    }

    @Test
    public void testPowExactLong() {
        // Test basic cases
        Assertions.assertEquals(1L, Numbers.powExact(2L, 0));
        Assertions.assertEquals(8L, Numbers.powExact(2L, 3));
        
        // Test large powers
        Assertions.assertEquals(1L << 30, Numbers.powExact(2L, 30));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.powExact(2L, 63);
        });
    }

    @Test
    public void testSaturatedAddInt() {
        // Test normal addition
        Assertions.assertEquals(5, Numbers.saturatedAdd(2, 3));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, 1));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, -1));
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void testSaturatedAddLong() {
        // Test normal addition
        Assertions.assertEquals(5L, Numbers.saturatedAdd(2L, 3L));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, 1L));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Long.MIN_VALUE, Numbers.saturatedAdd(Long.MIN_VALUE, -1L));
    }

    @Test
    public void testSaturatedSubtractInt() {
        // Test normal subtraction
        Assertions.assertEquals(-1, Numbers.saturatedSubtract(2, 3));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedSubtract(Integer.MAX_VALUE, -1));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedSubtract(Integer.MIN_VALUE, 1));
    }

    @Test
    public void testSaturatedSubtractLong() {
        // Test normal subtraction
        Assertions.assertEquals(-1L, Numbers.saturatedSubtract(2L, 3L));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.saturatedSubtract(Long.MAX_VALUE, -1L));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Long.MIN_VALUE, Numbers.saturatedSubtract(Long.MIN_VALUE, 1L));
    }

    @Test
    public void testSaturatedMultiplyInt() {
        // Test normal multiplication
        Assertions.assertEquals(6, Numbers.saturatedMultiply(2, 3));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, 2));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, -2));
    }

    @Test
    public void testSaturatedMultiplyLong() {
        // Test normal multiplication
        Assertions.assertEquals(6L, Numbers.saturatedMultiply(2L, 3L));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, 2L));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Long.MIN_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, -2L));
    }

    @Test
    public void testSaturatedPowInt() {
        // Test basic cases
        Assertions.assertEquals(1, Numbers.saturatedPow(2, 0));
        Assertions.assertEquals(8, Numbers.saturatedPow(2, 3));
        
        // Test special bases
        Assertions.assertEquals(1, Numbers.saturatedPow(0, 0));
        Assertions.assertEquals(0, Numbers.saturatedPow(0, 5));
        Assertions.assertEquals(1, Numbers.saturatedPow(1, 100));
        Assertions.assertEquals(-1, Numbers.saturatedPow(-1, 1));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(2, 31));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(3, 20));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.saturatedPow(2, -1);
        });
    }

    @Test
    public void testSaturatedPowLong() {
        // Test basic cases
        Assertions.assertEquals(1L, Numbers.saturatedPow(2L, 0));
        Assertions.assertEquals(8L, Numbers.saturatedPow(2L, 3));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(2L, 63));
    }

    @Test
    public void testSaturatedCast() {
        // Test normal cast
        Assertions.assertEquals(100, Numbers.saturatedCast(100L));
        Assertions.assertEquals(-100, Numbers.saturatedCast(-100L));
        
        // Test overflow to MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedCast(Long.MAX_VALUE));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedCast((long) Integer.MAX_VALUE + 1));
        
        // Test underflow to MIN_VALUE
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedCast(Long.MIN_VALUE));
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedCast((long) Integer.MIN_VALUE - 1));
    }

    @Test
    public void testFactorial() {
        // Test basic cases
        Assertions.assertEquals(1, Numbers.factorial(0));
        Assertions.assertEquals(1, Numbers.factorial(1));
        Assertions.assertEquals(2, Numbers.factorial(2));
        Assertions.assertEquals(6, Numbers.factorial(3));
        Assertions.assertEquals(24, Numbers.factorial(4));
        Assertions.assertEquals(120, Numbers.factorial(5));
        
        // Test large values return MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.factorial(13));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.factorial(20));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.factorial(-1);
        });
    }

    @Test
    public void testFactorialToLong() {
        // Test basic cases
        Assertions.assertEquals(1L, Numbers.factorialToLong(0));
        Assertions.assertEquals(1L, Numbers.factorialToLong(1));
        Assertions.assertEquals(2L, Numbers.factorialToLong(2));
        Assertions.assertEquals(6L, Numbers.factorialToLong(3));
        
        // Test larger values
        Assertions.assertEquals(3628800L, Numbers.factorialToLong(10));
        Assertions.assertEquals(479001600L, Numbers.factorialToLong(12));
        
        // Test overflow returns MAX_VALUE
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.factorialToLong(21));
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.factorialToLong(30));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.factorialToLong(-1);
        });
    }

    @Test
    public void testFactorialToDouble() {
        // Test basic cases
        Assertions.assertEquals(1.0, Numbers.factorialToDouble(0), 0.0001);
        Assertions.assertEquals(1.0, Numbers.factorialToDouble(1), 0.0001);
        Assertions.assertEquals(2.0, Numbers.factorialToDouble(2), 0.0001);
        Assertions.assertEquals(6.0, Numbers.factorialToDouble(3), 0.0001);
        
        // Test larger values
        Assertions.assertEquals(3628800.0, Numbers.factorialToDouble(10), 0.1);
        
        // Test very large values return POSITIVE_INFINITY
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Numbers.factorialToDouble(171), 0.0);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Numbers.factorialToDouble(200), 0.0);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.factorialToDouble(-1);
        });
    }

    @Test
    public void testFactorialToBigInteger() {
        // Test basic cases
        Assertions.assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(0));
        Assertions.assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(1));
        Assertions.assertEquals(BigInteger.valueOf(2), Numbers.factorialToBigInteger(2));
        Assertions.assertEquals(BigInteger.valueOf(6), Numbers.factorialToBigInteger(3));
        
        // Test larger values
        Assertions.assertEquals(BigInteger.valueOf(3628800), Numbers.factorialToBigInteger(10));
        
        // Test very large values (should not overflow)
        BigInteger result = Numbers.factorialToBigInteger(50);
        Assertions.assertTrue(result.compareTo(BigInteger.ZERO) > 0);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.factorialToBigInteger(-1);
        });
    }

    @Test
    public void testBinomial() {
        // Test basic cases
        Assertions.assertEquals(1, Numbers.binomial(0, 0));
        Assertions.assertEquals(1, Numbers.binomial(5, 0));
        Assertions.assertEquals(5, Numbers.binomial(5, 1));
        Assertions.assertEquals(10, Numbers.binomial(5, 2));
        Assertions.assertEquals(10, Numbers.binomial(5, 3));
        Assertions.assertEquals(5, Numbers.binomial(5, 4));
        Assertions.assertEquals(1, Numbers.binomial(5, 5));
        
        // Test larger values
        Assertions.assertEquals(252, Numbers.binomial(10, 5));
        
        // Test overflow returns MAX_VALUE
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.binomial(40, 20));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.binomial(-1, 0);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.binomial(5, -1);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.binomial(5, 6);
        });
    }

    @Test
    public void testBinomialToLong() {
        // Test basic cases
        Assertions.assertEquals(1L, Numbers.binomialToLong(0, 0));
        Assertions.assertEquals(1L, Numbers.binomialToLong(5, 0));
        Assertions.assertEquals(5L, Numbers.binomialToLong(5, 1));
        Assertions.assertEquals(10L, Numbers.binomialToLong(5, 2));
        
        // Test larger values
        Assertions.assertEquals(252L, Numbers.binomialToLong(10, 5));
        Assertions.assertEquals(184756L, Numbers.binomialToLong(20, 10));
        
        // Test overflow returns MAX_VALUE
        Assertions.assertEquals(Long.MAX_VALUE, Numbers.binomialToLong(67, 33));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.binomialToLong(-1, 0);
        });
    }

    @Test
    public void testBinomialToBigInteger() {
        // Test basic cases
        Assertions.assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(0, 0));
        Assertions.assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(5, 0));
        Assertions.assertEquals(BigInteger.valueOf(5), Numbers.binomialToBigInteger(5, 1));
        
        // Test larger values
        Assertions.assertEquals(BigInteger.valueOf(252), Numbers.binomialToBigInteger(10, 5));
        
        // Test very large values (should not overflow)
        BigInteger result = Numbers.binomialToBigInteger(100, 50);
        Assertions.assertTrue(result.compareTo(BigInteger.ZERO) > 0);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.binomialToBigInteger(-1, 0);
        });
    }

    @Test
    public void testMeanInt() {
        // Test basic cases
        Assertions.assertEquals(3, Numbers.mean(2, 4));
        Assertions.assertEquals(0, Numbers.mean(-1, 1));
        
        // Test overflow resilience
        Assertions.assertEquals(Integer.MAX_VALUE - 1, Numbers.mean(Integer.MAX_VALUE, Integer.MAX_VALUE - 2));
        
        // Test negative values
        Assertions.assertEquals(-3, Numbers.mean(-2, -4));
    }

    @Test
    public void testMeanLong() {
        // Test basic cases
        Assertions.assertEquals(3L, Numbers.mean(2L, 4L));
        Assertions.assertEquals(0L, Numbers.mean(-1L, 1L));
        
        // Test overflow resilience
        Assertions.assertEquals(Long.MAX_VALUE - 1, Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE - 2));
    }

    @Test
    public void testMeanDouble() {
        // Test basic cases
        Assertions.assertEquals(3.0, Numbers.mean(2.0, 4.0), 0.0001);
        Assertions.assertEquals(0.0, Numbers.mean(-1.0, 1.0), 0.0001);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(Double.NaN, 1.0);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(1.0, Double.POSITIVE_INFINITY);
        });
    }

    @Test
    public void testMeanIntArray() {
        // Test basic cases
        Assertions.assertEquals(3.0, Numbers.mean(1, 2, 3, 4, 5), 0.0001);
        Assertions.assertEquals(10.0, Numbers.mean(10), 0.0001);
        Assertions.assertEquals(0.0, Numbers.mean(-5, 0, 5), 0.0001);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(new int[0]);
        });
    }

    @Test
    public void testMeanLongArray() {
        // Test basic cases
        Assertions.assertEquals(3.0, Numbers.mean(1L, 2L, 3L, 4L, 5L), 0.0001);
        Assertions.assertEquals(10.0, Numbers.mean(10L), 0.0001);
        
        // Test large values
        Assertions.assertEquals(1000000.0, Numbers.mean(999999L, 1000000L, 1000001L), 0.0001);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(new long[0]);
        });
    }

    @Test
    public void testMeanDoubleArray() {
        // Test basic cases
        Assertions.assertEquals(3.0, Numbers.mean(1.0, 2.0, 3.0, 4.0, 5.0), 0.0001);
        Assertions.assertEquals(10.5, Numbers.mean(10.0, 11.0), 0.0001);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(new double[0]);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(1.0, Double.NaN);
        });
    }

    @Test
    public void testRoundFloat() {
        // Test basic rounding
        Assertions.assertEquals(1.0f, Numbers.round(1.234f, 0), 0.0001f);
        Assertions.assertEquals(1.2f, Numbers.round(1.234f, 1), 0.0001f);
        Assertions.assertEquals(1.23f, Numbers.round(1.234f, 2), 0.0001f);
        Assertions.assertEquals(1.234f, Numbers.round(1.234f, 3), 0.0001f);
        
        // Test rounding up
        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1), 0.0001f);
        
        // Test negative values
        Assertions.assertEquals(-1.2f, Numbers.round(-1.234f, 1), 0.0001f);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234f, -1);
        });
    }

    @Test
    public void testRoundDouble() {
        // Test basic rounding
        Assertions.assertEquals(1.0, Numbers.round(1.234, 0), 0.0001);
        Assertions.assertEquals(1.2, Numbers.round(1.234, 1), 0.0001);
        Assertions.assertEquals(1.23, Numbers.round(1.234, 2), 0.0001);
        
        // Test rounding up
        Assertions.assertEquals(1.3, Numbers.round(1.25, 1), 0.0001);
        
        // Test large scale
        Assertions.assertEquals(1.2345679, Numbers.round(1.23456789, 7), 0.00000001);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234, -1);
        });
    }

    @Test
    public void testRoundFloatWithRoundingMode() {
        // Test different rounding modes
        Assertions.assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.DOWN), 0.0001f);
        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1, RoundingMode.UP), 0.0001f);
        Assertions.assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.HALF_DOWN), 0.0001f);
        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1, RoundingMode.HALF_UP), 0.0001f);
        Assertions.assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.HALF_EVEN), 0.0001f);
        
        // Test with null rounding mode (defaults to HALF_UP)
        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1, null), 0.0001f);
    }

    @Test
    public void testRoundDoubleWithRoundingMode() {
        // Test different rounding modes
        Assertions.assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.DOWN), 0.0001);
        Assertions.assertEquals(1.3, Numbers.round(1.25, 1, RoundingMode.UP), 0.0001);
        Assertions.assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.HALF_DOWN), 0.0001);
        Assertions.assertEquals(1.3, Numbers.round(1.25, 1, RoundingMode.HALF_UP), 0.0001);
        Assertions.assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.HALF_EVEN), 0.0001);
    }

    @Test
    public void testRoundFloatWithStringFormat() {
        // Test with string format
        float result = Numbers.round(1.234f, "#.#");
        Assertions.assertEquals(1.2f, result, 0.0001f);
        
        result = Numbers.round(1.234f, "#.##");
        Assertions.assertEquals(1.23f, result, 0.0001f);
        
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.round(0.5f, "#%"));
    }

    @Test
    public void testRoundDoubleWithStringFormat() {
        // Test with string format
        double result = Numbers.round(1.234, "#.#");
        Assertions.assertEquals(1.2, result, 0.0001);
        
        result = Numbers.round(1.234, "#.##");
        Assertions.assertEquals(1.23, result, 0.0001);
        
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.round(0.5, "#%"));
    }

    @Test
    public void testRoundFloatWithDecimalFormat() {
        // Test with DecimalFormat
        DecimalFormat df = new DecimalFormat("#.#");
        float result = Numbers.round(1.234f, df);
        Assertions.assertEquals(1.2f, result, 0.0001f);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234f, (DecimalFormat) null);
        });
    }

    @Test
    public void testRoundDoubleWithDecimalFormat() {
        // Test with DecimalFormat
        DecimalFormat df = new DecimalFormat("#.##");
        double result = Numbers.round(1.234, df);
        Assertions.assertEquals(1.23, result, 0.0001);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234, (DecimalFormat) null);
        });
    }

    @Test
    public void testRoundToInt() {
        // Test basic rounding
        Assertions.assertEquals(1, Numbers.roundToInt(1.4, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.roundToInt(1.6, RoundingMode.UP));
        Assertions.assertEquals(2, Numbers.roundToInt(1.5, RoundingMode.HALF_UP));
        
        // Test negative values
        Assertions.assertEquals(-1, Numbers.roundToInt(-1.4, RoundingMode.DOWN));
        Assertions.assertEquals(-1, Numbers.roundToInt(-1.6, RoundingMode.DOWN));
        
        // Test exact values
        Assertions.assertEquals(5, Numbers.roundToInt(5.0, RoundingMode.UNNECESSARY));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToInt(Double.NaN, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToInt(Double.POSITIVE_INFINITY, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToInt(1.5, RoundingMode.UNNECESSARY);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToInt(1e10, RoundingMode.DOWN);
        });
    }

    @Test
    public void testRoundToLong() {
        // Test basic rounding
        Assertions.assertEquals(1L, Numbers.roundToLong(1.4, RoundingMode.DOWN));
        Assertions.assertEquals(2L, Numbers.roundToLong(1.6, RoundingMode.UP));
        Assertions.assertEquals(2L, Numbers.roundToLong(1.5, RoundingMode.HALF_UP));
        
        // Test large values
        Assertions.assertEquals(1000000000000L, Numbers.roundToLong(1e12, RoundingMode.UNNECESSARY));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToLong(Double.NaN, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToLong(1e20, RoundingMode.DOWN);
        });
    }

    @Test
    public void testRoundToBigInteger() {
        // Test basic rounding
        Assertions.assertEquals(BigInteger.ONE, Numbers.roundToBigInteger(1.4, RoundingMode.DOWN));
        Assertions.assertEquals(BigInteger.valueOf(2), Numbers.roundToBigInteger(1.6, RoundingMode.UP));
        
        // Test large values
        BigInteger result = Numbers.roundToBigInteger(1e20, RoundingMode.DOWN);
        Assertions.assertTrue(result.compareTo(BigInteger.ZERO) > 0);
        
        // Test negative values
        Assertions.assertEquals(BigInteger.valueOf(-1), Numbers.roundToBigInteger(-1.6, RoundingMode.DOWN));
        
        // Test exceptions
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToBigInteger(Double.NaN, RoundingMode.DOWN);
        });
        
        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.roundToBigInteger(1.5, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testFuzzyEqualsFloat() {
        // Test exact equality
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0f, 1.0f, 0.0f));
        
        // Test within tolerance
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0f, 1.001f, 0.01f));
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0f, 0.999f, 0.01f));
        
        // Test outside tolerance
        Assertions.assertFalse(Numbers.fuzzyEquals(1.0f, 1.1f, 0.01f));
        
        // Test special cases
        Assertions.assertTrue(Numbers.fuzzyEquals(Float.NaN, Float.NaN, 0.01f));
        Assertions.assertTrue(Numbers.fuzzyEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.01f));
        Assertions.assertFalse(Numbers.fuzzyEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1000.0f));
        
        // Test zero handling
        Assertions.assertTrue(Numbers.fuzzyEquals(0.0f, -0.0f, 0.0f));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyEquals(1.0f, 1.0f, -0.1f);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyEquals(1.0f, 1.0f, Float.NaN);
        });
    }

    @Test
    public void testFuzzyEqualsDouble() {
        // Test exact equality
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0, 1.0, 0.0));
        
        // Test within tolerance
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0, 1.001, 0.01));
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0, 0.999, 0.01));
        
        // Test outside tolerance
        Assertions.assertFalse(Numbers.fuzzyEquals(1.0, 1.1, 0.01));
        
        // Test special cases
        Assertions.assertTrue(Numbers.fuzzyEquals(Double.NaN, Double.NaN, 0.01));
        Assertions.assertTrue(Numbers.fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01));
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyEquals(1.0, 1.0, -0.1);
        });
    }

    @Test
    public void testFuzzyCompareFloat() {
        // Test exact equality
        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0f, 0.0f));
        
        // Test within tolerance (treated as equal)
        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.001f, 0.01f));
        
        // Test outside tolerance
        Assertions.assertTrue(Numbers.fuzzyCompare(1.0f, 2.0f, 0.01f) < 0);
        Assertions.assertTrue(Numbers.fuzzyCompare(2.0f, 1.0f, 0.01f) > 0);
        
        // Test NaN handling
        Assertions.assertEquals(0, Numbers.fuzzyCompare(Float.NaN, Float.NaN, 0.01f));
        Assertions.assertTrue(Numbers.fuzzyCompare(1.0f, Float.NaN, 0.01f) < 0);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyCompare(1.0f, 1.0f, -0.1f);
        });
    }

    @Test
    public void testFuzzyCompareDouble() {
        // Test exact equality
        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0, 1.0, 0.0));
        
        // Test within tolerance (treated as equal)
        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0, 1.001, 0.01));
        
        // Test outside tolerance
        Assertions.assertTrue(Numbers.fuzzyCompare(1.0, 2.0, 0.01) < 0);
        Assertions.assertTrue(Numbers.fuzzyCompare(2.0, 1.0, 0.01) > 0);
        
        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyCompare(1.0, 1.0, -0.1);
        });
    }

    @Test
    public void testIsMathematicalInteger() {
        // Test integers
        Assertions.assertTrue(Numbers.isMathematicalInteger(0.0));
        Assertions.assertTrue(Numbers.isMathematicalInteger(1.0));
        Assertions.assertTrue(Numbers.isMathematicalInteger(-1.0));
        Assertions.assertTrue(Numbers.isMathematicalInteger(100.0));
        
        // Test non-integers
        Assertions.assertFalse(Numbers.isMathematicalInteger(0.1));
        Assertions.assertFalse(Numbers.isMathematicalInteger(1.5));
        Assertions.assertFalse(Numbers.isMathematicalInteger(-1.5));
        
        // Test special values
        Assertions.assertFalse(Numbers.isMathematicalInteger(Double.NaN));
        Assertions.assertFalse(Numbers.isMathematicalInteger(Double.POSITIVE_INFINITY));
        Assertions.assertFalse(Numbers.isMathematicalInteger(Double.NEGATIVE_INFINITY));
        
        // Test edge cases
        Assertions.assertTrue(Numbers.isMathematicalInteger(Math.pow(2, 52)));
        Assertions.assertTrue(Numbers.isMathematicalInteger(-0.0));
    }

    @Test
    public void testAsinh() {
        // Test basic values
        Assertions.assertEquals(0.0, Numbers.asinh(0.0), 1e-10);
        Assertions.assertEquals(0.881373587, Numbers.asinh(1.0), 1e-9);
        Assertions.assertEquals(-0.881373587, Numbers.asinh(-1.0), 1e-9);
        
        // Test small values
        Assertions.assertEquals(0.0998340, Numbers.asinh(0.1), 1e-6);
        Assertions.assertEquals(0.001, Numbers.asinh(0.001), 1e-9);
        
        // Test larger values
        Assertions.assertEquals(1.443635475, Numbers.asinh(2.0), 1e-9);
        Assertions.assertEquals(2.998222950, Numbers.asinh(10.0), 1e-9);
        
        // Test very small values (different code paths)
        Assertions.assertEquals(0.0001, Numbers.asinh(0.0001), 1e-10);
        Assertions.assertEquals(0.00001, Numbers.asinh(0.00001), 1e-11);
    }

    @Test
    public void testAcosh() {
        // Test basic values
        Assertions.assertEquals(0.0, Numbers.acosh(1.0), 1e-10);
        Assertions.assertEquals(1.316957897, Numbers.acosh(2.0), 1e-9);
        Assertions.assertEquals(2.993222846, Numbers.acosh(10.0), 1e-9);
        
        // Test edge cases
        Assertions.assertEquals(0.962423650, Numbers.acosh(1.5), 1e-9);
        
        // Note: acosh is only defined for x >= 1
        // Values less than 1 would result in NaN
        Assertions.assertTrue(Double.isNaN(Numbers.acosh(0.5)));
    }

    @Test
    public void testAtanh() {
        // Test basic values
        Assertions.assertEquals(0.0, Numbers.atanh(0.0), 1e-10);
        Assertions.assertEquals(0.549306144, Numbers.atanh(0.5), 1e-9);
        Assertions.assertEquals(-0.549306144, Numbers.atanh(-0.5), 1e-9);
        
        // Test small values
        Assertions.assertEquals(0.100335, Numbers.atanh(0.1), 1e-6);
        Assertions.assertEquals(0.001, Numbers.atanh(0.001), 1e-9);
        
        // Test near boundary values
        Assertions.assertEquals(1.472219489, Numbers.atanh(0.9), 1e-9);
        
        // Test very small values (different code paths)
        Assertions.assertEquals(0.0001, Numbers.atanh(0.0001), 1e-9);
        
        // Note: atanh is only defined for |x| < 1
        // Values outside this range would result in NaN or infinity
        Assertions.assertTrue(Double.isInfinite(Numbers.atanh(1.0)));
        Assertions.assertTrue(Double.isNaN(Numbers.atanh(2.0)));
    }

    @Test
    public void testListProduct() {
        // Test empty list
        List<BigInteger> empty = new ArrayList<>();
        Assertions.assertEquals(BigInteger.ONE, Numbers.listProduct(empty));
        
        // Test single element
        List<BigInteger> single = new ArrayList<>();
        single.add(BigInteger.valueOf(5));
        Assertions.assertEquals(BigInteger.valueOf(5), Numbers.listProduct(single));
        
        // Test two elements
        List<BigInteger> two = new ArrayList<>();
        two.add(BigInteger.valueOf(3));
        two.add(BigInteger.valueOf(4));
        Assertions.assertEquals(BigInteger.valueOf(12), Numbers.listProduct(two));
        
        // Test three elements
        List<BigInteger> three = new ArrayList<>();
        three.add(BigInteger.valueOf(2));
        three.add(BigInteger.valueOf(3));
        three.add(BigInteger.valueOf(4));
        Assertions.assertEquals(BigInteger.valueOf(24), Numbers.listProduct(three));
        
        // Test multiple elements
        List<BigInteger> multiple = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            multiple.add(BigInteger.valueOf(i));
        }
        Assertions.assertEquals(BigInteger.valueOf(120), Numbers.listProduct(multiple));
    }

    @Test
    public void testFitsInLong() {
        // Test values that fit in long
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.ZERO));
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.ONE));
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MAX_VALUE)));
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MIN_VALUE)));
        
        // Test values that don't fit in long
        BigInteger tooLarge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        Assertions.assertFalse(Numbers.fitsInLong(tooLarge));
        
        BigInteger tooSmall = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        Assertions.assertFalse(Numbers.fitsInLong(tooSmall));
        
        // Test large BigInteger
        BigInteger veryLarge = new BigInteger("123456789012345678901234567890");
        Assertions.assertFalse(Numbers.fitsInLong(veryLarge));
    }

    @Test
    public void testIsPowerOfTwo() {
        // Test powers of two
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.ONE));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(2)));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(4)));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(8)));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(1024)));
        
        // Test non-powers of two
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.ZERO));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(3)));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(5)));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(6)));
        
        // Test negative values
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(-1)));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(-2)));
        
        // Test large power of two
        BigInteger largePower = BigInteger.ONE.shiftLeft(100);
        Assertions.assertTrue(Numbers.isPowerOfTwo(largePower));
    }
}