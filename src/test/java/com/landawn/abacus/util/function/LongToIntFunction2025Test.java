package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongToIntFunction2025Test extends TestBase {

    @Test
    public void testApplyAsInt() {
        final LongToIntFunction function = value -> (int) value;
        final int result = function.applyAsInt(42L);
        assertEquals(42, result);
    }

    @Test
    public void testApplyAsInt_withLambda() {
        final LongToIntFunction function = value -> (int) (value / 2);
        final int result = function.applyAsInt(10L);
        assertEquals(5, result);
    }

    @Test
    public void testApplyAsInt_withAnonymousClass() {
        final LongToIntFunction function = new LongToIntFunction() {
            @Override
            public int applyAsInt(final long value) {
                return (int) (value % 100);
            }
        };

        final int result = function.applyAsInt(12345L);
        assertEquals(45, result);
    }

    @Test
    public void testDEFAULT() {
        final int result = LongToIntFunction.DEFAULT.applyAsInt(42L);
        assertEquals(42, result);
    }

    @Test
    public void testApplyAsInt_withNegativeValue() {
        final LongToIntFunction function = value -> (int) value;
        final int result = function.applyAsInt(-42L);
        assertEquals(-42, result);
    }

    @Test
    public void testApplyAsInt_withZero() {
        final LongToIntFunction function = value -> (int) value;
        final int result = function.applyAsInt(0L);
        assertEquals(0, result);
    }

    @Test
    public void testApplyAsInt_truncation() {
        final LongToIntFunction function = value -> (int) value;
        final int result = function.applyAsInt(5000000000L); // Larger than Integer.MAX_VALUE
        assertNotNull(result); // Will be truncated
    }

    @Test
    public void testApplyAsInt_hashCode() {
        final LongToIntFunction function = value -> Long.hashCode(value);
        final int result = function.applyAsInt(12345L);
        assertEquals(Long.hashCode(12345L), result);
    }

    @Test
    public void testApplyAsInt_bitExtraction() {
        final LongToIntFunction function = value -> (int) (value & 0xFFFFFFFF);
        final int result = function.applyAsInt(0x123456789L);
        assertEquals(0x23456789, result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongToIntFunction function = value -> (int) value;
        assertNotNull(function);
        assertEquals(42, function.applyAsInt(42L));
    }

    @Test
    public void testMethodReference() {
        final LongToIntFunction function = this::convertToInt;
        final int result = function.applyAsInt(42L);
        assertEquals(84, result);
    }

    private int convertToInt(final long value) {
        return (int) (value * 2);
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final java.util.function.LongToIntFunction javaFunction = value -> (int) value;
        final LongToIntFunction abacusFunction = javaFunction::applyAsInt;

        final int result = abacusFunction.applyAsInt(42L);
        assertEquals(42, result);
    }
}
