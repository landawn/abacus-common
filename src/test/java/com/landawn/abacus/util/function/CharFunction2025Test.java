package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        CharFunction<String> toString = c -> String.valueOf(c);

        assertEquals("a", toString.apply('a'));
        assertEquals("Z", toString.apply('Z'));
        assertEquals("5", toString.apply('5'));
    }

    @Test
    public void testApplyWithLambda() {
        CharFunction<Integer> toInt = c -> (int) c;

        assertEquals(97, toInt.apply('a'));
        assertEquals(65, toInt.apply('A'));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        CharFunction<String> toUpperCase = new CharFunction<String>() {
            @Override
            public String apply(char value) {
                return String.valueOf(Character.toUpperCase(value));
            }
        };

        assertEquals("A", toUpperCase.apply('a'));
        assertEquals("Z", toUpperCase.apply('Z'));
    }

    @Test
    public void testAndThen() {
        CharFunction<Integer> toInt = c -> (int) c;
        java.util.function.Function<Integer, String> intToString = i -> "Code: " + i;

        CharFunction<String> combined = toInt.andThen(intToString);

        assertEquals("Code: 97", combined.apply('a'));
        assertEquals("Code: 65", combined.apply('A'));
    }

    @Test
    public void testAndThenChaining() {
        CharFunction<Character> toUpper = Character::toUpperCase;
        java.util.function.Function<Character, Integer> toInt = c -> (int) c;
        java.util.function.Function<Integer, String> toString = i -> "Result: " + i;

        CharFunction<String> combined = toUpper.andThen(toInt).andThen(toString);

        assertEquals("Result: 65", combined.apply('a'));
    }

    @Test
    public void testBox() {
        assertEquals(Character.valueOf('a'), CharFunction.BOX.apply('a'));
        assertEquals(Character.valueOf('Z'), CharFunction.BOX.apply('Z'));
        assertEquals(Character.valueOf('\0'), CharFunction.BOX.apply('\0'));
    }

    @Test
    public void testIdentity() {
        CharFunction<Character> identity = CharFunction.identity();

        assertEquals(Character.valueOf('a'), identity.apply('a'));
        assertEquals(Character.valueOf('Z'), identity.apply('Z'));
        assertEquals(Character.valueOf('\0'), identity.apply('\0'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharFunction<String> toString = c -> String.valueOf(c);

        assertEquals(String.valueOf(Character.MAX_VALUE), toString.apply(Character.MAX_VALUE));
        assertEquals(String.valueOf(Character.MIN_VALUE), toString.apply(Character.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        CharFunction<String> toString = String::valueOf;

        assertEquals("x", toString.apply('x'));
    }

    @Test
    public void testReturningComplexObject() {
        CharFunction<TestObject> createObject = c -> new TestObject(c);

        TestObject obj = createObject.apply('A');
        assertEquals('A', obj.value);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final char value;

        TestObject(char value) {
            this.value = value;
        }
    }
}
