package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleNFunction2025Test extends TestBase {

    @Test
    public void testApply_NoArgs() {
        DoubleNFunction<String> function = args -> "count=" + args.length;
        String result = function.apply();
        assertEquals("count=0", result);
    }

    @Test
    public void testApply_OneArg() {
        DoubleNFunction<Double> function = args -> args.length > 0 ? args[0] : 0.0;
        Double result = function.apply(5.5);
        assertEquals(5.5, result, 0.0001);
    }

    @Test
    public void testApply_MultipleArgs() {
        DoubleNFunction<Double> function = args -> {
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            return sum;
        };

        Double result = function.apply(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(15.0, result, 0.0001);
    }

    @Test
    public void testApply_WithAnonymousClass() {
        DoubleNFunction<String> function = new DoubleNFunction<String>() {
            @Override
            public String apply(double... args) {
                double product = 1.0;
                for (double arg : args) {
                    product *= arg;
                }
                return "product=" + product;
            }
        };

        String result = function.apply(2.0, 3.0, 4.0);
        assertEquals("product=24.0", result);
    }

    @Test
    public void testAndThen() {
        DoubleNFunction<String> function = args -> "length=" + args.length;
        java.util.function.Function<String, Integer> after = String::length;

        DoubleNFunction<Integer> composed = function.andThen(after);
        Integer result = composed.apply(1.0, 2.0, 3.0);

        assertEquals(8, result);   // "length=3".length() = 8
    }

    @Test
    public void testAndThen_MultipleChains() {
        DoubleNFunction<Double> function = args -> {
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            return sum;
        };
        java.util.function.Function<Double, String> after1 = d -> "sum=" + d;
        java.util.function.Function<String, Integer> after2 = String::length;

        DoubleNFunction<Integer> composed = function.andThen(after1).andThen(after2);
        Integer result = composed.apply(10.0, 20.0);

        assertEquals(8, result);   // "sum=30.0".length() = 8
    }

    @Test
    public void testApply_CalculatingAverage() {
        DoubleNFunction<Double> function = args -> {
            if (args.length == 0) {
                return 0.0;
            }
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            return sum / args.length;
        };

        Double result = function.apply(2.0, 4.0, 6.0, 8.0);
        assertEquals(5.0, result, 0.0001);
    }

    @Test
    public void testApply_CalculatingMax() {
        DoubleNFunction<Double> function = args -> {
            if (args.length == 0) {
                return Double.MIN_VALUE;
            }
            double max = args[0];
            for (int i = 1; i < args.length; i++) {
                if (args[i] > max) {
                    max = args[i];
                }
            }
            return max;
        };

        Double result = function.apply(3.0, 7.0, 2.0, 9.0, 5.0);
        assertEquals(9.0, result, 0.0001);
    }

    @Test
    public void testApply_WithNegativeValues() {
        DoubleNFunction<Double> function = args -> {
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            return sum;
        };

        Double result = function.apply(-1.0, -2.0, -3.0);
        assertEquals(-6.0, result, 0.0001);
    }

    @Test
    public void testApply_WithZeroValues() {
        DoubleNFunction<Integer> function = args -> args.length;
        Integer result = function.apply(0.0, 0.0, 0.0);
        assertEquals(3, result);
    }

    @Test
    public void testApply_ReturningNull() {
        DoubleNFunction<String> function = args -> null;
        String result = function.apply(1.0, 2.0);
        assertEquals(null, result);
    }

    @Test
    public void testApply_WithVarargsArray() {
        DoubleNFunction<Double> function = args -> {
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            return sum;
        };

        double[] values = { 1.0, 2.0, 3.0, 4.0 };
        Double result = function.apply(values);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testApply_ReturningComplexObject() {
        DoubleNFunction<java.util.Map<String, Object>> function = args -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("count", args.length);
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            map.put("sum", sum);
            map.put("average", args.length > 0 ? sum / args.length : 0.0);
            return map;
        };

        java.util.Map<String, Object> result = function.apply(2.0, 4.0, 6.0);
        assertEquals(3, result.get("count"));
        assertEquals(12.0, (Double) result.get("sum"), 0.0001);
        assertEquals(4.0, (Double) result.get("average"), 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleNFunction<String> lambda = args -> "test";
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.apply(1.0, 2.0, 3.0));
    }
}
