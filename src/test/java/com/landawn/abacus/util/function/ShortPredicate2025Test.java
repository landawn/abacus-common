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
public class ShortPredicate2025Test extends TestBase {

    @Test
    public void test_ALWAYS_TRUE() {
        assertTrue(ShortPredicate.ALWAYS_TRUE.test((short) 0));
        assertTrue(ShortPredicate.ALWAYS_TRUE.test((short) 100));
        assertTrue(ShortPredicate.ALWAYS_TRUE.test((short) -100));
        assertTrue(ShortPredicate.ALWAYS_TRUE.test(Short.MAX_VALUE));
        assertTrue(ShortPredicate.ALWAYS_TRUE.test(Short.MIN_VALUE));
    }

    @Test
    public void test_ALWAYS_FALSE() {
        assertFalse(ShortPredicate.ALWAYS_FALSE.test((short) 0));
        assertFalse(ShortPredicate.ALWAYS_FALSE.test((short) 100));
        assertFalse(ShortPredicate.ALWAYS_FALSE.test((short) -100));
        assertFalse(ShortPredicate.ALWAYS_FALSE.test(Short.MAX_VALUE));
        assertFalse(ShortPredicate.ALWAYS_FALSE.test(Short.MIN_VALUE));
    }

    @Test
    public void test_IS_ZERO() {
        assertTrue(ShortPredicate.IS_ZERO.test((short) 0));
        assertFalse(ShortPredicate.IS_ZERO.test((short) 1));
        assertFalse(ShortPredicate.IS_ZERO.test((short) -1));
        assertFalse(ShortPredicate.IS_ZERO.test(Short.MAX_VALUE));
        assertFalse(ShortPredicate.IS_ZERO.test(Short.MIN_VALUE));
    }

    @Test
    public void test_NOT_ZERO() {
        assertFalse(ShortPredicate.NOT_ZERO.test((short) 0));
        assertTrue(ShortPredicate.NOT_ZERO.test((short) 1));
        assertTrue(ShortPredicate.NOT_ZERO.test((short) -1));
        assertTrue(ShortPredicate.NOT_ZERO.test(Short.MAX_VALUE));
        assertTrue(ShortPredicate.NOT_ZERO.test(Short.MIN_VALUE));
    }

    @Test
    public void test_IS_POSITIVE() {
        assertTrue(ShortPredicate.IS_POSITIVE.test((short) 1));
        assertTrue(ShortPredicate.IS_POSITIVE.test((short) 100));
        assertTrue(ShortPredicate.IS_POSITIVE.test(Short.MAX_VALUE));
        assertFalse(ShortPredicate.IS_POSITIVE.test((short) 0));
        assertFalse(ShortPredicate.IS_POSITIVE.test((short) -1));
        assertFalse(ShortPredicate.IS_POSITIVE.test(Short.MIN_VALUE));
    }

    @Test
    public void test_NOT_POSITIVE() {
        assertFalse(ShortPredicate.NOT_POSITIVE.test((short) 1));
        assertFalse(ShortPredicate.NOT_POSITIVE.test((short) 100));
        assertFalse(ShortPredicate.NOT_POSITIVE.test(Short.MAX_VALUE));
        assertTrue(ShortPredicate.NOT_POSITIVE.test((short) 0));
        assertTrue(ShortPredicate.NOT_POSITIVE.test((short) -1));
        assertTrue(ShortPredicate.NOT_POSITIVE.test(Short.MIN_VALUE));
    }

    @Test
    public void test_IS_NEGATIVE() {
        assertTrue(ShortPredicate.IS_NEGATIVE.test((short) -1));
        assertTrue(ShortPredicate.IS_NEGATIVE.test((short) -100));
        assertTrue(ShortPredicate.IS_NEGATIVE.test(Short.MIN_VALUE));
        assertFalse(ShortPredicate.IS_NEGATIVE.test((short) 0));
        assertFalse(ShortPredicate.IS_NEGATIVE.test((short) 1));
        assertFalse(ShortPredicate.IS_NEGATIVE.test(Short.MAX_VALUE));
    }

    @Test
    public void test_NOT_NEGATIVE() {
        assertFalse(ShortPredicate.NOT_NEGATIVE.test((short) -1));
        assertFalse(ShortPredicate.NOT_NEGATIVE.test((short) -100));
        assertFalse(ShortPredicate.NOT_NEGATIVE.test(Short.MIN_VALUE));
        assertTrue(ShortPredicate.NOT_NEGATIVE.test((short) 0));
        assertTrue(ShortPredicate.NOT_NEGATIVE.test((short) 1));
        assertTrue(ShortPredicate.NOT_NEGATIVE.test(Short.MAX_VALUE));
    }

    @Test
    public void test_test_lambda() {
        ShortPredicate isEven = value -> value % 2 == 0;

        assertTrue(isEven.test((short) 2));
        assertTrue(isEven.test((short) 100));
        assertFalse(isEven.test((short) 3));
        assertFalse(isEven.test((short) 99));
    }

    @Test
    public void test_test_anonymousClass() {
        ShortPredicate isPositiveEven = new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return value > 0 && value % 2 == 0;
            }
        };

        assertTrue(isPositiveEven.test((short) 2));
        assertTrue(isPositiveEven.test((short) 100));
        assertFalse(isPositiveEven.test((short) 3));
        assertFalse(isPositiveEven.test((short) -2));
    }

    @Test
    public void test_of() {
        ShortPredicate original = value -> value > 10;
        ShortPredicate returned = ShortPredicate.of(original);

        assertSame(original, returned);
    }

    @Test
    public void test_negate() {
        ShortPredicate isPositive = value -> value > 0;
        ShortPredicate isNotPositive = isPositive.negate();

        assertTrue(isPositive.test((short) 5));
        assertFalse(isNotPositive.test((short) 5));
        assertFalse(isPositive.test((short) -5));
        assertTrue(isNotPositive.test((short) -5));
    }

    @Test
    public void test_and() {
        ShortPredicate isPositive = value -> value > 0;
        ShortPredicate isEven = value -> value % 2 == 0;
        ShortPredicate isPositiveEven = isPositive.and(isEven);

        assertTrue(isPositiveEven.test((short) 2));
        assertTrue(isPositiveEven.test((short) 100));
        assertFalse(isPositiveEven.test((short) 3));
        assertFalse(isPositiveEven.test((short) -2));
    }

    @Test
    public void test_and_shortCircuit() {
        final boolean[] secondCalled = { false };

        ShortPredicate alwaysFalse = value -> false;
        ShortPredicate trackCalls = value -> {
            secondCalled[0] = true;
            return true;
        };

        ShortPredicate combined = alwaysFalse.and(trackCalls);
        assertFalse(combined.test((short) 1));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void test_or() {
        ShortPredicate isNegative = value -> value < 0;
        ShortPredicate isEven = value -> value % 2 == 0;
        ShortPredicate isNegativeOrEven = isNegative.or(isEven);

        assertTrue(isNegativeOrEven.test((short) -1));
        assertTrue(isNegativeOrEven.test((short) 2));
        assertTrue(isNegativeOrEven.test((short) -2));
        assertFalse(isNegativeOrEven.test((short) 1));
        assertFalse(isNegativeOrEven.test((short) 3));
    }

    @Test
    public void test_or_shortCircuit() {
        final boolean[] secondCalled = { false };

        ShortPredicate alwaysTrue = value -> true;
        ShortPredicate trackCalls = value -> {
            secondCalled[0] = true;
            return false;
        };

        ShortPredicate combined = alwaysTrue.or(trackCalls);
        assertTrue(combined.test((short) 1));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void test_equal() {
        ShortPredicate equalToTen = ShortPredicate.equal((short) 10);

        assertTrue(equalToTen.test((short) 10));
        assertFalse(equalToTen.test((short) 9));
        assertFalse(equalToTen.test((short) 11));
        assertFalse(equalToTen.test((short) -10));
    }

    @Test
    public void test_notEqual() {
        ShortPredicate notEqualToTen = ShortPredicate.notEqual((short) 10);

        assertFalse(notEqualToTen.test((short) 10));
        assertTrue(notEqualToTen.test((short) 9));
        assertTrue(notEqualToTen.test((short) 11));
        assertTrue(notEqualToTen.test((short) -10));
    }

    @Test
    public void test_greaterThan() {
        ShortPredicate greaterThanTen = ShortPredicate.greaterThan((short) 10);

        assertTrue(greaterThanTen.test((short) 11));
        assertTrue(greaterThanTen.test((short) 100));
        assertFalse(greaterThanTen.test((short) 10));
        assertFalse(greaterThanTen.test((short) 9));
        assertFalse(greaterThanTen.test((short) -10));
    }

    @Test
    public void test_greaterEqual() {
        ShortPredicate greaterEqualToTen = ShortPredicate.greaterEqual((short) 10);

        assertTrue(greaterEqualToTen.test((short) 11));
        assertTrue(greaterEqualToTen.test((short) 10));
        assertTrue(greaterEqualToTen.test((short) 100));
        assertFalse(greaterEqualToTen.test((short) 9));
        assertFalse(greaterEqualToTen.test((short) -10));
    }

    @Test
    public void test_lessThan() {
        ShortPredicate lessThanTen = ShortPredicate.lessThan((short) 10);

        assertTrue(lessThanTen.test((short) 9));
        assertTrue(lessThanTen.test((short) -100));
        assertFalse(lessThanTen.test((short) 10));
        assertFalse(lessThanTen.test((short) 11));
        assertFalse(lessThanTen.test((short) 100));
    }

    @Test
    public void test_lessEqual() {
        ShortPredicate lessEqualToTen = ShortPredicate.lessEqual((short) 10);

        assertTrue(lessEqualToTen.test((short) 9));
        assertTrue(lessEqualToTen.test((short) 10));
        assertTrue(lessEqualToTen.test((short) -100));
        assertFalse(lessEqualToTen.test((short) 11));
        assertFalse(lessEqualToTen.test((short) 100));
    }

    @Test
    public void test_between() {
        ShortPredicate betweenTenAndTwenty = ShortPredicate.between((short) 10, (short) 20);

        assertTrue(betweenTenAndTwenty.test((short) 11));
        assertTrue(betweenTenAndTwenty.test((short) 15));
        assertTrue(betweenTenAndTwenty.test((short) 19));
        assertFalse(betweenTenAndTwenty.test((short) 10));
        assertFalse(betweenTenAndTwenty.test((short) 20));
        assertFalse(betweenTenAndTwenty.test((short) 9));
        assertFalse(betweenTenAndTwenty.test((short) 21));
    }
}
