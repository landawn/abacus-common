package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiPredicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BuilderEquivalenceTest extends BuilderTestSupport {
    @Test
    public void testEquivalenceBuilder_shortCircuit() {
        boolean result = Builder.equals("a", "b").equals(1, 1).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceChain() {
        boolean result = Builder.equals("a", "a").equals(1, 1).result();
        Assertions.assertTrue(result);

        result = Builder.equals("a", "a").equals(1, 2).result();
        Assertions.assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilderShortCircuit() {
        boolean[] evaluated = { false, false, false };

        BiPredicate<String, String> tracker1 = (a, b) -> {
            evaluated[0] = true;
            return true;
        };

        BiPredicate<String, String> tracker2 = (a, b) -> {
            evaluated[1] = true;
            return false;
        };

        BiPredicate<String, String> tracker3 = (a, b) -> {
            evaluated[2] = true;
            return true;
        };

        boolean result = Builder.equals("a", "a", tracker1).equals("b", "b", tracker2).equals("c", "c", tracker3).result();

        assertFalse(result);
        assertTrue(evaluated[0]);
        assertTrue(evaluated[1]);
        assertFalse(evaluated[2]);
    }

    @Test
    public void testEquivalenceBuilderChain() {
        boolean result = Builder.equals("a", "a").equals(5, 5).equals(true, true).result();

        assertTrue(result);

        result = Builder.equals("a", "a").equals(5, 10).equals(true, true).result();

        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilderPrimitives() {
        assertTrue(Builder.equals(true, true).result());
        assertTrue(Builder.equals('a', 'a').result());
        assertTrue(Builder.equals((byte) 5, (byte) 5).result());
        assertTrue(Builder.equals((short) 10, (short) 10).result());
        assertTrue(Builder.equals(100, 100).result());
        assertTrue(Builder.equals(100L, 100L).result());
        assertTrue(Builder.equals(1.5f, 1.5f).result());
        assertTrue(Builder.equals(2.5, 2.5).result());
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_boolean() {
        boolean result = Builder.equals("a", "a").equals(true, true).result();
        assertTrue(result);

        result = Builder.equals("a", "a").equals(true, false).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_char() {
        boolean result = Builder.equals("a", "a").equals('x', 'x').result();
        assertTrue(result);

        result = Builder.equals("a", "a").equals('x', 'y').result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_byte() {
        boolean result = Builder.equals("a", "a").equals((byte) 1, (byte) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_short() {
        boolean result = Builder.equals("a", "a").equals((short) 1, (short) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_int() {
        boolean result = Builder.equals("a", "a").equals(42, 42).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_long() {
        boolean result = Builder.equals("a", "a").equals(100L, 100L).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_float() {
        boolean result = Builder.equals("a", "a").equals(1.5f, 1.5f).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_floatWithTolerance() {
        boolean result = Builder.equals("a", "a").equals(1.0f, 1.0001f, 0.001f).result();
        assertTrue(result);

        result = Builder.equals("a", "a").equals(1.0f, 2.0f, 0.001f).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_double() {
        boolean result = Builder.equals("a", "a").equals(3.14, 3.14).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_doubleWithTolerance() {
        boolean result = Builder.equals("a", "a").equals(1.0, 1.0001, 0.001).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_object() {
        boolean result = Builder.equals(1, 1).equals("hello", "hello").result();
        assertTrue(result);

        result = Builder.equals(1, 1).equals("hello", "world").result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_shortCircuit_skipsSubsequent() {
        // When first equals is false, subsequent should be skipped
        boolean result = Builder.equals("a", "b").equals(1, 1).equals(true, true).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_skippedWhenFalse() {
        boolean result = Builder.equals("a", "b").equals("x", "x").equals(1, 1).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equalsWithPredicate_skippedWhenFalse() {
        boolean result = Builder.equals("a", "b").equals("x", "x", (a, b) -> a.equals(b)).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder() {
        assertTrue(Builder.equals(1, 1).result());
        assertFalse(Builder.equals(1, 2).result());

        assertTrue(Builder.equals("a", "a").result());
        assertFalse(Builder.equals("a", "b").result());

        assertTrue(Builder.equals(null, null).result());
        assertFalse(Builder.equals(null, "a").result());

        assertFalse(Builder.equals(1, 2).equals(3, 3).result());

        assertTrue(Builder.equals(true, true).result());
    }

    @Test
    public void testEquivalenceBuilderWithFunction() {
        BiPredicate<String, String> caseInsensitive = (s1, s2) -> s1.equalsIgnoreCase(s2);

        assertTrue(Builder.equals("Hello", "HELLO", caseInsensitive).result());
        assertFalse(Builder.equals("Hello", "World", caseInsensitive).result());
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_withPredicate() {
        boolean result = Builder.equals(1, 1).equals("ABC", "abc", (a, b) -> a.equalsIgnoreCase(b)).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_chaining() {
        boolean result = Builder.equals("a", "a").equals(1, 1).result();
        assertTrue(result);
    }
}
