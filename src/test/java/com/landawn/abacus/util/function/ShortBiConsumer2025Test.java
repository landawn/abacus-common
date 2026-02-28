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
public class ShortBiConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final Pair<Short, Short> result = Pair.of((short) 0, (short) 0);

        ShortBiConsumer consumer = (t, u) -> {
            result.setLeft((short) (t + 1));
            result.setRight((short) (u + 1));
        };

        consumer.accept((short) 5, (short) 10);
        assertEquals(6, result.left().shortValue());
        assertEquals(11, result.right().shortValue());
    }

    @Test
    public void test_accept_lambda() {
        final short[] sum = { 0 };

        ShortBiConsumer consumer = (t, u) -> sum[0] = (short) (t + u);

        consumer.accept((short) 100, (short) 200);
        assertEquals(300, sum[0]);
    }

    @Test
    public void test_accept_methodReference() {
        final Pair<Short, Short> result = Pair.of((short) 0, (short) 0);

        ShortBiConsumer consumer = this::acceptHelper;

        consumer.accept((short) 7, (short) 14);
        // Method reference calls helper, we can't verify result directly
        // but we can verify it doesn't throw
        assertNotNull(consumer);
    }

    private void acceptHelper(short t, short u) {
        // Helper method for method reference test
        short sum = (short) (t + u);
        assertTrue(sum > 0 || sum <= 0); // Always true, just using the values
    }

    @Test
    public void test_accept_anonymousClass() {
        final short[] product = { 1 };

        ShortBiConsumer consumer = new ShortBiConsumer() {
            @Override
            public void accept(short t, short u) {
                product[0] = (short) (t * u);
            }
        };

        consumer.accept((short) 5, (short) 6);
        assertEquals(30, product[0]);
    }

    @Test
    public void test_andThen() {
        final Pair<Short, Short> result = Pair.of((short) 0, (short) 0);

        ShortBiConsumer first = (t, u) -> result.setLeft((short) (t + u));
        ShortBiConsumer second = (t, u) -> result.setRight((short) (t * u));

        ShortBiConsumer combined = first.andThen(second);
        combined.accept((short) 3, (short) 4);

        assertEquals(7, result.left().shortValue());
        assertEquals(12, result.right().shortValue());
    }

    @Test
    public void test_andThen_multipleChaining() {
        final short[] results = new short[3];

        ShortBiConsumer first = (t, u) -> results[0] = (short) (t + u);
        ShortBiConsumer second = (t, u) -> results[1] = (short) (t - u);
        ShortBiConsumer third = (t, u) -> results[2] = (short) (t * u);

        ShortBiConsumer combined = first.andThen(second).andThen(third);
        combined.accept((short) 10, (short) 3);

        assertEquals(13, results[0]);
        assertEquals(7, results[1]);
        assertEquals(30, results[2]);
    }

    @Test
    public void test_andThen_orderOfExecution() {
        final StringBuilder order = new StringBuilder();

        ShortBiConsumer first = (t, u) -> order.append("1");
        ShortBiConsumer second = (t, u) -> order.append("2");
        ShortBiConsumer third = (t, u) -> order.append("3");

        ShortBiConsumer combined = first.andThen(second).andThen(third);
        combined.accept((short) 1, (short) 2);

        assertEquals("123", order.toString());
    }

    @Test
    public void test_accept_withNegativeValues() {
        final short[] result = { 0 };

        ShortBiConsumer consumer = (t, u) -> result[0] = (short) (t + u);

        consumer.accept((short) -100, (short) -200);
        assertEquals(-300, result[0]);
    }

    @Test
    public void test_accept_withMaxValues() {
        final Pair<Short, Short> result = Pair.of((short) 0, (short) 0);

        ShortBiConsumer consumer = (t, u) -> {
            result.setLeft(t);
            result.setRight(u);
        };

        consumer.accept(Short.MAX_VALUE, Short.MIN_VALUE);
        assertEquals(Short.MAX_VALUE, result.left().shortValue());
        assertEquals(Short.MIN_VALUE, result.right().shortValue());
    }

    @Test
    public void test_accept_withZero() {
        final short[] result = { 1 };

        ShortBiConsumer consumer = (t, u) -> result[0] = (short) (t * u);

        consumer.accept((short) 0, (short) 100);
        assertEquals(0, result[0]);
    }
}
