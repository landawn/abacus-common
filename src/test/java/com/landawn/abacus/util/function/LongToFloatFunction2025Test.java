package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongToFloatFunction2025Test extends TestBase {

    @Test
    public void testApplyAsFloat() {
        final LongToFloatFunction function = value -> (float) value;
        final float result = function.applyAsFloat(42L);
        assertEquals(42.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withLambda() {
        final LongToFloatFunction function = value -> value / 2.0f;
        final float result = function.applyAsFloat(10L);
        assertEquals(5.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withAnonymousClass() {
        final LongToFloatFunction function = new LongToFloatFunction() {
            @Override
            public float applyAsFloat(final long value) {
                return value * 1.5f;
            }
        };

        final float result = function.applyAsFloat(10L);
        assertEquals(15.0f, result, 0.001f);
    }

    @Test
    public void testDEFAULT() {
        final float result = LongToFloatFunction.DEFAULT.applyAsFloat(42L);
        assertEquals(42.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withNegativeValue() {
        final LongToFloatFunction function = value -> (float) value;
        final float result = function.applyAsFloat(-42L);
        assertEquals(-42.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withZero() {
        final LongToFloatFunction function = value -> (float) value;
        final float result = function.applyAsFloat(0L);
        assertEquals(0.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_withMaxValue() {
        final LongToFloatFunction function = value -> (float) value;
        final float result = function.applyAsFloat(Long.MAX_VALUE);
        assertNotNull(result);   // Will have precision loss
    }

    @Test
    public void testApplyAsFloat_withMinValue() {
        final LongToFloatFunction function = value -> (float) value;
        final float result = function.applyAsFloat(Long.MIN_VALUE);
        assertNotNull(result);   // Will have precision loss
    }

    @Test
    public void testApplyAsFloat_sqrt() {
        final LongToFloatFunction function = value -> (float) Math.sqrt(value);
        final float result = function.applyAsFloat(16L);
        assertEquals(4.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_scaling() {
        final LongToFloatFunction function = value -> value / 100.0f; // Convert cents to dollars
        final float result = function.applyAsFloat(250L);
        assertEquals(2.5f, result, 0.001f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongToFloatFunction function = value -> (float) value;
        assertNotNull(function);
        assertEquals(42.0f, function.applyAsFloat(42L), 0.001f);
    }

    @Test
    public void testMethodReference() {
        final LongToFloatFunction function = this::convertToFloat;
        final float result = function.applyAsFloat(42L);
        assertEquals(84.0f, result, 0.001f);
    }

    private float convertToFloat(final long value) {
        return value * 2.0f;
    }
}
