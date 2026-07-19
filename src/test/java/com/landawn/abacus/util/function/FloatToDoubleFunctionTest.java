package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

public class FloatToDoubleFunctionTest extends TestBase {

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

    @Test
    public void testThrowableFunctionCompatibility() {
        final Throwables.FloatToDoubleFunction<RuntimeException> function = FloatToDoubleFunction.DEFAULT;

        assertEquals(42.5, function.applyAsDouble(42.5f), 0.001);
    }

    @Test
    public void testDEFAULT_IsExactAndPreservesSpecialValues() {
        final FloatToDoubleFunction function = FloatToDoubleFunction.DEFAULT;

        assertEquals((double) Float.MAX_VALUE, function.applyAsDouble(Float.MAX_VALUE));
        assertEquals((double) Float.MIN_VALUE, function.applyAsDouble(Float.MIN_VALUE));
        assertEquals(Double.doubleToRawLongBits(0.0d), Double.doubleToRawLongBits(function.applyAsDouble(0.0f)));
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(function.applyAsDouble(-0.0f)));
        assertEquals(Double.POSITIVE_INFINITY, function.applyAsDouble(Float.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, function.applyAsDouble(Float.NEGATIVE_INFINITY));
        assertTrue(Double.isNaN(function.applyAsDouble(Float.NaN)));
    }
}
