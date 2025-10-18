package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongToDoubleFunction2025Test extends TestBase {

    @Test
    public void testApplyAsDouble() {
        final LongToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(42L);
        assertEquals(42.0, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withLambda() {
        final LongToDoubleFunction function = value -> value / 2.0;
        final double result = function.applyAsDouble(10L);
        assertEquals(5.0, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withAnonymousClass() {
        final LongToDoubleFunction function = new LongToDoubleFunction() {
            @Override
            public double applyAsDouble(final long value) {
                return value * 1.5;
            }
        };

        final double result = function.applyAsDouble(10L);
        assertEquals(15.0, result, 0.001);
    }

    @Test
    public void testDEFAULT() {
        final double result = LongToDoubleFunction.DEFAULT.applyAsDouble(42L);
        assertEquals(42.0, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withNegativeValue() {
        final LongToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(-42L);
        assertEquals(-42.0, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withZero() {
        final LongToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(0L);
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withMaxValue() {
        final LongToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(Long.MAX_VALUE);
        assertNotNull(result);
    }

    @Test
    public void testApplyAsDouble_withMinValue() {
        final LongToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(Long.MIN_VALUE);
        assertNotNull(result);
    }

    @Test
    public void testApplyAsDouble_sqrt() {
        final LongToDoubleFunction function = value -> Math.sqrt(value);
        final double result = function.applyAsDouble(16L);
        assertEquals(4.0, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_log() {
        final LongToDoubleFunction function = value -> Math.log(value);
        final double result = function.applyAsDouble(10L);
        assertEquals(Math.log(10), result, 0.001);
    }

    @Test
    public void testApplyAsDouble_scaling() {
        final LongToDoubleFunction function = value -> value / 100.0; // Convert cents to dollars
        final double result = function.applyAsDouble(250L);
        assertEquals(2.5, result, 0.001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongToDoubleFunction function = value -> (double) value;
        assertNotNull(function);
        assertEquals(42.0, function.applyAsDouble(42L), 0.001);
    }

    @Test
    public void testMethodReference() {
        final LongToDoubleFunction function = this::convertToDouble;
        final double result = function.applyAsDouble(42L);
        assertEquals(84.0, result, 0.001);
    }

    private double convertToDouble(final long value) {
        return value * 2.0;
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final java.util.function.LongToDoubleFunction javaFunction = value -> (double) value;
        final LongToDoubleFunction abacusFunction = javaFunction::applyAsDouble;

        final double result = abacusFunction.applyAsDouble(42L);
        assertEquals(42.0, result, 0.001);
    }
}
