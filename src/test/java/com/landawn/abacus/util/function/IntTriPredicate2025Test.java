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
public class IntTriPredicate2025Test extends TestBase {

    @Test
    public void test_ALWAYS_TRUE() {
        assertTrue(IntTriPredicate.ALWAYS_TRUE.test(0, 0, 0));
        assertTrue(IntTriPredicate.ALWAYS_TRUE.test(1, 2, 3));
    }

    @Test
    public void test_ALWAYS_FALSE() {
        assertFalse(IntTriPredicate.ALWAYS_FALSE.test(0, 0, 0));
        assertFalse(IntTriPredicate.ALWAYS_FALSE.test(1, 2, 3));
    }

    @Test
    public void test_test_lambda() {
        IntTriPredicate sumIsPositive = (a, b, c) -> (a + b + c) > 0;

        assertTrue(sumIsPositive.test(1, 2, 3));
        assertFalse(sumIsPositive.test(-10, -2, -3));
    }

    @Test
    public void test_negate() {
        IntTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        IntTriPredicate notAllPositive = allPositive.negate();

        assertTrue(allPositive.test(1, 2, 3));
        assertFalse(notAllPositive.test(1, 2, 3));
    }

    @Test
    public void test_and() {
        IntTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        IntTriPredicate sumGreaterThanTen = (a, b, c) -> (a + b + c) > 10;
        IntTriPredicate combined = allPositive.and(sumGreaterThanTen);

        assertTrue(combined.test(4, 5, 6));
        assertFalse(combined.test(1, 1, 1));
    }

    @Test
    public void test_or() {
        IntTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
        IntTriPredicate sumIsEven = (a, b, c) -> (a + b + c) % 2 == 0;
        IntTriPredicate combined = anyZero.or(sumIsEven);

        assertTrue(combined.test(0, 1, 2));
        assertTrue(combined.test(1, 2, 3));
    }
}
