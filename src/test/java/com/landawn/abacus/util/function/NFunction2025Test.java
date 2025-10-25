package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class NFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        NFunction<Integer, Integer> function = args -> {
            int sum = 0;
            for (Integer n : args) {
                sum += n;
            }
            return sum;
        };

        Integer result = function.apply(1, 2, 3, 4, 5);

        assertEquals(15, result);
    }

    @Test
    public void testApplyWithNoArguments() {
        NFunction<String, String> function = args -> {
            return "empty";
        };

        String result = function.apply();

        assertEquals("empty", result);
    }

    @Test
    public void testApplyWithSingleArgument() {
        NFunction<String, String> function = args -> {
            return args.length > 0 ? args[0].toUpperCase() : "";
        };

        String result = function.apply("hello");

        assertEquals("HELLO", result);
    }

    @Test
    public void testApplyWithLambda() {
        NFunction<String, String> function = args -> {
            return String.join(", ", args);
        };

        String result = function.apply("apple", "banana", "orange");

        assertEquals("apple, banana, orange", result);
    }

    @Test
    public void testApplyWithAnonymousClass() {
        NFunction<Integer, Double> function = new NFunction<Integer, Double>() {
            @Override
            public Double apply(Integer... args) {
                if (args.length == 0)
                    return 0.0;
                double sum = 0;
                for (Integer n : args) {
                    sum += n;
                }
                return sum / args.length;
            }
        };

        Double result = function.apply(10, 20, 30);

        assertEquals(20.0, result);
    }

    @Test
    public void testAndThen() {
        NFunction<Integer, Integer> function = args -> {
            int sum = 0;
            for (Integer n : args) {
                sum += n;
            }
            return sum;
        };
        java.util.function.Function<Integer, String> after = n -> "Total: " + n;

        NFunction<Integer, String> combined = function.andThen(after);
        String result = combined.apply(10, 20, 30);

        assertEquals("Total: 60", result);
    }

    @Test
    public void testAndThenChaining() {
        NFunction<Integer, Integer> function = args -> {
            int sum = 0;
            for (Integer n : args) {
                sum += n;
            }
            return sum;
        };
        java.util.function.Function<Integer, Integer> after1 = n -> n * 2;
        java.util.function.Function<Integer, String> after2 = n -> "Result: " + n;

        NFunction<Integer, String> combined = function.andThen(after1).andThen(after2);
        String result = combined.apply(5, 10, 15);

        assertEquals("Result: 60", result);
    }

    @Test
    public void testWithDifferentTypes() {
        NFunction<String, Integer> function = args -> {
            int totalLength = 0;
            for (String s : args) {
                totalLength += s.length();
            }
            return totalLength;
        };

        Integer result = function.apply("hello", "world", "!");

        assertEquals(11, result);
    }

    @Test
    public void testWithNullValues() {
        NFunction<String, Integer> function = args -> {
            int count = 0;
            for (String s : args) {
                if (s != null)
                    count++;
            }
            return count;
        };

        Integer result = function.apply("a", null, "b", null, "c");

        assertEquals(3, result);
    }

    @Test
    public void testReturningNull() {
        NFunction<String, String> function = args -> null;

        String result = function.apply("a", "b");

        assertNull(result);
    }

    @Test
    public void testComplexCalculation() {
        NFunction<Integer, Integer> function = args -> {
            if (args.length == 0)
                return 0;
            int max = args[0];
            for (Integer n : args) {
                if (n > max)
                    max = n;
            }
            return max;
        };

        Integer result = function.apply(5, 10, 3, 20, 7);

        assertEquals(20, result);
    }

    @Test
    public void testStringManipulation() {
        NFunction<String, String> function = args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                sb.append(i).append(":").append(args[i]);
                if (i < args.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        };

        String result = function.apply("a", "b", "c");

        assertEquals("0:a, 1:b, 2:c", result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(NFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
