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
public class ShortBiPredicate2025Test extends TestBase {

    @Test
    public void test_ALWAYS_TRUE() {
        assertTrue(ShortBiPredicate.ALWAYS_TRUE.test((short) 0, (short) 0));
        assertTrue(ShortBiPredicate.ALWAYS_TRUE.test((short) 100, (short) 200));
        assertTrue(ShortBiPredicate.ALWAYS_TRUE.test(Short.MAX_VALUE, Short.MIN_VALUE));
    }

    @Test
    public void test_ALWAYS_FALSE() {
        assertFalse(ShortBiPredicate.ALWAYS_FALSE.test((short) 0, (short) 0));
        assertFalse(ShortBiPredicate.ALWAYS_FALSE.test((short) 100, (short) 200));
        assertFalse(ShortBiPredicate.ALWAYS_FALSE.test(Short.MAX_VALUE, Short.MIN_VALUE));
    }

    @Test
    public void test_EQUAL() {
        assertTrue(ShortBiPredicate.EQUAL.test((short) 10, (short) 10));
        assertTrue(ShortBiPredicate.EQUAL.test((short) 0, (short) 0));
        assertTrue(ShortBiPredicate.EQUAL.test((short) -5, (short) -5));
        assertFalse(ShortBiPredicate.EQUAL.test((short) 10, (short) 20));
        assertFalse(ShortBiPredicate.EQUAL.test((short) -5, (short) 5));
    }

    @Test
    public void test_NOT_EQUAL() {
        assertFalse(ShortBiPredicate.NOT_EQUAL.test((short) 10, (short) 10));
        assertFalse(ShortBiPredicate.NOT_EQUAL.test((short) 0, (short) 0));
        assertTrue(ShortBiPredicate.NOT_EQUAL.test((short) 10, (short) 20));
        assertTrue(ShortBiPredicate.NOT_EQUAL.test((short) -5, (short) 5));
    }

    @Test
    public void test_GREATER_THAN() {
        assertTrue(ShortBiPredicate.GREATER_THAN.test((short) 20, (short) 10));
        assertTrue(ShortBiPredicate.GREATER_THAN.test((short) 5, (short) -5));
        assertFalse(ShortBiPredicate.GREATER_THAN.test((short) 10, (short) 10));
        assertFalse(ShortBiPredicate.GREATER_THAN.test((short) 10, (short) 20));
    }

    @Test
    public void test_GREATER_EQUAL() {
        assertTrue(ShortBiPredicate.GREATER_EQUAL.test((short) 20, (short) 10));
        assertTrue(ShortBiPredicate.GREATER_EQUAL.test((short) 10, (short) 10));
        assertTrue(ShortBiPredicate.GREATER_EQUAL.test((short) 5, (short) -5));
        assertFalse(ShortBiPredicate.GREATER_EQUAL.test((short) 10, (short) 20));
    }

    @Test
    public void test_LESS_THAN() {
        assertTrue(ShortBiPredicate.LESS_THAN.test((short) 10, (short) 20));
        assertTrue(ShortBiPredicate.LESS_THAN.test((short) -5, (short) 5));
        assertFalse(ShortBiPredicate.LESS_THAN.test((short) 10, (short) 10));
        assertFalse(ShortBiPredicate.LESS_THAN.test((short) 20, (short) 10));
    }

    @Test
    public void test_LESS_EQUAL() {
        assertTrue(ShortBiPredicate.LESS_EQUAL.test((short) 10, (short) 20));
        assertTrue(ShortBiPredicate.LESS_EQUAL.test((short) 10, (short) 10));
        assertTrue(ShortBiPredicate.LESS_EQUAL.test((short) -5, (short) 5));
        assertFalse(ShortBiPredicate.LESS_EQUAL.test((short) 20, (short) 10));
    }

    @Test
    public void test_test_lambda() {
        ShortBiPredicate sumIsEven = (t, u) -> (t + u) % 2 == 0;

        assertTrue(sumIsEven.test((short) 2, (short) 4));
        assertTrue(sumIsEven.test((short) 1, (short) 3));
        assertFalse(sumIsEven.test((short) 1, (short) 2));
    }

    @Test
    public void test_test_anonymousClass() {
        ShortBiPredicate productIsPositive = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return (t * u) > 0;
            }
        };

        assertTrue(productIsPositive.test((short) 2, (short) 3));
        assertTrue(productIsPositive.test((short) -2, (short) -3));
        assertFalse(productIsPositive.test((short) 2, (short) -3));
        assertFalse(productIsPositive.test((short) 0, (short) 5));
    }

    @Test
    public void test_negate() {
        ShortBiPredicate equal = (t, u) -> t == u;
        ShortBiPredicate notEqual = equal.negate();

        assertTrue(equal.test((short) 5, (short) 5));
        assertFalse(notEqual.test((short) 5, (short) 5));
        assertFalse(equal.test((short) 5, (short) 10));
        assertTrue(notEqual.test((short) 5, (short) 10));
    }

    @Test
    public void test_and() {
        ShortBiPredicate bothPositive = (t, u) -> t > 0 && u > 0;
        ShortBiPredicate sumGreaterThanTen = (t, u) -> (t + u) > 10;
        ShortBiPredicate combined = bothPositive.and(sumGreaterThanTen);

        assertTrue(combined.test((short) 6, (short) 6));
        assertFalse(combined.test((short) 2, (short) 2)); // positive but sum not > 10
        assertFalse(combined.test((short) -6, (short) 20)); // sum > 10 but not both positive
        assertFalse(combined.test((short) -5, (short) -5)); // neither condition
    }

    @Test
    public void test_and_shortCircuit() {
        final boolean[] secondCalled = { false };

        ShortBiPredicate alwaysFalse = (t, u) -> false;
        ShortBiPredicate trackCalls = (t, u) -> {
            secondCalled[0] = true;
            return true;
        };

        ShortBiPredicate combined = alwaysFalse.and(trackCalls);
        assertFalse(combined.test((short) 1, (short) 2));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void test_or() {
        ShortBiPredicate firstIsZero = (t, u) -> t == 0;
        ShortBiPredicate secondIsZero = (t, u) -> u == 0;
        ShortBiPredicate combined = firstIsZero.or(secondIsZero);

        assertTrue(combined.test((short) 0, (short) 5));
        assertTrue(combined.test((short) 5, (short) 0));
        assertTrue(combined.test((short) 0, (short) 0));
        assertFalse(combined.test((short) 5, (short) 5));
    }

    @Test
    public void test_or_shortCircuit() {
        final boolean[] secondCalled = { false };

        ShortBiPredicate alwaysTrue = (t, u) -> true;
        ShortBiPredicate trackCalls = (t, u) -> {
            secondCalled[0] = true;
            return false;
        };

        ShortBiPredicate combined = alwaysTrue.or(trackCalls);
        assertTrue(combined.test((short) 1, (short) 2));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void test_complexCombination() {
        ShortBiPredicate bothEven = (t, u) -> t % 2 == 0 && u % 2 == 0;
        ShortBiPredicate sumLessThanHundred = (t, u) -> (t + u) < 100;
        ShortBiPredicate firstPositive = (t, u) -> t > 0;

        ShortBiPredicate complex = bothEven.and(sumLessThanHundred).or(firstPositive.negate());

        assertTrue(complex.test((short) 10, (short) 20)); // both even and sum < 100
        assertTrue(complex.test((short) -5, (short) 100)); // first not positive
        assertFalse(complex.test((short) 10, (short) 100)); // even but sum >= 100, and first is positive
    }
}
