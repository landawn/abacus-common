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
public class IntTernaryOperator2025Test extends TestBase {

    @Test
    public void test_applyAsInt_sum() {
        IntTernaryOperator sum = (a, b, c) -> a + b + c;

        assertEquals(15, sum.applyAsInt(3, 5, 7));
        assertEquals(0, sum.applyAsInt(0, 0, 0));
        assertEquals(-15, sum.applyAsInt(-3, -5, -7));
    }

    @Test
    public void test_applyAsInt_product() {
        IntTernaryOperator product = (a, b, c) -> a * b * c;

        assertEquals(24, product.applyAsInt(2, 3, 4));
        assertEquals(0, product.applyAsInt(0, 5, 10));
    }

    @Test
    public void test_applyAsInt_max() {
        IntTernaryOperator max = (a, b, c) -> {
            int temp = Math.max(a, b);
            return Math.max(temp, c);
        };

        assertEquals(10, max.applyAsInt(5, 10, 3));
        assertEquals(10, max.applyAsInt(10, 5, 3));
    }

    @Test
    public void test_applyAsInt_min() {
        IntTernaryOperator min = (a, b, c) -> {
            int temp = Math.min(a, b);
            return Math.min(temp, c);
        };

        assertEquals(3, min.applyAsInt(5, 10, 3));
        assertEquals(3, min.applyAsInt(3, 10, 5));
    }
}
