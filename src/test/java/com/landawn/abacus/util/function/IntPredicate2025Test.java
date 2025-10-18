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
public class IntPredicate2025Test extends TestBase {

    @Test
    public void test_ALWAYS_TRUE() {
        assertTrue(IntPredicate.ALWAYS_TRUE.test(0));
        assertTrue(IntPredicate.ALWAYS_TRUE.test(100));
        assertTrue(IntPredicate.ALWAYS_TRUE.test(-100));
        assertTrue(IntPredicate.ALWAYS_TRUE.test(Integer.MAX_VALUE));
        assertTrue(IntPredicate.ALWAYS_TRUE.test(Integer.MIN_VALUE));
    }

    @Test
    public void test_ALWAYS_FALSE() {
        assertFalse(IntPredicate.ALWAYS_FALSE.test(0));
        assertFalse(IntPredicate.ALWAYS_FALSE.test(100));
        assertFalse(IntPredicate.ALWAYS_FALSE.test(-100));
        assertFalse(IntPredicate.ALWAYS_FALSE.test(Integer.MAX_VALUE));
        assertFalse(IntPredicate.ALWAYS_FALSE.test(Integer.MIN_VALUE));
    }

    @Test
    public void test_IS_ZERO() {
        assertTrue(IntPredicate.IS_ZERO.test(0));
        assertFalse(IntPredicate.IS_ZERO.test(1));
        assertFalse(IntPredicate.IS_ZERO.test(-1));
        assertFalse(IntPredicate.IS_ZERO.test(Integer.MAX_VALUE));
        assertFalse(IntPredicate.IS_ZERO.test(Integer.MIN_VALUE));
    }

    @Test
    public void test_NOT_ZERO() {
        assertFalse(IntPredicate.NOT_ZERO.test(0));
        assertTrue(IntPredicate.NOT_ZERO.test(1));
        assertTrue(IntPredicate.NOT_ZERO.test(-1));
        assertTrue(IntPredicate.NOT_ZERO.test(Integer.MAX_VALUE));
        assertTrue(IntPredicate.NOT_ZERO.test(Integer.MIN_VALUE));
    }

    @Test
    public void test_IS_POSITIVE() {
        assertTrue(IntPredicate.IS_POSITIVE.test(1));
        assertTrue(IntPredicate.IS_POSITIVE.test(100));
        assertTrue(IntPredicate.IS_POSITIVE.test(Integer.MAX_VALUE));
        assertFalse(IntPredicate.IS_POSITIVE.test(0));
        assertFalse(IntPredicate.IS_POSITIVE.test(-1));
        assertFalse(IntPredicate.IS_POSITIVE.test(Integer.MIN_VALUE));
    }

    @Test
    public void test_NOT_POSITIVE() {
        assertFalse(IntPredicate.NOT_POSITIVE.test(1));
        assertFalse(IntPredicate.NOT_POSITIVE.test(100));
        assertFalse(IntPredicate.NOT_POSITIVE.test(Integer.MAX_VALUE));
        assertTrue(IntPredicate.NOT_POSITIVE.test(0));
        assertTrue(IntPredicate.NOT_POSITIVE.test(-1));
        assertTrue(IntPredicate.NOT_POSITIVE.test(Integer.MIN_VALUE));
    }

    @Test
    public void test_IS_NEGATIVE() {
        assertTrue(IntPredicate.IS_NEGATIVE.test(-1));
        assertTrue(IntPredicate.IS_NEGATIVE.test(-100));
        assertTrue(IntPredicate.IS_NEGATIVE.test(Integer.MIN_VALUE));
        assertFalse(IntPredicate.IS_NEGATIVE.test(0));
        assertFalse(IntPredicate.IS_NEGATIVE.test(1));
        assertFalse(IntPredicate.IS_NEGATIVE.test(Integer.MAX_VALUE));
    }

    @Test
    public void test_NOT_NEGATIVE() {
        assertFalse(IntPredicate.NOT_NEGATIVE.test(-1));
        assertFalse(IntPredicate.NOT_NEGATIVE.test(-100));
        assertFalse(IntPredicate.NOT_NEGATIVE.test(Integer.MIN_VALUE));
        assertTrue(IntPredicate.NOT_NEGATIVE.test(0));
        assertTrue(IntPredicate.NOT_NEGATIVE.test(1));
        assertTrue(IntPredicate.NOT_NEGATIVE.test(Integer.MAX_VALUE));
    }

    @Test
    public void test_test_lambda() {
        IntPredicate isEven = value -> value % 2 == 0;

        assertTrue(isEven.test(2));
        assertTrue(isEven.test(100));
        assertFalse(isEven.test(3));
        assertFalse(isEven.test(99));
    }

    @Test
    public void test_test_anonymousClass() {
        IntPredicate isPositiveEven = new IntPredicate() {
            @Override
            public boolean test(int value) {
                return value > 0 && value % 2 == 0;
            }
        };

        assertTrue(isPositiveEven.test(2));
        assertTrue(isPositiveEven.test(100));
        assertFalse(isPositiveEven.test(3));
        assertFalse(isPositiveEven.test(-2));
    }

    @Test
    public void test_of() {
        IntPredicate original = value -> value > 10;
        IntPredicate returned = IntPredicate.of(original);

        assertSame(original, returned);
    }

    @Test
    public void test_negate() {
        IntPredicate isPositive = value -> value > 0;
        IntPredicate isNotPositive = isPositive.negate();

        assertTrue(isPositive.test(5));
        assertFalse(isNotPositive.test(5));
        assertFalse(isPositive.test(-5));
        assertTrue(isNotPositive.test(-5));
    }

    @Test
    public void test_and() {
        IntPredicate isPositive = value -> value > 0;
        IntPredicate isEven = value -> value % 2 == 0;
        IntPredicate isPositiveEven = isPositive.and(isEven);

        assertTrue(isPositiveEven.test(2));
        assertTrue(isPositiveEven.test(100));
        assertFalse(isPositiveEven.test(3));
        assertFalse(isPositiveEven.test(-2));
    }

    @Test
    public void test_or() {
        IntPredicate isNegative = value -> value < 0;
        IntPredicate isEven = value -> value % 2 == 0;
        IntPredicate isNegativeOrEven = isNegative.or(isEven);

        assertTrue(isNegativeOrEven.test(-1));
        assertTrue(isNegativeOrEven.test(2));
        assertTrue(isNegativeOrEven.test(-2));
        assertFalse(isNegativeOrEven.test(1));
        assertFalse(isNegativeOrEven.test(3));
    }

    @Test
    public void test_equal() {
        IntPredicate equalToTen = IntPredicate.equal(10);

        assertTrue(equalToTen.test(10));
        assertFalse(equalToTen.test(9));
        assertFalse(equalToTen.test(11));
    }

    @Test
    public void test_notEqual() {
        IntPredicate notEqualToTen = IntPredicate.notEqual(10);

        assertFalse(notEqualToTen.test(10));
        assertTrue(notEqualToTen.test(9));
        assertTrue(notEqualToTen.test(11));
    }

    @Test
    public void test_greaterThan() {
        IntPredicate greaterThanTen = IntPredicate.greaterThan(10);

        assertTrue(greaterThanTen.test(11));
        assertTrue(greaterThanTen.test(100));
        assertFalse(greaterThanTen.test(10));
        assertFalse(greaterThanTen.test(9));
    }

    @Test
    public void test_greaterEqual() {
        IntPredicate greaterEqualToTen = IntPredicate.greaterEqual(10);

        assertTrue(greaterEqualToTen.test(11));
        assertTrue(greaterEqualToTen.test(10));
        assertTrue(greaterEqualToTen.test(100));
        assertFalse(greaterEqualToTen.test(9));
    }

    @Test
    public void test_lessThan() {
        IntPredicate lessThanTen = IntPredicate.lessThan(10);

        assertTrue(lessThanTen.test(9));
        assertTrue(lessThanTen.test(-100));
        assertFalse(lessThanTen.test(10));
        assertFalse(lessThanTen.test(11));
    }

    @Test
    public void test_lessEqual() {
        IntPredicate lessEqualToTen = IntPredicate.lessEqual(10);

        assertTrue(lessEqualToTen.test(9));
        assertTrue(lessEqualToTen.test(10));
        assertTrue(lessEqualToTen.test(-100));
        assertFalse(lessEqualToTen.test(11));
    }

    @Test
    public void test_between() {
        IntPredicate betweenTenAndTwenty = IntPredicate.between(10, 20);

        assertTrue(betweenTenAndTwenty.test(11));
        assertTrue(betweenTenAndTwenty.test(15));
        assertTrue(betweenTenAndTwenty.test(19));
        assertFalse(betweenTenAndTwenty.test(10));
        assertFalse(betweenTenAndTwenty.test(20));
        assertFalse(betweenTenAndTwenty.test(9));
        assertFalse(betweenTenAndTwenty.test(21));
    }
}
