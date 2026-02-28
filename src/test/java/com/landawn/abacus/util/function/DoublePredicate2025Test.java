package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoublePredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        DoublePredicate predicate = value -> value > 5.0;
        assertTrue(predicate.test(10.0));
        assertFalse(predicate.test(3.0));
    }

    @Test
    public void testTest_WithMethodReference() {
        DoublePredicate predicate = value -> value == 0.0;
        assertTrue(predicate.test(0.0));
        assertFalse(predicate.test(5.0));
    }

    @Test
    public void testTest_WithAnonymousClass() {
        DoublePredicate predicate = new DoublePredicate() {
            @Override
            public boolean test(double value) {
                return value % 2 == 0;
            }
        };

        assertTrue(predicate.test(4.0));
        assertFalse(predicate.test(5.0));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(DoublePredicate.ALWAYS_TRUE.test(5.0));
        assertTrue(DoublePredicate.ALWAYS_TRUE.test(-5.0));
        assertTrue(DoublePredicate.ALWAYS_TRUE.test(0.0));
        assertTrue(DoublePredicate.ALWAYS_TRUE.test(Double.NaN));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(DoublePredicate.ALWAYS_FALSE.test(5.0));
        assertFalse(DoublePredicate.ALWAYS_FALSE.test(-5.0));
        assertFalse(DoublePredicate.ALWAYS_FALSE.test(0.0));
        assertFalse(DoublePredicate.ALWAYS_FALSE.test(Double.NaN));
    }

    @Test
    public void testIsZero() {
        assertTrue(DoublePredicate.IS_ZERO.test(0.0));
        assertFalse(DoublePredicate.IS_ZERO.test(1.0));
        assertFalse(DoublePredicate.IS_ZERO.test(-1.0));
    }

    @Test
    public void testNotZero() {
        assertTrue(DoublePredicate.NOT_ZERO.test(1.0));
        assertTrue(DoublePredicate.NOT_ZERO.test(-1.0));
        assertFalse(DoublePredicate.NOT_ZERO.test(0.0));
    }

    @Test
    public void testIsPositive() {
        assertTrue(DoublePredicate.IS_POSITIVE.test(1.0));
        assertFalse(DoublePredicate.IS_POSITIVE.test(0.0));
        assertFalse(DoublePredicate.IS_POSITIVE.test(-1.0));
    }

    @Test
    public void testNotPositive() {
        assertTrue(DoublePredicate.NOT_POSITIVE.test(0.0));
        assertTrue(DoublePredicate.NOT_POSITIVE.test(-1.0));
        assertFalse(DoublePredicate.NOT_POSITIVE.test(1.0));
    }

    @Test
    public void testIsNegative() {
        assertTrue(DoublePredicate.IS_NEGATIVE.test(-1.0));
        assertFalse(DoublePredicate.IS_NEGATIVE.test(0.0));
        assertFalse(DoublePredicate.IS_NEGATIVE.test(1.0));
    }

    @Test
    public void testNotNegative() {
        assertTrue(DoublePredicate.NOT_NEGATIVE.test(0.0));
        assertTrue(DoublePredicate.NOT_NEGATIVE.test(1.0));
        assertFalse(DoublePredicate.NOT_NEGATIVE.test(-1.0));
    }

    @Test
    public void testNegate() {
        DoublePredicate predicate = value -> value > 5.0;
        DoublePredicate negated = predicate.negate();

        assertFalse(negated.test(10.0));
        assertTrue(negated.test(3.0));
    }

    @Test
    public void testAnd() {
        DoublePredicate predicate1 = value -> value > 0;
        DoublePredicate predicate2 = value -> value < 10;

        DoublePredicate combined = predicate1.and(predicate2);

        assertTrue(combined.test(5.0));
        assertFalse(combined.test(-1.0));
        assertFalse(combined.test(11.0));
    }

    @Test
    public void testOr() {
        DoublePredicate predicate1 = value -> value < 0;
        DoublePredicate predicate2 = value -> value > 10;

        DoublePredicate combined = predicate1.or(predicate2);

        assertTrue(combined.test(-1.0));
        assertTrue(combined.test(11.0));
        assertFalse(combined.test(5.0));
    }

    @Test
    public void testOf() {
        DoublePredicate predicate = value -> value > 5.0;
        DoublePredicate result = DoublePredicate.of(predicate);
        assertSame(predicate, result);
    }

    @Test
    public void testEqual() {
        DoublePredicate predicate = DoublePredicate.equal(5.0);
        assertTrue(predicate.test(5.0));
        assertFalse(predicate.test(3.0));
    }

    @Test
    public void testNotEqual() {
        DoublePredicate predicate = DoublePredicate.notEqual(5.0);
        assertTrue(predicate.test(3.0));
        assertFalse(predicate.test(5.0));
    }

    @Test
    public void testGreaterThan() {
        DoublePredicate predicate = DoublePredicate.greaterThan(5.0);
        assertTrue(predicate.test(10.0));
        assertFalse(predicate.test(5.0));
        assertFalse(predicate.test(3.0));
    }

    @Test
    public void testGreaterEqual() {
        DoublePredicate predicate = DoublePredicate.greaterEqual(5.0);
        assertTrue(predicate.test(10.0));
        assertTrue(predicate.test(5.0));
        assertFalse(predicate.test(3.0));
    }

    @Test
    public void testLessThan() {
        DoublePredicate predicate = DoublePredicate.lessThan(5.0);
        assertTrue(predicate.test(3.0));
        assertFalse(predicate.test(5.0));
        assertFalse(predicate.test(10.0));
    }

    @Test
    public void testLessEqual() {
        DoublePredicate predicate = DoublePredicate.lessEqual(5.0);
        assertTrue(predicate.test(3.0));
        assertTrue(predicate.test(5.0));
        assertFalse(predicate.test(10.0));
    }

    @Test
    public void testBetween() {
        DoublePredicate predicate = DoublePredicate.between(5.0, 10.0);
        assertTrue(predicate.test(7.0));
        assertFalse(predicate.test(5.0)); // exclusive
        assertFalse(predicate.test(10.0)); // exclusive
        assertFalse(predicate.test(3.0));
        assertFalse(predicate.test(11.0));
    }

    @Test
    public void testComplex_AndOrNegate() {
        DoublePredicate predicate = DoublePredicate.greaterThan(5.0).and(DoublePredicate.lessThan(10.0)).or(DoublePredicate.equal(15.0)).negate();

        assertFalse(predicate.test(7.0)); // (7>5 && 7<10) = true, negated = false
        assertFalse(predicate.test(15.0)); // (15==15) = true, negated = false
        assertTrue(predicate.test(3.0)); // (3>5 && 3<10) = false, (3==15) = false, negated = true
    }

    @Test
    public void testWithNegativeValues() {
        DoublePredicate predicate = value -> value < 0;
        assertTrue(predicate.test(-5.0));
        assertFalse(predicate.test(5.0));
    }

    @Test
    public void testWithSpecialValues() {
        DoublePredicate isNaN = Double::isNaN;
        assertTrue(isNaN.test(Double.NaN));
        assertFalse(isNaN.test(1.0));

        DoublePredicate isInfinite = Double::isInfinite;
        assertTrue(isInfinite.test(Double.POSITIVE_INFINITY));
        assertTrue(isInfinite.test(Double.NEGATIVE_INFINITY));
        assertFalse(isInfinite.test(1.0));
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoublePredicate predicate = value -> value > 5.0;
        java.util.function.DoublePredicate javaPredicate = predicate;

        assertTrue(javaPredicate.test(10.0));
        assertFalse(javaPredicate.test(3.0));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoublePredicate lambda = value -> true;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.test(1.0));
    }
}
