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
public class IntUnaryOperator2025Test extends TestBase {

    @Test
    public void test_applyAsInt() {
        IntUnaryOperator doubler = operand -> operand * 2;

        assertEquals(20, doubler.applyAsInt(10));
        assertEquals(-20, doubler.applyAsInt(-10));
        assertEquals(0, doubler.applyAsInt(0));
    }

    @Test
    public void test_applyAsInt_lambda() {
        IntUnaryOperator incrementer = operand -> operand + 1;

        assertEquals(11, incrementer.applyAsInt(10));
        assertEquals(1, incrementer.applyAsInt(0));
        assertEquals(0, incrementer.applyAsInt(-1));
    }

    @Test
    public void test_applyAsInt_anonymousClass() {
        IntUnaryOperator negator = new IntUnaryOperator() {
            @Override
            public int applyAsInt(int operand) {
                return -operand;
            }
        };

        assertEquals(-10, negator.applyAsInt(10));
        assertEquals(10, negator.applyAsInt(-10));
        assertEquals(0, negator.applyAsInt(0));
    }

    @Test
    public void test_compose() {
        IntUnaryOperator addTen = operand -> operand + 10;
        IntUnaryOperator multiplyByTwo = operand -> operand * 2;

        IntUnaryOperator composed = multiplyByTwo.compose(addTen);

        // First adds 10, then multiplies by 2
        assertEquals(30, composed.applyAsInt(5)); // (5 + 10) * 2 = 30
        assertEquals(20, composed.applyAsInt(0)); // (0 + 10) * 2 = 20
    }

    @Test
    public void test_compose_multipleChaining() {
        IntUnaryOperator addOne = operand -> operand + 1;
        IntUnaryOperator multiplyByTwo = operand -> operand * 2;
        IntUnaryOperator subtractFive = operand -> operand - 5;

        IntUnaryOperator composed = subtractFive.compose(multiplyByTwo).compose(addOne);

        // First adds 1, then multiplies by 2, then subtracts 5
        assertEquals(7, composed.applyAsInt(5)); // ((5 + 1) * 2) - 5 = 7
    }

    @Test
    public void test_andThen() {
        IntUnaryOperator addTen = operand -> operand + 10;
        IntUnaryOperator multiplyByTwo = operand -> operand * 2;

        IntUnaryOperator composed = addTen.andThen(multiplyByTwo);

        // First adds 10, then multiplies by 2
        assertEquals(30, composed.applyAsInt(5)); // (5 + 10) * 2 = 30
        assertEquals(20, composed.applyAsInt(0)); // (0 + 10) * 2 = 20
    }

    @Test
    public void test_andThen_multipleChaining() {
        IntUnaryOperator addOne = operand -> operand + 1;
        IntUnaryOperator multiplyByTwo = operand -> operand * 2;
        IntUnaryOperator subtractFive = operand -> operand - 5;

        IntUnaryOperator composed = addOne.andThen(multiplyByTwo).andThen(subtractFive);

        // First adds 1, then multiplies by 2, then subtracts 5
        assertEquals(7, composed.applyAsInt(5)); // ((5 + 1) * 2) - 5 = 7
    }

    @Test
    public void test_compose_vs_andThen() {
        IntUnaryOperator addTen = operand -> operand + 10;
        IntUnaryOperator multiplyByTwo = operand -> operand * 2;

        IntUnaryOperator composedWithCompose = multiplyByTwo.compose(addTen);
        IntUnaryOperator composedWithAndThen = addTen.andThen(multiplyByTwo);

        // Both should give the same result
        assertEquals(30, composedWithCompose.applyAsInt(5));
        assertEquals(30, composedWithAndThen.applyAsInt(5));
    }

    @Test
    public void test_identity() {
        IntUnaryOperator identity = IntUnaryOperator.identity();

        assertEquals(42, identity.applyAsInt(42));
        assertEquals(0, identity.applyAsInt(0));
        assertEquals(-100, identity.applyAsInt(-100));
        assertEquals(Integer.MAX_VALUE, identity.applyAsInt(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, identity.applyAsInt(Integer.MIN_VALUE));
    }

    @Test
    public void test_identity_composition() {
        IntUnaryOperator identity = IntUnaryOperator.identity();
        IntUnaryOperator doubler = operand -> operand * 2;

        IntUnaryOperator composedBefore = doubler.compose(identity);
        IntUnaryOperator composedAfter = doubler.andThen(identity);

        assertEquals(20, composedBefore.applyAsInt(10));
        assertEquals(20, composedAfter.applyAsInt(10));
    }

    @Test
    public void test_applyAsInt_withMaxValue() {
        IntUnaryOperator identity = IntUnaryOperator.identity();

        assertEquals(Integer.MAX_VALUE, identity.applyAsInt(Integer.MAX_VALUE));
    }

    @Test
    public void test_applyAsInt_withMinValue() {
        IntUnaryOperator identity = IntUnaryOperator.identity();

        assertEquals(Integer.MIN_VALUE, identity.applyAsInt(Integer.MIN_VALUE));
    }
}
