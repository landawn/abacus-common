package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharTriFunction2025Test extends TestBase {
    @Test
    public void testApplyWithAnonymousClass() {
        CharTriFunction<String> toUpper = new CharTriFunction<String>() {
            @Override
            public String apply(char a, char b, char c) {
                return "" + Character.toUpperCase(a) + Character.toUpperCase(b) + Character.toUpperCase(c);
            }
        };

        assertEquals("ABC", toUpper.apply('a', 'b', 'c'));
        assertEquals("XYZ", toUpper.apply('x', 'y', 'z'));
    }

    @Test
    public void testAndThen() {
        CharTriFunction<String> concat = (a, b, c) -> "" + a + b + c;
        java.util.function.Function<String, String> toUpper = String::toUpperCase;

        CharTriFunction<String> combined = concat.andThen(toUpper);

        assertEquals("ABC", combined.apply('a', 'b', 'c'));
        assertEquals("XYZ", combined.apply('x', 'y', 'z'));
    }

    @Test
    public void testAndThenChaining() {
        CharTriFunction<String> concat = (a, b, c) -> "" + a + b + c;
        java.util.function.Function<String, String> toUpper = String::toUpperCase;
        java.util.function.Function<String, String> addPrefix = s -> "Result: " + s;

        CharTriFunction<String> combined = concat.andThen(toUpper).andThen(addPrefix);

        assertEquals("Result: ABC", combined.apply('a', 'b', 'c'));
    }

    @Test
    public void testReturningComplexObject() {
        CharTriFunction<TestObject> createObject = (a, b, c) -> new TestObject(a, b, c);

        TestObject obj = createObject.apply('a', 'b', 'c');
        assertEquals('a', obj.a);
        assertEquals('b', obj.b);
        assertEquals('c', obj.c);
    }

    @Test
    public void testWithBoundaryValues() {
        CharTriFunction<String> toString = (a, b, c) -> "" + a + "," + b + "," + c;

        String result = toString.apply(Character.MIN_VALUE, 'M', Character.MAX_VALUE);
        assertEquals(Character.MIN_VALUE + ",M," + Character.MAX_VALUE, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharTriFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final char a;
        final char b;
        final char c;

        TestObject(char a, char b, char c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}
