package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.math.IntMath;
import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Numbers200Test extends TestBase {

    private static final double DELTA = 1e-9;

    @Test
    public void testByteConstants() {
        assertEquals((byte) 0, Numbers.BYTE_ZERO);
        assertEquals((byte) 1, Numbers.BYTE_ONE);
        assertEquals((byte) -1, Numbers.BYTE_MINUS_ONE);
    }

    @Test
    public void testShortConstants() {
        assertEquals((short) 0, Numbers.SHORT_ZERO);
        assertEquals((short) 1, Numbers.SHORT_ONE);
        assertEquals((short) -1, Numbers.SHORT_MINUS_ONE);
    }

    @Test
    public void testIntegerConstants() {
        assertEquals(0, Numbers.INTEGER_ZERO);
        assertEquals(1, Numbers.INTEGER_ONE);
        assertEquals(2, Numbers.INTEGER_TWO);
        assertEquals(-1, Numbers.INTEGER_MINUS_ONE);
    }

    @Test
    public void testLongConstants() {
        assertEquals(0L, Numbers.LONG_ZERO);
        assertEquals(1L, Numbers.LONG_ONE);
        assertEquals(-1L, Numbers.LONG_MINUS_ONE);
    }

    @Test
    public void testFloatConstants() {
        assertEquals(0.0f, Numbers.FLOAT_ZERO, 0.0f);
        assertEquals(1.0f, Numbers.FLOAT_ONE, 0.0f);
        assertEquals(-1.0f, Numbers.FLOAT_MINUS_ONE, 0.0f);
    }

    @Test
    public void testDoubleConstants() {
        assertEquals(0.0d, Numbers.DOUBLE_ZERO, 0.0d);
        assertEquals(1.0d, Numbers.DOUBLE_ONE, 0.0d);
        assertEquals(-1.0d, Numbers.DOUBLE_MINUS_ONE, 0.0d);
    }

    @Test
    public void testConvert_nullValue() {
        assertNull(Numbers.convert(null, Integer.class));
        assertEquals((byte) 0, Numbers.convert(null, byte.class));
        assertEquals(0, Numbers.convert(null, int.class));
    }

    @Test
    public void testConvert_byteToOthers() {
        Byte val = (byte) 10;
        assertEquals((byte) 10, Numbers.convert(val, byte.class));
        assertEquals((short) 10, Numbers.convert(val, short.class));
        assertEquals(10, Numbers.convert(val, int.class));
        assertEquals(10L, Numbers.convert(val, long.class));
        assertEquals(10.0f, Numbers.convert(val, float.class), DELTA);
        assertEquals(10.0d, Numbers.convert(val, double.class), DELTA);
        assertEquals(BigInteger.valueOf(10), Numbers.convert(val, BigInteger.class));
        assertEquals(BigDecimal.valueOf(10), Numbers.convert(val, BigDecimal.class));
    }

    @Test
    public void testConvert_intToByte_valid() {
        assertEquals((byte) 100, Numbers.convert(100, byte.class));
    }

    @Test
    public void testConvert_intToByte_overflowPositive() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(300, byte.class));
    }

    @Test
    public void testConvert_intToByte_overflowNegative() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(-129, byte.class));
    }

    @Test
    public void testConvert_longToInt_valid() {
        assertEquals(12345, Numbers.convert(12345L, int.class));
    }

    @Test
    public void testConvert_longToInt_overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Integer.MAX_VALUE + 1L, int.class));
    }

    @Test
    public void testConvert_doubleToLong_valid() {
        assertEquals(123L, Numbers.convert(123.45d, long.class));
    }

    @Test
    public void testConvert_doubleToLong_overflow() {
        assertEquals(Long.MAX_VALUE, Numbers.convert((double) Long.MAX_VALUE + 100.0, long.class));
    }

    @Test
    public void testConvert_bigDecimalToInteger_valid() {
        assertEquals(123, Numbers.convert(new BigDecimal("123.789"), Integer.class));
    }

    @Test
    public void testConvert_bigDecimalToInteger_overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal(Long.MAX_VALUE), Integer.class));
    }

    @Test
    public void testConvert_floatToInteger_NaN() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.NaN, int.class));
    }

    @Test
    public void testConvert_doubleToFloat_Infinity() {
        assertEquals(Float.POSITIVE_INFINITY, Numbers.convert(Double.POSITIVE_INFINITY, float.class));
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(Double.NEGATIVE_INFINITY, float.class));
    }

    @Test
    public void testConvert_doubleToFloat_NaN() {
        assertEquals(Float.NaN, Numbers.convert(Double.NaN, float.class));
    }

    @Test
    public void testConvert_doubleToFloat_overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.MAX_VALUE, float.class));
    }

    static Stream<Arguments> formatIntegerProvider() {
        return Stream.of(Arguments.of(123, "0000", "0123"), Arguments.of(123, "#,##0", "123"), Arguments.of(12345, "#,##0", "12,345"),
                Arguments.of(-123, "0.00", "-123.00"));
    }

    @ParameterizedTest
    @MethodSource("formatIntegerProvider")
    public void testFormat_int(int val, String pattern, String expected) {
        assertEquals(expected, Numbers.format(val, pattern));
    }

    @ParameterizedTest
    @MethodSource("formatIntegerProvider")
    public void testFormat_Integer(int val, String pattern, String expected) {
        assertEquals(expected, Numbers.format(Integer.valueOf(val), pattern));
    }

    @Test
    public void testFormat_Integer_null() {
        assertEquals("0", Numbers.format((Integer) null, "#"));
        assertEquals("0.00", Numbers.format((Integer) null, "0.00"));
    }

    static Stream<Arguments> formatLongProvider() {
        return Stream.of(Arguments.of(123L, "0000", "0123"), Arguments.of(1234567890L, "#,##0", "1,234,567,890"));
    }

    @ParameterizedTest
    @MethodSource("formatLongProvider")
    public void testFormat_long(long val, String pattern, String expected) {
        assertEquals(expected, Numbers.format(val, pattern));
    }

    @ParameterizedTest
    @MethodSource("formatLongProvider")
    public void testFormat_Long(long val, String pattern, String expected) {
        assertEquals(expected, Numbers.format(Long.valueOf(val), pattern));
    }

    @Test
    public void testFormat_Long_null() {
        assertEquals("0.0", Numbers.format((Long) null, "0.0"));
    }

    static Stream<Arguments> formatFloatProvider() {
        return Stream.of(Arguments.of(123.456f, "0.00", "123.46"), Arguments.of(0.75f, "#.##%", "75%"), Arguments.of(0.12156f, "#.##%", "12.16%"));
    }

    @ParameterizedTest
    @MethodSource("formatFloatProvider")
    public void testFormat_float(float val, String pattern, String expected) {
        DecimalFormat df = new DecimalFormat(pattern);
        assertEquals(df.format(val), Numbers.format(val, pattern));
    }

    @ParameterizedTest
    @MethodSource("formatFloatProvider")
    public void testFormat_Float(float val, String pattern, String expected) {
        DecimalFormat df = new DecimalFormat(pattern);
        assertEquals(df.format(val), Numbers.format(Float.valueOf(val), pattern));
    }

    @Test
    public void testFormat_Float_null() {
        assertEquals("0.00", Numbers.format((Float) null, "0.00"));
    }

    static Stream<Arguments> formatDoubleProvider() {
        return Stream.of(Arguments.of(123.4567, "0.00", "123.46"), Arguments.of(0.12156, "#.##%", "12.16%"), Arguments.of(Double.NaN, "0.0", "\uFFFD"),
                Arguments.of(Double.POSITIVE_INFINITY, "0.0", "\u221E"));
    }

    @ParameterizedTest
    @MethodSource("formatDoubleProvider")
    public void testFormat_double(double val, String pattern, String expected) {
        DecimalFormat df = new DecimalFormat(pattern);
        assertEquals(df.format(val), Numbers.format(val, pattern));
    }

    @ParameterizedTest
    @MethodSource("formatDoubleProvider")
    public void testFormat_Double(double val, String pattern, String expected) {
        DecimalFormat df = new DecimalFormat(pattern);
        assertEquals(df.format(val), Numbers.format(Double.valueOf(val), pattern));
    }

    @Test
    public void testFormat_Double_null() {
        assertEquals("0.00", Numbers.format((Double) null, "0.00"));
    }

    @Test
    public void testFormat_nullDecimalFormat() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(123, null));
    }

    @Test
    public void testExtractFirstInt() {
        assertEquals(123, Numbers.extractFirstInt("abc 123 def 456"));
        assertEquals(0, Numbers.extractFirstInt("abc def"));
        assertEquals(0, Numbers.extractFirstInt(null));
        assertEquals(0, Numbers.extractFirstInt(""));
        assertEquals(-45, Numbers.extractFirstInt("xyz -45 abc"));
    }

    @Test
    public void testExtractFirstInt_withDefault() {
        assertEquals(123, Numbers.extractFirstInt("abc 123 def", 99));
        assertEquals(99, Numbers.extractFirstInt("abc def", 99));
        assertEquals(99, Numbers.extractFirstInt(null, 99));
    }

    @Test
    public void testExtractFirstLong() {
        assertEquals(1234567890L, Numbers.extractFirstLong("abc 1234567890 def"));
        assertEquals(0L, Numbers.extractFirstLong("abc def"));
        assertEquals(-987L, Numbers.extractFirstLong("word -987 test"));
    }

    @Test
    public void testExtractFirstLong_withDefault() {
        assertEquals(123L, Numbers.extractFirstLong("abc 123 def", 999L));
        assertEquals(999L, Numbers.extractFirstLong("abc def", 999L));
    }

    @Test
    public void testExtractFirstDouble() {
        assertEquals(123.45, Numbers.extractFirstDouble("abc 123.45 def"), DELTA);
        assertEquals(0.0, Numbers.extractFirstDouble("abc def"), DELTA);
        assertEquals(-0.5, Numbers.extractFirstDouble("neg -0.5 test"), DELTA);
        assertEquals(1.2e3, Numbers.extractFirstDouble("sci 1.2e3 end", true), DELTA);
        assertEquals(1.2, Numbers.extractFirstDouble("sci 1.2e3 end", false), DELTA);
    }

    @Test
    public void testExtractFirstDouble_withDefault() {
        assertEquals(123.45, Numbers.extractFirstDouble("abc 123.45 def", 9.9), DELTA);
        assertEquals(9.9, Numbers.extractFirstDouble("abc def", 9.9), DELTA);
        assertEquals(1.2E-5, Numbers.extractFirstDouble("num 1.2E-5 text", 0.0, true), DELTA);
        assertEquals(0.000012, Numbers.extractFirstDouble("num 0.000012E0 text", 0.0, true), DELTA);
    }

    @Test
    public void testToByte() {
        assertEquals((byte) 10, Numbers.toByte("10"));
        assertEquals((byte) 0, Numbers.toByte(null));
        assertEquals((byte) 0, Numbers.toByte(""));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("abc", (byte) 5));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("128"));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("abc"));

        assertEquals((byte) 10, Numbers.toByte(Byte.valueOf((byte) 10)));
        assertEquals((byte) 10, Numbers.toByte(10));
        assertEquals((byte) 0, Numbers.toByte(null, (byte) 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte(300, (byte) 0));
    }

    @Test
    public void testToShort() {
        assertEquals((short) 1000, Numbers.toShort("1000"));
        assertEquals((short) 0, Numbers.toShort(null));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("xyz", (short) -5));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("32768"));

        assertEquals((short) 100, Numbers.toShort(Short.valueOf((short) 100)));
        assertEquals((short) 100, Numbers.toShort(100));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort(40000, (short) 0));
    }

    @Test
    public void testToInt() {
        assertEquals(100000, Numbers.toInt("100000"));
        assertEquals(0, Numbers.toInt(null));
        assertEquals(77, Numbers.toInt("", 77));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("2147483648"));

        assertEquals(1000, Numbers.toInt(Integer.valueOf(1000)));
        assertEquals(1000, Numbers.toInt(1000L));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt(3000000000L, 0));
    }

    @Test
    public void testToLong() {
        assertEquals(123456789012L, Numbers.toLong("123456789012"));
        assertEquals(0L, Numbers.toLong(null));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong("test", 88L));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong("9223372036854775808"));
        assertEquals(123L, Numbers.toLong("123L"));
        assertEquals(123L, Numbers.toLong("123l"));

        assertEquals(500L, Numbers.toLong(Long.valueOf(500L)));
        assertEquals(500L, Numbers.toLong(500));
        assertEquals(500L, Numbers.toLong(new BigInteger("500")));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong(new BigInteger(Long.MAX_VALUE + "1"), 0L));
    }

    @Test
    public void testToFloat() {
        assertEquals(123.45f, Numbers.toFloat("123.45"), DELTA);
        assertEquals(0.0f, Numbers.toFloat(null), DELTA);
        assertThrows(NumberFormatException.class, () -> Numbers.toFloat("invalid"));

        assertEquals(12.3f, Numbers.toFloat(Float.valueOf(12.3f)), DELTA);
        assertEquals(12.3f, Numbers.toFloat(12.3d), DELTA);
    }

    @Test
    public void testToDouble() {
        assertEquals(123.4567, Numbers.toDouble("123.4567"), DELTA);
        assertEquals(0.0d, Numbers.toDouble((String) null), DELTA);
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble("pi", 3.14));
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble("bad"));

        assertEquals(45.67, Numbers.toDouble(Double.valueOf(45.67)), DELTA);
        assertEquals(45.67, Numbers.toDouble(45.67f), DELTA);

        assertEquals(8.5d, Numbers.toDouble(BigDecimal.valueOf(8.5d)), DELTA);
        assertEquals(0.0d, Numbers.toDouble((BigDecimal) null), DELTA);
        assertEquals(1.1d, Numbers.toDouble((String) null, 1.1d), DELTA);

    }

    @Test
    public void testToScaledBigDecimal_BigDecimal() {
        assertEquals(new BigDecimal("12.34"), Numbers.toScaledBigDecimal(new BigDecimal("12.345")));
        assertEquals(new BigDecimal("12.3"), Numbers.toScaledBigDecimal(new BigDecimal("12.345"), 1, RoundingMode.HALF_DOWN));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((BigDecimal) null));
    }

    @Test
    public void testToScaledBigDecimal_Float() {
        assertEquals(new BigDecimal("5.68"), Numbers.toScaledBigDecimal(5.678f));
        assertEquals(new BigDecimal("5.7"), Numbers.toScaledBigDecimal(5.678f, 1, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Float) null));
    }

    @Test
    public void testToScaledBigDecimal_Double() {
        assertEquals(new BigDecimal("9.88"), Numbers.toScaledBigDecimal(9.8765d));
        assertEquals(new BigDecimal("9.877"), Numbers.toScaledBigDecimal(9.8765d, 3, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Double) null));
    }

    @Test
    public void testToScaledBigDecimal_String() {
        assertEquals(new BigDecimal("3.14"), Numbers.toScaledBigDecimal("3.14159"));
        assertEquals(new BigDecimal("3.142"), Numbers.toScaledBigDecimal("3.14159", 3, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((String) null));
        assertThrows(NumberFormatException.class, () -> Numbers.toScaledBigDecimal("not a number"));
    }

    @Test
    public void testToIntExact() {
        assertEquals(123, Numbers.toIntExact(123L));
        assertEquals(Integer.MAX_VALUE, Numbers.toIntExact(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.toIntExact(Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Integer.MAX_VALUE + 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Integer.MIN_VALUE - 1L));
    }

    @Test
    public void testCreateInteger() {
        assertEquals(Integer.valueOf(123), Numbers.createInteger("123"));
        assertEquals(Integer.valueOf(0xFF), Numbers.createInteger("0xFF"));
        assertEquals(Integer.valueOf(077), Numbers.createInteger("077"));
        assertNull(Numbers.createInteger(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createInteger("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.createInteger(""));
    }

    @Test
    public void testCreateLong() {
        assertEquals(Long.valueOf(12345L), Numbers.createLong("12345"));
        assertEquals(Long.valueOf("123"), Numbers.createLong("123L"));
        assertEquals(Long.valueOf(0xFFFFL), Numbers.createLong("0xFFFF"));
        assertNull(Numbers.createLong(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createLong("def"));
    }

    @Test
    public void testCreateFloat() {
        assertEquals(Float.valueOf(123.45f), Numbers.createFloat("123.45"));
        assertNull(Numbers.createFloat(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createFloat("ghi"));
    }

    @Test
    public void testCreateDouble() {
        assertEquals(Double.valueOf(123.4567), Numbers.createDouble("123.4567"));
        assertNull(Numbers.createDouble(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createDouble("jkl"));
    }

    @Test
    public void testCreateBigInteger() {
        assertEquals(new BigInteger("12345678901234567890"), Numbers.createBigInteger("12345678901234567890"));
        assertEquals(new BigInteger("FF", 16), Numbers.createBigInteger("0xFF"));
        assertEquals(new BigInteger("77", 8), Numbers.createBigInteger("077"));
        assertNull(Numbers.createBigInteger(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createBigInteger("mno"));
        assertThrows(NumberFormatException.class, () -> Numbers.createBigInteger(""));
    }

    @Test
    public void testCreateBigDecimal() {
        assertEquals(new BigDecimal("123.4567890123456789"), Numbers.createBigDecimal("123.4567890123456789"));
        assertNull(Numbers.createBigDecimal(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createBigDecimal("pqr"));
        assertThrows(NumberFormatException.class, () -> Numbers.createBigDecimal(""));
    }

    @Test
    public void testCreateNumber() {
        assertEquals(Integer.valueOf(123), Numbers.createNumber("123"));
        assertEquals(Long.valueOf(12345678901L), Numbers.createNumber("12345678901L"));
        assertEquals(Float.valueOf(1.23f), Numbers.createNumber("1.23f"));
        assertEquals(Double.valueOf(1.23d), Numbers.createNumber("1.23d"));
        assertEquals(Double.valueOf(1.23e4), Numbers.createNumber("1.23e4"));
        assertEquals(new BigInteger("1234567890123456789012345"), Numbers.createNumber("1234567890123456789012345"));
        assertEquals(Double.valueOf("0.1234567890123456789012345"), Numbers.createNumber("0.1234567890123456789012345"));
        assertEquals(Integer.valueOf(0xFF), Numbers.createNumber("0xFF"));
        assertEquals(Integer.valueOf(077), Numbers.createNumber("077"));
        assertNull(Numbers.createNumber(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("stu"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber(""));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("-"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1.2.3"));
        assertEquals(Double.valueOf("10D"), Numbers.createNumber("10D"));
        assertEquals(Float.valueOf("10F"), Numbers.createNumber("10F"));
        assertEquals(Long.valueOf("10"), Numbers.createNumber("10L"));
    }

    @Test
    public void testIsDigits() {
        assertTrue(Numbers.isDigits("123"));
        assertFalse(Numbers.isDigits("123.4"));
        assertFalse(Numbers.isDigits("12a3"));
        assertFalse(Numbers.isDigits(""));
        assertFalse(Numbers.isDigits(null));
    }

    @Test
    public void testIsCreatableAndIsNumber() {
        assertTrue(Numbers.isCreatable("123"));
        assertTrue(Numbers.isNumber("123"));
        assertTrue(Numbers.isCreatable("123.45"));
        assertTrue(Numbers.isCreatable("-1.2e-5"));
        assertTrue(Numbers.isCreatable("0xFF"));
        assertTrue(Numbers.isCreatable("077"));
        assertTrue(Numbers.isCreatable("123L"));
        assertTrue(Numbers.isCreatable("1.2f"));
        assertTrue(Numbers.isCreatable("1.2d"));

        assertFalse(Numbers.isCreatable("abc"));
        assertFalse(Numbers.isCreatable(""));
        assertFalse(Numbers.isCreatable(null));
        assertFalse(Numbers.isCreatable("1.2.3"));
        assertFalse(Numbers.isCreatable("--1"));
        assertFalse(Numbers.isCreatable("09"));
        assertTrue(Numbers.isCreatable("0.9"));
    }

    @Test
    public void testIsParsable() {
        assertTrue(Numbers.isParsable("123"));
        assertTrue(Numbers.isParsable("-123"));
        assertTrue(Numbers.isParsable("123.45"));
        assertTrue(Numbers.isParsable(".5"));
        assertTrue(Numbers.isParsable("0.5"));

        assertFalse(Numbers.isParsable("1.2e3"));
        assertFalse(Numbers.isParsable("0xFF"));
        assertFalse(Numbers.isParsable("123L"));
        assertFalse(Numbers.isParsable("abc"));
        assertFalse(Numbers.isParsable(""));
        assertFalse(Numbers.isParsable(null));
        assertFalse(Numbers.isParsable("1.2.3"));
        assertFalse(Numbers.isParsable("123."));
        assertFalse(Numbers.isParsable("+"));
        assertFalse(Numbers.isParsable("-"));

    }

    @Test
    public void testIsPrime() {
        assertFalse(Numbers.isPrime(0));
        assertFalse(Numbers.isPrime(1));
        assertTrue(Numbers.isPrime(2));
        assertTrue(Numbers.isPrime(3));
        assertFalse(Numbers.isPrime(4));
        assertTrue(Numbers.isPrime(5));
        assertTrue(Numbers.isPrime(13));
        assertFalse(Numbers.isPrime(15));
        assertTrue(Numbers.isPrime(97));
        assertFalse(Numbers.isPrime(100));
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPrime(-1));
    }

    @Test
    public void testIsPerfectSquare() {
        assertTrue(Numbers.isPerfectSquare(0));
        assertTrue(Numbers.isPerfectSquare(1));
        assertTrue(Numbers.isPerfectSquare(4));
        assertTrue(Numbers.isPerfectSquare(9));
        assertTrue(Numbers.isPerfectSquare(100));
        assertFalse(Numbers.isPerfectSquare(2));
        assertFalse(Numbers.isPerfectSquare(8));
        assertFalse(Numbers.isPerfectSquare(-4));

        assertTrue(Numbers.isPerfectSquare(0L));
        assertTrue(Numbers.isPerfectSquare(1L));
        assertTrue(Numbers.isPerfectSquare(100000000L * 100000000L));
        assertFalse(Numbers.isPerfectSquare(Long.MAX_VALUE));
    }

    @Test
    public void testIsPowerOfTwo() {
        assertTrue(Numbers.isPowerOfTwo(1));
        assertTrue(Numbers.isPowerOfTwo(2));
        assertTrue(Numbers.isPowerOfTwo(4));
        assertTrue(Numbers.isPowerOfTwo(1024));
        assertFalse(Numbers.isPowerOfTwo(0));
        assertFalse(Numbers.isPowerOfTwo(3));
        assertFalse(Numbers.isPowerOfTwo(-2));

        assertTrue(Numbers.isPowerOfTwo(1L));
        assertTrue(Numbers.isPowerOfTwo(1L << 30));
        assertFalse(Numbers.isPowerOfTwo(0L));

        assertTrue(Numbers.isPowerOfTwo(1.0));
        assertTrue(Numbers.isPowerOfTwo(2.0));
        assertTrue(Numbers.isPowerOfTwo(0.5));
        assertTrue(Numbers.isPowerOfTwo(0.25));
        assertFalse(Numbers.isPowerOfTwo(3.0));
        assertFalse(Numbers.isPowerOfTwo(0.0));
        assertFalse(Numbers.isPowerOfTwo(-2.0));
        assertFalse(Numbers.isPowerOfTwo(Double.NaN));
        assertFalse(Numbers.isPowerOfTwo(Double.POSITIVE_INFINITY));

        assertTrue(Numbers.isPowerOfTwo(BigInteger.ONE));
        assertTrue(Numbers.isPowerOfTwo(new BigInteger("1024")));
        assertFalse(Numbers.isPowerOfTwo(BigInteger.ZERO));
        assertFalse(Numbers.isPowerOfTwo(new BigInteger("3")));
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPowerOfTwo((BigInteger) null));
        assertFalse(Numbers.isPowerOfTwo(new BigInteger("-2")));
    }

    @Test
    public void testLog() {
        assertEquals(Math.log(10.0), Numbers.log(10.0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.log(0.0));
        assertTrue(Double.isNaN(Numbers.log(-1.0)));
    }

    @Test
    public void testLog2_int() {
        assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(10, RoundingMode.DOWN));
        assertEquals(4, Numbers.log2(10, RoundingMode.UP));
        assertEquals(3, Numbers.log2(11, RoundingMode.HALF_DOWN));
        assertEquals(4, IntMath.log2(12, RoundingMode.HALF_DOWN));
        assertEquals(4, Numbers.log2(13, RoundingMode.HALF_DOWN));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log2(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog2_long() {
        assertEquals(3, Numbers.log2(8L, RoundingMode.UNNECESSARY));
        assertEquals(60, Numbers.log2(1L << 60, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0L, RoundingMode.FLOOR));
    }

    @Test
    public void testLog2_double() {
        assertEquals(3.0, Numbers.log2(8.0), DELTA);
        assertEquals(Math.log(10.0) / Math.log(2.0), Numbers.log2(10.0), DELTA);
    }

    @Test
    public void testLog2_double_RoundingMode() {
        assertEquals(3, Numbers.log2(8.0, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(10.0, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10.0, RoundingMode.CEILING));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.NaN, RoundingMode.FLOOR));
    }

    @Test
    public void testLog2_BigInteger() {
        assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.UNNECESSARY));
        assertEquals(9, Numbers.log2(new BigInteger("1000"), RoundingMode.FLOOR));
        assertEquals(10, Numbers.log2(new BigInteger("1000"), RoundingMode.CEILING));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.ZERO, RoundingMode.FLOOR));
    }

    @Test
    public void testLog10_int() {
        assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(150, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(150, RoundingMode.CEILING));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0, RoundingMode.FLOOR));
    }

    @Test
    public void testLog10_long() {
        assertEquals(2, Numbers.log10(100L, RoundingMode.UNNECESSARY));
        assertEquals(9, Numbers.log10(1000000000L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0L, RoundingMode.FLOOR));
    }

    @Test
    public void testLog10_double() {
        assertEquals(2.0, Numbers.log10(100.0), DELTA);
    }

    @Test
    public void testLog10_BigInteger() {
        assertEquals(2, Numbers.log10(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(new BigInteger("1234"), RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(BigInteger.ZERO, RoundingMode.FLOOR));
    }

    @Test
    public void testPow_int() {
        assertEquals(8, Numbers.pow(2, 3));
        assertEquals(1, Numbers.pow(10, 0));
        assertEquals(0, Numbers.pow(0, 5));
        assertEquals(1, Numbers.pow(0, 0));
        assertEquals(-8, Numbers.pow(-2, 3));
        assertEquals(16, Numbers.pow(-2, 4));
        assertEquals(0, Numbers.pow(2, 32));
        assertThrows(IllegalArgumentException.class, () -> Numbers.pow(2, -1));
    }

    @Test
    public void testPow_long() {
        assertEquals(8L, Numbers.pow(2L, 3));
        assertEquals(1L << 60, Numbers.pow(2L, 60));
        assertEquals(0L, Numbers.pow(2L, 64));
        assertThrows(IllegalArgumentException.class, () -> Numbers.pow(2L, -1));
    }

    @Test
    public void testCeilingPowerOfTwo_long() {
        assertEquals(1L, Numbers.ceilingPowerOfTwo(1L));
        assertEquals(2L, Numbers.ceilingPowerOfTwo(2L));
        assertEquals(4L, Numbers.ceilingPowerOfTwo(3L));
        assertEquals(1024L, Numbers.ceilingPowerOfTwo(1000L));
        assertEquals(1L << 62, Numbers.ceilingPowerOfTwo((1L << 62) - 1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(0L));
        assertThrows(ArithmeticException.class, () -> Numbers.ceilingPowerOfTwo((1L << 62) + 1));
    }

    @Test
    public void testCeilingPowerOfTwo_BigInteger() {
        assertEquals(BigInteger.valueOf(4), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.ONE.shiftLeft(100), Numbers.ceilingPowerOfTwo(BigInteger.ONE.shiftLeft(100)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(BigInteger.ZERO));
    }

    @Test
    public void testFloorPowerOfTwo_long() {
        assertEquals(1L, Numbers.floorPowerOfTwo(1L));
        assertEquals(2L, Numbers.floorPowerOfTwo(2L));
        assertEquals(2L, Numbers.floorPowerOfTwo(3L));
        assertEquals(512L, Numbers.floorPowerOfTwo(1000L));
        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(0L));
    }

    @Test
    public void testFloorPowerOfTwo_BigInteger() {
        assertEquals(BigInteger.valueOf(2), Numbers.floorPowerOfTwo(BigInteger.valueOf(3)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(BigInteger.ZERO));
    }

    @Test
    public void testSqrt_int() {
        assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.CEILING));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testSqrt_long() {
        assertEquals(3L, Numbers.sqrt(9L, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt((Long.MAX_VALUE / (1L << 15)) * (Long.MAX_VALUE / (1L << 15)), RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1L, RoundingMode.FLOOR));
    }

    @Test
    public void testSqrt_BigInteger() {
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(9), RoundingMode.UNNECESSARY));
        assertEquals(new BigInteger("1000000000"), Numbers.sqrt(new BigInteger("1000000000000000000"), RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.FLOOR));
    }

    @Test
    public void testDivide_int() {
        assertEquals(3, Numbers.divide(10, 3, RoundingMode.FLOOR));
        assertEquals(4, Numbers.divide(10, 3, RoundingMode.CEILING));
        assertEquals(3, Numbers.divide(10, 3, RoundingMode.DOWN));
        assertEquals(3, Numbers.divide(9, 3, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(10, 0, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(10, 3, RoundingMode.UNNECESSARY));
        assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.DOWN));
        assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.FLOOR));
        assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.CEILING));
        assertEquals(3, Numbers.divide(5, 2, RoundingMode.HALF_UP));
        assertEquals(3, Numbers.divide(5, 2, RoundingMode.HALF_UP));
        assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_DOWN));
        assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_EVEN));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.HALF_EVEN));

    }

    @Test
    public void testDivide_long() {
        assertEquals(3L, Numbers.divide(10L, 3L, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(10L, 0L, RoundingMode.FLOOR));
    }

    @Test
    public void testDivide_BigInteger() {
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.TEN, BigInteger.ZERO, RoundingMode.FLOOR));
    }

    @Test
    public void testMod_int() {
        assertEquals(3, Numbers.mod(7, 4));
        assertEquals(1, Numbers.mod(-7, 4));
        assertEquals(0, Numbers.mod(8, 4));
        assertThrows(ArithmeticException.class, () -> Numbers.mod(7, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.mod(7, -4));
    }

    @Test
    public void testMod_long_int() {
        assertEquals(3, Numbers.mod(7L, 4));
        assertEquals(1, Numbers.mod(-7L, 4));
    }

    @Test
    public void testMod_long_long() {
        assertEquals(3L, Numbers.mod(7L, 4L));
        assertEquals(1L, Numbers.mod(-7L, 4L));
    }

    @Test
    public void testGcd_int() {
        assertEquals(6, Numbers.gcd(54, 24));
        assertEquals(6, Numbers.gcd(-54, 24));
        assertEquals(6, Numbers.gcd(54, -24));
        assertEquals(6, Numbers.gcd(-54, -24));
        assertEquals(5, Numbers.gcd(5, 0));
        assertEquals(5, Numbers.gcd(0, 5));
        assertEquals(0, Numbers.gcd(0, 0));
        assertEquals(1, Numbers.gcd(17, 13));
        assertEquals(Integer.MAX_VALUE, Numbers.gcd(Integer.MAX_VALUE, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.gcd(Integer.MIN_VALUE, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.gcd(0, Integer.MIN_VALUE));

    }

    @Test
    public void testGcd_long() {
        assertEquals(6L, Numbers.gcd(54L, 24L));
        assertEquals(Long.MAX_VALUE, Numbers.gcd(Long.MAX_VALUE, 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.gcd(Long.MIN_VALUE, 0L));
    }

    @Test
    public void testLcm_int() {
        assertEquals(21, Numbers.lcm(3, 7));
        assertEquals(6, Numbers.lcm(6, 2));
        assertEquals(0, Numbers.lcm(0, 5));
        assertEquals(0, Numbers.lcm(5, 0));
        assertEquals(0, Numbers.lcm(0, 0));
        assertEquals(21, Numbers.lcm(-3, 7));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));
        assertEquals(2147483646, Numbers.lcm(Integer.MIN_VALUE / 2 + 1, 2));
    }

    @Test
    public void testLcm_long() {
        assertEquals(21L, Numbers.lcm(3L, 7L));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Long.MAX_VALUE, Long.MAX_VALUE - 1L));
    }

    @Test
    public void testAddExact() {
        assertEquals(5, Numbers.addExact(2, 3));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MAX_VALUE, 1));
        assertEquals(5L, Numbers.addExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MAX_VALUE, 1L));
    }

    @Test
    public void testSubtractExact() {
        assertEquals(-1, Numbers.subtractExact(2, 3));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MIN_VALUE, 1));
        assertEquals(-1L, Numbers.subtractExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MIN_VALUE, 1L));
    }

    @Test
    public void testMultiplyExact() {
        assertEquals(6, Numbers.multiplyExact(2, 3));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MAX_VALUE, 2));
        assertEquals(6L, Numbers.multiplyExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Long.MAX_VALUE, 2L));
    }

    @Test
    public void testPowExact_int() {
        assertEquals(8, Numbers.powExact(2, 3));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> Numbers.powExact(2, -1));
    }

    @Test
    public void testPowExact_long() {
        assertEquals(8L, Numbers.powExact(2L, 3));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Long.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> Numbers.powExact(2L, -1));
    }

    @Test
    public void testSaturatedAdd() {
        assertEquals(5, Numbers.saturatedAdd(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, -1));

        assertEquals(5L, Numbers.saturatedAdd(2L, 3L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, 1L));
    }

    @Test
    public void testSaturatedSubtract() {
        assertEquals(-1, Numbers.saturatedSubtract(2, 3));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedSubtract(Integer.MIN_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedSubtract(Integer.MAX_VALUE, -1));

        assertEquals(-1L, Numbers.saturatedSubtract(2L, 3L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedSubtract(Long.MIN_VALUE, 1L));
    }

    @Test
    public void testSaturatedMultiply() {
        assertEquals(6, Numbers.saturatedMultiply(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, -2));

        assertEquals(6L, Numbers.saturatedMultiply(2L, 3L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, 2L));
    }

    @Test
    public void testSaturatedPow_int() {
        assertEquals(8, Numbers.saturatedPow(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(2, 100));
        assertEquals(1, Numbers.saturatedPow(-1, 100));
        assertEquals(-1, Numbers.saturatedPow(-1, 101));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedPow(2, -1));
    }

    @Test
    public void testSaturatedPow_long() {
        assertEquals(8L, Numbers.saturatedPow(2L, 3));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(Long.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedPow(2L, -1));
    }

    @Test
    public void testSaturatedCast() {
        assertEquals(123, Numbers.saturatedCast(123L));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCast(Long.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCast(Long.MIN_VALUE));
    }

    @Test
    public void testFactorial_int() {
        assertEquals(1, Numbers.factorial(0));
        assertEquals(1, Numbers.factorial(1));
        assertEquals(2, Numbers.factorial(2));
        assertEquals(120, Numbers.factorial(5));
        assertEquals(Integer.MAX_VALUE, Numbers.factorial(13));
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorial(-1));
    }

    @Test
    public void testFactorialToLong() {
        assertEquals(1L, Numbers.factorialToLong(0));
        assertEquals(120L, Numbers.factorialToLong(5));
        assertEquals(Long.MAX_VALUE, Numbers.factorialToLong(21));
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToLong(-1));
    }

    @Test
    public void testFactorialToDouble() {
        assertEquals(1.0, Numbers.factorialToDouble(0), DELTA);
        assertEquals(120.0, Numbers.factorialToDouble(5), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.factorialToDouble(171));
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToDouble(-1));
    }

    @Test
    public void testFactorialToBigInteger() {
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(0));
        assertEquals(BigInteger.valueOf(120), Numbers.factorialToBigInteger(5));
        assertEquals(new BigInteger("2432902008176640000"), Numbers.factorialToBigInteger(20));
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToBigInteger(-1));
    }

    @Test
    public void testBinomial_int() {
        assertEquals(1, Numbers.binomial(5, 0));
        assertEquals(1, Numbers.binomial(5, 5));
        assertEquals(5, Numbers.binomial(5, 1));
        assertEquals(10, Numbers.binomial(5, 2));
        assertEquals(Integer.MAX_VALUE, Numbers.binomial(34, 17));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomial(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomial(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomial(5, 6));
    }

    @Test
    public void testBinomialToLong() {
        assertEquals(10L, Numbers.binomialToLong(5, 2));
        assertEquals(98280, Numbers.binomialToLong(28, 5));
        assertEquals(Long.MAX_VALUE, Numbers.binomialToLong(67, 33));
    }

    @Test
    public void testBinomialToBigInteger() {
        assertEquals(BigInteger.valueOf(10), Numbers.binomialToBigInteger(5, 2));
        BigInteger expected = new BigInteger("61474519");
        assertEquals(expected, Numbers.binomialToBigInteger(62, 6));
        BigInteger largeBinomial = Numbers.factorialToBigInteger(100).divide(Numbers.factorialToBigInteger(50).multiply(Numbers.factorialToBigInteger(50)));
        assertEquals(largeBinomial, Numbers.binomialToBigInteger(100, 50));
    }

    @Test
    public void testMean_int_int() {
        assertEquals(2, Numbers.mean(2, 3));
        assertEquals(2, Numbers.mean(3, 2));
        assertEquals(Integer.MAX_VALUE - 1, Numbers.mean(Integer.MAX_VALUE, Integer.MAX_VALUE - 2));
        assertEquals(-3, Numbers.mean(-2, -3));
    }

    @Test
    public void testMean_long_long() {
        assertEquals(2L, Numbers.mean(2L, 3L));
        assertEquals(Long.MAX_VALUE - 1L, Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE - 2L));
    }

    @Test
    public void testMean_double_double() {
        assertEquals(2.5, Numbers.mean(2.0, 3.0), DELTA);
    }

    @Test
    public void testMean_intArray() {
        assertEquals(3.0, Numbers.mean(1, 2, 3, 4, 5), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new int[] {}));
    }

    @Test
    public void testMean_longArray() {
        assertEquals(3.0, Numbers.mean(1L, 2L, 3L, 4L, 5L), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new long[] {}));
    }

    @Test
    public void testMean_doubleArray() {
        assertEquals(3.0, Numbers.mean(1.0, 2.0, 3.0, 4.0, 5.0), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new double[] {}));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(1.0, Double.NaN));
    }

    @Test
    public void testRound_float_scale() {
        assertEquals(12.35f, Numbers.round(12.345f, 2), DELTA);
        assertEquals(12.3f, Numbers.round(12.345f, 1), DELTA);
        assertEquals(12.0f, Numbers.round(12.345f, 0), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(12.3f, -1));
    }

    @Test
    public void testRound_double_scale() {
        assertEquals(12.346, Numbers.round(12.3456, 3), DELTA);
        assertEquals(Math.round(12.3456 * 1000.0) / 1000.0, Numbers.round(12.3456, 3), DELTA);
        assertEquals(12.0, Numbers.round(12.3456, 0), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(12.3, -1));
    }

    @Test
    public void testRound_float_scale_mode() {
        assertEquals(12.34f, Numbers.round(12.345f, 2, RoundingMode.HALF_DOWN), DELTA);
        assertEquals(12.35f, Numbers.round(12.345f, 2, RoundingMode.HALF_UP), DELTA);
    }

    @Test
    public void testRound_double_scale_mode() {
        assertEquals(12.345, Numbers.round(12.3455, 3, RoundingMode.HALF_DOWN), DELTA);
        assertEquals(12.346, Numbers.round(12.3455, 3, RoundingMode.HALF_UP), DELTA);
        assertEquals(0.0d * 12.345, Numbers.round(12.345, 2, RoundingMode.FLOOR) * (0.0d * 12.345) != 0 ? 1 : 0.0, DELTA);
    }

    @Test
    public void testRound_float_decimalFormatString() {
        assertEquals(12.35f, Numbers.round(12.345f, "0.00"), DELTA);
        assertEquals(12.3f, Numbers.round(12.345f, "#.#"), DELTA);
    }

    @Test
    public void testRound_double_decimalFormatString() {
        assertEquals(12.35, Numbers.round(12.345, "0.00"), DELTA);
    }

    @Test
    public void testRound_float_DecimalFormat() {
        DecimalFormat df = new DecimalFormat("0.0");
        df.setRoundingMode(RoundingMode.UP);
        assertEquals(12.4f, Numbers.round(12.345f, df), DELTA);
    }

    @Test
    public void testRound_double_DecimalFormat() {
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.FLOOR);
        assertEquals(12.34, Numbers.round(12.345, df), DELTA);
    }

    @Test
    public void testRoundToInt() {
        assertEquals(5, Numbers.roundToInt(4.5, RoundingMode.HALF_UP));
        assertEquals(4, Numbers.roundToInt(4.5, RoundingMode.HALF_DOWN));
        assertEquals(4, Numbers.roundToInt(4.5, RoundingMode.DOWN));
        assertEquals(5, Numbers.roundToInt(4.5, RoundingMode.CEILING));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Double.NaN, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt((double) Integer.MAX_VALUE + 1.0, RoundingMode.FLOOR));
    }

    @Test
    public void testRoundToLong() {
        assertEquals(5L, Numbers.roundToLong(4.5, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.POSITIVE_INFINITY, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong((double) Long.MAX_VALUE + 100.0, RoundingMode.FLOOR));
    }

    @Test
    public void testRoundToBigInteger() {
        assertEquals(BigInteger.valueOf(5), Numbers.roundToBigInteger(4.5, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToBigInteger(Double.NaN, RoundingMode.FLOOR));
    }

    @Test
    public void testFuzzyEquals() {
        assertFalse(Numbers.fuzzyEquals(1.0, 1.000000001, 1e-9));
        assertFalse(Numbers.fuzzyEquals(1.0, 1.000000001, 1e-10));
        assertTrue(Numbers.fuzzyEquals(Double.NaN, Double.NaN, 0.1));
        assertTrue(Numbers.fuzzyEquals(0.0, -0.0, 0.0));
        assertTrue(Numbers.fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1));
        assertTrue(Numbers.fuzzyEquals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0, 1.0, -0.1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0, 1.0, Double.NaN));
    }

    @Test
    public void testFuzzyCompare() {
        assertEquals(-1, Numbers.fuzzyCompare(1.0, 1.000000001, 1e-9));
        assertEquals(-1, Numbers.fuzzyCompare(1.0, 1.000000001, 1e-10));
        assertEquals(1, Numbers.fuzzyCompare(1.000000001, 1.0, 1e-10));
        assertEquals(0, Numbers.fuzzyCompare(Double.NaN, Double.NaN, 0.1));
        assertEquals(1, Numbers.fuzzyCompare(Double.NaN, 1.0, 0.1));
    }

    @Test
    public void testIsMathematicalInteger() {
        assertTrue(Numbers.isMathematicalInteger(5.0));
        assertTrue(Numbers.isMathematicalInteger(-3.0));
        assertTrue(Numbers.isMathematicalInteger(0.0));
        assertFalse(Numbers.isMathematicalInteger(5.1));
        assertFalse(Numbers.isMathematicalInteger(Double.NaN));
        assertFalse(Numbers.isMathematicalInteger(Double.POSITIVE_INFINITY));
    }

    @Test
    public void testAsinh() {
        assertEquals(0.0, Numbers.asinh(0.0), DELTA);
        assertEquals(Numbers.asinh(2.5), Numbers.asinh(2.5), DELTA);
        double x = 0.1;
        assertEquals(Math.log(x + Math.sqrt(x * x + 1)), Numbers.asinh(x), 1e-7);
        x = 0.01;
        assertEquals(Math.log(x + Math.sqrt(x * x + 1)), Numbers.asinh(x), 1e-9);
        x = 2.0;
        assertEquals(Math.log(x + Math.sqrt(x * x + 1)), Numbers.asinh(x), DELTA);
    }

    @Test
    public void testAcosh() {
        assertEquals(0.0, Numbers.acosh(1.0), DELTA);
        double x = 2.0;
        assertEquals(Math.log(x + Math.sqrt(x * x - 1)), Numbers.acosh(x), DELTA);
        assertTrue(Double.isNaN(Numbers.acosh(0.5)));
    }

    @Test
    public void testAtanh() {
        assertEquals(0.0, Numbers.atanh(0.0), DELTA);
        double x = 0.5;
        assertEquals(0.5 * Math.log((1 + x) / (1 - x)), Numbers.atanh(x), DELTA);
        x = 0.1;
        assertEquals(0.5 * Math.log((1 + x) / (1 - x)), Numbers.atanh(x), 1e-7);

        assertEquals(Double.POSITIVE_INFINITY, Numbers.atanh(1.0));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.atanh(-1.0));
        assertTrue(Double.isNaN(Numbers.atanh(2.0)));
    }
}
