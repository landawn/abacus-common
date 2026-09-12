package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiPredicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BuilderEqualsTest extends BuilderTestSupport {
    @Test
    public void testEquals_objects() {
        boolean result = Builder.equals("test", "test").result();
        assertTrue(result);
    }

    @Test
    public void testEquals_objectsNotEqual() {
        boolean result = Builder.equals("test", "other").result();
        assertFalse(result);
    }

    @Test
    public void testEquals_boolean() {
        boolean result = Builder.equals(true, true).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_booleanNotEqual() {
        boolean result = Builder.equals(true, false).result();
        assertFalse(result);
    }

    @Test
    public void testEquals_char() {
        boolean result = Builder.equals('a', 'a').result();
        assertTrue(result);
    }

    @Test
    public void testEquals_byte() {
        boolean result = Builder.equals((byte) 1, (byte) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_short() {
        boolean result = Builder.equals((short) 1, (short) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_int() {
        boolean result = Builder.equals(1, 1).result();
        assertTrue(result);

        Integer boxed = 1;
        assertTrue(Builder.equals(1, (int) boxed).result());
        assertTrue(Builder.equals((Object) 1, boxed).result());

        Integer nullable = null;
        assertFalse(Builder.equals((Object) 1, nullable).result());
        Assertions.assertThrows(NullPointerException.class, () -> Builder.equals(1, (int) nullable));
        assertFalse(Builder.equals((Object) Integer.valueOf(1), Long.valueOf(1)).result());
        assertTrue(Builder.equals(1L, boxed.longValue()).result());
    }

    @Test
    public void testEquals_long() {
        boolean result = Builder.equals(1L, 1L).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_float() {
        boolean result = Builder.equals(1.0f, 1.0f).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_floatWithTolerance() {
        boolean result = Builder.equals(1.0001f, 1.0002f, 0.001f).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_double() {
        boolean result = Builder.equals(1.0, 1.0).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_doubleWithTolerance() {
        boolean result = Builder.equals(1.00001, 1.00002, 0.0001).result();
        assertTrue(result);
    }

    @Test
    public void testEqualsPrimitives() {
        Assertions.assertTrue(Builder.equals(true, true).result());
        Assertions.assertFalse(Builder.equals(true, false).result());

        Assertions.assertTrue(Builder.equals('a', 'a').result());
        Assertions.assertFalse(Builder.equals('a', 'b').result());

        Assertions.assertTrue(Builder.equals((byte) 1, (byte) 1).result());
        Assertions.assertFalse(Builder.equals((byte) 1, (byte) 2).result());

        Assertions.assertTrue(Builder.equals((short) 1, (short) 1).result());
        Assertions.assertFalse(Builder.equals((short) 1, (short) 2).result());

        Assertions.assertTrue(Builder.equals(1, 1).result());
        Assertions.assertFalse(Builder.equals(1, 2).result());

        Assertions.assertTrue(Builder.equals(1L, 1L).result());
        Assertions.assertFalse(Builder.equals(1L, 2L).result());

        Assertions.assertTrue(Builder.equals(1.0f, 1.0f).result());
        Assertions.assertFalse(Builder.equals(1.0f, 2.0f).result());

        Assertions.assertTrue(Builder.equals(1.0, 1.0).result());
        Assertions.assertFalse(Builder.equals(1.0, 2.0).result());
    }

    @Test
    public void testEqualsWithTolerance() {
        Assertions.assertTrue(Builder.equals(1.0f, 1.001f, 0.01f).result());
        Assertions.assertFalse(Builder.equals(1.0f, 1.1f, 0.01f).result());

        Assertions.assertTrue(Builder.equals(1.0, 1.001, 0.01).result());
        Assertions.assertFalse(Builder.equals(1.0, 1.1, 0.01).result());
    }

    @Test
    public void testEquals_static_boolean() {
        assertTrue(Builder.equals(true, true).result());
        assertFalse(Builder.equals(true, false).result());
    }

    @Test
    public void testEquals_static_char() {
        assertTrue(Builder.equals('a', 'a').result());
        assertFalse(Builder.equals('a', 'b').result());
    }

    @Test
    public void testEquals_static_byte() {
        assertTrue(Builder.equals((byte) 1, (byte) 1).result());
        assertFalse(Builder.equals((byte) 1, (byte) 2).result());
    }

    @Test
    public void testEquals_static_short() {
        assertTrue(Builder.equals((short) 1, (short) 1).result());
        assertFalse(Builder.equals((short) 1, (short) 2).result());
    }

    @Test
    public void testEquals_static_int() {
        assertTrue(Builder.equals(42, 42).result());
        assertFalse(Builder.equals(42, 43).result());
    }

    @Test
    public void testEquals_static_long() {
        assertTrue(Builder.equals(100L, 100L).result());
        assertFalse(Builder.equals(100L, 200L).result());
    }

    @Test
    public void testEquals_static_float() {
        assertTrue(Builder.equals(1.5f, 1.5f).result());
        assertFalse(Builder.equals(1.5f, 2.5f).result());
    }

    @Test
    public void testEquals_static_floatWithTolerance() {
        assertTrue(Builder.equals(1.0f, 1.0001f, 0.001f).result());
        assertFalse(Builder.equals(1.0f, 2.0f, 0.001f).result());
    }

    @Test
    public void testEquals_static_double() {
        assertTrue(Builder.equals(3.14, 3.14).result());
        assertFalse(Builder.equals(3.14, 2.71).result());
    }

    @Test
    public void testEquals_static_doubleWithTolerance() {
        assertTrue(Builder.equals(1.0, 1.0001, 0.001).result());
        assertFalse(Builder.equals(1.0, 2.0, 0.001).result());
    }

    @Test
    public void testEquals_float_notEqual() {
        assertFalse(Builder.equals(1.0f, 2.0f).result());
    }

    @Test
    public void testEquals_double_notEqual() {
        assertFalse(Builder.equals(1.0, 2.0).result());
    }

    @Test
    public void testEquals_char_notEqual() {
        assertFalse(Builder.equals('a', 'b').result());
    }

    @Test
    public void testEquals_byte_notEqual() {
        assertFalse(Builder.equals((byte) 1, (byte) 2).result());
    }

    @Test
    public void testEquals_short_notEqual() {
        assertFalse(Builder.equals((short) 1, (short) 2).result());
    }

    @Test
    public void testEquals_int_notEqual() {
        assertFalse(Builder.equals(1, 2).result());
    }

    @Test
    public void testEquals_long_notEqual() {
        assertFalse(Builder.equals(1L, 2L).result());
    }

    @Test
    public void testEquals_floatWithTolerance_notEqual() {
        assertFalse(Builder.equals(1.0f, 2.0f, 0.001f).result());
    }

    @Test
    public void testEquals_doubleWithTolerance_notEqual() {
        assertFalse(Builder.equals(1.0, 2.0, 0.001).result());
    }

    @Test
    public void testEquals_objectsWithNull() {
        boolean result = Builder.equals(null, null).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_withPredicate() {
        boolean result = Builder.equals("test", "TEST", (a, b) -> a.equalsIgnoreCase(b)).result();
        assertTrue(result);
    }

    @Test
    public void testEquals() {
        boolean result = Builder.equals("a", "a").result();
        Assertions.assertTrue(result);

        result = Builder.equals("a", "b").result();
        Assertions.assertFalse(result);

        result = Builder.equals(null, null).result();
        Assertions.assertTrue(result);

        result = Builder.equals("a", null).result();
        Assertions.assertFalse(result);
    }

    @Test
    public void testEqualsWithFunction() {
        BiPredicate<String, String> caseInsensitiveEquals = (a, b) -> a.equalsIgnoreCase(b);

        boolean result = Builder.equals("Hello", "hello", caseInsensitiveEquals).result();
        Assertions.assertTrue(result);
    }

    @Test
    public void testEquals_objectNull_nonNull() {
        boolean result = Builder.equals(null, "hello").result();
        assertFalse(result);
    }

    @Test
    public void testEquals_objectNonNull_null() {
        boolean result = Builder.equals("hello", null).result();
        assertFalse(result);
    }
}
