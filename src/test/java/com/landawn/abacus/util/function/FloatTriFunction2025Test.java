package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatTriFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        final FloatTriFunction<Float> function = (a, b, c) -> a + b + c;
        final Float result = function.apply(10.5f, 20.3f, 30.2f);
        assertEquals(61.0f, result, 0.001f);
    }

    @Test
    public void testAndThen() {
        final FloatTriFunction<Float> function = (a, b, c) -> a + b + c;
        final java.util.function.Function<Float, String> after = val -> "Result: " + val;

        final FloatTriFunction<String> composed = function.andThen(after);
        final String result = composed.apply(10.5f, 20.3f, 30.2f);

        assertEquals("Result: 61.0", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatTriFunction<String> function = (a, b, c) -> "test";
        assertNotNull(function.apply(1.0f, 2.0f, 3.0f));
    }
}
