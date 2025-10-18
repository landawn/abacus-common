package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatBinaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsFloat() {
        final FloatBinaryOperator operator = (left, right) -> left + right;
        final float result = operator.applyAsFloat(10.5f, 20.3f);
        assertEquals(30.8f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withLambda() {
        final FloatBinaryOperator operator = (a, b) -> a * b;
        final float result = operator.applyAsFloat(5.5f, 2.0f);
        assertEquals(11.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_addition() {
        final FloatBinaryOperator add = Float::sum;
        assertEquals(15.8f, add.applyAsFloat(10.5f, 5.3f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_subtraction() {
        final FloatBinaryOperator subtract = (a, b) -> a - b;
        assertEquals(5.2f, subtract.applyAsFloat(10.5f, 5.3f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_multiplication() {
        final FloatBinaryOperator multiply = (a, b) -> a * b;
        assertEquals(52.5f, multiply.applyAsFloat(10.5f, 5.0f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_division() {
        final FloatBinaryOperator divide = (a, b) -> a / b;
        assertEquals(2.0f, divide.applyAsFloat(10.0f, 5.0f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_max() {
        final FloatBinaryOperator max = Math::max;
        assertEquals(20.3f, max.applyAsFloat(10.5f, 20.3f), 0.001f);
        assertEquals(20.3f, max.applyAsFloat(20.3f, 10.5f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_min() {
        final FloatBinaryOperator min = Math::min;
        assertEquals(10.5f, min.applyAsFloat(10.5f, 20.3f), 0.001f);
        assertEquals(10.5f, min.applyAsFloat(20.3f, 10.5f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_withNegativeValues() {
        final FloatBinaryOperator operator = (a, b) -> a + b;
        assertEquals(-30.8f, operator.applyAsFloat(-10.5f, -20.3f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_withZeroValues() {
        final FloatBinaryOperator operator = (a, b) -> a * b;
        assertEquals(0f, operator.applyAsFloat(0f, 10.5f), 0.001f);
    }
}
