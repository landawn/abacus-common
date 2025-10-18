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
public class IntBinaryOperator2025Test extends TestBase {

    @Test
    public void test_applyAsInt_add() {
        IntBinaryOperator add = (left, right) -> left + right;

        assertEquals(15, add.applyAsInt(5, 10));
        assertEquals(0, add.applyAsInt(5, -5));
        assertEquals(-15, add.applyAsInt(-5, -10));
    }

    @Test
    public void test_applyAsInt_subtract() {
        IntBinaryOperator subtract = (left, right) -> left - right;

        assertEquals(5, subtract.applyAsInt(15, 10));
        assertEquals(-5, subtract.applyAsInt(5, 10));
        assertEquals(0, subtract.applyAsInt(10, 10));
    }

    @Test
    public void test_applyAsInt_multiply() {
        IntBinaryOperator multiply = (left, right) -> left * right;

        assertEquals(50, multiply.applyAsInt(5, 10));
        assertEquals(-50, multiply.applyAsInt(5, -10));
        assertEquals(0, multiply.applyAsInt(0, 10));
    }

    @Test
    public void test_applyAsInt_divide() {
        IntBinaryOperator divide = (left, right) -> left / right;

        assertEquals(5, divide.applyAsInt(50, 10));
        assertEquals(3, divide.applyAsInt(10, 3));
        assertEquals(-5, divide.applyAsInt(50, -10));
    }

    @Test
    public void test_applyAsInt_max() {
        IntBinaryOperator max = (left, right) -> Math.max(left, right);

        assertEquals(10, max.applyAsInt(5, 10));
        assertEquals(10, max.applyAsInt(10, 5));
        assertEquals(5, max.applyAsInt(5, -10));
    }

    @Test
    public void test_applyAsInt_min() {
        IntBinaryOperator min = (left, right) -> Math.min(left, right);

        assertEquals(5, min.applyAsInt(5, 10));
        assertEquals(5, min.applyAsInt(10, 5));
        assertEquals(-10, min.applyAsInt(5, -10));
    }

    @Test
    public void test_applyAsInt_anonymousClass() {
        IntBinaryOperator modulo = new IntBinaryOperator() {
            @Override
            public int applyAsInt(int left, int right) {
                return left % right;
            }
        };

        assertEquals(1, modulo.applyAsInt(10, 3));
        assertEquals(0, modulo.applyAsInt(10, 5));
    }

    @Test
    public void test_applyAsInt_withMaxValue() {
        IntBinaryOperator identity = (left, right) -> left;

        assertEquals(Integer.MAX_VALUE, identity.applyAsInt(Integer.MAX_VALUE, 0));
    }
}
