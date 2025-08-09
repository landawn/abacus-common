package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class Numbers102Test extends TestBase {

    @Test
    public void testConvertNumberToTargetType() {
        // Test byte conversions
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10, byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10L, Byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10.0f, byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(10.0d, byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(BigInteger.valueOf(10), byte.class));
        Assertions.assertEquals(Byte.valueOf((byte) 10), Numbers.convert(BigDecimal.valueOf(10), byte.class));
        
        // Test overflow
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(1000, byte.class));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(-1000, byte.class));
        
        // Test short conversions
        Assertions.assertEquals(Short.valueOf((short) 1000), Numbers.convert(1000, short.class));
        Assertions.assertEquals(Short.valueOf((short) 1000), Numbers.convert(1000L, Short.class));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(100000, short.class));
        
        // Test int conversions
        Assertions.assertEquals(Integer.valueOf(100000), Numbers.convert(100000L, int.class));
        Assertions.assertEquals(Integer.valueOf(100000), Numbers.convert(100000.0f, Integer.class));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.convert(10000000000L, int.class));
        
        // Test long conversions
        Assertions.assertEquals(Long.valueOf(10000000000L), Numbers.convert(10000000000L, long.class));
        Assertions.assertEquals(Long.valueOf(10000000000L), Numbers.convert(BigInteger.valueOf(10000000000L), Long.class));
        
        // Test float conversions
        Assertions.assertEquals(Float.valueOf(10.5f), Numbers.convert(10.5d, float.class));
        Assertions.assertEquals(Float.valueOf(10.5f), Numbers.convert(BigDecimal.valueOf(10.5), Float.class));
        
        // Test double conversions
        Assertions.assertEquals(Double.valueOf(10.5d), Numbers.convert(10.5f, double.class));
        Assertions.assertEquals(Double.valueOf(10.5d), Numbers.convert(BigDecimal.valueOf(10.5), Double.class));
        
        // Test BigInteger conversions
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.convert(100, BigInteger.class));
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.convert(100L, BigInteger.class));
        Assertions.assertEquals(BigInteger.valueOf(100), Numbers.convert(100.0d, BigInteger.class));
        
        // Test BigDecimal conversions
        Assertions.assertEquals(BigDecimal.valueOf(100), Numbers.convert(100, BigDecimal.class));
        Assertions.assertEquals(BigDecimal.valueOf(100L), Numbers.convert(100L, BigDecimal.class));
        
        // Test null handling
        Assertions.assertEquals(Byte.valueOf((byte) 0), Numbers.convert(null, byte.class));
        Assertions.assertEquals(null, Numbers.convert(null, Integer.class));
    }

    @Test
    public void testConvertNumberWithType() {
        Type<Integer> intType = Type.of(int.class);
        Type<Long> longType = Type.of(Long.class);
        
        Assertions.assertEquals(Integer.valueOf(100), Numbers.convert(100L, intType));
        Assertions.assertEquals(Long.valueOf(100L), Numbers.convert(100, longType));
        
        // Test null handling
        Assertions.assertEquals(Integer.valueOf(0), Numbers.convert(null, intType));
    }

    @Test
    public void testFormatInt() {
        Assertions.assertEquals("123", Numbers.format(123, "0"));
        Assertions.assertEquals("123.00", Numbers.format(123, "0.00"));
        Assertions.assertEquals("1,234", Numbers.format(1234, "#,###"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(123, null));
    }

    @Test
    public void testFormatInteger() {
        Assertions.assertEquals("123", Numbers.format(Integer.valueOf(123), "0"));
        Assertions.assertEquals("0", Numbers.format((Integer) null, "0"));
        Assertions.assertEquals("123.00", Numbers.format(Integer.valueOf(123), "0.00"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(Integer.valueOf(123), null));
    }

    @Test
    public void testFormatLong() {
        Assertions.assertEquals("1234567890", Numbers.format(1234567890L, "0"));
        Assertions.assertEquals("1,234,567,890", Numbers.format(1234567890L, "#,###"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.format(123L, null));
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
    public void testExtractFirstInt() {
        Assertions.assertEquals(123, Numbers.extractFirstInt("abc123def"));
        Assertions.assertEquals(-456, Numbers.extractFirstInt("test-456end"));
        Assertions.assertEquals(0, Numbers.extractFirstInt("no numbers here"));
        Assertions.assertEquals(0, Numbers.extractFirstInt(null));
        Assertions.assertEquals(0, Numbers.extractFirstInt(""));
        
        Assertions.assertEquals(123, Numbers.extractFirstInt("abc123def", 999));
        Assertions.assertEquals(999, Numbers.extractFirstInt("no numbers", 999));
        Assertions.assertEquals(999, Numbers.extractFirstInt(null, 999));
    }

    @Test
    public void testExtractFirstLong() {
        Assertions.assertEquals(123456789012L, Numbers.extractFirstLong("abc123456789012def"));
        Assertions.assertEquals(-456L, Numbers.extractFirstLong("test-456end"));
        Assertions.assertEquals(0L, Numbers.extractFirstLong("no numbers here"));
        Assertions.assertEquals(0L, Numbers.extractFirstLong(null));
        
        Assertions.assertEquals(123L, Numbers.extractFirstLong("abc123def", 999L));
        Assertions.assertEquals(999L, Numbers.extractFirstLong("no numbers", 999L));
    }

    @Test
    public void testExtractFirstDouble() {
        Assertions.assertEquals(123.45, Numbers.extractFirstDouble("abc123.45def"), 0.001);
        Assertions.assertEquals(-456.78, Numbers.extractFirstDouble("test-456.78end"), 0.001);
        Assertions.assertEquals(0.0, Numbers.extractFirstDouble("no numbers here"), 0.001);
        Assertions.assertEquals(0.0, Numbers.extractFirstDouble(null), 0.001);
        
        // Test with scientific notation
        Assertions.assertEquals(1.23e5, Numbers.extractFirstDouble("value: 1.23e5", true), 0.001);
        Assertions.assertEquals(1.23, Numbers.extractFirstDouble("value: 1.23e5", false), 0.001);
        
        // Test with default value
        Assertions.assertEquals(999.9, Numbers.extractFirstDouble("no numbers", 999.9), 0.001);
        Assertions.assertEquals(123.45, Numbers.extractFirstDouble("abc123.45def", 999.9, false), 0.001);
        Assertions.assertEquals(1.23e5, Numbers.extractFirstDouble("value: 1.23e5", 999.9, true), 0.001);
    }

    @Test
    public void testToByte() {
        // Test String conversions
        Assertions.assertEquals((byte) 123, Numbers.toByte("123"));
        Assertions.assertEquals((byte) -123, Numbers.toByte("-123"));
        Assertions.assertEquals((byte) 0, Numbers.toByte(null));
        Assertions.assertEquals((byte) 0, Numbers.toByte(""));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toByte("256"));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toByte("abc"));
        
        // Test String with default value
        Assertions.assertEquals((byte) 99, Numbers.toByte(null, (byte) 99));
        Assertions.assertEquals((byte) 99, Numbers.toByte("", (byte) 99));
        Assertions.assertEquals((byte) 123, Numbers.toByte("123", (byte) 99));
        
        // Test Object conversions
        Assertions.assertEquals((byte) 123, Numbers.toByte(Integer.valueOf(123)));
        Assertions.assertEquals((byte) 123, Numbers.toByte(Long.valueOf(123L)));
        Assertions.assertEquals((byte) 123, Numbers.toByte(Double.valueOf(123.0)));
        Assertions.assertEquals((byte) 0, Numbers.toByte((Object) null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toByte(Integer.valueOf(256)));
        
        // Test Object with default value
        Assertions.assertEquals((byte) 99, Numbers.toByte((Object) null, (byte) 99));
        Assertions.assertEquals((byte) 123, Numbers.toByte(Integer.valueOf(123), (byte) 99));
    }

    @Test
    public void testToShort() {
        // Test String conversions
        Assertions.assertEquals((short) 12345, Numbers.toShort("12345"));
        Assertions.assertEquals((short) -12345, Numbers.toShort("-12345"));
        Assertions.assertEquals((short) 0, Numbers.toShort(null));
        Assertions.assertEquals((short) 0, Numbers.toShort(""));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toShort("65536"));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toShort("abc"));
        
        // Test String with default value
        Assertions.assertEquals((short) 999, Numbers.toShort(null, (short) 999));
        Assertions.assertEquals((short) 999, Numbers.toShort("", (short) 999));
        Assertions.assertEquals((short) 12345, Numbers.toShort("12345", (short) 999));
        
        // Test Object conversions
        Assertions.assertEquals((short) 12345, Numbers.toShort(Integer.valueOf(12345)));
        Assertions.assertEquals((short) 12345, Numbers.toShort(Long.valueOf(12345L)));
        Assertions.assertEquals((short) 12345, Numbers.toShort(Double.valueOf(12345.0)));
        Assertions.assertEquals((short) 0, Numbers.toShort((Object) null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toShort(Integer.valueOf(65536)));
        
        // Test Object with default value
        Assertions.assertEquals((short) 999, Numbers.toShort((Object) null, (short) 999));
        Assertions.assertEquals((short) 12345, Numbers.toShort(Integer.valueOf(12345), (short) 999));
    }

    @Test
    public void testToInt() {
        // Test String conversions
        Assertions.assertEquals(123456, Numbers.toInt("123456"));
        Assertions.assertEquals(-123456, Numbers.toInt("-123456"));
        Assertions.assertEquals(0, Numbers.toInt(null));
        Assertions.assertEquals(0, Numbers.toInt(""));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc"));
        
        // Test String with default value
        Assertions.assertEquals(999, Numbers.toInt(null, 999));
        Assertions.assertEquals(999, Numbers.toInt("", 999));
        Assertions.assertEquals(123456, Numbers.toInt("123456", 999));
        
        // Test Object conversions
        Assertions.assertEquals(123456, Numbers.toInt(Integer.valueOf(123456)));
        Assertions.assertEquals(123456, Numbers.toInt(Long.valueOf(123456L)));
        Assertions.assertEquals(123456, Numbers.toInt(Double.valueOf(123456.0)));
        Assertions.assertEquals(0, Numbers.toInt((Object) null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toInt(Long.valueOf(10000000000L)));
        
        // Test Object with default value
        Assertions.assertEquals(999, Numbers.toInt((Object) null, 999));
        Assertions.assertEquals(123456, Numbers.toInt(Integer.valueOf(123456), 999));
    }

    @Test
    public void testToLong() {
        // Test String conversions
        Assertions.assertEquals(1234567890123L, Numbers.toLong("1234567890123"));
        Assertions.assertEquals(-1234567890123L, Numbers.toLong("-1234567890123"));
        Assertions.assertEquals(0L, Numbers.toLong(null));
        Assertions.assertEquals(0L, Numbers.toLong(""));
        Assertions.assertEquals(123L, Numbers.toLong("123L"));
        Assertions.assertEquals(123L, Numbers.toLong("123l"));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toLong("abc"));
        
        // Test String with default value
        Assertions.assertEquals(999L, Numbers.toLong(null, 999L));
        Assertions.assertEquals(999L, Numbers.toLong("", 999L));
        Assertions.assertEquals(1234567890123L, Numbers.toLong("1234567890123", 999L));
        
        // Test Object conversions
        Assertions.assertEquals(1234567890123L, Numbers.toLong(Long.valueOf(1234567890123L)));
        Assertions.assertEquals(123456L, Numbers.toLong(Integer.valueOf(123456)));
        Assertions.assertEquals(123456L, Numbers.toLong(Double.valueOf(123456.0)));
        Assertions.assertEquals(0L, Numbers.toLong((Object) null));
        
        // Test Object with default value
        Assertions.assertEquals(999L, Numbers.toLong((Object) null, 999L));
        Assertions.assertEquals(1234567890123L, Numbers.toLong(Long.valueOf(1234567890123L), 999L));
    }

    @Test
    public void testToFloat() {
        // Test String conversions
        Assertions.assertEquals(123.45f, Numbers.toFloat("123.45"), 0.001f);
        Assertions.assertEquals(-123.45f, Numbers.toFloat("-123.45"), 0.001f);
        Assertions.assertEquals(0.0f, Numbers.toFloat(null), 0.001f);
        Assertions.assertEquals(0.0f, Numbers.toFloat(""), 0.001f);
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toFloat("abc"));
        
        // Test String with default value
        Assertions.assertEquals(999.9f, Numbers.toFloat(null, 999.9f), 0.001f);
        Assertions.assertEquals(999.9f, Numbers.toFloat("", 999.9f), 0.001f);
        Assertions.assertEquals(123.45f, Numbers.toFloat("123.45", 999.9f), 0.001f);
        
        // Test Object conversions
        Assertions.assertEquals(123.45f, Numbers.toFloat(Float.valueOf(123.45f)), 0.001f);
        Assertions.assertEquals(123.0f, Numbers.toFloat(Integer.valueOf(123)), 0.001f);
        Assertions.assertEquals(123.45f, Numbers.toFloat(Double.valueOf(123.45)), 0.001f);
        Assertions.assertEquals(0.0f, Numbers.toFloat((Object) null), 0.001f);
        
        // Test Object with default value
        Assertions.assertEquals(999.9f, Numbers.toFloat((Object) null, 999.9f), 0.001f);
        Assertions.assertEquals(123.45f, Numbers.toFloat(Float.valueOf(123.45f), 999.9f), 0.001f);
    }

    @Test
    public void testToDouble() {
        // Test String conversions
        Assertions.assertEquals(123.456789, Numbers.toDouble("123.456789"), 0.000001);
        Assertions.assertEquals(-123.456789, Numbers.toDouble("-123.456789"), 0.000001);
        Assertions.assertEquals(0.0, Numbers.toDouble((String) null), 0.000001);
        Assertions.assertEquals(0.0, Numbers.toDouble(""), 0.000001);
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.toDouble("abc"));
        
        // Test String with default value
        Assertions.assertEquals(999.999, Numbers.toDouble((String) null, 999.999), 0.000001);
        Assertions.assertEquals(999.999, Numbers.toDouble("", 999.999), 0.000001);
        Assertions.assertEquals(123.456789, Numbers.toDouble("123.456789", 999.999), 0.000001);
        
        // Test Object conversions
        Assertions.assertEquals(123.456789, Numbers.toDouble(Double.valueOf(123.456789)), 0.000001);
        Assertions.assertEquals(123.0, Numbers.toDouble(Integer.valueOf(123)), 0.000001);
        Assertions.assertEquals(123.45f, Numbers.toDouble(Float.valueOf(123.45f)), 0.001);
        Assertions.assertEquals(0.0, Numbers.toDouble((Object) null), 0.000001);
        
        // Test Object with default value
        Assertions.assertEquals(999.999, Numbers.toDouble((Object) null, 999.999), 0.000001);
        Assertions.assertEquals(123.456789, Numbers.toDouble(Double.valueOf(123.456789), 999.999), 0.000001);
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
        // Test BigDecimal scaling
        BigDecimal bd = BigDecimal.valueOf(123.456789);
        BigDecimal scaled = Numbers.toScaledBigDecimal(bd);
        Assertions.assertEquals(2, scaled.scale());
        Assertions.assertEquals(BigDecimal.valueOf(123.46), scaled);
        
        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((BigDecimal) null));
        
        // Test with custom scale and rounding mode
        scaled = Numbers.toScaledBigDecimal(bd, 3, RoundingMode.DOWN);
        Assertions.assertEquals(3, scaled.scale());
        Assertions.assertEquals(BigDecimal.valueOf(123.456), scaled);
        
        // Test Float scaling
        Float f = 123.456789f;
        scaled = Numbers.toScaledBigDecimal(f);
        Assertions.assertEquals(2, scaled.scale());
        
        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Float) null));
        
        scaled = Numbers.toScaledBigDecimal(f, 3, RoundingMode.UP);
        Assertions.assertEquals(3, scaled.scale());
        
        // Test Double scaling
        Double d = 123.456789;
        scaled = Numbers.toScaledBigDecimal(d);
        Assertions.assertEquals(2, scaled.scale());
        
        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((Double) null));
        
        scaled = Numbers.toScaledBigDecimal(d, 3, RoundingMode.CEILING);
        Assertions.assertEquals(3, scaled.scale());
        
        // Test String scaling
        String s = "123.456789";
        scaled = Numbers.toScaledBigDecimal(s);
        Assertions.assertEquals(2, scaled.scale());
        
        Assertions.assertEquals(BigDecimal.ZERO, Numbers.toScaledBigDecimal((String) null));
        
        scaled = Numbers.toScaledBigDecimal(s, 3, RoundingMode.FLOOR);
        Assertions.assertEquals(3, scaled.scale());
    }

    @Test
    public void testToIntExact() {
        Assertions.assertEquals(12345, Numbers.toIntExact(12345L));
        Assertions.assertEquals(-12345, Numbers.toIntExact(-12345L));
        Assertions.assertEquals(Integer.MAX_VALUE, Numbers.toIntExact((long) Integer.MAX_VALUE));
        Assertions.assertEquals(Integer.MIN_VALUE, Numbers.toIntExact((long) Integer.MIN_VALUE));
        
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Long.MAX_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Long.MIN_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MAX_VALUE + 1));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MIN_VALUE - 1));
    }

    @Test
    public void testCreateInteger() {
        Assertions.assertEquals(Integer.valueOf(123), Numbers.createInteger("123"));
        Assertions.assertEquals(Integer.valueOf(-123), Numbers.createInteger("-123"));
        Assertions.assertEquals(Integer.valueOf(0x1A), Numbers.createInteger("0x1A"));
        Assertions.assertEquals(Integer.valueOf(0x1a), Numbers.createInteger("0x1a"));
        Assertions.assertEquals(Integer.valueOf(077), Numbers.createInteger("077"));
        Assertions.assertNull(Numbers.createInteger(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createInteger("abc"));
    }

    @Test
    public void testCreateLong() {
        Assertions.assertEquals(Long.valueOf(123L), Numbers.createLong("123"));
        Assertions.assertEquals(Long.valueOf(-123L), Numbers.createLong("-123"));
        Assertions.assertEquals(Long.valueOf(123L), Numbers.createLong("123L"));
        Assertions.assertEquals(Long.valueOf(123L), Numbers.createLong("123l"));
        Assertions.assertEquals(Long.valueOf(0x1AL), Numbers.createLong("0x1A"));
        Assertions.assertEquals(Long.valueOf(077L), Numbers.createLong("077"));
        Assertions.assertNull(Numbers.createLong(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createLong("abc"));
    }

    @Test
    public void testCreateFloat() {
        Assertions.assertEquals(Float.valueOf(123.45f), Numbers.createFloat("123.45"));
        Assertions.assertEquals(Float.valueOf(-123.45f), Numbers.createFloat("-123.45"));
        Assertions.assertNull(Numbers.createFloat(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createFloat("abc"));
    }

    @Test
    public void testCreateDouble() {
        Assertions.assertEquals(Double.valueOf(123.456789), Numbers.createDouble("123.456789"));
        Assertions.assertEquals(Double.valueOf(-123.456789), Numbers.createDouble("-123.456789"));
        Assertions.assertNull(Numbers.createDouble(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createDouble("abc"));
    }

    @Test
    public void testCreateBigInteger() {
        Assertions.assertEquals(new BigInteger("123456789012345678901234567890"), 
                Numbers.createBigInteger("123456789012345678901234567890"));
        Assertions.assertEquals(new BigInteger("-123456789012345678901234567890"), 
                Numbers.createBigInteger("-123456789012345678901234567890"));
        Assertions.assertEquals(new BigInteger("1A", 16), Numbers.createBigInteger("0x1A"));
        Assertions.assertEquals(new BigInteger("1A", 16), Numbers.createBigInteger("0X1A"));
        Assertions.assertEquals(new BigInteger("1A", 16), Numbers.createBigInteger("#1A"));
        Assertions.assertEquals(new BigInteger("77", 8), Numbers.createBigInteger("077"));
        Assertions.assertNull(Numbers.createBigInteger(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createBigInteger("abc"));
    }

    @Test
    public void testCreateBigDecimal() {
        Assertions.assertEquals(new BigDecimal("123.456789012345678901234567890"), 
                Numbers.createBigDecimal("123.456789012345678901234567890"));
        Assertions.assertEquals(new BigDecimal("-123.456789012345678901234567890"), 
                Numbers.createBigDecimal("-123.456789012345678901234567890"));
        Assertions.assertNull(Numbers.createBigDecimal(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createBigDecimal("abc"));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createBigDecimal(""));
    }

    @Test
    public void testCreateNumber() {
        // Test integers
        Assertions.assertEquals(Integer.valueOf(123), Numbers.createNumber("123"));
        Assertions.assertEquals(Long.valueOf(12345678901L), Numbers.createNumber("12345678901"));
        Assertions.assertTrue(Numbers.createNumber("123456789012345678901234567890") instanceof BigInteger);
        
        // Test decimals
        Assertions.assertTrue(Numbers.createNumber("123.45") instanceof Double);
        Assertions.assertTrue(Numbers.createNumber("123.456789012345678901234567890") instanceof Double);
        
        // Test with type specifiers
        Assertions.assertTrue(Numbers.createNumber("123L") instanceof Long);
        Assertions.assertTrue(Numbers.createNumber("123l") instanceof Long);
        Assertions.assertTrue(Numbers.createNumber("123.45F") instanceof Float);
        Assertions.assertTrue(Numbers.createNumber("123.45f") instanceof Float);
        Assertions.assertTrue(Numbers.createNumber("123.45D") instanceof Double);
        Assertions.assertTrue(Numbers.createNumber("123.45d") instanceof Double);
        
        // Test hex and octal
        Assertions.assertEquals(Integer.valueOf(0x1A), Numbers.createNumber("0x1A"));
        Assertions.assertEquals(Integer.valueOf(077), Numbers.createNumber("077"));
        
        // Test null and invalid
        Assertions.assertNull(Numbers.createNumber(null));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createNumber(""));
        Assertions.assertThrows(NumberFormatException.class, () -> Numbers.createNumber("abc"));
    }

    @Test
    public void testIsDigits() {
        Assertions.assertTrue(Numbers.isDigits("123"));
        Assertions.assertTrue(Numbers.isDigits("0"));
        Assertions.assertFalse(Numbers.isDigits("123.45"));
        Assertions.assertFalse(Numbers.isDigits("-123"));
        Assertions.assertFalse(Numbers.isDigits("abc"));
        Assertions.assertFalse(Numbers.isDigits(""));
        Assertions.assertFalse(Numbers.isDigits(null));
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
        Assertions.assertTrue(Numbers.isCreatable("123"));
        Assertions.assertTrue(Numbers.isCreatable("-123"));
        Assertions.assertTrue(Numbers.isCreatable("123.45"));
        Assertions.assertTrue(Numbers.isCreatable("-123.45"));
        Assertions.assertTrue(Numbers.isCreatable("1.23e5"));
        Assertions.assertTrue(Numbers.isCreatable("1.23E5"));
        Assertions.assertTrue(Numbers.isCreatable("123L"));
        Assertions.assertTrue(Numbers.isCreatable("123l"));
        Assertions.assertTrue(Numbers.isCreatable("123.45f"));
        Assertions.assertTrue(Numbers.isCreatable("123.45F"));
        Assertions.assertTrue(Numbers.isCreatable("123.45d"));
        Assertions.assertTrue(Numbers.isCreatable("123.45D"));
        Assertions.assertTrue(Numbers.isCreatable("0x1A"));
        Assertions.assertTrue(Numbers.isCreatable("077"));
        
        Assertions.assertFalse(Numbers.isCreatable(""));
        Assertions.assertFalse(Numbers.isCreatable(null));
        Assertions.assertFalse(Numbers.isCreatable("abc"));
        Assertions.assertFalse(Numbers.isCreatable("123.45.67"));
        Assertions.assertFalse(Numbers.isCreatable("123e"));
    }

    @Test
    public void testIsParsable() {
        Assertions.assertTrue(Numbers.isParsable("123"));
        Assertions.assertTrue(Numbers.isParsable("-123"));
        Assertions.assertTrue(Numbers.isParsable("123.45"));
        Assertions.assertTrue(Numbers.isParsable("-123.45"));
        
        Assertions.assertFalse(Numbers.isParsable(""));
        Assertions.assertFalse(Numbers.isParsable(null));
        Assertions.assertFalse(Numbers.isParsable("abc"));
        Assertions.assertFalse(Numbers.isParsable("123."));
        Assertions.assertFalse(Numbers.isParsable("123.45.67"));
        Assertions.assertFalse(Numbers.isParsable("1.23e5")); // Scientific notation not parsable
        Assertions.assertFalse(Numbers.isParsable("0x1A")); // Hex not parsable
        Assertions.assertFalse(Numbers.isParsable("-"));
        Assertions.assertFalse(Numbers.isParsable("+"));
    }

    @Test
    public void testIsPrime() {
        // Test small primes
        Assertions.assertTrue(Numbers.isPrime(2));
        Assertions.assertTrue(Numbers.isPrime(3));
        Assertions.assertTrue(Numbers.isPrime(5));
        Assertions.assertTrue(Numbers.isPrime(7));
        Assertions.assertTrue(Numbers.isPrime(11));
        Assertions.assertTrue(Numbers.isPrime(13));
        
        // Test non-primes
        Assertions.assertFalse(Numbers.isPrime(0));
        Assertions.assertFalse(Numbers.isPrime(1));
        Assertions.assertFalse(Numbers.isPrime(4));
        Assertions.assertFalse(Numbers.isPrime(6));
        Assertions.assertFalse(Numbers.isPrime(8));
        Assertions.assertFalse(Numbers.isPrime(9));
        Assertions.assertFalse(Numbers.isPrime(10));
        
        // Test larger primes
        Assertions.assertTrue(Numbers.isPrime(97));
        Assertions.assertTrue(Numbers.isPrime(101));
        Assertions.assertTrue(Numbers.isPrime(997));
        
        // Test larger non-primes
        Assertions.assertFalse(Numbers.isPrime(100));
        Assertions.assertFalse(Numbers.isPrime(999));
        
        // Test negative numbers
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.isPrime(-1));
    }

    @Test
    public void testIsPerfectSquare() {
        // Test int perfect squares
        Assertions.assertTrue(Numbers.isPerfectSquare(0));
        Assertions.assertTrue(Numbers.isPerfectSquare(1));
        Assertions.assertTrue(Numbers.isPerfectSquare(4));
        Assertions.assertTrue(Numbers.isPerfectSquare(9));
        Assertions.assertTrue(Numbers.isPerfectSquare(16));
        Assertions.assertTrue(Numbers.isPerfectSquare(25));
        Assertions.assertTrue(Numbers.isPerfectSquare(100));
        Assertions.assertTrue(Numbers.isPerfectSquare(10000));
        
        // Test int non-perfect squares
        Assertions.assertFalse(Numbers.isPerfectSquare(2));
        Assertions.assertFalse(Numbers.isPerfectSquare(3));
        Assertions.assertFalse(Numbers.isPerfectSquare(5));
        Assertions.assertFalse(Numbers.isPerfectSquare(10));
        Assertions.assertFalse(Numbers.isPerfectSquare(99));
        Assertions.assertFalse(Numbers.isPerfectSquare(-1));
        Assertions.assertFalse(Numbers.isPerfectSquare(-4));
        
        // Test long perfect squares
        Assertions.assertTrue(Numbers.isPerfectSquare(0L));
        Assertions.assertTrue(Numbers.isPerfectSquare(1L));
        Assertions.assertTrue(Numbers.isPerfectSquare(4L));
        Assertions.assertTrue(Numbers.isPerfectSquare(9L));
        Assertions.assertTrue(Numbers.isPerfectSquare(1000000L)); // 1000^2
        
        // Test long non-perfect squares
        Assertions.assertFalse(Numbers.isPerfectSquare(2L));
        Assertions.assertFalse(Numbers.isPerfectSquare(999999L));
        Assertions.assertFalse(Numbers.isPerfectSquare(-1L));
        Assertions.assertFalse(Numbers.isPerfectSquare(-4L));
    }

    @Test
    public void testIsPowerOfTwo() {
        // Test int powers of two
        Assertions.assertTrue(Numbers.isPowerOfTwo(1));
        Assertions.assertTrue(Numbers.isPowerOfTwo(2));
        Assertions.assertTrue(Numbers.isPowerOfTwo(4));
        Assertions.assertTrue(Numbers.isPowerOfTwo(8));
        Assertions.assertTrue(Numbers.isPowerOfTwo(16));
        Assertions.assertTrue(Numbers.isPowerOfTwo(32));
        Assertions.assertTrue(Numbers.isPowerOfTwo(1024));
        Assertions.assertTrue(Numbers.isPowerOfTwo(1 << 30));
        
        // Test int non-powers of two
        Assertions.assertFalse(Numbers.isPowerOfTwo(0));
        Assertions.assertFalse(Numbers.isPowerOfTwo(-1));
        Assertions.assertFalse(Numbers.isPowerOfTwo(3));
        Assertions.assertFalse(Numbers.isPowerOfTwo(5));
        Assertions.assertFalse(Numbers.isPowerOfTwo(6));
        Assertions.assertFalse(Numbers.isPowerOfTwo(7));
        Assertions.assertFalse(Numbers.isPowerOfTwo(9));
        Assertions.assertFalse(Numbers.isPowerOfTwo(1023));
        
        // Test long powers of two
        Assertions.assertTrue(Numbers.isPowerOfTwo(1L));
        Assertions.assertTrue(Numbers.isPowerOfTwo(2L));
        Assertions.assertTrue(Numbers.isPowerOfTwo(1L << 40));
        Assertions.assertTrue(Numbers.isPowerOfTwo(1L << 62));
        
        // Test long non-powers of two
        Assertions.assertFalse(Numbers.isPowerOfTwo(0L));
        Assertions.assertFalse(Numbers.isPowerOfTwo(-1L));
        Assertions.assertFalse(Numbers.isPowerOfTwo(3L));
        Assertions.assertFalse(Numbers.isPowerOfTwo((1L << 40) + 1));
        
        // Test double powers of two
        Assertions.assertTrue(Numbers.isPowerOfTwo(1.0));
        Assertions.assertTrue(Numbers.isPowerOfTwo(2.0));
        Assertions.assertTrue(Numbers.isPowerOfTwo(4.0));
        Assertions.assertTrue(Numbers.isPowerOfTwo(0.5));
        Assertions.assertTrue(Numbers.isPowerOfTwo(0.25));
        
        // Test double non-powers of two
        Assertions.assertFalse(Numbers.isPowerOfTwo(0.0));
        Assertions.assertFalse(Numbers.isPowerOfTwo(-1.0));
        Assertions.assertFalse(Numbers.isPowerOfTwo(3.0));
        Assertions.assertFalse(Numbers.isPowerOfTwo(1.5));
        Assertions.assertFalse(Numbers.isPowerOfTwo(Double.NaN));
        Assertions.assertFalse(Numbers.isPowerOfTwo(Double.POSITIVE_INFINITY));
        
        // Test BigInteger powers of two
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(1)));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(2)));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(1024)));
        Assertions.assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(2).pow(100)));
        
        // Test BigInteger non-powers of two
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(0)));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(-1)));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(3)));
        Assertions.assertFalse(Numbers.isPowerOfTwo(BigInteger.valueOf(2).pow(100).add(BigInteger.ONE)));
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.isPowerOfTwo((BigInteger) null));
    }

    @Test
    public void testLog() {
        Assertions.assertEquals(Math.log(10), Numbers.log(10), 0.000001);
        Assertions.assertEquals(Math.log(100), Numbers.log(100), 0.000001);
        Assertions.assertEquals(Math.log(0.5), Numbers.log(0.5), 0.000001);
        Assertions.assertTrue(Double.isNaN(Numbers.log(-1)));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Numbers.log(0), 0);
    }

    @Test
    public void testLog2Int() {
        // Test UNNECESSARY mode (exact powers of 2)
        Assertions.assertEquals(0, Numbers.log2(1, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log2(2, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.log2(4, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(10, Numbers.log2(1024, RoundingMode.UNNECESSARY));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.log2(3, RoundingMode.UNNECESSARY));
        
        // Test DOWN/FLOOR mode
        Assertions.assertEquals(0, Numbers.log2(1, RoundingMode.DOWN));
        Assertions.assertEquals(1, Numbers.log2(2, RoundingMode.DOWN));
        Assertions.assertEquals(1, Numbers.log2(3, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(4, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(5, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(7, RoundingMode.DOWN));
        Assertions.assertEquals(3, Numbers.log2(8, RoundingMode.DOWN));
        
        // Test UP/CEILING mode
        Assertions.assertEquals(0, Numbers.log2(1, RoundingMode.UP));
        Assertions.assertEquals(1, Numbers.log2(2, RoundingMode.UP));
        Assertions.assertEquals(2, Numbers.log2(3, RoundingMode.UP));
        Assertions.assertEquals(2, Numbers.log2(4, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(5, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(7, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(8, RoundingMode.UP));
        
        // Test HALF_UP, HALF_DOWN, HALF_EVEN modes
        Assertions.assertEquals(2, Numbers.log2(3, RoundingMode.HALF_UP));
        Assertions.assertEquals(2, Numbers.log2(5, RoundingMode.HALF_UP));
        Assertions.assertEquals(3, Numbers.log2(6, RoundingMode.HALF_UP));
        
        // Test invalid input
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1, RoundingMode.DOWN));
    }

    @Test
    public void testLog2Long() {
        // Test UNNECESSARY mode (exact powers of 2)
        Assertions.assertEquals(0, Numbers.log2(1L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log2(2L, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(32, Numbers.log2(1L << 32, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(62, Numbers.log2(1L << 62, RoundingMode.UNNECESSARY));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.log2(3L, RoundingMode.UNNECESSARY));
        
        // Test DOWN/FLOOR mode
        Assertions.assertEquals(1, Numbers.log2(3L, RoundingMode.DOWN));
        Assertions.assertEquals(2, Numbers.log2(5L, RoundingMode.DOWN));
        Assertions.assertEquals(32, Numbers.log2((1L << 32) + 1, RoundingMode.DOWN));
        
        // Test UP/CEILING mode
        Assertions.assertEquals(2, Numbers.log2(3L, RoundingMode.UP));
        Assertions.assertEquals(3, Numbers.log2(5L, RoundingMode.UP));
        Assertions.assertEquals(33, Numbers.log2((1L << 32) + 1, RoundingMode.UP));
        
        // Test HALF modes
        Assertions.assertEquals(2, Numbers.log2(3L, RoundingMode.HALF_UP));
        Assertions.assertEquals(3, Numbers.log2(6L, RoundingMode.HALF_UP));
        
        // Test invalid input
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0L, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1L, RoundingMode.DOWN));
    }

    @Test
    public void testLog2Double() {
        // Test exact result
        Assertions.assertEquals(0.0, Numbers.log2(1.0), 0.000001);
        Assertions.assertEquals(1.0, Numbers.log2(2.0), 0.000001);
        Assertions.assertEquals(2.0, Numbers.log2(4.0), 0.000001);
        Assertions.assertEquals(3.0, Numbers.log2(8.0), 0.000001);
        Assertions.assertEquals(-1.0, Numbers.log2(0.5), 0.000001);
        Assertions.assertEquals(-2.0, Numbers.log2(0.25), 0.000001);
        
        // Test non-exact results
        Assertions.assertEquals(1.584962, Numbers.log2(3.0), 0.000001);
        Assertions.assertEquals(2.321928, Numbers.log2(5.0), 0.000001);
        
        // Test special cases
        Assertions.assertTrue(Double.isNaN(Numbers.log2(-1.0)));
        Assertions.assertTrue(Double.isNaN(Numbers.log2(Double.NaN)));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Numbers.log2(0.0), 0);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Numbers.log2(Double.POSITIVE_INFINITY), 0);
    }

    @Test
    public void testLog2DoubleWithRoundingMode() {
        // Test UNNECESSARY mode with exact powers of 2
        Assertions.assertEquals(0, Numbers.log2(1.0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1, Numbers.log2(2.0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(2, Numbers.log2(4.0, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(-1, Numbers.log2(0.5, RoundingMode.UNNECESSARY));
        Assertions.assertThrows(ArithmeticException.class, () -> Numbers.log2(3.0, RoundingMode.UNNECESSARY));
        
        // Test FLOOR mode
        Assertions.assertEquals(1, Numbers.log2(3.0, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.log2(5.0, RoundingMode.FLOOR));
        Assertions.assertEquals(2, Numbers.log2(7.9, RoundingMode.FLOOR));
        
        // Test CEILING mode
        Assertions.assertEquals(2, Numbers.log2(3.0, RoundingMode.CEILING));
        Assertions.assertEquals(3, Numbers.log2(5.0, RoundingMode.CEILING));
        Assertions.assertEquals(3, Numbers.log2(4.1, RoundingMode.CEILING));
        
        // Test DOWN mode (toward zero)
        Assertions.assertEquals(1, Numbers.log2(3.0, RoundingMode.DOWN));
        Assertions.assertEquals(-1, Numbers.log2(0.3, RoundingMode.DOWN));
        
        // Test UP mode (away from zero)
        Assertions.assertEquals(2, Numbers.log2(3.0, RoundingMode.UP));
        Assertions.assertEquals(-2, Numbers.log2(0.3, RoundingMode.UP));
        
        // Test HALF modes
        Assertions.assertEquals(2, Numbers.log2(3.0, RoundingMode.HALF_UP));
        Assertions.assertEquals(3, Numbers.log2(6.0, RoundingMode.HALF_UP));
        
        // Test invalid inputs
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1.0, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.NaN, RoundingMode.DOWN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
    }
}