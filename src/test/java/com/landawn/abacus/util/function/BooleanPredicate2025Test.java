package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BooleanPredicate predicate = value -> value;

        assertTrue(predicate.test(true));
        assertFalse(predicate.test(false));
    }

    @Test
    public void testTestWithLambda() {
        BooleanPredicate predicate = value -> !value;

        assertFalse(predicate.test(true));
        assertTrue(predicate.test(false));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(BooleanPredicate.ALWAYS_TRUE.test(true));
        assertTrue(BooleanPredicate.ALWAYS_TRUE.test(false));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(BooleanPredicate.ALWAYS_FALSE.test(true));
        assertFalse(BooleanPredicate.ALWAYS_FALSE.test(false));
    }

    @Test
    public void testIsTrue() {
        assertTrue(BooleanPredicate.IS_TRUE.test(true));
        assertFalse(BooleanPredicate.IS_TRUE.test(false));
    }

    @Test
    public void testIsFalse() {
        assertFalse(BooleanPredicate.IS_FALSE.test(true));
        assertTrue(BooleanPredicate.IS_FALSE.test(false));
    }

    @Test
    public void testOf() {
        BooleanPredicate original = value -> value;
        BooleanPredicate returned = BooleanPredicate.of(original);

        assertNotNull(returned);
        assertTrue(returned.test(true));
        assertFalse(returned.test(false));
    }

    @Test
    public void testNegate() {
        BooleanPredicate predicate = value -> value;
        BooleanPredicate negated = predicate.negate();

        assertFalse(negated.test(true));
        assertTrue(negated.test(false));
    }

    @Test
    public void testAnd() {
        BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
        BooleanPredicate alwaysTrue = BooleanPredicate.ALWAYS_TRUE;

        BooleanPredicate combined = isTrue.and(alwaysTrue);

        assertTrue(combined.test(true)); // true && true
        assertFalse(combined.test(false)); // false && true
    }

    @Test
    public void testAndShortCircuit() {
        BooleanPredicate firstFalse = value -> false;
        BooleanPredicate shouldNotExecute = value -> {
            throw new RuntimeException("Should not execute");
        };

        BooleanPredicate combined = firstFalse.and(shouldNotExecute);

        assertFalse(combined.test(true)); // Should not throw exception
    }

    @Test
    public void testOr() {
        BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
        BooleanPredicate isFalse = BooleanPredicate.IS_FALSE;

        BooleanPredicate combined = isTrue.or(isFalse);

        assertTrue(combined.test(true)); // true || false
        assertTrue(combined.test(false)); // false || true
    }

    @Test
    public void testOrShortCircuit() {
        BooleanPredicate firstTrue = value -> true;
        BooleanPredicate shouldNotExecute = value -> {
            throw new RuntimeException("Should not execute");
        };

        BooleanPredicate combined = firstTrue.or(shouldNotExecute);

        assertTrue(combined.test(true)); // Should not throw exception
    }

    @Test
    public void testComplexCombination() {
        BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
        BooleanPredicate alwaysTrue = BooleanPredicate.ALWAYS_TRUE;

        BooleanPredicate complex = isTrue.and(alwaysTrue);

        assertTrue(complex.test(true));
        assertFalse(complex.test(false));
    }

    @Test
    public void testNegateAfterAnd() {
        BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
        BooleanPredicate alwaysTrue = BooleanPredicate.ALWAYS_TRUE;

        BooleanPredicate combined = isTrue.and(alwaysTrue).negate();

        assertFalse(combined.test(true));
        assertTrue(combined.test(false));
    }

    @Test
    public void testTestWithException() {
        BooleanPredicate predicate = value -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> predicate.test(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanPredicate predicate = new BooleanPredicate() {
            @Override
            public boolean test(boolean value) {
                return !value; // Negation
            }
        };

        assertFalse(predicate.test(true));
        assertTrue(predicate.test(false));
    }

    @Test
    public void testConstantsNotNull() {
        assertNotNull(BooleanPredicate.ALWAYS_TRUE);
        assertNotNull(BooleanPredicate.ALWAYS_FALSE);
        assertNotNull(BooleanPredicate.IS_TRUE);
        assertNotNull(BooleanPredicate.IS_FALSE);
    }

    @Test
    public void testDoubleNegate() {
        BooleanPredicate predicate = BooleanPredicate.IS_TRUE;
        BooleanPredicate doubleNegated = predicate.negate().negate();

        assertTrue(doubleNegated.test(true));
        assertFalse(doubleNegated.test(false));
    }

    @Test
    public void testAndWithSelf() {
        BooleanPredicate predicate = BooleanPredicate.IS_TRUE;
        BooleanPredicate combined = predicate.and(predicate);

        assertTrue(combined.test(true));
        assertFalse(combined.test(false));
    }

    @Test
    public void testOrWithSelf() {
        BooleanPredicate predicate = BooleanPredicate.IS_TRUE;
        BooleanPredicate combined = predicate.or(predicate);

        assertTrue(combined.test(true));
        assertFalse(combined.test(false));
    }
}
