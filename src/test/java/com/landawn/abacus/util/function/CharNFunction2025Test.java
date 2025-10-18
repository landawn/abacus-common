package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharNFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        CharNFunction<String> toString = args -> {
            StringBuilder sb = new StringBuilder();
            for (char c : args) {
                sb.append(c);
            }
            return sb.toString();
        };

        assertEquals("abc", toString.apply('a', 'b', 'c'));
        assertEquals("X", toString.apply('X'));
    }

    @Test
    public void testApplyWithLambda() {
        CharNFunction<Integer> count = args -> args.length;

        assertEquals(3, count.apply('a', 'b', 'c'));
        assertEquals(5, count.apply('v', 'w', 'x', 'y', 'z'));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        CharNFunction<String> toUpper = new CharNFunction<String>() {
            @Override
            public String apply(char... args) {
                StringBuilder sb = new StringBuilder();
                for (char c : args) {
                    sb.append(Character.toUpperCase(c));
                }
                return sb.toString();
            }
        };

        assertEquals("ABC", toUpper.apply('a', 'b', 'c'));
        assertEquals("XYZ", toUpper.apply('x', 'y', 'z'));
    }

    @Test
    public void testApplyEmptyArray() {
        CharNFunction<Integer> count = args -> args.length;

        assertEquals(0, count.apply());
    }

    @Test
    public void testApplySingleElement() {
        CharNFunction<Character> first = args -> args.length > 0 ? args[0] : null;

        assertEquals('X', first.apply('X'));
    }

    @Test
    public void testApplyManyElements() {
        CharNFunction<Integer> count = args -> args.length;

        assertEquals(7, count.apply('a', 'b', 'c', 'd', 'e', 'f', 'g'));
    }

    @Test
    public void testAndThen() {
        CharNFunction<String> concat = args -> {
            StringBuilder sb = new StringBuilder();
            for (char c : args) {
                sb.append(c);
            }
            return sb.toString();
        };
        java.util.function.Function<String, String> toUpper = String::toUpperCase;

        CharNFunction<String> combined = concat.andThen(toUpper);

        assertEquals("ABC", combined.apply('a', 'b', 'c'));
        assertEquals("XYZ", combined.apply('x', 'y', 'z'));
    }

    @Test
    public void testAndThenChaining() {
        CharNFunction<String> concat = args -> {
            StringBuilder sb = new StringBuilder();
            for (char c : args) {
                sb.append(c);
            }
            return sb.toString();
        };
        java.util.function.Function<String, String> toUpper = String::toUpperCase;
        java.util.function.Function<String, String> addPrefix = s -> "Result: " + s;

        CharNFunction<String> combined = concat.andThen(toUpper).andThen(addPrefix);

        assertEquals("Result: ABC", combined.apply('a', 'b', 'c'));
    }

    @Test
    public void testReturningComplexObject() {
        CharNFunction<TestObject> createObject = args -> new TestObject(args);

        TestObject obj = createObject.apply('a', 'b', 'c');
        assertEquals(3, obj.values.length);
        assertEquals('a', obj.values[0]);
    }

    @Test
    public void testWithSpecialCharacters() {
        CharNFunction<String> toString = args -> {
            StringBuilder sb = new StringBuilder();
            for (char c : args) {
                sb.append(c);
            }
            return sb.toString();
        };

        assertEquals("\n\t", toString.apply('\n', '\t'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharNFunction<String> toString = args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(",");
                sb.append((int) args[i]);
            }
            return sb.toString();
        };

        String result = toString.apply(Character.MIN_VALUE, 'M', Character.MAX_VALUE);
        assertEquals((int) Character.MIN_VALUE + "," + (int) 'M' + "," + (int) Character.MAX_VALUE, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharNFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final char[] values;

        TestObject(char... values) {
            this.values = values;
        }
    }
}
