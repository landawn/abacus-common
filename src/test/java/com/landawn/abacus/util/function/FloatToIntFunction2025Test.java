package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatToIntFunction2025Test extends TestBase {

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
}
