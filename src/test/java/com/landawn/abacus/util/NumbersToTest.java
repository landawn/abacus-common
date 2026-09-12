/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

public class NumbersToTest extends NumbersTestSupport {

    @Test
    public void testToByte() {
        assertEquals((byte) 10, Numbers.toByte("10"));
        assertEquals((byte) 0, Numbers.toByte((String) null));
        assertEquals((byte) 0, Numbers.toByte(""));
        assertEquals((byte) -127, Numbers.toByte("-127"));
        assertEquals((byte) 42, Numbers.toByte(null, (byte) 42));
        assertEquals((byte) 5, Numbers.toByte("", (byte) 5));
        assertEquals((byte) 10, Numbers.toByte(10));
        assertEquals((byte) 10, Numbers.toByte((Object) "10"));
        assertEquals((byte) 0, Numbers.toByte((Object) null));
        assertEquals((byte) 5, Numbers.toByte((Object) null, (byte) 5));
        assertEquals((byte) 12, Numbers.toByte("12L"));
        assertEquals((byte) 127, Numbers.toByte("127"));
        assertEquals((byte) 127, Numbers.toByte("0x7F"));
        assertEquals((byte) -128, Numbers.toByte("-0x80"));
        assertEquals((byte) 10, Numbers.toByte("010"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("128"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("200", (byte) 5));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(200));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("abc"));
        assertEquals("byte overflow: 200", assertThrows(ArithmeticException.class, () -> Numbers.toByte("200")).getMessage());
        assertEquals("byte overflow: 0x100", assertThrows(ArithmeticException.class, () -> Numbers.toByte("0x100")).getMessage());
    }

    @Test
    public void testToShort() {
        assertEquals((short) 1000, Numbers.toShort("1000"));
        assertEquals((short) 0, Numbers.toShort((String) null));
        assertEquals((short) 0, Numbers.toShort(""));
        assertEquals((short) 42, Numbers.toShort(null, (short) 42));
        assertEquals((short) 100, Numbers.toShort(100));
        assertEquals((short) 100, Numbers.toShort((Object) "100"));
        assertEquals((short) 50, Numbers.toShort((Object) null, (short) 50));
        assertEquals((short) 1234, Numbers.toShort("1234l"));
        assertEquals((short) -32768, Numbers.toShort("-32768"));
        assertEquals((short) 4095, Numbers.toShort("0xFFF"));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort("32768"));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(40000));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("abc"));
        assertEquals("short overflow: 40000", assertThrows(ArithmeticException.class, () -> Numbers.toShort("40000")).getMessage());
    }

    @Test
    public void testToInt() {
        assertEquals(1, Numbers.toInt("1"));
        assertEquals(100000, Numbers.toInt("100000"));
        assertEquals(0, Numbers.toInt((String) null));
        assertEquals(0, Numbers.toInt(""));
        assertEquals(42, Numbers.toInt(null, 42));
        assertEquals(77, Numbers.toInt("", 77));
        assertEquals(1000, Numbers.toInt(1000));
        assertEquals(1000, Numbers.toInt(1000L));
        assertEquals(1000, Numbers.toInt((Object) "1000"));
        assertEquals(0, Numbers.toInt((Object) null));
        assertEquals(500, Numbers.toInt((Object) null, 500));
        assertEquals(123456, Numbers.toInt("123456L"));
        assertEquals(10, Numbers.toInt("010"));
        assertEquals(255, Numbers.toInt("0xFF"));
        assertEquals(255, Numbers.toInt("#FF"));
        assertEquals(2147483647, Numbers.toInt("2147483647"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt((long) Integer.MAX_VALUE + 1));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc", 0));
        assertEquals(7, Numbers.toInt((String) null, 7));
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc")).getMessage());
    }

    @Test
    public void testToLong() {
        assertEquals(1L, Numbers.toLong("1"));
        assertEquals(1L, Numbers.toLong("1l"));
        assertEquals(0L, Numbers.toLong("0L"));
        assertEquals(0L, Numbers.toLong((String) null));
        assertEquals(0L, Numbers.toLong(""));
        assertEquals(42L, Numbers.toLong(null, 42L));
        assertEquals(1000000L, Numbers.toLong(1000000));
        assertEquals(500L, Numbers.toLong(new BigInteger("500")));
        assertEquals(10L, Numbers.toLong("010"));
        assertEquals(255L, Numbers.toLong("0xFF"));
        assertEquals(255L, Numbers.toLong("#ff"));
        assertEquals(9223372036854775807L, Numbers.toLong("9223372036854775807"));
        assertEquals(123456789012345L, Numbers.toLong(BigInteger.valueOf(123456789012345L), 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9223372036854775808"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), 0L));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong("abc"));
        assertEquals("abc is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong("abc")).getMessage());
    }

    @Test
    public void testToXxx_OverflowAndHex() {
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("128"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("-129"));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort("32768"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9223372036854775808"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("0xFF"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("0xFFF"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("0x100000000"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("0x10000000000000000"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("09999999999999999999"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("128L"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("L"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("1.5"));

        assertEquals(10, Numbers.toInt("010"));
        assertEquals(Integer.valueOf(8), Numbers.decodeInteger("010"));
        assertEquals(123, Numbers.toInt("0123L"));
        assertEquals(Long.valueOf(83L), Numbers.decodeLong("0123L"));
        assertEquals(8, Numbers.toInt("08"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("08"));
        assertTrue(Numbers.isParsable("08"));
        assertFalse(Numbers.isCreatable("08"));
    }

    @Test
    public void testToXxx_Object_OverflowAndTruncation() {
        final BigInteger wrapsTo50 = new BigInteger("18446744073709551666");
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(wrapsTo50, (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(wrapsTo50, 0));
        assertEquals((byte) 100, Numbers.toByte(new BigDecimal("100.9"), (byte) 0));
        assertEquals((byte) 127, Numbers.toByte(new BigDecimal("127.9"), (byte) 0));
        assertEquals((byte) -128, Numbers.toByte(new BigDecimal("-128.9"), (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new BigDecimal("128.9"), (byte) 0));

        assertEquals((byte) 127, Numbers.toByte(127.9f, (byte) 0));
        assertEquals((byte) -128, Numbers.toByte(-128.9f, (byte) 0));
        assertEquals(2147483647, Numbers.toInt(2147483647.5d, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(128.5f, (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(Double.NaN, (byte) 9));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(Double.POSITIVE_INFINITY, 0));

        assertEquals(Long.MAX_VALUE, Numbers.toLong(new BigDecimal("9223372036854775807.9"), 0L));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new BigDecimal("-9223372036854775808.9"), 0L));
        assertEquals(0L, Numbers.toLong(new BigDecimal("-0.9"), 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(new BigDecimal("9223372036854775808"), 0L));
        assertEquals(9223372036854774784L, Numbers.toLong(9223372036854774784.0, 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(0x1p63, 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(1e30, 0L));
    }

    @Test
    public void testToXxx_UnknownNumber() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9);
        assertEquals((byte) 12, Numbers.toByte(fractional));
        assertEquals(12, Numbers.toInt(fractional));
        assertEquals(12L, Numbers.toLong(fractional));

        final LongAdder adder = new LongAdder();
        adder.add(-5);
        assertEquals(1234, Numbers.toInt(new AtomicInteger(1234)));
        assertEquals(-5L, Numbers.toLong(adder));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new DecimalTextNumber("-9223372036854775808")));
        assertEquals(Long.MAX_VALUE, Numbers.toLong(new DecimalTextNumber("9223372036854775807.9")));
        assertEquals(42L, Numbers.toLong(new FormattedNumber(42.7)));
        assertEquals(-3, Numbers.toInt(new FormattedNumber(-3.9)));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new AtomicInteger(1234)));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new FormattedNumber(Double.NaN)));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(new DecimalTextNumber("9223372036854775808")));
    }

    @Test
    public void testToFloat() {
        assertEquals(3.14f, Numbers.toFloat("3.14"), 0.001f);
        assertEquals(0.0f, Numbers.toFloat((String) null), FLOAT_DELTA);
        assertEquals(0.0f, Numbers.toFloat(""), FLOAT_DELTA);
        assertEquals(1.0f, Numbers.toFloat("", 1.0f), 0.001f);
        assertEquals(3.14f, Numbers.toFloat(3.14f), 0.001f);
        assertEquals(3.14f, Numbers.toFloat((Object) "3.14"), 0.001f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat("Infinity"));
        assertTrue(Float.isNaN(Numbers.toFloat("NaN")));
        assertEquals(0.0f, Numbers.toFloat((BigDecimal) null), 0.0f);
        assertEquals(8.5f, Numbers.toFloat(BigDecimal.valueOf(8.5)), 0.0f);
        assertTrue(Float.isInfinite(Numbers.toFloat(new BigDecimal("1e40"))));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat((Object) new BigDecimal("1e500")), 0.0f);
        assertThrows(NumberFormatException.class, () -> Numbers.toFloat("abc"));
    }

    @Test
    public void testToDouble() {
        assertEquals(3.14159, Numbers.toDouble("3.14159"), 0.00001);
        assertEquals(0.0, Numbers.toDouble((String) null), DELTA);
        assertEquals(0.0, Numbers.toDouble(""), DELTA);
        assertEquals(1.0, Numbers.toDouble("", 1.0), 0.001);
        assertEquals(3.14159, Numbers.toDouble((Object) "3.14159"), 0.00001);
        assertEquals(123.456, Numbers.toDouble(new BigDecimal("123.456")), 0.001);
        assertEquals(0.0, Numbers.toDouble((BigDecimal) null), 0.001);
        assertEquals(42.0, Numbers.toDouble((BigDecimal) null, 42.0), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.toDouble((Object) new BigDecimal("1e500")), 0.0);
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble("abc"));
    }

    @Test
    public void testToFloatToDouble_BoundedMessage() {
        final String hostile = "9".repeat(4000) + "Z";
        for (final Runnable call : List.<Runnable> of(() -> Numbers.toFloat(hostile), () -> Numbers.toDouble(hostile))) {
            final String message = assertThrows(NumberFormatException.class, call::run).getMessage();
            assertTrue(message.length() < 200, message);
            assertTrue(message.contains("...[" + hostile.length() + " chars]"));
            assertFalse(message.startsWith("For input string"));
        }
        final String nl = assertThrows(NumberFormatException.class, () -> Numbers.toFloat("1\n2")).getMessage();
        assertFalse(nl.contains("\n"));
        assertTrue(nl.contains("1\\u000A2"));

        for (final String s : new String[] { "1.5", "NaN", "Infinity", "0x1.8p1", " 123 " }) {
            assertEquals(Float.parseFloat(s), Numbers.toFloat(s), s);
            assertEquals(Double.parseDouble(s), Numbers.toDouble(s), s);
        }
        for (final String s : new String[] { "abc", "0xFF", "123L" }) {
            assertThrows(NumberFormatException.class, () -> Numbers.toFloat(s), s);
        }
    }

    @Test
    public void testToIntExact() {
        assertEquals(123, Numbers.toIntExact(123L));
        assertEquals(Integer.MAX_VALUE, Numbers.toIntExact(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.toIntExact(Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Integer.MAX_VALUE + 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Integer.MIN_VALUE - 1L));
    }
}
