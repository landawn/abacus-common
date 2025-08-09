package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.Test;

import junit.framework.TestCase;

public class NumbersTest extends TestCase {

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
            assertTrue(Numbers.isCreatable("0.0e-2000"));

            // assertEquals(Float.valueOf(0.0f), Numbers.createNumber("0.0e-2000"));
            assertEquals(Float.valueOf(0.0f), Numbers.createNumber("0.0e-2000F"));
            assertEquals(Double.valueOf(0.0), Numbers.createNumber("0.0e-2000D"));
        }
    }

    @Test
    public void testCreatePositiveHexInteger_NumberUtils() {
        // Hex is only supported for integers so no test for hex floating point formats
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
        // Hex is only supported for integers so no test for hex floating point formats
        assertTrue(Numbers.isCreatable("+0xF"));
        assertTrue(Numbers.isCreatable("+0xFFFFFFFF"));
        assertTrue(Numbers.isCreatable("+0xFFFFFFFFFFFFFFFFF"));

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
        //LANG-1613
        assertTrue(Numbers.createNumber("4.9e-324D") instanceof Double);
        assertTrue(Numbers.createNumber("4.9e-324F") instanceof Double);

        assertTrue(NumberUtils.createNumber("0001.797693134862315759e+308") instanceof BigDecimal);
        assertTrue(Numbers.createNumber("0001.797693134862315759e+308") instanceof Double); // 1.7976931348623157E308
        assertTrue(Numbers.createNumber("-001.797693134862315759e+308") instanceof Double);
        assertTrue(Numbers.createNumber("+001.797693134862315759e+308") instanceof Double);

        final String str = "0x100";
        N.println(Integer.decode(str));
        // N.println(Integer.parseInt(str));
        assertFalse(Numbers.isParsable(str));

        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17.0.1"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("170"));

        assertTrue(NumberUtils.createNumber("4.9e-324D") instanceof Double);
        assertTrue(NumberUtils.createNumber("4.9e-324F") instanceof Double);

        assertTrue(11 == Numbers.toLong("11l"));
        assertTrue(11 == Numbers.toLong("11L"));
    }

}
