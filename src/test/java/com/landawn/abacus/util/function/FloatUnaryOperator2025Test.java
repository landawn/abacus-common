package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatUnaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsFloat() {
        final FloatUnaryOperator operator = val -> val * 2;
        final float result = operator.applyAsFloat(21.25f);
        assertEquals(42.5f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withLambda() {
        final FloatUnaryOperator operator = val -> val + 10.0f;
        assertEquals(20.5f, operator.applyAsFloat(10.5f), 0.001f);
    }

    @Test
    public void testCompose() {
        final FloatUnaryOperator multiplyBy3 = val -> val * 3;
        final FloatUnaryOperator add10 = val -> val + 10;

        final FloatUnaryOperator composed = multiplyBy3.compose(add10);
        final float result = composed.applyAsFloat(5.0f);

        assertEquals(45.0f, result, 0.001f);   // (5 + 10) * 3
    }

    @Test
    public void testAndThen() {
        final FloatUnaryOperator multiplyBy2 = val -> val * 2;
        final FloatUnaryOperator subtract5 = val -> val - 5;

        final FloatUnaryOperator composed = multiplyBy2.andThen(subtract5);
        final float result = composed.applyAsFloat(10.0f);

        assertEquals(15.0f, result, 0.001f);   // (10 * 2) - 5
    }

    @Test
    public void testIdentity() {
        final FloatUnaryOperator identity = FloatUnaryOperator.identity();
        assertEquals(42.5f, identity.applyAsFloat(42.5f), 0.001f);
        assertEquals(0f, identity.applyAsFloat(0f), 0.001f);
        assertEquals(-10.5f, identity.applyAsFloat(-10.5f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_increment() {
        final FloatUnaryOperator increment = val -> val + 1.0f;
        assertEquals(11.5f, increment.applyAsFloat(10.5f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_negate() {
        final FloatUnaryOperator negate = val -> -val;
        assertEquals(-10.5f, negate.applyAsFloat(10.5f), 0.001f);
        assertEquals(10.5f, negate.applyAsFloat(-10.5f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_absolute() {
        final FloatUnaryOperator absolute = Math::abs;
        assertEquals(10.5f, absolute.applyAsFloat(-10.5f), 0.001f);
        assertEquals(10.5f, absolute.applyAsFloat(10.5f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_square() {
        final FloatUnaryOperator square = val -> val * val;
        assertEquals(100.0f, square.applyAsFloat(10.0f), 0.001f);
        assertEquals(25.0f, square.applyAsFloat(-5.0f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_withZero() {
        final FloatUnaryOperator operator = val -> val * 2;
        assertEquals(0f, operator.applyAsFloat(0f), 0.001f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatUnaryOperator operator = val -> val + 1;
        assertNotNull(operator);
        assertEquals(2.0f, operator.applyAsFloat(1.0f), 0.001f);
    }
}
