package com.landawn.abacus.util;

import static com.landawn.abacus.util.Numbers.acosh;
import static com.landawn.abacus.util.Numbers.addExact;
import static com.landawn.abacus.util.Numbers.asinh;
import static com.landawn.abacus.util.Numbers.atanh;
import static com.landawn.abacus.util.Numbers.binomial;
import static com.landawn.abacus.util.Numbers.ceilingPowerOfTwo;
import static com.landawn.abacus.util.Numbers.convert;
import static com.landawn.abacus.util.Numbers.createBigDecimal;
import static com.landawn.abacus.util.Numbers.createBigInteger;
import static com.landawn.abacus.util.Numbers.createDouble;
import static com.landawn.abacus.util.Numbers.createFloat;
import static com.landawn.abacus.util.Numbers.createInteger;
import static com.landawn.abacus.util.Numbers.createLong;
import static com.landawn.abacus.util.Numbers.createNumber;
import static com.landawn.abacus.util.Numbers.divide;
import static com.landawn.abacus.util.Numbers.extractFirstDouble;
import static com.landawn.abacus.util.Numbers.extractFirstInt;
import static com.landawn.abacus.util.Numbers.factorial;
import static com.landawn.abacus.util.Numbers.factorialToBigInteger;
import static com.landawn.abacus.util.Numbers.factorialToDouble;
import static com.landawn.abacus.util.Numbers.factorialToLong;
import static com.landawn.abacus.util.Numbers.floorPowerOfTwo;
import static com.landawn.abacus.util.Numbers.fuzzyCompare;
import static com.landawn.abacus.util.Numbers.fuzzyEquals;
import static com.landawn.abacus.util.Numbers.gcd;
import static com.landawn.abacus.util.Numbers.isCreatable;
import static com.landawn.abacus.util.Numbers.isDigits;
import static com.landawn.abacus.util.Numbers.isMathematicalInteger;
import static com.landawn.abacus.util.Numbers.isParsable;
import static com.landawn.abacus.util.Numbers.isPerfectSquare;
import static com.landawn.abacus.util.Numbers.isPowerOfTwo;
import static com.landawn.abacus.util.Numbers.isPrime;
import static com.landawn.abacus.util.Numbers.lcm;
import static com.landawn.abacus.util.Numbers.log10;
import static com.landawn.abacus.util.Numbers.log2;
import static com.landawn.abacus.util.Numbers.mean;
import static com.landawn.abacus.util.Numbers.mod;
import static com.landawn.abacus.util.Numbers.multiplyExact;
import static com.landawn.abacus.util.Numbers.pow;
import static com.landawn.abacus.util.Numbers.powExact;
import static com.landawn.abacus.util.Numbers.round;
import static com.landawn.abacus.util.Numbers.roundToInt;
import static com.landawn.abacus.util.Numbers.saturatedAdd;
import static com.landawn.abacus.util.Numbers.saturatedCast;
import static com.landawn.abacus.util.Numbers.saturatedMultiply;
import static com.landawn.abacus.util.Numbers.sqrt;
import static com.landawn.abacus.util.Numbers.subtractExact;
import static com.landawn.abacus.util.Numbers.toByte;
import static com.landawn.abacus.util.Numbers.toDouble;
import static com.landawn.abacus.util.Numbers.toFloat;
import static com.landawn.abacus.util.Numbers.toInt;
import static com.landawn.abacus.util.Numbers.toIntExact;
import static com.landawn.abacus.util.Numbers.toLong;
import static com.landawn.abacus.util.Numbers.toScaledBigDecimal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("new-test")
public class Numbers101Test extends TestBase {

    @Nested
    @DisplayName("Number Conversion Tests")
    public class ConversionTests {

        @Test
        @DisplayName("Convert between same types should return identity")
        public void testConvertSameType() {
            assertEquals(Integer.valueOf(42), convert(42, Integer.class));
            assertEquals(Long.valueOf(42L), convert(42L, Long.class));
            assertEquals(Double.valueOf(42.5), convert(42.5, Double.class));
        }

        @Test
        @DisplayName("Convert null should return default value")
        public void testConvertNull() {
            assertEquals(null, convert(null, Integer.class));
            assertEquals(null, convert(null, Long.class));
            assertEquals(null, convert(null, Double.class));
            assertEquals(null, convert(null, Float.class));

            assertEquals(0, convert(null, int.class).intValue());
            assertEquals(0l, convert(null, long.class).longValue());
            assertEquals(0d, convert(null, double.class).doubleValue());
            assertEquals(0f, convert(null, float.class).floatValue());
        }

        @Test
        @DisplayName("Convert with overflow should throw ArithmeticException")
        public void testConvertOverflow() {
            assertThrows(ArithmeticException.class, () -> convert(128, byte.class));
            assertThrows(ArithmeticException.class, () -> convert(-129, byte.class));

            assertThrows(ArithmeticException.class, () -> convert(32768, short.class));
            assertThrows(ArithmeticException.class, () -> convert(-32769, short.class));

            assertThrows(ArithmeticException.class, () -> convert(Long.MAX_VALUE, int.class));
        }

        @Test
        @DisplayName("Convert within valid ranges should succeed")
        public void testConvertValidRanges() {
            assertEquals(Byte.valueOf((byte) 127), convert(127, byte.class));
            assertEquals(Byte.valueOf((byte) -128), convert(-128, byte.class));

            assertEquals(Short.valueOf((short) 32767), convert(32767, short.class));
            assertEquals(Short.valueOf((short) -32768), convert(-32768, short.class));

            assertEquals(Integer.valueOf(Integer.MAX_VALUE), convert(Integer.MAX_VALUE, int.class));
            assertEquals(Integer.valueOf(Integer.MIN_VALUE), convert(Integer.MIN_VALUE, int.class));
        }

        @Test
        @DisplayName("Convert BigInteger and BigDecimal")
        public void testConvertBigNumbers() {
            BigInteger bigInt = BigInteger.valueOf(1000);
            assertEquals(Integer.valueOf(1000), convert(bigInt, int.class));

            BigDecimal bigDec = BigDecimal.valueOf(1000.5);
            assertEquals(Integer.valueOf(1000), convert(bigDec, int.class));

            BigInteger tooBig = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
            assertThrows(ArithmeticException.class, () -> convert(tooBig, long.class));
        }
    }

    @Nested
    @DisplayName("String to Number Conversion Tests")
    public class StringConversionTests {

        @ParameterizedTest
        @CsvSource({ "42, 42", "-42, -42", "0, 0", "'', 0", "null, 0" })
        @DisplayName("toByte with valid inputs")
        public void testToByte(String input, byte expected) {
            if ("null".equals(input)) {
                assertEquals(expected, toByte((String) null));
            } else {
                assertEquals(expected, toByte(input));
            }
        }

        @Test
        @DisplayName("toByte with invalid input should throw NumberFormatException")
        public void testToByteInvalid() {
            assertThrows(NumberFormatException.class, () -> toByte("abc"));
            assertThrows(NumberFormatException.class, () -> toByte("128"));
        }

        @ParameterizedTest
        @CsvSource({ "42, 42", "-42, -42", "2147483647, 2147483647", "-2147483648, -2147483648" })
        @DisplayName("toInt with valid inputs")
        public void testToInt(String input, int expected) {
            assertEquals(expected, toInt(input));
        }

        @Test
        @DisplayName("toInt with cached values")
        public void testToIntCached() {
            assertEquals(1, toInt("1"));
            assertEquals(-1, toInt("-1"));
            assertEquals(100, toInt("100"));
        }

        @ParameterizedTest
        @CsvSource({ "42L, 42", "9223372036854775807L, 9223372036854775807", "-9223372036854775808L, -9223372036854775808" })
        @DisplayName("toLong with valid inputs including L suffix")
        public void testToLong(String input, long expected) {
            assertEquals(expected, toLong(input));
        }

        @ParameterizedTest
        @CsvSource({ "42.5, 42.5", "-42.5, -42.5", "1.7976931348623157E308, 1.7976931348623157E308", "4.9E-324, 4.9E-324" })
        @DisplayName("toDouble with valid inputs")
        public void testToDouble(String input, double expected) {
            assertEquals(expected, toDouble(input), 1e-10);
        }

        @Test
        @DisplayName("toFloat with special values")
        public void testToFloatSpecialValues() {
            assertEquals(Float.POSITIVE_INFINITY, toFloat("Infinity"));
            assertEquals(Float.NEGATIVE_INFINITY, toFloat("-Infinity"));
            assertTrue(Float.isNaN(toFloat("NaN")));
        }
    }

    @Nested
    @DisplayName("Number Formatting Tests")
    public class FormattingTests {

        @ParameterizedTest
        @CsvSource({ "12.345, '#.##', '12.35'", "12.345, '0.00', '12.35'", "0.123, '#.##%', '12.3%'", "12345, '#,###', '12,345'" })
        @DisplayName("format with different patterns")
        public void testFormat(double value, String pattern, String expected) {
            assertEquals(expected, Numbers.format(value, pattern));
        }

        @Test
        @DisplayName("format with null value should use default")
        public void testFormatNull() {
            assertEquals("0", Numbers.format((Integer) null, "0"));
            assertEquals("0.00", Numbers.format((Double) null, "0.00"));
        }

        @Test
        @DisplayName("format with DecimalFormat object")
        public void testFormatWithDecimalFormat() {
            assertEquals("12.35", Numbers.format(12.345, "#.##"));
        }

        @Test
        @DisplayName("format should throw on null pattern")
        public void testFormatNullPattern() {
            assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.345, (String) null));
        }
    }

    @Nested
    @DisplayName("Number Extraction Tests")
    public class ExtractionTests {

        @ParameterizedTest
        @CsvSource({ "'abc123def', 123", "'no numbers here', 0", "'-456xyz', -456", "'', 0" })
        @DisplayName("extractFirstInt from strings")
        public void testExtractFirstInt(String input, int expected) {
            assertEquals(expected, extractFirstInt(input));
        }

        @Test
        @DisplayName("extractFirstInt with default value")
        public void testExtractFirstIntWithDefault() {
            assertEquals(999, extractFirstInt("no numbers", 999));
            assertEquals(123, extractFirstInt("abc123", 999));
        }

        @ParameterizedTest
        @CsvSource({ "'price: 12.34', 12.34", "'no decimal here', 0.0", "'-45.67 degrees', -45.67" })
        @DisplayName("extractFirstDouble from strings")
        public void testExtractFirstDouble(String input, double expected) {
            assertEquals(expected, extractFirstDouble(input), 1e-10);
        }

        @ParameterizedTest
        @CsvSource({ "'1.23e10', 1.23e10", "'2.5E-5', 2.5E-5", "'no scientific notation', 0.0" })
        @DisplayName("extractFirstSciDouble from strings")
        public void testExtractFirstSciDouble(String input, double expected) {
            assertEquals(expected, extractFirstDouble(input, true), 1e-10);
        }
    }

    @Nested
    @DisplayName("Number Creation Tests")
    public class CreationTests {

        @Test
        @DisplayName("createInteger with various formats")
        public void testCreateInteger() {
            assertEquals(Integer.valueOf(42), createInteger("42"));
            assertEquals(Integer.valueOf(255), createInteger("0xFF"));
            assertEquals(Integer.valueOf(8), createInteger("010"));
            assertNull(createInteger(null));
        }

        @Test
        @DisplayName("createLong with L suffix")
        public void testCreateLong() {
            assertEquals(Long.valueOf(42L), createLong("42"));
            assertEquals(Long.valueOf(42L), createLong("42L"));
            assertEquals(Long.valueOf(42L), createLong("42l"));
        }

        @Test
        @DisplayName("createFloat and createDouble")
        public void testCreateFloat() {
            assertEquals(Float.valueOf(42.5f), createFloat("42.5"));
            assertEquals(Double.valueOf(42.5), createDouble("42.5"));

            assertNull(createFloat(null));
            assertNull(createDouble(null));
        }

        @Test
        @DisplayName("createBigInteger with different bases")
        public void testCreateBigInteger() {
            assertEquals(BigInteger.valueOf(42), createBigInteger("42"));
            assertEquals(BigInteger.valueOf(255), createBigInteger("0xFF"));
            assertEquals(BigInteger.valueOf(255), createBigInteger("#FF"));
            assertEquals(BigInteger.valueOf(8), createBigInteger("010"));

            assertNull(createBigInteger(null));
        }

        @Test
        @DisplayName("createNumber with type qualifiers")
        public void testCreateNumberWithQualifiers() {
            assertTrue(createNumber("42L") instanceof Long);
            assertTrue(createNumber("42.5f") instanceof Float);
            assertTrue(createNumber("42.5d") instanceof Double);
            assertTrue(createNumber("42") instanceof Integer);
        }

        @Test
        @DisplayName("createNumber with scientific notation")
        public void testCreateNumberScientific() {
            assertEquals(1.23e10, ((Double) createNumber("1.23e10")).doubleValue(), 1e-10);
            assertEquals(1.23e-10, ((Double) createNumber("1.23e-10")).doubleValue(), 1e-20);
        }

        @Test
        @DisplayName("createNumber should return appropriate type for size")
        public void testCreateNumberAutoType() {
            assertTrue(createNumber("42") instanceof Integer);

            assertTrue(createNumber("999999999999") instanceof Long);

            assertTrue(createNumber("99999999999999999999999999999999") instanceof BigInteger);

            assertTrue(createNumber("42.5") instanceof Double);
        }
    }

    @Nested
    @DisplayName("Number Validation Tests")
    public class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = { "42", "-42", "42.5", "-42.5", "1.23e10", "0xFF", "010" })
        @DisplayName("isCreatable should return true for valid numbers")
        public void testIsCreatableValid(String input) {
            assertTrue(isCreatable(input));
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "abc", "42.5.6", "1.23e", "0x", "--42" })
        @DisplayName("isCreatable should return false for invalid numbers")
        public void testIsCreatableInvalid(String input) {
            assertFalse(isCreatable(input));
        }

        @Test
        @DisplayName("isCreatable should handle null and empty")
        public void testIsCreatableNullEmpty() {
            assertFalse(isCreatable(null));
            assertFalse(isCreatable(""));
            assertFalse(isCreatable("   "));
        }

        @ParameterizedTest
        @ValueSource(strings = { "42", "-42", "42.5", "-42.5" })
        @DisplayName("isParsable should return true for parsable numbers")
        public void testIsParsableValid(String input) {
            assertTrue(isParsable(input));
        }

        @ParameterizedTest
        @ValueSource(strings = { "0xFF", "1.23e10", "42L" })
        @DisplayName("isParsable should return false for non-parsable formats")
        public void testIsParsableInvalid(String input) {
            N.println(input);
            assertFalse(NumberUtils.isParsable(input));
            N.println(Integer.parseInt("010"));
            assertFalse(isParsable(input));
        }

        @Test
        @DisplayName("isDigits should validate digit-only strings")
        public void testIsDigits() {
            assertTrue(isDigits("12345"));
            assertFalse(isDigits("123.45"));
            assertFalse(isDigits("-123"));
            assertFalse(isDigits(""));
            assertFalse(isDigits(null));
        }
    }

    @Nested
    @DisplayName("Mathematical Operations Tests")
    public class MathematicalOperationsTests {

        @ParameterizedTest
        @CsvSource({ "2, true", "3, true", "4, false", "17, true", "25, false", "97, true" })
        @DisplayName("isPrime should correctly identify prime numbers")
        public void testIsPrime(long number, boolean expected) {
            assertEquals(expected, isPrime(number));
        }

        @Test
        @DisplayName("isPrime should handle edge cases")
        public void testIsPrimeEdgeCases() {
            assertFalse(isPrime(0));
            assertFalse(isPrime(1));
            assertThrows(IllegalArgumentException.class, () -> isPrime(-1));
        }

        @ParameterizedTest
        @CsvSource({ "1, true", "4, true", "9, true", "16, true", "15, false", "17, false" })
        @DisplayName("isPerfectSquare should identify perfect squares")
        public void testIsPerfectSquare(int number, boolean expected) {
            assertEquals(expected, isPerfectSquare(number));
        }

        @ParameterizedTest
        @CsvSource({ "1, true", "2, true", "4, true", "8, true", "3, false", "5, false" })
        @DisplayName("isPowerOfTwo should identify powers of two")
        public void testIsPowerOfTwo(int number, boolean expected) {
            assertEquals(expected, isPowerOfTwo(number));
        }

        @Test
        @DisplayName("isPowerOfTwo with BigInteger")
        public void testIsPowerOfTwoBigInteger() {
            assertTrue(isPowerOfTwo(BigInteger.valueOf(1024)));
            assertFalse(isPowerOfTwo(BigInteger.valueOf(1023)));
            assertThrows(IllegalArgumentException.class, () -> isPowerOfTwo((BigInteger) null));
        }

        @ParameterizedTest
        @CsvSource({ "12, 8, 4", "15, 10, 5", "7, 3, 1", "0, 5, 5", "5, 0, 5" })
        @DisplayName("gcd should compute greatest common divisor")
        public void testGcd(int a, int b, int expected) {
            assertEquals(expected, gcd(a, b));
        }

        @ParameterizedTest
        @CsvSource({ "4, 6, 12", "3, 7, 21", "12, 8, 24", "0, 5, 0", "5, 0, 0", "10, -2, 10" })
        @DisplayName("lcm should compute least common multiple")
        public void testLcm(int a, int b, int expected) {
            N.println(a + ", " + b + ", " + expected);
            assertEquals(expected, lcm(a, b));
        }
    }

    @Nested
    @DisplayName("Logarithm and Power Tests")
    public class LogarithmPowerTests {

        @Test
        @DisplayName("log2 with rounding modes")
        public void testLog2() {
            assertEquals(3, log2(8, RoundingMode.UNNECESSARY));
            assertEquals(2, log2(7, RoundingMode.FLOOR));
            assertEquals(4, log2(9, RoundingMode.CEILING));

            assertThrows(ArithmeticException.class, () -> log2(7, RoundingMode.UNNECESSARY));
            assertThrows(IllegalArgumentException.class, () -> log2(0, RoundingMode.FLOOR));
        }

        @Test
        @DisplayName("log10 with rounding modes")
        public void testLog10() {
            assertEquals(2, log10(100, RoundingMode.UNNECESSARY));
            assertEquals(1, log10(99, RoundingMode.FLOOR));
            assertEquals(3, log10(101, RoundingMode.CEILING));
        }

        @Test
        @DisplayName("pow should compute powers correctly")
        public void testPow() {
            assertEquals(8, pow(2, 3));
            assertEquals(1, pow(5, 0));
            assertEquals(1, pow(1, 100));
            assertEquals(1, pow(-1, 2));
            assertEquals(-1, pow(-1, 3));
        }

        @Test
        @DisplayName("pow should handle edge cases")
        public void testPowEdgeCases() {
            assertEquals(0, pow(0, 5));
            assertEquals(1, pow(0, 0));
            assertThrows(IllegalArgumentException.class, () -> pow(2, -1));
        }

        @Test
        @DisplayName("powExact should detect overflow")
        public void testPowExact() {
            assertEquals(8, powExact(2, 3));
            assertThrows(ArithmeticException.class, () -> powExact(2, 31));
        }
    }

    @Nested
    @DisplayName("Rounding Tests")
    public class RoundingTests {

        @Test
        @DisplayName("round with scale")
        public void testRoundWithScale() {
            assertEquals(12.35, round(12.3456, 2), 1e-10);
            assertEquals(12.0, round(12.3456, 0), 1e-10);
            assertEquals(12.346, round(12.3456, 3), 1e-10);
        }

        @Test
        @DisplayName("round with RoundingMode")
        public void testRoundWithRoundingMode() {
            assertEquals(12.34, round(12.3456, 2, RoundingMode.DOWN), 1e-10);
            assertEquals(12.35, round(12.3456, 2, RoundingMode.UP), 1e-10);
            assertEquals(12.35, round(12.345, 2, RoundingMode.HALF_UP), 1e-10);
            assertEquals(12.34, round(12.345, 2, RoundingMode.HALF_DOWN), 1e-10);
        }

        @Test
        @DisplayName("round with DecimalFormat")
        public void testRoundWithDecimalFormat() {
            assertEquals(12.35, round(12.3456, "#.##"), 1e-10);
            assertEquals(12.0, round(12.3456, "#"), 1e-10);
        }

        @Test
        @DisplayName("roundToInt should convert to integer")
        public void testRoundToInt() {
            assertEquals(12, roundToInt(12.3, RoundingMode.DOWN));
            assertEquals(13, roundToInt(12.7, RoundingMode.UP));
            assertEquals(13, roundToInt(12.5, RoundingMode.HALF_UP));
            assertEquals(12, roundToInt(12.5, RoundingMode.HALF_DOWN));
        }

        @Test
        @DisplayName("roundToInt should handle overflow")
        public void testRoundToIntOverflow() {
            assertThrows(ArithmeticException.class, () -> roundToInt(Double.MAX_VALUE, RoundingMode.DOWN));
            assertThrows(ArithmeticException.class, () -> roundToInt(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
            assertThrows(ArithmeticException.class, () -> roundToInt(Double.NaN, RoundingMode.DOWN));
        }
    }

    @Nested
    @DisplayName("Statistical Functions Tests")
    public class StatisticalTests {

        @Test
        @DisplayName("mean of integers")
        public void testMeanInts() {
            assertEquals(3.0, mean(1, 2, 3, 4, 5), 1e-10);
            assertEquals(0.0, mean(0), 1e-10);
            assertEquals(-1.0, mean(-2, 0, -1), 1e-10);
        }

        @Test
        @DisplayName("mean of longs")
        public void testMeanLongs() {
            assertEquals(3.0, mean(1L, 2L, 3L, 4L, 5L), 1e-10);
            assertEquals(Long.MAX_VALUE, mean(Long.MAX_VALUE), 1e-10);
        }

        @Test
        @DisplayName("mean of doubles")
        public void testMeanDoubles() {
            assertEquals(2.5, mean(1.0, 2.0, 3.0, 4.0), 1e-10);
            assertThrows(IllegalArgumentException.class, () -> mean(new double[0]));
        }

        @Test
        @DisplayName("mean should handle special values")
        public void testMeanSpecialValues() {
            assertThrows(IllegalArgumentException.class, () -> mean(Double.NaN, 1.0, 2.0));
            assertThrows(IllegalArgumentException.class, () -> mean(Double.POSITIVE_INFINITY, 1.0));
        }

        @Test
        @DisplayName("mean of two values")
        public void testMeanTwo() {
            assertEquals(1, mean(1, 2), 1e-10);
            assertEquals(0.0, mean(-1, 1), 1e-10);
            assertEquals(1.5, mean(1.0, 2.0), 1e-10);
        }

        @Test
        @DisplayName("fuzzyEquals should handle tolerance")
        public void testFuzzyEquals() {
            assertTrue(fuzzyEquals(1.0, 1.0001, 0.001));
            assertFalse(fuzzyEquals(1.0, 1.01, 0.001));
            assertTrue(fuzzyEquals(Double.NaN, Double.NaN, 0.1));
            assertTrue(fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1));
        }

        @Test
        @DisplayName("fuzzyCompare should order with tolerance")
        public void testFuzzyCompare() {
            assertEquals(0, fuzzyCompare(1.0, 1.0001, 0.001));
            assertTrue(fuzzyCompare(1.0, 2.0, 0.1) < 0);
            assertTrue(fuzzyCompare(2.0, 1.0, 0.1) > 0);
        }
    }

    @Nested
    @DisplayName("Exact Arithmetic Tests")
    public class ExactArithmeticTests {

        @Test
        @DisplayName("addExact should detect overflow")
        public void testAddExact() {
            assertEquals(5, addExact(2, 3));
            assertThrows(ArithmeticException.class, () -> addExact(Integer.MAX_VALUE, 1));
            assertThrows(ArithmeticException.class, () -> addExact(Integer.MIN_VALUE, -1));
        }

        @Test
        @DisplayName("subtractExact should detect overflow")
        public void testSubtractExact() {
            assertEquals(-1, subtractExact(2, 3));
            assertThrows(ArithmeticException.class, () -> subtractExact(Integer.MIN_VALUE, 1));
            assertThrows(ArithmeticException.class, () -> subtractExact(Integer.MAX_VALUE, -1));
        }

        @Test
        @DisplayName("multiplyExact should detect overflow")
        public void testMultiplyExact() {
            assertEquals(6, multiplyExact(2, 3));
            assertThrows(ArithmeticException.class, () -> multiplyExact(Integer.MAX_VALUE, 2));
            assertThrows(ArithmeticException.class, () -> multiplyExact(Integer.MIN_VALUE, 2));
        }

        @Test
        @DisplayName("saturatedAdd should not overflow")
        public void testSaturatedAdd() {
            assertEquals(5, saturatedAdd(2, 3));
            assertEquals(Integer.MAX_VALUE, saturatedAdd(Integer.MAX_VALUE, 1));
            assertEquals(Integer.MIN_VALUE, saturatedAdd(Integer.MIN_VALUE, -1));
        }

        @Test
        @DisplayName("saturatedMultiply should not overflow")
        public void testSaturatedMultiply() {
            assertEquals(6, saturatedMultiply(2, 3));
            assertEquals(Integer.MAX_VALUE, saturatedMultiply(Integer.MAX_VALUE, 2));
            assertEquals(Integer.MIN_VALUE, saturatedMultiply(Integer.MAX_VALUE, -2));
        }
    }

    @Nested
    @DisplayName("Factorial and Combinatorial Tests")
    public class FactorialTests {

        @Test
        @DisplayName("factorial should compute correctly")
        public void testFactorial() {
            assertEquals(1, factorial(0));
            assertEquals(1, factorial(1));
            assertEquals(2, factorial(2));
            assertEquals(6, factorial(3));
            assertEquals(24, factorial(4));
            assertEquals(120, factorial(5));
        }

        @Test
        @DisplayName("factorial should handle large values")
        public void testFactorialLarge() {
            assertEquals(Integer.MAX_VALUE, factorial(20));
            assertThrows(IllegalArgumentException.class, () -> factorial(-1));
        }

        @Test
        @DisplayName("factorialToLong should compute correctly")
        public void testFactorialToLong() {
            assertEquals(1L, factorialToLong(0));
            assertEquals(120L, factorialToLong(5));
            assertEquals(3628800L, factorialToLong(10));
        }

        @Test
        @DisplayName("factorialToDouble should compute correctly")
        public void testFactorialToDouble() {
            assertEquals(1.0, factorialToDouble(0), 1e-10);
            assertEquals(120.0, factorialToDouble(5), 1e-10);
            assertEquals(Double.POSITIVE_INFINITY, factorialToDouble(200));
        }

        @Test
        @DisplayName("factorialToBigInteger should handle large values")
        public void testFactorialToBigInteger() {
            assertEquals(BigInteger.ONE, factorialToBigInteger(0));
            assertEquals(BigInteger.valueOf(120), factorialToBigInteger(5));

            BigInteger result = factorialToBigInteger(100);
            assertNotNull(result);
            assertTrue(result.compareTo(BigInteger.ZERO) > 0);
        }

        @Test
        @DisplayName("binomial should compute combinations")
        public void testBinomial() {
            assertEquals(1, binomial(5, 0));
            assertEquals(5, binomial(5, 1));
            assertEquals(10, binomial(5, 2));
            assertEquals(10, binomial(5, 3));
            assertEquals(5, binomial(5, 4));
            assertEquals(1, binomial(5, 5));
        }

        @Test
        @DisplayName("binomial should handle edge cases")
        public void testBinomialEdgeCases() {
            assertThrows(IllegalArgumentException.class, () -> binomial(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> binomial(5, -1));
            assertThrows(IllegalArgumentException.class, () -> binomial(3, 5));
        }
    }

    @Nested
    @DisplayName("Modular Arithmetic Tests")
    public class ModularArithmeticTests {

        @Test
        @DisplayName("mod should compute positive remainder")
        public void testMod() {
            assertEquals(1, mod(7, 3));
            assertEquals(2, mod(-7, 3));
            assertEquals(0, mod(6, 3));
            assertEquals(1, mod(-8, 3));
        }

        @Test
        @DisplayName("mod should handle edge cases")
        public void testModEdgeCases() {
            assertThrows(ArithmeticException.class, () -> mod(5, 0));
            assertThrows(ArithmeticException.class, () -> mod(5, -1));
        }

        @Test
        @DisplayName("divide with rounding modes")
        public void testDivide() {
            assertEquals(2, divide(7, 3, RoundingMode.DOWN));
            assertEquals(3, divide(7, 3, RoundingMode.UP));
            assertEquals(2, divide(7, 3, RoundingMode.HALF_DOWN));
            assertEquals(3, divide(8, 3, RoundingMode.HALF_UP));

            assertThrows(ArithmeticException.class, () -> divide(7, 0, RoundingMode.DOWN));
            assertThrows(ArithmeticException.class, () -> divide(7, 3, RoundingMode.UNNECESSARY));
        }

        @Test
        @DisplayName("divide should handle exact division")
        public void testDivideExact() {
            assertEquals(2, divide(6, 3, RoundingMode.UNNECESSARY));
            assertEquals(-2, divide(-6, 3, RoundingMode.UNNECESSARY));
        }
    }

    @Nested
    @DisplayName("Square Root Tests")
    public class SquareRootTests {

        @Test
        @DisplayName("sqrt with rounding modes")
        public void testSqrt() {
            assertEquals(3, sqrt(9, RoundingMode.UNNECESSARY));
            assertEquals(3, sqrt(10, RoundingMode.DOWN));
            assertEquals(4, sqrt(10, RoundingMode.UP));
            assertEquals(3, sqrt(10, RoundingMode.HALF_DOWN));
            assertEquals(3, sqrt(11, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("sqrt should handle edge cases")
        public void testSqrtEdgeCases() {
            assertEquals(0, sqrt(0, RoundingMode.DOWN));
            assertThrows(IllegalArgumentException.class, () -> sqrt(-1, RoundingMode.DOWN));
            assertThrows(ArithmeticException.class, () -> sqrt(10, RoundingMode.UNNECESSARY));
        }

        @Test
        @DisplayName("sqrt for long values")
        public void testSqrtLong() {
            assertEquals(1000L, sqrt(1000000L, RoundingMode.UNNECESSARY));
            assertEquals(1000L, sqrt(1000001L, RoundingMode.DOWN));
            assertEquals(1001L, sqrt(1000001L, RoundingMode.UP));
        }

        @Test
        @DisplayName("sqrt for BigInteger")
        public void testSqrtBigInteger() {
            BigInteger nine = BigInteger.valueOf(9);
            assertEquals(BigInteger.valueOf(3), sqrt(nine, RoundingMode.UNNECESSARY));

            BigInteger ten = BigInteger.valueOf(10);
            assertEquals(BigInteger.valueOf(3), sqrt(ten, RoundingMode.DOWN));
            assertEquals(BigInteger.valueOf(4), sqrt(ten, RoundingMode.UP));
        }
    }

    @Nested
    @DisplayName("Scaling and BigDecimal Tests")
    public class ScalingTests {

        @Test
        @DisplayName("toScaledBigDecimal with default parameters")
        public void testToScaledBigDecimalDefault() {
            assertEquals(new BigDecimal("12.35"), toScaledBigDecimal(12.3456));
            assertEquals(BigDecimal.ZERO, toScaledBigDecimal((BigDecimal) null));
        }

        @Test
        @DisplayName("toScaledBigDecimal with custom scale and rounding")
        public void testToScaledBigDecimalCustom() {
            assertEquals(new BigDecimal("12.346"), toScaledBigDecimal(12.3456, 3, RoundingMode.HALF_UP));
            assertEquals(new BigDecimal("12.345"), toScaledBigDecimal(12.3456, 3, RoundingMode.DOWN));
        }

        @Test
        @DisplayName("toScaledBigDecimal from String")
        public void testToScaledBigDecimalFromString() {
            assertEquals(new BigDecimal("12.35"), toScaledBigDecimal("12.3456"));
            assertEquals(BigDecimal.ZERO, toScaledBigDecimal((String) null));
        }

        @Test
        @DisplayName("toScaledBigDecimal from Float and Double")
        public void testToScaledBigDecimalFromFloatDouble() {
            assertEquals(new BigDecimal("12.35"), toScaledBigDecimal(12.3456f));
            assertEquals(new BigDecimal("12.35"), toScaledBigDecimal(12.3456d));
        }

        @Test
        @DisplayName("toDouble from BigDecimal")
        public void testToDoubleFromBigDecimal() {
            assertEquals(12.3456, toDouble(new BigDecimal("12.3456")), 1e-10);
            assertEquals(0.0, toDouble((BigDecimal) null), 1e-10);
            assertEquals(99.0, toDouble((BigDecimal) null, 99.0), 1e-10);
        }
    }

    @Nested
    @DisplayName("Power and Ceiling/Floor Tests")
    public class PowerCeilingFloorTests {

        @Test
        @DisplayName("ceilingPowerOfTwo should find next power of two")
        public void testCeilingPowerOfTwo() {
            assertEquals(8, ceilingPowerOfTwo(5));
            assertEquals(8, ceilingPowerOfTwo(8));
            assertEquals(16, ceilingPowerOfTwo(9));
            assertEquals(1, ceilingPowerOfTwo(1));
        }

        @Test
        @DisplayName("ceilingPowerOfTwo should handle edge cases")
        public void testCeilingPowerOfTwoEdgeCases() {
            assertThrows(IllegalArgumentException.class, () -> ceilingPowerOfTwo(0));
            assertThrows(ArithmeticException.class, () -> ceilingPowerOfTwo(Long.MAX_VALUE));
        }

        @Test
        @DisplayName("floorPowerOfTwo should find largest power of two")
        public void testFloorPowerOfTwo() {
            assertEquals(4, floorPowerOfTwo(5));
            assertEquals(8, floorPowerOfTwo(8));
            assertEquals(8, floorPowerOfTwo(15));
            assertEquals(1, floorPowerOfTwo(1));
        }

        @Test
        @DisplayName("ceilingPowerOfTwo for BigInteger")
        public void testCeilingPowerOfTwoBigInteger() {
            assertEquals(BigInteger.valueOf(8), ceilingPowerOfTwo(BigInteger.valueOf(5)));
            assertEquals(BigInteger.valueOf(8), ceilingPowerOfTwo(BigInteger.valueOf(8)));
        }

        @Test
        @DisplayName("floorPowerOfTwo for BigInteger")
        public void testFloorPowerOfTwoBigInteger() {
            assertEquals(BigInteger.valueOf(4), floorPowerOfTwo(BigInteger.valueOf(5)));
            assertEquals(BigInteger.valueOf(8), floorPowerOfTwo(BigInteger.valueOf(8)));
        }
    }

    @Nested
    @DisplayName("Hyperbolic Functions Tests")
    public class HyperbolicFunctionsTests {

        @Test
        @DisplayName("asinh should compute inverse hyperbolic sine")
        public void testAsinh() {
            assertEquals(0.0, asinh(0.0), 1e-10);
            assertEquals(-asinh(1.0), asinh(-1.0), 1e-10);

            double x = 1.0;
            double result = asinh(x);
            assertEquals(x, Math.sinh(result), 1e-10);
        }

        @Test
        @DisplayName("acosh should compute inverse hyperbolic cosine")
        public void testAcosh() {
            assertEquals(0.0, acosh(1.0), 1e-10);

            double x = 2.0;
            double result = acosh(x);
            assertEquals(x, Math.cosh(result), 1e-10);
        }

        @Test
        @DisplayName("atanh should compute inverse hyperbolic tangent")
        public void testAtanh() {
            assertEquals(0.0, atanh(0.0), 1e-10);
            assertEquals(-atanh(0.5), atanh(-0.5), 1e-10);

            double x = 0.5;
            double result = atanh(x);
            assertEquals(x, Math.tanh(result), 1e-10);
        }

        @Test
        @DisplayName("hyperbolic functions should handle edge cases")
        public void testHyperbolicEdgeCases() {
            assertNotEquals(Double.NaN, asinh(Double.MAX_VALUE));
            assertNotEquals(Double.NaN, asinh(-Double.MAX_VALUE));

            assertTrue(Double.isNaN(acosh(0.5)));

            assertEquals(Double.POSITIVE_INFINITY, atanh(1.0), 1e-10);
            assertEquals(Double.NEGATIVE_INFINITY, atanh(-1.0), 1e-10);
            assertTrue(Double.isNaN(atanh(1.5)));
        }
    }

    @Nested
    @DisplayName("Utility and Helper Function Tests")
    public class UtilityTests {

        @Test
        @DisplayName("isMathematicalInteger should identify integers")
        public void testIsMathematicalInteger() {
            assertTrue(isMathematicalInteger(42.0));
            assertTrue(isMathematicalInteger(-42.0));
            assertTrue(isMathematicalInteger(0.0));
            assertFalse(isMathematicalInteger(42.5));
            assertFalse(isMathematicalInteger(Double.NaN));
            assertFalse(isMathematicalInteger(Double.POSITIVE_INFINITY));
        }

        @Test
        @DisplayName("saturatedCast should clamp to int range")
        public void testSaturatedCast() {
            assertEquals(42, saturatedCast(42L));
            assertEquals(Integer.MAX_VALUE, saturatedCast(Long.MAX_VALUE));
            assertEquals(Integer.MIN_VALUE, saturatedCast(Long.MIN_VALUE));
        }

        @Test
        @DisplayName("toIntExact should detect overflow")
        public void testToIntExact() {
            assertEquals(42, toIntExact(42L));
            assertThrows(ArithmeticException.class, () -> toIntExact(Long.MAX_VALUE));
            assertThrows(ArithmeticException.class, () -> toIntExact(Long.MIN_VALUE));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    public class EdgeCasesTests {

        @Test
        @DisplayName("should handle null inputs appropriately")
        public void testNullInputs() {
            assertEquals(0, toByte((String) null));
            assertEquals(0, toInt((String) null));
            assertEquals(0L, toLong((String) null));
            assertEquals(0.0f, toFloat((String) null), 1e-10f);
            assertEquals(0.0, toDouble((String) null), 1e-10);

            assertNull(createInteger(null));
            assertNull(createLong(null));
            assertNull(createFloat(null));
            assertNull(createDouble(null));
            assertNull(createBigInteger(null));
            assertNull(createBigDecimal(null));
            assertNull(createNumber(null));
        }

        @Test
        @DisplayName("should handle empty strings appropriately")
        public void testEmptyStrings() {
            assertEquals(0, toByte(""));
            assertEquals(0, toInt(""));
            assertEquals(0L, toLong(""));
            assertEquals(0.0f, toFloat(""), 1e-10f);
            assertEquals(0.0, toDouble(""), 1e-10);

            assertFalse(isCreatable(""));
            assertFalse(isParsable(""));
        }

        @Test
        @DisplayName("should handle special floating point values")
        public void testSpecialFloatingPointValues() {
            assertTrue(Float.isNaN(toFloat("NaN")));
            assertTrue(Double.isNaN(toDouble("NaN")));

            assertEquals(Float.POSITIVE_INFINITY, toFloat("Infinity"));
            assertEquals(Float.NEGATIVE_INFINITY, toFloat("-Infinity"));
            assertEquals(Double.POSITIVE_INFINITY, toDouble("Infinity"));
            assertEquals(Double.NEGATIVE_INFINITY, toDouble("-Infinity"));

            assertEquals("NaN", Numbers.format(Float.NaN, "#.##"));
            assertEquals("∞", Numbers.format(Float.POSITIVE_INFINITY, "#.##"));
        }

        @Test
        @DisplayName("should validate argument ranges")
        public void testArgumentValidation() {
            assertThrows(IllegalArgumentException.class, () -> factorial(-1));
            assertThrows(IllegalArgumentException.class, () -> pow(2, -1));
            assertThrows(IllegalArgumentException.class, () -> round(1.5, -1));
            assertThrows(IllegalArgumentException.class, () -> fuzzyEquals(1.0, 2.0, -0.1));

            assertThrows(IllegalArgumentException.class, () -> binomial(3, 5));
            assertThrows(ArithmeticException.class, () -> gcd(0, Integer.MIN_VALUE));
            assertThrows(ArithmeticException.class, () -> gcd(Long.MIN_VALUE, 0));
        }

        @Test
        @DisplayName("should handle very large numbers")
        public void testVeryLargeNumbers() {
            BigInteger veryLarge = new BigInteger("123456789012345678901234567890");
            assertNotNull(sqrt(veryLarge, RoundingMode.DOWN));

            assertThrows(ArithmeticException.class, () -> multiplyExact(Integer.MAX_VALUE, Integer.MAX_VALUE));

            assertDoesNotThrow(() -> saturatedMultiply(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("should handle precision edge cases")
        public void testPrecisionEdgeCases() {
            double verySmall = 1e-100;
            assertEquals(0.0f, convert(verySmall, float.class), 1e-10f);

            double almostHalf = 0.4999999999999999;
            assertEquals(0, roundToInt(almostHalf, RoundingMode.HALF_UP));

            assertTrue(fuzzyEquals(1.0, 1.0 + 1e-16, 1e-15));
            assertFalse(fuzzyEquals(1.0, 1.0 + 1e-14, 1e-15));
        }
    }

    @Nested
    @DisplayName("Performance and Optimization Tests")
    public class PerformanceTests {

        @Test
        @DisplayName("string to int conversion should use cache for small values")
        public void testStringIntCache() {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                toInt("42");
                toInt("1");
                toInt("-1");
                toInt("100");
            }
            long elapsed = System.nanoTime() - start;

            Assertions.assertTrue(elapsed < 10_000_000, "String to int conversion too slow: " + elapsed + "ns");
        }

        @Test
        @DisplayName("prime testing should be efficient for known ranges")
        public void testPrimePerformance() {
            long start = System.nanoTime();
            int primeCount = 0;
            for (int i = 2; i < 10000; i++) {
                if (isPrime(i)) {
                    primeCount++;
                }
            }
            long elapsed = System.nanoTime() - start;

            assertEquals(1229, primeCount);
            Assertions.assertTrue(elapsed < 100_000_000, "Prime testing too slow: " + elapsed + "ns");
        }

        @Test
        @DisplayName("factorial computation should handle large inputs efficiently")
        public void testFactorialPerformance() {
            assertTimeout(java.time.Duration.ofSeconds(1), () -> {
                factorialToBigInteger(1000);
            });
        }
    }

    static Stream<Arguments> provideNumberFormats() {
        return Stream.of(Arguments.of(12.345, "#.##", "12.35"), Arguments.of(12.345, "0.00", "12.35"), Arguments.of(0.123, "#.##%", "12.3%"),
                Arguments.of(12345, "#,###", "12,345"), Arguments.of(0.0, "#.##", "0"), Arguments.of(-12.345, "#.##", "-12.35"));
    }

    static Stream<Arguments> provideValidNumbers() {
        return Stream.of(Arguments.of("42", true), Arguments.of("-42", true), Arguments.of("42.5", true), Arguments.of("1.23e10", true),
                Arguments.of("0xFF", true), Arguments.of("010", true), Arguments.of("42L", true), Arguments.of("42.5f", true), Arguments.of("", false),
                Arguments.of("abc", false), Arguments.of("42.5.6", false));
    }

    static Stream<Arguments> providePrimeNumbers() {
        return Stream.of(Arguments.of(2L, true), Arguments.of(3L, true), Arguments.of(4L, false), Arguments.of(17L, true), Arguments.of(25L, false),
                Arguments.of(97L, true), Arguments.of(982451653L, true), Arguments.of(982451654L, false));
    }
}
