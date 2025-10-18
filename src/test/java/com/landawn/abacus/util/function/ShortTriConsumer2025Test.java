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
public class ShortTriConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final short[] result = new short[3];

        ShortTriConsumer consumer = (a, b, c) -> {
            result[0] = a;
            result[1] = b;
            result[2] = c;
        };

        consumer.accept((short) 1, (short) 2, (short) 3);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
    }

    @Test
    public void test_accept_lambda() {
        final short[] sum = { 0 };

        ShortTriConsumer consumer = (a, b, c) -> sum[0] = (short) (a + b + c);

        consumer.accept((short) 10, (short) 20, (short) 30);
        assertEquals(60, sum[0]);
    }

    @Test
    public void test_accept_anonymousClass() {
        final short[] product = { 1 };

        ShortTriConsumer consumer = new ShortTriConsumer() {
            @Override
            public void accept(short a, short b, short c) {
                product[0] = (short) (a * b * c);
            }
        };

        consumer.accept((short) 2, (short) 3, (short) 4);
        assertEquals(24, product[0]);
    }

    @Test
    public void test_andThen() {
        final short[] results = new short[2];

        ShortTriConsumer first = (a, b, c) -> results[0] = (short) (a + b + c);
        ShortTriConsumer second = (a, b, c) -> results[1] = (short) (a * b * c);

        ShortTriConsumer combined = first.andThen(second);
        combined.accept((short) 2, (short) 3, (short) 4);

        assertEquals(9, results[0]);
        assertEquals(24, results[1]);
    }

    @Test
    public void test_andThen_multipleChaining() {
        final short[] results = new short[3];

        ShortTriConsumer first = (a, b, c) -> results[0] = (short) (a + b + c);
        ShortTriConsumer second = (a, b, c) -> results[1] = (short) (a - b - c);
        ShortTriConsumer third = (a, b, c) -> results[2] = (short) (a * b * c);

        ShortTriConsumer combined = first.andThen(second).andThen(third);
        combined.accept((short) 10, (short) 2, (short) 3);

        assertEquals(15, results[0]);
        assertEquals(5, results[1]);
        assertEquals(60, results[2]);
    }

    @Test
    public void test_accept_withNegativeValues() {
        final short[] result = { 0 };

        ShortTriConsumer consumer = (a, b, c) -> result[0] = (short) (a + b + c);

        consumer.accept((short) -10, (short) -20, (short) -30);
        assertEquals(-60, result[0]);
    }

    @Test
    public void test_accept_withZero() {
        final short[] result = { 1 };

        ShortTriConsumer consumer = (a, b, c) -> result[0] = (short) (a * b * c);

        consumer.accept((short) 0, (short) 10, (short) 20);
        assertEquals(0, result[0]);
    }
}
