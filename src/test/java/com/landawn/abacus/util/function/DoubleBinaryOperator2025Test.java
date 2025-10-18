package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleBinaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsDouble() {
        DoubleBinaryOperator operator = (left, right) -> left + right;
        double result = operator.applyAsDouble(5.0, 3.0);
        assertEquals(8.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Subtraction() {
        DoubleBinaryOperator operator = (left, right) -> left - right;
        double result = operator.applyAsDouble(10.0, 3.0);
        assertEquals(7.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Multiplication() {
        DoubleBinaryOperator operator = (left, right) -> left * right;
        double result = operator.applyAsDouble(4.0, 5.0);
        assertEquals(20.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Division() {
        DoubleBinaryOperator operator = (left, right) -> left / right;
        double result = operator.applyAsDouble(20.0, 4.0);
        assertEquals(5.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithAnonymousClass() {
        DoubleBinaryOperator operator = new DoubleBinaryOperator() {
            @Override
            public double applyAsDouble(double left, double right) {
                return Math.max(left, right);
            }
        };

        double result = operator.applyAsDouble(3.5, 7.2);
        assertEquals(7.2, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithMethodReference() {
        DoubleBinaryOperator operator = Math::max;
        double result = operator.applyAsDouble(10.5, 20.5);
        assertEquals(20.5, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithNegativeValues() {
        DoubleBinaryOperator operator = (left, right) -> left + right;
        double result = operator.applyAsDouble(-5.0, -3.0);
        assertEquals(-8.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithZeroValues() {
        DoubleBinaryOperator operator = (left, right) -> left * right;
        double result = operator.applyAsDouble(0.0, 100.0);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithSpecialValues() {
        DoubleBinaryOperator operator = (left, right) -> left + right;

        // Infinity tests
        assertEquals(Double.POSITIVE_INFINITY,
                operator.applyAsDouble(Double.POSITIVE_INFINITY, 1.0), 0.0001);
        assertEquals(Double.NEGATIVE_INFINITY,
                operator.applyAsDouble(Double.NEGATIVE_INFINITY, 1.0), 0.0001);

        // NaN test
        assertTrue(Double.isNaN(operator.applyAsDouble(Double.NaN, 1.0)));
    }

    @Test
    public void testApplyAsDouble_MinOperation() {
        DoubleBinaryOperator operator = Math::min;
        double result = operator.applyAsDouble(15.5, 10.5);
        assertEquals(10.5, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_MaxOperation() {
        DoubleBinaryOperator operator = Math::max;
        double result = operator.applyAsDouble(15.5, 10.5);
        assertEquals(15.5, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_PowerOperation() {
        DoubleBinaryOperator operator = Math::pow;
        double result = operator.applyAsDouble(2.0, 3.0);
        assertEquals(8.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_ModuloOperation() {
        DoubleBinaryOperator operator = (left, right) -> left % right;
        double result = operator.applyAsDouble(10.0, 3.0);
        assertEquals(1.0, result, 0.0001);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoubleBinaryOperator operator = (left, right) -> left + right;
        java.util.function.DoubleBinaryOperator javaOperator = operator;
        double result = javaOperator.applyAsDouble(5.0, 3.0);
        assertEquals(8.0, result, 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleBinaryOperator lambda = (left, right) -> left + right;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.applyAsDouble(1.0, 2.0));
    }
}
