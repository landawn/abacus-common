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
public class IntObjOperator2025Test extends TestBase {

    @Test
    public void test_applyAsInt() {
        IntObjOperator<String> operator = (t, u) -> u.length() + t;

        assertEquals(14, operator.applyAsInt(10, "test"));
        assertEquals(5, operator.applyAsInt(0, "value"));
    }

    @Test
    public void test_applyAsInt_lambda() {
        IntObjOperator<Integer> operator = (index, value) -> index * value;

        assertEquals(420, operator.applyAsInt(42, 10));
        assertEquals(0, operator.applyAsInt(0, 100));
        assertEquals(-50, operator.applyAsInt(10, -5));
    }

    @Test
    public void test_applyAsInt_anonymousClass() {
        IntObjOperator<String> operator = new IntObjOperator<String>() {
            @Override
            public int applyAsInt(int t, String u) {
                return Math.min(t, u.length());
            }
        };

        assertEquals(3, operator.applyAsInt(3, "hello"));
        assertEquals(5, operator.applyAsInt(10, "hello"));
    }
}
