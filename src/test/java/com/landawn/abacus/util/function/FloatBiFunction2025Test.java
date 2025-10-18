package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatBiFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        final FloatBiFunction<String> function = (t, u) -> String.format("%.1f+%.1f", t, u);
        final String result = function.apply(10.5f, 20.3f);
        assertEquals("10.5+20.3", result);
    }

    @Test
    public void testApply_withLambda() {
        final FloatBiFunction<Float> function = (a, b) -> a * b;
        final Float result = function.apply(5.5f, 2.0f);
        assertEquals(11.0f, result, 0.001f);
    }

    @Test
    public void testApply_withAnonymousClass() {
        final FloatBiFunction<String> function = new FloatBiFunction<String>() {
            @Override
            public String apply(final float t, final float u) {
                return "Values: " + t + ", " + u;
            }
        };

        final String result = function.apply(10.5f, 20.3f);
        assertEquals("Values: 10.5, 20.3", result);
    }

    @Test
    public void testAndThen() {
        final FloatBiFunction<Float> function = (t, u) -> t + u;
        final java.util.function.Function<Float, String> after = val -> "Result: " + val;

        final FloatBiFunction<String> composed = function.andThen(after);
        final String result = composed.apply(10.5f, 20.3f);

        assertEquals("Result: 30.8", result);
    }

    @Test
    public void testApply_returnsNull() {
        final FloatBiFunction<String> function = (t, u) -> null;
        final String result = function.apply(1.0f, 2.0f);
        assertNull(result);
    }

    @Test
    public void testApply_withNegativeValues() {
        final FloatBiFunction<Float> function = (t, u) -> t + u;
        final Float result = function.apply(-10.5f, -20.3f);
        assertEquals(-30.8f, result, 0.001f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatBiFunction<String> function = (t, u) -> "test";
        assertNotNull(function.apply(1.0f, 2.0f));
    }
}
