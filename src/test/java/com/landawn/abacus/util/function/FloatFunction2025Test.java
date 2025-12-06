package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        final FloatFunction<String> function = val -> "Value: " + val;
        final String result = function.apply(42.5f);
        assertEquals("Value: 42.5", result);
    }

    @Test
    public void testApply_withLambda() {
        final FloatFunction<Double> function = val -> (double) val * 2;
        final Double result = function.apply(21.25f);
        assertEquals(42.5, result, 0.001);
    }

    @Test
    public void testAndThen() {
        final FloatFunction<String> function = String::valueOf;
        final java.util.function.Function<String, Integer> after = String::length;

        final FloatFunction<Integer> composed = function.andThen(after);
        final Integer result = composed.apply(123.45f);

        assertEquals(6, result);   // "123.45".length()
    }

    @Test
    public void testBOX() {
        final Float result = FloatFunction.BOX.apply(42.5f);
        assertEquals(42.5f, result, 0.001f);
    }

    @Test
    public void testIdentity() {
        final FloatFunction<Float> identity = FloatFunction.identity();
        final Float result = identity.apply(123.45f);
        assertEquals(123.45f, result, 0.001f);
    }

    @Test
    public void testApply_returnsNull() {
        final FloatFunction<String> function = val -> null;
        final String result = function.apply(1.0f);
        assertNull(result);
    }

    @Test
    public void testApply_withNegativeValue() {
        final FloatFunction<String> function = val -> "Value: " + val;
        final String result = function.apply(-42.5f);
        assertEquals("Value: -42.5", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatFunction<String> function = val -> "test";
        assertNotNull(function.apply(1.0f));
    }
}
