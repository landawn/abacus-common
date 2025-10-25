package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanBinaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsBoolean() {
        BooleanBinaryOperator operator = (left, right) -> left && right;

        assertTrue(operator.applyAsBoolean(true, true));
        assertFalse(operator.applyAsBoolean(true, false));
        assertFalse(operator.applyAsBoolean(false, true));
        assertFalse(operator.applyAsBoolean(false, false));
    }

    @Test
    public void testApplyAsBooleanWithLambda() {
        BooleanBinaryOperator operator = (left, right) -> left || right;

        assertTrue(operator.applyAsBoolean(true, true));
        assertTrue(operator.applyAsBoolean(true, false));
        assertTrue(operator.applyAsBoolean(false, true));
        assertFalse(operator.applyAsBoolean(false, false));
    }

    @Test
    public void testLogicalAnd() {
        BooleanBinaryOperator andOperator = (left, right) -> left && right;

        assertTrue(andOperator.applyAsBoolean(true, true));
        assertFalse(andOperator.applyAsBoolean(true, false));
    }

    @Test
    public void testLogicalOr() {
        BooleanBinaryOperator orOperator = (left, right) -> left || right;

        assertTrue(orOperator.applyAsBoolean(true, false));
        assertFalse(orOperator.applyAsBoolean(false, false));
    }

    @Test
    public void testLogicalXor() {
        BooleanBinaryOperator xorOperator = (left, right) -> left != right;

        assertTrue(xorOperator.applyAsBoolean(true, false));
        assertTrue(xorOperator.applyAsBoolean(false, true));
        assertFalse(xorOperator.applyAsBoolean(true, true));
        assertFalse(xorOperator.applyAsBoolean(false, false));
    }

    @Test
    public void testLogicalNand() {
        BooleanBinaryOperator nandOperator = (left, right) -> !(left && right);

        assertFalse(nandOperator.applyAsBoolean(true, true));
        assertTrue(nandOperator.applyAsBoolean(true, false));
        assertTrue(nandOperator.applyAsBoolean(false, true));
        assertTrue(nandOperator.applyAsBoolean(false, false));
    }

    @Test
    public void testLogicalNor() {
        BooleanBinaryOperator norOperator = (left, right) -> !(left || right);

        assertFalse(norOperator.applyAsBoolean(true, true));
        assertFalse(norOperator.applyAsBoolean(true, false));
        assertFalse(norOperator.applyAsBoolean(false, true));
        assertTrue(norOperator.applyAsBoolean(false, false));
    }

    @Test
    public void testLogicalEquality() {
        BooleanBinaryOperator equalityOperator = (left, right) -> left == right;

        assertTrue(equalityOperator.applyAsBoolean(true, true));
        assertFalse(equalityOperator.applyAsBoolean(true, false));
        assertFalse(equalityOperator.applyAsBoolean(false, true));
        assertTrue(equalityOperator.applyAsBoolean(false, false));
    }

    @Test
    public void testAlwaysTrue() {
        BooleanBinaryOperator alwaysTrue = (left, right) -> true;

        assertTrue(alwaysTrue.applyAsBoolean(true, true));
        assertTrue(alwaysTrue.applyAsBoolean(true, false));
        assertTrue(alwaysTrue.applyAsBoolean(false, true));
        assertTrue(alwaysTrue.applyAsBoolean(false, false));
    }

    @Test
    public void testAlwaysFalse() {
        BooleanBinaryOperator alwaysFalse = (left, right) -> false;

        assertFalse(alwaysFalse.applyAsBoolean(true, true));
        assertFalse(alwaysFalse.applyAsBoolean(true, false));
        assertFalse(alwaysFalse.applyAsBoolean(false, true));
        assertFalse(alwaysFalse.applyAsBoolean(false, false));
    }

    @Test
    public void testReturnLeft() {
        BooleanBinaryOperator returnLeft = (left, right) -> left;

        assertTrue(returnLeft.applyAsBoolean(true, false));
        assertFalse(returnLeft.applyAsBoolean(false, true));
    }

    @Test
    public void testReturnRight() {
        BooleanBinaryOperator returnRight = (left, right) -> right;

        assertFalse(returnRight.applyAsBoolean(true, false));
        assertTrue(returnRight.applyAsBoolean(false, true));
    }

    @Test
    public void testApplyAsBooleanWithException() {
        BooleanBinaryOperator operator = (left, right) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> operator.applyAsBoolean(true, false));
    }

    @Test
    public void testAnonymousClass() {
        BooleanBinaryOperator operator = new BooleanBinaryOperator() {
            @Override
            public boolean applyAsBoolean(boolean left, boolean right) {
                return left && !right;
            }
        };

        assertFalse(operator.applyAsBoolean(true, true));
        assertTrue(operator.applyAsBoolean(true, false));
        assertFalse(operator.applyAsBoolean(false, true));
        assertFalse(operator.applyAsBoolean(false, false));
    }

    @Test
    public void testAllCombinations() {
        BooleanBinaryOperator operator = (left, right) -> left && right;

        boolean[][] testCases = { { true, true, true }, { true, false, false }, { false, true, false }, { false, false, false } };

        for (boolean[] testCase : testCases) {
            boolean result = operator.applyAsBoolean(testCase[0], testCase[1]);
            assertEquals(testCase[2], result, String.format("Expected %b for inputs (%b, %b)", testCase[2], testCase[0], testCase[1]));
        }
    }
}
