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
public class ShortConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final short[] result = { 0 };

        ShortConsumer consumer = t -> result[0] = (short) (t * 2);

        consumer.accept((short) 5);
        assertEquals(10, result[0]);
    }

    @Test
    public void test_accept_lambda() {
        final short[] value = { 0 };

        ShortConsumer consumer = t -> value[0] = t;

        consumer.accept((short) 100);
        assertEquals(100, value[0]);
    }

    @Test
    public void test_accept_methodReference() {
        final short[] result = { 0 };

        ShortConsumer consumer = this::acceptHelper;

        consumer.accept((short) 42);
        assertNotNull(consumer);
    }

    private void acceptHelper(short t) {
        // Helper method for method reference test
        assertTrue(t >= Short.MIN_VALUE && t <= Short.MAX_VALUE);
    }

    @Test
    public void test_accept_anonymousClass() {
        final short[] squared = { 0 };

        ShortConsumer consumer = new ShortConsumer() {
            @Override
            public void accept(short t) {
                squared[0] = (short) (t * t);
            }
        };

        consumer.accept((short) 7);
        assertEquals(49, squared[0]);
    }

    @Test
    public void test_andThen() {
        final short[] results = new short[2];

        ShortConsumer first = t -> results[0] = (short) (t + 10);
        ShortConsumer second = t -> results[1] = (short) (t * 2);

        ShortConsumer combined = first.andThen(second);
        combined.accept((short) 5);

        assertEquals(15, results[0]);
        assertEquals(10, results[1]);
    }

    @Test
    public void test_andThen_multipleChaining() {
        final short[] results = new short[3];

        ShortConsumer first = t -> results[0] = t;
        ShortConsumer second = t -> results[1] = (short) (t + 1);
        ShortConsumer third = t -> results[2] = (short) (t + 2);

        ShortConsumer combined = first.andThen(second).andThen(third);
        combined.accept((short) 10);

        assertEquals(10, results[0]);
        assertEquals(11, results[1]);
        assertEquals(12, results[2]);
    }

    @Test
    public void test_andThen_orderOfExecution() {
        final StringBuilder order = new StringBuilder();

        ShortConsumer first = t -> order.append("A");
        ShortConsumer second = t -> order.append("B");
        ShortConsumer third = t -> order.append("C");

        ShortConsumer combined = first.andThen(second).andThen(third);
        combined.accept((short) 1);

        assertEquals("ABC", order.toString());
    }

    @Test
    public void test_accept_withNegativeValue() {
        final short[] result = { 0 };

        ShortConsumer consumer = t -> result[0] = (short) -t;

        consumer.accept((short) -50);
        assertEquals(50, result[0]);
    }

    @Test
    public void test_accept_withMaxValue() {
        final short[] result = { 0 };

        ShortConsumer consumer = t -> result[0] = t;

        consumer.accept(Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, result[0]);
    }

    @Test
    public void test_accept_withMinValue() {
        final short[] result = { 0 };

        ShortConsumer consumer = t -> result[0] = t;

        consumer.accept(Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, result[0]);
    }

    @Test
    public void test_accept_withZero() {
        final short[] result = { 1 };

        ShortConsumer consumer = t -> result[0] = t;

        consumer.accept((short) 0);
        assertEquals(0, result[0]);
    }
}
