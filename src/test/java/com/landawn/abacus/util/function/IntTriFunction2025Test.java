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
public class IntTriFunction2025Test extends TestBase {

    @Test
    public void test_apply() {
        IntTriFunction<Long> sum = (a, b, c) -> (long) (a + b + c);

        assertEquals(15L, sum.apply(3, 5, 7));
        assertEquals(0L, sum.apply(0, 0, 0));
    }

    @Test
    public void test_apply_lambda() {
        IntTriFunction<String> formatter = (a, b, c) -> String.format("(%d, %d, %d)", a, b, c);

        assertEquals("(1, 2, 3)", formatter.apply(1, 2, 3));
    }

    @Test
    public void test_andThen() {
        IntTriFunction<Long> product = (a, b, c) -> (long) a * b * c;
        java.util.function.Function<Long, String> addPrefix = result -> "Result: " + result;

        IntTriFunction<String> combined = product.andThen(addPrefix);

        assertEquals("Result: 24", combined.apply(2, 3, 4));
    }
}
