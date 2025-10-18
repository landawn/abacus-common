package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleBiFunction2025Test extends TestBase {
    @Test
    public void testApply_WithNegativeValues() {
        DoubleBiFunction<Double> function = (t, u) -> t * u;
        Double result = function.apply(-5.0, 3.0);
        assertEquals(-15.0, result, 0.0001);
    }

    @Test
    public void testApply_WithZeroValues() {
        DoubleBiFunction<Double> function = (t, u) -> t / u;
        Double result = function.apply(0.0, 5.0);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    public void testApply_WithSpecialValues() {
        DoubleBiFunction<Boolean> function = (t, u) -> Double.isInfinite(t) || Double.isInfinite(u);

        assertEquals(true, function.apply(Double.POSITIVE_INFINITY, 1.0));
        assertEquals(true, function.apply(1.0, Double.NEGATIVE_INFINITY));
        assertEquals(false, function.apply(1.0, 2.0));
    }

    @Test
    public void testApply_ReturningNull() {
        DoubleBiFunction<String> function = (t, u) -> null;
        String result = function.apply(1.0, 2.0);
        assertEquals(null, result);
    }

    @Test
    public void testApply_ReturningComplexObject() {
        DoubleBiFunction<java.util.Map<String, Double>> function = (t, u) -> {
            java.util.Map<String, Double> map = new java.util.HashMap<>();
            map.put("t", t);
            map.put("u", u);
            map.put("sum", t + u);
            return map;
        };

        java.util.Map<String, Double> result = function.apply(10.0, 20.0);
        assertEquals(10.0, result.get("t"), 0.0001);
        assertEquals(20.0, result.get("u"), 0.0001);
        assertEquals(30.0, result.get("sum"), 0.0001);
    }

    @Test
    public void testAndThen_WithIdentityFunction() {
        DoubleBiFunction<String> function = (t, u) -> "result=" + (t + u);
        DoubleBiFunction<String> composed = function.andThen(s -> s);

        String result = composed.apply(1.0, 2.0);
        assertEquals("result=3.0", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleBiFunction<String> lambda = (t, u) -> "test";
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.apply(1.0, 2.0));
    }
}
