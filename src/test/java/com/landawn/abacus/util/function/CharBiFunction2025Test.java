package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharBiFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        CharBiFunction<String> toString = (t, u) -> "" + t + u;

        assertEquals("ab", toString.apply('a', 'b'));
        assertEquals("XY", toString.apply('X', 'Y'));
    }

    @Test
    public void testApplyWithLambda() {
        CharBiFunction<Integer> sumCodePoints = (t, u) -> (int) t + (int) u;

        assertEquals(195, sumCodePoints.apply('a', 'b'));
        assertEquals(177, sumCodePoints.apply('A', 'p'));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        CharBiFunction<String> concat = new CharBiFunction<String>() {
            @Override
            public String apply(char t, char u) {
                return "" + Character.toUpperCase(t) + Character.toUpperCase(u);
            }
        };

        assertEquals("AB", concat.apply('a', 'b'));
        assertEquals("XY", concat.apply('x', 'y'));
    }

    @Test
    public void testAndThen() {
        CharBiFunction<String> concat = (t, u) -> "" + t + u;
        java.util.function.Function<String, String> toUpper = String::toUpperCase;

        CharBiFunction<String> combined = concat.andThen(toUpper);

        assertEquals("AB", combined.apply('a', 'b'));
        assertEquals("XY", combined.apply('x', 'y'));
    }

    @Test
    public void testAndThenChaining() {
        CharBiFunction<String> concat = (t, u) -> "" + t + u;
        java.util.function.Function<String, String> toUpper = String::toUpperCase;
        java.util.function.Function<String, String> addPrefix = s -> "Result: " + s;

        CharBiFunction<String> combined = concat.andThen(toUpper).andThen(addPrefix);

        assertEquals("Result: AB", combined.apply('a', 'b'));
    }

    @Test
    public void testReturningComplexObject() {
        CharBiFunction<TestObject> createObject = (t, u) -> new TestObject(t, u);

        TestObject obj = createObject.apply('a', 'b');
        assertEquals('a', obj.first);
        assertEquals('b', obj.second);
    }

    @Test
    public void testWithBoundaryValues() {
        CharBiFunction<String> toString = (t, u) -> "" + t + "," + u;

        String result = toString.apply(Character.MIN_VALUE, Character.MAX_VALUE);
        assertEquals(Character.MIN_VALUE + "," + Character.MAX_VALUE, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharBiFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final char first;
        final char second;

        TestObject(char first, char second) {
            this.first = first;
            this.second = second;
        }
    }
}
