package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        final LongFunction<String> function = val -> "Value: " + val;
        final String result = function.apply(42L);
        assertEquals("Value: 42", result);
    }

    @Test
    public void testApply_withLambda() {
        final LongFunction<Long> function = val -> val * 2;
        final Long result = function.apply(21L);
        assertEquals(42L, result);
    }

    @Test
    public void testApply_withAnonymousClass() {
        final LongFunction<String> function = new LongFunction<String>() {
            @Override
            public String apply(final long value) {
                return "Number: " + value;
            }
        };

        final String result = function.apply(100L);
        assertEquals("Number: 100", result);
    }

    @Test
    public void testAndThen() {
        final LongFunction<String> function = val -> String.valueOf(val);
        final java.util.function.Function<String, Integer> after = String::length;

        final LongFunction<Integer> composed = function.andThen(after);
        final Integer result = composed.apply(12345L);

        assertEquals(5, result);
    }

    @Test
    public void testAndThen_multipleChains() {
        final LongFunction<Long> function = val -> val * 2;
        final java.util.function.Function<Long, String> toString = Object::toString;
        final java.util.function.Function<String, Integer> toLength = String::length;

        final LongFunction<Integer> composed = function.andThen(toString).andThen(toLength);
        final Integer result = composed.apply(5L); // 5 * 2 = 10, "10".length() = 2

        assertEquals(2, result);
    }

    @Test
    public void testBOX() {
        final Long result = LongFunction.BOX.apply(42L);
        assertEquals(42L, result);
    }

    @Test
    public void testIdentity() {
        final LongFunction<Long> identity = LongFunction.identity();
        final Long result = identity.apply(123L);
        assertEquals(123L, result);
    }

    @Test
    public void testApply_returnsNull() {
        final LongFunction<String> function = val -> null;
        final String result = function.apply(1L);
        assertNull(result);
    }

    @Test
    public void testApply_withNegativeValue() {
        final LongFunction<String> function = val -> "Value: " + val;
        final String result = function.apply(-42L);
        assertEquals("Value: -42", result);
    }

    @Test
    public void testApply_withZero() {
        final LongFunction<Boolean> function = val -> val == 0;
        final Boolean result = function.apply(0L);
        assertEquals(true, result);
    }

    @Test
    public void testApply_withMaxValue() {
        final LongFunction<String> function = String::valueOf;
        final String result = function.apply(Long.MAX_VALUE);
        assertEquals(String.valueOf(Long.MAX_VALUE), result);
    }

    @Test
    public void testApply_withMinValue() {
        final LongFunction<String> function = String::valueOf;
        final String result = function.apply(Long.MIN_VALUE);
        assertEquals(String.valueOf(Long.MIN_VALUE), result);
    }

    @Test
    public void testApply_returningDifferentType() {
        final LongFunction<Double> function = val -> val / 2.0;
        final Double result = function.apply(10L);
        assertEquals(5.0, result, 0.001);
    }

    @Test
    public void testApply_complexObject() {
        final LongFunction<String> function = val -> String.format("ID: %d", val);
        final String result = function.apply(12345L);
        assertEquals("ID: 12345", result);
    }

    @Test
    public void testAndThen_transformationType() {
        final LongFunction<String> function = val -> String.valueOf(val);
        final java.util.function.Function<String, Boolean> after = s -> s.length() > 2;

        final LongFunction<Boolean> composed = function.andThen(after);
        final Boolean result = composed.apply(123L);

        assertEquals(true, result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongFunction<String> function = val -> "test";
        assertNotNull(function.apply(1L));
    }

    @Test
    public void testMethodReference() {
        final LongFunction<String> function = this::convertToString;
        final String result = function.apply(42L);
        assertEquals("Value is 42", result);
    }

    private String convertToString(final long value) {
        return "Value is " + value;
    }

    @Test
    public void testBOX_identity() {
        final Long value = 42L;
        final Long result = LongFunction.BOX.apply(value);
        assertEquals(value, result);
    }

    @Test
    public void testIdentity_returnsSameValue() {
        final LongFunction<Long> identity = LongFunction.identity();
        assertEquals(0L, identity.apply(0L));
        assertEquals(-100L, identity.apply(-100L));
        assertEquals(Long.MAX_VALUE, identity.apply(Long.MAX_VALUE));
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final java.util.function.LongFunction<String> javaFunction = String::valueOf;
        final LongFunction<String> abacusFunction = javaFunction::apply;

        final String result = abacusFunction.apply(42L);
        assertEquals("42", result);
    }
}
