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
public class IntConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final int[] result = { 0 };

        IntConsumer consumer = t -> result[0] = t * 2;

        consumer.accept(5);
        assertEquals(10, result[0]);
    }

    @Test
    public void test_accept_lambda() {
        final int[] value = { 0 };

        IntConsumer consumer = t -> value[0] = t;

        consumer.accept(100);
        assertEquals(100, value[0]);
    }

    @Test
    public void test_accept_anonymousClass() {
        final int[] squared = { 0 };

        IntConsumer consumer = new IntConsumer() {
            @Override
            public void accept(int t) {
                squared[0] = t * t;
            }
        };

        consumer.accept(7);
        assertEquals(49, squared[0]);
    }

    @Test
    public void test_andThen() {
        final int[] results = new int[2];

        IntConsumer first = t -> results[0] = t + 10;
        IntConsumer second = t -> results[1] = t * 2;

        IntConsumer combined = first.andThen(second);
        combined.accept(5);

        assertEquals(15, results[0]);
        assertEquals(10, results[1]);
    }

    @Test
    public void test_andThen_multipleChaining() {
        final int[] results = new int[3];

        IntConsumer first = t -> results[0] = t;
        IntConsumer second = t -> results[1] = t + 1;
        IntConsumer third = t -> results[2] = t + 2;

        IntConsumer combined = first.andThen(second).andThen(third);
        combined.accept(10);

        assertEquals(10, results[0]);
        assertEquals(11, results[1]);
        assertEquals(12, results[2]);
    }

    @Test
    public void test_accept_withNegativeValue() {
        final int[] result = { 0 };

        IntConsumer consumer = t -> result[0] = -t;

        consumer.accept(-50);
        assertEquals(50, result[0]);
    }

    @Test
    public void test_accept_withMaxValue() {
        final int[] result = { 0 };

        IntConsumer consumer = t -> result[0] = t;

        consumer.accept(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, result[0]);
    }

    @Test
    public void test_accept_withMinValue() {
        final int[] result = { 0 };

        IntConsumer consumer = t -> result[0] = t;

        consumer.accept(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, result[0]);
    }

    @Test
    public void test_accept_withZero() {
        final int[] result = { 1 };

        IntConsumer consumer = t -> result[0] = t;

        consumer.accept(0);
        assertEquals(0, result[0]);
    }
}
