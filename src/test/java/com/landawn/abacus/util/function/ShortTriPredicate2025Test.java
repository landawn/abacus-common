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
public class ShortTriPredicate2025Test extends TestBase {

    @Test
    public void test_ALWAYS_TRUE() {
        assertTrue(ShortTriPredicate.ALWAYS_TRUE.test((short) 0, (short) 0, (short) 0));
        assertTrue(ShortTriPredicate.ALWAYS_TRUE.test((short) 1, (short) 2, (short) 3));
        assertTrue(ShortTriPredicate.ALWAYS_TRUE.test(Short.MAX_VALUE, (short) 0, Short.MIN_VALUE));
    }

    @Test
    public void test_ALWAYS_FALSE() {
        assertFalse(ShortTriPredicate.ALWAYS_FALSE.test((short) 0, (short) 0, (short) 0));
        assertFalse(ShortTriPredicate.ALWAYS_FALSE.test((short) 1, (short) 2, (short) 3));
        assertFalse(ShortTriPredicate.ALWAYS_FALSE.test(Short.MAX_VALUE, (short) 0, Short.MIN_VALUE));
    }

    @Test
    public void test_test_lambda() {
        ShortTriPredicate sumIsPositive = (a, b, c) -> (a + b + c) > 0;

        assertTrue(sumIsPositive.test((short) 1, (short) 2, (short) 3));
        assertTrue(sumIsPositive.test((short) 10, (short) -2, (short) -3));
        assertFalse(sumIsPositive.test((short) -10, (short) -2, (short) -3));
        assertFalse(sumIsPositive.test((short) 0, (short) 0, (short) 0));
    }

    @Test
    public void test_test_anonymousClass() {
        ShortTriPredicate allEqual = new ShortTriPredicate() {
            @Override
            public boolean test(short a, short b, short c) {
                return a == b && b == c;
            }
        };

        assertTrue(allEqual.test((short) 5, (short) 5, (short) 5));
        assertTrue(allEqual.test((short) 0, (short) 0, (short) 0));
        assertFalse(allEqual.test((short) 1, (short) 2, (short) 3));
        assertFalse(allEqual.test((short) 5, (short) 5, (short) 6));
    }

    @Test
    public void test_negate() {
        ShortTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        ShortTriPredicate notAllPositive = allPositive.negate();

        assertTrue(allPositive.test((short) 1, (short) 2, (short) 3));
        assertFalse(notAllPositive.test((short) 1, (short) 2, (short) 3));
        assertFalse(allPositive.test((short) 1, (short) -2, (short) 3));
        assertTrue(notAllPositive.test((short) 1, (short) -2, (short) 3));
    }

    @Test
    public void test_and() {
        ShortTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        ShortTriPredicate sumGreaterThanTen = (a, b, c) -> (a + b + c) > 10;
        ShortTriPredicate combined = allPositive.and(sumGreaterThanTen);

        assertTrue(combined.test((short) 4, (short) 5, (short) 6));   // all positive and sum > 10
        assertFalse(combined.test((short) 1, (short) 1, (short) 1));   // all positive but sum not > 10
        assertFalse(combined.test((short) -1, (short) 10, (short) 10));   // sum > 10 but not all positive
    }

    @Test
    public void test_and_shortCircuit() {
        final boolean[] secondCalled = { false };

        ShortTriPredicate alwaysFalse = (a, b, c) -> false;
        ShortTriPredicate trackCalls = (a, b, c) -> {
            secondCalled[0] = true;
            return true;
        };

        ShortTriPredicate combined = alwaysFalse.and(trackCalls);
        assertFalse(combined.test((short) 1, (short) 2, (short) 3));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void test_or() {
        ShortTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
        ShortTriPredicate sumIsEven = (a, b, c) -> (a + b + c) % 2 == 0;
        ShortTriPredicate combined = anyZero.or(sumIsEven);

        assertTrue(combined.test((short) 0, (short) 1, (short) 2));   // has zero
        assertTrue(combined.test((short) 1, (short) 2, (short) 3));   // sum is even
        assertTrue(combined.test((short) 0, (short) 2, (short) 4));   // both conditions
        assertFalse(combined.test((short) 1, (short) 2, (short) 4));   // neither condition
    }

    @Test
    public void test_or_shortCircuit() {
        final boolean[] secondCalled = { false };

        ShortTriPredicate alwaysTrue = (a, b, c) -> true;
        ShortTriPredicate trackCalls = (a, b, c) -> {
            secondCalled[0] = true;
            return false;
        };

        ShortTriPredicate combined = alwaysTrue.or(trackCalls);
        assertTrue(combined.test((short) 1, (short) 2, (short) 3));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void test_complexCombination() {
        ShortTriPredicate allEven = (a, b, c) -> a % 2 == 0 && b % 2 == 0 && c % 2 == 0;
        ShortTriPredicate sumLessThanHundred = (a, b, c) -> (a + b + c) < 100;
        ShortTriPredicate firstPositive = (a, b, c) -> a > 0;

        ShortTriPredicate complex = allEven.and(sumLessThanHundred).or(firstPositive.negate());

        assertTrue(complex.test((short) 2, (short) 4, (short) 6));   // all even and sum < 100
        assertTrue(complex.test((short) -5, (short) 1, (short) 2));   // first not positive
        assertFalse(complex.test((short) 10, (short) 50, (short) 50));   // even but sum >= 100, and first is positive
    }
}
