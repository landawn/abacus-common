package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("2025")
public class Numbers2025Test extends TestBase {

    @Test
    public void test_convert_toByte() {
        assertEquals((byte) 10, Numbers.convert(10, byte.class));
        assertEquals((byte) 10, Numbers.convert(10, Byte.class));
        assertEquals((byte) 0, Numbers.convert(null, byte.class));
        assertNull(Numbers.convert(null, Byte.class));

        assertEquals((byte) 127, Numbers.convert(127L, byte.class));
        assertEquals((byte) -128, Numbers.convert(-128L, byte.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(128, byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(-129, byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(200L, Byte.class));
    }

    @Test
    public void test_convert_toShort() {
        assertEquals((short) 100, Numbers.convert(100, short.class));
        assertEquals((short) 100, Numbers.convert(100, Short.class));
        assertEquals((short) 0, Numbers.convert(null, short.class));
        assertNull(Numbers.convert(null, Short.class));

        assertEquals((short) 32767, Numbers.convert(32767L, short.class));
        assertEquals((short) -32768, Numbers.convert(-32768L, short.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(32768, short.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(-32769, short.class));
    }

    @Test
    public void test_convert_toInt() {
        assertEquals(1000, Numbers.convert(1000L, int.class));
        assertEquals(1000, Numbers.convert(1000L, Integer.class));
        assertEquals(0, Numbers.convert(null, int.class));
        assertNull(Numbers.convert(null, Integer.class));

        assertEquals(Integer.MAX_VALUE, Numbers.convert((long) Integer.MAX_VALUE, int.class));
        assertEquals(Integer.MIN_VALUE, Numbers.convert((long) Integer.MIN_VALUE, int.class));

        assertThrows(ArithmeticException.class, () -> Numbers.convert((long) Integer.MAX_VALUE + 1, int.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((long) Integer.MIN_VALUE - 1, int.class));
    }

    @Test
    public void test_convert_toLong() {
        assertEquals(10000L, Numbers.convert(10000, long.class));
        assertEquals(10000L, Numbers.convert(10000, Long.class));
        assertEquals(0L, Numbers.convert(null, long.class));
        assertNull(Numbers.convert(null, Long.class));

        assertEquals(Long.MAX_VALUE, Numbers.convert(new BigInteger(String.valueOf(Long.MAX_VALUE)), long.class));
        assertEquals(Long.MIN_VALUE, Numbers.convert(new BigInteger(String.valueOf(Long.MIN_VALUE)), long.class));

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
        assertEquals((byte) 10, Numbers.convert(10, byte.class, (byte) 5));
        assertEquals((byte) 5, Numbers.convert(null, byte.class, (byte) 5));
        assertEquals((short) 20, Numbers.convert(null, Short.class, (short) 20));
        assertEquals(100, Numbers.convert(null, Integer.class, 100));
        assertEquals(200L, Numbers.convert(null, Long.class, 200L));
    }

    @Test
    public void test_convert_withType() {
        Type<Integer> intType = Type.of(Integer.class);
        assertEquals(100, Numbers.convert(100L, intType));
        assertNull(Numbers.convert(null, intType));

        Type<Long> longType = Type.of(Long.class);
        assertEquals(200L, Numbers.convert(200, longType));

        assertEquals(50L, Numbers.convert(null, longType, 50L));
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
    public void test_isCreatable() {
        assertTrue(Numbers.isCreatable("123"));
        assertTrue(Numbers.isCreatable("-123"));
        assertTrue(Numbers.isCreatable("12.3"));
        assertTrue(Numbers.isCreatable("1.23e10"));
        assertTrue(Numbers.isCreatable("0x1F"));
        assertFalse(Numbers.isCreatable("abc"));
        assertFalse(Numbers.isCreatable(""));
        assertFalse(Numbers.isCreatable(null));
    }

    @Test
    public void test_isParsable() {
        assertTrue(Numbers.isParsable("123"));
        assertTrue(Numbers.isParsable("-123"));
        assertTrue(Numbers.isParsable("12.3"));
        assertFalse(Numbers.isParsable("abc"));
        assertFalse(Numbers.isParsable(""));
        assertFalse(Numbers.isParsable(null));
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
}
