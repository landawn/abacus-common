/*
 * Copyright (C) 2025 HaiYang Li
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

package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ShortTernaryOperator2025Test extends TestBase {

    @Test
    public void test_applyAsShort_sum() {
        ShortTernaryOperator sum = (a, b, c) -> (short) (a + b + c);

        assertEquals(15, sum.applyAsShort((short) 3, (short) 5, (short) 7));
        assertEquals(0, sum.applyAsShort((short) 0, (short) 0, (short) 0));
        assertEquals(-15, sum.applyAsShort((short) -3, (short) -5, (short) -7));
    }

    @Test
    public void test_applyAsShort_product() {
        ShortTernaryOperator product = (a, b, c) -> (short) (a * b * c);

        assertEquals(24, product.applyAsShort((short) 2, (short) 3, (short) 4));
        assertEquals(0, product.applyAsShort((short) 0, (short) 5, (short) 10));
        assertEquals(-24, product.applyAsShort((short) 2, (short) 3, (short) -4));
    }

    @Test
    public void test_applyAsShort_average() {
        ShortTernaryOperator average = (a, b, c) -> (short) ((a + b + c) / 3);

        assertEquals(5, average.applyAsShort((short) 3, (short) 5, (short) 7));
        assertEquals(0, average.applyAsShort((short) 0, (short) 0, (short) 0));
        assertEquals(10, average.applyAsShort((short) 10, (short) 10, (short) 10));
    }

    @Test
    public void test_applyAsShort_max() {
        ShortTernaryOperator max = (a, b, c) -> {
            short temp = (short) Math.max(a, b);
            return (short) Math.max(temp, c);
        };

        assertEquals(10, max.applyAsShort((short) 5, (short) 10, (short) 3));
        assertEquals(10, max.applyAsShort((short) 10, (short) 5, (short) 3));
        assertEquals(10, max.applyAsShort((short) 3, (short) 5, (short) 10));
        assertEquals(0, max.applyAsShort((short) 0, (short) 0, (short) 0));
    }

    @Test
    public void test_applyAsShort_min() {
        ShortTernaryOperator min = (a, b, c) -> {
            short temp = (short) Math.min(a, b);
            return (short) Math.min(temp, c);
        };

        assertEquals(3, min.applyAsShort((short) 5, (short) 10, (short) 3));
        assertEquals(3, min.applyAsShort((short) 3, (short) 10, (short) 5));
        assertEquals(3, min.applyAsShort((short) 10, (short) 3, (short) 5));
        assertEquals(0, min.applyAsShort((short) 0, (short) 0, (short) 0));
    }

    @Test
    public void test_applyAsShort_lambda() {
        ShortTernaryOperator sumMinusProduct = (a, b, c) -> (short) ((a + b + c) - (a * b * c));

        // Let me recalculate: (2+3+4) = 9, (2*3*4) = 24, 9 - 24 = -15
        assertEquals(-15, sumMinusProduct.applyAsShort((short) 2, (short) 3, (short) 4));
    }

    @Test
    public void test_applyAsShort_anonymousClass() {
        ShortTernaryOperator median = new ShortTernaryOperator() {
            @Override
            public short applyAsShort(short a, short b, short c) {
                // Simple median calculation
                short max = (short) Math.max(Math.max(a, b), c);
                short min = (short) Math.min(Math.min(a, b), c);
                return (short) (a + b + c - max - min);
            }
        };

        assertEquals(5, median.applyAsShort((short) 3, (short) 5, (short) 7));
        assertEquals(10, median.applyAsShort((short) 5, (short) 10, (short) 15));
        assertEquals(0, median.applyAsShort((short) -5, (short) 0, (short) 5));
    }

    @Test
    public void test_applyAsShort_withNegativeValues() {
        ShortTernaryOperator sum = (a, b, c) -> (short) (a + b + c);

        assertEquals(-15, sum.applyAsShort((short) -3, (short) -5, (short) -7));
        assertEquals(0, sum.applyAsShort((short) -5, (short) 2, (short) 3));
    }

    @Test
    public void test_applyAsShort_withZero() {
        ShortTernaryOperator product = (a, b, c) -> (short) (a * b * c);

        assertEquals(0, product.applyAsShort((short) 0, (short) 5, (short) 10));
        assertEquals(0, product.applyAsShort((short) 5, (short) 0, (short) 10));
        assertEquals(0, product.applyAsShort((short) 5, (short) 10, (short) 0));
    }

    @Test
    public void test_applyAsShort_bitwiseOperations() {
        ShortTernaryOperator bitwiseOr = (a, b, c) -> (short) (a | b | c);

        assertEquals(0x0F, bitwiseOr.applyAsShort((short) 0x01, (short) 0x02, (short) 0x0C));
        assertEquals(0xFF, bitwiseOr.applyAsShort((short) 0xF0, (short) 0x0F, (short) 0x00));
    }
}
