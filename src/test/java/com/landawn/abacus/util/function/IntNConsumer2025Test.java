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
public class IntNConsumer2025Test extends TestBase {

    @Test
    public void test_accept_noArgs() {
        final int[] count = { 0 };

        IntNConsumer consumer = args -> count[0] = args.length;

        consumer.accept();
        assertEquals(0, count[0]);
    }

    @Test
    public void test_accept_multipleArgs() {
        final int[] sum = { 0 };

        IntNConsumer consumer = args -> {
            int total = 0;
            for (int value : args) {
                total += value;
            }
            sum[0] = total;
        };

        consumer.accept(1, 2, 3, 4, 5);
        assertEquals(15, sum[0]);
    }

    @Test
    public void test_andThen() {
        final int[] results = new int[2];

        IntNConsumer first = args -> {
            int sum = 0;
            for (int value : args) {
                sum += value;
            }
            results[0] = sum;
        };

        IntNConsumer second = args -> {
            int product = 1;
            for (int value : args) {
                product *= value;
            }
            results[1] = product;
        };

        IntNConsumer combined = first.andThen(second);
        combined.accept(2, 3, 4);

        assertEquals(9, results[0]);
        assertEquals(24, results[1]);
    }
}
