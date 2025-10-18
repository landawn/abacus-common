package com.landawn.abacus.util;

import static com.landawn.abacus.util.Numbers.BYTE_MINUS_ONE;
import static com.landawn.abacus.util.Numbers.BYTE_ONE;
import static com.landawn.abacus.util.Numbers.BYTE_ZERO;
import static com.landawn.abacus.util.Numbers.DOUBLE_MINUS_ONE;
import static com.landawn.abacus.util.Numbers.DOUBLE_ONE;
import static com.landawn.abacus.util.Numbers.DOUBLE_ZERO;
import static com.landawn.abacus.util.Numbers.FLOAT_MINUS_ONE;
import static com.landawn.abacus.util.Numbers.FLOAT_ONE;
import static com.landawn.abacus.util.Numbers.FLOAT_ZERO;
import static com.landawn.abacus.util.Numbers.INTEGER_MINUS_ONE;
import static com.landawn.abacus.util.Numbers.INTEGER_ONE;
import static com.landawn.abacus.util.Numbers.INTEGER_TWO;
import static com.landawn.abacus.util.Numbers.INTEGER_ZERO;
import static com.landawn.abacus.util.Numbers.LONG_MINUS_ONE;
import static com.landawn.abacus.util.Numbers.LONG_ONE;
import static com.landawn.abacus.util.Numbers.LONG_ZERO;
import static com.landawn.abacus.util.Numbers.SHORT_MINUS_ONE;
import static com.landawn.abacus.util.Numbers.SHORT_ONE;
import static com.landawn.abacus.util.Numbers.SHORT_ZERO;
import static com.landawn.abacus.util.Numbers.acosh;
import static com.landawn.abacus.util.Numbers.addExact;
import static com.landawn.abacus.util.Numbers.asinh;
import static com.landawn.abacus.util.Numbers.atanh;
import static com.landawn.abacus.util.Numbers.binomial;
import static com.landawn.abacus.util.Numbers.binomialToBigInteger;
import static com.landawn.abacus.util.Numbers.binomialToLong;
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
import static com.landawn.abacus.util.Numbers.extractFirstLong;
import static com.landawn.abacus.util.Numbers.factorial;
import static com.landawn.abacus.util.Numbers.factorialToBigInteger;
import static com.landawn.abacus.util.Numbers.factorialToDouble;
import static com.landawn.abacus.util.Numbers.factorialToLong;
import static com.landawn.abacus.util.Numbers.floorPowerOfTwo;
import static com.landawn.abacus.util.Numbers.format;
import static com.landawn.abacus.util.Numbers.fuzzyCompare;
import static com.landawn.abacus.util.Numbers.fuzzyEquals;
import static com.landawn.abacus.util.Numbers.gcd;
import static com.landawn.abacus.util.Numbers.isCreatable;
import static com.landawn.abacus.util.Numbers.isDigits;
import static com.landawn.abacus.util.Numbers.isMathematicalInteger;
import static com.landawn.abacus.util.Numbers.isNumber;
import static com.landawn.abacus.util.Numbers.isParsable;
import static com.landawn.abacus.util.Numbers.isPerfectSquare;
import static com.landawn.abacus.util.Numbers.isPowerOfTwo;
import static com.landawn.abacus.util.Numbers.isPrime;
import static com.landawn.abacus.util.Numbers.lcm;
import static com.landawn.abacus.util.Numbers.log;
import static com.landawn.abacus.util.Numbers.log10;
import static com.landawn.abacus.util.Numbers.log2;
import static com.landawn.abacus.util.Numbers.mean;
import static com.landawn.abacus.util.Numbers.mod;
import static com.landawn.abacus.util.Numbers.multiplyExact;
import static com.landawn.abacus.util.Numbers.pow;
import static com.landawn.abacus.util.Numbers.powExact;
import static com.landawn.abacus.util.Numbers.round;
import static com.landawn.abacus.util.Numbers.roundToBigInteger;
import static com.landawn.abacus.util.Numbers.roundToInt;
import static com.landawn.abacus.util.Numbers.roundToLong;
import static com.landawn.abacus.util.Numbers.saturatedAdd;
import static com.landawn.abacus.util.Numbers.saturatedCast;
import static com.landawn.abacus.util.Numbers.saturatedMultiply;
import static com.landawn.abacus.util.Numbers.saturatedPow;
import static com.landawn.abacus.util.Numbers.saturatedSubtract;
import static com.landawn.abacus.util.Numbers.sqrt;
import static com.landawn.abacus.util.Numbers.subtractExact;
import static com.landawn.abacus.util.Numbers.toByte;
import static com.landawn.abacus.util.Numbers.toDouble;
import static com.landawn.abacus.util.Numbers.toFloat;
import static com.landawn.abacus.util.Numbers.toInt;
import static com.landawn.abacus.util.Numbers.toIntExact;
import static com.landawn.abacus.util.Numbers.toLong;
import static com.landawn.abacus.util.Numbers.toScaledBigDecimal;
import static com.landawn.abacus.util.Numbers.toShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.FastMath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Numbers100Test extends TestBase {

    private static final double DELTA = 1e-15;
    private static final float FLOAT_DELTA = 1e-7f;

    @Test
    public void testConstants() {
        assertEquals((byte) 0, BYTE_ZERO.byteValue());
        assertEquals((byte) 1, BYTE_ONE.byteValue());
        assertEquals((byte) -1, BYTE_MINUS_ONE.byteValue());
        assertEquals((short) 0, SHORT_ZERO.shortValue());
        assertEquals((short) 1, SHORT_ONE.shortValue());
        assertEquals((short) -1, SHORT_MINUS_ONE.shortValue());
        assertEquals(0, INTEGER_ZERO.intValue());
        assertEquals(1, INTEGER_ONE.intValue());
        assertEquals(2, INTEGER_TWO.intValue());
        assertEquals(-1, INTEGER_MINUS_ONE.intValue());
        assertEquals(0L, LONG_ZERO.longValue());
        assertEquals(1L, LONG_ONE.longValue());
        assertEquals(-1L, LONG_MINUS_ONE.longValue());
        assertEquals(0.0f, FLOAT_ZERO, FLOAT_DELTA);
        assertEquals(1.0f, FLOAT_ONE, FLOAT_DELTA);
        assertEquals(-1.0f, FLOAT_MINUS_ONE, FLOAT_DELTA);
        assertEquals(0.0d, DOUBLE_ZERO, DELTA);
        assertEquals(1.0d, DOUBLE_ONE, DELTA);
        assertEquals(-1.0d, DOUBLE_MINUS_ONE, DELTA);
    }

    @Test
    public void testConvert() {
        assertEquals(Byte.valueOf((byte) 42), convert(42, byte.class));
        assertEquals(Byte.valueOf((byte) 42), convert(42L, Byte.class));
        assertEquals(Byte.valueOf((byte) 42), convert(42.0f, byte.class));
        assertEquals(Byte.valueOf((byte) 42), convert(42.0d, Byte.class));
        assertEquals(Byte.valueOf((byte) 42), convert(new BigInteger("42"), byte.class));
        assertEquals(Byte.valueOf((byte) 42), convert(new BigDecimal("42"), Byte.class));

        assertEquals(Short.valueOf((short) 300), convert(300, short.class));
        assertEquals(Short.valueOf((short) 300), convert(300L, Short.class));
        assertEquals(Short.valueOf((short) 300), convert(300.0f, short.class));
        assertEquals(Short.valueOf((short) 300), convert(300.0d, Short.class));

        assertEquals(Integer.valueOf(70000), convert(70000L, int.class));
        assertEquals(Integer.valueOf(70000), convert(70000.0f, Integer.class));
        assertEquals(Integer.valueOf(70000), convert(70000.0d, int.class));
        assertEquals(Integer.valueOf(70000), convert(new BigInteger("70000"), Integer.class));

        assertEquals(Long.valueOf(1234567890123L), convert(1234567890123L, long.class));
        assertEquals(Long.valueOf(1234567890123L), convert(new BigInteger("1234567890123"), Long.class));

        assertEquals(Float.valueOf(3.14f), convert(3.14f, float.class));
        assertEquals(Float.valueOf(3.14f), convert(3.14d, Float.class));

        assertEquals(Double.valueOf(3.14159), convert(3.14159d, double.class));
        assertEquals(Double.valueOf(3.14159), convert(new BigDecimal("3.14159"), Double.class));

        assertEquals(new BigInteger("12345"), convert(12345, BigInteger.class));
        assertEquals(new BigInteger("12345"), convert(12345L, BigInteger.class));
        assertEquals(new BigInteger("12345"), convert(new BigDecimal("12345"), BigInteger.class));

        assertEquals(new BigDecimal("12345"), convert(12345, BigDecimal.class));
        assertEquals(new BigDecimal("12345.67"), convert(12345.67d, BigDecimal.class));

        assertEquals(Byte.valueOf((byte) 0), convert(null, byte.class));
        assertEquals(null, convert(null, Integer.class));
        assertEquals(Long.valueOf(0L), convert(null, long.class));
        assertEquals(null, convert(null, Float.class));
        assertEquals(Double.valueOf(0.0d), convert(null, double.class));
        assertEquals(null, convert(null, BigInteger.class));
        assertEquals(null, convert(null, BigDecimal.class));

        try {
            convert(200, byte.class);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            convert(70000, short.class);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            convert(3000000000L, int.class);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testFormat() {
        assertEquals("123", format(123, "#"));
        assertEquals("123", format(123, "0"));
        assertEquals("123.0", format(123, "0.0"));
        assertEquals("123.00", format(123, "0.00"));
        assertEquals("12300%", format(123, "0%"));
        assertEquals("12.3%", format(0.123f, "0.0%"));

        assertEquals("456", format(Integer.valueOf(456), "#"));
        assertEquals("0", format((Integer) null, "0"));
        assertEquals("0.00", format((Integer) null, "0.00"));

        assertEquals("789", format(789L, "#"));
        assertEquals("789.00", format(789L, "0.00"));

        assertEquals("101112", format(Long.valueOf(101112L), "#"));
        assertEquals("0", format((Long) null, "0"));

        assertEquals("3.14", format(3.14159f, "0.00"));
        assertEquals("3.1", format(3.14159f, "#.#"));
        assertEquals("314.16%", format(3.14159f, "0.00%"));

        assertEquals("2.72", format(Float.valueOf(2.71828f), "0.00"));
        assertEquals("0.00", format((Float) null, "0.00"));

        assertEquals("3.14159", format(3.14159265359, "0.00000"));
        assertEquals("3.14", format(3.14159265359, "#.##"));
        assertEquals("31.42%", format(0.31415926, "0.00%"));

        assertEquals("2.71828", format(Double.valueOf(2.718281828), "0.00000"));
        assertEquals("0.00", format((Double) null, "0.00"));
    }

    @Test
    public void testExtractFirstInt() {
        assertEquals(123, extractFirstInt("abc123def").get());
        assertEquals(-456, extractFirstInt("xyz-456abc").get());
        assertEquals(789, extractFirstInt("789").get());
        assertTrue(extractFirstInt("abc").isEmpty());
        assertTrue(extractFirstInt(" ").isEmpty());
        assertTrue(extractFirstInt(null).isEmpty());
        assertEquals(999, extractFirstInt("no numbers here", 999));
        assertEquals(42, extractFirstInt("answer is 42", 0));
    }

    @Test
    public void testExtractFirstLong() {
        assertEquals(1234567890123L, extractFirstLong("abc1234567890123def").get());
        assertEquals(-9876543210L, extractFirstLong("xyz-9876543210abc").get());
        assertTrue(extractFirstLong("abc").isEmpty());
        assertTrue(extractFirstLong(" ").isEmpty());
        assertTrue(extractFirstLong(null).isEmpty());
        assertEquals(999L, extractFirstLong("no numbers here", 999L));
    }

    @Test
    public void testExtractFirstDouble() {
        assertEquals(123.456, extractFirstDouble("abc123.456def").orElseThrow(), DELTA);
        assertEquals(-78.9, extractFirstDouble("xyz-78.9abc").orElseThrow(), DELTA);
        assertTrue(extractFirstDouble("no numbers").isEmpty());
        assertEquals(99.9, extractFirstDouble("no numbers", 99.9), DELTA);

        assertEquals(1.23e4, extractFirstDouble("value is 1.23e4", true).orElseThrow(), DELTA);
        assertEquals(1.23, extractFirstDouble("value is 1.23e4", false).orElseThrow(), DELTA);
        assertEquals(-5.67e-3, extractFirstDouble("result: -5.67e-3", true).orElseThrow(), DELTA);
    }

    @Test
    public void testToByte() {
        assertEquals((byte) 42, toByte("42"));
        assertEquals((byte) -128, toByte("-128"));
        assertEquals((byte) 127, toByte("127"));
        assertEquals((byte) 0, toByte(""));
        assertEquals((byte) 0, toByte(null));
        assertEquals((byte) 99, toByte("", (byte) 99));
        assertEquals((byte) 99, toByte(null, (byte) 99));

        assertEquals((byte) 42, toByte(Integer.valueOf(42)));
        assertEquals((byte) 42, toByte(Long.valueOf(42L)));
        assertEquals((byte) 42, toByte(Float.valueOf(42.0f)));
        assertEquals((byte) 42, toByte(Double.valueOf(42.0d)));
        assertEquals((byte) 42, toByte(new BigInteger("42")));
        assertEquals((byte) 42, toByte(new BigDecimal("42")));
        assertEquals((byte) 0, toByte((Object) null));
        assertEquals((byte) 99, toByte((Object) null, (byte) 99));

        try {
            toByte("200");
            fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    @Test
    public void testToShort() {
        assertEquals((short) 300, toShort("300"));
        assertEquals((short) -32768, toShort("-32768"));
        assertEquals((short) 32767, toShort("32767"));
        assertEquals((short) 0, toShort(""));
        assertEquals((short) 0, toShort(null));
        assertEquals((short) 999, toShort("", (short) 999));

        assertEquals((short) 300, toShort(Integer.valueOf(300)));
        assertEquals((short) 300, toShort(Long.valueOf(300L)));
        assertEquals((short) 300, toShort(Float.valueOf(300.0f)));
        assertEquals((short) 300, toShort(Double.valueOf(300.0d)));
        assertEquals((short) 0, toShort((Object) null));
        assertEquals((short) 999, toShort((Object) null, (short) 999));
    }

    @Test
    public void testToInt() {
        assertEquals(70000, toInt("70000"));
        assertEquals(-2147483648, toInt("-2147483648"));
        assertEquals(2147483647, toInt("2147483647"));
        assertEquals(0, toInt(""));
        assertEquals(0, toInt(null));
        assertEquals(999, toInt("", 999));

        assertEquals(70000, toInt(Integer.valueOf(70000)));
        assertEquals(70000, toInt(Long.valueOf(70000L)));
        assertEquals(70000, toInt(Float.valueOf(70000.0f)));
        assertEquals(70000, toInt(Double.valueOf(70000.0d)));
        assertEquals(70000, toInt(new BigInteger("70000")));
        assertEquals(70000, toInt(new BigDecimal("70000")));
        assertEquals(0, toInt((Object) null));
        assertEquals(999, toInt((Object) null, 999));
    }

    @Test
    public void testToLong() {
        assertEquals(1234567890123L, toLong("1234567890123"));
        assertEquals(1234567890123L, toLong("1234567890123L"));
        assertEquals(1234567890123L, toLong("1234567890123l"));
        assertEquals(0L, toLong(""));
        assertEquals(0L, toLong(null));
        assertEquals(999L, toLong("", 999L));

        assertEquals(1234567890123L, toLong(Long.valueOf(1234567890123L)));
        assertEquals(70000L, toLong(Integer.valueOf(70000)));
        assertEquals(1234567890123L, toLong(new BigInteger("1234567890123")));
        assertEquals(1234567890123L, toLong(new BigDecimal("1234567890123")));
        assertEquals(0L, toLong((Object) null));
        assertEquals(999L, toLong((Object) null, 999L));
    }

    @Test
    public void testToFloat() {
        assertEquals(3.14f, toFloat("3.14"), FLOAT_DELTA);
        assertEquals(-123.456f, toFloat("-123.456"), FLOAT_DELTA);
        assertEquals(0.0f, toFloat(""), FLOAT_DELTA);
        assertEquals(0.0f, toFloat(null), FLOAT_DELTA);
        assertEquals(99.9f, toFloat("", 99.9f), FLOAT_DELTA);

        assertEquals(3.14f, toFloat(Float.valueOf(3.14f)), FLOAT_DELTA);
        assertEquals(42.0f, toFloat(Integer.valueOf(42)), FLOAT_DELTA);
        assertEquals(42.0f, toFloat(Double.valueOf(42.0d)), FLOAT_DELTA);
        assertEquals(0.0f, toFloat((Object) null), FLOAT_DELTA);
        assertEquals(99.9f, toFloat((Object) null, 99.9f), FLOAT_DELTA);
    }

    @Test
    public void testToDouble() {
        assertEquals(3.14159, toDouble("3.14159"), DELTA);
        assertEquals(-123.456789, toDouble("-123.456789"), DELTA);
        assertEquals(0.0, toDouble(""), DELTA);
        assertEquals(0.0, toDouble((String) null), DELTA);
        assertEquals(99.999, toDouble("", 99.999), DELTA);

        assertEquals(3.14159, toDouble(Double.valueOf(3.14159)), DELTA);
        assertEquals(42.0, toDouble(Integer.valueOf(42)), DELTA);
        assertEquals(3.14, toDouble(Float.valueOf(3.14f)), DELTA);
        assertEquals(0.0, toDouble((Object) null), DELTA);
        assertEquals(99.999, toDouble((Object) null, 99.999), DELTA);
    }

    @Test
    public void testToDoubleBigDecimal() {
        assertEquals(123.456, toDouble(new BigDecimal("123.456")), DELTA);
        assertEquals(0.0, toDouble((BigDecimal) null), DELTA);
        assertEquals(99.9, toDouble((BigDecimal) null, 99.9), DELTA);
    }

    @Test
    public void testToScaledBigDecimal() {
        assertEquals(new BigDecimal("123.46"), toScaledBigDecimal(new BigDecimal("123.456")));
        assertEquals(new BigDecimal("123.456"), toScaledBigDecimal(new BigDecimal("123.456"), 3, RoundingMode.HALF_EVEN));
        assertEquals(BigDecimal.ZERO, toScaledBigDecimal((BigDecimal) null));

        assertEquals(new BigDecimal("123.46"), toScaledBigDecimal(Float.valueOf(123.456f)));
        assertEquals(new BigDecimal("123.5"), toScaledBigDecimal(Float.valueOf(123.456f), 1, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, toScaledBigDecimal((Float) null));

        assertEquals(new BigDecimal("123.46"), toScaledBigDecimal(Double.valueOf(123.456)));
        assertEquals(new BigDecimal("123.5"), toScaledBigDecimal(Double.valueOf(123.456), 1, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, toScaledBigDecimal((Double) null));

        assertEquals(new BigDecimal("123.46"), toScaledBigDecimal("123.456"));
        assertEquals(new BigDecimal("123.5"), toScaledBigDecimal("123.456", 1, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, toScaledBigDecimal((String) null));
    }

    @Test
    public void testToIntExact() {
        assertEquals(12345, toIntExact(12345L));
        assertEquals(-12345, toIntExact(-12345L));
        assertEquals(Integer.MAX_VALUE, toIntExact(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, toIntExact(Integer.MIN_VALUE));

        try {
            toIntExact(Integer.MAX_VALUE + 1L);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            toIntExact(Integer.MIN_VALUE - 1L);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testCreateInteger() {
        assertEquals(Integer.valueOf(123), createInteger("123"));
        assertEquals(Integer.valueOf(-456), createInteger("-456"));
        assertEquals(Integer.valueOf(0x1A), createInteger("0x1A"));
        assertEquals(Integer.valueOf(026), createInteger("026"));
        assertNull(createInteger(null));

        try {
            createInteger("not a number");
            fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    @Test
    public void testCreateLong() {
        assertEquals(Long.valueOf(123L), createLong("123"));
        assertEquals(Long.valueOf(123L), createLong("123L"));
        assertEquals(Long.valueOf(123L), createLong("123l"));
        assertEquals(Long.valueOf(-456L), createLong("-456"));
        assertEquals(Long.valueOf(0x1AL), createLong("0x1A"));
        assertEquals(Long.valueOf(026L), createLong("026"));
        assertNull(createLong(null));
    }

    @Test
    public void testCreateFloat() {
        assertEquals(Float.valueOf(123.45f), createFloat("123.45"));
        assertEquals(Float.valueOf(-67.89f), createFloat("-67.89"));
        assertNull(createFloat(null));
    }

    @Test
    public void testCreateDouble() {
        assertEquals(Double.valueOf(123.456789), createDouble("123.456789"));
        assertEquals(Double.valueOf(-67.890123), createDouble("-67.890123"));
        assertNull(createDouble(null));
    }

    @Test
    public void testCreateBigInteger() {
        assertEquals(new BigInteger("123456789012345678901234567890"), createBigInteger("123456789012345678901234567890"));
        assertEquals(new BigInteger("1A", 16), createBigInteger("0x1A"));
        assertEquals(new BigInteger("1A", 16), createBigInteger("#1A"));
        assertEquals(new BigInteger("26", 8), createBigInteger("026"));
        assertNull(createBigInteger(null));
    }

    @Test
    public void testCreateBigDecimal() {
        assertEquals(new BigDecimal("123.456789012345678901234567890"), createBigDecimal("123.456789012345678901234567890"));
        assertEquals(new BigDecimal("-67.890123456789012345678901234"), createBigDecimal("-67.890123456789012345678901234"));
        assertNull(createBigDecimal(null));
    }

    @Test
    public void testCreateNumber() {
        assertEquals(Integer.valueOf(42), createNumber("42"));
        assertEquals(Long.valueOf(3000000000L), createNumber("3000000000"));
        assertEquals(Long.valueOf(42), createNumber("42L"));

        assertEquals(Double.valueOf(3.14), createNumber("3.14"));
        assertEquals(Float.valueOf(3.14f), createNumber("3.14f"));
        assertEquals(Float.valueOf(3.14f), createNumber("3.14F"));
        assertEquals(Double.valueOf(3.14), createNumber("3.14d"));
        assertEquals(Double.valueOf(3.14), createNumber("3.14D"));

        assertEquals(Integer.valueOf(0x1A), NumberUtils.createNumber("0x1A"));
        assertEquals(Integer.valueOf(0x1A), createNumber("0x1A"));
        assertEquals(Long.valueOf(0x1234567890L), createNumber("0x1234567890"));

        assertEquals(Integer.valueOf(026), createNumber("026"));

        assertEquals(Double.valueOf(1.23e4), createNumber("1.23e4"));
        assertEquals(Double.valueOf(1.23e-4), createNumber("1.23e-4"));

        assertNull(createNumber(null));
    }

    @Test
    public void testIsDigits() {
        assertTrue(isDigits("12345"));
        assertTrue(isDigits("0"));
        assertFalse(isDigits("123.45"));
        assertFalse(isDigits("-123"));
        assertFalse(isDigits("123a"));
        assertFalse(isDigits(""));
        assertFalse(isDigits(null));
    }

    @Test
    public void testIsNumber() {
        assertTrue(isNumber("123"));
        assertTrue(isNumber("-456"));
        assertTrue(isNumber("123.456"));
        assertTrue(isNumber("-123.456"));
        assertTrue(isNumber("1.23e4"));
        assertTrue(isNumber("1.23E-4"));
        assertTrue(isNumber("0x1A"));
        assertTrue(isNumber("026"));
        assertTrue(isNumber("123L"));
        assertTrue(isNumber("123.45f"));
        assertTrue(isNumber("123.45d"));
        assertFalse(isNumber(""));
        assertFalse(isNumber(null));
        assertFalse(isNumber("not a number"));
        assertFalse(isNumber("123.45.67"));
    }

    @Test
    public void testIsCreatable() {
        assertTrue(isCreatable("123"));
        assertTrue(isCreatable("-456"));
        assertTrue(isCreatable("123.456"));
        assertTrue(isCreatable("-123.456"));
        assertTrue(isCreatable("1.23e4"));
        assertTrue(isCreatable("1.23E-4"));
        assertTrue(isCreatable("0x1A"));
        assertTrue(isCreatable("026"));
        assertTrue(isCreatable("123L"));
        assertTrue(isCreatable("123.45f"));
        assertTrue(isCreatable("123.45d"));
        assertFalse(isCreatable(""));
        assertFalse(isCreatable(null));
        assertFalse(isCreatable("not a number"));
        assertFalse(isCreatable("123.45.67"));
    }

    @Test
    public void testIsParsable() {
        assertTrue(isParsable("123"));
        assertTrue(isParsable("-456"));
        assertTrue(isParsable("123.456"));
        assertTrue(isParsable("-123.456"));
        assertFalse(isParsable("1.23e4"));
        assertFalse(isParsable("0x1A"));
        assertFalse(isParsable(""));
        assertFalse(isParsable(null));
        assertFalse(isParsable("not a number"));
        assertFalse(isParsable("123."));
    }

    @Test
    public void testIsPrime() {
        assertTrue(isPrime(2));
        assertTrue(isPrime(3));
        assertTrue(isPrime(5));
        assertTrue(isPrime(7));
        assertTrue(isPrime(11));
        assertTrue(isPrime(13));
        assertTrue(isPrime(17));
        assertTrue(isPrime(19));
        assertTrue(isPrime(23));
        assertTrue(isPrime(29));

        assertFalse(isPrime(0));
        assertFalse(isPrime(1));
        assertFalse(isPrime(4));
        assertFalse(isPrime(6));
        assertFalse(isPrime(8));
        assertFalse(isPrime(9));
        assertFalse(isPrime(10));
        assertFalse(isPrime(12));

        assertTrue(isPrime(97));
        assertTrue(isPrime(997));
        assertTrue(isPrime(9973));

        try {
            isPrime(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testIsPerfectSquare() {
        assertTrue(isPerfectSquare(0));
        assertTrue(isPerfectSquare(1));
        assertTrue(isPerfectSquare(4));
        assertTrue(isPerfectSquare(9));
        assertTrue(isPerfectSquare(16));
        assertTrue(isPerfectSquare(25));
        assertTrue(isPerfectSquare(100));
        assertTrue(isPerfectSquare(10000));

        assertFalse(isPerfectSquare(2));
        assertFalse(isPerfectSquare(3));
        assertFalse(isPerfectSquare(5));
        assertFalse(isPerfectSquare(10));
        assertFalse(isPerfectSquare(-1));
        assertFalse(isPerfectSquare(-4));

        assertTrue(isPerfectSquare(0L));
        assertTrue(isPerfectSquare(1L));
        assertTrue(isPerfectSquare(4L));
        assertTrue(isPerfectSquare(9L));
        assertTrue(isPerfectSquare(1000000L));
        assertTrue(isPerfectSquare(1000000000000L));

        assertFalse(isPerfectSquare(2L));
        assertFalse(isPerfectSquare(1000001L));
        assertFalse(isPerfectSquare(-1L));
    }

    @Test
    public void testIsPowerOfTwo() {
        assertTrue(isPowerOfTwo(1));
        assertTrue(isPowerOfTwo(2));
        assertTrue(isPowerOfTwo(4));
        assertTrue(isPowerOfTwo(8));
        assertTrue(isPowerOfTwo(16));
        assertTrue(isPowerOfTwo(1024));
        assertTrue(isPowerOfTwo(1 << 30));

        assertFalse(isPowerOfTwo(0));
        assertFalse(isPowerOfTwo(3));
        assertFalse(isPowerOfTwo(5));
        assertFalse(isPowerOfTwo(6));
        assertFalse(isPowerOfTwo(7));
        assertFalse(isPowerOfTwo(-1));
        assertFalse(isPowerOfTwo(-2));

        assertTrue(isPowerOfTwo(1L));
        assertTrue(isPowerOfTwo(2L));
        assertTrue(isPowerOfTwo(1L << 40));
        assertTrue(isPowerOfTwo(1L << 62));

        assertFalse(isPowerOfTwo(0L));
        assertFalse(isPowerOfTwo(3L));
        assertFalse(isPowerOfTwo(-1L));

        assertTrue(isPowerOfTwo(1.0));
        assertTrue(isPowerOfTwo(2.0));
        assertTrue(isPowerOfTwo(4.0));
        assertTrue(isPowerOfTwo(8.0));
        assertTrue(isPowerOfTwo(0.5));
        assertTrue(isPowerOfTwo(0.25));

        assertFalse(isPowerOfTwo(0.0));
        assertFalse(isPowerOfTwo(3.0));
        assertFalse(isPowerOfTwo(5.0));
        assertFalse(isPowerOfTwo(-1.0));
        assertFalse(isPowerOfTwo(Double.NaN));
        assertFalse(isPowerOfTwo(Double.POSITIVE_INFINITY));

        assertTrue(isPowerOfTwo(BigInteger.ONE));
        assertTrue(isPowerOfTwo(BigInteger.valueOf(2)));
        assertTrue(isPowerOfTwo(BigInteger.valueOf(1024)));
        assertTrue(isPowerOfTwo(BigInteger.ONE.shiftLeft(100)));

        assertFalse(isPowerOfTwo(BigInteger.ZERO));
        assertFalse(isPowerOfTwo(BigInteger.valueOf(3)));
        assertFalse(isPowerOfTwo(BigInteger.valueOf(-1)));
    }

    @Test
    public void testLog() {
        assertEquals(0.0, log(1.0), DELTA);
        assertEquals(Math.log(2.0), log(2.0), DELTA);
        assertEquals(Math.log(Math.E), log(Math.E), DELTA);
        assertEquals(Math.log(10.0), log(10.0), DELTA);
    }

    @Test
    public void testLog2() {
        assertEquals(0, log2(1, RoundingMode.UNNECESSARY));
        assertEquals(1, log2(2, RoundingMode.UNNECESSARY));
        assertEquals(2, log2(4, RoundingMode.UNNECESSARY));
        assertEquals(3, log2(8, RoundingMode.UNNECESSARY));
        assertEquals(10, log2(1024, RoundingMode.UNNECESSARY));

        assertEquals(2, log2(5, RoundingMode.FLOOR));
        assertEquals(3, log2(5, RoundingMode.CEILING));
        assertEquals(2, log2(5, RoundingMode.DOWN));
        assertEquals(3, log2(5, RoundingMode.UP));
        assertEquals(2, log2(5, RoundingMode.HALF_DOWN));
        assertEquals(2, log2(5, RoundingMode.HALF_UP));
        assertEquals(2, log2(5, RoundingMode.HALF_EVEN));

        assertEquals(0, log2(1L, RoundingMode.UNNECESSARY));
        assertEquals(1, log2(2L, RoundingMode.UNNECESSARY));
        assertEquals(40, log2(1L << 40, RoundingMode.UNNECESSARY));

        assertEquals(0.0, log2(1.0), DELTA);
        assertEquals(1.0, log2(2.0), DELTA);
        assertEquals(2.0, log2(4.0), DELTA);
        assertEquals(3.0, log2(8.0), DELTA);
        assertEquals(-1.0, log2(0.5), DELTA);
        assertEquals(-2.0, log2(0.25), DELTA);

        assertEquals(0, log2(1.0, RoundingMode.UNNECESSARY));
        assertEquals(1, log2(2.0, RoundingMode.UNNECESSARY));
        assertEquals(2, log2(5.0, RoundingMode.FLOOR));
        assertEquals(3, log2(5.0, RoundingMode.CEILING));

        assertEquals(0, log2(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, log2(BigInteger.valueOf(2), RoundingMode.UNNECESSARY));
        assertEquals(100, log2(BigInteger.ONE.shiftLeft(100), RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog10() {
        assertEquals(0, log10(1, RoundingMode.UNNECESSARY));
        assertEquals(1, log10(10, RoundingMode.UNNECESSARY));
        assertEquals(2, log10(100, RoundingMode.UNNECESSARY));
        assertEquals(3, log10(1000, RoundingMode.UNNECESSARY));

        assertEquals(1, log10(15, RoundingMode.FLOOR));
        assertEquals(2, log10(15, RoundingMode.CEILING));
        assertEquals(1, log10(15, RoundingMode.DOWN));
        assertEquals(2, log10(15, RoundingMode.UP));

        assertEquals(0, log10(1L, RoundingMode.UNNECESSARY));
        assertEquals(1, log10(10L, RoundingMode.UNNECESSARY));
        assertEquals(9, log10(1000000000L, RoundingMode.UNNECESSARY));

        assertEquals(0.0, log10(1.0), DELTA);
        assertEquals(1.0, log10(10.0), DELTA);
        assertEquals(2.0, log10(100.0), DELTA);
        assertEquals(-1.0, log10(0.1), DELTA);

        assertEquals(0, log10(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, log10(BigInteger.TEN, RoundingMode.UNNECESSARY));
        assertEquals(2, log10(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));
    }

    @Test
    public void testPow() {
        assertEquals(1, pow(2, 0));
        assertEquals(2, pow(2, 1));
        assertEquals(4, pow(2, 2));
        assertEquals(8, pow(2, 3));
        assertEquals(1024, pow(2, 10));
        assertEquals(1, pow(1, 100));
        assertEquals(1, pow(-1, 0));
        assertEquals(-1, pow(-1, 1));
        assertEquals(1, pow(-1, 2));
        assertEquals(-1, pow(-1, 3));
        assertEquals(0, pow(0, 1));
        assertEquals(1, pow(0, 0));
        assertEquals(9, pow(3, 2));
        assertEquals(27, pow(3, 3));

        assertEquals(1L, pow(2L, 0));
        assertEquals(2L, pow(2L, 1));
        assertEquals(1024L, pow(2L, 10));
        assertEquals(1L, pow(1L, 100));
        assertEquals(1L << 40, pow(2L, 40));
        assertEquals(9L, pow(3L, 2));
        assertEquals(27L, pow(3L, 3));
    }

    @Test
    public void testCeilingPowerOfTwo() {
        assertEquals(1L, ceilingPowerOfTwo(1L));
        assertEquals(2L, ceilingPowerOfTwo(2L));
        assertEquals(4L, ceilingPowerOfTwo(3L));
        assertEquals(4L, ceilingPowerOfTwo(4L));
        assertEquals(8L, ceilingPowerOfTwo(5L));
        assertEquals(16L, ceilingPowerOfTwo(15L));
        assertEquals(16L, ceilingPowerOfTwo(16L));
        assertEquals(32L, ceilingPowerOfTwo(17L));
        assertEquals(1L << 40, ceilingPowerOfTwo((1L << 40) - 1));
        assertEquals(1L << 40, ceilingPowerOfTwo(1L << 40));

        assertEquals(BigInteger.ONE, ceilingPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.valueOf(2), ceilingPowerOfTwo(BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(4), ceilingPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(1024), ceilingPowerOfTwo(BigInteger.valueOf(1000)));
    }

    @Test
    public void testFloorPowerOfTwo() {
        assertEquals(1L, floorPowerOfTwo(1L));
        assertEquals(2L, floorPowerOfTwo(2L));
        assertEquals(2L, floorPowerOfTwo(3L));
        assertEquals(4L, floorPowerOfTwo(4L));
        assertEquals(4L, floorPowerOfTwo(5L));
        assertEquals(8L, floorPowerOfTwo(15L));
        assertEquals(16L, floorPowerOfTwo(16L));
        assertEquals(16L, floorPowerOfTwo(17L));
        assertEquals(1L << 39, floorPowerOfTwo((1L << 40) - 1));
        assertEquals(1L << 40, floorPowerOfTwo(1L << 40));

        assertEquals(BigInteger.ONE, floorPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.valueOf(2), floorPowerOfTwo(BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(2), floorPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(512), floorPowerOfTwo(BigInteger.valueOf(1000)));
    }

    @Test
    public void testSqrt() {
        assertEquals(0, sqrt(0, RoundingMode.UNNECESSARY));
        assertEquals(1, sqrt(1, RoundingMode.UNNECESSARY));
        assertEquals(2, sqrt(4, RoundingMode.UNNECESSARY));
        assertEquals(3, sqrt(9, RoundingMode.UNNECESSARY));
        assertEquals(10, sqrt(100, RoundingMode.UNNECESSARY));

        assertEquals(2, sqrt(5, RoundingMode.FLOOR));
        assertEquals(3, sqrt(5, RoundingMode.CEILING));
        assertEquals(2, sqrt(5, RoundingMode.DOWN));
        assertEquals(3, sqrt(5, RoundingMode.UP));
        assertEquals(2, sqrt(5, RoundingMode.HALF_DOWN));
        assertEquals(2, sqrt(5, RoundingMode.HALF_UP));
        assertEquals(2, sqrt(5, RoundingMode.HALF_EVEN));

        assertEquals(0L, sqrt(0L, RoundingMode.UNNECESSARY));
        assertEquals(1L, sqrt(1L, RoundingMode.UNNECESSARY));
        assertEquals(1000L, sqrt(1000000L, RoundingMode.UNNECESSARY));
        assertEquals(31622L, sqrt(999999999L, RoundingMode.FLOOR));
        assertEquals(31623L, sqrt(999999999L, RoundingMode.CEILING));

        assertEquals(BigInteger.ZERO, sqrt(BigInteger.ZERO, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.ONE, sqrt(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(1000), sqrt(BigInteger.valueOf(1000000), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(31622), sqrt(BigInteger.valueOf(999999999), RoundingMode.FLOOR));
    }

    @Test
    public void testDivide() {
        assertEquals(3, divide(10, 3, RoundingMode.FLOOR));
        assertEquals(4, divide(10, 3, RoundingMode.CEILING));
        assertEquals(3, divide(10, 3, RoundingMode.DOWN));
        assertEquals(4, divide(10, 3, RoundingMode.UP));
        assertEquals(3, divide(10, 3, RoundingMode.HALF_DOWN));
        assertEquals(3, divide(10, 3, RoundingMode.HALF_UP));
        assertEquals(3, divide(10, 3, RoundingMode.HALF_EVEN));

        assertEquals(-4, divide(-10, 3, RoundingMode.FLOOR));
        assertEquals(-3, divide(-10, 3, RoundingMode.CEILING));
        assertEquals(-3, divide(-10, 3, RoundingMode.DOWN));
        assertEquals(-4, divide(-10, 3, RoundingMode.UP));

        assertEquals(5, divide(10, 2, RoundingMode.UNNECESSARY));

        assertEquals(3L, divide(10L, 3L, RoundingMode.FLOOR));
        assertEquals(4L, divide(10L, 3L, RoundingMode.CEILING));
        assertEquals(333333333L, divide(1000000000L, 3L, RoundingMode.FLOOR));
        assertEquals(333333334L, divide(1000000000L, 3L, RoundingMode.CEILING));

        assertEquals(BigInteger.valueOf(3), divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(4), divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.CEILING));

        try {
            divide(10, 0, RoundingMode.FLOOR);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testMod() {
        assertEquals(2, mod(5, 3));
        assertEquals(1, mod(-5, 3));
        assertEquals(0, mod(6, 3));
        assertEquals(2, mod(-1, 3));

        assertEquals(2, mod(5L, 3));
        assertEquals(1, mod(-5L, 3));

        assertEquals(2L, mod(5L, 3L));
        assertEquals(1L, mod(-5L, 3L));
        assertEquals(1000000000L, mod(10000000001L, 9000000001L));

        try {
            mod(5, 0);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            mod(5L, -1L);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testGcd() {
        assertEquals(1, gcd(3, 5));
        assertEquals(2, gcd(4, 6));
        assertEquals(3, gcd(9, 12));
        assertEquals(5, gcd(10, 15));
        assertEquals(10, gcd(10, 0));
        assertEquals(10, gcd(0, 10));
        assertEquals(0, gcd(0, 0));
        assertEquals(1, gcd(1, 1));
        assertEquals(7, gcd(7, 7));

        assertEquals(5, gcd(-10, 15));
        assertEquals(5, gcd(10, -15));
        assertEquals(5, gcd(-10, -15));

        assertEquals(1L, gcd(3L, 5L));
        assertEquals(2L, gcd(4L, 6L));
        assertEquals(1000000L, gcd(5000000L, 7000000L));
        assertEquals(0L, gcd(0L, 0L));

        try {
            gcd(0, Integer.MIN_VALUE);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            gcd(0L, Long.MIN_VALUE);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testLcm() {
        assertEquals(15, lcm(3, 5));
        assertEquals(12, lcm(4, 6));
        assertEquals(36, lcm(9, 12));
        assertEquals(30, lcm(10, 15));
        assertEquals(0, lcm(10, 0));
        assertEquals(0, lcm(0, 10));
        assertEquals(0, lcm(0, 0));
        assertEquals(1, lcm(1, 1));
        assertEquals(7, lcm(7, 7));

        assertEquals(30, lcm(-10, 15));
        assertEquals(30, lcm(10, -15));
        assertEquals(30, lcm(-10, -15));

        assertEquals(15L, lcm(3L, 5L));
        assertEquals(12L, lcm(4L, 6L));
        assertEquals(35000000L, lcm(5000000L, 7000000L));
        assertEquals(0L, lcm(0L, 0L));
    }

    @Test
    public void testAddExact() {
        assertEquals(5, addExact(2, 3));
        assertEquals(-1, addExact(2, -3));
        assertEquals(0, addExact(-5, 5));
        assertEquals(Integer.MAX_VALUE, addExact(Integer.MAX_VALUE, 0));
        assertEquals(Integer.MIN_VALUE, addExact(Integer.MIN_VALUE, 0));

        try {
            addExact(Integer.MAX_VALUE, 1);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            addExact(Integer.MIN_VALUE, -1);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        assertEquals(5L, addExact(2L, 3L));
        assertEquals(-1L, addExact(2L, -3L));
        assertEquals(Long.MAX_VALUE, addExact(Long.MAX_VALUE, 0L));

        try {
            addExact(Long.MAX_VALUE, 1L);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testSubtractExact() {
        assertEquals(-1, subtractExact(2, 3));
        assertEquals(5, subtractExact(2, -3));
        assertEquals(-10, subtractExact(-5, 5));
        assertEquals(Integer.MAX_VALUE, subtractExact(Integer.MAX_VALUE, 0));
        assertEquals(Integer.MIN_VALUE, subtractExact(Integer.MIN_VALUE, 0));

        try {
            subtractExact(Integer.MIN_VALUE, 1);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            subtractExact(Integer.MAX_VALUE, -1);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        assertEquals(-1L, subtractExact(2L, 3L));
        assertEquals(5L, subtractExact(2L, -3L));
        assertEquals(Long.MAX_VALUE, subtractExact(Long.MAX_VALUE, 0L));

        try {
            subtractExact(Long.MIN_VALUE, 1L);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testMultiplyExact() {
        assertEquals(6, multiplyExact(2, 3));
        assertEquals(-6, multiplyExact(2, -3));
        assertEquals(0, multiplyExact(0, 5));
        assertEquals(0, multiplyExact(5, 0));
        assertEquals(1000000, multiplyExact(1000, 1000));

        try {
            multiplyExact(Integer.MAX_VALUE, 2);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            multiplyExact(100000, 100000);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        assertEquals(6L, multiplyExact(2L, 3L));
        assertEquals(-6L, multiplyExact(2L, -3L));
        assertEquals(1000000000000L, multiplyExact(1000000L, 1000000L));

        try {
            multiplyExact(Long.MAX_VALUE, 2L);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testPowExact() {
        assertEquals(1, powExact(2, 0));
        assertEquals(2, powExact(2, 1));
        assertEquals(4, powExact(2, 2));
        assertEquals(8, powExact(2, 3));
        assertEquals(1024, powExact(2, 10));
        assertEquals(1, powExact(1, 1000));
        assertEquals(1, powExact(-1, 0));
        assertEquals(-1, powExact(-1, 1));
        assertEquals(1, powExact(-1, 2));
        assertEquals(9, powExact(3, 2));

        try {
            powExact(2, 31);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            powExact(10, 10);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        assertEquals(1L, powExact(2L, 0));
        assertEquals(2L, powExact(2L, 1));
        assertEquals(1024L, powExact(2L, 10));
        assertEquals(1L << 40, powExact(2L, 40));

        try {
            powExact(2L, 63);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testSaturatedAdd() {
        assertEquals(5, saturatedAdd(2, 3));
        assertEquals(-1, saturatedAdd(2, -3));
        assertEquals(Integer.MAX_VALUE, saturatedAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MIN_VALUE, saturatedAdd(Integer.MIN_VALUE, -1));
        assertEquals(Integer.MAX_VALUE, saturatedAdd(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, saturatedAdd(Integer.MIN_VALUE, Integer.MIN_VALUE));

        assertEquals(5L, saturatedAdd(2L, 3L));
        assertEquals(-1L, saturatedAdd(2L, -3L));
        assertEquals(Long.MAX_VALUE, saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE, saturatedAdd(Long.MIN_VALUE, -1L));
        assertEquals(Long.MAX_VALUE, saturatedAdd(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, saturatedAdd(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testSaturatedSubtract() {
        assertEquals(-1, saturatedSubtract(2, 3));
        assertEquals(5, saturatedSubtract(2, -3));
        assertEquals(Integer.MIN_VALUE, saturatedSubtract(Integer.MIN_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, saturatedSubtract(Integer.MAX_VALUE, -1));
        assertEquals(Integer.MAX_VALUE, saturatedSubtract(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, saturatedSubtract(Integer.MIN_VALUE, Integer.MAX_VALUE));

        assertEquals(-1L, saturatedSubtract(2L, 3L));
        assertEquals(5L, saturatedSubtract(2L, -3L));
        assertEquals(Long.MIN_VALUE, saturatedSubtract(Long.MIN_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, saturatedSubtract(Long.MAX_VALUE, -1L));
        assertEquals(Long.MAX_VALUE, saturatedSubtract(Long.MAX_VALUE, Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, saturatedSubtract(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void testSaturatedMultiply() {
        assertEquals(6, saturatedMultiply(2, 3));
        assertEquals(-6, saturatedMultiply(2, -3));
        assertEquals(Integer.MAX_VALUE, saturatedMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, saturatedMultiply(Integer.MIN_VALUE, 2));
        assertEquals(Integer.MAX_VALUE, saturatedMultiply(100000, 100000));
        assertEquals(Integer.MIN_VALUE, saturatedMultiply(-100000, 100000));

        assertEquals(6L, saturatedMultiply(2L, 3L));
        assertEquals(-6L, saturatedMultiply(2L, -3L));
        assertEquals(Long.MAX_VALUE, saturatedMultiply(Long.MAX_VALUE, 2L));
        assertEquals(Long.MIN_VALUE, saturatedMultiply(Long.MIN_VALUE, 2L));
        assertEquals(Long.MAX_VALUE, saturatedMultiply(1000000000000L, 1000000000000L));
    }

    @Test
    public void testSaturatedPow() {
        assertEquals(1, saturatedPow(2, 0));
        assertEquals(2, saturatedPow(2, 1));
        assertEquals(4, saturatedPow(2, 2));
        assertEquals(8, saturatedPow(2, 3));
        assertEquals(1024, saturatedPow(2, 10));
        assertEquals(Integer.MAX_VALUE, saturatedPow(2, 31));
        assertEquals(Integer.MAX_VALUE, saturatedPow(10, 10));
        assertEquals(1, saturatedPow(-1, 0));
        assertEquals(-1, saturatedPow(-1, 1));
        assertEquals(1, saturatedPow(-1, 2));
        assertEquals(Integer.MIN_VALUE, saturatedPow(-2, 31));

        assertEquals(1L, saturatedPow(2L, 0));
        assertEquals(2L, saturatedPow(2L, 1));
        assertEquals(1024L, saturatedPow(2L, 10));
        assertEquals(1L << 40, saturatedPow(2L, 40));
        assertEquals(Long.MAX_VALUE, saturatedPow(2L, 63));
        assertEquals(Long.MAX_VALUE, saturatedPow(10L, 20));
    }

    @Test
    public void testSaturatedCast() {
        assertEquals(100, saturatedCast(100L));
        assertEquals(-100, saturatedCast(-100L));
        assertEquals(Integer.MAX_VALUE, saturatedCast(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, saturatedCast(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, saturatedCast(Long.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, saturatedCast(Long.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, saturatedCast(Integer.MAX_VALUE + 1L));
        assertEquals(Integer.MIN_VALUE, saturatedCast(Integer.MIN_VALUE - 1L));
    }

    @Test
    public void testFactorial() {
        assertEquals(1, factorial(0));
        assertEquals(1, factorial(1));
        assertEquals(2, factorial(2));
        assertEquals(6, factorial(3));
        assertEquals(24, factorial(4));
        assertEquals(120, factorial(5));
        assertEquals(720, factorial(6));
        assertEquals(5040, factorial(7));
        assertEquals(40320, factorial(8));
        assertEquals(362880, factorial(9));
        assertEquals(3628800, factorial(10));
        assertEquals(39916800, factorial(11));
        assertEquals(479001600, factorial(12));
        assertEquals(Integer.MAX_VALUE, factorial(13));
        assertEquals(Integer.MAX_VALUE, factorial(20));

        try {
            factorial(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testFactorialToLong() {
        assertEquals(1L, factorialToLong(0));
        assertEquals(1L, factorialToLong(1));
        assertEquals(2L, factorialToLong(2));
        assertEquals(6L, factorialToLong(3));
        assertEquals(24L, factorialToLong(4));
        assertEquals(120L, factorialToLong(5));
        assertEquals(3628800L, factorialToLong(10));
        assertEquals(479001600L, factorialToLong(12));
        assertEquals(6227020800L, factorialToLong(13));
        assertEquals(87178291200L, factorialToLong(14));
        assertEquals(1307674368000L, factorialToLong(15));
        assertEquals(20922789888000L, factorialToLong(16));
        assertEquals(355687428096000L, factorialToLong(17));
        assertEquals(6402373705728000L, factorialToLong(18));
        assertEquals(121645100408832000L, factorialToLong(19));
        assertEquals(2432902008176640000L, factorialToLong(20));
        assertEquals(Long.MAX_VALUE, factorialToLong(21));

        try {
            factorialToLong(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testFactorialToDouble() {
        assertEquals(1.0, factorialToDouble(0), DELTA);
        assertEquals(1.0, factorialToDouble(1), DELTA);
        assertEquals(2.0, factorialToDouble(2), DELTA);
        assertEquals(6.0, factorialToDouble(3), DELTA);
        assertEquals(24.0, factorialToDouble(4), DELTA);
        assertEquals(120.0, factorialToDouble(5), DELTA);
        assertEquals(3628800.0, factorialToDouble(10), DELTA);
        assertEquals(2.432902008176640000e18, factorialToDouble(20), 1e6);
        assertEquals(1.2164510040883200e17, factorialToDouble(19), 1e5);

        assertTrue(factorialToDouble(100) > 1e100);
        assertTrue(factorialToDouble(170) < Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, factorialToDouble(171), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, factorialToDouble(200), DELTA);

        try {
            factorialToDouble(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testFactorialToBigInteger() {
        assertEquals(BigInteger.ONE, factorialToBigInteger(0));
        assertEquals(BigInteger.ONE, factorialToBigInteger(1));
        assertEquals(BigInteger.valueOf(2), factorialToBigInteger(2));
        assertEquals(BigInteger.valueOf(6), factorialToBigInteger(3));
        assertEquals(BigInteger.valueOf(24), factorialToBigInteger(4));
        assertEquals(BigInteger.valueOf(3628800), factorialToBigInteger(10));
        assertEquals(BigInteger.valueOf(2432902008176640000L), factorialToBigInteger(20));

        BigInteger fact50 = factorialToBigInteger(50);
        assertTrue(fact50.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
        assertTrue(fact50.toString().startsWith("30414093201713378"));

        BigInteger fact100 = factorialToBigInteger(100);
        assertTrue(fact100.bitLength() > 500);

        try {
            factorialToBigInteger(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testBinomial() {
        assertEquals(1, binomial(0, 0));
        assertEquals(1, binomial(5, 0));
        assertEquals(5, binomial(5, 1));
        assertEquals(10, binomial(5, 2));
        assertEquals(10, binomial(5, 3));
        assertEquals(5, binomial(5, 4));
        assertEquals(1, binomial(5, 5));

        assertEquals(35, binomial(7, 3));
        assertEquals(35, binomial(7, 4));
        assertEquals(120, binomial(10, 3));
        assertEquals(252, binomial(10, 5));

        assertEquals(Integer.MAX_VALUE, binomial(50, 25));

        try {
            binomial(-1, 0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            binomial(5, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            binomial(5, 6);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testBinomialToLong() {
        assertEquals(1L, binomialToLong(0, 0));
        assertEquals(1L, binomialToLong(5, 0));
        assertEquals(5L, binomialToLong(5, 1));
        assertEquals(10L, binomialToLong(5, 2));
        assertEquals(10L, binomialToLong(5, 3));
        assertEquals(5L, binomialToLong(5, 4));
        assertEquals(1L, binomialToLong(5, 5));

        assertEquals(35L, binomialToLong(7, 3));
        assertEquals(120L, binomialToLong(10, 3));
        assertEquals(252L, binomialToLong(10, 5));
        assertEquals(184756L, binomialToLong(20, 10));

        assertEquals(Long.MAX_VALUE, binomialToLong(70, 35));

        try {
            binomialToLong(-1, 0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            binomialToLong(5, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            binomialToLong(5, 6);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testBinomialToBigInteger() {
        assertEquals(BigInteger.ONE, binomialToBigInteger(0, 0));
        assertEquals(BigInteger.ONE, binomialToBigInteger(5, 0));
        assertEquals(BigInteger.valueOf(5), binomialToBigInteger(5, 1));
        assertEquals(BigInteger.valueOf(10), binomialToBigInteger(5, 2));
        assertEquals(BigInteger.valueOf(10), binomialToBigInteger(5, 3));
        assertEquals(BigInteger.valueOf(5), binomialToBigInteger(5, 4));
        assertEquals(BigInteger.ONE, binomialToBigInteger(5, 5));

        assertEquals(BigInteger.valueOf(35), binomialToBigInteger(7, 3));
        assertEquals(BigInteger.valueOf(184756), binomialToBigInteger(20, 10));

        BigInteger result = binomialToBigInteger(100, 50);
        assertTrue(result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);

        try {
            binomialToBigInteger(-1, 0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            binomialToBigInteger(5, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            binomialToBigInteger(5, 6);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testMeanInt() {
        assertEquals(2, mean(1, 3));
        assertEquals(2, mean(2, 3));
        assertEquals(5, mean(4, 6));
        assertEquals(-1, mean(-2, 0));
        assertEquals(-2, mean(-3, -1));

        assertEquals(Integer.MAX_VALUE - 1, mean(Integer.MAX_VALUE, Integer.MAX_VALUE - 2));
        assertEquals(Integer.MIN_VALUE + 1, mean(Integer.MIN_VALUE, Integer.MIN_VALUE + 2));
        assertEquals(-1, mean(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void testMeanLong() {
        assertEquals(2L, mean(1L, 3L));
        assertEquals(2L, mean(2L, 3L));
        assertEquals(5L, mean(4L, 6L));
        assertEquals(-1L, mean(-2L, 0L));
        assertEquals(-2L, mean(-3L, -1L));

        assertEquals(Long.MAX_VALUE - 1, mean(Long.MAX_VALUE, Long.MAX_VALUE - 2));
        assertEquals(Long.MIN_VALUE + 1, mean(Long.MIN_VALUE, Long.MIN_VALUE + 2));
        assertEquals(-1L, mean(Long.MAX_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testMeanDouble() {
        assertEquals(2.5, mean(2.0, 3.0), DELTA);
        assertEquals(5.0, mean(4.0, 6.0), DELTA);
        assertEquals(-1.5, mean(-2.0, -1.0), DELTA);
        assertEquals(0.0, mean(-1.0, 1.0), DELTA);

        assertEquals(Double.MAX_VALUE, mean(Double.MAX_VALUE, Double.MAX_VALUE), DELTA);
        assertEquals(0.0, (-Double.MAX_VALUE + Double.MAX_VALUE) / 2, DELTA);
        assertEquals(0.0, mean(-Double.MAX_VALUE, Double.MAX_VALUE), DELTA);
    }

    @Test
    public void testMeanIntArray() {
        assertEquals(3.0, mean(1, 2, 3, 4, 5), DELTA);
        assertEquals(2.5, mean(1, 2, 3, 4), DELTA);
        assertEquals(5.0, mean(5), DELTA);
        assertEquals(0.0, mean(-5, -3, -1, 1, 3, 5), DELTA);

        int[] largeValues = new int[1000];
        for (int i = 0; i < 1000; i++) {
            largeValues[i] = i;
        }
        assertEquals(499.5, mean(largeValues), DELTA);

        try {
            mean(new int[0]);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testMeanLongArray() {
        assertEquals(3.0, mean(1L, 2L, 3L, 4L, 5L), DELTA);
        assertEquals(2.5, mean(1L, 2L, 3L, 4L), DELTA);
        assertEquals(5.0, mean(5L), DELTA);
        assertEquals(0.0, mean(-5L, -3L, -1L, 1L, 3L, 5L), DELTA);

        assertEquals(5000000000.0, mean(4000000000L, 5000000000L, 6000000000L), DELTA);

        try {
            mean(new long[0]);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testMeanDoubleArray() {
        assertEquals(3.0, mean(1.0, 2.0, 3.0, 4.0, 5.0), DELTA);
        assertEquals(2.5, mean(1.0, 2.0, 3.0, 4.0), DELTA);
        assertEquals(5.0, mean(5.0), DELTA);
        assertEquals(0.0, mean(-5.0, -3.0, -1.0, 1.0, 3.0, 5.0), DELTA);

        assertEquals(3.14159, mean(3.14159), DELTA);
        assertEquals(2.5, mean(1.5, 2.5, 3.5), DELTA);

        try {
            mean(new double[0]);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            mean(1.0, Double.NaN, 3.0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            mean(1.0, Double.POSITIVE_INFINITY, 3.0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testRound() {
        assertEquals(3.14f, round(3.14159f, 2), FLOAT_DELTA);
        assertEquals(3.1f, round(3.14159f, 1), FLOAT_DELTA);
        assertEquals(3.0f, round(3.14159f, 0), FLOAT_DELTA);
        assertEquals(-3.14f, round(-3.14159f, 2), FLOAT_DELTA);
        assertEquals(3.142f, round(3.14159f, 3), FLOAT_DELTA);

        assertEquals(3.14, round(3.14159265359, 2), DELTA);
        assertEquals(3.1, round(3.14159265359, 1), DELTA);
        assertEquals(3.0, round(3.14159265359, 0), DELTA);
        assertEquals(-3.14, round(-3.14159265359, 2), DELTA);
        assertEquals(3.142, round(3.14159265359, 3), DELTA);
        assertEquals(3.14159, round(3.14159265359, 5), DELTA);

        assertEquals(3.14f, round(3.14159f, 2, RoundingMode.HALF_UP), FLOAT_DELTA);
        assertEquals(3.15f, round(3.145f, 2, RoundingMode.HALF_UP), FLOAT_DELTA);
        assertEquals(3.15d, round(3.145d, 2, RoundingMode.HALF_UP), FLOAT_DELTA);
        assertEquals(3.14f, round(3.145f, 2, RoundingMode.HALF_DOWN), FLOAT_DELTA);
        assertEquals(3.14f, round(3.145f, 2, RoundingMode.HALF_EVEN), FLOAT_DELTA);
        assertEquals(3.15f, round(3.145f, 2, RoundingMode.UP), FLOAT_DELTA);
        assertEquals(3.14f, round(3.145f, 2, RoundingMode.DOWN), FLOAT_DELTA);
        assertEquals(3.15f, round(3.141f, 2, RoundingMode.CEILING), FLOAT_DELTA);
        assertEquals(3.14f, round(3.149f, 2, RoundingMode.FLOOR), FLOAT_DELTA);

        assertEquals(3.14, round(3.14159, 2, RoundingMode.HALF_UP), DELTA);
        assertEquals(3.15, round(3.145, 2, RoundingMode.HALF_UP), DELTA);
        assertEquals(3.14, round(3.145, 2, RoundingMode.HALF_DOWN), DELTA);

        assertEquals(3.14f, round(3.14159f, "0.00"), FLOAT_DELTA);
        assertEquals(3.1f, round(3.14159f, "#.#"), FLOAT_DELTA);

        assertEquals(3.14, round(3.14159, "0.00"), DELTA);
        assertEquals(3.1, round(3.14159, "#.#"), DELTA);

        DecimalFormat df = new DecimalFormat("0.00");
        assertEquals(3.14f, round(3.14159f, df), FLOAT_DELTA);
        assertEquals(3.14, round(3.14159, df), DELTA);

        try {
            round(3.14, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testRoundToInt() {
        assertEquals(3, roundToInt(3.14, RoundingMode.DOWN));
        assertEquals(4, roundToInt(3.14, RoundingMode.UP));
        assertEquals(3, roundToInt(3.14, RoundingMode.FLOOR));
        assertEquals(4, roundToInt(3.14, RoundingMode.CEILING));
        assertEquals(3, roundToInt(3.14, RoundingMode.HALF_UP));
        assertEquals(4, roundToInt(3.5, RoundingMode.HALF_UP));
        assertEquals(3, roundToInt(3.5, RoundingMode.HALF_DOWN));
        assertEquals(4, roundToInt(4.5, RoundingMode.HALF_EVEN));
        assertEquals(4, roundToInt(3.5, RoundingMode.HALF_EVEN));

        assertEquals(-3, roundToInt(-3.14, RoundingMode.DOWN));
        assertEquals(-4, roundToInt(-3.14, RoundingMode.UP));
        assertEquals(-4, roundToInt(-3.14, RoundingMode.FLOOR));
        assertEquals(-3, roundToInt(-3.14, RoundingMode.CEILING));

        assertEquals(5, roundToInt(5.0, RoundingMode.UNNECESSARY));

        try {
            roundToInt(Integer.MAX_VALUE + 1.0, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            roundToInt(Double.NaN, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            roundToInt(Double.POSITIVE_INFINITY, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testRoundToLong() {
        assertEquals(3L, roundToLong(3.14, RoundingMode.DOWN));
        assertEquals(4L, roundToLong(3.14, RoundingMode.UP));
        assertEquals(3L, roundToLong(3.14, RoundingMode.FLOOR));
        assertEquals(4L, roundToLong(3.14, RoundingMode.CEILING));
        assertEquals(3L, roundToLong(3.14, RoundingMode.HALF_UP));
        assertEquals(4L, roundToLong(3.5, RoundingMode.HALF_UP));
        assertEquals(3L, roundToLong(3.5, RoundingMode.HALF_DOWN));
        assertEquals(4L, roundToLong(4.5, RoundingMode.HALF_EVEN));
        assertEquals(4L, roundToLong(3.5, RoundingMode.HALF_EVEN));

        assertEquals(-3L, roundToLong(-3.14, RoundingMode.DOWN));
        assertEquals(-4L, roundToLong(-3.14, RoundingMode.UP));
        assertEquals(-4L, roundToLong(-3.14, RoundingMode.FLOOR));
        assertEquals(-3L, roundToLong(-3.14, RoundingMode.CEILING));

        assertEquals(5L, roundToLong(5.0, RoundingMode.UNNECESSARY));

        assertEquals(1234567890123L, roundToLong(1234567890123.0, RoundingMode.UNNECESSARY));
        assertEquals(1234567890123L, roundToLong(1234567890123.4, RoundingMode.DOWN));
        assertEquals(1234567890124L, roundToLong(1234567890123.6, RoundingMode.UP));

        try {
            roundToLong(Double.NaN, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            roundToLong(Double.POSITIVE_INFINITY, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testRoundToBigInteger() {
        assertEquals(BigInteger.valueOf(3), roundToBigInteger(3.14, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), roundToBigInteger(3.14, RoundingMode.UP));
        assertEquals(BigInteger.valueOf(3), roundToBigInteger(3.14, RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(4), roundToBigInteger(3.14, RoundingMode.CEILING));
        assertEquals(BigInteger.valueOf(3), roundToBigInteger(3.14, RoundingMode.HALF_UP));
        assertEquals(BigInteger.valueOf(4), roundToBigInteger(3.5, RoundingMode.HALF_UP));
        assertEquals(BigInteger.valueOf(3), roundToBigInteger(3.5, RoundingMode.HALF_DOWN));
        assertEquals(BigInteger.valueOf(4), roundToBigInteger(4.5, RoundingMode.HALF_EVEN));
        assertEquals(BigInteger.valueOf(4), roundToBigInteger(3.5, RoundingMode.HALF_EVEN));

        assertEquals(BigInteger.valueOf(-3), roundToBigInteger(-3.14, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(-4), roundToBigInteger(-3.14, RoundingMode.UP));
        assertEquals(BigInteger.valueOf(-4), roundToBigInteger(-3.14, RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(-3), roundToBigInteger(-3.14, RoundingMode.CEILING));

        assertEquals(BigInteger.valueOf(5), roundToBigInteger(5.0, RoundingMode.UNNECESSARY));

        double largeValue = 1e100;
        BigInteger result = roundToBigInteger(largeValue, RoundingMode.DOWN);
        assertTrue(result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);

        try {
            roundToBigInteger(Double.NaN, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }

        try {
            roundToBigInteger(Double.POSITIVE_INFINITY, RoundingMode.DOWN);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
        }
    }

    @Test
    public void testFuzzyEquals() {
        assertTrue(Numbers.fuzzyEquals(1.0001f, 1.0002f, 0.001f));
        assertFalse(Numbers.fuzzyEquals(1.0f, 1.1f, 0.01f));
        assertTrue(fuzzyEquals(1.0, 1.0, 0.0));
        assertFalse(Math.abs(1.0 - 1.1) <= 0.1);
        assertTrue(fuzzyEquals(1.0, 1.1, 0.10001));
        assertTrue(fuzzyEquals(1.0, 1.05, 0.1));
        assertFalse(fuzzyEquals(1.0, 1.2, 0.1));

        assertTrue(fuzzyEquals(-1.0, -1.0, 0.0));
        assertTrue(fuzzyEquals(-1.0, -1.1, 0.10001));
        assertFalse(fuzzyEquals(-1.0, -1.2, 0.1));

        assertTrue(fuzzyEquals(0.0, 0.0, 0.0));
        assertTrue(fuzzyEquals(0.0, 0.1, 0.1));
        assertTrue(fuzzyEquals(-0.0, 0.0, 0.0));

        assertTrue(fuzzyEquals(Double.NaN, Double.NaN, 0.1));
        assertFalse(fuzzyEquals(Double.NaN, 1.0, 0.1));
        assertFalse(fuzzyEquals(1.0, Double.NaN, 0.1));

        assertTrue(fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0));
        assertTrue(fuzzyEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0));
        assertTrue(fuzzyEquals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertTrue(fuzzyEquals(Double.POSITIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY));

        assertTrue(fuzzyEquals(1.0, 1.0 + 1e-10, 1e-9));
        assertFalse(fuzzyEquals(1.0, 1.0 + 1e-10, 1e-11));

        try {
            fuzzyEquals(1.0, 1.0, -0.1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testFuzzyCompare() {
        assertEquals(0, fuzzyCompare(1.0, 1.0, 0.0));
        assertEquals(0, fuzzyCompare(1.0, 1.05, 0.1));
        assertEquals(-1, fuzzyCompare(1.0, 1.2, 0.1));
        assertEquals(1, fuzzyCompare(1.2, 1.0, 0.1));

        assertEquals(0, fuzzyCompare(-1.0, -1.05, 0.1));
        assertEquals(-1, fuzzyCompare(-1.2, -1.0, 0.1));
        assertEquals(1, fuzzyCompare(-1.0, -1.2, 0.1));

        assertEquals(0, fuzzyCompare(0.0, 0.0, 0.0));
        assertEquals(0, fuzzyCompare(0.0, 0.05, 0.1));
        assertEquals(0, fuzzyCompare(-0.0, 0.0, 0.0));

        assertEquals(0, fuzzyCompare(Double.NaN, Double.NaN, 0.1));
        assertEquals(1, fuzzyCompare(Double.NaN, 1.0, 0.1));
        assertEquals(-1, fuzzyCompare(1.0, Double.NaN, 0.1));

        assertEquals(0, fuzzyCompare(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0));
        assertEquals(0, fuzzyCompare(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(1, fuzzyCompare(Double.POSITIVE_INFINITY, 1.0, 0.0));

        try {
            fuzzyCompare(1.0, 1.0, -0.1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testIsMathematicalInteger() {
        assertTrue(isMathematicalInteger(0.0));
        assertTrue(isMathematicalInteger(1.0));
        assertTrue(isMathematicalInteger(-1.0));
        assertTrue(isMathematicalInteger(100.0));
        assertTrue(isMathematicalInteger(-100.0));
        assertTrue(isMathematicalInteger(1234567890.0));

        assertFalse(isMathematicalInteger(0.1));
        assertFalse(isMathematicalInteger(0.5));
        assertFalse(isMathematicalInteger(1.1));
        assertFalse(isMathematicalInteger(-1.1));
        assertFalse(isMathematicalInteger(Math.PI));
        assertFalse(isMathematicalInteger(Math.E));

        assertFalse(isMathematicalInteger(Double.NaN));
        assertFalse(isMathematicalInteger(Double.POSITIVE_INFINITY));
        assertFalse(isMathematicalInteger(Double.NEGATIVE_INFINITY));

        assertTrue(isMathematicalInteger(Math.pow(2, 52)));
        assertTrue(isMathematicalInteger(Math.pow(2, 53) - 1));
        assertTrue(isMathematicalInteger(-0.0));
    }

    @Test
    public void testAsinh() {
        assertEquals(0.0, asinh(0.0), DELTA);
        assertEquals(-asinh(1.0), asinh(-1.0), DELTA);
        assertEquals(Math.log(2.0 + Math.sqrt(5.0)), asinh(2.0), DELTA);

        double result = FastMath.asinh(0.1);
        System.out.println(result);

        assertEquals(0.001, asinh(0.001), 1e-9);
        assertEquals(0.01, asinh(0.01), 1e-6);
        assertEquals(0.1, asinh(0.1), 1e-3);

        assertEquals(Math.log(10.0 + Math.sqrt(101.0)), asinh(10.0), DELTA);
        assertEquals(Math.log(100.0 + Math.sqrt(10001.0)), asinh(100.0), DELTA);

        assertEquals(-Math.log(10.0 + Math.sqrt(101.0)), asinh(-10.0), DELTA);
    }

    @Test
    public void testAcosh() {
        assertEquals(0.0, acosh(1.0), DELTA);
        assertEquals(Math.log(2.0 + Math.sqrt(3.0)), acosh(2.0), DELTA);
        assertEquals(Math.log(10.0 + Math.sqrt(99.0)), acosh(10.0), DELTA);

        assertEquals(Math.log(100.0 + Math.sqrt(9999.0)), acosh(100.0), DELTA);
        assertEquals(Math.log(1000.0 + Math.sqrt(999999.0)), acosh(1000.0), DELTA);
    }

    @Test
    public void testAtanh() {
        assertEquals(0.0, atanh(0.0), DELTA);
        assertEquals(-atanh(0.5), atanh(-0.5), DELTA);

        assertEquals(0.001, atanh(0.001), 1e-9);
        assertEquals(0.01, atanh(0.01), 1e-6);
        assertEquals(0.1, atanh(0.1), 1e-3);

        assertEquals(0.5 * Math.log(3.0), atanh(0.5), DELTA);
        assertEquals(0.5 * Math.log(9.0), atanh(0.8), DELTA);

        assertEquals(-0.5 * Math.log(3.0), atanh(-0.5), DELTA);
        assertEquals(-0.5 * Math.log(9.0), atanh(-0.8), DELTA);
    }
}
