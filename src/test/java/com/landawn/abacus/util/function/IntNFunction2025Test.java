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
public class IntNFunction2025Test extends TestBase {

    @Test
    public void test_apply_noArgs() {
        IntNFunction<Integer> function = args -> args.length;

        assertEquals(0, function.apply());
    }

    @Test
    public void test_apply_multipleArgs() {
        IntNFunction<Long> sum = args -> {
            long total = 0;
            for (int value : args) {
                total += value;
            }
            return total;
        };

        assertEquals(15L, sum.apply(1, 2, 3, 4, 5));
    }

    @Test
    public void test_andThen() {
        IntNFunction<Long> sum = args -> {
            long total = 0;
            for (int value : args) {
                total += value;
            }
            return total;
        };
        java.util.function.Function<Long, String> addPrefix = result -> "Sum: " + result;

        IntNFunction<String> combined = sum.andThen(addPrefix);

        assertEquals("Sum: 15", combined.apply(5, 10));
    }
}
