package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleObjFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        DoubleObjFunction<String, String> function = (d, s) -> d + ":" + s;

        String result1 = function.apply(5.0, "test");
        String result2 = function.apply(10.5, "value");

        assertEquals("5.0:test", result1);
        assertEquals("10.5:value", result2);
    }

    @Test
    public void testApplyWithLambda() {
        DoubleObjFunction<Integer, Double> function = (d, i) -> d * i;

        Double result = function.apply(2.5, 4);

        assertEquals(10.0, result);
    }

    @Test
    public void testApplyWithAnonymousClass() {
        DoubleObjFunction<String, String> function = new DoubleObjFunction<String, String>() {
            @Override
            public String apply(double t, String u) {
                return String.format("%.2f-%s", t, u);
            }
        };

        String result = function.apply(3.14159, "pi");

        assertEquals("3.14-pi", result);
    }

    @Test
    public void testAndThen() {
        DoubleObjFunction<String, String> function = (d, s) -> d + ":" + s;
        java.util.function.Function<String, String> after = s -> s.toUpperCase();

        DoubleObjFunction<String, String> combined = function.andThen(after);
        String result = combined.apply(5.0, "test");

        assertEquals("5.0:TEST", result);
    }

    @Test
    public void testAndThenChaining() {
        DoubleObjFunction<Integer, Integer> function = (d, i) -> (int) (d + i);
        java.util.function.Function<Integer, Integer> after1 = i -> i * 2;
        java.util.function.Function<Integer, String> after2 = i -> "Result:" + i;

        DoubleObjFunction<Integer, String> combined = function.andThen(after1).andThen(after2);
        String result = combined.apply(5.0, 10);

        assertEquals("Result:30", result);
    }

    @Test
    public void testWithNegativeValues() {
        DoubleObjFunction<String, String> function = (d, s) -> d + ":" + s;

        String result1 = function.apply(-5.5, "negative");
        String result2 = function.apply(-100.75, "value");

        assertEquals("-5.5:negative", result1);
        assertEquals("-100.75:value", result2);
    }

    @Test
    public void testWithBoundaryValues() {
        DoubleObjFunction<String, String> function = (d, s) -> d + ":" + s;

        String result1 = function.apply(Double.MAX_VALUE, "max");
        String result2 = function.apply(Double.MIN_VALUE, "min");
        String result3 = function.apply(0.0, "zero");

        assertEquals(Double.MAX_VALUE + ":max", result1);
        assertEquals(Double.MIN_VALUE + ":min", result2);
        assertEquals("0.0:zero", result3);
    }

    @Test
    public void testWithSpecialValues() {
        DoubleObjFunction<String, String> function = (d, s) -> d + ":" + s;

        String result1 = function.apply(Double.NaN, "nan");
        String result2 = function.apply(Double.POSITIVE_INFINITY, "posinf");
        String result3 = function.apply(Double.NEGATIVE_INFINITY, "neginf");

        assertEquals("NaN:nan", result1);
        assertEquals("Infinity:posinf", result2);
        assertEquals("-Infinity:neginf", result3);
    }

    @Test
    public void testWithNullObject() {
        DoubleObjFunction<String, String> function = (d, s) -> d + ":" + s;

        String result = function.apply(5.0, null);

        assertEquals("5.0:null", result);
    }

    @Test
    public void testReturningNull() {
        DoubleObjFunction<String, String> function = (d, s) -> null;

        String result = function.apply(5.0, "test");

        assertNull(result);
    }

    @Test
    public void testWithDifferentTypes() {
        DoubleObjFunction<Integer, Boolean> function = (d, i) -> d > i;

        Boolean result1 = function.apply(10.5, 5);
        Boolean result2 = function.apply(3.5, 10);

        assertEquals(true, result1);
        assertEquals(false, result2);
    }

    @Test
    public void testComplexCalculation() {
        DoubleObjFunction<Double, Double> function = (d1, d2) -> d1 * d2 + d1 / d2;

        Double result = function.apply(10.0, 2.0);

        assertEquals(25.0, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(DoubleObjFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
