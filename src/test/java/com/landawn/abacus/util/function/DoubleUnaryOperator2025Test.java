package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleUnaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsDouble() {
        DoubleUnaryOperator operator = operand -> operand * 2;
        double result = operator.applyAsDouble(5.0);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithAnonymousClass() {
        DoubleUnaryOperator operator = new DoubleUnaryOperator() {
            @Override
            public double applyAsDouble(double operand) {
                return operand * operand;
            }
        };

        double result = operator.applyAsDouble(5.0);
        assertEquals(25.0, result, 0.0001);
    }

    @Test
    public void testCompose() {
        DoubleUnaryOperator multiplyBy2 = operand -> operand * 2;
        DoubleUnaryOperator add3 = operand -> operand + 3;

        // compose: first add3, then multiplyBy2: (x + 3) * 2
        DoubleUnaryOperator composed = multiplyBy2.compose(add3);
        double result = composed.applyAsDouble(5.0);
        assertEquals(16.0, result, 0.0001);   // (5 + 3) * 2 = 16
    }

    @Test
    public void testAndThen() {
        DoubleUnaryOperator multiplyBy2 = operand -> operand * 2;
        DoubleUnaryOperator add3 = operand -> operand + 3;

        // andThen: first multiplyBy2, then add3: (x * 2) + 3
        DoubleUnaryOperator composed = multiplyBy2.andThen(add3);
        double result = composed.applyAsDouble(5.0);
        assertEquals(13.0, result, 0.0001);   // (5 * 2) + 3 = 13
    }

    @Test
    public void testIdentity() {
        DoubleUnaryOperator identity = DoubleUnaryOperator.identity();
        assertEquals(5.0, identity.applyAsDouble(5.0), 0.0001);
        assertEquals(-5.0, identity.applyAsDouble(-5.0), 0.0001);
        assertEquals(0.0, identity.applyAsDouble(0.0), 0.0001);
    }

    @Test
    public void testCompose_MultipleChains() {
        DoubleUnaryOperator add1 = operand -> operand + 1;
        DoubleUnaryOperator multiplyBy2 = operand -> operand * 2;
        DoubleUnaryOperator subtract3 = operand -> operand - 3;

        // ((x + 1) * 2) - 3
        DoubleUnaryOperator composed = subtract3.compose(multiplyBy2).compose(add1);
        double result = composed.applyAsDouble(5.0);
        assertEquals(9.0, result, 0.0001);   // ((5 + 1) * 2) - 3 = 9
    }

    @Test
    public void testAndThen_MultipleChains() {
        DoubleUnaryOperator add1 = operand -> operand + 1;
        DoubleUnaryOperator multiplyBy2 = operand -> operand * 2;
        DoubleUnaryOperator subtract3 = operand -> operand - 3;

        // (x + 1) * 2 - 3
        DoubleUnaryOperator composed = add1.andThen(multiplyBy2).andThen(subtract3);
        double result = composed.applyAsDouble(5.0);
        assertEquals(9.0, result, 0.0001);   // ((5 + 1) * 2) - 3 = 9
    }

    @Test
    public void testApplyAsDouble_MathOperations() {
        DoubleUnaryOperator sqrt = Math::sqrt;
        assertEquals(5.0, sqrt.applyAsDouble(25.0), 0.0001);

        DoubleUnaryOperator abs = Math::abs;
        assertEquals(5.0, abs.applyAsDouble(-5.0), 0.0001);

        DoubleUnaryOperator negate = operand -> -operand;
        assertEquals(-5.0, negate.applyAsDouble(5.0), 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithNegativeValues() {
        DoubleUnaryOperator operator = operand -> operand * 2;
        double result = operator.applyAsDouble(-5.0);
        assertEquals(-10.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithZero() {
        DoubleUnaryOperator operator = operand -> operand * 2;
        double result = operator.applyAsDouble(0.0);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    public void testCompose_WithIdentity() {
        DoubleUnaryOperator multiplyBy2 = operand -> operand * 2;
        DoubleUnaryOperator composed = multiplyBy2.compose(DoubleUnaryOperator.identity());
        double result = composed.applyAsDouble(5.0);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testAndThen_WithIdentity() {
        DoubleUnaryOperator multiplyBy2 = operand -> operand * 2;
        DoubleUnaryOperator composed = multiplyBy2.andThen(DoubleUnaryOperator.identity());
        double result = composed.applyAsDouble(5.0);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoubleUnaryOperator operator = operand -> operand * 2;
        java.util.function.DoubleUnaryOperator javaOperator = operator;
        double result = javaOperator.applyAsDouble(5.0);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleUnaryOperator lambda = operand -> operand * 2;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.applyAsDouble(1.0));
    }
}
