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
public class ShortNConsumer2025Test extends TestBase {

    @Test
    public void test_accept_noArgs() {
        final int[] count = { 0 };

        ShortNConsumer consumer = args -> count[0] = args.length;

        consumer.accept();
        assertEquals(0, count[0]);
    }

    @Test
    public void test_accept_singleArg() {
        final short[] result = { 0 };

        ShortNConsumer consumer = args -> {
            if (args.length > 0) {
                result[0] = args[0];
            }
        };

        consumer.accept((short) 42);
        assertEquals(42, result[0]);
    }

    @Test
    public void test_accept_multipleArgs() {
        final short[] sum = { 0 };

        ShortNConsumer consumer = args -> {
            short total = 0;
            for (short value : args) {
                total += value;
            }
            sum[0] = total;
        };

        consumer.accept((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        assertEquals(15, sum[0]);
    }

    @Test
    public void test_accept_lambda() {
        final int[] count = { 0 };

        ShortNConsumer consumer = args -> count[0] = args.length;

        consumer.accept((short) 10, (short) 20, (short) 30);
        assertEquals(3, count[0]);
    }

    @Test
    public void test_accept_anonymousClass() {
        final short[] max = { Short.MIN_VALUE };

        ShortNConsumer consumer = new ShortNConsumer() {
            @Override
            public void accept(short... args) {
                for (short value : args) {
                    if (value > max[0]) {
                        max[0] = value;
                    }
                }
            }
        };

        consumer.accept((short) 5, (short) 10, (short) 3, (short) 15, (short) 7);
        assertEquals(15, max[0]);
    }

    @Test
    public void test_andThen() {
        final short[] results = new short[2];

        ShortNConsumer first = args -> {
            short sum = 0;
            for (short value : args) {
                sum += value;
            }
            results[0] = sum;
        };

        ShortNConsumer second = args -> {
            short product = 1;
            for (short value : args) {
                product *= value;
            }
            results[1] = product;
        };

        ShortNConsumer combined = first.andThen(second);
        combined.accept((short) 2, (short) 3, (short) 4);

        assertEquals(9, results[0]);
        assertEquals(24, results[1]);
    }

    @Test
    public void test_andThen_multipleChaining() {
        final short[] results = new short[3];

        ShortNConsumer first = args -> results[0] = (short) args.length;
        ShortNConsumer second = args -> results[1] = args.length > 0 ? args[0] : 0;
        ShortNConsumer third = args -> results[2] = args.length > 1 ? args[1] : 0;

        ShortNConsumer combined = first.andThen(second).andThen(third);
        combined.accept((short) 10, (short) 20, (short) 30);

        assertEquals(3, results[0]);
        assertEquals(10, results[1]);
        assertEquals(20, results[2]);
    }

    @Test
    public void test_accept_withNegativeValues() {
        final short[] sum = { 0 };

        ShortNConsumer consumer = args -> {
            short total = 0;
            for (short value : args) {
                total += value;
            }
            sum[0] = total;
        };

        consumer.accept((short) -10, (short) -20, (short) -30);
        assertEquals(-60, sum[0]);
    }

    @Test
    public void test_accept_withZero() {
        final int[] countZeros = { 0 };

        ShortNConsumer consumer = args -> {
            for (short value : args) {
                if (value == 0) {
                    countZeros[0]++;
                }
            }
        };

        consumer.accept((short) 0, (short) 1, (short) 0, (short) 2, (short) 0);
        assertEquals(3, countZeros[0]);
    }

    @Test
    public void test_accept_varargs() {
        final short[] result = { 0 };

        ShortNConsumer consumer = args -> {
            for (short value : args) {
                result[0] += value;
            }
        };

        // Test with different number of arguments
        consumer.accept((short) 1);
        assertEquals(1, result[0]);

        result[0] = 0;
        consumer.accept((short) 1, (short) 2);
        assertEquals(3, result[0]);

        result[0] = 0;
        consumer.accept((short) 1, (short) 2, (short) 3, (short) 4);
        assertEquals(10, result[0]);
    }
}
