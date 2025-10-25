package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleTriPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        DoubleTriPredicate predicate = (a, b, c) -> a + b + c > 10.0;
        assertTrue(predicate.test(5.0, 6.0, 7.0));
        assertFalse(predicate.test(1.0, 2.0, 3.0));
    }

    @Test
    public void testTest_WithAnonymousClass() {
        DoubleTriPredicate predicate = new DoubleTriPredicate() {
            @Override
            public boolean test(double a, double b, double c) {
                return a * b * c > 20.0;
            }
        };

        assertTrue(predicate.test(3.0, 4.0, 2.0));
        assertFalse(predicate.test(1.0, 2.0, 3.0));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(DoubleTriPredicate.ALWAYS_TRUE.test(1.0, 2.0, 3.0));
        assertTrue(DoubleTriPredicate.ALWAYS_TRUE.test(-1.0, -2.0, -3.0));
        assertTrue(DoubleTriPredicate.ALWAYS_TRUE.test(0.0, 0.0, 0.0));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(DoubleTriPredicate.ALWAYS_FALSE.test(1.0, 2.0, 3.0));
        assertFalse(DoubleTriPredicate.ALWAYS_FALSE.test(-1.0, -2.0, -3.0));
        assertFalse(DoubleTriPredicate.ALWAYS_FALSE.test(0.0, 0.0, 0.0));
    }

    @Test
    public void testNegate() {
        DoubleTriPredicate predicate = (a, b, c) -> a + b + c > 10.0;
        DoubleTriPredicate negated = predicate.negate();

        assertFalse(negated.test(5.0, 6.0, 7.0));
        assertTrue(negated.test(1.0, 2.0, 3.0));
    }

    @Test
    public void testAnd() {
        DoubleTriPredicate predicate1 = (a, b, c) -> a > 0;
        DoubleTriPredicate predicate2 = (a, b, c) -> b > 0;
        DoubleTriPredicate predicate3 = (a, b, c) -> c > 0;

        DoubleTriPredicate combined = predicate1.and(predicate2).and(predicate3);

        assertTrue(combined.test(1.0, 2.0, 3.0));
        assertFalse(combined.test(-1.0, 2.0, 3.0));
        assertFalse(combined.test(1.0, -2.0, 3.0));
        assertFalse(combined.test(1.0, 2.0, -3.0));
    }

    @Test
    public void testOr() {
        DoubleTriPredicate predicate1 = (a, b, c) -> a > 10.0;
        DoubleTriPredicate predicate2 = (a, b, c) -> b > 10.0;
        DoubleTriPredicate predicate3 = (a, b, c) -> c > 10.0;

        DoubleTriPredicate combined = predicate1.or(predicate2).or(predicate3);

        assertTrue(combined.test(15.0, 2.0, 3.0));
        assertTrue(combined.test(2.0, 15.0, 3.0));
        assertTrue(combined.test(2.0, 3.0, 15.0));
        assertFalse(combined.test(2.0, 3.0, 4.0));
    }

    @Test
    public void testTriangleInequality() {
        DoubleTriPredicate isValidTriangle = (a, b, c) -> (a + b > c) && (b + c > a) && (a + c > b);

        assertTrue(isValidTriangle.test(3.0, 4.0, 5.0));
        assertFalse(isValidTriangle.test(1.0, 2.0, 10.0));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleTriPredicate lambda = (a, b, c) -> true;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.test(1.0, 2.0, 3.0));
    }
}
