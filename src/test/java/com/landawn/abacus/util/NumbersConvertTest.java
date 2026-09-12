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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.type.Type;

public class NumbersConvertTest extends NumbersTestSupport {

    @Test
    public void testConvert_BuiltInTargets() {
        final Byte val = (byte) 10;
        assertEquals(Byte.valueOf((byte) 10), Numbers.convert(val, byte.class));
        assertEquals(Short.valueOf((short) 10), Numbers.convert(val, short.class));
        assertEquals(Integer.valueOf(10), Numbers.convert(val, int.class));
        assertEquals(Long.valueOf(10L), Numbers.convert(val, long.class));
        assertEquals(10.0f, Numbers.convert(val, float.class), DELTA);
        assertEquals(10.0d, Numbers.convert(val, double.class), DELTA);
        assertEquals(BigInteger.valueOf(10), Numbers.convert(val, BigInteger.class));
        assertEquals(BigDecimal.valueOf(10), Numbers.convert(val, BigDecimal.class));

        assertEquals(Byte.valueOf((byte) 100), Numbers.convert(100, byte.class));
        assertEquals(Integer.valueOf(12345), Numbers.convert(12345L, int.class));
        assertEquals(Long.valueOf(123L), Numbers.convert(123.45d, long.class));
        assertEquals(Integer.valueOf(123), Numbers.convert(new BigDecimal("123.789"), Integer.class));
        assertEquals(Integer.valueOf(42), Numbers.convert(42, Integer.class));
    }

    @Test
    public void testConvert_NullAndDefault() {
        assertNull(Numbers.convert(null, Integer.class));
        assertEquals(Byte.valueOf((byte) 0), Numbers.convert(null, byte.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, int.class));
        assertEquals(Byte.valueOf((byte) 5), Numbers.convert(null, byte.class, (byte) 5));
        assertEquals(Integer.valueOf(100), Numbers.convert(null, Integer.class, 100));
        assertNull(Numbers.convert(null, Type.of(Integer.class)));
        assertEquals(Long.valueOf(50L), Numbers.convert(null, Type.of(Long.class), 50L));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, Type.of(int.class)));
    }

    @Test
    public void testConvert_NaNAndInfinity() {
        assertEquals(Float.NaN, Numbers.convert(Double.NaN, float.class));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.convert(Double.POSITIVE_INFINITY, float.class));
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(Double.NEGATIVE_INFINITY, float.class));
        assertEquals(Double.NaN, Numbers.convert(Float.NaN, double.class));
        assertTrue(Numbers.convert(Double.NaN, float.class).isNaN());
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigDecimal.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.POSITIVE_INFINITY, BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.NaN, Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, Type.of(BigInteger.class)));
        assertEquals(Float.valueOf(Float.NaN), Numbers.convert(Double.NaN, Float.class));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Numbers.convert(1e300d, Float.class));
    }

    @Test
    public void testConvert_Overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Long.MAX_VALUE + 100.0, long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((double) Long.MAX_VALUE, long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((float) Integer.MAX_VALUE, int.class));
        assertEquals(Long.MIN_VALUE, Numbers.convert((double) Long.MIN_VALUE, long.class).longValue());
        assertEquals(Integer.MIN_VALUE, Numbers.convert((float) Integer.MIN_VALUE, int.class).intValue());
        assertThrows(ArithmeticException.class, () -> Numbers.convert(BigInteger.valueOf(3000000000L), int.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal("100000000000000000000"), long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((short) 300, byte.class));
        assertEquals("int overflow: 1.0E300", assertThrows(ArithmeticException.class, () -> Numbers.convert(1e300, Integer.class)).getMessage());
        assertEquals(assertThrows(ArithmeticException.class, () -> Numbers.toInt(1e300)).getMessage(),
                assertThrows(ArithmeticException.class, () -> Numbers.convert(1e300, Integer.class)).getMessage());
    }

    @Test
    public void testConvert_Narrowing() {
        assertEquals(Byte.valueOf((byte) 127), Numbers.convert(Float.valueOf(127.9f), Byte.class));
        assertEquals(Byte.valueOf((byte) 127), Numbers.convert(new BigDecimal("127.9"), Byte.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(Double.valueOf(-0.9d), Integer.class));
        assertEquals(Long.valueOf(-5L), Numbers.convert(Byte.valueOf((byte) -5), Long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(Integer.MAX_VALUE), Integer.class));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.convert(Float.valueOf(Integer.MIN_VALUE), Integer.class));

        final Number[] sources = { Integer.valueOf(1000), Long.valueOf(1000L), Float.valueOf(1000.9f), Double.valueOf(1000.9d), BigInteger.valueOf(1000),
                new BigDecimal("1000.9") };
        for (final Number source : sources) {
            assertThrows(ArithmeticException.class, () -> Numbers.convert(source, Byte.class), source.getClass().getSimpleName());
            assertEquals(Short.valueOf((short) 1000), Numbers.convert(source, Short.class));
            assertEquals(1000, Numbers.toInt(source));
        }
    }

    @Test
    public void testConvert_FloatDoubleSpelling() {
        for (final double d : new double[] { 1.1d, 1.21d, 123.45d, -0.1d, 1e-40d, 1e300d, Double.MIN_VALUE }) {
            assertEquals((float) d, Numbers.convert(d, Float.class), 0.0f);
            assertEquals((float) d, Numbers.toFloat(d), 0.0f);
        }
        final float source = 1.21f;
        final double canonicalDecimal = Double.parseDouble(Float.toString(source));
        assertNotEquals(Double.doubleToRawLongBits(source), Double.doubleToRawLongBits(canonicalDecimal));
        assertEquals(canonicalDecimal, Numbers.convert(source, Double.class), 0.0d);

        final float floatValue = 1e20f;
        assertEquals(new BigInteger("100000002004087734272"), Numbers.convert(floatValue, BigInteger.class));
        assertEquals(new BigDecimal("1.0E+20"), Numbers.convert(floatValue, BigDecimal.class));
        assertNotEquals(Numbers.convert(floatValue, BigDecimal.class).toBigInteger(), Numbers.convert(floatValue, BigInteger.class));
        assertEquals(new BigDecimal("1.21"), Numbers.convert(1.21f, BigDecimal.class));
    }

    @Test
    public void testConvert_UnknownSourceAndUnsupportedTarget() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9);
        assertEquals(Integer.valueOf(12), Numbers.convert(fractional, Integer.class));
        assertEquals(Byte.valueOf((byte) 12), Numbers.convert(fractional, Byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new AtomicInteger(1234), Byte.class));
        assertEquals(BigInteger.valueOf(1234), Numbers.convert(new AtomicInteger(1234), BigInteger.class));

        final IllegalArgumentException fromClass = assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), Number.class));
        assertTrue(fromClass.getMessage().contains("Unsupported target type"), fromClass.getMessage());
        assertNotNull(fromClass.getCause());
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), (Class<Integer>) null));
        assertNull(Numbers.convert(null, Number.class));
        assertEquals(5, Numbers.convert(Integer.valueOf(5), AtomicInteger.class).intValue());

        final Type<Integer> intType = Type.of(Integer.class);
        assertEquals(Integer.valueOf(100), Numbers.convert(100L, intType));
        assertEquals(Integer.valueOf(100), Numbers.convert(100L, Type.of(int.class)));
    }

    @Test
    public void testConvert_TypeAgreesWithClass() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigDecimal.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, Type.of(BigDecimal.class)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(Integer.MAX_VALUE), Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(Integer.MAX_VALUE), Type.of(Integer.class)));
        assertTrue(Double.isNaN(Numbers.convert(Double.NaN, Double.class)));
        assertTrue(Double.isNaN(Numbers.convert(Double.NaN, Type.of(Double.class))));
    }
}
