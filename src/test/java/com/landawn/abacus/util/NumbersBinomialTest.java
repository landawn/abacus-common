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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.time.Duration;

import org.junit.jupiter.api.Test;

public class NumbersBinomialTest extends NumbersTestSupport {

    // ===== saturatedBinomial / binomialExact =====

    @Test
    public void testSaturatedBinomial() {
        assertEquals(1, Numbers.saturatedBinomial(0, 0));
        assertEquals(1, Numbers.saturatedBinomial(5, 0));
        assertEquals(5, Numbers.saturatedBinomial(5, 1));
        assertEquals(10, Numbers.saturatedBinomial(5, 2));
        assertEquals(10, Numbers.saturatedBinomial(5, 3));
        assertEquals(5, Numbers.saturatedBinomial(5, 4));
        assertEquals(1, Numbers.saturatedBinomial(5, 5));
        assertEquals(252, Numbers.saturatedBinomial(10, 5));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedBinomial(34, 17));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedBinomial(40, 20));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedBinomial(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedBinomial(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedBinomial(5, 6));
    }

    @Test
    public void testBinomialExact() {
        assertEquals(10, Numbers.binomialExact(5, 2));
        assertEquals(252, Numbers.binomialExact(10, 5));
        assertEquals("binomialExact(100, 50) overflow", assertThrows(ArithmeticException.class, () -> Numbers.binomialExact(100, 50)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialExact(5, 6));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialExact(-1, 0));
    }

    // ===== saturatedBinomialToLong / binomialExactToLong =====

    @Test
    public void testSaturatedBinomialToLong() {
        assertEquals(1L, Numbers.saturatedBinomialToLong(0, 0));
        assertEquals(1L, Numbers.saturatedBinomialToLong(10, 0));
        assertEquals(1L, Numbers.saturatedBinomialToLong(10, 10));
        assertEquals(10L, Numbers.saturatedBinomialToLong(5, 2));
        assertEquals(120L, Numbers.saturatedBinomialToLong(10, 3));
        assertEquals(252L, Numbers.saturatedBinomialToLong(10, 5));
        assertEquals(435L, Numbers.saturatedBinomialToLong(30, 2));
        assertEquals(2598960L, Numbers.saturatedBinomialToLong(52, 5));
        assertEquals(98280L, Numbers.saturatedBinomialToLong(28, 5));
        assertEquals(60L, Numbers.saturatedBinomialToLong(60, 59));
        assertEquals(60L, Numbers.saturatedBinomialToLong(60, 1));
        assertEquals(118264581564861424L, Numbers.saturatedBinomialToLong(60, 30));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedBinomialToLong(67, 33));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedBinomialToLong(68, 34));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedBinomialToLong(100, 50));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedBinomialToLong(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedBinomialToLong(5, 6));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedBinomialToLong(-1, 0));
    }

    @Test
    public void testBinomialExactToLong() {
        assertEquals(10L, Numbers.binomialExactToLong(5, 2));
        assertEquals(118264581564861424L, Numbers.binomialExactToLong(60, 30));
        assertEquals("binomialExactToLong(100, 50) overflow", assertThrows(ArithmeticException.class, () -> Numbers.binomialExactToLong(100, 50)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialExactToLong(5, 6));
    }

    @Test
    public void testBinomial_saturatingAndThrowingVariantsAgree() {
        for (int n = 0; n <= 200; n++) {
            for (int k = 0; k <= n; k++) {
                final int saturated = Numbers.saturatedBinomial(n, k);
                if (saturated == Integer.MAX_VALUE) {
                    final int fn = n;
                    final int fk = k;
                    assertThrows(ArithmeticException.class, () -> Numbers.binomialExact(fn, fk), "C(" + n + ", " + k + ")");
                } else {
                    assertEquals(saturated, Numbers.binomialExact(n, k));
                    assertEquals(BigInteger.valueOf(saturated), Numbers.binomialToBigInteger(n, k));
                }

                final long saturatedLong = Numbers.saturatedBinomialToLong(n, k);
                if (saturatedLong == Long.MAX_VALUE) {
                    final int fn = n;
                    final int fk = k;
                    assertThrows(ArithmeticException.class, () -> Numbers.binomialExactToLong(fn, fk), "C(" + n + ", " + k + ")");
                } else {
                    assertEquals(saturatedLong, Numbers.binomialExactToLong(n, k));
                    assertEquals(BigInteger.valueOf(saturatedLong), Numbers.binomialToBigInteger(n, k));
                }
            }
        }
    }

    // ===== binomialToDouble =====

    @Test
    public void testBinomialToDouble() {
        assertEquals(1.0, Numbers.binomialToDouble(0, 0), 0.0);
        assertEquals(1.0, Numbers.binomialToDouble(10, 0), 0.0);
        assertEquals(10.0, Numbers.binomialToDouble(5, 2), 0.0);
        assertEquals(252.0, Numbers.binomialToDouble(10, 5), 0.0);
        assertEquals(2598960.0, Numbers.binomialToDouble(52, 5), 0.0);
        assertEquals(1023.0d, Numbers.binomialToDouble(1023, 1), 0.0d);

        final double c100_50 = Numbers.binomialToDouble(100, 50);
        assertTrue(c100_50 > 1.0e29 && c100_50 < 2.0e29);
        assertEquals(Numbers.binomialToBigInteger(1000, 500).doubleValue(), Numbers.binomialToDouble(1000, 500), 0.0);

        final BigInteger c66_33 = Numbers.binomialToBigInteger(66, 33);
        final BigInteger c67_33 = Numbers.binomialToBigInteger(67, 33);
        assertEquals(new BigInteger("7219428434016265740"), c66_33);
        assertEquals(new BigInteger("14226520737620288370"), c67_33);
        assertEquals(c66_33.doubleValue(), Numbers.binomialToDouble(66, 33), 0.0d);
        assertEquals(c67_33.doubleValue(), Numbers.binomialToDouble(67, 33), 0.0d);

        final BigInteger c1029_514 = Numbers.binomialToBigInteger(1029, 514);
        assertEquals(1024, c1029_514.bitLength());
        assertEquals(c1029_514.doubleValue(), Numbers.binomialToDouble(1029, 514), 0.0d);

        assertEquals(Double.POSITIVE_INFINITY, Numbers.binomialToDouble(2000, 1000), 0.0);
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToDouble(5, 6));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToDouble(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToDouble(5, -1));
    }

    @Test
    public void testBinomialToDouble_Overflow() {
        assertEquals(Double.POSITIVE_INFINITY, Numbers.binomialToDouble(20_000, 10_000), 0.0d);
        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertEquals(Double.POSITIVE_INFINITY, Numbers.binomialToDouble(Integer.MAX_VALUE, 1 << 30), 0.0d));
    }

    // ===== binomialToBigInteger =====

    @Test
    public void testBinomialToBigInteger() {
        assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(0, 0));
        assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(5, 0));
        assertEquals(BigInteger.ONE, Numbers.binomialToBigInteger(100, 0));
        assertEquals(BigInteger.valueOf(5), Numbers.binomialToBigInteger(5, 1));
        assertEquals(BigInteger.valueOf(10), Numbers.binomialToBigInteger(5, 2));
        assertEquals(BigInteger.valueOf(100), Numbers.binomialToBigInteger(100, 1));
        assertEquals(BigInteger.valueOf(252), Numbers.binomialToBigInteger(10, 5));
        assertEquals(BigInteger.valueOf(184756), Numbers.binomialToBigInteger(20, 10));
        assertEquals(new BigInteger("61474519"), Numbers.binomialToBigInteger(62, 6));

        final BigInteger c100_50 = Numbers.binomialToBigInteger(100, 50);
        assertTrue(c100_50.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
        assertEquals(Numbers.factorialToBigInteger(100).divide(Numbers.factorialToBigInteger(50).multiply(Numbers.factorialToBigInteger(50))), c100_50);

        assertEquals(new BigInteger("4495501000"), Numbers.binomialToBigInteger(3000, 3));
        assertEquals(BigInteger.valueOf(Numbers.saturatedBinomialToLong(3000, 3)), Numbers.binomialToBigInteger(3000, 3));
        assertEquals(new BigInteger("2449965000"), Numbers.binomialToBigInteger(70000, 2));

        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(5, -1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(5, 6));
    }
}
