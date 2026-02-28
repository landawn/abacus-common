package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleBiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        DoubleBiPredicate predicate = (t, u) -> t > u;
        assertTrue(predicate.test(5.0, 3.0));
        assertFalse(predicate.test(2.0, 4.0));
    }

    @Test
    public void testTest_WithMethodReference() {
        DoubleBiPredicate predicate = (t, u) -> Double.compare(t, u) == 0;
        assertTrue(predicate.test(5.0, 5.0));
        assertFalse(predicate.test(5.0, 3.0));
    }

    @Test
    public void testTest_WithAnonymousClass() {
        DoubleBiPredicate predicate = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return t + u > 10.0;
            }
        };

        assertTrue(predicate.test(6.0, 5.0));
        assertFalse(predicate.test(2.0, 3.0));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(DoubleBiPredicate.ALWAYS_TRUE.test(1.0, 2.0));
        assertTrue(DoubleBiPredicate.ALWAYS_TRUE.test(-1.0, -2.0));
        assertTrue(DoubleBiPredicate.ALWAYS_TRUE.test(0.0, 0.0));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(DoubleBiPredicate.ALWAYS_FALSE.test(1.0, 2.0));
        assertFalse(DoubleBiPredicate.ALWAYS_FALSE.test(-1.0, -2.0));
        assertFalse(DoubleBiPredicate.ALWAYS_FALSE.test(0.0, 0.0));
    }

    @Test
    public void testEqual() {
        assertTrue(DoubleBiPredicate.EQUAL.test(5.0, 5.0));
        assertTrue(DoubleBiPredicate.EQUAL.test(0.0, 0.0));
        assertFalse(DoubleBiPredicate.EQUAL.test(5.0, 3.0));
    }

    @Test
    public void testNotEqual() {
        assertTrue(DoubleBiPredicate.NOT_EQUAL.test(5.0, 3.0));
        assertFalse(DoubleBiPredicate.NOT_EQUAL.test(5.0, 5.0));
    }

    @Test
    public void testGreaterThan() {
        assertTrue(DoubleBiPredicate.GREATER_THAN.test(5.0, 3.0));
        assertFalse(DoubleBiPredicate.GREATER_THAN.test(3.0, 5.0));
        assertFalse(DoubleBiPredicate.GREATER_THAN.test(5.0, 5.0));
    }

    @Test
    public void testGreaterEqual() {
        assertTrue(DoubleBiPredicate.GREATER_EQUAL.test(5.0, 3.0));
        assertTrue(DoubleBiPredicate.GREATER_EQUAL.test(5.0, 5.0));
        assertFalse(DoubleBiPredicate.GREATER_EQUAL.test(3.0, 5.0));
    }

    @Test
    public void testLessThan() {
        assertTrue(DoubleBiPredicate.LESS_THAN.test(3.0, 5.0));
        assertFalse(DoubleBiPredicate.LESS_THAN.test(5.0, 3.0));
        assertFalse(DoubleBiPredicate.LESS_THAN.test(5.0, 5.0));
    }

    @Test
    public void testLessEqual() {
        assertTrue(DoubleBiPredicate.LESS_EQUAL.test(3.0, 5.0));
        assertTrue(DoubleBiPredicate.LESS_EQUAL.test(5.0, 5.0));
        assertFalse(DoubleBiPredicate.LESS_EQUAL.test(5.0, 3.0));
    }

    @Test
    public void testNegate() {
        DoubleBiPredicate predicate = (t, u) -> t > u;
        DoubleBiPredicate negated = predicate.negate();

        assertFalse(negated.test(5.0, 3.0));
        assertTrue(negated.test(2.0, 4.0));
    }

    @Test
    public void testAnd() {
        DoubleBiPredicate predicate1 = (t, u) -> t > 0;
        DoubleBiPredicate predicate2 = (t, u) -> u > 0;

        DoubleBiPredicate combined = predicate1.and(predicate2);

        assertTrue(combined.test(1.0, 2.0));
        assertFalse(combined.test(-1.0, 2.0));
        assertFalse(combined.test(1.0, -2.0));
        assertFalse(combined.test(-1.0, -2.0));
    }

    @Test
    public void testOr() {
        DoubleBiPredicate predicate1 = (t, u) -> t > 5.0;
        DoubleBiPredicate predicate2 = (t, u) -> u > 5.0;

        DoubleBiPredicate combined = predicate1.or(predicate2);

        assertTrue(combined.test(6.0, 2.0));
        assertTrue(combined.test(2.0, 6.0));
        assertTrue(combined.test(6.0, 6.0));
        assertFalse(combined.test(2.0, 2.0));
    }

    @Test
    public void testComplex_AndOrNegate() {
        DoubleBiPredicate predicate = ((DoubleBiPredicate) (t, u) -> t > 5.0).and((t, u) -> u < 10.0).or((t, u) -> t == u).negate();

        assertFalse(predicate.test(6.0, 8.0)); // (6>5 && 8<10) = true, negated = false
        assertFalse(predicate.test(3.0, 3.0)); // (3==3) = true, negated = false
        assertTrue(predicate.test(3.0, 15.0)); // (3>5 && 15<10) = false, (3==15) = false, negated = true
    }

    @Test
    public void testWithNegativeValues() {
        DoubleBiPredicate predicate = (t, u) -> t < u;
        assertTrue(predicate.test(-5.0, -3.0));
        assertFalse(predicate.test(-3.0, -5.0));
    }

    @Test
    public void testWithZeroValues() {
        DoubleBiPredicate predicate = DoubleBiPredicate.EQUAL;
        assertTrue(predicate.test(0.0, 0.0));
        assertFalse(predicate.test(0.0, 1.0));
    }

    @Test
    public void testWithSpecialValues() {
        DoubleBiPredicate predicate = (t, u) -> !Double.isNaN(t) && !Double.isNaN(u);

        assertTrue(predicate.test(1.0, 2.0));
        assertFalse(predicate.test(Double.NaN, 2.0));
        assertFalse(predicate.test(1.0, Double.NaN));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleBiPredicate lambda = (t, u) -> true;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.test(1.0, 2.0));
    }
}
