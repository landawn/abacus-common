package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        DoubleFunction<String> function = value -> "value=" + value;
        String result = function.apply(5.5);
        assertEquals("value=5.5", result);
    }

    @Test
    public void testApply_WithMethodReference() {
        DoubleFunction<String> function = String::valueOf;
        String result = function.apply(10.5);
        assertEquals("10.5", result);
    }

    @Test
    public void testApply_WithAnonymousClass() {
        DoubleFunction<Integer> function = new DoubleFunction<Integer>() {
            @Override
            public Integer apply(double value) {
                return (int) Math.ceil(value);
            }
        };

        Integer result = function.apply(5.3);
        assertEquals(6, result);
    }

    @Test
    public void testBOX() {
        DoubleFunction<Double> box = DoubleFunction.BOX;
        Double result = box.apply(5.5);
        assertEquals(5.5, result, 0.0001);
    }

    @Test
    public void testAndThen() {
        DoubleFunction<String> function = value -> "value=" + value;
        java.util.function.Function<String, Integer> after = String::length;

        DoubleFunction<Integer> composed = function.andThen(after);
        Integer result = composed.apply(5.5);

        assertEquals(9, result); // "value=5.5".length() = 9
    }

    @Test
    public void testAndThen_MultipleChains() {
        DoubleFunction<Double> function = value -> value * 2;
        java.util.function.Function<Double, String> after1 = d -> "result=" + d;
        java.util.function.Function<String, Integer> after2 = String::length;

        DoubleFunction<Integer> composed = function.andThen(after1).andThen(after2);
        Integer result = composed.apply(5.0);

        assertEquals(11, result); // "result=10.0".length() = 11
    }

    @Test
    public void testIdentity() {
        DoubleFunction<Double> identity = DoubleFunction.identity();
        Double result = identity.apply(5.5);
        assertEquals(5.5, result, 0.0001);
    }

    @Test
    public void testIdentity_WithNegativeValue() {
        DoubleFunction<Double> identity = DoubleFunction.identity();
        Double result = identity.apply(-5.5);
        assertEquals(-5.5, result, 0.0001);
    }

    @Test
    public void testApply_WithNegativeValue() {
        DoubleFunction<Double> function = value -> value * -1;
        Double result = function.apply(-5.5);
        assertEquals(5.5, result, 0.0001);
    }

    @Test
    public void testApply_WithZeroValue() {
        DoubleFunction<String> function = value -> "zero=" + value;
        String result = function.apply(0.0);
        assertEquals("zero=0.0", result);
    }

    @Test
    public void testApply_WithSpecialValues() {
        DoubleFunction<Boolean> function = Double::isInfinite;

        assertEquals(false, function.apply(1.0));
        assertEquals(true, function.apply(Double.POSITIVE_INFINITY));
        assertEquals(true, function.apply(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testApply_ReturningNull() {
        DoubleFunction<String> function = value -> null;
        String result = function.apply(5.5);
        assertEquals(null, result);
    }

    @Test
    public void testApply_ReturningComplexObject() {
        DoubleFunction<java.util.Map<String, Object>> function = value -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("original", value);
            map.put("doubled", value * 2);
            map.put("squared", value * value);
            return map;
        };

        java.util.Map<String, Object> result = function.apply(5.0);
        assertEquals(5.0, (Double) result.get("original"), 0.0001);
        assertEquals(10.0, (Double) result.get("doubled"), 0.0001);
        assertEquals(25.0, (Double) result.get("squared"), 0.0001);
    }

    @Test
    public void testAndThen_WithIdentityFunction() {
        DoubleFunction<String> function = value -> "test=" + value;
        DoubleFunction<String> composed = function.andThen(s -> s);

        String result = composed.apply(5.0);
        assertEquals("test=5.0", result);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoubleFunction<String> function = value -> "value=" + value;
        java.util.function.DoubleFunction<String> javaFunction = function;

        String result = javaFunction.apply(5.5);
        assertEquals("value=5.5", result);
    }

    @Test
    public void testBOX_IsConstant() {
        DoubleFunction<Double> box1 = DoubleFunction.BOX;
        DoubleFunction<Double> box2 = DoubleFunction.BOX;
        assertSame(box1, box2);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleFunction<String> lambda = value -> "test";
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.apply(1.0));
    }
}
