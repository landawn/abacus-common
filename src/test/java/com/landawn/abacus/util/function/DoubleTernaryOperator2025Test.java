package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleTernaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsDouble() {
        DoubleTernaryOperator operator = (a, b, c) -> a + b + c;
        double result = operator.applyAsDouble(1.0, 2.0, 3.0);
        assertEquals(6.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithAnonymousClass() {
        DoubleTernaryOperator operator = new DoubleTernaryOperator() {
            @Override
            public double applyAsDouble(double a, double b, double c) {
                return a * b * c;
            }
        };

        double result = operator.applyAsDouble(2.0, 3.0, 4.0);
        assertEquals(24.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Average() {
        DoubleTernaryOperator operator = (a, b, c) -> (a + b + c) / 3.0;
        double result = operator.applyAsDouble(3.0, 6.0, 9.0);
        assertEquals(6.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Max() {
        DoubleTernaryOperator operator = (a, b, c) -> Math.max(a, Math.max(b, c));
        double result = operator.applyAsDouble(5.0, 9.0, 3.0);
        assertEquals(9.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Min() {
        DoubleTernaryOperator operator = (a, b, c) -> Math.min(a, Math.min(b, c));
        double result = operator.applyAsDouble(5.0, 9.0, 3.0);
        assertEquals(3.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithNegativeValues() {
        DoubleTernaryOperator operator = (a, b, c) -> a + b + c;
        double result = operator.applyAsDouble(-1.0, -2.0, -3.0);
        assertEquals(-6.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WithZeroValues() {
        DoubleTernaryOperator operator = (a, b, c) -> a * b * c;
        double result = operator.applyAsDouble(0.0, 5.0, 10.0);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_WeightedSum() {
        DoubleTernaryOperator operator = (a, b, c) -> (a * 0.5) + (b * 0.3) + (c * 0.2);
        double result = operator.applyAsDouble(10.0, 20.0, 30.0);
        assertEquals(17.0, result, 0.0001);
    }

    @Test
    public void testApplyAsDouble_Conditional() {
        DoubleTernaryOperator operator = (a, b, c) -> a > 0 ? b : c;
        assertEquals(2.0, operator.applyAsDouble(1.0, 2.0, 3.0), 0.0001);
        assertEquals(3.0, operator.applyAsDouble(-1.0, 2.0, 3.0), 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleTernaryOperator lambda = (a, b, c) -> a + b + c;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.applyAsDouble(1.0, 2.0, 3.0));
    }
}
