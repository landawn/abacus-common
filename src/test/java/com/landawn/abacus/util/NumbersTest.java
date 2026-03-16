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
import static com.landawn.abacus.util.Numbers.factorial;
import static com.landawn.abacus.util.Numbers.factorialToBigInteger;
import static com.landawn.abacus.util.Numbers.format;
import static com.landawn.abacus.util.Numbers.fuzzyEquals;
import static com.landawn.abacus.util.Numbers.gcd;
import static com.landawn.abacus.util.Numbers.isConvertibleToNumber;
import static com.landawn.abacus.util.Numbers.isParsable;
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
import static com.landawn.abacus.util.Numbers.saturatedMultiply;
import static com.landawn.abacus.util.Numbers.saturatedPow;
import static com.landawn.abacus.util.Numbers.sqrt;
import static com.landawn.abacus.util.Numbers.toByte;
import static com.landawn.abacus.util.Numbers.toDouble;
import static com.landawn.abacus.util.Numbers.toFloat;
import static com.landawn.abacus.util.Numbers.toInt;
import static com.landawn.abacus.util.Numbers.toLong;
import static com.landawn.abacus.util.Numbers.toScaledBigDecimal;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.math.IntMath;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class NumbersTest extends TestBase {

    private static final double DELTA = 1e-15;
    private static final float FLOAT_DELTA = 1e-7f;

    @Test
    public void test_isParsable() {
        assertEquals("12.16%", Numbers.format(0.12156, "#.##%"));

        assertFalse(Numbers.isParsable("+"));
        assertFalse(Numbers.isParsable("-"));
        assertFalse(Numbers.isParsable("."));
        assertFalse(Numbers.isParsable("0."));
    }

    @Test
    public void test_to() {
        assertEquals(true, Strings.parseBoolean("True"));
        assertEquals(1, Numbers.toByte("1"));
        assertEquals(1, Numbers.toShort("1"));
        assertEquals(1, Numbers.toInt("1"));
        assertEquals(0, Numbers.toInt(""));
        assertEquals(1, Numbers.toLong("1"));
        assertEquals(1, Numbers.toLong("1l"));
        assertEquals(0, Numbers.toLong("0L"));
        assertEquals(0, Numbers.toLong(""));
        assertEquals(1f, Numbers.toFloat("1"));
        assertEquals(1f, Numbers.toFloat("1f"));
        assertEquals(0f, Numbers.toFloat("0F"));
        assertEquals(0f, Numbers.toFloat(""));
        assertEquals(1d, Numbers.toDouble("1"));
        assertEquals(1d, Numbers.toDouble("1d"));
        assertEquals(0d, Numbers.toDouble("0D"));
        assertEquals(0d, Numbers.toDouble(""));
    }

    @Test
    public void test_floating_point_zero() {
        {
            assertEquals(Float.valueOf(0), NumberUtils.createNumber("0F"));
            assertEquals(Double.valueOf(0), NumberUtils.createNumber("0D"));

            assertEquals(Float.valueOf(0), NumberUtils.createNumber("0.F"));
            assertEquals(Double.valueOf(0), NumberUtils.createNumber("0.D"));
            assertEquals(Float.valueOf(0), NumberUtils.createNumber("0e0F"));
            assertEquals(Double.valueOf(0), NumberUtils.createNumber("0e0D"));

            assertEquals(0, Double.compare(0.0d, Double.parseDouble("0.0e-2000")));
            assertTrue(NumberUtils.isCreatable("0.0e-2000"));

            assertEquals(Float.valueOf(0.0f), NumberUtils.createNumber("0.0e-2000"));
            assertEquals(Float.valueOf(0.0f), NumberUtils.createNumber("0.0e-2000F"));
            assertEquals(Double.valueOf(0.0), NumberUtils.createNumber("0.0e-2000D"));
        }

        {
            assertEquals(Float.valueOf(0), Numbers.createNumber("0F"));
            assertEquals(Double.valueOf(0), Numbers.createNumber("0D"));

            assertEquals(Float.valueOf(0), Numbers.createNumber("0.F"));
            assertEquals(Double.valueOf(0), Numbers.createNumber("0.D"));
            assertEquals(Float.valueOf(0), Numbers.createNumber("0e0F"));
            assertEquals(Double.valueOf(0), Numbers.createNumber("0e0D"));

            assertEquals(0, Double.compare(0.0d, Double.parseDouble("0.0e-2000")));
            assertTrue(Numbers.isConvertibleToNumber("0.0e-2000"));

            assertEquals(Float.valueOf(0.0f), Numbers.createNumber("0.0e-2000F"));
            assertEquals(Double.valueOf(0.0), Numbers.createNumber("0.0e-2000D"));
        }
    }

    @Test
    public void testCreatePositiveHexInteger_NumberUtils() {
        assertTrue(NumberUtils.isCreatable("+0xF"));
        assertTrue(NumberUtils.isCreatable("+0xFFFFFFFF"));
        assertTrue(NumberUtils.isCreatable("+0xFFFFFFFFFFFFFFFFF"));

        assertEquals(Integer.decode("+0xF"), NumberUtils.createInteger("+0xF"));
        assertEquals(Long.decode("+0xFFFFFFFF"), NumberUtils.createLong("+0xFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), NumberUtils.createBigInteger("0xFFFFFFFFFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), NumberUtils.createBigInteger("+0xFFFFFFFFFFFFFFFF"));

        assertEquals(Integer.decode("+0xF"), NumberUtils.createNumber("0xF"));

        assertEquals(Integer.decode("+0xF"), NumberUtils.createNumber("+0xF"));

        assertEquals(Long.decode("+0xFFFFFFFF"), NumberUtils.createNumber("0xFFFFFFFF"));

        assertEquals(Long.decode("+0xFFFFFFFF"), NumberUtils.createNumber("+0xFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), NumberUtils.createNumber("0xFFFFFFFFFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), NumberUtils.createNumber("+0xFFFFFFFFFFFFFFFF"));

    }

    @Test
    public void testCreatePositiveHexInteger() {
        assertTrue(Numbers.isConvertibleToNumber("+0xF"));
        assertTrue(Numbers.isConvertibleToNumber("+0xFFFFFFFF"));
        assertTrue(Numbers.isConvertibleToNumber("+0xFFFFFFFFFFFFFFFFF"));

        assertEquals(Integer.decode("+0xF"), Numbers.createInteger("+0xF"));
        assertEquals(Long.decode("+0xFFFFFFFF"), Numbers.createLong("+0xFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), Numbers.createBigInteger("0xFFFFFFFFFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), Numbers.createBigInteger("+0xFFFFFFFFFFFFFFFF"));

        assertEquals(Integer.decode("+0xF"), Numbers.createNumber("0xF"));

        assertEquals(Integer.decode("+0xF"), Numbers.createNumber("+0xF"));

        assertEquals(Long.decode("+0xFFFFFFFF"), Numbers.createNumber("0xFFFFFFFF"));

        assertEquals(Long.decode("+0xFFFFFFFF"), Numbers.createNumber("+0xFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), Numbers.createNumber("0xFFFFFFFFFFFFFFFF"));

        assertEquals(new BigInteger("+FFFFFFFFFFFFFFFF", 16), Numbers.createNumber("+0xFFFFFFFFFFFFFFFF"));

    }

    @Test
    public void test_format() {
        {
            N.println(Strings.repeat("=", 80));
            final double val = 12.1050f;
            assertEquals("12", Numbers.format(val, "0"));
            assertEquals("12", Numbers.format(val, "#"));
            assertEquals("1210%", Numbers.format(val, "0%"));
            assertEquals("1210%", Numbers.format(val, "#%"));
            assertEquals("12.1", Numbers.format(val, "0.0"));
            assertEquals("12.1", Numbers.format(val, "#.#"));
            assertEquals("1210.5%", Numbers.format(val, "0.0%"));
            assertEquals("1210.5%", Numbers.format(val, "#.#%"));
            assertEquals("12.10", Numbers.format(val, "0.00"));
            assertEquals("12.1", Numbers.format(val, "#.##"));
            assertEquals("1210.50%", Numbers.format(val, "0.00%"));
            assertEquals("1210.5%", Numbers.format(val, "#.##%"));
            assertEquals("1210.500%", Numbers.format(val, "0.000%"));
            assertEquals("12.10500", Numbers.format(val, "0.00000"));
            assertEquals("12.105", Numbers.format(val, "#.#####"));
            assertEquals("1210.49995%", Numbers.format(val, "0.00000%"));
            assertEquals("1210.49995%", Numbers.format(val, "#.#####%"));
            assertEquals("12.10%", Numbers.format(0.121, "0.00%"));
            assertEquals("12.1%", Numbers.format(0.121, "0.##%"));
            assertEquals("12.16%", Numbers.format(0.12156, "0.00%"));
            assertEquals("12.16%", Numbers.format(0.12156, "#.##%"));
        }
    }

    @Test
    public void test_round() {
        {
            N.println(Strings.repeat("=", 80));
            final float val = 12.10560233f;
            N.println(Numbers.round(val, 2));
            N.println(Numbers.round(val, 3));
            N.println(Numbers.round(val, 4));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.HALF_EVEN));
            N.println(Numbers.round(val, 3, RoundingMode.HALF_EVEN));
            N.println(Numbers.round(val, 4, RoundingMode.HALF_EVEN));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.HALF_UP));
            N.println(Numbers.round(val, 3, RoundingMode.HALF_UP));
            N.println(Numbers.round(val, 4, RoundingMode.HALF_UP));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.HALF_DOWN));
            N.println(Numbers.round(val, 3, RoundingMode.HALF_DOWN));
            N.println(Numbers.round(val, 4, RoundingMode.HALF_DOWN));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.DOWN));
            N.println(Numbers.round(val, 3, RoundingMode.DOWN));
            N.println(Numbers.round(val, 4, RoundingMode.DOWN));
        }
        {
            N.println(Strings.repeat("=", 80));
            final double val = 12.156233d;
            N.println(Numbers.round(val, 2));
            N.println(Numbers.round(val, 3));
            N.println(Numbers.round(val, 4));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.HALF_EVEN));
            N.println(Numbers.round(val, 3, RoundingMode.HALF_EVEN));
            N.println(Numbers.round(val, 4, RoundingMode.HALF_EVEN));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.HALF_UP));
            N.println(Numbers.round(val, 3, RoundingMode.HALF_UP));
            N.println(Numbers.round(val, 4, RoundingMode.HALF_UP));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.HALF_DOWN));
            N.println(Numbers.round(val, 3, RoundingMode.HALF_DOWN));
            N.println(Numbers.round(val, 4, RoundingMode.HALF_DOWN));
            N.println(Strings.repeat("-", 20));
            N.println(Numbers.round(val, 2, RoundingMode.DOWN));
            N.println(Numbers.round(val, 3, RoundingMode.DOWN));
            N.println(Numbers.round(val, 4, RoundingMode.DOWN));
        }
    }

    @Test
    public void test_01() {
        assertTrue(Numbers.createNumber("4.9e-324D") instanceof Double);
        assertTrue(Numbers.createNumber("4.9e-324F") instanceof Double);

        assertTrue(NumberUtils.createNumber("0001.797693134862315759e+308") instanceof BigDecimal);
        assertTrue(Numbers.createNumber("0001.797693134862315759e+308") instanceof Double);
        assertTrue(Numbers.createNumber("-001.797693134862315759e+308") instanceof Double);
        assertTrue(Numbers.createNumber("+001.797693134862315759e+308") instanceof Double);

        final String str = "0x100";
        N.println(Integer.decode(str));
        assertFalse(Numbers.isParsable(str));

        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17.0.1"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("170"));

        assertTrue(NumberUtils.createNumber("4.9e-324D") instanceof Double);
        assertTrue(NumberUtils.createNumber("4.9e-324F") instanceof Double);

        assertTrue(11 == Numbers.toLong("11l"));
        assertTrue(11 == Numbers.toLong("11L"));
    }

    @Test
    public void test_convert_toByte() {
        assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10, byte.class));
        assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10, Byte.class));
        assertEquals(Byte.valueOf((byte) 0), Numbers.convert(null, byte.class));
        assertNull(Numbers.convert(null, Byte.class));

        assertEquals(Byte.valueOf((byte) 127), Numbers.convert(127L, byte.class));
        assertEquals(Byte.valueOf((byte) -128), Numbers.convert(-128L, byte.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(128, byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(-129, byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(200L, Byte.class));
    }

    @Test
    public void test_convert_toShort() {
        assertEquals(Short.valueOf((short) 100), Numbers.convert(100, short.class));
        assertEquals(Short.valueOf((short) 100), Numbers.convert(100, Short.class));
        assertEquals(Short.valueOf((short) 0), Numbers.convert(null, short.class));
        assertNull(Numbers.convert(null, Short.class));

        assertEquals(Short.valueOf((short) 32767), Numbers.convert(32767L, short.class));
        assertEquals(Short.valueOf((short) -32768), Numbers.convert(-32768L, short.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(32768, short.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(-32769, short.class));
    }

    @Test
    public void test_convert_toInt() {
        assertEquals(Integer.valueOf(1000), Numbers.convert(1000L, int.class));
        assertEquals(Integer.valueOf(1000), Numbers.convert(1000L, Integer.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, int.class));
        assertNull(Numbers.convert(null, Integer.class));

        assertEquals(Integer.valueOf(Integer.MAX_VALUE), Numbers.convert((long) Integer.MAX_VALUE, int.class));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.convert((long) Integer.MIN_VALUE, int.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert((long) Integer.MAX_VALUE + 1, int.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((long) Integer.MIN_VALUE - 1, int.class));
    }

    @Test
    public void test_convert_toLong() {
        assertEquals(Long.valueOf(10000L), Numbers.convert(10000, long.class));
        assertEquals(Long.valueOf(10000L), Numbers.convert(10000, Long.class));
        assertEquals(Long.valueOf(0L), Numbers.convert(null, long.class));
        assertNull(Numbers.convert(null, Long.class));

        assertEquals(Long.valueOf(Long.MAX_VALUE), Numbers.convert(new BigInteger(String.valueOf(Long.MAX_VALUE)), long.class));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.convert(new BigInteger(String.valueOf(Long.MIN_VALUE)), long.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigInteger(String.valueOf(Long.MAX_VALUE)).add(BigInteger.ONE), long.class));
    }

    @Test
    public void test_convert_toFloat() {
        assertEquals(3.14f, Numbers.convert(3.14, float.class), 0.001f);
        assertEquals(3.14f, Numbers.convert(3.14, Float.class), 0.001f);
        assertEquals(0.0f, Numbers.convert(null, float.class));
        assertNull(Numbers.convert(null, Float.class));

        assertEquals(Float.NaN, Numbers.convert(Double.NaN, float.class));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.convert(Double.POSITIVE_INFINITY, float.class));
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(Double.NEGATIVE_INFINITY, float.class));
    }

    @Test
    public void test_convert_toDouble() {
        assertEquals(3.14159, Numbers.convert(3.14159f, double.class), 0.00001);
        assertEquals(3.14159, Numbers.convert(3.14159f, Double.class), 0.00001);
        assertEquals(0.0, Numbers.convert(null, double.class));
        assertNull(Numbers.convert(null, Double.class));

        assertEquals(Double.NaN, Numbers.convert(Float.NaN, double.class));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.convert(Float.POSITIVE_INFINITY, double.class));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.convert(Float.NEGATIVE_INFINITY, double.class));
    }

    @Test
    public void test_convert_toBigInteger() {
        assertEquals(BigInteger.valueOf(100), Numbers.convert(100, BigInteger.class));
        assertEquals(BigInteger.valueOf(100), Numbers.convert(100L, BigInteger.class));
        assertEquals(BigInteger.valueOf(100), Numbers.convert(100.5, BigInteger.class));
        assertNull(Numbers.convert(null, BigInteger.class));
    }

    @Test
    public void test_convert_toBigDecimal() {
        assertEquals(new BigDecimal("100"), Numbers.convert(100, BigDecimal.class));
        assertEquals(new BigDecimal("100"), Numbers.convert(100L, BigDecimal.class));
        assertNull(Numbers.convert(null, BigDecimal.class));
    }

    @Test
    public void test_convert_withDefaultValue() {
        assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10, byte.class, (byte) 5));
        assertEquals(Byte.valueOf((byte) 5), Numbers.convert(null, byte.class, (byte) 5));
        assertEquals(Short.valueOf((short) 20), Numbers.convert(null, Short.class, (short) 20));
        assertEquals(Integer.valueOf(100), Numbers.convert(null, Integer.class, 100));
        assertEquals(Long.valueOf(200L), Numbers.convert(null, Long.class, 200L));
    }

    @Test
    public void test_convert_withType() {
        Type<Integer> intType = Type.of(Integer.class);
        assertEquals(Integer.valueOf(100), Numbers.convert(100L, intType));
        assertNull(Numbers.convert(null, intType));

        Type<Long> longType = Type.of(Long.class);
        assertEquals(Long.valueOf(200L), Numbers.convert(200, longType));

        assertEquals(Long.valueOf(50L), Numbers.convert(null, longType, 50L));
    }

    @Test
    public void test_format_int() {
        assertEquals("123", Numbers.format(123, "0"));
        assertEquals("123.00", Numbers.format(123, "0.00"));
        assertEquals("1,234", Numbers.format(1234, "#,###"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format(123, null));
    }

    @Test
    public void test_format_Integer() {
        assertEquals("123", Numbers.format(Integer.valueOf(123), "0"));
        assertEquals("0", Numbers.format((Integer) null, "0"));
        assertEquals("456.00", Numbers.format(Integer.valueOf(456), "0.00"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format((Integer) 123, null));
    }

    @Test
    public void test_format_long() {
        assertEquals("1234567890", Numbers.format(1234567890L, "0"));
        assertEquals("1,234,567,890", Numbers.format(1234567890L, "#,###"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format(123L, null));
    }

    @Test
    public void test_format_Long() {
        assertEquals("123", Numbers.format(Long.valueOf(123), "0"));
        assertEquals("0", Numbers.format((Long) null, "0"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format(Long.valueOf(123), null));
    }

    @Test
    public void test_format_float() {
        assertEquals("12.10", Numbers.format(12.105f, "0.00"));
        assertEquals("12.1", Numbers.format(12.105f, "#.##"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.1f, null));
    }

    @Test
    public void test_format_Float() {
        assertEquals("12.10", Numbers.format(Float.valueOf(12.105f), "0.00"));
        assertEquals("0.00", Numbers.format((Float) null, "0.00"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format((Float) 12.1f, null));
    }

    @Test
    public void test_format_double() {
        assertEquals("12.11", Numbers.format(12.105, "0.00"));
        assertEquals("12.11", Numbers.format(12.105, "#.##"));
        assertEquals("12.1%", Numbers.format(0.121, "#.##%"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.1, null));
    }

    @Test
    public void test_format_Double() {
        assertEquals("12.11", Numbers.format(Double.valueOf(12.105), "0.00"));
        assertEquals("0.00", Numbers.format((Double) null, "0.00"));
        assertEquals("12.16%", Numbers.format(Double.valueOf(0.12156), "#.##%"));

        assertThrows(IllegalArgumentException.class, () -> Numbers.format((Double) 12.1, null));
    }

    @Test
    public void test_extractFirstInt() {
        assertEquals(123, Numbers.extractFirstInt("abc123def").get());
        assertEquals(-456, Numbers.extractFirstInt("test-456xyz").get());
        assertTrue(Numbers.extractFirstInt("noNumber").isEmpty());
        assertTrue(Numbers.extractFirstInt("").isEmpty());
        assertTrue(Numbers.extractFirstInt(null).isEmpty());
    }

    @Test
    public void test_extractFirstInt_withDefault() {
        assertEquals(123, Numbers.extractFirstInt("abc123def", 99));
        assertEquals(99, Numbers.extractFirstInt("abcdef", 99));
        assertEquals(99, Numbers.extractFirstInt("", 99));
        assertEquals(99, Numbers.extractFirstInt(null, 99));
    }

    @Test
    public void test_extractFirstLong() {
        assertEquals(9223372036854775807L, Numbers.extractFirstLong("value9223372036854775807end").get());
        assertTrue(Numbers.extractFirstLong("noNumber").isEmpty());
        assertTrue(Numbers.extractFirstLong("").isEmpty());
        assertTrue(Numbers.extractFirstLong(null).isEmpty());
    }

    @Test
    public void test_extractFirstLong_withDefault() {
        assertEquals(123L, Numbers.extractFirstLong("abc123def", 99L));
        assertEquals(99L, Numbers.extractFirstLong("abcdef", 99L));
        assertEquals(99L, Numbers.extractFirstLong("", 99L));
        assertEquals(99L, Numbers.extractFirstLong(null, 99L));
    }

    @Test
    public void test_extractFirstDouble() {
        assertEquals(3.14, Numbers.extractFirstDouble("pi is 3.14").get(), 0.001);
        assertTrue(Numbers.extractFirstDouble("noNumber").isEmpty());
        assertTrue(Numbers.extractFirstDouble("").isEmpty());
        assertTrue(Numbers.extractFirstDouble("null").isEmpty());
        assertTrue(Numbers.extractFirstDouble("noNumber").isEmpty());
        assertEquals(-2.5, Numbers.extractFirstDouble("temp is -2.5 degrees").get(), 0.001);
    }

    @Test
    public void test_extractFirstDouble_withDefault() {
        assertEquals(3.14, Numbers.extractFirstDouble("pi is 3.14", 1.0), 0.001);
        assertEquals(1.0, Numbers.extractFirstDouble("noNumber", 1.0), 0.001);
        assertEquals(1.0, Numbers.extractFirstDouble("", 1.0), 0.001);
        assertEquals(1.0, Numbers.extractFirstDouble(null, 1.0), 0.001);
    }

    @Test
    public void test_extractFirstDouble_scientific() {
        assertEquals(1.23e-4, Numbers.extractFirstDouble("value is 1.23e-4", true).get(), 0.00001);
        assertEquals(1.23, Numbers.extractFirstDouble("value is 1.23e-4", false).get(), 0.001);
        assertEquals(1.5e10, Numbers.extractFirstDouble("number 1.5e10 here", 0.0, true), 0.001);
    }

    @Test
    public void test_toByte_String() {
        assertEquals((byte) 10, Numbers.toByte("10"));
        assertEquals((byte) 0, Numbers.toByte(""));
        assertEquals((byte) 0, Numbers.toByte(null));
        assertEquals((byte) -127, Numbers.toByte("-127"));

        assertThrows(NumberFormatException.class, () -> Numbers.toByte("128"));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("abc"));
    }

    @Test
    public void test_toByte_String_withDefault() {
        assertEquals((byte) 10, Numbers.toByte("10", (byte) 5));
        assertEquals((byte) 5, Numbers.toByte("", (byte) 5));
        assertEquals((byte) 5, Numbers.toByte(null, (byte) 5));

        assertThrows(NumberFormatException.class, () -> Numbers.toByte("200", (byte) 5));
    }

    @Test
    public void test_toByte_Object() {
        assertEquals((byte) 10, Numbers.toByte(10));
        assertEquals((byte) 10, Numbers.toByte((byte) 10));
        assertEquals((byte) 10, Numbers.toByte((Object) "10"));
        assertEquals((byte) 0, Numbers.toByte((Object) null));

        assertThrows(NumberFormatException.class, () -> Numbers.toByte(200));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte(200L));
    }

    @Test
    public void test_toByte_Object_withDefault() {
        assertEquals((byte) 10, Numbers.toByte(10, (byte) 5));
        assertEquals((byte) 5, Numbers.toByte((Object) null, (byte) 5));
        assertEquals((byte) 10, Numbers.toByte((Object) "10", (byte) 5));

        assertThrows(NumberFormatException.class, () -> Numbers.toByte(300L, (byte) 5));
    }

    @Test
    public void test_toShort_String() {
        assertEquals((short) 100, Numbers.toShort("100"));
        assertEquals((short) 0, Numbers.toShort(""));
        assertEquals((short) 0, Numbers.toShort(null));
        assertEquals((short) -1000, Numbers.toShort("-1000"));

        assertThrows(NumberFormatException.class, () -> Numbers.toShort("40000"));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("abc"));
    }

    @Test
    public void test_toShort_String_withDefault() {
        assertEquals((short) 100, Numbers.toShort("100", (short) 50));
        assertEquals((short) 50, Numbers.toShort("", (short) 50));
        assertEquals((short) 50, Numbers.toShort(null, (short) 50));

        assertThrows(NumberFormatException.class, () -> Numbers.toShort("50000", (short) 50));
    }

    @Test
    public void test_toShort_Object() {
        assertEquals((short) 100, Numbers.toShort(100));
        assertEquals((short) 100, Numbers.toShort((short) 100));
        assertEquals((short) 100, Numbers.toShort((Object) "100"));
        assertEquals((short) 0, Numbers.toShort((Object) null));

        assertThrows(NumberFormatException.class, () -> Numbers.toShort(40000));
    }

    @Test
    public void test_toShort_Object_withDefault() {
        assertEquals((short) 100, Numbers.toShort(100, (short) 50));
        assertEquals((short) 50, Numbers.toShort((Object) null, (short) 50));
        assertEquals((short) 100, Numbers.toShort((Object) "100", (short) 50));

        assertThrows(NumberFormatException.class, () -> Numbers.toShort(50000L, (short) 50));
    }

    @Test
    public void test_toInt_String() {
        assertEquals(1000, Numbers.toInt("1000"));
        assertEquals(0, Numbers.toInt(""));
        assertEquals(0, Numbers.toInt(null));
        assertEquals(-5000, Numbers.toInt("-5000"));

        assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc"));
    }

    @Test
    public void test_toInt_String_withDefault() {
        assertEquals(1000, Numbers.toInt("1000", 500));
        assertEquals(500, Numbers.toInt("", 500));
        assertEquals(500, Numbers.toInt(null, 500));
    }

    @Test
    public void test_toInt_Object() {
        assertEquals(1000, Numbers.toInt(1000));
        assertEquals(1000, Numbers.toInt(1000L));
        assertEquals(1000, Numbers.toInt((Object) "1000"));
        assertEquals(0, Numbers.toInt((Object) null));

        assertThrows(NumberFormatException.class, () -> Numbers.toInt((long) Integer.MAX_VALUE + 1));
    }

    @Test
    public void test_toInt_Object_withDefault() {
        assertEquals(1000, Numbers.toInt(1000, 500));
        assertEquals(500, Numbers.toInt((Object) null, 500));
        assertEquals(1000, Numbers.toInt((Object) "1000", 500));
    }

    @Test
    public void test_toLong_String() {
        assertEquals(1000000L, Numbers.toLong("1000000"));
        assertEquals(0L, Numbers.toLong(""));
        assertEquals(0L, Numbers.toLong(null));
        assertEquals(-5000000L, Numbers.toLong("-5000000"));

        assertThrows(NumberFormatException.class, () -> Numbers.toLong("abc"));
    }

    @Test
    public void test_toLong_String_withDefault() {
        assertEquals(1000000L, Numbers.toLong("1000000", 500L));
        assertEquals(500L, Numbers.toLong("", 500L));
        assertEquals(500L, Numbers.toLong(null, 500L));
    }

    @Test
    public void test_toLong_Object() {
        assertEquals(1000000L, Numbers.toLong(1000000));
        assertEquals(1000000L, Numbers.toLong(1000000L));
        assertEquals(1000000L, Numbers.toLong((Object) "1000000"));
        assertEquals(0L, Numbers.toLong((Object) null));
    }

    @Test
    public void test_toLong_Object_withDefault() {
        assertEquals(1000000L, Numbers.toLong(1000000, 500L));
        assertEquals(500L, Numbers.toLong((Object) null, 500L));
        assertEquals(1000000L, Numbers.toLong((Object) "1000000", 500L));
    }

    @Test
    public void test_toFloat_String() {
        assertEquals(3.14f, Numbers.toFloat("3.14"), 0.001f);
        assertEquals(0.0f, Numbers.toFloat(""), 0.001f);
        assertEquals(0.0f, Numbers.toFloat(null), 0.001f);
        assertEquals(-2.5f, Numbers.toFloat("-2.5"), 0.001f);

        assertThrows(NumberFormatException.class, () -> Numbers.toFloat("abc"));
    }

    @Test
    public void test_toFloat_String_withDefault() {
        assertEquals(3.14f, Numbers.toFloat("3.14", 1.0f), 0.001f);
        assertEquals(1.0f, Numbers.toFloat("", 1.0f), 0.001f);
        assertEquals(1.0f, Numbers.toFloat(null, 1.0f), 0.001f);
    }

    @Test
    public void test_toFloat_Object() {
        assertEquals(3.14f, Numbers.toFloat(3.14f), 0.001f);
        assertEquals(3.14f, Numbers.toFloat(3.14), 0.001f);
        assertEquals(3.14f, Numbers.toFloat((Object) "3.14"), 0.001f);
        assertEquals(0.0f, Numbers.toFloat((Object) null), 0.001f);
    }

    @Test
    public void test_toFloat_Object_withDefault() {
        assertEquals(3.14f, Numbers.toFloat(3.14f, 1.0f), 0.001f);
        assertEquals(1.0f, Numbers.toFloat((Object) null, 1.0f), 0.001f);
        assertEquals(3.14f, Numbers.toFloat((Object) "3.14", 1.0f), 0.001f);
    }

    @Test
    public void test_toDouble_String() {
        assertEquals(3.14159, Numbers.toDouble("3.14159"), 0.00001);
        assertEquals(0.0, Numbers.toDouble(""), 0.001);
        assertEquals(0.0, Numbers.toDouble((String) null), 0.001);
        assertEquals(-2.71828, Numbers.toDouble("-2.71828"), 0.00001);

        assertThrows(NumberFormatException.class, () -> Numbers.toDouble("abc"));
    }

    @Test
    public void test_toDouble_String_withDefault() {
        assertEquals(3.14159, Numbers.toDouble("3.14159", 1.0), 0.00001);
        assertEquals(1.0, Numbers.toDouble("", 1.0), 0.001);
        assertEquals(1.0, Numbers.toDouble((String) null, 1.0), 0.001);
    }

    @Test
    public void test_toDouble_Object() {
        assertEquals(3.14159, Numbers.toDouble(3.14159), 0.00001);
        assertEquals(3.14, Numbers.toDouble(3.14f), 0.01);
        assertEquals(3.14159, Numbers.toDouble((Object) "3.14159"), 0.00001);
        assertEquals(0.0, Numbers.toDouble((Object) null), 0.001);
    }

    @Test
    public void test_toDouble_Object_withDefault() {
        assertEquals(3.14159, Numbers.toDouble(3.14159, 1.0), 0.00001);
        assertEquals(1.0, Numbers.toDouble((Object) null, 1.0), 0.001);
        assertEquals(3.14159, Numbers.toDouble((Object) "3.14159", 1.0), 0.00001);
    }

    @Test
    public void test_toDouble_BigDecimal() {
        assertEquals(123.456, Numbers.toDouble(new BigDecimal("123.456")), 0.001);
        assertEquals(0.0, Numbers.toDouble((BigDecimal) null), 0.001);
    }

    @Test
    public void test_toDouble_BigDecimal_withDefault() {
        assertEquals(123.456, Numbers.toDouble(new BigDecimal("123.456"), 1.0), 0.001);
        assertEquals(1.0, Numbers.toDouble((BigDecimal) null, 1.0), 0.001);
    }

    @Test
    public void test_toScaledBigDecimal_BigDecimal() {
        BigDecimal result = Numbers.toScaledBigDecimal(new BigDecimal("123.456"));
        assertNotNull(result);
        assertEquals(2, result.scale());
    }

    @Test
    public void test_toScaledBigDecimal_BigDecimal_withScaleAndMode() {
        BigDecimal result = Numbers.toScaledBigDecimal(new BigDecimal("123.456"), 3, RoundingMode.HALF_UP);
        assertNotNull(result);
        assertEquals(3, result.scale());
        assertEquals(new BigDecimal("123.456"), result);
    }

    @Test
    public void test_toScaledBigDecimal_Float() {
        BigDecimal result = Numbers.toScaledBigDecimal(Float.valueOf(123.45f));
        assertNotNull(result);
        assertEquals(2, result.scale());
    }

    @Test
    public void test_toScaledBigDecimal_Float_withScaleAndMode() {
        BigDecimal result = Numbers.toScaledBigDecimal(Float.valueOf(123.456f), 3, RoundingMode.HALF_UP);
        assertNotNull(result);
        assertEquals(3, result.scale());
    }

    @Test
    public void test_toScaledBigDecimal_Double() {
        BigDecimal result = Numbers.toScaledBigDecimal(Double.valueOf(123.45));
        assertNotNull(result);
        assertEquals(2, result.scale());
    }

    @Test
    public void test_toScaledBigDecimal_Double_withScaleAndMode() {
        BigDecimal result = Numbers.toScaledBigDecimal(Double.valueOf(123.456), 3, RoundingMode.HALF_UP);
        assertNotNull(result);
        assertEquals(3, result.scale());
    }

    @Test
    public void test_toScaledBigDecimal_String() {
        BigDecimal result = Numbers.toScaledBigDecimal("123.45");
        assertNotNull(result);
        assertEquals(2, result.scale());
    }

    @Test
    public void test_toScaledBigDecimal_String_withScaleAndMode() {
        BigDecimal result = Numbers.toScaledBigDecimal("123.456", 3, RoundingMode.HALF_UP);
        assertNotNull(result);
        assertEquals(3, result.scale());
    }

    @Test
    public void test_toIntExact() {
        assertEquals(100, Numbers.toIntExact(100L));
        assertEquals(Integer.MAX_VALUE, Numbers.toIntExact(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.toIntExact(Integer.MIN_VALUE));

        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MAX_VALUE + 1));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MIN_VALUE - 1));
    }

    @Test
    public void test_createInteger() {
        assertEquals(Integer.valueOf(123), Numbers.createInteger("123"));
        assertEquals(Integer.valueOf(-456), Numbers.createInteger("-456"));
        assertNull(Numbers.createInteger(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createInteger(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createInteger("abc"));
    }

    @Test
    public void test_createLong() {
        assertEquals(Long.valueOf(123456789L), Numbers.createLong("123456789"));
        assertEquals(Long.valueOf(-987654321L), Numbers.createLong("-987654321"));
        assertNull(Numbers.createLong(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createLong(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createLong("abc"));
    }

    @Test
    public void test_createFloat() {
        assertEquals(Float.valueOf(3.14f), Numbers.createFloat("3.14"));
        assertEquals(Float.valueOf(-2.5f), Numbers.createFloat("-2.5"));
        assertNull(Numbers.createFloat(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createFloat(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createFloat("abc"));
    }

    @Test
    public void test_createDouble() {
        assertEquals(Double.valueOf(3.14159), Numbers.createDouble("3.14159"));
        assertEquals(Double.valueOf(-2.71828), Numbers.createDouble("-2.71828"));
        assertNull(Numbers.createDouble(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createDouble(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createDouble("abc"));
    }

    @Test
    public void test_createBigInteger() {
        assertEquals(new BigInteger("123456789012345678901234567890"), Numbers.createBigInteger("123456789012345678901234567890"));
        assertNull(Numbers.createBigInteger(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createBigInteger(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createBigInteger("abc"));
    }

    @Test
    public void test_createBigDecimal() {
        assertEquals(new BigDecimal("123.456789"), Numbers.createBigDecimal("123.456789"));
        assertNull(Numbers.createBigDecimal(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createBigDecimal(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createBigDecimal("abc"));
    }

    @Test
    public void test_createNumber() {
        assertEquals(123, Numbers.createNumber("123"));
        assertEquals(123L, Numbers.createNumber("123L"));
        assertEquals(3.14f, Numbers.createNumber("3.14f"));
        assertEquals(3.14, Numbers.createNumber("3.14"));
        assertNull(Numbers.createNumber(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber(""));

        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("abc"));
    }

    @Test
    public void test_isDigits() {
        assertTrue(Numbers.isDigits("123"));
        assertTrue(Numbers.isDigits("0"));
        assertFalse(Numbers.isDigits("12.3"));
        assertFalse(Numbers.isDigits("-123"));
        assertFalse(Numbers.isDigits("abc"));
        assertFalse(Numbers.isDigits(""));
        assertFalse(Numbers.isDigits(null));
    }

    @Test
    public void test_isNumber() {
        assertTrue(Numbers.isNumber("123"));
        assertTrue(Numbers.isNumber("-123"));
        assertTrue(Numbers.isNumber("12.3"));
        assertTrue(Numbers.isNumber("-12.3"));
        assertFalse(Numbers.isNumber("abc"));
        assertFalse(Numbers.isNumber(""));
        assertFalse(Numbers.isNumber(null));
    }

    @Test
    public void test_isConvertibleToNumber() {
        assertTrue(Numbers.isConvertibleToNumber("123"));
        assertTrue(Numbers.isConvertibleToNumber("-123"));
        assertTrue(Numbers.isConvertibleToNumber("12.3"));
        assertTrue(Numbers.isConvertibleToNumber("1.23e10"));
        assertTrue(Numbers.isConvertibleToNumber("0x1F"));
        assertFalse(Numbers.isConvertibleToNumber("abc"));
        assertFalse(Numbers.isConvertibleToNumber(""));
        assertFalse(Numbers.isConvertibleToNumber(null));
    }

    @Test
    public void test_isPrime() {
        assertTrue(Numbers.isPrime(2));
        assertTrue(Numbers.isPrime(3));
        assertTrue(Numbers.isPrime(5));
        assertTrue(Numbers.isPrime(7));
        assertTrue(Numbers.isPrime(11));
        assertFalse(Numbers.isPrime(1));
        assertFalse(Numbers.isPrime(4));
        assertFalse(Numbers.isPrime(6));
        assertFalse(Numbers.isPrime(8));
        assertFalse(Numbers.isPrime(9));
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPrime(-1));
        assertFalse(Numbers.isPrime(0));
    }

    @Test
    public void test_isPerfectSquare_int() {
        assertTrue(Numbers.isPerfectSquare(0));
        assertTrue(Numbers.isPerfectSquare(1));
        assertTrue(Numbers.isPerfectSquare(4));
        assertTrue(Numbers.isPerfectSquare(9));
        assertTrue(Numbers.isPerfectSquare(16));
        assertTrue(Numbers.isPerfectSquare(25));
        assertFalse(Numbers.isPerfectSquare(2));
        assertFalse(Numbers.isPerfectSquare(3));
        assertFalse(Numbers.isPerfectSquare(5));
        assertFalse(Numbers.isPerfectSquare(-4));
    }

    @Test
    public void test_isPerfectSquare_long() {
        assertTrue(Numbers.isPerfectSquare(0L));
        assertTrue(Numbers.isPerfectSquare(1L));
        assertTrue(Numbers.isPerfectSquare(4L));
        assertTrue(Numbers.isPerfectSquare(100L));
        assertFalse(Numbers.isPerfectSquare(2L));
        assertFalse(Numbers.isPerfectSquare(3L));
        assertFalse(Numbers.isPerfectSquare(-4L));
    }

    @Test
    public void test_isPowerOfTwo_int() {
        assertTrue(Numbers.isPowerOfTwo(1));
        assertTrue(Numbers.isPowerOfTwo(2));
        assertTrue(Numbers.isPowerOfTwo(4));
        assertTrue(Numbers.isPowerOfTwo(8));
        assertTrue(Numbers.isPowerOfTwo(16));
        assertFalse(Numbers.isPowerOfTwo(0));
        assertFalse(Numbers.isPowerOfTwo(3));
        assertFalse(Numbers.isPowerOfTwo(5));
        assertFalse(Numbers.isPowerOfTwo(-2));
    }

    @Test
    public void test_isPowerOfTwo_long() {
        assertTrue(Numbers.isPowerOfTwo(1L));
        assertTrue(Numbers.isPowerOfTwo(2L));
        assertTrue(Numbers.isPowerOfTwo(4L));
        assertTrue(Numbers.isPowerOfTwo(1024L));
        assertFalse(Numbers.isPowerOfTwo(0L));
        assertFalse(Numbers.isPowerOfTwo(3L));
        assertFalse(Numbers.isPowerOfTwo(-2L));
    }

    @Test
    public void test_isPowerOfTwo_double() {
        assertTrue(Numbers.isPowerOfTwo(1.0));
        assertTrue(Numbers.isPowerOfTwo(2.0));
        assertTrue(Numbers.isPowerOfTwo(4.0));
        assertTrue(Numbers.isPowerOfTwo(0.5));
        assertTrue(Numbers.isPowerOfTwo(0.25));
        assertFalse(Numbers.isPowerOfTwo(0.0));
        assertFalse(Numbers.isPowerOfTwo(3.0));
        assertFalse(Numbers.isPowerOfTwo(-2.0));
    }

    @Test
    public void test_isPowerOfTwo_BigInteger() {
        assertTrue(Numbers.isPowerOfTwo(BigInteger.ONE));
        assertTrue(Numbers.isPowerOfTwo(BigInteger.TWO));
        assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(4)));
        assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(1024)));
        assertFalse(Numbers.isPowerOfTwo(BigInteger.ZERO));
        assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(3)));

        assertThrows(IllegalArgumentException.class, () -> Numbers.isPowerOfTwo((BigInteger) null));
    }

    @Test
    public void test_log() {
        assertEquals(Math.log(10), Numbers.log(10), 0.0001);
        assertEquals(Math.log(100), Numbers.log(100), 0.0001);
    }

    @Test
    public void test_log2_int() {
        assertEquals(0, Numbers.log2(1, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(2, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log2(4, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10, RoundingMode.CEILING));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1, RoundingMode.FLOOR));
    }

    @Test
    public void test_log2_long() {
        assertEquals(0, Numbers.log2(1L, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(2L, RoundingMode.UNNECESSARY));
        assertEquals(10, Numbers.log2(1024L, RoundingMode.UNNECESSARY));
        assertEquals(9, Numbers.log2(1000L, RoundingMode.FLOOR));
        assertEquals(10, Numbers.log2(2000L, RoundingMode.FLOOR));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1L, RoundingMode.FLOOR));
    }

    @Test
    public void test_log2_double() {
        assertEquals(0.0, Numbers.log2(1.0), 0.0001);
        assertEquals(1.0, Numbers.log2(2.0), 0.0001);
        assertEquals(2.0, Numbers.log2(4.0), 0.0001);
        assertEquals(3.0, Numbers.log2(8.0), 0.0001);
    }

    @Test
    public void test_log2_double_withRoundingMode() {
        assertEquals(3, Numbers.log2(10.0, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10.0, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(8.0, RoundingMode.UNNECESSARY));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1.0, RoundingMode.FLOOR));
    }

    @Test
    public void test_log2_BigInteger() {
        assertEquals(0, Numbers.log2(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(BigInteger.TWO, RoundingMode.UNNECESSARY));
        assertEquals(10, Numbers.log2(BigInteger.valueOf(1024), RoundingMode.UNNECESSARY));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.ZERO, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.valueOf(-1), RoundingMode.FLOOR));
    }

    @Test
    public void test_log10_int() {
        assertEquals(0, Numbers.log10(1, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(10, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(123, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(123, RoundingMode.CEILING));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-1, RoundingMode.FLOOR));
    }

    @Test
    public void test_log10_long() {
        assertEquals(0, Numbers.log10(1L, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(10L, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(1000L, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(1234L, RoundingMode.FLOOR));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-1L, RoundingMode.FLOOR));
    }

    @Test
    public void test_log10_double() {
        assertEquals(0.0, Numbers.log10(1.0), 0.0001);
        assertEquals(1.0, Numbers.log10(10.0), 0.0001);
        assertEquals(2.0, Numbers.log10(100.0), 0.0001);
    }

    @Test
    public void test_log10_BigInteger() {
        assertEquals(0, Numbers.log10(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(BigInteger.TEN, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(BigInteger.ZERO, RoundingMode.FLOOR));
    }

    @Test
    public void test_pow_int() {
        assertEquals(1, Numbers.pow(2, 0));
        assertEquals(2, Numbers.pow(2, 1));
        assertEquals(4, Numbers.pow(2, 2));
        assertEquals(8, Numbers.pow(2, 3));
        assertEquals(16, Numbers.pow(2, 4));
        assertEquals(1000, Numbers.pow(10, 3));

        assertThrows(IllegalArgumentException.class, () -> Numbers.pow(2, -1));
    }

    @Test
    public void test_pow_long() {
        assertEquals(1L, Numbers.pow(2L, 0));
        assertEquals(2L, Numbers.pow(2L, 1));
        assertEquals(1024L, Numbers.pow(2L, 10));
        assertEquals(1000000L, Numbers.pow(10L, 6));

        assertThrows(IllegalArgumentException.class, () -> Numbers.pow(2L, -1));
    }

    @Test
    public void test_ceilingPowerOfTwo_long() {
        assertEquals(1L, Numbers.ceilingPowerOfTwo(1L));
        assertEquals(2L, Numbers.ceilingPowerOfTwo(2L));
        assertEquals(4L, Numbers.ceilingPowerOfTwo(3L));
        assertEquals(8L, Numbers.ceilingPowerOfTwo(5L));
        assertEquals(16L, Numbers.ceilingPowerOfTwo(9L));

        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(0L));
        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(-1L));
    }

    @Test
    public void test_ceilingPowerOfTwo_BigInteger() {
        assertEquals(BigInteger.ONE, Numbers.ceilingPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.TWO, Numbers.ceilingPowerOfTwo(BigInteger.TWO));
        assertEquals(BigInteger.valueOf(4), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(8), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(5)));

        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(BigInteger.ZERO));
    }

    @Test
    public void test_floorPowerOfTwo_long() {
        assertEquals(1L, Numbers.floorPowerOfTwo(1L));
        assertEquals(2L, Numbers.floorPowerOfTwo(2L));
        assertEquals(2L, Numbers.floorPowerOfTwo(3L));
        assertEquals(4L, Numbers.floorPowerOfTwo(5L));
        assertEquals(8L, Numbers.floorPowerOfTwo(9L));

        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(0L));
        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(-1L));
    }

    @Test
    public void test_floorPowerOfTwo_BigInteger() {
        assertEquals(BigInteger.ONE, Numbers.floorPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.TWO, Numbers.floorPowerOfTwo(BigInteger.TWO));
        assertEquals(BigInteger.TWO, Numbers.floorPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(4), Numbers.floorPowerOfTwo(BigInteger.valueOf(5)));

        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(BigInteger.ZERO));
    }

    @Test
    public void test_sqrt_int() {
        assertEquals(0, Numbers.sqrt(0, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.sqrt(1, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.sqrt(4, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.CEILING));

        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1, RoundingMode.FLOOR));
    }

    @Test
    public void test_sqrt_long() {
        assertEquals(0L, Numbers.sqrt(0L, RoundingMode.UNNECESSARY));
        assertEquals(1L, Numbers.sqrt(1L, RoundingMode.UNNECESSARY));
        assertEquals(10L, Numbers.sqrt(100L, RoundingMode.UNNECESSARY));
        assertEquals(31L, Numbers.sqrt(1000L, RoundingMode.FLOOR));
        assertEquals(32L, Numbers.sqrt(1000L, RoundingMode.CEILING));

        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1L, RoundingMode.FLOOR));
    }

    @Test
    public void test_sqrt_BigInteger() {
        assertEquals(BigInteger.ZERO, Numbers.sqrt(BigInteger.ZERO, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.ONE, Numbers.sqrt(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.TEN, Numbers.sqrt(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));

        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.FLOOR));
    }

    @Test
    public void test_divide_int() {
        assertEquals(5, Numbers.divide(10, 2, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.divide(10, 3, RoundingMode.FLOOR));
        assertEquals(4, Numbers.divide(10, 3, RoundingMode.CEILING));
        assertEquals(3, Numbers.divide(10, 3, RoundingMode.HALF_UP));

        assertThrows(ArithmeticException.class, () -> Numbers.divide(10, 0, RoundingMode.FLOOR));
    }

    @Test
    public void test_divide_long() {
        assertEquals(5L, Numbers.divide(10L, 2L, RoundingMode.UNNECESSARY));
        assertEquals(3L, Numbers.divide(10L, 3L, RoundingMode.FLOOR));
        assertEquals(4L, Numbers.divide(10L, 3L, RoundingMode.CEILING));

        assertThrows(ArithmeticException.class, () -> Numbers.divide(10L, 0L, RoundingMode.FLOOR));
    }

    @Test
    public void test_divide_BigInteger() {
        assertEquals(BigInteger.valueOf(5), Numbers.divide(BigInteger.TEN, BigInteger.TWO, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.TEN, BigInteger.valueOf(3), RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(BigInteger.TEN, BigInteger.valueOf(3), RoundingMode.CEILING));

        assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.TEN, BigInteger.ZERO, RoundingMode.FLOOR));
    }

    @Test
    public void test_mod_int_int() {
        assertEquals(1, Numbers.mod(10, 3));
        assertEquals(0, Numbers.mod(10, 5));
        assertEquals(2, Numbers.mod(-8, 5));

        assertThrows(ArithmeticException.class, () -> Numbers.mod(10, 0));
    }

    @Test
    public void test_mod_long_int() {
        assertEquals(1, Numbers.mod(10L, 3));
        assertEquals(0, Numbers.mod(10L, 5));
        assertEquals(2, Numbers.mod(-8L, 5));

        assertThrows(ArithmeticException.class, () -> Numbers.mod(10L, 0));
    }

    @Test
    public void test_mod_long_long() {
        assertEquals(1L, Numbers.mod(10L, 3L));
        assertEquals(0L, Numbers.mod(10L, 5L));
        assertEquals(2L, Numbers.mod(-8L, 5L));

        assertThrows(ArithmeticException.class, () -> Numbers.mod(10L, 0L));
    }

    @Test
    public void test_gcd_int() {
        assertEquals(1, Numbers.gcd(10, 3));
        assertEquals(5, Numbers.gcd(10, 5));
        assertEquals(6, Numbers.gcd(12, 18));
        assertEquals(1, Numbers.gcd(17, 19));
        assertEquals(12, Numbers.gcd(12, 0));
        assertEquals(12, Numbers.gcd(0, 12));

        assertEquals(1, Numbers.gcd(Integer.MIN_VALUE, 1));
    }

    @Test
    public void test_gcd_long() {
        assertEquals(1L, Numbers.gcd(10L, 3L));
        assertEquals(5L, Numbers.gcd(10L, 5L));
        assertEquals(6L, Numbers.gcd(12L, 18L));
        assertEquals(12L, Numbers.gcd(12L, 0L));
        assertEquals(12L, Numbers.gcd(0L, 12L));

        assertEquals(1L, Numbers.gcd(Long.MIN_VALUE, 1L));
    }

    @Test
    public void test_lcm_int() {
        assertEquals(30, Numbers.lcm(10, 15));
        assertEquals(10, Numbers.lcm(10, 5));
        assertEquals(36, Numbers.lcm(12, 18));
        assertEquals(0, Numbers.lcm(0, 10));

        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Integer.MAX_VALUE, 2));
    }

    @Test
    public void test_lcm_long() {
        assertEquals(30L, Numbers.lcm(10L, 15L));
        assertEquals(10L, Numbers.lcm(10L, 5L));
        assertEquals(36L, Numbers.lcm(12L, 18L));
        assertEquals(0L, Numbers.lcm(0L, 10L));

        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Long.MAX_VALUE, 2L));
    }

    @Test
    public void test_addExact_int() {
        assertEquals(5, Numbers.addExact(2, 3));
        assertEquals(-1, Numbers.addExact(2, -3));
        assertEquals(Integer.MAX_VALUE, Numbers.addExact(Integer.MAX_VALUE - 1, 1));

        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MIN_VALUE, -1));
    }

    @Test
    public void test_addExact_long() {
        assertEquals(5L, Numbers.addExact(2L, 3L));
        assertEquals(-1L, Numbers.addExact(2L, -3L));
        assertEquals(Long.MAX_VALUE, Numbers.addExact(Long.MAX_VALUE - 1, 1L));

        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MAX_VALUE, 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MIN_VALUE, -1L));
    }

    @Test
    public void test_subtractExact_int() {
        assertEquals(-1, Numbers.subtractExact(2, 3));
        assertEquals(5, Numbers.subtractExact(2, -3));
        assertEquals(Integer.MIN_VALUE, Numbers.subtractExact(Integer.MIN_VALUE + 1, 1));

        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MAX_VALUE, -1));
    }

    @Test
    public void test_subtractExact_long() {
        assertEquals(-1L, Numbers.subtractExact(2L, 3L));
        assertEquals(5L, Numbers.subtractExact(2L, -3L));
        assertEquals(Long.MIN_VALUE, Numbers.subtractExact(Long.MIN_VALUE + 1, 1L));

        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MIN_VALUE, 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MAX_VALUE, -1L));
    }

    @Test
    public void test_multiplyExact_int() {
        assertEquals(6, Numbers.multiplyExact(2, 3));
        assertEquals(-6, Numbers.multiplyExact(2, -3));
        assertEquals(0, Numbers.multiplyExact(0, Integer.MAX_VALUE));

        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MAX_VALUE, 2));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MIN_VALUE, 2));
    }

    @Test
    public void test_multiplyExact_long() {
        assertEquals(6L, Numbers.multiplyExact(2L, 3L));
        assertEquals(-6L, Numbers.multiplyExact(2L, -3L));
        assertEquals(0L, Numbers.multiplyExact(0L, Long.MAX_VALUE));

        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Long.MAX_VALUE, 2L));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Long.MIN_VALUE, 2L));
    }

    @Test
    public void test_powExact_int() {
        assertEquals(1, Numbers.powExact(2, 0));
        assertEquals(8, Numbers.powExact(2, 3));
        assertEquals(1000, Numbers.powExact(10, 3));

        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> Numbers.powExact(2, -1));
    }

    @Test
    public void test_powExact_long() {
        assertEquals(1L, Numbers.powExact(2L, 0));
        assertEquals(1024L, Numbers.powExact(2L, 10));
        assertEquals(1000000L, Numbers.powExact(10L, 6));

        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Long.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> Numbers.powExact(2L, -1));
    }

    @Test
    public void test_saturatedAdd_int() {
        assertEquals(5, Numbers.saturatedAdd(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, -1));
    }

    @Test
    public void test_saturatedAdd_long() {
        assertEquals(5L, Numbers.saturatedAdd(2L, 3L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedAdd(Long.MIN_VALUE, -1L));
    }

    @Test
    public void test_saturatedSubtract_int() {
        assertEquals(-1, Numbers.saturatedSubtract(2, 3));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedSubtract(Integer.MIN_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedSubtract(Integer.MAX_VALUE, -1));
    }

    @Test
    public void test_saturatedSubtract_long() {
        assertEquals(-1L, Numbers.saturatedSubtract(2L, 3L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedSubtract(Long.MIN_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedSubtract(Long.MAX_VALUE, -1L));
    }

    @Test
    public void test_saturatedMultiply_int() {
        assertEquals(6, Numbers.saturatedMultiply(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, -2));
    }

    @Test
    public void test_saturatedMultiply_long() {
        assertEquals(6L, Numbers.saturatedMultiply(2L, 3L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, 2L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, -2L));
    }

    @Test
    public void test_saturatedPow_int() {
        assertEquals(1, Numbers.saturatedPow(2, 0));
        assertEquals(8, Numbers.saturatedPow(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(Integer.MAX_VALUE, 2));

        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedPow(2, -1));
    }

    @Test
    public void test_saturatedPow_long() {
        assertEquals(1L, Numbers.saturatedPow(2L, 0));
        assertEquals(1024L, Numbers.saturatedPow(2L, 10));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(Long.MAX_VALUE, 2));

        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedPow(2L, -1));
    }

    @Test
    public void test_saturatedCast() {
        assertEquals(100, Numbers.saturatedCast(100L));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCast((long) Integer.MAX_VALUE + 1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCast((long) Integer.MIN_VALUE - 1));
    }

    @Test
    public void test_factorial() {
        assertEquals(1, Numbers.factorial(0));
        assertEquals(1, Numbers.factorial(1));
        assertEquals(2, Numbers.factorial(2));
        assertEquals(6, Numbers.factorial(3));
        assertEquals(24, Numbers.factorial(4));
        assertEquals(120, Numbers.factorial(5));

        assertThrows(IllegalArgumentException.class, () -> Numbers.factorial(-1));
    }

    @Test
    public void test_factorialToLong() {
        assertEquals(1L, Numbers.factorialToLong(0));
        assertEquals(1L, Numbers.factorialToLong(1));
        assertEquals(2L, Numbers.factorialToLong(2));
        assertEquals(6L, Numbers.factorialToLong(3));
        assertEquals(3628800L, Numbers.factorialToLong(10));

        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToLong(-1));
    }

    @Test
    public void test_factorialToDouble() {
        assertEquals(1.0, Numbers.factorialToDouble(0), 0.001);
        assertEquals(1.0, Numbers.factorialToDouble(1), 0.001);
        assertEquals(2.0, Numbers.factorialToDouble(2), 0.001);
        assertEquals(6.0, Numbers.factorialToDouble(3), 0.001);
        assertEquals(3628800.0, Numbers.factorialToDouble(10), 0.001);

        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToDouble(-1));
    }

    @Test
    public void test_factorialToBigInteger() {
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(0));
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(1));
        assertEquals(BigInteger.valueOf(2), Numbers.factorialToBigInteger(2));
        assertEquals(BigInteger.valueOf(6), Numbers.factorialToBigInteger(3));

        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToBigInteger(-1));
    }

    @Test
    public void test_binomial() {
        assertEquals(1, Numbers.binomial(5, 0));
        assertEquals(5, Numbers.binomial(5, 1));
        assertEquals(10, Numbers.binomial(5, 2));
        assertEquals(10, Numbers.binomial(5, 3));
        assertEquals(5, Numbers.binomial(5, 4));
        assertEquals(1, Numbers.binomial(5, 5));

        assertThrows(IllegalArgumentException.class, () -> Numbers.binomial(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomial(5, 6));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomial(-1, 2));
    }

    @Test
    public void test_binomialToLong() {
        assertEquals(1L, Numbers.binomialToLong(5, 0));
        assertEquals(5L, Numbers.binomialToLong(5, 1));
        assertEquals(10L, Numbers.binomialToLong(5, 2));
        assertEquals(20L, Numbers.binomialToLong(6, 3));

        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToLong(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToLong(5, 6));
    }

    @Test
    public void test_binomialToBigInteger() {
        assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(5, 0));
        assertEquals(BigInteger.valueOf(5), Numbers.binomialToBigInteger(5, 1));
        assertEquals(BigInteger.valueOf(10), Numbers.binomialToBigInteger(5, 2));

        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(5, 6));
    }

    @Test
    public void test_mean_int_int() {
        assertEquals(5, Numbers.mean(4, 6));
        assertEquals(0, Numbers.mean(-1, 1));
        assertEquals(Integer.MAX_VALUE, Numbers.mean(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void test_mean_long_long() {
        assertEquals(5L, Numbers.mean(4L, 6L));
        assertEquals(0L, Numbers.mean(-1L, 1L));
        assertEquals(Long.MAX_VALUE, Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void test_mean_double_double() {
        assertEquals(5.0, Numbers.mean(4.0, 6.0), 0.001);
        assertEquals(0.0, Numbers.mean(-1.0, 1.0), 0.001);
        assertEquals(3.5, Numbers.mean(3.0, 4.0), 0.001);
    }

    @Test
    public void test_mean_int_array() {
        assertEquals(5.0, Numbers.mean(3, 5, 7), 0.001);
        assertEquals(10.0, Numbers.mean(10), 0.001);

        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new int[0]));
    }

    @Test
    public void test_mean_long_array() {
        assertEquals(5.0, Numbers.mean(3L, 5L, 7L), 0.001);
        assertEquals(10.0, Numbers.mean(10L), 0.001);

        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new long[0]));
    }

    @Test
    public void test_mean_double_array() {
        assertEquals(5.0, Numbers.mean(3.0, 5.0, 7.0), 0.001);
        assertEquals(10.0, Numbers.mean(10.0), 0.001);
        assertEquals(3.5, Numbers.mean(3.0, 4.0), 0.001);

        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new double[0]));
    }

    @Test
    public void test_round_float_scale() {
        assertEquals(12.11f, Numbers.round(12.105f, 2), 0.001f);
        assertEquals(12.1f, Numbers.round(12.105f, 1), 0.001f);
        assertEquals(12.0f, Numbers.round(12.105f, 0), 0.001f);

        assertThrows(IllegalArgumentException.class, () -> Numbers.round(12.105f, -1));
    }

    @Test
    public void test_round_double_scale() {
        assertEquals(12.11, Numbers.round(12.105, 2), 0.001);
        assertEquals(12.1, Numbers.round(12.105, 1), 0.001);
        assertEquals(12.0, Numbers.round(12.105, 0), 0.001);

        assertThrows(IllegalArgumentException.class, () -> Numbers.round(12.105, -1));
    }

    @Test
    public void test_round_float_scale_mode() {
        assertEquals(12.11f, Numbers.round(12.105f, 2, RoundingMode.HALF_UP), 0.001f);
        assertEquals(12.10f, Numbers.round(12.105f, 2, RoundingMode.DOWN), 0.001f);
        assertEquals(12.11f, Numbers.round(12.105f, 2, RoundingMode.UP), 0.001f);
    }

    @Test
    public void test_round_double_scale_mode() {
        assertEquals(12.11, Numbers.round(12.105, 2, RoundingMode.HALF_UP), 0.001);
        assertEquals(12.10, Numbers.round(12.105, 2, RoundingMode.DOWN), 0.001);
        assertEquals(12.11, Numbers.round(12.105, 2, RoundingMode.UP), 0.001);
    }

    @Test
    public void test_round_float_decimalFormat_string() {
        assertEquals(12.1f, Numbers.round(12.105f, "0.00"), 0.001f);
        assertEquals(12.1f, Numbers.round(12.105f, "#.#"), 0.001f);
    }

    @Test
    public void test_round_double_decimalFormat_string() {
        assertEquals(12.11, Numbers.round(12.105, "0.00"), 0.001);
        assertEquals(12.1, Numbers.round(12.105, "#.#"), 0.001);
    }

    @Test
    public void test_roundToInt() {
        assertEquals(13, Numbers.roundToInt(12.5, RoundingMode.HALF_UP));
        assertEquals(13, Numbers.roundToInt(12.5, RoundingMode.CEILING));
        assertEquals(12, Numbers.roundToInt(12.5, RoundingMode.FLOOR));
        assertEquals(12, Numbers.roundToInt(12.3, RoundingMode.HALF_UP));

        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Double.POSITIVE_INFINITY, RoundingMode.HALF_UP));
    }

    @Test
    public void test_roundToLong() {
        assertEquals(13L, Numbers.roundToLong(12.5, RoundingMode.HALF_UP));
        assertEquals(13L, Numbers.roundToLong(12.5, RoundingMode.CEILING));
        assertEquals(12L, Numbers.roundToLong(12.5, RoundingMode.FLOOR));

        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.POSITIVE_INFINITY, RoundingMode.HALF_UP));
    }

    @Test
    public void test_roundToBigInteger() {
        assertEquals(BigInteger.valueOf(13), Numbers.roundToBigInteger(12.5, RoundingMode.HALF_UP));
        assertEquals(BigInteger.valueOf(13), Numbers.roundToBigInteger(12.5, RoundingMode.CEILING));
        assertEquals(BigInteger.valueOf(12), Numbers.roundToBigInteger(12.5, RoundingMode.FLOOR));

        assertThrows(ArithmeticException.class, () -> Numbers.roundToBigInteger(Double.POSITIVE_INFINITY, RoundingMode.HALF_UP));
    }

    @Test
    public void test_fuzzyEquals_float() {
        assertTrue(Numbers.fuzzyEquals(1.0f, 1.0001f, 0.001f));
        assertTrue(Numbers.fuzzyEquals(1.0f, 1.0f, 0.0f));
        assertFalse(Numbers.fuzzyEquals(1.0f, 1.1f, 0.01f));
        assertFalse(Numbers.fuzzyEquals(1.0f, 2.0f, 0.5f));
    }

    @Test
    public void test_fuzzyEquals_double() {
        assertTrue(Numbers.fuzzyEquals(1.0, 1.0001, 0.001));
        assertTrue(Numbers.fuzzyEquals(1.0, 1.0, 0.0));
        assertFalse(Numbers.fuzzyEquals(1.0, 1.1, 0.01));
        assertFalse(Numbers.fuzzyEquals(1.0, 2.0, 0.5));
    }

    @Test
    public void test_fuzzyCompare_float() {
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0001f, 0.001f));
        assertEquals(-1, Numbers.fuzzyCompare(1.0f, 2.0f, 0.1f));
        assertEquals(1, Numbers.fuzzyCompare(2.0f, 1.0f, 0.1f));
    }

    @Test
    public void test_fuzzyCompare_double() {
        assertEquals(0, Numbers.fuzzyCompare(1.0, 1.0001, 0.001));
        assertEquals(-1, Numbers.fuzzyCompare(1.0, 2.0, 0.1));
        assertEquals(1, Numbers.fuzzyCompare(2.0, 1.0, 0.1));
    }

    @Test
    public void test_isMathematicalInteger() {
        assertTrue(Numbers.isMathematicalInteger(1.0));
        assertTrue(Numbers.isMathematicalInteger(0.0));
        assertTrue(Numbers.isMathematicalInteger(-5.0));
        assertTrue(Numbers.isMathematicalInteger(100.0));
        assertFalse(Numbers.isMathematicalInteger(1.5));
        assertFalse(Numbers.isMathematicalInteger(0.1));
        assertFalse(Numbers.isMathematicalInteger(Double.NaN));
        assertFalse(Numbers.isMathematicalInteger(Double.POSITIVE_INFINITY));
    }

    @Test
    public void test_asinh() {
        assertEquals(0.0, Numbers.asinh(0.0), 0.0001);
        assertEquals(Math.log(2.0 + Math.sqrt(5.0)), Numbers.asinh(2.0), 0.0001);
        assertTrue(Numbers.asinh(1.0) > 0);
        assertTrue(Numbers.asinh(-1.0) < 0);
    }

    @Test
    public void test_acosh() {
        assertEquals(0.0, Numbers.acosh(1.0), 0.0001);
        assertTrue(Numbers.acosh(2.0) > 0);
        assertEquals(Double.NaN, Numbers.acosh(0.5), 0.0001);
    }

    @Test
    public void test_atanh() {
        assertEquals(0.0, Numbers.atanh(0.0), 0.0001);
        assertTrue(Numbers.atanh(0.5) > 0);
        assertTrue(Numbers.atanh(-0.5) < 0);
        assertEquals(Double.NaN, Numbers.atanh(1.5), 0.0001);
    }

    @Test
    public void test_round_float_decimalFormat() {
        java.text.DecimalFormat df1 = new java.text.DecimalFormat("0.00");
        assertEquals(12.1f, Numbers.round(12.105f, df1), 0.001f);

        java.text.DecimalFormat df2 = new java.text.DecimalFormat("#.#");
        assertEquals(12.1f, Numbers.round(12.105f, df2), 0.001f);

        java.text.DecimalFormat df3 = new java.text.DecimalFormat("0");
        assertEquals(12.0f, Numbers.round(12.105f, df3), 0.001f);

        assertThrows(IllegalArgumentException.class, () -> Numbers.round(12.105f, (java.text.DecimalFormat) null));
    }

    @Test
    public void test_round_double_decimalFormat() {
        java.text.DecimalFormat df1 = new java.text.DecimalFormat("0.00");
        assertEquals(12.11, Numbers.round(12.105, df1), 0.001);

        java.text.DecimalFormat df2 = new java.text.DecimalFormat("#.#");
        assertEquals(12.1, Numbers.round(12.105, df2), 0.001);

        java.text.DecimalFormat df3 = new java.text.DecimalFormat("0");
        assertEquals(12.0, Numbers.round(12.105, df3), 0.001);

        java.text.DecimalFormat df4 = new java.text.DecimalFormat("#.##%");
        assertEquals(0.12, Numbers.round(0.12156, df4), 0.01);

        assertThrows(IllegalArgumentException.class, () -> Numbers.round(12.105, (java.text.DecimalFormat) null));
    }

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
        assertEquals(Byte.valueOf((byte) 0), Numbers.convert(null, byte.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, int.class));
    }

    @Test
    public void testConvert_byteToOthers() {
        Byte val = (byte) 10;
        assertEquals(Byte.valueOf((byte) 10), Numbers.convert(val, byte.class));
        assertEquals(Short.valueOf((short) 10), Numbers.convert(val, short.class));
        assertEquals(Integer.valueOf(10), Numbers.convert(val, int.class));
        assertEquals(Long.valueOf(10L), Numbers.convert(val, long.class));
        assertEquals(10.0f, Numbers.convert(val, float.class), DELTA);
        assertEquals(10.0d, Numbers.convert(val, double.class), DELTA);
        assertEquals(BigInteger.valueOf(10), Numbers.convert(val, BigInteger.class));
        assertEquals(BigDecimal.valueOf(10), Numbers.convert(val, BigDecimal.class));
    }

    @Test
    public void testConvert_intToByte_valid() {
        assertEquals(Byte.valueOf((byte) 100), Numbers.convert(100, byte.class));
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
        assertEquals(Integer.valueOf(12345), Numbers.convert(12345L, int.class));
    }

    @Test
    public void testConvert_longToInt_overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Integer.MAX_VALUE + 1L, int.class));
    }

    @Test
    public void testConvert_doubleToLong_valid() {
        assertEquals(Long.valueOf(123L), Numbers.convert(123.45d, long.class));
    }

    @Test
    public void testConvert_doubleToLong_overflow() {
        assertEquals(Long.valueOf(Long.MAX_VALUE), Numbers.convert(Long.MAX_VALUE + 100.0, long.class));
    }

    @Test
    public void testConvert_bigDecimalToInteger_valid() {
        assertEquals(Integer.valueOf(123), Numbers.convert(new BigDecimal("123.789"), Integer.class));
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

    @Test
    public void testFormat_Integer_null() {
        assertEquals("0", Numbers.format((Integer) null, "#"));
        assertEquals("0.00", Numbers.format((Integer) null, "0.00"));
    }

    @Test
    public void testFormat_Long_null() {
        assertEquals("0.0", Numbers.format((Long) null, "0.0"));
    }

    @Test
    public void testFormat_Float_null() {
        assertEquals("0.00", Numbers.format((Float) null, "0.00"));
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
        assertEquals(123, Numbers.extractFirstInt("abc 123 def 456").get());
        assertEquals(-45, Numbers.extractFirstInt("xyz -45 abc").get());
        assertTrue(Numbers.extractFirstInt("abc def").isEmpty());
        assertTrue(Numbers.extractFirstInt("").isEmpty());
        assertTrue(Numbers.extractFirstInt(null).isEmpty());
    }

    @Test
    public void testExtractFirstInt_withDefault() {
        assertEquals(123, Numbers.extractFirstInt("abc 123 def", 99));
        assertEquals(99, Numbers.extractFirstInt("abc def", 99));
        assertEquals(99, Numbers.extractFirstInt(null, 99));
    }

    @Test
    public void testExtractFirstLong() {
        assertEquals(1234567890L, Numbers.extractFirstLong("abc 1234567890 def").get());
        assertEquals(-987L, Numbers.extractFirstLong("word -987 test").get());
        assertTrue(Numbers.extractFirstLong("abc def").isEmpty());
        assertTrue(Numbers.extractFirstLong("").isEmpty());
        assertTrue(Numbers.extractFirstLong(null).isEmpty());
    }

    @Test
    public void testExtractFirstLong_withDefault() {
        assertEquals(123L, Numbers.extractFirstLong("abc 123 def", 999L));
        assertEquals(999L, Numbers.extractFirstLong("abc def", 999L));
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
        assertTrue(Numbers.isConvertibleToNumber("123"));
        assertTrue(Numbers.isNumber("123"));
        assertTrue(Numbers.isConvertibleToNumber("123.45"));
        assertTrue(Numbers.isConvertibleToNumber("-1.2e-5"));
        assertTrue(Numbers.isConvertibleToNumber("0xFF"));
        assertTrue(Numbers.isConvertibleToNumber("077"));
        assertTrue(Numbers.isConvertibleToNumber("123L"));
        assertTrue(Numbers.isConvertibleToNumber("1.2f"));
        assertTrue(Numbers.isConvertibleToNumber("1.2d"));

        assertFalse(Numbers.isConvertibleToNumber("abc"));
        assertFalse(Numbers.isConvertibleToNumber(""));
        assertFalse(Numbers.isConvertibleToNumber(null));
        assertFalse(Numbers.isConvertibleToNumber("1.2.3"));
        assertFalse(Numbers.isConvertibleToNumber("--1"));
        assertFalse(Numbers.isConvertibleToNumber("09"));
        assertTrue(Numbers.isConvertibleToNumber("0.9"));
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
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Integer.MAX_VALUE + 1.0, RoundingMode.FLOOR));
    }

    @Test
    public void testRoundToLong() {
        assertEquals(5L, Numbers.roundToLong(4.5, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.POSITIVE_INFINITY, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Long.MAX_VALUE + 100.0, RoundingMode.FLOOR));
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

    @Test
    public void testLog2BigInteger() {
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.DOWN));
        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.FLOOR));

        Assertions.assertEquals(3, Numbers.log2(BigInteger.valueOf(10), RoundingMode.DOWN));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(10), RoundingMode.UP));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(10), RoundingMode.CEILING));

        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_DOWN));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_UP));
        Assertions.assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_EVEN));

        BigInteger large = new BigInteger("1000000000000000000000000000000");
        Assertions.assertTrue(Numbers.log2(large, RoundingMode.DOWN) > 0);

        Assertions.assertEquals(0, Numbers.log2(BigInteger.ONE, RoundingMode.UNNECESSARY));

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
        Assertions.assertEquals(0, Numbers.log10(1, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log10(10, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(1, Numbers.log10(15, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log10(15, RoundingMode.UP));
        Assertions.assertEquals(1, Numbers.log10(15, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.log10(15, RoundingMode.CEILING));

        Assertions.assertEquals(1, Numbers.log10(30, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.log10(32, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.log10(32, RoundingMode.HALF_EVEN));

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
        Assertions.assertEquals(0, Numbers.log10(1L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.log10(1000L, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(9, Numbers.log10(1_000_000_000L, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(5, Numbers.log10(123456L, RoundingMode.DOWN));
        Assertions.assertEquals(6, Numbers.log10(123456L, RoundingMode.UP));

        Assertions.assertEquals(2, Numbers.log10(316L, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.log10(316L, RoundingMode.HALF_UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log10(0L, RoundingMode.DOWN);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.log10(999L, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testLog10Double() {
        Assertions.assertEquals(0.0, Numbers.log10(1.0), 0.0001);
        Assertions.assertEquals(1.0, Numbers.log10(10.0), 0.0001);
        Assertions.assertEquals(2.0, Numbers.log10(100.0), 0.0001);

        Assertions.assertEquals(-1.0, Numbers.log10(0.1), 0.0001);

        Assertions.assertTrue(Double.isInfinite(Numbers.log10(0.0)));
        Assertions.assertTrue(Double.isNaN(Numbers.log10(-1.0)));
    }

    @Test
    public void testLog10BigInteger() {
        Assertions.assertEquals(3, Numbers.log10(BigInteger.valueOf(1000), RoundingMode.UNNECESSARY));

        BigInteger large = new BigInteger("1000000000000000000000000000000");
        int result = Numbers.log10(large, RoundingMode.DOWN);
        Assertions.assertTrue(result > 20);

        Assertions.assertEquals(2, Numbers.log10(BigInteger.valueOf(150), RoundingMode.DOWN));
        Assertions.assertEquals(3, Numbers.log10(BigInteger.valueOf(150), RoundingMode.UP));

        BigInteger val = BigInteger.valueOf(316);
        Assertions.assertEquals(2, Numbers.log10(val, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.log10(val, RoundingMode.HALF_UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.log10(BigInteger.ZERO, RoundingMode.DOWN);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.log10(BigInteger.valueOf(999), RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testPowInt() {
        Assertions.assertEquals(1, Numbers.pow(2, 0));
        Assertions.assertEquals(8, Numbers.pow(2, 3));
        Assertions.assertEquals(1024, Numbers.pow(2, 10));

        Assertions.assertEquals(1, Numbers.pow(0, 0));
        Assertions.assertEquals(0, Numbers.pow(0, 5));
        Assertions.assertEquals(1, Numbers.pow(1, 100));
        Assertions.assertEquals(1, Numbers.pow(-1, 0));
        Assertions.assertEquals(-1, Numbers.pow(-1, 1));
        Assertions.assertEquals(1, Numbers.pow(-1, 2));

        Assertions.assertEquals(4, Numbers.pow(-2, 2));
        Assertions.assertEquals(-8, Numbers.pow(-2, 3));

        Assertions.assertEquals(0, Numbers.pow(2, 32));

        Assertions.assertEquals(81, Numbers.pow(3, 4));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.pow(2, -1);
        });
    }

    @Test
    public void testPowLong() {
        Assertions.assertEquals(1L, Numbers.pow(2L, 0));
        Assertions.assertEquals(8L, Numbers.pow(2L, 3));

        Assertions.assertEquals(1L, Numbers.pow(0L, 0));
        Assertions.assertEquals(0L, Numbers.pow(0L, 5));
        Assertions.assertEquals(1L, Numbers.pow(1L, 100));
        Assertions.assertEquals(-1L, Numbers.pow(-1L, 1));

        Assertions.assertEquals(1L << 30, Numbers.pow(2L, 30));

        Assertions.assertEquals(27L, Numbers.pow(3L, 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.pow(2L, -1);
        });
    }

    @Test
    public void testCeilingPowerOfTwo() {
        Assertions.assertEquals(1L, Numbers.ceilingPowerOfTwo(1L));
        Assertions.assertEquals(2L, Numbers.ceilingPowerOfTwo(2L));
        Assertions.assertEquals(4L, Numbers.ceilingPowerOfTwo(4L));

        Assertions.assertEquals(4L, Numbers.ceilingPowerOfTwo(3L));
        Assertions.assertEquals(8L, Numbers.ceilingPowerOfTwo(5L));
        Assertions.assertEquals(16L, Numbers.ceilingPowerOfTwo(9L));

        Assertions.assertEquals(1L << 30, Numbers.ceilingPowerOfTwo((1L << 30) - 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.ceilingPowerOfTwo(0L);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.ceilingPowerOfTwo((1L << 62) + 1);
        });
    }

    @Test
    public void testCeilingPowerOfTwoBigInteger() {
        Assertions.assertEquals(BigInteger.valueOf(4), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(3)));
        Assertions.assertEquals(BigInteger.valueOf(8), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(8)));

        BigInteger large = new BigInteger("1000000000000000000000000");
        BigInteger result = Numbers.ceilingPowerOfTwo(large);
        Assertions.assertTrue(result.compareTo(large) >= 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.ceilingPowerOfTwo(BigInteger.ZERO);
        });
    }

    @Test
    public void testFloorPowerOfTwo() {
        Assertions.assertEquals(1L, Numbers.floorPowerOfTwo(1L));
        Assertions.assertEquals(2L, Numbers.floorPowerOfTwo(2L));
        Assertions.assertEquals(4L, Numbers.floorPowerOfTwo(4L));

        Assertions.assertEquals(2L, Numbers.floorPowerOfTwo(3L));
        Assertions.assertEquals(4L, Numbers.floorPowerOfTwo(5L));
        Assertions.assertEquals(8L, Numbers.floorPowerOfTwo(15L));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.floorPowerOfTwo(0L);
        });
    }

    @Test
    public void testFloorPowerOfTwoBigInteger() {
        Assertions.assertEquals(BigInteger.valueOf(2), Numbers.floorPowerOfTwo(BigInteger.valueOf(3)));
        Assertions.assertEquals(BigInteger.valueOf(8), Numbers.floorPowerOfTwo(BigInteger.valueOf(8)));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.floorPowerOfTwo(BigInteger.ZERO);
        });
    }

    @Test
    public void testSqrtInt() {
        Assertions.assertEquals(0, Numbers.sqrt(0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.sqrt(1, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.sqrt(4, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(1, Numbers.sqrt(2, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.sqrt(2, RoundingMode.UP));
        Assertions.assertEquals(1, Numbers.sqrt(2, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.sqrt(2, RoundingMode.CEILING));

        Assertions.assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_EVEN));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.sqrt(-1, RoundingMode.DOWN);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.sqrt(2, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testSqrtLong() {
        Assertions.assertEquals(0L, Numbers.sqrt(0L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(10L, Numbers.sqrt(100L, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(100000L, Numbers.sqrt(10000000000L, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(3L, Numbers.sqrt(10L, RoundingMode.DOWN));
        Assertions.assertEquals(4L, Numbers.sqrt(10L, RoundingMode.UP));

        Assertions.assertEquals(7L, Numbers.sqrt(50L, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(7L, Numbers.sqrt(50L, RoundingMode.HALF_UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.sqrt(-1L, RoundingMode.DOWN);
        });
    }

    @Test
    public void testSqrtBigInteger() {
        Assertions.assertEquals(BigInteger.ZERO, Numbers.sqrt(BigInteger.ZERO, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.sqrt(BigInteger.valueOf(10000), RoundingMode.UNNECESSARY));

        BigInteger large = new BigInteger("1000000000000000000000000");
        BigInteger largeSq = large.multiply(large);
        Assertions.assertEquals(large, Numbers.sqrt(largeSq, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.DOWN));
        Assertions.assertEquals(BigInteger.valueOf(4), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.DOWN);
        });
    }

    @Test
    public void testDivideInt() {
        Assertions.assertEquals(2, Numbers.divide(10, 5, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(3, Numbers.divide(10, 3, RoundingMode.DOWN));
        Assertions.assertEquals(4, Numbers.divide(10, 3, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.divide(10, 3, RoundingMode.FLOOR));
        Assertions.assertEquals(4, Numbers.divide(10, 3, RoundingMode.CEILING));

        Assertions.assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.DOWN));
        Assertions.assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.UP));
        Assertions.assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.FLOOR));
        Assertions.assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.CEILING));

        Assertions.assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(3, Numbers.divide(5, 2, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_EVEN));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(10, 0, RoundingMode.DOWN);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(10, 3, RoundingMode.UNNECESSARY);
        });
    }

    @Test
    public void testDivideLong() {
        Assertions.assertEquals(2L, Numbers.divide(10L, 5L, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(3L, Numbers.divide(10L, 3L, RoundingMode.DOWN));
        Assertions.assertEquals(4L, Numbers.divide(10L, 3L, RoundingMode.UP));

        Assertions.assertEquals(1000000L, Numbers.divide(1000000000000L, 1000000L, RoundingMode.UNNECESSARY));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(10L, 0L, RoundingMode.DOWN);
        });
    }

    @Test
    public void testDivideBigInteger() {
        BigInteger result = Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(5), RoundingMode.UNNECESSARY);
        Assertions.assertEquals(BigInteger.valueOf(2), result);

        result = Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.DOWN);
        Assertions.assertEquals(BigInteger.valueOf(3), result);

        result = Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.UP);
        Assertions.assertEquals(BigInteger.valueOf(4), result);

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.divide(BigInteger.valueOf(10), BigInteger.ZERO, RoundingMode.DOWN);
        });
    }

    @Test
    public void testModInt() {
        Assertions.assertEquals(3, Numbers.mod(7, 4));
        Assertions.assertEquals(1, Numbers.mod(-7, 4));
        Assertions.assertEquals(3, Numbers.mod(-1, 4));
        Assertions.assertEquals(0, Numbers.mod(-8, 4));
        Assertions.assertEquals(0, Numbers.mod(8, 4));

        Assertions.assertEquals(0, Numbers.mod(0, 5));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10, 0);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10, -1);
        });
    }

    @Test
    public void testModLongInt() {
        Assertions.assertEquals(3, Numbers.mod(7L, 4));
        Assertions.assertEquals(1, Numbers.mod(-7L, 4));

        Assertions.assertEquals(123456 % 1000, Numbers.mod(123456L, 1000));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10L, 0);
        });
    }

    @Test
    public void testModLong() {
        Assertions.assertEquals(3L, Numbers.mod(7L, 4L));
        Assertions.assertEquals(1L, Numbers.mod(-7L, 4L));
        Assertions.assertEquals(3L, Numbers.mod(-1L, 4L));
        Assertions.assertEquals(0L, Numbers.mod(-8L, 4L));
        Assertions.assertEquals(0L, Numbers.mod(8L, 4L));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.mod(10L, 0L);
        });
    }

    @Test
    public void testGcdInt() {
        Assertions.assertEquals(6, Numbers.gcd(12, 18));
        Assertions.assertEquals(1, Numbers.gcd(17, 19));
        Assertions.assertEquals(10, Numbers.gcd(0, 10));
        Assertions.assertEquals(10, Numbers.gcd(10, 0));

        Assertions.assertEquals(6, Numbers.gcd(-12, 18));
        Assertions.assertEquals(6, Numbers.gcd(12, -18));
        Assertions.assertEquals(6, Numbers.gcd(-12, -18));

        Assertions.assertEquals(0, Numbers.gcd(0, 0));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.gcd(0, Integer.MIN_VALUE);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.gcd(Integer.MIN_VALUE, 0);
        });
    }

    @Test
    public void testGcdLong() {
        Assertions.assertEquals(6L, Numbers.gcd(12L, 18L));
        Assertions.assertEquals(1L, Numbers.gcd(17L, 19L));
        Assertions.assertEquals(10L, Numbers.gcd(0L, 10L));

        Assertions.assertEquals(6L, Numbers.gcd(-12L, 18L));

        Assertions.assertEquals(1000000L, Numbers.gcd(1000000L, 2000000L));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.gcd(0L, Long.MIN_VALUE);
        });
    }

    @Test
    public void testLcmInt() {
        Assertions.assertEquals(36, Numbers.lcm(12, 18));
        Assertions.assertEquals(323, Numbers.lcm(17, 19));

        Assertions.assertEquals(0, Numbers.lcm(0, 10));
        Assertions.assertEquals(0, Numbers.lcm(10, 0));

        Assertions.assertEquals(36, Numbers.lcm(-12, 18));
        Assertions.assertEquals(36, Numbers.lcm(12, -18));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testLcmLong() {
        Assertions.assertEquals(36L, Numbers.lcm(12L, 18L));

        Assertions.assertEquals(0L, Numbers.lcm(0L, 10L));

        long a = 1000000L;
        long b = 2000000L;
        Assertions.assertEquals(2000000L, Numbers.lcm(a, b));
    }

    @Test
    public void testAddExactInt() {
        Assertions.assertEquals(5, Numbers.addExact(2, 3));
        Assertions.assertEquals(0, Numbers.addExact(-5, 5));

        Assertions.assertEquals(0, Numbers.addExact(Integer.MAX_VALUE, -Integer.MAX_VALUE));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.addExact(Integer.MAX_VALUE, 1);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.addExact(Integer.MIN_VALUE, -1);
        });
    }

    @Test
    public void testAddExactLong() {
        Assertions.assertEquals(5L, Numbers.addExact(2L, 3L));

        Assertions.assertEquals(2000000000L, Numbers.addExact(1000000000L, 1000000000L));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.addExact(Long.MAX_VALUE, 1L);
        });
    }

    @Test
    public void testSubtractExactInt() {
        Assertions.assertEquals(-1, Numbers.subtractExact(2, 3));
        Assertions.assertEquals(5, Numbers.subtractExact(8, 3));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.subtractExact(Integer.MIN_VALUE, 1);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.subtractExact(Integer.MAX_VALUE, -1);
        });
    }

    @Test
    public void testSubtractExactLong() {
        Assertions.assertEquals(-1L, Numbers.subtractExact(2L, 3L));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.subtractExact(Long.MIN_VALUE, 1L);
        });
    }

    @Test
    public void testMultiplyExactInt() {
        Assertions.assertEquals(6, Numbers.multiplyExact(2, 3));
        Assertions.assertEquals(0, Numbers.multiplyExact(0, Integer.MAX_VALUE));

        Assertions.assertEquals(-6, Numbers.multiplyExact(-2, 3));
        Assertions.assertEquals(6, Numbers.multiplyExact(-2, -3));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.multiplyExact(Integer.MAX_VALUE, 2);
        });
    }

    @Test
    public void testMultiplyExactLong() {
        Assertions.assertEquals(6L, Numbers.multiplyExact(2L, 3L));

        Assertions.assertEquals(1000000000000L, Numbers.multiplyExact(1000000L, 1000000L));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.multiplyExact(Long.MAX_VALUE, 2L);
        });
    }

    @Test
    public void testPowExactInt() {
        Assertions.assertEquals(1, Numbers.powExact(2, 0));
        Assertions.assertEquals(8, Numbers.powExact(2, 3));

        Assertions.assertEquals(1, Numbers.powExact(0, 0));
        Assertions.assertEquals(0, Numbers.powExact(0, 5));
        Assertions.assertEquals(1, Numbers.powExact(1, 100));
        Assertions.assertEquals(-1, Numbers.powExact(-1, 1));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.powExact(2, 31);
        });

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.powExact(3, 20);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.powExact(2, -1);
        });
    }

    @Test
    public void testPowExactLong() {
        Assertions.assertEquals(1L, Numbers.powExact(2L, 0));
        Assertions.assertEquals(8L, Numbers.powExact(2L, 3));

        Assertions.assertEquals(1L << 30, Numbers.powExact(2L, 30));

        Assertions.assertThrows(ArithmeticException.class, () -> {
            Numbers.powExact(2L, 63);
        });
    }

    @Test
    public void testSaturatedAddInt() {
        Assertions.assertEquals(5, Numbers.saturatedAdd(2, 3));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, 1));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, -1));
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void testSaturatedSubtractInt() {
        Assertions.assertEquals(-1, Numbers.saturatedSubtract(2, 3));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedSubtract(Integer.MAX_VALUE, -1));

        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.saturatedSubtract(Integer.MIN_VALUE, 1));
    }

    @Test
    public void testSaturatedSubtractLong() {
        Assertions.assertEquals(-1L, Numbers.saturatedSubtract(2L, 3L));

        Assertions.assertEquals(Long.MAX_VALUE, Numbers.saturatedSubtract(Long.MAX_VALUE, -1L));

        Assertions.assertEquals(Long.MIN_VALUE, Numbers.saturatedSubtract(Long.MIN_VALUE, 1L));
    }

    @Test
    public void testSaturatedPowInt() {
        Assertions.assertEquals(1, Numbers.saturatedPow(2, 0));
        Assertions.assertEquals(8, Numbers.saturatedPow(2, 3));

        Assertions.assertEquals(1, Numbers.saturatedPow(0, 0));
        Assertions.assertEquals(0, Numbers.saturatedPow(0, 5));
        Assertions.assertEquals(1, Numbers.saturatedPow(1, 100));
        Assertions.assertEquals(-1, Numbers.saturatedPow(-1, 1));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(2, 31));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(3, 20));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.saturatedPow(2, -1);
        });
    }

    @Test
    public void testSaturatedPowLong() {
        Assertions.assertEquals(1L, Numbers.saturatedPow(2L, 0));
        Assertions.assertEquals(8L, Numbers.saturatedPow(2L, 3));

        Assertions.assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(2L, 63));
    }

    @Test
    public void testFactorial() {
        Assertions.assertEquals(1, Numbers.factorial(0));
        Assertions.assertEquals(1, Numbers.factorial(1));
        Assertions.assertEquals(2, Numbers.factorial(2));
        Assertions.assertEquals(6, Numbers.factorial(3));
        Assertions.assertEquals(24, Numbers.factorial(4));
        Assertions.assertEquals(120, Numbers.factorial(5));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.factorial(13));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.factorial(20));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.factorial(-1);
        });
    }

    @Test
    public void testBinomial() {
        Assertions.assertEquals(1, Numbers.binomial(0, 0));
        Assertions.assertEquals(1, Numbers.binomial(5, 0));
        Assertions.assertEquals(5, Numbers.binomial(5, 1));
        Assertions.assertEquals(10, Numbers.binomial(5, 2));
        Assertions.assertEquals(10, Numbers.binomial(5, 3));
        Assertions.assertEquals(5, Numbers.binomial(5, 4));
        Assertions.assertEquals(1, Numbers.binomial(5, 5));

        Assertions.assertEquals(252, Numbers.binomial(10, 5));

        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.binomial(40, 20));

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
    public void testMeanInt() {
        Assertions.assertEquals(3, Numbers.mean(2, 4));
        Assertions.assertEquals(0, Numbers.mean(-1, 1));

        Assertions.assertEquals(Integer.MAX_VALUE - 1, Numbers.mean(Integer.MAX_VALUE, Integer.MAX_VALUE - 2));

        Assertions.assertEquals(-3, Numbers.mean(-2, -4));
    }

    @Test
    public void testMeanLong() {
        Assertions.assertEquals(3L, Numbers.mean(2L, 4L));
        Assertions.assertEquals(0L, Numbers.mean(-1L, 1L));

        Assertions.assertEquals(Long.MAX_VALUE - 1, Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE - 2));
    }

    @Test
    public void testMeanDouble() {
        Assertions.assertEquals(3.0, Numbers.mean(2.0, 4.0), 0.0001);
        Assertions.assertEquals(0.0, Numbers.mean(-1.0, 1.0), 0.0001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(Double.NaN, 1.0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(1.0, Double.POSITIVE_INFINITY);
        });
    }

    @Test
    public void testMeanIntArray() {
        Assertions.assertEquals(3.0, Numbers.mean(1, 2, 3, 4, 5), 0.0001);
        Assertions.assertEquals(10.0, Numbers.mean(10), 0.0001);
        Assertions.assertEquals(0.0, Numbers.mean(-5, 0, 5), 0.0001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(new int[0]);
        });
    }

    @Test
    public void testMeanLongArray() {
        Assertions.assertEquals(3.0, Numbers.mean(1L, 2L, 3L, 4L, 5L), 0.0001);
        Assertions.assertEquals(10.0, Numbers.mean(10L), 0.0001);

        Assertions.assertEquals(1000000.0, Numbers.mean(999999L, 1000000L, 1000001L), 0.0001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(new long[0]);
        });
    }

    @Test
    public void testMeanDoubleArray() {
        Assertions.assertEquals(3.0, Numbers.mean(1.0, 2.0, 3.0, 4.0, 5.0), 0.0001);
        Assertions.assertEquals(10.5, Numbers.mean(10.0, 11.0), 0.0001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(new double[0]);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.mean(1.0, Double.NaN);
        });
    }

    @Test
    public void testRoundFloat() {
        Assertions.assertEquals(1.0f, Numbers.round(1.234f, 0), 0.0001f);
        Assertions.assertEquals(1.2f, Numbers.round(1.234f, 1), 0.0001f);
        Assertions.assertEquals(1.23f, Numbers.round(1.234f, 2), 0.0001f);
        Assertions.assertEquals(1.234f, Numbers.round(1.234f, 3), 0.0001f);

        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1), 0.0001f);

        Assertions.assertEquals(-1.2f, Numbers.round(-1.234f, 1), 0.0001f);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234f, -1);
        });
    }

    @Test
    public void testRoundDouble() {
        Assertions.assertEquals(1.0, Numbers.round(1.234, 0), 0.0001);
        Assertions.assertEquals(1.2, Numbers.round(1.234, 1), 0.0001);
        Assertions.assertEquals(1.23, Numbers.round(1.234, 2), 0.0001);

        Assertions.assertEquals(1.3, Numbers.round(1.25, 1), 0.0001);

        Assertions.assertEquals(1.2345679, Numbers.round(1.23456789, 7), 0.00000001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234, -1);
        });
    }

    @Test
    public void testRoundFloatWithRoundingMode() {
        Assertions.assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.DOWN), 0.0001f);
        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1, RoundingMode.UP), 0.0001f);
        Assertions.assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.HALF_DOWN), 0.0001f);
        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1, RoundingMode.HALF_UP), 0.0001f);
        Assertions.assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.HALF_EVEN), 0.0001f);

        Assertions.assertEquals(1.3f, Numbers.round(1.25f, 1, null), 0.0001f);
    }

    @Test
    public void testRoundDoubleWithRoundingMode() {
        Assertions.assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.DOWN), 0.0001);
        Assertions.assertEquals(1.3, Numbers.round(1.25, 1, RoundingMode.UP), 0.0001);
        Assertions.assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.HALF_DOWN), 0.0001);
        Assertions.assertEquals(1.3, Numbers.round(1.25, 1, RoundingMode.HALF_UP), 0.0001);
        Assertions.assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.HALF_EVEN), 0.0001);
    }

    @Test
    public void testRoundFloatWithStringFormat() {
        float result = Numbers.round(1.234f, "#.#");
        Assertions.assertEquals(1.2f, result, 0.0001f);

        result = Numbers.round(1.234f, "#.##");
        Assertions.assertEquals(1.23f, result, 0.0001f);

        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.round(0.5f, "#%"));
    }

    @Test
    public void testRoundDoubleWithStringFormat() {
        double result = Numbers.round(1.234, "#.#");
        Assertions.assertEquals(1.2, result, 0.0001);

        result = Numbers.round(1.234, "#.##");
        Assertions.assertEquals(1.23, result, 0.0001);

        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.round(0.5, "#%"));
    }

    @Test
    public void testRoundFloatWithDecimalFormat() {
        DecimalFormat df = new DecimalFormat("#.#");
        float result = Numbers.round(1.234f, df);
        Assertions.assertEquals(1.2f, result, 0.0001f);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234f, (DecimalFormat) null);
        });
    }

    @Test
    public void testRoundDoubleWithDecimalFormat() {
        DecimalFormat df = new DecimalFormat("#.##");
        double result = Numbers.round(1.234, df);
        Assertions.assertEquals(1.23, result, 0.0001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.round(1.234, (DecimalFormat) null);
        });
    }

    @Test
    public void testFuzzyEqualsFloat() {
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0f, 1.0f, 0.0f));

        Assertions.assertTrue(Numbers.fuzzyEquals(1.0f, 1.001f, 0.01f));
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0f, 0.999f, 0.01f));

        Assertions.assertFalse(Numbers.fuzzyEquals(1.0f, 1.1f, 0.01f));

        Assertions.assertTrue(Numbers.fuzzyEquals(Float.NaN, Float.NaN, 0.01f));
        Assertions.assertTrue(Numbers.fuzzyEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.01f));
        Assertions.assertFalse(Numbers.fuzzyEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1000.0f));

        Assertions.assertTrue(Numbers.fuzzyEquals(0.0f, -0.0f, 0.0f));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyEquals(1.0f, 1.0f, -0.1f);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyEquals(1.0f, 1.0f, Float.NaN);
        });
    }

    @Test
    public void testFuzzyEqualsDouble() {
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0, 1.0, 0.0));

        Assertions.assertTrue(Numbers.fuzzyEquals(1.0, 1.001, 0.01));
        Assertions.assertTrue(Numbers.fuzzyEquals(1.0, 0.999, 0.01));

        Assertions.assertFalse(Numbers.fuzzyEquals(1.0, 1.1, 0.01));

        Assertions.assertTrue(Numbers.fuzzyEquals(Double.NaN, Double.NaN, 0.01));
        Assertions.assertTrue(Numbers.fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyEquals(1.0, 1.0, -0.1);
        });
    }

    @Test
    public void testFuzzyCompareFloat() {
        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0f, 0.0f));

        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.001f, 0.01f));

        Assertions.assertTrue(Numbers.fuzzyCompare(1.0f, 2.0f, 0.01f) < 0);
        Assertions.assertTrue(Numbers.fuzzyCompare(2.0f, 1.0f, 0.01f) > 0);

        Assertions.assertEquals(0, Numbers.fuzzyCompare(Float.NaN, Float.NaN, 0.01f));
        Assertions.assertTrue(Numbers.fuzzyCompare(1.0f, Float.NaN, 0.01f) < 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyCompare(1.0f, 1.0f, -0.1f);
        });
    }

    @Test
    public void testFuzzyCompareDouble() {
        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0, 1.0, 0.0));

        Assertions.assertEquals(0, Numbers.fuzzyCompare(1.0, 1.001, 0.01));

        Assertions.assertTrue(Numbers.fuzzyCompare(1.0, 2.0, 0.01) < 0);
        Assertions.assertTrue(Numbers.fuzzyCompare(2.0, 1.0, 0.01) > 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Numbers.fuzzyCompare(1.0, 1.0, -0.1);
        });
    }

    @Test
    public void testListProduct() {
        List<BigInteger> empty = new ArrayList<>();
        Assertions.assertEquals(BigInteger.ONE, Numbers.listProduct(empty));

        List<BigInteger> single = new ArrayList<>();
        single.add(BigInteger.valueOf(5));
        Assertions.assertEquals(BigInteger.valueOf(5), Numbers.listProduct(single));

        List<BigInteger> two = new ArrayList<>();
        two.add(BigInteger.valueOf(3));
        two.add(BigInteger.valueOf(4));
        Assertions.assertEquals(BigInteger.valueOf(12), Numbers.listProduct(two));

        List<BigInteger> three = new ArrayList<>();
        three.add(BigInteger.valueOf(2));
        three.add(BigInteger.valueOf(3));
        three.add(BigInteger.valueOf(4));
        Assertions.assertEquals(BigInteger.valueOf(24), Numbers.listProduct(three));

        List<BigInteger> multiple = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            multiple.add(BigInteger.valueOf(i));
        }
        Assertions.assertEquals(BigInteger.valueOf(120), Numbers.listProduct(multiple));
    }

    @Test
    public void testFitsInLong() {
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.ZERO));
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.ONE));
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MAX_VALUE)));
        Assertions.assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MIN_VALUE)));

        BigInteger tooLarge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        Assertions.assertFalse(Numbers.fitsInLong(tooLarge));

        BigInteger tooSmall = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        Assertions.assertFalse(Numbers.fitsInLong(tooSmall));

        BigInteger veryLarge = new BigInteger("123456789012345678901234567890");
        Assertions.assertFalse(Numbers.fitsInLong(veryLarge));
    }

    @Test
    public void testConvertNumberToTargetType() {
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10, byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10L, Byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10.0f, byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10.0d, byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(BigInteger.valueOf(10), byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(BigDecimal.valueOf(10), byte.class));

        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(1000, byte.class));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(-1000, byte.class));

        Assertions.assertEquals(Short.valueOf((short) 1000), Numbers.convert(1000, short.class));
        Assertions.assertEquals(Short.valueOf((short) 1000), Numbers.convert(1000L, Short.class));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(100000, short.class));

        Assertions.assertEquals(Integer.valueOf(100000), Numbers.convert(100000L, int.class));
        Assertions.assertEquals(Integer.valueOf(100000), Numbers.convert(100000.0f, Integer.class));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(10000000000L, int.class));

        Assertions.assertEquals(Long.valueOf(10000000000L), Numbers.convert(10000000000L, long.class));
        Assertions.assertEquals(Long.valueOf(10000000000L), Numbers.convert(BigInteger.valueOf(10000000000L), Long.class));

        Assertions.assertEquals(Float.valueOf(10.5f), Numbers.convert(10.5d, float.class));
        Assertions.assertEquals(Float.valueOf(10.5f), Numbers.convert(BigDecimal.valueOf(10.5), Float.class));

        Assertions.assertEquals(Double.valueOf(10.5d), Numbers.convert(10.5f, double.class));
        Assertions.assertEquals(Double.valueOf(10.5d), Numbers.convert(BigDecimal.valueOf(10.5), Double.class));

        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.convert(100, BigInteger.class));
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.convert(100L, BigInteger.class));
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.convert(100.0d, BigInteger.class));

        Assertions.assertEquals(BigDecimal.valueOf(100), Numbers.convert(100, BigDecimal.class));
        Assertions.assertEquals(BigDecimal.valueOf(100L), Numbers.convert(100L, BigDecimal.class));

        Assertions.assertEquals(Byte.valueOf((byte) 0), Numbers.convert(null, byte.class));
        Assertions.assertEquals(null, Numbers.convert(null, Integer.class));
    }

    @Test
    public void testConvertNumberWithType() {
        Type<Integer> intType = Type.of(int.class);
        Type<Long> longType = Type.of(Long.class);

        Assertions.assertEquals(Integer.valueOf(100), Numbers.convert(100L, intType));
        Assertions.assertEquals(Long.valueOf(100L), Numbers.convert(100, longType));

        Assertions.assertEquals(Integer.valueOf(0), Numbers.convert(null, intType));
    }

    @Test
    public void testFormatInteger() {
        Assertions.assertEquals("123", Numbers.format(Integer.valueOf(123), "0"));
        Assertions.assertEquals("0", Numbers.format((Integer) null, "0"));
        Assertions.assertEquals("123.00", Numbers.format(Integer.valueOf(123), "0.00"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(Integer.valueOf(123), null));
    }

    @Test
    public void testFormatLongObject() {
        Assertions.assertEquals("1234567890", Numbers.format(Long.valueOf(1234567890L), "0"));
        Assertions.assertEquals("0", Numbers.format((Long) null, "0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(Long.valueOf(123L), null));
    }

    @Test
    public void testFormatFloat() {
        Assertions.assertEquals("12.10", Numbers.format(12.105f, "0.00"));
        Assertions.assertEquals("12.1", Numbers.format(12.105f, "#.##"));
        Assertions.assertEquals("12.1%", Numbers.format(0.121f, "#.##%"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.1f, null));
    }

    @Test
    public void testFormatFloatObject() {
        Assertions.assertEquals("12.10", Numbers.format(Float.valueOf(12.105f), "0.00"));
        Assertions.assertEquals("0.00", Numbers.format((Float) null, "0.00"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(Float.valueOf(12.1f), null));
    }

    @Test
    public void testFormatDouble() {
        Assertions.assertEquals("12.11", Numbers.format(12.105d, "0.00"));
        Assertions.assertEquals("12.11", Numbers.format(12.105d, "#.##"));
        Assertions.assertEquals("12.10%", Numbers.format(0.121d, "0.00%"));
        Assertions.assertEquals("12.16%", Numbers.format(0.12156d, "#.##%"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.1d, null));
    }

    @Test
    public void testFormatDoubleObject() {
        Assertions.assertEquals("12.11", Numbers.format(Double.valueOf(12.105d), "0.00"));
        Assertions.assertEquals("0.00", Numbers.format((Double) null, "0.00"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(Double.valueOf(12.1d), null));
    }

    @Test
    public void testToDoubleBigDecimal() {
        Assertions.assertEquals(123.456, Numbers.toDouble(BigDecimal.valueOf(123.456)), 0.001);
        Assertions.assertEquals(0.0, Numbers.toDouble((BigDecimal) null), 0.001);

        Assertions.assertEquals(999.999, Numbers.toDouble((BigDecimal) null, 999.999), 0.001);
        Assertions.assertEquals(123.456, Numbers.toDouble(BigDecimal.valueOf(123.456), 999.999), 0.001);
    }

    @Test
    public void testToScaledBigDecimal() {
        BigDecimal bd = BigDecimal.valueOf(123.456789);
        BigDecimal scaled = Numbers.toScaledBigDecimal(bd);
        Assertions.assertEquals(2, scaled.scale());
        Assertions.assertEquals(BigDecimal.valueOf(123.46), scaled);

        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((BigDecimal) null));

        scaled = Numbers.toScaledBigDecimal(bd, 3, RoundingMode.DOWN);
        Assertions.assertEquals(3, scaled.scale());
        Assertions.assertEquals(BigDecimal.valueOf(123.456), scaled);

        Float f = 123.456789f;
        scaled = Numbers.toScaledBigDecimal(f);
        Assertions.assertEquals(2, scaled.scale());

        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Float) null));

        scaled = Numbers.toScaledBigDecimal(f, 3, RoundingMode.UP);
        Assertions.assertEquals(3, scaled.scale());

        Double d = 123.456789;
        scaled = Numbers.toScaledBigDecimal(d);
        Assertions.assertEquals(2, scaled.scale());

        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Double) null));

        scaled = Numbers.toScaledBigDecimal(d, 3, RoundingMode.CEILING);
        Assertions.assertEquals(3, scaled.scale());

        String s = "123.456789";
        scaled = Numbers.toScaledBigDecimal(s);
        Assertions.assertEquals(2, scaled.scale());

        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((String) null));

        scaled = Numbers.toScaledBigDecimal(s, 3, RoundingMode.FLOOR);
        Assertions.assertEquals(3, scaled.scale());
    }

    @Test
    public void testIsNumber() {
        Assertions.assertTrue(Numbers.isNumber("123"));
        Assertions.assertTrue(Numbers.isNumber("-123"));
        Assertions.assertTrue(Numbers.isNumber("123.45"));
        Assertions.assertTrue(Numbers.isNumber("-123.45"));
        Assertions.assertTrue(Numbers.isNumber("1.23e5"));
        Assertions.assertTrue(Numbers.isNumber("1.23E5"));
        Assertions.assertTrue(Numbers.isNumber("123L"));
        Assertions.assertTrue(Numbers.isNumber("123l"));
        Assertions.assertTrue(Numbers.isNumber("123.45f"));
        Assertions.assertTrue(Numbers.isNumber("123.45F"));
        Assertions.assertTrue(Numbers.isNumber("123.45d"));
        Assertions.assertTrue(Numbers.isNumber("123.45D"));
        Assertions.assertTrue(Numbers.isNumber("0x1A"));
        Assertions.assertTrue(Numbers.isNumber("077"));

        Assertions.assertFalse(Numbers.isNumber(""));
        Assertions.assertFalse(Numbers.isNumber(null));
        Assertions.assertFalse(Numbers.isNumber("abc"));
        Assertions.assertFalse(Numbers.isNumber("123.45.67"));
        Assertions.assertFalse(Numbers.isNumber("123e"));
    }

    @Test
    public void testIsCreatable() {
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("-123"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123.45"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("-123.45"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.23e5"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.23E5"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123L"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123l"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123.45f"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123.45F"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123.45d"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123.45D"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("0x1A"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("077"));

        Assertions.assertFalse(Numbers.isConvertibleToNumber(""));
        Assertions.assertFalse(Numbers.isConvertibleToNumber(null));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("abc"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("123.45.67"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("123e"));
    }

    @Test
    public void testLog2Int() {
        Assertions.assertEquals(0, Numbers.log2(1, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log2(2, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.log2(4, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(10, Numbers.log2(1024, RoundingMode.UNNECESSARY));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.log2(3, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(0, Numbers.log2(1, RoundingMode.DOWN));
        Assertions.assertEquals(1, Numbers.log2(2, RoundingMode.DOWN));
        Assertions.assertEquals(1, Numbers.log2(3, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(4, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(5, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(7, RoundingMode.DOWN));
        Assertions.assertEquals(3, Numbers.log2(8, RoundingMode.DOWN));

        Assertions.assertEquals(0, Numbers.log2(1, RoundingMode.UP));
        Assertions.assertEquals(1, Numbers.log2(2, RoundingMode.UP));
        Assertions.assertEquals(2, Numbers.log2(3, RoundingMode.UP));
        Assertions.assertEquals(2, Numbers.log2(4, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(5, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(7, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(8, RoundingMode.UP));

        Assertions.assertEquals(2, Numbers.log2(3, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.log2(5, RoundingMode.HALF_UP));
        Assertions.assertEquals(3, Numbers.log2(6, RoundingMode.HALF_UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1, RoundingMode.DOWN));
    }

    @Test
    public void testLog2Long() {
        Assertions.assertEquals(0, Numbers.log2(1L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log2(2L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(32, Numbers.log2(1L << 32, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(62, Numbers.log2(1L << 62, RoundingMode.UNNECESSARY));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.log2(3L, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(1, Numbers.log2(3L, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(5L, RoundingMode.DOWN));
        Assertions.assertEquals(32, Numbers.log2((1L << 32) + 1, RoundingMode.DOWN));

        Assertions.assertEquals(2, Numbers.log2(3L, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(5L, RoundingMode.UP));
        Assertions.assertEquals(33, Numbers.log2((1L << 32) + 1, RoundingMode.UP));

        Assertions.assertEquals(2, Numbers.log2(3L, RoundingMode.HALF_UP));
        Assertions.assertEquals(3, Numbers.log2(6L, RoundingMode.HALF_UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0L, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1L, RoundingMode.DOWN));
    }

    @Test
    public void testLog2Double() {
        Assertions.assertEquals(0.0, Numbers.log2(1.0), 0.000001);
        Assertions.assertEquals(1.0, Numbers.log2(2.0), 0.000001);
        Assertions.assertEquals(2.0, Numbers.log2(4.0), 0.000001);
        Assertions.assertEquals(3.0, Numbers.log2(8.0), 0.000001);
        Assertions.assertEquals(-1.0, Numbers.log2(0.5), 0.000001);
        Assertions.assertEquals(-2.0, Numbers.log2(0.25), 0.000001);

        Assertions.assertEquals(1.584962, Numbers.log2(3.0), 0.000001);
        Assertions.assertEquals(2.321928, Numbers.log2(5.0), 0.000001);

        Assertions.assertTrue(Double.isNaN(Numbers.log2(-1.0)));
        Assertions.assertTrue(Double.isNaN(Numbers.log2(Double.NaN)));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Numbers.log2(0.0), 0);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Numbers.log2(Double.POSITIVE_INFINITY), 0);
    }

    @Test
    public void testLog2DoubleWithRoundingMode() {
        Assertions.assertEquals(0, Numbers.log2(1.0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log2(2.0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.log2(4.0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(-1, Numbers.log2(0.5, RoundingMode.UNNECESSARY));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.log2(3.0, RoundingMode.UNNECESSARY));

        Assertions.assertEquals(1, Numbers.log2(3.0, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.log2(5.0, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.log2(7.9, RoundingMode.FLOOR));

        Assertions.assertEquals(2, Numbers.log2(3.0, RoundingMode.CEILING));
        Assertions.assertEquals(3, Numbers.log2(5.0, RoundingMode.CEILING));
        Assertions.assertEquals(3, Numbers.log2(4.1, RoundingMode.CEILING));

        Assertions.assertEquals(1, Numbers.log2(3.0, RoundingMode.DOWN));
        Assertions.assertEquals(-1, Numbers.log2(0.3, RoundingMode.DOWN));

        Assertions.assertEquals(2, Numbers.log2(3.0, RoundingMode.UP));
        Assertions.assertEquals(-2, Numbers.log2(0.3, RoundingMode.UP));

        Assertions.assertEquals(2, Numbers.log2(3.0, RoundingMode.HALF_UP));
        Assertions.assertEquals(3, Numbers.log2(6.0, RoundingMode.HALF_UP));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1.0, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.NaN, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
    }

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

    @Test
    @DisplayName("toByte with invalid input should throw NumberFormatException")
    public void testToByteInvalid() {
        assertThrows(NumberFormatException.class, () -> toByte("abc"));
        assertThrows(NumberFormatException.class, () -> toByte("128"));
    }

    @Test
    @DisplayName("toInt with cached values")
    public void testToIntCached() {
        assertEquals(1, toInt("1"));
        assertEquals(-1, toInt("-1"));
        assertEquals(100, toInt("100"));
    }

    @Test
    @DisplayName("toFloat with special values")
    public void testToFloatSpecialValues() {
        assertEquals(Float.POSITIVE_INFINITY, toFloat("Infinity"));
        assertEquals(Float.NEGATIVE_INFINITY, toFloat("-Infinity"));
        assertTrue(Float.isNaN(toFloat("NaN")));
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

    @Test
    @DisplayName("isConvertibleToNumber should handle null and empty")
    public void testIsCreatableNullEmpty() {
        assertFalse(isConvertibleToNumber(null));
        assertFalse(isConvertibleToNumber(""));
        assertFalse(isConvertibleToNumber("   "));
    }

    @Test
    @DisplayName("isPrime should handle edge cases")
    public void testIsPrimeEdgeCases() {
        assertFalse(isPrime(0));
        assertFalse(isPrime(1));
        assertThrows(IllegalArgumentException.class, () -> isPrime(-1));
    }

    @Test
    @DisplayName("isPowerOfTwo with BigInteger")
    public void testIsPowerOfTwoBigInteger() {
        assertTrue(isPowerOfTwo(BigInteger.valueOf(1024)));
        assertFalse(isPowerOfTwo(BigInteger.valueOf(1023)));
        assertThrows(IllegalArgumentException.class, () -> isPowerOfTwo((BigInteger) null));
    }

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
    @DisplayName("roundToInt should handle overflow")
    public void testRoundToIntOverflow() {
        assertThrows(ArithmeticException.class, () -> roundToInt(Double.MAX_VALUE, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> roundToInt(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> roundToInt(Double.NaN, RoundingMode.DOWN));
    }

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
    @DisplayName("factorial should handle large values")
    public void testFactorialLarge() {
        assertEquals(Integer.MAX_VALUE, factorial(20));
        assertThrows(IllegalArgumentException.class, () -> factorial(-1));
    }

    @Test
    @DisplayName("binomial should handle edge cases")
    public void testBinomialEdgeCases() {
        assertThrows(IllegalArgumentException.class, () -> binomial(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> binomial(5, -1));
        assertThrows(IllegalArgumentException.class, () -> binomial(3, 5));
    }

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

    @Test
    @DisplayName("ceilingPowerOfTwo should handle edge cases")
    public void testCeilingPowerOfTwoEdgeCases() {
        assertThrows(IllegalArgumentException.class, () -> ceilingPowerOfTwo(0));
        assertThrows(ArithmeticException.class, () -> ceilingPowerOfTwo(Long.MAX_VALUE));
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

        assertFalse(isConvertibleToNumber(""));
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
    public void testConvert_withTypeAndDefaultValue() {
        Type<Integer> intType = Type.of(Integer.class);
        Type<Long> longType = Type.of(Long.class);

        // Null value should return the specified default value
        Assertions.assertEquals(Integer.valueOf(42), Numbers.convert(null, intType, 42));
        Assertions.assertEquals(Long.valueOf(99L), Numbers.convert(null, longType, 99L));

        // Non-null value should convert normally, ignoring default
        Assertions.assertEquals(Integer.valueOf(100), Numbers.convert(100L, intType, 42));
        Assertions.assertEquals(Long.valueOf(200L), Numbers.convert(200, longType, 99L));
    }

    @Test
    public void testIsConvertibleToNumber_whitespace() {
        Assertions.assertFalse(Numbers.isConvertibleToNumber(" "));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("\t"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("  123  "));
    }

    @Test
    public void testIsConvertibleToNumber_signs() {
        Assertions.assertTrue(Numbers.isConvertibleToNumber("+123"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("-123"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("++123"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("+-123"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("+"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("-"));
    }

    @Test
    public void testIsConvertibleToNumber_hexEdgeCases() {
        Assertions.assertTrue(Numbers.isConvertibleToNumber("0x0"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("0X0"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("-0xABCDEF"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("0x"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("0xG"));
    }

    @Test
    public void testIsConvertibleToNumber_octalEdgeCases() {
        Assertions.assertTrue(Numbers.isConvertibleToNumber("07"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("00"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("08"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("09"));
    }

    @Test
    public void testIsConvertibleToNumber_scientificNotation() {
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1e10"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1E10"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.5e-3"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.5E+3"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("1e"));
        Assertions.assertFalse(Numbers.isConvertibleToNumber("e10"));
    }

    @Test
    public void testIsConvertibleToNumber_typeQualifiers() {
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123L"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("123l"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.0f"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.0F"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.0d"));
        Assertions.assertTrue(Numbers.isConvertibleToNumber("1.0D"));
    }

    // ==================== Additional Tests for Missing Coverage ====================

    @Test
    public void testExtractFirstDouble_OptionalWithScientific() {
        // extractFirstDouble(String, boolean) overload
        u.OptionalDouble result = Numbers.extractFirstDouble("value 1.23e10 end", true);
        assertTrue(result.isPresent());
        assertEquals(1.23e10, result.getAsDouble(), DELTA);

        // Without scientific - should only extract the mantissa portion
        u.OptionalDouble result2 = Numbers.extractFirstDouble("value 1.23e10 end", false);
        assertTrue(result2.isPresent());
        assertEquals(1.23, result2.getAsDouble(), DELTA);

        // null/empty
        assertFalse(Numbers.extractFirstDouble(null, true).isPresent());
        assertFalse(Numbers.extractFirstDouble("", true).isPresent());
        assertFalse(Numbers.extractFirstDouble("no number here", true).isPresent());
    }

    @Test
    public void testExtractFirstDouble_DefaultWithScientific() {
        // extractFirstDouble(String, double, boolean) overload
        assertEquals(1.23e10, Numbers.extractFirstDouble("val 1.23e10 end", 0.0, true), DELTA);
        assertEquals(1.23, Numbers.extractFirstDouble("val 1.23e10 end", 0.0, false), DELTA);

        // default value when no match
        assertEquals(-99.0, Numbers.extractFirstDouble("no numbers", -99.0, true), DELTA);
        assertEquals(-99.0, Numbers.extractFirstDouble(null, -99.0, true), DELTA);
        assertEquals(-99.0, Numbers.extractFirstDouble("", -99.0, false), DELTA);
    }

    @Test
    public void testExtractFirstDouble_NegativeScientific() {
        u.OptionalDouble result = Numbers.extractFirstDouble("temp -3.14e-2 celsius", true);
        assertTrue(result.isPresent());
        assertEquals(-3.14e-2, result.getAsDouble(), DELTA);
    }

    @Test
    public void testConvert_BigDecimalToAllTypes() {
        BigDecimal bd = new BigDecimal("42");
        assertEquals(Byte.valueOf((byte) 42), Numbers.convert(bd, Byte.class));
        assertEquals(Short.valueOf((short) 42), Numbers.convert(bd, Short.class));
        assertEquals(Integer.valueOf(42), Numbers.convert(bd, Integer.class));
        assertEquals(Long.valueOf(42L), Numbers.convert(bd, Long.class));
        assertEquals(Float.valueOf(42.0f), Numbers.convert(bd, Float.class));
        assertEquals(Double.valueOf(42.0), Numbers.convert(bd, Double.class));
        assertEquals(new BigInteger("42"), Numbers.convert(bd, BigInteger.class));
    }

    @Test
    public void testConvert_BigIntegerToAllTypes() {
        BigInteger bi = new BigInteger("100");
        assertEquals(Byte.valueOf((byte) 100), Numbers.convert(bi, Byte.class));
        assertEquals(Short.valueOf((short) 100), Numbers.convert(bi, Short.class));
        assertEquals(Integer.valueOf(100), Numbers.convert(bi, Integer.class));
        assertEquals(Long.valueOf(100L), Numbers.convert(bi, Long.class));
        assertEquals(Float.valueOf(100.0f), Numbers.convert(bi, Float.class));
        assertEquals(Double.valueOf(100.0), Numbers.convert(bi, Double.class));
        assertEquals(new BigDecimal("100"), Numbers.convert(bi, BigDecimal.class));
    }

    @Test
    public void testConvert_WithClassAndDefaultValue() {
        assertEquals(Integer.valueOf(42), Numbers.convert(null, Integer.class, 42));
        assertEquals(Long.valueOf(99L), Numbers.convert(null, Long.class, 99L));
        // non-null should ignore default
        assertEquals(Integer.valueOf(10), Numbers.convert(10L, Integer.class, 42));
    }

    @Test
    public void testIsPerfectSquare_IntEdgeCases() {
        assertTrue(Numbers.isPerfectSquare(0));
        assertTrue(Numbers.isPerfectSquare(1));
        assertTrue(Numbers.isPerfectSquare(4));
        assertTrue(Numbers.isPerfectSquare(9));
        assertTrue(Numbers.isPerfectSquare(100));
        assertTrue(Numbers.isPerfectSquare(Integer.MAX_VALUE - Integer.MAX_VALUE % ((int) Math.sqrt(Integer.MAX_VALUE)) == 0 ? 1 : 10000));
        assertFalse(Numbers.isPerfectSquare(-1));
        assertFalse(Numbers.isPerfectSquare(-100));
        assertFalse(Numbers.isPerfectSquare(2));
        assertFalse(Numbers.isPerfectSquare(3));
        assertFalse(Numbers.isPerfectSquare(5));
        assertFalse(Numbers.isPerfectSquare(Integer.MAX_VALUE));
    }

    @Test
    public void testIsPerfectSquare_LongEdgeCases() {
        assertTrue(Numbers.isPerfectSquare(0L));
        assertTrue(Numbers.isPerfectSquare(1L));
        assertTrue(Numbers.isPerfectSquare(1000000L)); // 1000^2
        assertTrue(Numbers.isPerfectSquare(10000000000L)); // 100000^2
        assertFalse(Numbers.isPerfectSquare(-1L));
        assertFalse(Numbers.isPerfectSquare(Long.MAX_VALUE));
        assertFalse(Numbers.isPerfectSquare(2L));
        assertFalse(Numbers.isPerfectSquare(Long.MIN_VALUE));
    }

    @Test
    public void testLog2_IntAllRoundingModes() {
        // Exact power of 2
        assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(8, RoundingMode.DOWN));
        assertEquals(3, Numbers.log2(8, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log2(8, RoundingMode.UP));
        assertEquals(3, Numbers.log2(8, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(8, RoundingMode.HALF_UP));
        assertEquals(3, Numbers.log2(8, RoundingMode.HALF_DOWN));
        assertEquals(3, Numbers.log2(8, RoundingMode.HALF_EVEN));

        // Non-power of 2
        assertEquals(3, Numbers.log2(10, RoundingMode.DOWN));
        assertEquals(3, Numbers.log2(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10, RoundingMode.UP));
        assertEquals(4, Numbers.log2(10, RoundingMode.CEILING));
    }

    @Test
    public void testLog2_IntUnnecessaryThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.log2(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog2_IntNonPositiveThrows() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1, RoundingMode.DOWN));
    }

    @Test
    public void testLog10_IntAllRoundingModes() {
        assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(100, RoundingMode.DOWN));
        assertEquals(2, Numbers.log10(200, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(200, RoundingMode.UP));
    }

    @Test
    public void testLog10_IntUnnecessaryThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.log10(200, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testDivide_IntAllRoundingModes() {
        assertEquals(3, Numbers.divide(7, 2, RoundingMode.DOWN));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.UP));
        assertEquals(3, Numbers.divide(7, 2, RoundingMode.FLOOR));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.CEILING));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.HALF_UP));

        // Negative division
        assertEquals(-3, Numbers.divide(-7, 2, RoundingMode.DOWN));
        assertEquals(-4, Numbers.divide(-7, 2, RoundingMode.UP));
        assertEquals(-4, Numbers.divide(-7, 2, RoundingMode.FLOOR));
        assertEquals(-3, Numbers.divide(-7, 2, RoundingMode.CEILING));
    }

    @Test
    public void testDivide_IntByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.divide(10, 0, RoundingMode.DOWN));
    }

    @Test
    public void testDivide_LongAllRoundingModes() {
        assertEquals(3L, Numbers.divide(7L, 2L, RoundingMode.DOWN));
        assertEquals(4L, Numbers.divide(7L, 2L, RoundingMode.UP));
        assertEquals(3L, Numbers.divide(7L, 2L, RoundingMode.FLOOR));
        assertEquals(4L, Numbers.divide(7L, 2L, RoundingMode.CEILING));
    }

    @Test
    public void testDivide_BigIntegerAllRoundingModes() {
        BigInteger seven = BigInteger.valueOf(7);
        BigInteger two = BigInteger.valueOf(2);
        assertEquals(BigInteger.valueOf(3), Numbers.divide(seven, two, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(seven, two, RoundingMode.UP));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(seven, two, RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(seven, two, RoundingMode.CEILING));
    }

    @Test
    public void testRoundToLong_AllModes() {
        assertEquals(6L, Numbers.roundToLong(5.5, RoundingMode.UP));
        assertEquals(5L, Numbers.roundToLong(5.5, RoundingMode.DOWN));
        assertEquals(6L, Numbers.roundToLong(5.5, RoundingMode.HALF_UP));
        assertEquals(5L, Numbers.roundToLong(5.0, RoundingMode.UNNECESSARY));
        assertEquals(-6L, Numbers.roundToLong(-5.5, RoundingMode.UP));
        assertEquals(-5L, Numbers.roundToLong(-5.5, RoundingMode.DOWN));
    }

    @Test
    public void testRoundToLong_InfinityThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.NaN, RoundingMode.DOWN));
    }

    @Test
    public void testRoundToBigInteger_AllModes() {
        assertEquals(BigInteger.valueOf(6), Numbers.roundToBigInteger(5.5, RoundingMode.UP));
        assertEquals(BigInteger.valueOf(5), Numbers.roundToBigInteger(5.5, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(6), Numbers.roundToBigInteger(5.5, RoundingMode.HALF_UP));
        assertEquals(BigInteger.valueOf(-6), Numbers.roundToBigInteger(-5.5, RoundingMode.UP));
    }

    @Test
    public void testRoundToBigInteger_LargeValue() {
        BigInteger result = Numbers.roundToBigInteger(1e20, RoundingMode.DOWN);
        assertNotNull(result);
        assertTrue(result.compareTo(BigInteger.ZERO) > 0);
    }

    @Test
    public void testRoundToBigInteger_InfinityThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.roundToBigInteger(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToBigInteger(Double.NaN, RoundingMode.DOWN));
    }

    @Test
    public void testSaturatedAdd_IntOverflow() {
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, -1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, Integer.MIN_VALUE));
        // normal addition still works
        assertEquals(5, Numbers.saturatedAdd(2, 3));
        assertEquals(-5, Numbers.saturatedAdd(-2, -3));
    }

    @Test
    public void testSaturatedAdd_LongOverflow() {
        assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedAdd(Long.MIN_VALUE, -1L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedAdd(Long.MIN_VALUE, Long.MIN_VALUE));
        assertEquals(5L, Numbers.saturatedAdd(2L, 3L));
    }

    @Test
    public void testSaturatedSubtract_IntOverflow() {
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedSubtract(Integer.MAX_VALUE, -1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedSubtract(Integer.MIN_VALUE, 1));
        assertEquals(0, Numbers.saturatedSubtract(5, 5));
    }

    @Test
    public void testSaturatedSubtract_LongOverflow() {
        assertEquals(Long.MAX_VALUE, Numbers.saturatedSubtract(Long.MAX_VALUE, -1L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedSubtract(Long.MIN_VALUE, 1L));
        assertEquals(0L, Numbers.saturatedSubtract(5L, 5L));
    }

    @Test
    public void testSaturatedMultiply_IntOverflow() {
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, -2));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MIN_VALUE, 2));
        assertEquals(6, Numbers.saturatedMultiply(2, 3));
        assertEquals(0, Numbers.saturatedMultiply(0, Integer.MAX_VALUE));
    }

    @Test
    public void testSaturatedMultiply_LongOverflow() {
        assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, 2L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, -2L));
        assertEquals(6L, Numbers.saturatedMultiply(2L, 3L));
        assertEquals(0L, Numbers.saturatedMultiply(0L, Long.MAX_VALUE));
    }

    @Test
    public void testGcd_IntEdgeCases() {
        assertEquals(5, Numbers.gcd(0, 5));
        assertEquals(5, Numbers.gcd(5, 0));
        assertEquals(0, Numbers.gcd(0, 0));
        assertEquals(1, Numbers.gcd(1, 1));
        assertEquals(6, Numbers.gcd(12, 18));
        assertEquals(1, Numbers.gcd(13, 17)); // primes
        assertEquals(1, Numbers.gcd(-1, 1));
    }

    @Test
    public void testGcd_LongEdgeCases() {
        assertEquals(5L, Numbers.gcd(0L, 5L));
        assertEquals(5L, Numbers.gcd(5L, 0L));
        assertEquals(0L, Numbers.gcd(0L, 0L));
        assertEquals(6L, Numbers.gcd(12L, 18L));
        assertEquals(1L, Numbers.gcd(13L, 17L));
    }

    @Test
    public void testLcm_IntBasic() {
        assertEquals(0, Numbers.lcm(0, 5));
        assertEquals(0, Numbers.lcm(5, 0));
        assertEquals(12, Numbers.lcm(4, 6));
        assertEquals(36, Numbers.lcm(12, 18));
        assertEquals(1, Numbers.lcm(1, 1));
    }

    @Test
    public void testLcm_LongBasic() {
        assertEquals(0L, Numbers.lcm(0L, 5L));
        assertEquals(12L, Numbers.lcm(4L, 6L));
        assertEquals(36L, Numbers.lcm(12L, 18L));
    }

    @Test
    public void testBinomial_EdgeCases() {
        assertEquals(1, Numbers.binomial(0, 0));
        assertEquals(1, Numbers.binomial(5, 0));
        assertEquals(1, Numbers.binomial(5, 5));
        assertEquals(5, Numbers.binomial(5, 1));
        assertEquals(10, Numbers.binomial(5, 2));
    }

    @Test
    public void testBinomialToLong_EdgeCases() {
        assertEquals(1L, Numbers.binomialToLong(0, 0));
        assertEquals(1L, Numbers.binomialToLong(10, 0));
        assertEquals(1L, Numbers.binomialToLong(10, 10));
        assertEquals(252L, Numbers.binomialToLong(10, 5));
    }

    @Test
    public void testBinomialToBigInteger_LargeValues() {
        assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(0, 0));
        assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(100, 0));
        assertEquals(BigInteger.valueOf(100), Numbers.binomialToBigInteger(100, 1));
        // C(20,10) = 184756
        assertEquals(BigInteger.valueOf(184756), Numbers.binomialToBigInteger(20, 10));
    }

    @Test
    public void testFloorPowerOfTwo_LongEdgeCases() {
        assertEquals(1L, Numbers.floorPowerOfTwo(1L));
        assertEquals(2L, Numbers.floorPowerOfTwo(2L));
        assertEquals(2L, Numbers.floorPowerOfTwo(3L));
        assertEquals(4L, Numbers.floorPowerOfTwo(4L));
        assertEquals(4L, Numbers.floorPowerOfTwo(5L));
        assertEquals(8L, Numbers.floorPowerOfTwo(15L));
        assertEquals(16L, Numbers.floorPowerOfTwo(16L));
    }

    @Test
    public void testFloorPowerOfTwo_BigIntegerEdgeCases() {
        assertEquals(BigInteger.ONE, Numbers.floorPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.valueOf(2), Numbers.floorPowerOfTwo(BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(2), Numbers.floorPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(4), Numbers.floorPowerOfTwo(BigInteger.valueOf(5)));
    }

    @Test
    public void testCeilingPowerOfTwo_LongEdgeCases() {
        assertEquals(1L, Numbers.ceilingPowerOfTwo(1L));
        assertEquals(2L, Numbers.ceilingPowerOfTwo(2L));
        assertEquals(4L, Numbers.ceilingPowerOfTwo(3L));
        assertEquals(4L, Numbers.ceilingPowerOfTwo(4L));
        assertEquals(8L, Numbers.ceilingPowerOfTwo(5L));
        assertEquals(16L, Numbers.ceilingPowerOfTwo(16L));
    }

    @Test
    public void testCeilingPowerOfTwo_BigIntegerEdgeCases() {
        assertEquals(BigInteger.ONE, Numbers.ceilingPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.valueOf(2), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(4), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(8), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(5)));
    }

    @Test
    public void testSqrt_IntAllRoundingModes() {
        assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.DOWN));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.UP));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.CEILING));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.HALF_DOWN));
    }

    @Test
    public void testSqrt_IntUnnecessaryThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testSqrt_LongAllRoundingModes() {
        assertEquals(3L, Numbers.sqrt(9L, RoundingMode.UNNECESSARY));
        assertEquals(3L, Numbers.sqrt(10L, RoundingMode.DOWN));
        assertEquals(4L, Numbers.sqrt(10L, RoundingMode.UP));
    }

    @Test
    public void testSqrt_BigIntegerAllRoundingModes() {
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(9), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.UP));
    }

    @Test
    public void testMod_IntEdgeCases() {
        assertEquals(1, Numbers.mod(7, 3));
        assertEquals(2, Numbers.mod(-1, 3));
        assertEquals(0, Numbers.mod(6, 3));
    }

    @Test
    public void testMod_LongInt_EdgeCases() {
        assertEquals(1, Numbers.mod(7L, 3));
        assertEquals(2, Numbers.mod(-1L, 3));
    }

    @Test
    public void testMod_LongLong_EdgeCases() {
        assertEquals(1L, Numbers.mod(7L, 3L));
        assertEquals(2L, Numbers.mod(-1L, 3L));
        assertEquals(0L, Numbers.mod(6L, 3L));
    }

    @Test
    public void testFactorial_EdgeCases() {
        assertEquals(1, Numbers.factorial(0));
        assertEquals(1, Numbers.factorial(1));
        assertEquals(2, Numbers.factorial(2));
        assertEquals(6, Numbers.factorial(3));
        assertEquals(24, Numbers.factorial(4));
        assertEquals(120, Numbers.factorial(5));
    }

    @Test
    public void testFactorial_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorial(-1));
    }

    @Test
    public void testFactorialToLong_EdgeCases() {
        assertEquals(1L, Numbers.factorialToLong(0));
        assertEquals(1L, Numbers.factorialToLong(1));
        assertEquals(120L, Numbers.factorialToLong(5));
        assertEquals(3628800L, Numbers.factorialToLong(10));
    }

    @Test
    public void testFactorialToDouble_EdgeCases() {
        assertEquals(1.0, Numbers.factorialToDouble(0), DELTA);
        assertEquals(1.0, Numbers.factorialToDouble(1), DELTA);
        assertEquals(120.0, Numbers.factorialToDouble(5), DELTA);
        // Large factorials
        assertTrue(Numbers.factorialToDouble(170) > 0);
    }

    @Test
    public void testFactorialToBigInteger_EdgeCases() {
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(0));
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(1));
        assertEquals(BigInteger.valueOf(120), Numbers.factorialToBigInteger(5));
        assertEquals(BigInteger.valueOf(3628800), Numbers.factorialToBigInteger(10));
        // Large factorial
        assertNotNull(Numbers.factorialToBigInteger(50));
        assertTrue(Numbers.factorialToBigInteger(50).compareTo(BigInteger.ZERO) > 0);
    }

    @Test
    public void testPow_IntEdgeCases() {
        assertEquals(1, Numbers.pow(2, 0));
        assertEquals(2, Numbers.pow(2, 1));
        assertEquals(1024, Numbers.pow(2, 10));
        assertEquals(1, Numbers.pow(1, 100));
        assertEquals(0, Numbers.pow(0, 5));
    }

    @Test
    public void testPow_LongEdgeCases() {
        assertEquals(1L, Numbers.pow(2L, 0));
        assertEquals(2L, Numbers.pow(2L, 1));
        assertEquals(1024L, Numbers.pow(2L, 10));
        assertEquals(1L, Numbers.pow(1L, 100));
    }

    @Test
    public void testPowExact_IntOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(2, 31));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MAX_VALUE, 2));
    }

    @Test
    public void testPowExact_LongOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(2L, 63));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Long.MAX_VALUE, 2));
    }

    @Test
    public void testSaturatedPow_IntEdgeCases() {
        assertEquals(1, Numbers.saturatedPow(2, 0));
        assertEquals(8, Numbers.saturatedPow(2, 3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(2, 31));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(Integer.MAX_VALUE, 2));
    }

    @Test
    public void testSaturatedPow_LongEdgeCases() {
        assertEquals(1L, Numbers.saturatedPow(2L, 0));
        assertEquals(8L, Numbers.saturatedPow(2L, 3));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(2L, 63));
    }

    @Test
    public void testSaturatedCast_EdgeCases() {
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCast(Long.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCast(Long.MIN_VALUE));
        assertEquals(0, Numbers.saturatedCast(0L));
        assertEquals(42, Numbers.saturatedCast(42L));
        assertEquals(-42, Numbers.saturatedCast(-42L));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCast((long) Integer.MAX_VALUE + 1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCast((long) Integer.MIN_VALUE - 1));
    }

    @Test
    public void testAddExact_IntOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MIN_VALUE, -1));
        assertEquals(0, Numbers.addExact(-1, 1));
    }

    @Test
    public void testAddExact_LongOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MAX_VALUE, 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MIN_VALUE, -1L));
        assertEquals(0L, Numbers.addExact(-1L, 1L));
    }

    @Test
    public void testSubtractExact_IntOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MAX_VALUE, -1));
        assertEquals(0, Numbers.subtractExact(1, 1));
    }

    @Test
    public void testSubtractExact_LongOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MIN_VALUE, 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MAX_VALUE, -1L));
        assertEquals(0L, Numbers.subtractExact(1L, 1L));
    }

    @Test
    public void testMultiplyExact_IntOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MAX_VALUE, 2));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MIN_VALUE, 2));
        assertEquals(0, Numbers.multiplyExact(0, Integer.MAX_VALUE));
    }

    @Test
    public void testMultiplyExact_LongOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Long.MAX_VALUE, 2L));
        assertEquals(0L, Numbers.multiplyExact(0L, Long.MAX_VALUE));
    }

    @Test
    public void testIsMathematicalInteger_EdgeCases() {
        assertTrue(Numbers.isMathematicalInteger(0.0));
        assertTrue(Numbers.isMathematicalInteger(-0.0));
        assertTrue(Numbers.isMathematicalInteger(1.0));
        assertTrue(Numbers.isMathematicalInteger(-1.0));
        assertTrue(Numbers.isMathematicalInteger(1e10));
        assertFalse(Numbers.isMathematicalInteger(Double.POSITIVE_INFINITY));
        assertFalse(Numbers.isMathematicalInteger(Double.NEGATIVE_INFINITY));
        assertFalse(Numbers.isMathematicalInteger(0.5));
        assertFalse(Numbers.isMathematicalInteger(Double.NaN));
        assertFalse(Numbers.isMathematicalInteger(Math.PI));
    }

    @Test
    public void testFuzzyEquals_FloatEdgeCases() {
        assertTrue(Numbers.fuzzyEquals(0.0f, 0.0f, 0.0f));
        assertTrue(Numbers.fuzzyEquals(Float.NaN, Float.NaN, 0.0f));
        assertTrue(Numbers.fuzzyEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.0f));
        assertFalse(Numbers.fuzzyEquals(Float.NaN, 0.0f, 1.0f));
        assertFalse(Numbers.fuzzyEquals(1.0f, 2.0f, 0.5f));
        assertTrue(Numbers.fuzzyEquals(1.0f, 1.5f, 0.5f));
    }

    @Test
    public void testFuzzyEquals_DoubleEdgeCases() {
        assertTrue(Numbers.fuzzyEquals(0.0, 0.0, 0.0));
        assertTrue(Numbers.fuzzyEquals(Double.NaN, Double.NaN, 0.0));
        assertTrue(Numbers.fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0));
        assertFalse(Numbers.fuzzyEquals(Double.NaN, 0.0, 1.0));
        assertFalse(Numbers.fuzzyEquals(1.0, 2.0, 0.5));
        assertTrue(Numbers.fuzzyEquals(1.0, 1.5, 0.5));
    }

    @Test
    public void testFuzzyCompare_FloatEdgeCases() {
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0f, 0.0f));
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.05f, 0.1f));
        assertTrue(Numbers.fuzzyCompare(1.0f, 2.0f, 0.0f) < 0);
        assertTrue(Numbers.fuzzyCompare(2.0f, 1.0f, 0.0f) > 0);
    }

    @Test
    public void testFuzzyCompare_DoubleEdgeCases() {
        assertEquals(0, Numbers.fuzzyCompare(1.0, 1.0, 0.0));
        assertEquals(0, Numbers.fuzzyCompare(1.0, 1.05, 0.1));
        assertTrue(Numbers.fuzzyCompare(1.0, 2.0, 0.0) < 0);
        assertTrue(Numbers.fuzzyCompare(2.0, 1.0, 0.0) > 0);
    }

    @Test
    public void testAsinh_EdgeCases() {
        assertEquals(0.0, Numbers.asinh(0.0), DELTA);
        assertTrue(Numbers.asinh(1.0) > 0);
        assertTrue(Numbers.asinh(-1.0) < 0);
        // asinh(-x) == -asinh(x)
        assertEquals(-Numbers.asinh(5.0), Numbers.asinh(-5.0), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.asinh(Double.POSITIVE_INFINITY), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.asinh(Double.NEGATIVE_INFINITY), DELTA);
    }

    @Test
    public void testAcosh_EdgeCases() {
        assertEquals(0.0, Numbers.acosh(1.0), DELTA);
        assertTrue(Numbers.acosh(2.0) > 0);
        assertTrue(Double.isNaN(Numbers.acosh(0.5))); // acosh undefined for x < 1
        assertEquals(Double.POSITIVE_INFINITY, Numbers.acosh(Double.POSITIVE_INFINITY), DELTA);
    }

    @Test
    public void testAtanh_EdgeCases() {
        assertEquals(0.0, Numbers.atanh(0.0), DELTA);
        assertTrue(Numbers.atanh(0.5) > 0);
        assertTrue(Numbers.atanh(-0.5) < 0);
        // atanh(-x) == -atanh(x)
        assertEquals(-Numbers.atanh(0.5), Numbers.atanh(-0.5), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.atanh(1.0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.atanh(-1.0), DELTA);
    }

    @Test
    public void testToIntExact_EdgeCases() {
        assertEquals(Integer.MAX_VALUE, Numbers.toIntExact(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.toIntExact(Integer.MIN_VALUE));
        assertEquals(0, Numbers.toIntExact(0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MAX_VALUE + 1));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MIN_VALUE - 1));
    }

    @Test
    public void testMean_IntPair_EdgeCases() {
        assertEquals(0, Numbers.mean(0, 0));
        assertEquals(Integer.MAX_VALUE, Numbers.mean(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.mean(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(-1, Numbers.mean(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testMean_LongPair_EdgeCases() {
        assertEquals(0L, Numbers.mean(0L, 0L));
        assertEquals(Long.MAX_VALUE, Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, Numbers.mean(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testMean_DoublePair_SpecialValues() {
        // mean(double, double) throws for non-finite values
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.NaN, 1.0));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testMean_IntArray_EdgeCases() {
        assertEquals(5.0, Numbers.mean(new int[] { 5 }), DELTA);
        assertEquals(2.0, Numbers.mean(new int[] { 1, 2, 3 }), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new int[] {}));
    }

    @Test
    public void testMean_LongArray_EdgeCases() {
        assertEquals(5.0, Numbers.mean(new long[] { 5L }), DELTA);
        assertEquals(2.0, Numbers.mean(new long[] { 1L, 2L, 3L }), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new long[] {}));
    }

    @Test
    public void testMean_DoubleArray_EdgeCases() {
        assertEquals(5.0, Numbers.mean(new double[] { 5.0 }), DELTA);
        assertEquals(2.0, Numbers.mean(new double[] { 1.0, 2.0, 3.0 }), DELTA);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new double[] {}));
    }

    @Test
    public void testRound_FloatWithDecimalFormat_Null() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(1.5f, (DecimalFormat) null));
    }

    @Test
    public void testRound_DoubleWithDecimalFormat_Null() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(1.5, (DecimalFormat) null));
    }

    @Test
    public void testCreateNumber_NullAndEmpty() {
        assertNull(Numbers.createNumber(null));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber(""));
    }

    @Test
    public void testCreateNumber_Various() {
        assertEquals(Integer.valueOf(255), Numbers.createNumber("0xFF"));
        assertNotNull(Numbers.createNumber("1.23e10"));
        assertNotNull(Numbers.createNumber("123L"));
        assertNotNull(Numbers.createNumber("1.0f"));
        assertNotNull(Numbers.createNumber("1.0d"));
    }

    @Test
    public void testCreateBigInteger_EdgeCases() {
        assertNull(Numbers.createBigInteger(null));
        assertEquals(BigInteger.ZERO, Numbers.createBigInteger("0"));
        assertEquals(BigInteger.ONE, Numbers.createBigInteger("1"));
        assertEquals(BigInteger.valueOf(-1), Numbers.createBigInteger("-1"));
        // Hex
        assertEquals(BigInteger.valueOf(255), Numbers.createBigInteger("0xFF"));
    }

    @Test
    public void testCreateBigDecimal_EdgeCases() {
        assertNull(Numbers.createBigDecimal(null));
        assertEquals(BigDecimal.ZERO, Numbers.createBigDecimal("0"));
        assertEquals(new BigDecimal("1.23"), Numbers.createBigDecimal("1.23"));
    }

    @Test
    public void testIsPrime_LargePrimes() {
        assertTrue(Numbers.isPrime(2));
        assertTrue(Numbers.isPrime(3));
        assertTrue(Numbers.isPrime(5));
        assertTrue(Numbers.isPrime(7));
        assertTrue(Numbers.isPrime(11));
        assertTrue(Numbers.isPrime(13));
        assertTrue(Numbers.isPrime(97));
        assertTrue(Numbers.isPrime(997));
        assertTrue(Numbers.isPrime(7919));
        assertFalse(Numbers.isPrime(0));
        assertFalse(Numbers.isPrime(1));
        assertFalse(Numbers.isPrime(4));
        assertFalse(Numbers.isPrime(100));
    }

    @Test
    public void testIsPrime_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPrime(-1));
    }

    @Test
    public void testIsDigits_EdgeCases() {
        assertTrue(Numbers.isDigits("0"));
        assertTrue(Numbers.isDigits("123"));
        assertFalse(Numbers.isDigits(""));
        assertFalse(Numbers.isDigits(null));
        assertFalse(Numbers.isDigits("12.3"));
        assertFalse(Numbers.isDigits("-1"));
        assertFalse(Numbers.isDigits("abc"));
    }

    @Test
    public void testIsCreatable_EdgeCases() {
        assertTrue(Numbers.isCreatable("0"));
        assertTrue(Numbers.isCreatable("1"));
        assertTrue(Numbers.isCreatable("-1"));
        assertTrue(Numbers.isCreatable("0xFF"));
        assertTrue(Numbers.isCreatable("1.23e10"));
        assertFalse(Numbers.isCreatable(null));
        assertFalse(Numbers.isCreatable(""));
        assertFalse(Numbers.isCreatable("abc"));
    }

    @Test
    public void testIsParsable_EdgeCases() {
        assertTrue(Numbers.isParsable("0"));
        assertTrue(Numbers.isParsable("1"));
        assertTrue(Numbers.isParsable("-1"));
        assertTrue(Numbers.isParsable("1.23"));
        assertFalse(Numbers.isParsable(null));
        assertFalse(Numbers.isParsable(""));
        assertFalse(Numbers.isParsable("abc"));
        assertFalse(Numbers.isParsable("0xFF"));
    }

    @Test
    public void testToDouble_BigDecimal_Null() {
        assertEquals(0.0, Numbers.toDouble((BigDecimal) null), DELTA);
    }

    @Test
    public void testToDouble_BigDecimal_WithDefault_Null() {
        assertEquals(42.0, Numbers.toDouble((BigDecimal) null, 42.0), DELTA);
        assertEquals(1.23, Numbers.toDouble(new BigDecimal("1.23"), 42.0), DELTA);
    }

    @Test
    public void testToScaledBigDecimal_NullInputs() {
        // toScaledBigDecimal returns BigDecimal.ZERO for null inputs, not null
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((BigDecimal) null));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Float) null));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Double) null));
        assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((String) null));
    }

    @Test
    public void testToScaledBigDecimal_WithCustomScale() {
        BigDecimal result = Numbers.toScaledBigDecimal(new BigDecimal("3.14159"), 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3.14"), result);

        BigDecimal result2 = Numbers.toScaledBigDecimal(3.14159f, 2, RoundingMode.HALF_UP);
        assertNotNull(result2);

        BigDecimal result3 = Numbers.toScaledBigDecimal(3.14159, 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3.14"), result3);

        BigDecimal result4 = Numbers.toScaledBigDecimal("3.14159", 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3.14"), result4);
    }

    @Test
    public void testIsPowerOfTwo_IntEdgeCases() {
        assertFalse(Numbers.isPowerOfTwo(0));
        assertTrue(Numbers.isPowerOfTwo(1));
        assertTrue(Numbers.isPowerOfTwo(2));
        assertTrue(Numbers.isPowerOfTwo(4));
        assertTrue(Numbers.isPowerOfTwo(1024));
        assertFalse(Numbers.isPowerOfTwo(-1));
        assertFalse(Numbers.isPowerOfTwo(-2));
        assertFalse(Numbers.isPowerOfTwo(3));
        assertFalse(Numbers.isPowerOfTwo(Integer.MIN_VALUE));
    }

    @Test
    public void testIsPowerOfTwo_LongEdgeCases() {
        assertFalse(Numbers.isPowerOfTwo(0L));
        assertTrue(Numbers.isPowerOfTwo(1L));
        assertTrue(Numbers.isPowerOfTwo(2L));
        assertTrue(Numbers.isPowerOfTwo(1099511627776L)); // 2^40
        assertFalse(Numbers.isPowerOfTwo(-1L));
        assertFalse(Numbers.isPowerOfTwo(Long.MIN_VALUE));
    }

    @Test
    public void testIsPowerOfTwo_DoubleEdgeCases() {
        assertTrue(Numbers.isPowerOfTwo(1.0));
        assertTrue(Numbers.isPowerOfTwo(2.0));
        assertTrue(Numbers.isPowerOfTwo(0.5));
        assertTrue(Numbers.isPowerOfTwo(0.25));
        assertFalse(Numbers.isPowerOfTwo(0.0));
        assertFalse(Numbers.isPowerOfTwo(-1.0));
        assertFalse(Numbers.isPowerOfTwo(3.0));
        assertFalse(Numbers.isPowerOfTwo(Double.NaN));
        assertFalse(Numbers.isPowerOfTwo(Double.POSITIVE_INFINITY));
    }

    @Test
    public void testIsPowerOfTwo_BigIntegerEdgeCases() {
        assertTrue(Numbers.isPowerOfTwo(BigInteger.ONE));
        assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(2)));
        assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(1024)));
        assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(3)));
        assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(0)));
    }

    @Test
    public void testLog_BasicCases() {
        assertEquals(0.0, Numbers.log(1.0), DELTA);
        assertEquals(Math.log(Math.E), Numbers.log(Math.E), DELTA);
        assertEquals(Math.log(10.0), Numbers.log(10.0), DELTA);
        assertTrue(Double.isNaN(Numbers.log(-1.0)));
        assertTrue(Double.isInfinite(Numbers.log(0.0)));
    }

    @Test
    public void testLog2_Double_BasicCases() {
        assertEquals(0.0, Numbers.log2(1.0), DELTA);
        assertEquals(1.0, Numbers.log2(2.0), DELTA);
        assertEquals(10.0, Numbers.log2(1024.0), DELTA);
        assertTrue(Double.isNaN(Numbers.log2(-1.0)));
    }

    @Test
    public void testLog10_Double_BasicCases() {
        assertEquals(0.0, Numbers.log10(1.0), DELTA);
        assertEquals(1.0, Numbers.log10(10.0), DELTA);
        assertEquals(2.0, Numbers.log10(100.0), DELTA);
    }

    @Test
    public void testRound_FloatWithScale_EdgeCases() {
        assertEquals(1.24f, Numbers.round(1.235f, 2), FLOAT_DELTA);
        assertEquals(0.0f, Numbers.round(0.0f, 5), FLOAT_DELTA);
        assertEquals(-1.23f, Numbers.round(-1.235f, 2), FLOAT_DELTA);
    }

    @Test
    public void testRound_DoubleWithScale_EdgeCases() {
        assertEquals(1.24, Numbers.round(1.235, 2), DELTA);
        assertEquals(0.0, Numbers.round(0.0, 5), DELTA);
        assertEquals(-1.24, Numbers.round(-1.235, 2), DELTA);
    }

    @Test
    public void testFormat_IntWithPattern() {
        assertEquals("1,234", Numbers.format(1234, "#,###"));
        assertEquals("1234.00", Numbers.format(1234, "0.00"));
    }

    @Test
    public void testFormat_LongWithPattern() {
        assertEquals("1,234", Numbers.format(1234L, "#,###"));
    }

    @Test
    public void testFormat_FloatWithPattern() {
        assertEquals("3.14", Numbers.format(3.14f, "0.00"));
    }

    @Test
    public void testFormat_DoubleWithPattern() {
        assertEquals("3.14", Numbers.format(3.14, "0.00"));
    }

    @Test
    public void testToByte_NullAndEmpty() {
        assertEquals((byte) 0, Numbers.toByte((String) null));
        assertEquals((byte) 0, Numbers.toByte(""));
        assertEquals((byte) 42, Numbers.toByte(null, (byte) 42));
        assertEquals((byte) 42, Numbers.toByte("", (byte) 42));
    }

    @Test
    public void testToShort_NullAndEmpty() {
        assertEquals((short) 0, Numbers.toShort((String) null));
        assertEquals((short) 0, Numbers.toShort(""));
        assertEquals((short) 42, Numbers.toShort(null, (short) 42));
        assertEquals((short) 42, Numbers.toShort("", (short) 42));
    }

    @Test
    public void testToInt_NullAndEmpty() {
        assertEquals(0, Numbers.toInt((String) null));
        assertEquals(0, Numbers.toInt(""));
        assertEquals(42, Numbers.toInt(null, 42));
        assertEquals(42, Numbers.toInt("", 42));
    }

    @Test
    public void testToLong_NullAndEmpty() {
        assertEquals(0L, Numbers.toLong((String) null));
        assertEquals(0L, Numbers.toLong(""));
        assertEquals(42L, Numbers.toLong(null, 42L));
        assertEquals(42L, Numbers.toLong("", 42L));
    }

    @Test
    public void testToFloat_NullAndEmpty() {
        assertEquals(0.0f, Numbers.toFloat((String) null), FLOAT_DELTA);
        assertEquals(0.0f, Numbers.toFloat(""), FLOAT_DELTA);
        assertEquals(42.0f, Numbers.toFloat(null, 42.0f), FLOAT_DELTA);
    }

    @Test
    public void testToDouble_NullAndEmpty() {
        assertEquals(0.0, Numbers.toDouble((String) null), DELTA);
        assertEquals(0.0, Numbers.toDouble(""), DELTA);
        assertEquals(42.0, Numbers.toDouble((String) null, 42.0), DELTA);
    }

    @Test
    public void testExtractFirstInt_EdgeCases() {
        assertTrue(Numbers.extractFirstInt("abc 42 def").isPresent());
        assertEquals(42, Numbers.extractFirstInt("abc 42 def").getAsInt());
        assertFalse(Numbers.extractFirstInt("no numbers").isPresent());
        assertFalse(Numbers.extractFirstInt(null).isPresent());
        assertFalse(Numbers.extractFirstInt("").isPresent());
    }

    @Test
    public void testExtractFirstLong_EdgeCases() {
        assertTrue(Numbers.extractFirstLong("abc 42 def").isPresent());
        assertEquals(42L, Numbers.extractFirstLong("abc 42 def").getAsLong());
        assertFalse(Numbers.extractFirstLong("no numbers").isPresent());
        assertFalse(Numbers.extractFirstLong(null).isPresent());
        assertFalse(Numbers.extractFirstLong("").isPresent());
    }

    @Test
    public void testConvert_FloatToBigDecimal() {
        BigDecimal result = Numbers.convert(1.5f, BigDecimal.class);
        assertNotNull(result);
    }

    @Test
    public void testConvert_DoubleToBigInteger() {
        BigInteger result = Numbers.convert(42.0, BigInteger.class);
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    public void testCreateInteger_EdgeCases() {
        assertNull(Numbers.createInteger(null));
        assertEquals(Integer.valueOf(0), Numbers.createInteger("0"));
        assertEquals(Integer.valueOf(-1), Numbers.createInteger("-1"));
        assertEquals(Integer.valueOf(255), Numbers.createInteger("0xFF"));
        assertEquals(Integer.valueOf(8), Numbers.createInteger("010")); // octal
        assertThrows(NumberFormatException.class, () -> Numbers.createInteger("abc"));
    }

    @Test
    public void testCreateLong_EdgeCases() {
        assertNull(Numbers.createLong(null));
        assertEquals(Long.valueOf(0L), Numbers.createLong("0"));
        assertEquals(Long.valueOf(-1L), Numbers.createLong("-1"));
        assertThrows(NumberFormatException.class, () -> Numbers.createLong("abc"));
    }

    @Test
    public void testCreateFloat_EdgeCases() {
        assertNull(Numbers.createFloat(null));
        assertEquals(Float.valueOf(0.0f), Numbers.createFloat("0"));
        assertEquals(Float.valueOf(-1.5f), Numbers.createFloat("-1.5"));
        assertThrows(NumberFormatException.class, () -> Numbers.createFloat("abc"));
    }

    @Test
    public void testCreateDouble_EdgeCases() {
        assertNull(Numbers.createDouble(null));
        assertEquals(Double.valueOf(0.0), Numbers.createDouble("0"));
        assertEquals(Double.valueOf(-1.5), Numbers.createDouble("-1.5"));
        assertThrows(NumberFormatException.class, () -> Numbers.createDouble("abc"));
    }

    @Test
    public void testAsinh_SeriesThresholds() {
        final double[] values = { 0.12d, 0.05d, 0.001d };

        for (final double value : values) {
            assertEquals(Math.log(value + Math.sqrt(value * value + 1)), Numbers.asinh(value), 1e-12);
            assertEquals(-Numbers.asinh(value), Numbers.asinh(-value), 1e-12);
        }
    }

    @Test
    public void testAtanh_SeriesThresholds() {
        final double[] values = { 0.10d, 0.05d, 0.001d };

        for (final double value : values) {
            assertEquals(0.5d * Math.log((1 + value) / (1 - value)), Numbers.atanh(value), 1e-12);
            assertEquals(-Numbers.atanh(value), Numbers.atanh(-value), 1e-12);
        }
    }

    @Test
    public void testFactorialToBigInteger_BeyondLongRange() {
        assertEquals(new BigInteger("51090942171709440000"), Numbers.factorialToBigInteger(21));
    }

    @Test
    public void testBinomialToLong_SymmetricEdgeCase() {
        assertEquals(60L, Numbers.binomialToLong(60, 59));
        assertEquals(60L, Numbers.binomialToLong(60, 1));
    }

    @Test
    public void testAsinh_AllSeriesBranches() {
        final double[] values = { 0.16d, 0.09d, 0.02d, 0.001d };

        for (final double value : values) {
            assertEquals(Math.log(value + Math.sqrt(value * value + 1)), Numbers.asinh(value), 1e-12);
            assertEquals(-Numbers.asinh(value), Numbers.asinh(-value), 1e-12);
        }
    }

    @Test
    public void testAtanh_OutOfDomainAndNaN() {
        final double[] values = { 0.14d, 0.08d, 0.02d, 0.001d };

        for (final double value : values) {
            assertEquals(0.5d * Math.log((1 + value) / (1 - value)), Numbers.atanh(value), 1e-12);
            assertEquals(-Numbers.atanh(value), Numbers.atanh(-value), 1e-12);
        }

        assertTrue(Double.isNaN(Numbers.atanh(Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.atanh(1.0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.atanh(-1.0), DELTA);
        assertTrue(Double.isNaN(Numbers.atanh(1.01d)));
    }

    @Test
    public void testBinomialToLong_OverflowSaturates() {
        assertEquals(Long.MAX_VALUE, Numbers.binomialToLong(68, 34));
        assertEquals(Long.MAX_VALUE, Numbers.binomialToLong(70, 35));
    }

}
