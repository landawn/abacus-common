package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatNFunction2025Test extends TestBase {

    @Test
    public void testApply_noArgs() {
        final FloatNFunction<Integer> function = args -> args.length;
        final Integer result = function.apply();
        assertEquals(0, result);
    }

    @Test
    public void testApply_singleArg() {
        final FloatNFunction<Float> function = args -> args[0];
        final Float result = function.apply(42.5f);
        assertEquals(42.5f, result, 0.001f);
    }

    @Test
    public void testApply_multipleArgs() {
        final FloatNFunction<Float> function = args -> {
            float sum = 0;
            for (final float arg : args) {
                sum += arg;
            }
            return sum;
        };

        final Float result = function.apply(10.5f, 20.3f, 30.2f, 40.0f);
        assertEquals(101.0f, result, 0.001f);
    }

    @Test
    public void testAndThen() {
        final FloatNFunction<Float> function = args -> {
            float sum = 0;
            for (final float arg : args) {
                sum += arg;
            }
            return sum;
        };
        final java.util.function.Function<Float, String> after = val -> "Result: " + val;

        final FloatNFunction<String> composed = function.andThen(after);
        final String result = composed.apply(10.5f, 20.3f, 30.2f);

        assertEquals("Result: 61.0", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatNFunction<String> function = args -> "test";
        assertNotNull(function.apply(1.0f, 2.0f));
    }
}
