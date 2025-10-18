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
public class IntTriConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final int[] result = new int[3];

        IntTriConsumer consumer = (a, b, c) -> {
            result[0] = a;
            result[1] = b;
            result[2] = c;
        };

        consumer.accept(1, 2, 3);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
    }

    @Test
    public void test_accept_lambda() {
        final int[] sum = { 0 };

        IntTriConsumer consumer = (a, b, c) -> sum[0] = a + b + c;

        consumer.accept(10, 20, 30);
        assertEquals(60, sum[0]);
    }

    @Test
    public void test_andThen() {
        final int[] results = new int[2];

        IntTriConsumer first = (a, b, c) -> results[0] = a + b + c;
        IntTriConsumer second = (a, b, c) -> results[1] = a * b * c;

        IntTriConsumer combined = first.andThen(second);
        combined.accept(2, 3, 4);

        assertEquals(9, results[0]);
        assertEquals(24, results[1]);
    }
}
