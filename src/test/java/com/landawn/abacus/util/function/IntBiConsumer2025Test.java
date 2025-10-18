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
import com.landawn.abacus.util.Pair;

@Tag("2025")
public class IntBiConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final Pair<Integer, Integer> result = Pair.of(0, 0);

        IntBiConsumer consumer = (t, u) -> {
            result.setLeft(t + 1);
            result.setRight(u + 1);
        };

        consumer.accept(5, 10);
        assertEquals(6, result.left().intValue());
        assertEquals(11, result.right().intValue());
    }

    @Test
    public void test_accept_lambda() {
        final int[] sum = { 0 };

        IntBiConsumer consumer = (t, u) -> sum[0] = t + u;

        consumer.accept(100, 200);
        assertEquals(300, sum[0]);
    }

    @Test
    public void test_accept_anonymousClass() {
        final int[] product = { 1 };

        IntBiConsumer consumer = new IntBiConsumer() {
            @Override
            public void accept(int t, int u) {
                product[0] = t * u;
            }
        };

        consumer.accept(5, 6);
        assertEquals(30, product[0]);
    }

    @Test
    public void test_andThen() {
        final Pair<Integer, Integer> result = Pair.of(0, 0);

        IntBiConsumer first = (t, u) -> result.setLeft(t + u);
        IntBiConsumer second = (t, u) -> result.setRight(t * u);

        IntBiConsumer combined = first.andThen(second);
        combined.accept(3, 4);

        assertEquals(7, result.left().intValue());
        assertEquals(12, result.right().intValue());
    }

    @Test
    public void test_andThen_multipleChaining() {
        final int[] results = new int[3];

        IntBiConsumer first = (t, u) -> results[0] = t + u;
        IntBiConsumer second = (t, u) -> results[1] = t - u;
        IntBiConsumer third = (t, u) -> results[2] = t * u;

        IntBiConsumer combined = first.andThen(second).andThen(third);
        combined.accept(10, 3);

        assertEquals(13, results[0]);
        assertEquals(7, results[1]);
        assertEquals(30, results[2]);
    }

    @Test
    public void test_accept_withNegativeValues() {
        final int[] result = { 0 };

        IntBiConsumer consumer = (t, u) -> result[0] = t + u;

        consumer.accept(-100, -200);
        assertEquals(-300, result[0]);
    }

    @Test
    public void test_accept_withMaxValues() {
        final Pair<Integer, Integer> result = Pair.of(0, 0);

        IntBiConsumer consumer = (t, u) -> {
            result.setLeft(t);
            result.setRight(u);
        };

        consumer.accept(Integer.MAX_VALUE, Integer.MIN_VALUE);
        assertEquals(Integer.MAX_VALUE, result.left().intValue());
        assertEquals(Integer.MIN_VALUE, result.right().intValue());
    }

    @Test
    public void test_accept_withZero() {
        final int[] result = { 1 };

        IntBiConsumer consumer = (t, u) -> result[0] = t * u;

        consumer.accept(0, 100);
        assertEquals(0, result[0]);
    }
}
