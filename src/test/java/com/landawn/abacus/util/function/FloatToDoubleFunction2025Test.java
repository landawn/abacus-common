package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatToDoubleFunction2025Test extends TestBase {

    @Test
    public void testApplyAsDouble() {
        final FloatToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(42.5f);
        assertEquals(42.5, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withLambda() {
        final FloatToDoubleFunction function = value -> value / 2.0;
        final double result = function.applyAsDouble(10.0f);
        assertEquals(5.0, result, 0.001);
    }

    @Test
    public void testDEFAULT() {
        final double result = FloatToDoubleFunction.DEFAULT.applyAsDouble(42.5f);
        assertEquals(42.5, result, 0.001);
    }

    @Test
    public void testApplyAsDouble_withNegativeValue() {
        final FloatToDoubleFunction function = value -> (double) value;
        final double result = function.applyAsDouble(-42.5f);
        assertEquals(-42.5, result, 0.001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatToDoubleFunction function = value -> (double) value;
        assertNotNull(function);
        assertEquals(42.5, function.applyAsDouble(42.5f), 0.001);
    }
}
