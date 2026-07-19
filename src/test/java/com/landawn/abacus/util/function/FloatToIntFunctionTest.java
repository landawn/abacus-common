package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

public class FloatToIntFunctionTest extends TestBase {

    @Test
    public void testApplyAsInt() {
        final FloatToIntFunction function = value -> (int) value;
        final int result = function.applyAsInt(42.5f);
        assertEquals(42, result);
    }

    @Test
    public void testApplyAsInt_withLambda() {
        final FloatToIntFunction function = value -> Math.round(value);
        final int result = function.applyAsInt(10.7f);
        assertEquals(11, result);
    }

    @Test
    public void testDEFAULT() {
        final int result = FloatToIntFunction.DEFAULT.applyAsInt(42.5f);
        assertEquals(42, result);
    }

    @Test
    public void testApplyAsInt_withNegativeValue() {
        final FloatToIntFunction function = value -> (int) value;
        final int result = function.applyAsInt(-42.5f);
        assertEquals(-42, result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatToIntFunction function = value -> (int) value;
        assertNotNull(function);
        assertEquals(42, function.applyAsInt(42.5f));
    }

    @Test
    public void testThrowableFunctionCompatibility() {
        final Throwables.FloatToIntFunction<RuntimeException> function = FloatToIntFunction.DEFAULT;

        assertEquals(42, function.applyAsInt(42.5f));
    }

    @Test
    public void testDEFAULT_SpecialValuesAndSaturation() {
        final FloatToIntFunction function = FloatToIntFunction.DEFAULT;

        assertEquals(0, function.applyAsInt(Float.NaN));
        assertEquals(Integer.MAX_VALUE, function.applyAsInt(Float.POSITIVE_INFINITY));
        assertEquals(Integer.MIN_VALUE, function.applyAsInt(Float.NEGATIVE_INFINITY));
        assertEquals(Integer.MAX_VALUE, function.applyAsInt(Float.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, function.applyAsInt(-Float.MAX_VALUE));
    }
}
