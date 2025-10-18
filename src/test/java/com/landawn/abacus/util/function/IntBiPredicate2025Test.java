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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IntBiPredicate2025Test extends TestBase {

    @Test
    public void test_ALWAYS_TRUE() {
        assertTrue(IntBiPredicate.ALWAYS_TRUE.test(0, 0));
        assertTrue(IntBiPredicate.ALWAYS_TRUE.test(100, 200));
        assertTrue(IntBiPredicate.ALWAYS_TRUE.test(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void test_ALWAYS_FALSE() {
        assertFalse(IntBiPredicate.ALWAYS_FALSE.test(0, 0));
        assertFalse(IntBiPredicate.ALWAYS_FALSE.test(100, 200));
        assertFalse(IntBiPredicate.ALWAYS_FALSE.test(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void test_EQUAL() {
        assertTrue(IntBiPredicate.EQUAL.test(10, 10));
        assertTrue(IntBiPredicate.EQUAL.test(0, 0));
        assertTrue(IntBiPredicate.EQUAL.test(-5, -5));
        assertFalse(IntBiPredicate.EQUAL.test(10, 20));
        assertFalse(IntBiPredicate.EQUAL.test(-5, 5));
    }

    @Test
    public void test_NOT_EQUAL() {
        assertFalse(IntBiPredicate.NOT_EQUAL.test(10, 10));
        assertFalse(IntBiPredicate.NOT_EQUAL.test(0, 0));
        assertTrue(IntBiPredicate.NOT_EQUAL.test(10, 20));
        assertTrue(IntBiPredicate.NOT_EQUAL.test(-5, 5));
    }

    @Test
    public void test_GREATER_THAN() {
        assertTrue(IntBiPredicate.GREATER_THAN.test(20, 10));
        assertTrue(IntBiPredicate.GREATER_THAN.test(5, -5));
        assertFalse(IntBiPredicate.GREATER_THAN.test(10, 10));
        assertFalse(IntBiPredicate.GREATER_THAN.test(10, 20));
    }

    @Test
    public void test_GREATER_EQUAL() {
        assertTrue(IntBiPredicate.GREATER_EQUAL.test(20, 10));
        assertTrue(IntBiPredicate.GREATER_EQUAL.test(10, 10));
        assertTrue(IntBiPredicate.GREATER_EQUAL.test(5, -5));
        assertFalse(IntBiPredicate.GREATER_EQUAL.test(10, 20));
    }

    @Test
    public void test_LESS_THAN() {
        assertTrue(IntBiPredicate.LESS_THAN.test(10, 20));
        assertTrue(IntBiPredicate.LESS_THAN.test(-5, 5));
        assertFalse(IntBiPredicate.LESS_THAN.test(10, 10));
        assertFalse(IntBiPredicate.LESS_THAN.test(20, 10));
    }

    @Test
    public void test_LESS_EQUAL() {
        assertTrue(IntBiPredicate.LESS_EQUAL.test(10, 20));
        assertTrue(IntBiPredicate.LESS_EQUAL.test(10, 10));
        assertTrue(IntBiPredicate.LESS_EQUAL.test(-5, 5));
        assertFalse(IntBiPredicate.LESS_EQUAL.test(20, 10));
    }

    @Test
    public void test_test_lambda() {
        IntBiPredicate sumIsEven = (t, u) -> (t + u) % 2 == 0;

        assertTrue(sumIsEven.test(2, 4));
        assertTrue(sumIsEven.test(1, 3));
        assertFalse(sumIsEven.test(1, 2));
    }

    @Test
    public void test_test_anonymousClass() {
        IntBiPredicate productIsPositive = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return ((long) t * u) > 0;
            }
        };

        assertTrue(productIsPositive.test(2, 3));
        assertTrue(productIsPositive.test(-2, -3));
        assertFalse(productIsPositive.test(2, -3));
        assertFalse(productIsPositive.test(0, 5));
    }

    @Test
    public void test_negate() {
        IntBiPredicate equal = (t, u) -> t == u;
        IntBiPredicate notEqual = equal.negate();

        assertTrue(equal.test(5, 5));
        assertFalse(notEqual.test(5, 5));
        assertFalse(equal.test(5, 10));
        assertTrue(notEqual.test(5, 10));
    }

    @Test
    public void test_and() {
        IntBiPredicate bothPositive = (t, u) -> t > 0 && u > 0;
        IntBiPredicate sumGreaterThanTen = (t, u) -> (t + u) > 10;
        IntBiPredicate combined = bothPositive.and(sumGreaterThanTen);

        assertTrue(combined.test(6, 6));
        assertFalse(combined.test(2, 2));
        assertFalse(combined.test(-6, 20));
    }

    @Test
    public void test_or() {
        IntBiPredicate firstIsZero = (t, u) -> t == 0;
        IntBiPredicate secondIsZero = (t, u) -> u == 0;
        IntBiPredicate combined = firstIsZero.or(secondIsZero);

        assertTrue(combined.test(0, 5));
        assertTrue(combined.test(5, 0));
        assertTrue(combined.test(0, 0));
        assertFalse(combined.test(5, 5));
    }
}
