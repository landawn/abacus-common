package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanUnaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsBoolean() {
        BooleanUnaryOperator operator = operand -> !operand;

        assertFalse(operator.applyAsBoolean(true));
        assertTrue(operator.applyAsBoolean(false));
    }

    @Test
    public void testApplyAsBooleanWithLambda() {
        BooleanUnaryOperator operator = operand -> operand;

        assertTrue(operator.applyAsBoolean(true));
        assertFalse(operator.applyAsBoolean(false));
    }

    @Test
    public void testIdentity() {
        BooleanUnaryOperator identity = BooleanUnaryOperator.identity();

        assertTrue(identity.applyAsBoolean(true));
        assertFalse(identity.applyAsBoolean(false));
    }

    @Test
    public void testNegation() {
        BooleanUnaryOperator negation = operand -> !operand;

        assertFalse(negation.applyAsBoolean(true));
        assertTrue(negation.applyAsBoolean(false));
    }

    @Test
    public void testCompose() {
        BooleanUnaryOperator negate = operand -> !operand;
        BooleanUnaryOperator identity = operand -> operand;

        BooleanUnaryOperator composed = identity.compose(negate);

        assertFalse(composed.applyAsBoolean(true));  // negate(true) = false, identity(false) = false
        assertTrue(composed.applyAsBoolean(false));  // negate(false) = true, identity(true) = true
    }

    @Test
    public void testAndThen() {
        BooleanUnaryOperator identity = operand -> operand;
        BooleanUnaryOperator negate = operand -> !operand;

        BooleanUnaryOperator chained = identity.andThen(negate);

        assertFalse(chained.applyAsBoolean(true));  // identity(true) = true, negate(true) = false
        assertTrue(chained.applyAsBoolean(false)); // identity(false) = false, negate(false) = true
    }

    @Test
    public void testComposeMultiple() {
        BooleanUnaryOperator negate = operand -> !operand;

        BooleanUnaryOperator doubleNegate = negate.compose(negate);

        assertTrue(doubleNegate.applyAsBoolean(true));  // negate(negate(true)) = true
        assertFalse(doubleNegate.applyAsBoolean(false)); // negate(negate(false)) = false
    }

    @Test
    public void testAndThenMultiple() {
        BooleanUnaryOperator negate = operand -> !operand;

        BooleanUnaryOperator doubleNegate = negate.andThen(negate);

        assertTrue(doubleNegate.applyAsBoolean(true));  // negate(true) = false, negate(false) = true
        assertFalse(doubleNegate.applyAsBoolean(false)); // negate(false) = true, negate(true) = false
    }

    @Test
    public void testApplyAsBooleanWithException() {
        BooleanUnaryOperator operator = operand -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> operator.applyAsBoolean(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanUnaryOperator operator = new BooleanUnaryOperator() {
            @Override
            public boolean applyAsBoolean(boolean operand) {
                return !operand;
            }
        };

        assertFalse(operator.applyAsBoolean(true));
        assertTrue(operator.applyAsBoolean(false));
    }

    @Test
    public void testIdentityNotNull() {
        assertNotNull(BooleanUnaryOperator.identity());
    }

    @Test
    public void testConstantTrue() {
        BooleanUnaryOperator constantTrue = operand -> true;

        assertTrue(constantTrue.applyAsBoolean(true));
        assertTrue(constantTrue.applyAsBoolean(false));
    }

    @Test
    public void testConstantFalse() {
        BooleanUnaryOperator constantFalse = operand -> false;

        assertFalse(constantFalse.applyAsBoolean(true));
        assertFalse(constantFalse.applyAsBoolean(false));
    }
}
