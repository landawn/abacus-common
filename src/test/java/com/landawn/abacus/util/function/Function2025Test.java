package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Function2025Test extends TestBase {

    @Test
    public void testApply() {
        Function<String, Integer> function = String::length;
        Integer result = function.apply("hello");
        assertEquals(5, result);
    }

    @Test
    public void testApply_WithAnonymousClass() {
        Function<String, String> function = new Function<String, String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        };

        String result = function.apply("hello");
        assertEquals("HELLO", result);
    }

    @Test
    public void testIdentity() {
        Function<String, String> identity = Function.identity();
        assertEquals("test", identity.apply("test"));
        assertEquals("hello", identity.apply("hello"));
    }

    @Test
    public void testCompose() {
        Function<Integer, Integer> doubleIt = n -> n * 2;
        Function<Integer, String> toString = n -> String.valueOf(n);

        Function<Integer, String> composed = toString.compose(doubleIt);
        String result = composed.apply(5);

        assertEquals("10", result); // doubleIt first: 5*2=10, then toString: "10"
    }

    @Test
    public void testAndThen() {
        Function<String, Integer> length = String::length;
        Function<Integer, Integer> doubleIt = n -> n * 2;

        Function<String, Integer> composed = length.andThen(doubleIt);
        Integer result = composed.apply("hello");

        assertEquals(10, result); // "hello".length() = 5, then 5 * 2 = 10
    }

    @Test
    public void testCompose_MultipleChains() {
        Function<Integer, Integer> add1 = n -> n + 1;
        Function<Integer, Integer> multiplyBy2 = n -> n * 2;
        Function<Integer, Integer> subtract3 = n -> n - 3;

        Function<Integer, Integer> composed = subtract3.compose(multiplyBy2).compose(add1);
        Integer result = composed.apply(5);

        assertEquals(9, result); // ((5 + 1) * 2) - 3 = 9
    }

    @Test
    public void testAndThen_MultipleChains() {
        Function<Integer, Integer> add1 = n -> n + 1;
        Function<Integer, Integer> multiplyBy2 = n -> n * 2;
        Function<Integer, Integer> subtract3 = n -> n - 3;

        Function<Integer, Integer> composed = add1.andThen(multiplyBy2).andThen(subtract3);
        Integer result = composed.apply(5);

        assertEquals(9, result); // ((5 + 1) * 2) - 3 = 9
    }

    @Test
    public void testToThrowable() {
        Function<String, Integer> function = String::length;
        com.landawn.abacus.util.Throwables.Function<String, Integer, ?> throwableFunction = function.toThrowable();
        assertNotNull(throwableFunction);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        Function<String, Integer> function = String::length;
        java.util.function.Function<String, Integer> javaFunction = function;

        Integer result = javaFunction.apply("hello");
        assertEquals(5, result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        Function<String, String> lambda = s -> s;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.apply("test"));
    }
}
