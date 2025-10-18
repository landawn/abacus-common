package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteToBooleanFunction2025Test extends TestBase {

    @Test
    public void testApplyAsBoolean() {
        ByteToBooleanFunction isPositive = value -> value > 0;

        assertTrue(isPositive.applyAsBoolean((byte) 5));
        assertFalse(isPositive.applyAsBoolean((byte) -5));
        assertFalse(isPositive.applyAsBoolean((byte) 0));
    }

    @Test
    public void testApplyAsBooleanWithLambda() {
        ByteToBooleanFunction isEven = value -> value % 2 == 0;

        assertTrue(isEven.applyAsBoolean((byte) 4));
        assertFalse(isEven.applyAsBoolean((byte) 3));
        assertTrue(isEven.applyAsBoolean((byte) 0));
    }

    @Test
    public void testApplyAsBooleanWithAnonymousClass() {
        ByteToBooleanFunction greaterThan10 = new ByteToBooleanFunction() {
            @Override
            public boolean applyAsBoolean(byte value) {
                return value > 10;
            }
        };

        assertTrue(greaterThan10.applyAsBoolean((byte) 15));
        assertFalse(greaterThan10.applyAsBoolean((byte) 5));
    }

    @Test
    public void testDefault() {
        assertTrue(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) 5));
        assertFalse(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) 0));
        assertFalse(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) -5));
    }

    @Test
    public void testDefaultWithBoundaryValues() {
        assertTrue(ByteToBooleanFunction.DEFAULT.applyAsBoolean(Byte.MAX_VALUE));
        assertFalse(ByteToBooleanFunction.DEFAULT.applyAsBoolean(Byte.MIN_VALUE));
    }

    @Test
    public void testIsZero() {
        ByteToBooleanFunction isZero = value -> value == 0;

        assertTrue(isZero.applyAsBoolean((byte) 0));
        assertFalse(isZero.applyAsBoolean((byte) 1));
        assertFalse(isZero.applyAsBoolean((byte) -1));
    }

    @Test
    public void testIsNegative() {
        ByteToBooleanFunction isNegative = value -> value < 0;

        assertTrue(isNegative.applyAsBoolean((byte) -5));
        assertFalse(isNegative.applyAsBoolean((byte) 0));
        assertFalse(isNegative.applyAsBoolean((byte) 5));
    }

    @Test
    public void testInRange() {
        ByteToBooleanFunction inRange = value -> value >= 10 && value <= 20;

        assertTrue(inRange.applyAsBoolean((byte) 15));
        assertTrue(inRange.applyAsBoolean((byte) 10));
        assertTrue(inRange.applyAsBoolean((byte) 20));
        assertFalse(inRange.applyAsBoolean((byte) 5));
        assertFalse(inRange.applyAsBoolean((byte) 25));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteToBooleanFunction isMax = value -> value == Byte.MAX_VALUE;
        ByteToBooleanFunction isMin = value -> value == Byte.MIN_VALUE;

        assertTrue(isMax.applyAsBoolean(Byte.MAX_VALUE));
        assertTrue(isMin.applyAsBoolean(Byte.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        ByteToBooleanFunction isPositive = ByteToBooleanFunction2025Test::checkPositive;

        assertTrue(isPositive.applyAsBoolean((byte) 5));
        assertFalse(isPositive.applyAsBoolean((byte) -5));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteToBooleanFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static boolean checkPositive(byte value) {
        return value > 0;
    }
}
