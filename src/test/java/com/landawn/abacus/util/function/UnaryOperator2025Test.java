package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UnaryOperator2025Test extends TestBase {

    @Test
    public void testApply() {
        UnaryOperator<String> operator = s -> s.toUpperCase();
        String result = operator.apply("hello");
        assertEquals("HELLO", result);
    }

    @Test
    public void testApply_WithAnonymousClass() {
        UnaryOperator<Integer> operator = new UnaryOperator<Integer>() {
            @Override
            public Integer apply(Integer t) {
                return t * 2;
            }
        };

        Integer result = operator.apply(5);
        assertEquals(10, result);
    }

    @Test
    public void testIdentity() {
        UnaryOperator<String> identity = UnaryOperator.identity();
        assertEquals("test", identity.apply("test"));
        assertEquals("hello", identity.apply("hello"));
    }

    @Test
    public void testCompose() {
        UnaryOperator<String> trim = String::trim;
        UnaryOperator<String> upperCase = String::toUpperCase;

        UnaryOperator<String> composed = upperCase.compose(trim);
        String result = composed.apply("  hello  ");

        assertEquals("HELLO", result);
    }

    @Test
    public void testAndThen() {
        UnaryOperator<String> trim = String::trim;
        UnaryOperator<String> upperCase = String::toUpperCase;

        UnaryOperator<String> composed = trim.andThen(upperCase);
        String result = composed.apply("  hello  ");

        assertEquals("HELLO", result);
    }

    @Test
    public void testCompose_MultipleChains() {
        UnaryOperator<Integer> add1 = n -> n + 1;
        UnaryOperator<Integer> multiplyBy2 = n -> n * 2;
        UnaryOperator<Integer> subtract3 = n -> n - 3;

        UnaryOperator<Integer> composed = subtract3.compose(multiplyBy2).compose(add1);
        Integer result = composed.apply(5);

        assertEquals(9, result);   // ((5 + 1) * 2) - 3 = 9
    }

    @Test
    public void testAndThen_MultipleChains() {
        UnaryOperator<Integer> add1 = n -> n + 1;
        UnaryOperator<Integer> multiplyBy2 = n -> n * 2;
        UnaryOperator<Integer> subtract3 = n -> n - 3;

        UnaryOperator<Integer> composed = add1.andThen(multiplyBy2).andThen(subtract3);
        Integer result = composed.apply(5);

        assertEquals(9, result);   // ((5 + 1) * 2) - 3 = 9
    }

    @Test
    public void testToThrowable() {
        UnaryOperator<String> operator = s -> s.toUpperCase();
        com.landawn.abacus.util.Throwables.UnaryOperator<String, ?> throwableOperator = operator.toThrowable();
        assertNotNull(throwableOperator);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        UnaryOperator<String> operator = s -> s.toUpperCase();
        java.util.function.UnaryOperator<String> javaOperator = operator;

        String result = javaOperator.apply("hello");
        assertEquals("HELLO", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        UnaryOperator<String> lambda = s -> s;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.apply("test"));
    }
}
