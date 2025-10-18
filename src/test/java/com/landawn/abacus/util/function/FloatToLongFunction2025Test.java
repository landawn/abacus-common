package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatToLongFunction2025Test extends TestBase {

    @Test
    public void testApplyAsLong() {
        final FloatToLongFunction function = value -> (long) value;
        final long result = function.applyAsLong(42.5f);
        assertEquals(42L, result);
    }

    @Test
    public void testApplyAsLong_withLambda() {
        final FloatToLongFunction function = value -> Math.round(value);
        final long result = function.applyAsLong(10.7f);
        assertEquals(11L, result);
    }

    @Test
    public void testDEFAULT() {
        final long result = FloatToLongFunction.DEFAULT.applyAsLong(42.5f);
        assertEquals(42L, result);
    }

    @Test
    public void testApplyAsLong_withNegativeValue() {
        final FloatToLongFunction function = value -> (long) value;
        final long result = function.applyAsLong(-42.5f);
        assertEquals(-42L, result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatToLongFunction function = value -> (long) value;
        assertNotNull(function);
        assertEquals(42L, function.applyAsLong(42.5f));
    }
}
