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
public class ShortNFunction2025Test extends TestBase {

    @Test
    public void test_apply_noArgs() {
        ShortNFunction<Integer> function = args -> args.length;

        assertEquals(0, function.apply());
    }

    @Test
    public void test_apply_singleArg() {
        ShortNFunction<Integer> function = args -> args.length > 0 ? (int) args[0] : 0;

        assertEquals(42, function.apply((short) 42));
    }

    @Test
    public void test_apply_multipleArgs() {
        ShortNFunction<Integer> sum = args -> {
            int total = 0;
            for (short value : args) {
                total += value;
            }
            return total;
        };

        assertEquals(15, sum.apply((short) 1, (short) 2, (short) 3, (short) 4, (short) 5));
    }

    @Test
    public void test_apply_lambda() {
        ShortNFunction<String> formatter = args -> {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i]);
            }
            sb.append("]");
            return sb.toString();
        };

        assertEquals("[1, 2, 3]", formatter.apply((short) 1, (short) 2, (short) 3));
    }

    @Test
    public void test_apply_anonymousClass() {
        ShortNFunction<Double> average = new ShortNFunction<Double>() {
            @Override
            public Double apply(short... args) {
                if (args.length == 0) {
                    return 0.0;
                }
                double sum = 0;
                for (short value : args) {
                    sum += value;
                }
                return sum / args.length;
            }
        };

        assertEquals(5.0, average.apply((short) 3, (short) 5, (short) 7), 0.001);
        assertEquals(0.0, average.apply(), 0.001);
    }

    @Test
    public void test_apply_returningString() {
        ShortNFunction<String> concatenate = args -> {
            if (args.length == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (short value : args) {
                sb.append(value);
            }
            return sb.toString();
        };

        assertEquals("123", concatenate.apply((short) 1, (short) 2, (short) 3));
        assertEquals("", concatenate.apply());
    }

    @Test
    public void test_andThen() {
        ShortNFunction<Integer> sum = args -> {
            int total = 0;
            for (short value : args) {
                total += value;
            }
            return total;
        };
        java.util.function.Function<Integer, String> addPrefix = result -> "Sum: " + result;

        ShortNFunction<String> combined = sum.andThen(addPrefix);

        assertEquals("Sum: 15", combined.apply((short) 5, (short) 10));
        assertEquals("Sum: 0", combined.apply());
    }

    @Test
    public void test_andThen_multipleChaining() {
        ShortNFunction<Integer> product = args -> {
            int result = 1;
            for (short value : args) {
                result *= value;
            }
            return result;
        };
        java.util.function.Function<Integer, Double> toDouble = i -> i * 1.5;
        java.util.function.Function<Double, String> format = d -> String.format("%.1f", d);

        ShortNFunction<String> combined = product.andThen(toDouble).andThen(format);

        assertEquals("36.0", combined.apply((short) 2, (short) 3, (short) 4));
    }

    @Test
    public void test_apply_returningNull() {
        ShortNFunction<String> alwaysNull = args -> null;

        assertNull(alwaysNull.apply((short) 1, (short) 2, (short) 3));
    }

    @Test
    public void test_apply_withNegativeValues() {
        ShortNFunction<Integer> sum = args -> {
            int total = 0;
            for (short value : args) {
                total += value;
            }
            return total;
        };

        assertEquals(-60, sum.apply((short) -10, (short) -20, (short) -30));
    }

    @Test
    public void test_apply_varargs() {
        ShortNFunction<Integer> count = args -> args.length;

        assertEquals(1, count.apply((short) 1));
        assertEquals(2, count.apply((short) 1, (short) 2));
        assertEquals(5, count.apply((short) 1, (short) 2, (short) 3, (short) 4, (short) 5));
    }
}
