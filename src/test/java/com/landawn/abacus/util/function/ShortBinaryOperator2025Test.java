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
public class ShortBinaryOperator2025Test extends TestBase {

    @Test
    public void test_applyAsShort_add() {
        ShortBinaryOperator add = (left, right) -> (short) (left + right);

        assertEquals(15, add.applyAsShort((short) 5, (short) 10));
        assertEquals(0, add.applyAsShort((short) 5, (short) -5));
        assertEquals(-15, add.applyAsShort((short) -5, (short) -10));
    }

    @Test
    public void test_applyAsShort_subtract() {
        ShortBinaryOperator subtract = (left, right) -> (short) (left - right);

        assertEquals(5, subtract.applyAsShort((short) 15, (short) 10));
        assertEquals(-5, subtract.applyAsShort((short) 5, (short) 10));
        assertEquals(0, subtract.applyAsShort((short) 10, (short) 10));
    }

    @Test
    public void test_applyAsShort_multiply() {
        ShortBinaryOperator multiply = (left, right) -> (short) (left * right);

        assertEquals(50, multiply.applyAsShort((short) 5, (short) 10));
        assertEquals(-50, multiply.applyAsShort((short) 5, (short) -10));
        assertEquals(0, multiply.applyAsShort((short) 0, (short) 10));
    }

    @Test
    public void test_applyAsShort_divide() {
        ShortBinaryOperator divide = (left, right) -> (short) (left / right);

        assertEquals(5, divide.applyAsShort((short) 50, (short) 10));
        assertEquals(3, divide.applyAsShort((short) 10, (short) 3));
        assertEquals(-5, divide.applyAsShort((short) 50, (short) -10));
    }

    @Test
    public void test_applyAsShort_max() {
        ShortBinaryOperator max = (left, right) -> (short) Math.max(left, right);

        assertEquals(10, max.applyAsShort((short) 5, (short) 10));
        assertEquals(10, max.applyAsShort((short) 10, (short) 5));
        assertEquals(5, max.applyAsShort((short) 5, (short) -10));
        assertEquals(0, max.applyAsShort((short) 0, (short) 0));
    }

    @Test
    public void test_applyAsShort_min() {
        ShortBinaryOperator min = (left, right) -> (short) Math.min(left, right);

        assertEquals(5, min.applyAsShort((short) 5, (short) 10));
        assertEquals(5, min.applyAsShort((short) 10, (short) 5));
        assertEquals(-10, min.applyAsShort((short) 5, (short) -10));
        assertEquals(0, min.applyAsShort((short) 0, (short) 0));
    }

    @Test
    public void test_applyAsShort_bitwiseAnd() {
        ShortBinaryOperator bitwiseAnd = (left, right) -> (short) (left & right);

        assertEquals(0x0F, bitwiseAnd.applyAsShort((short) 0xFF, (short) 0x0F));
        assertEquals(0, bitwiseAnd.applyAsShort((short) 0xF0, (short) 0x0F));
        assertEquals(0x08, bitwiseAnd.applyAsShort((short) 0x0C, (short) 0x0A));
    }

    @Test
    public void test_applyAsShort_bitwiseOr() {
        ShortBinaryOperator bitwiseOr = (left, right) -> (short) (left | right);

        assertEquals(0xFF, bitwiseOr.applyAsShort((short) 0xF0, (short) 0x0F));
        assertEquals(0x0F, bitwiseOr.applyAsShort((short) 0x00, (short) 0x0F));
        assertEquals(0x0E, bitwiseOr.applyAsShort((short) 0x0C, (short) 0x0A));
    }

    @Test
    public void test_applyAsShort_bitwiseXor() {
        ShortBinaryOperator bitwiseXor = (left, right) -> (short) (left ^ right);

        assertEquals(0xF0, bitwiseXor.applyAsShort((short) 0xFF, (short) 0x0F));
        assertEquals(0x0F, bitwiseXor.applyAsShort((short) 0x00, (short) 0x0F));
        assertEquals(0x06, bitwiseXor.applyAsShort((short) 0x0C, (short) 0x0A));
    }

    @Test
    public void test_applyAsShort_lambda() {
        ShortBinaryOperator average = (left, right) -> (short) ((left + right) / 2);

        assertEquals(7, average.applyAsShort((short) 5, (short) 10));
        assertEquals(0, average.applyAsShort((short) -5, (short) 5));
        assertEquals(-7, average.applyAsShort((short) -5, (short) -10));
    }

    @Test
    public void test_applyAsShort_anonymousClass() {
        ShortBinaryOperator modulo = new ShortBinaryOperator() {
            @Override
            public short applyAsShort(short left, short right) {
                return (short) (left % right);
            }
        };

        assertEquals(1, modulo.applyAsShort((short) 10, (short) 3));
        assertEquals(0, modulo.applyAsShort((short) 10, (short) 5));
        assertEquals(2, modulo.applyAsShort((short) 17, (short) 5));
    }

    @Test
    public void test_applyAsShort_withMaxValue() {
        ShortBinaryOperator identity = (left, right) -> left;

        assertEquals(Short.MAX_VALUE, identity.applyAsShort(Short.MAX_VALUE, (short) 0));
    }

    @Test
    public void test_applyAsShort_withMinValue() {
        ShortBinaryOperator identity = (left, right) -> left;

        assertEquals(Short.MIN_VALUE, identity.applyAsShort(Short.MIN_VALUE, (short) 0));
    }

    @Test
    public void test_applyAsShort_withZero() {
        ShortBinaryOperator multiply = (left, right) -> (short) (left * right);

        assertEquals(0, multiply.applyAsShort((short) 0, (short) 100));
        assertEquals(0, multiply.applyAsShort((short) 100, (short) 0));
    }
}
