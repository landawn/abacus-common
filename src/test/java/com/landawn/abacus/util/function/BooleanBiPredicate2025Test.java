package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanBiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BooleanBiPredicate predicate = (t, u) -> t && u;

        assertTrue(predicate.test(true, true));
        assertFalse(predicate.test(true, false));
        assertFalse(predicate.test(false, true));
        assertFalse(predicate.test(false, false));
    }

    @Test
    public void testTestWithLambda() {
        BooleanBiPredicate predicate = (t, u) -> t || u;

        assertTrue(predicate.test(true, true));
        assertTrue(predicate.test(true, false));
        assertTrue(predicate.test(false, true));
        assertFalse(predicate.test(false, false));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(BooleanBiPredicate.ALWAYS_TRUE.test(true, true));
        assertTrue(BooleanBiPredicate.ALWAYS_TRUE.test(true, false));
        assertTrue(BooleanBiPredicate.ALWAYS_TRUE.test(false, true));
        assertTrue(BooleanBiPredicate.ALWAYS_TRUE.test(false, false));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(BooleanBiPredicate.ALWAYS_FALSE.test(true, true));
        assertFalse(BooleanBiPredicate.ALWAYS_FALSE.test(true, false));
        assertFalse(BooleanBiPredicate.ALWAYS_FALSE.test(false, true));
        assertFalse(BooleanBiPredicate.ALWAYS_FALSE.test(false, false));
    }

    @Test
    public void testBothTrue() {
        assertTrue(BooleanBiPredicate.BOTH_TRUE.test(true, true));
        assertFalse(BooleanBiPredicate.BOTH_TRUE.test(true, false));
        assertFalse(BooleanBiPredicate.BOTH_TRUE.test(false, true));
        assertFalse(BooleanBiPredicate.BOTH_TRUE.test(false, false));
    }

    @Test
    public void testBothFalse() {
        assertFalse(BooleanBiPredicate.BOTH_FALSE.test(true, true));
        assertFalse(BooleanBiPredicate.BOTH_FALSE.test(true, false));
        assertFalse(BooleanBiPredicate.BOTH_FALSE.test(false, true));
        assertTrue(BooleanBiPredicate.BOTH_FALSE.test(false, false));
    }

    @Test
    public void testEqual() {
        assertTrue(BooleanBiPredicate.EQUAL.test(true, true));
        assertFalse(BooleanBiPredicate.EQUAL.test(true, false));
        assertFalse(BooleanBiPredicate.EQUAL.test(false, true));
        assertTrue(BooleanBiPredicate.EQUAL.test(false, false));
    }

    @Test
    public void testNotEqual() {
        assertFalse(BooleanBiPredicate.NOT_EQUAL.test(true, true));
        assertTrue(BooleanBiPredicate.NOT_EQUAL.test(true, false));
        assertTrue(BooleanBiPredicate.NOT_EQUAL.test(false, true));
        assertFalse(BooleanBiPredicate.NOT_EQUAL.test(false, false));
    }

    @Test
    public void testNegate() {
        BooleanBiPredicate predicate = (t, u) -> t && u;
        BooleanBiPredicate negated = predicate.negate();

        assertFalse(negated.test(true, true));
        assertTrue(negated.test(true, false));
        assertTrue(negated.test(false, true));
        assertTrue(negated.test(false, false));
    }

    @Test
    public void testAnd() {
        BooleanBiPredicate predicate1 = (t, u) -> t;
        BooleanBiPredicate predicate2 = (t, u) -> u;

        BooleanBiPredicate combined = predicate1.and(predicate2);

        assertTrue(combined.test(true, true));
        assertFalse(combined.test(true, false));
        assertFalse(combined.test(false, true));
        assertFalse(combined.test(false, false));
    }

    @Test
    public void testAndShortCircuit() {
        BooleanBiPredicate firstFalse = (t, u) -> false;
        BooleanBiPredicate shouldNotExecute = (t, u) -> {
            throw new RuntimeException("Should not execute");
        };

        BooleanBiPredicate combined = firstFalse.and(shouldNotExecute);

        assertFalse(combined.test(true, true)); // Should not throw exception
    }

    @Test
    public void testOr() {
        BooleanBiPredicate predicate1 = (t, u) -> t;
        BooleanBiPredicate predicate2 = (t, u) -> u;

        BooleanBiPredicate combined = predicate1.or(predicate2);

        assertTrue(combined.test(true, true));
        assertTrue(combined.test(true, false));
        assertTrue(combined.test(false, true));
        assertFalse(combined.test(false, false));
    }

    @Test
    public void testOrShortCircuit() {
        BooleanBiPredicate firstTrue = (t, u) -> true;
        BooleanBiPredicate shouldNotExecute = (t, u) -> {
            throw new RuntimeException("Should not execute");
        };

        BooleanBiPredicate combined = firstTrue.or(shouldNotExecute);

        assertTrue(combined.test(true, true)); // Should not throw exception
    }

    @Test
    public void testComplexCombination() {
        BooleanBiPredicate bothTrue = BooleanBiPredicate.BOTH_TRUE;
        BooleanBiPredicate bothFalse = BooleanBiPredicate.BOTH_FALSE;

        BooleanBiPredicate complex = bothTrue.or(bothFalse);

        assertTrue(complex.test(true, true));
        assertFalse(complex.test(true, false));
        assertFalse(complex.test(false, true));
        assertTrue(complex.test(false, false));
    }

    @Test
    public void testNegateAfterAnd() {
        BooleanBiPredicate predicate1 = (t, u) -> t;
        BooleanBiPredicate predicate2 = (t, u) -> u;

        BooleanBiPredicate combined = predicate1.and(predicate2).negate();

        assertFalse(combined.test(true, true));
        assertTrue(combined.test(true, false));
    }

    @Test
    public void testTestWithException() {
        BooleanBiPredicate predicate = (t, u) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> predicate.test(true, false));
    }

    @Test
    public void testAnonymousClass() {
        BooleanBiPredicate predicate = new BooleanBiPredicate() {
            @Override
            public boolean test(boolean t, boolean u) {
                return t != u;  // XOR
            }
        };

        assertFalse(predicate.test(true, true));
        assertTrue(predicate.test(true, false));
        assertTrue(predicate.test(false, true));
        assertFalse(predicate.test(false, false));
    }

    @Test
    public void testXorLogic() {
        BooleanBiPredicate xor = (t, u) -> t != u;

        assertFalse(xor.test(true, true));
        assertTrue(xor.test(true, false));
        assertTrue(xor.test(false, true));
        assertFalse(xor.test(false, false));
    }

    @Test
    public void testImpliesLogic() {
        BooleanBiPredicate implies = (t, u) -> !t || u;

        assertTrue(implies.test(true, true));
        assertFalse(implies.test(true, false));
        assertTrue(implies.test(false, true));
        assertTrue(implies.test(false, false));
    }
}
