package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

@Tag("2025")
public class BinaryOperator2025Test extends TestBase {

    @Test
    public void testApply() {
        BinaryOperator<Integer> operator = (a, b) -> a + b;
        Integer result = operator.apply(10, 20);
        assertEquals(30, result);
    }

    @Test
    public void testApplyWithLambda() {
        BinaryOperator<String> operator = (s1, s2) -> s1 + s2;
        String result = operator.apply("Hello", "World");
        assertEquals("HelloWorld", result);
    }

    @Test
    public void testApplyWithMethodReference() {
        BinaryOperator<Integer> operator = Integer::sum;
        Integer result = operator.apply(15, 25);
        assertEquals(40, result);
    }

    @Test
    public void testApplyWithMax() {
        BinaryOperator<Integer> operator = Integer::max;
        Integer result = operator.apply(10, 20);
        assertEquals(20, result);
    }

    @Test
    public void testApplyWithMin() {
        BinaryOperator<Integer> operator = Integer::min;
        Integer result = operator.apply(10, 20);
        assertEquals(10, result);
    }

    @Test
    public void testAndThen() {
        BinaryOperator<Integer> operator = (a, b) -> a + b;
        java.util.function.Function<Integer, String> afterFunction = Object::toString;

        BiFunction<Integer, Integer, String> chainedFunction = operator.andThen(afterFunction);
        String result = chainedFunction.apply(10, 20);

        assertEquals("30", result);
    }

    @Test
    public void testAndThenMultipleChains() {
        BinaryOperator<Integer> operator = (a, b) -> a * b;
        java.util.function.Function<Integer, Integer> addTen = x -> x + 10;
        java.util.function.Function<Integer, Integer> doubleIt = x -> x * 2;

        BiFunction<Integer, Integer, Integer> chainedFunction = operator.andThen(addTen).andThen(doubleIt);
        Integer result = chainedFunction.apply(5, 3);

        assertEquals(50, result); // (5 * 3 + 10) * 2 = 50
    }

    @Test
    public void testApplyWithException() {
        BinaryOperator<Integer> operator = (a, b) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> operator.apply(10, 20));
    }

    @Test
    public void testToThrowable() {
        BinaryOperator<Integer> operator = (a, b) -> a + b;
        Throwables.BinaryOperator<Integer, ?> throwableOperator = operator.toThrowable();

        assertNotNull(throwableOperator);
    }

    @Test
    public void testToThrowableWithExecution() throws Throwable {
        BinaryOperator<Integer> operator = (a, b) -> a - b;
        Throwables.BinaryOperator<Integer, ?> throwableOperator = operator.toThrowable();

        Integer result = throwableOperator.apply(50, 8);
        assertEquals(42, result);
    }

    @Test
    public void testAnonymousClass() {
        BinaryOperator<String> operator = new BinaryOperator<String>() {
            @Override
            public String apply(String s1, String s2) {
                return s1 + "-" + s2;
            }
        };

        String result = operator.apply("foo", "bar");
        assertEquals("foo-bar", result);
    }

    @Test
    public void testWithStringConcatenation() {
        BinaryOperator<String> operator = (s1, s2) -> s1 + " " + s2;
        String result = operator.apply("Hello", "World");
        assertEquals("Hello World", result);
    }

    @Test
    public void testWithDoubleMultiplication() {
        BinaryOperator<Double> operator = (a, b) -> a * b;
        Double result = operator.apply(3.5, 2.0);
        assertEquals(7.0, result, 0.001);
    }

    @Test
    public void testWithNullValues() {
        BinaryOperator<String> operator = (s1, s2) -> {
            if (s1 == null && s2 == null)
                return "both null";
            if (s1 == null)
                return "first null";
            if (s2 == null)
                return "second null";
            return s1 + s2;
        };

        assertEquals("both null", operator.apply(null, null));
        assertEquals("first null", operator.apply(null, "test"));
        assertEquals("second null", operator.apply("test", null));
        assertEquals("ab", operator.apply("a", "b"));
    }

    @Test
    public void testAsBiFunction() {
        BinaryOperator<Integer> operator = (a, b) -> a + b;
        BiFunction<Integer, Integer, Integer> biFunction = operator;

        Integer result = biFunction.apply(10, 20);
        assertEquals(30, result);
    }
}
