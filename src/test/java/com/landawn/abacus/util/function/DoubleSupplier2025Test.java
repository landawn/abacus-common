package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleSupplier2025Test extends TestBase {

    @Test
    public void testGetAsDouble() {
        DoubleSupplier supplier = () -> 5.5;
        double result = supplier.getAsDouble();
        assertEquals(5.5, result, 0.0001);
    }

    @Test
    public void testGetAsDouble_WithMethodReference() {
        double value = 10.5;
        DoubleSupplier supplier = () -> value;
        double result = supplier.getAsDouble();
        assertEquals(10.5, result, 0.0001);
    }

    @Test
    public void testGetAsDouble_WithAnonymousClass() {
        DoubleSupplier supplier = new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return 15.5;
            }
        };

        double result = supplier.getAsDouble();
        assertEquals(15.5, result, 0.0001);
    }

    @Test
    public void testZERO() {
        DoubleSupplier supplier = DoubleSupplier.ZERO;
        double result = supplier.getAsDouble();
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    public void testZERO_MultipleInvocations() {
        DoubleSupplier supplier = DoubleSupplier.ZERO;
        assertEquals(0.0, supplier.getAsDouble(), 0.0001);
        assertEquals(0.0, supplier.getAsDouble(), 0.0001);
        assertEquals(0.0, supplier.getAsDouble(), 0.0001);
    }

    @Test
    public void testRANDOM() {
        DoubleSupplier supplier = DoubleSupplier.RANDOM;
        double result = supplier.getAsDouble();
        assertTrue(result >= 0.0 && result < 1.0);
    }

    @Test
    public void testRANDOM_MultipleInvocations() {
        DoubleSupplier supplier = DoubleSupplier.RANDOM;
        double result1 = supplier.getAsDouble();
        double result2 = supplier.getAsDouble();
        double result3 = supplier.getAsDouble();

        assertTrue(result1 >= 0.0 && result1 < 1.0);
        assertTrue(result2 >= 0.0 && result2 < 1.0);
        assertTrue(result3 >= 0.0 && result3 < 1.0);

        // Random values should be different (with high probability)
        // Note: there's a tiny chance they could be equal, but extremely unlikely
    }

    @Test
    public void testGetAsDouble_WithNegativeValue() {
        DoubleSupplier supplier = () -> -5.5;
        double result = supplier.getAsDouble();
        assertEquals(-5.5, result, 0.0001);
    }

    @Test
    public void testGetAsDouble_WithSpecialValues() {
        DoubleSupplier maxSupplier = () -> Double.MAX_VALUE;
        assertEquals(Double.MAX_VALUE, maxSupplier.getAsDouble(), 0.0001);

        DoubleSupplier minSupplier = () -> Double.MIN_VALUE;
        assertEquals(Double.MIN_VALUE, minSupplier.getAsDouble(), 0.0001);

        DoubleSupplier posInfSupplier = () -> Double.POSITIVE_INFINITY;
        assertEquals(Double.POSITIVE_INFINITY, posInfSupplier.getAsDouble(), 0.0001);

        DoubleSupplier negInfSupplier = () -> Double.NEGATIVE_INFINITY;
        assertEquals(Double.NEGATIVE_INFINITY, negInfSupplier.getAsDouble(), 0.0001);

        DoubleSupplier nanSupplier = () -> Double.NaN;
        assertTrue(Double.isNaN(nanSupplier.getAsDouble()));
    }

    @Test
    public void testGetAsDouble_WithCalculation() {
        DoubleSupplier supplier = () -> 10.0 * 2.5;
        double result = supplier.getAsDouble();
        assertEquals(25.0, result, 0.0001);
    }

    @Test
    public void testGetAsDouble_WithExternalState() {
        final double[] counter = {0.0};
        DoubleSupplier supplier = () -> {
            counter[0] += 1.0;
            return counter[0];
        };

        assertEquals(1.0, supplier.getAsDouble(), 0.0001);
        assertEquals(2.0, supplier.getAsDouble(), 0.0001);
        assertEquals(3.0, supplier.getAsDouble(), 0.0001);
    }

    @Test
    public void testGetAsDouble_WithMathOperations() {
        DoubleSupplier supplier = () -> Math.PI;
        double result = supplier.getAsDouble();
        assertEquals(Math.PI, result, 0.0001);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoubleSupplier supplier = () -> 5.5;
        java.util.function.DoubleSupplier javaSupplier = supplier;

        double result = javaSupplier.getAsDouble();
        assertEquals(5.5, result, 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleSupplier lambda = () -> 1.0;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.getAsDouble());
    }
}
