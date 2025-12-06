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
public class ShortUnaryOperator2025Test extends TestBase {

    @Test
    public void test_applyAsShort() {
        ShortUnaryOperator doubler = operand -> (short) (operand * 2);

        assertEquals(20, doubler.applyAsShort((short) 10));
        assertEquals(-20, doubler.applyAsShort((short) -10));
        assertEquals(0, doubler.applyAsShort((short) 0));
    }

    @Test
    public void test_applyAsShort_lambda() {
        ShortUnaryOperator incrementer = operand -> (short) (operand + 1);

        assertEquals(11, incrementer.applyAsShort((short) 10));
        assertEquals(1, incrementer.applyAsShort((short) 0));
        assertEquals(0, incrementer.applyAsShort((short) -1));
    }

    @Test
    public void test_applyAsShort_anonymousClass() {
        ShortUnaryOperator negator = new ShortUnaryOperator() {
            @Override
            public short applyAsShort(short operand) {
                return (short) -operand;
            }
        };

        assertEquals(-10, negator.applyAsShort((short) 10));
        assertEquals(10, negator.applyAsShort((short) -10));
        assertEquals(0, negator.applyAsShort((short) 0));
    }

    @Test
    public void test_compose() {
        ShortUnaryOperator addTen = operand -> (short) (operand + 10);
        ShortUnaryOperator multiplyByTwo = operand -> (short) (operand * 2);

        ShortUnaryOperator composed = multiplyByTwo.compose(addTen);

        // First adds 10, then multiplies by 2
        assertEquals(30, composed.applyAsShort((short) 5));   // (5 + 10) * 2 = 30
        assertEquals(20, composed.applyAsShort((short) 0));   // (0 + 10) * 2 = 20
    }

    @Test
    public void test_compose_multipleChaining() {
        ShortUnaryOperator addOne = operand -> (short) (operand + 1);
        ShortUnaryOperator multiplyByTwo = operand -> (short) (operand * 2);
        ShortUnaryOperator subtractFive = operand -> (short) (operand - 5);

        ShortUnaryOperator composed = subtractFive.compose(multiplyByTwo).compose(addOne);

        // First adds 1, then multiplies by 2, then subtracts 5
        assertEquals(7, composed.applyAsShort((short) 5));   // ((5 + 1) * 2) - 5 = 7
    }

    @Test
    public void test_andThen() {
        ShortUnaryOperator addTen = operand -> (short) (operand + 10);
        ShortUnaryOperator multiplyByTwo = operand -> (short) (operand * 2);

        ShortUnaryOperator composed = addTen.andThen(multiplyByTwo);

        // First adds 10, then multiplies by 2
        assertEquals(30, composed.applyAsShort((short) 5));   // (5 + 10) * 2 = 30
        assertEquals(20, composed.applyAsShort((short) 0));   // (0 + 10) * 2 = 20
    }

    @Test
    public void test_andThen_multipleChaining() {
        ShortUnaryOperator addOne = operand -> (short) (operand + 1);
        ShortUnaryOperator multiplyByTwo = operand -> (short) (operand * 2);
        ShortUnaryOperator subtractFive = operand -> (short) (operand - 5);

        ShortUnaryOperator composed = addOne.andThen(multiplyByTwo).andThen(subtractFive);

        // First adds 1, then multiplies by 2, then subtracts 5
        assertEquals(7, composed.applyAsShort((short) 5));   // ((5 + 1) * 2) - 5 = 7
    }

    @Test
    public void test_compose_vs_andThen() {
        ShortUnaryOperator addTen = operand -> (short) (operand + 10);
        ShortUnaryOperator multiplyByTwo = operand -> (short) (operand * 2);

        ShortUnaryOperator composedWithCompose = multiplyByTwo.compose(addTen);
        ShortUnaryOperator composedWithAndThen = addTen.andThen(multiplyByTwo);

        // Both should give the same result
        assertEquals(30, composedWithCompose.applyAsShort((short) 5));
        assertEquals(30, composedWithAndThen.applyAsShort((short) 5));
    }

    @Test
    public void test_identity() {
        ShortUnaryOperator identity = ShortUnaryOperator.identity();

        assertEquals(42, identity.applyAsShort((short) 42));
        assertEquals(0, identity.applyAsShort((short) 0));
        assertEquals(-100, identity.applyAsShort((short) -100));
        assertEquals(Short.MAX_VALUE, identity.applyAsShort(Short.MAX_VALUE));
        assertEquals(Short.MIN_VALUE, identity.applyAsShort(Short.MIN_VALUE));
    }

    @Test
    public void test_identity_composition() {
        ShortUnaryOperator identity = ShortUnaryOperator.identity();
        ShortUnaryOperator doubler = operand -> (short) (operand * 2);

        ShortUnaryOperator composedBefore = doubler.compose(identity);
        ShortUnaryOperator composedAfter = doubler.andThen(identity);

        assertEquals(20, composedBefore.applyAsShort((short) 10));
        assertEquals(20, composedAfter.applyAsShort((short) 10));
    }

    @Test
    public void test_applyAsShort_withMaxValue() {
        ShortUnaryOperator identity = ShortUnaryOperator.identity();

        assertEquals(Short.MAX_VALUE, identity.applyAsShort(Short.MAX_VALUE));
    }

    @Test
    public void test_applyAsShort_withMinValue() {
        ShortUnaryOperator identity = ShortUnaryOperator.identity();

        assertEquals(Short.MIN_VALUE, identity.applyAsShort(Short.MIN_VALUE));
    }
}
