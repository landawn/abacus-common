package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanTriPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BooleanTriPredicate predicate = (a, b, c) -> a && b && c;

        assertTrue(predicate.test(true, true, true));
        assertFalse(predicate.test(true, true, false));
        assertFalse(predicate.test(true, false, true));
        assertFalse(predicate.test(false, true, true));
    }

    @Test
    public void testTestWithLambda() {
        BooleanTriPredicate predicate = (a, b, c) -> a || b || c;

        assertTrue(predicate.test(true, false, false));
        assertTrue(predicate.test(false, true, false));
        assertTrue(predicate.test(false, false, true));
        assertFalse(predicate.test(false, false, false));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(BooleanTriPredicate.ALWAYS_TRUE.test(true, true, true));
        assertTrue(BooleanTriPredicate.ALWAYS_TRUE.test(false, false, false));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(BooleanTriPredicate.ALWAYS_FALSE.test(true, true, true));
        assertFalse(BooleanTriPredicate.ALWAYS_FALSE.test(false, false, false));
    }

    @Test
    public void testNegate() {
        BooleanTriPredicate predicate = (a, b, c) -> a && b && c;
        BooleanTriPredicate negated = predicate.negate();

        assertFalse(negated.test(true, true, true));
        assertTrue(negated.test(true, true, false));
    }

    @Test
    public void testAnd() {
        BooleanTriPredicate predicate1 = (a, b, c) -> a || b;
        BooleanTriPredicate predicate2 = (a, b, c) -> b || c;

        BooleanTriPredicate combined = predicate1.and(predicate2);

        assertTrue(combined.test(true, true, false));
        assertFalse(combined.test(false, false, false));
    }

    @Test
    public void testAndShortCircuit() {
        BooleanTriPredicate firstFalse = (a, b, c) -> false;
        BooleanTriPredicate shouldNotExecute = (a, b, c) -> {
            throw new RuntimeException("Should not execute");
        };

        BooleanTriPredicate combined = firstFalse.and(shouldNotExecute);

        assertFalse(combined.test(true, true, true)); // Should not throw exception
    }

    @Test
    public void testOr() {
        BooleanTriPredicate predicate1 = (a, b, c) -> a;
        BooleanTriPredicate predicate2 = (a, b, c) -> b && c;

        BooleanTriPredicate combined = predicate1.or(predicate2);

        assertTrue(combined.test(true, false, false));
        assertTrue(combined.test(false, true, true));
        assertFalse(combined.test(false, false, false));
    }

    @Test
    public void testOrShortCircuit() {
        BooleanTriPredicate firstTrue = (a, b, c) -> true;
        BooleanTriPredicate shouldNotExecute = (a, b, c) -> {
            throw new RuntimeException("Should not execute");
        };

        BooleanTriPredicate combined = firstTrue.or(shouldNotExecute);

        assertTrue(combined.test(true, true, true)); // Should not throw exception
    }

    @Test
    public void testTestWithException() {
        BooleanTriPredicate predicate = (a, b, c) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> predicate.test(true, false, true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanTriPredicate predicate = new BooleanTriPredicate() {
            @Override
            public boolean test(boolean a, boolean b, boolean c) {
                return (a && b) || c;
            }
        };

        assertTrue(predicate.test(true, true, false));
        assertTrue(predicate.test(false, false, true));
        assertFalse(predicate.test(false, false, false));
    }

    @Test
    public void testConstantsNotNull() {
        assertNotNull(BooleanTriPredicate.ALWAYS_TRUE);
        assertNotNull(BooleanTriPredicate.ALWAYS_FALSE);
    }
}
